package server.netsiddev;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.SIDChip;
import libsidplay.common.SamplingMethod;
import libsidplay.sidtune.PSidHeader;
import libsidutils.fingerprinting.IFingerprintMatcher;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import server.netsiddev.ini.IniJSIDDeviceAudioSection;
import server.netsiddev.ini.JSIDDeviceConfig;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.fingerprinting.FingerprintJsonClient;
import sidplay.fingerprinting.WhatsSidSupport;

/**
 * Container for client-specific data.
 *
 * @author Ken Händel
 * @author Antti Lankila
 * @author Wilfred Bos
 *
 * @see NetworkSIDDevice
 */
class ClientContext {
	/** String encoding */
	private static final Charset ISO_8859 = Charset.forName("ISO-8859-1");

	/** See netsiddev.ad for version info of the protocol. */
	private static final byte SID_NETWORK_PROTOCOL_VERSION = 4;

	/** Maximum time to wait for queue in milliseconds. */
	private static final long MAX_TIME_TO_WAIT_FOR_QUEUE = 50;

	/** Expected buffer fill rate */
	private final int latency;

	/** Cached commands because values() returns new array each time. */
	private final Command[] commands = Command.values();

	/** Our back-end thread */
	private final AudioGeneratorThread eventConsumerThread;

	/** Shadow SID clocked with client to read from */
	private SIDChip[] sidRead;

	/**
	 * Allocate read buffer. Maximum command + maximum socket buffer size (assumed
	 * to be per request 16K)
	 */
	private final ByteBuffer dataRead = ByteBuffer.allocateDirect(65536 + 4 + 16384);

	/**
	 * Allocate write buffer. Maximum supported writes are currently 260 bytes long.
	 */
	private final ByteBuffer dataWrite = ByteBuffer.allocateDirect(260);

	/** A selectable channel for stream-oriented listening sockets. */
	private static ServerSocketChannel ssc = null;

	/** The selector which is registered to the server socket channel. */
	private static Selector selector;

	/** Current command. */
	private Command command;

	/** Current sid number in command. */
	private int sidNumber;

	/** Length of data packet associated to command. */
	private int dataLength;

	/** Current clock value in input. */
	private long inputClock;

	private PSidHeader tuneHeader;

	/**
	 * Indicates if a new connection should be opened in case of connection settings
	 * changes.
	 */
	private static boolean openNewConnection = true;

	/** Map which holds all instances of each client connection. */
	private static Map<SocketChannel, ClientContext> clientContextMap = new ConcurrentHashMap<>();

	private MusicInfoWithConfidenceBean whatsSidResult;

	private static int clientContextNumToCheck;

	private static Thread whatsSidThread;

	public static String getRecognizedTunes() {
		return clientContextMap.values().stream().map(cc -> toWhatsSidAnswer(cc)).collect(Collectors.joining("\n"));
	}

	private static String toWhatsSidAnswer(ClientContext cc) {
		StringBuilder result = new StringBuilder();
		if (cc.whatsSidResult != null) {
			result.append(cc.whatsSidResult.toString());
		}
		cc.whatsSidResult = null;
		return result.toString();
	}

	/** Construct a new audio player for connected client */
	private ClientContext(AudioConfig config, int latency) {
		this.latency = latency;
		((Buffer) dataWrite).position(dataWrite.capacity());
		eventConsumerThread = new AudioGeneratorThread(config);
		eventConsumerThread.start();
		((Buffer) dataRead).limit(4);

		setDefaultSidConfiguration();
	}

	private void setDefaultSidConfiguration() {
		sidRead = new SIDChip[1];
		sidRead[sidNumber] = NetworkSIDDevice.getSidConfig(0);
		eventConsumerThread.setSidArray(new SIDChip[1]);
		eventConsumerThread.setSID(sidNumber, NetworkSIDDevice.getSidConfig(0));
	}

	/**
	 * Callback to handle protocol after new data has been received.
	 *
	 * @throws InvalidCommandException
	 */
	private void processReadBuffer() throws InvalidCommandException {
		/* Not enough data to handle the data packet? */
		if (dataRead.position() < dataRead.limit()) {
			return;
		}

		/* Needs to read command? */
		if (command == null) {
			int commandByte = dataRead.get(0) & 0xff;
			if (commandByte >= commands.length) {
				throw new InvalidCommandException("Unknown command number: " + commandByte, 4);
			}
			command = commands[commandByte];
			sidNumber = dataRead.get(1) & 0xff;
			dataLength = dataRead.getShort(2) & 0xffff;

			((Buffer) dataRead).limit(4 + dataLength);
			if (dataRead.position() < dataRead.limit()) {
				return;
			}
		}

		long clientTimeDifference = inputClock - eventConsumerThread.getPlaybackClock();
		boolean isBufferFull = clientTimeDifference > latency;
		boolean isBufferHalfFull = clientTimeDifference > latency / 2;

		/* Handle data packet. */
		final BlockingQueue<SIDWrite> sidCommandQueue = eventConsumerThread.getSidCommandQueue();
		final JavaSound audioDriver = eventConsumerThread.getDriver();

		((Buffer) dataWrite).clear();

		switch (command) {
		case FLUSH:
			if (dataLength != 0) {
				throw new InvalidCommandException("FLUSH needs no data", dataLength);
			}

			sidCommandQueue.clear();
			audioDriver.flush();

			/*
			 * The playback clock may still increase for a while, because audio generation
			 * may still be ongoing. We aren't allowed to wait for it, either, so this is
			 * the best we can do.
			 */
			inputClock = eventConsumerThread.getPlaybackClock();
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case TRY_SET_SID_COUNT:
			if (dataLength != 0) {
				throw new InvalidCommandException("TRY_SET_SID_COUNT needs no data", dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			SIDChip[] sid = new SIDChip[sidNumber];
			sidRead = new SIDChip[sidNumber];
			eventConsumerThread.setSidArray(sid);

			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case MUTE:
			if (dataLength != 2) {
				throw new InvalidCommandException("MUTE needs 2 bytes (voice and channel to mute)", dataLength);
			}

			final byte voiceNo = dataRead.get(4);
			final boolean mute = dataRead.get(5) != 0;
			eventConsumerThread.mute(sidNumber, voiceNo, mute);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case TRY_RESET:
			if (dataLength != 1) {
				throw new InvalidCommandException("RESET needs 1 byte (volume after reset)", dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			final byte volume = dataRead.get(4);

			tuneHeader = null;
			/*
			 * The read-side SID reset is more profound, it actually fully initializes the
			 * SID.
			 */
			for (int i = 0; i < sidRead.length; i++) {
				eventConsumerThread.reset(i, volume);
				sidRead[i].reset();
				sidRead[i].write(0x18, volume);
			}
			eventConsumerThread.reopen();

			dataWrite.put((byte) Response.OK.ordinal());
			break;

		/* SID command queuing section. */
		case TRY_DELAY: {
			if (dataLength != 2) {
				throw new InvalidCommandException("TRY_DELAY needs 2 bytes (16-bit delay value)", dataLength);
			}

			if (isBufferHalfFull) {
				eventConsumerThread.ensureDraining();
			}

			if (isBufferFull) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			final int cycles = dataRead.getShort(4) & 0xffff;
			handleDelayPacket(sidNumber, cycles);
			dataWrite.put((byte) Response.OK.ordinal());
			break;
		}

		case TRY_WRITE: {
			if (dataLength < 4 && dataLength % 4 != 0) {
				throw new InvalidCommandException("TRY_WRITE needs 4*n bytes, with n > 1 (hardsid protocol)",
						dataLength);
			}

			if (isBufferHalfFull) {
				eventConsumerThread.ensureDraining();
			}

			if (isBufferFull) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			handleWritePacket(dataLength);
			dataWrite.put((byte) Response.OK.ordinal());
			break;
		}

		case TRY_READ: {
			if ((dataLength - 3) % 4 != 0) {
				throw new InvalidCommandException(
						"READ needs 4*n+3 bytes (4*n hardsid protocol + 16-bit delay + register to read)", dataLength);
			}

			if (isBufferHalfFull) {
				eventConsumerThread.ensureDraining();
			}

			if (isBufferFull) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			handleWritePacket(dataLength - 3);

			/* Handle the read off our simulated SID device. */
			final int readCycles = dataRead.getShort(4 + dataLength - 3) & 0xffff;
			final byte register = dataRead.get(4 + dataLength - 1);
			if (readCycles > 0) {
				handleDelayPacket(sidNumber, readCycles);
			}

			dataWrite.put((byte) Response.READ.ordinal());
			dataWrite.put(sidRead[sidNumber].read(register & 0x1f));
			break;
		}

		/* Metdata method section */
		case GET_VERSION:
			if (dataLength != 0) {
				throw new InvalidCommandException("GET_VERSION needs no data", dataLength);
			}

			dataWrite.put((byte) Response.VERSION.ordinal());
			dataWrite.put(SID_NETWORK_PROTOCOL_VERSION);
			break;

		case TRY_SET_SAMPLING:
			if (dataLength != 1) {
				throw new InvalidCommandException(
						"SET_SAMPLING needs 1 byte (method to use: 0=bad quality but fast, 1=good quality but slow)",
						dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			eventConsumerThread.setSampling(SamplingMethod.values()[dataRead.get(4)]);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case TRY_SET_CLOCKING:
			if (dataLength != 1) {
				throw new InvalidCommandException("SET_CLOCKING needs 1 byte (0=PAL, 1=NTSC)", dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			eventConsumerThread.setClocking(CPUClock.values()[dataRead.get(4)]);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case GET_CONFIG_COUNT:
			if (dataLength != 0) {
				throw new InvalidCommandException("GET_COUNT needs no data", dataLength);
			}

			dataWrite.put((byte) Response.COUNT.ordinal());
			dataWrite.put(NetworkSIDDevice.getSidCount());
			break;

		case GET_CONFIG_INFO:
			if (dataLength != 0) {
				throw new InvalidCommandException("GET_INFO needs no data", dataLength);
			}

			dataWrite.put((byte) Response.INFO.ordinal());
			dataWrite
					.put((byte) (NetworkSIDDevice.getSidConfig(sidNumber).getChipModel() == ChipModel.MOS8580 ? 1 : 0));
			byte[] name = NetworkSIDDevice.getSidName(sidNumber).getBytes(ISO_8859);

			dataWrite.put(name, 0, Math.min(name.length, 255));
			dataWrite.put((byte) 0);
			break;

		case SET_SID_POSITION:
			if (dataLength != 1) {
				throw new InvalidCommandException("SET_SID_POSITION needs 1 byte", dataLength);
			}

			eventConsumerThread.setPosition(sidNumber, dataRead.get(4));
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case SET_SID_LEVEL:
			if (dataLength != 1) {
				throw new InvalidCommandException("SET_SID_LEVEL needs 1 byte", dataLength);
			}

			eventConsumerThread.setLevelAdjustment(sidNumber, dataRead.get(4));
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case TRY_SET_SID_MODEL:
			if (dataLength != 1) {
				throw new InvalidCommandException("SET_SID_MODEL needs 1 byte", dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			final int config = dataRead.get(4) & 0xff;

			sidRead[sidNumber] = NetworkSIDDevice.getSidConfig(config);
			eventConsumerThread.setSID(sidNumber, NetworkSIDDevice.getSidConfig(config));

			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case SET_DELAY:
			if (dataLength != 1) {
				throw new InvalidCommandException("SET_DELAY needs 1 byte", dataLength);
			}

			final int delay = dataRead.get(4) & 0xff;

			eventConsumerThread.setDelay(sidNumber, delay);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case SET_FADE_IN:
			if (dataLength != 4) {
				throw new InvalidCommandException(
						"SET_FADE_IN needs 4 bytes (float value according to the IEEE 754 floating-point \"single format\" bit layout)",
						dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			final long fadeInMillis = dataRead.getInt(4) & 0xffffffffl;
			final float fadeInSec = fadeInMillis / 1000;

			eventConsumerThread.setFadeIn(fadeInSec);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case SET_FADE_OUT:
			if (dataLength != 4) {
				throw new InvalidCommandException(
						"SET_FADE_OUT needs 4 bytes (float value according to the IEEE 754 floating-point \"single format\" bit layout)",
						dataLength);
			}

			if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
				dataWrite.put((byte) Response.BUSY.ordinal());
				break;
			}

			final long fadeOutMillis = dataRead.getInt(4) & 0xffffffffl;
			final float fadeOutSec = fadeOutMillis / 1000;

			eventConsumerThread.setFadeOut(fadeOutSec);
			dataWrite.put((byte) Response.OK.ordinal());
			break;

		case SET_SID_HEADER:
			final int sidHeaderSize = getSidHeaderSize(dataLength);

			if (dataLength < PSidHeader.SIZE || dataLength != sidHeaderSize) {
				throw new InvalidCommandException(
						"SET_SID_HEADER needs " + Math.max(sidHeaderSize, PSidHeader.SIZE) + " bytes", dataLength);
			}

			final byte[] tuneHeaderBytes = new byte[sidHeaderSize];
			for (int i = 0; i < sidHeaderSize; i++) {
				tuneHeaderBytes[i] = dataRead.get(4 + i);
			}

			tuneHeader = new PSidHeader(tuneHeaderBytes);

			dataWrite.put((byte) Response.OK.ordinal());
			break;

		default:
			throw new InvalidCommandException("Unsupported command: " + command);
		}

		((Buffer) dataWrite).limit(dataWrite.position());
		((Buffer) dataWrite).rewind();

		/* Move rest of the junk to beginning of bytebuffer */
		((Buffer) dataRead).position(4 + dataLength);
		dataRead.compact();

		/* Mark that we need to read a new command. */
		command = null;
		((Buffer) dataRead).limit(4);
	}

	private int getSidHeaderSize(final int dataLength) {
		if (dataLength >= PSidHeader.SIZE) {
			return (dataRead.get(4 + PSidHeader.DATA_OFFSET_FIELD) << 8)
					+ (dataRead.get(4 + PSidHeader.DATA_OFFSET_FIELD + 1) & 0xff);
		} else {
			return dataLength;
		}
	}

	private void handleDelayPacket(final int sidNumber, final int cycles) throws InvalidCommandException {
		Queue<SIDWrite> q = eventConsumerThread.getSidCommandQueue();
		inputClock += cycles;
		q.add(SIDWrite.makePureDelay(sidNumber, cycles));
		sidRead[sidNumber].clock(cycles, sample -> {
		});
	}

	private void handleWritePacket(final int dataLength) throws InvalidCommandException {
		Queue<SIDWrite> q = eventConsumerThread.getSidCommandQueue();
		for (int i = 0; i < dataLength; i += 4) {
			final int writeCycles = dataRead.getShort(4 + i) & 0xffff;
			byte reg = dataRead.get(4 + i + 2);
			byte sid = (byte) ((reg & 0xe0) >> 5);
			reg &= 0x1f;
			final byte value = dataRead.get(4 + i + 3);
			inputClock += writeCycles;
			q.add(new SIDWrite(sid, reg, value, writeCycles));
			sidRead[sid].clock(writeCycles, sample -> {
			});
			sidRead[sid].write(reg & 0x1f, value);
		}
	}

	protected void dispose() {
		if (!eventConsumerThread.waitUntilQueueReady(MAX_TIME_TO_WAIT_FOR_QUEUE)) {
			eventConsumerThread.interrupt();
			return;
		}

		eventConsumerThread.getSidCommandQueue().add(SIDWrite.makeEnd());
		eventConsumerThread.ensureDraining();
	}

	protected void disposeWait() {
		try {
			eventConsumerThread.join();
		} catch (InterruptedException e) {
		}
	}

	private ByteBuffer getReadBuffer() {
		return dataRead;
	}

	private ByteBuffer getWriteBuffer() {
		return dataWrite;
	}

	public PSidHeader getTuneHeader() {
		return tuneHeader;
	}

	/**
	 * changeDevice will change the device to the specified device for all connected
	 * client contexts
	 *
	 * @param deviceInfo the device that should be used
	 */
	public static void changeDevice(final Mixer.Info deviceInfo) {
		for (ClientContext clientContext : clientContextMap.values()) {
			clientContext.eventConsumerThread.changeDevice(deviceInfo);
		}
	}

	/**
	 * setDigiBoost will change the digiboost setting for each 8580 device for all
	 * connected client contexts
	 *
	 * @param enabled specifies if the digiboost feature is turned on
	 */
	public static void setDigiBoost(final boolean enabled) {
		for (ClientContext clientContext : clientContextMap.values()) {
			clientContext.eventConsumerThread.setDigiBoost(enabled);
		}
	}

	/**
	 * setAudioBufferSize will change the size of the audio buffer for all connected
	 * client contexts
	 *
	 * @param audioBufferSize specifies the size of the audio buffer (1024-16384 as
	 *                        a power of two)
	 */
	public static void setAudioBufferSize(final Integer audioBufferSize) {
		for (ClientContext clientContext : clientContextMap.values()) {
			clientContext.eventConsumerThread.setAudioBufferSize(audioBufferSize);
		}
	}

	public static Collection<PSidHeader> getTuneHeaders() {
		return clientContextMap.values().stream().map(clientContext -> clientContext.getTuneHeader())
				.collect(Collectors.toList());
	}

	public static int getClientsConnectedCount() {
		return clientContextMap.size();
	}

	/**
	 * applyConnectionConfigChanges will close all current connections and apply the
	 * new configuration which is stored in the SIDDeviceSettings.
	 */
	public static void applyConnectionConfigChanges() {
		openNewConnection = true;
		if (selector != null) {
			selector.wakeup();
		}
	}

	public static void listenForClients(final JSIDDeviceConfig config) {
		try {
			/* check for new connections. */
			openNewConnection = true;

			SIDDeviceSettings settings = SIDDeviceSettings.getInstance();
			startWhatsSidThread(settings);

			while (openNewConnection) {

				ssc = ServerSocketChannel.open();
				ssc.configureBlocking(false);

				boolean allowExternalIpConnections = settings.getAllowExternalConnections();

				String ipAddress = allowExternalIpConnections ? "0.0.0.0" : config.jsiddevice().getHostname();
				ssc.socket().bind(new InetSocketAddress(ipAddress, config.jsiddevice().getPort()));

				System.out.println("Opening listening socket on ip address " + ipAddress);

				selector = Selector.open();
				ssc.register(selector, SelectionKey.OP_ACCEPT);

				clientContextMap.clear();

				openNewConnection = false;
				while (selector.select() > 0) {
					if (openNewConnection) {
						break;
					}

					for (SelectionKey sk : selector.selectedKeys()) {
						if (sk.isAcceptable()) {
							SocketChannel sc = ((ServerSocketChannel) sk.channel()).accept();
							sc.socket().setReceiveBufferSize(16384);
							sc.socket().setSendBufferSize(1024);
							sc.configureBlocking(false);

							sc.register(selector, SelectionKey.OP_READ);
							IniJSIDDeviceAudioSection audio = config.audio();
							AudioConfig audioConfig = new AudioConfig(audio.getSamplingRate().getFrequency(), 2,
									audio.getAudioBufferSize());
							ClientContext cc = new ClientContext(audioConfig, config.jsiddevice().getLatency());
							clientContextMap.put(sc, cc);
							System.out.println("New client: " + cc);
						}

						if (sk.isReadable()) {
							SocketChannel sc = (SocketChannel) sk.channel();
							ClientContext cc = clientContextMap.get(sc);

							try {
								int length = sc.read(cc.getReadBuffer());
								if (length == -1) {
									throw new EOFException();
								}

								cc.processReadBuffer();
							} catch (Exception e) {
								System.out.println("Read: closing client " + cc + " due to exception: " + e);

								cc.dispose();
								clientContextMap.remove(sc);
								sk.cancel();
								sc.close();

								/*
								 * IOExceptions are probably not worth bothering user about, they could be
								 * normal stuff like apps exiting or closing connection. Other stuff is
								 * important, though.
								 */
								if (!(e instanceof IOException)) {
									StringWriter sw = new StringWriter();
									e.printStackTrace(new PrintWriter(sw));
									SwingUtilities.invokeLater(() -> {
										JOptionPane.showMessageDialog(null, sw.toString(), "Error",
												JOptionPane.ERROR_MESSAGE);
										System.exit(0);
									});
								}
								continue;
							}

							/* Switch to writing? */
							ByteBuffer data = cc.getWriteBuffer();
							if (data.remaining() != 0) {
								sc.register(selector, SelectionKey.OP_WRITE);
							}
						}

						if (sk.isWritable()) {
							SocketChannel sc = (SocketChannel) sk.channel();
							ClientContext cc = clientContextMap.get(sc);

							try {
								ByteBuffer data = cc.getWriteBuffer();
								sc.write(data);

								/* Switch to reading? */
								if (data.remaining() == 0) {
									sc.register(selector, SelectionKey.OP_READ);
								}
							} catch (IOException ioe) {
								System.out.println("Write: closing client " + cc + " due to exception: " + ioe);
								cc.dispose();
								clientContextMap.remove(sc);
								sk.cancel();
								sc.close();
								continue;
							}
						}
					}

					selector.selectedKeys().clear();
				}

				closeClientConnections();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void startWhatsSidThread(SIDDeviceSettings settings) {
		clientContextNumToCheck = 0;
		whatsSidThread = new Thread(() -> {
			do {
				try {
					Thread.sleep(settings.getWhatsSidMatchRetryTime() * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (settings.isWhatsSidEnable()) {
					Collection<ClientContext> clientContexts = clientContextMap.values();
					Optional<ClientContext> clientContextToCheck = clientContexts.stream().skip(clientContextNumToCheck)
							.findFirst();
					if (++clientContextNumToCheck >= clientContexts.size()) {
						clientContextNumToCheck = 0;
					}
					if (clientContextToCheck.isPresent()) {
						ClientContext clientContext = clientContextToCheck.get();
						WhatsSidSupport whatsSidSupport = clientContext.eventConsumerThread.getWhatsSidSupport();
						if (whatsSidSupport != null) {
							try {
								IFingerprintMatcher fingerprintMatcher = new FingerprintJsonClient(
										settings.getWhatsSidUrl(), settings.getWhatsSidUsername(),
										settings.getWhatsSidPassword(), settings.getWhatsSidConnectionTimeout());
								MusicInfoWithConfidenceBean result = whatsSidSupport.match(fingerprintMatcher);
								if (result != null) {
									clientContext.whatsSidResult = result;
								}
							} catch (Throwable e) {
								// server not available? silently ignore!
							}
						}
					}
				}
			} while (true);
		});
		whatsSidThread.setPriority(Thread.MIN_PRIORITY);
		whatsSidThread.start();
	}

	private static void closeClientConnections() throws IOException {
		for (ClientContext cc : clientContextMap.values()) {
			System.out.println("Cleaning up client: " + cc);
			cc.dispose();
		}

		for (SocketChannel sc : clientContextMap.keySet()) {
			sc.close();
		}

		for (ClientContext cc : clientContextMap.values()) {
			cc.disposeWait();
		}

		if (ssc.socket().isBound()) {
			ssc.socket().close();
		}

		if (selector.isOpen()) {
			selector.close();
		}

		ssc.close();
		System.out.println("Listening socket closed.");
	}
}