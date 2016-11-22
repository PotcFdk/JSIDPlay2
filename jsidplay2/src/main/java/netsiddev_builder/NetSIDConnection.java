package netsiddev_builder;

import static netsiddev.Response.BUSY;
import static netsiddev.Response.INFO;
import static netsiddev.Response.OK;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.util.Pair;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.components.pla.PLA;
import netsiddev.Response;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.GetConfigCount;
import netsiddev_builder.commands.GetConfigInfo;
import netsiddev_builder.commands.GetVersion;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.NetSIDPkg;
import netsiddev_builder.commands.SetClocking;
import netsiddev_builder.commands.SetSidLevel;
import netsiddev_builder.commands.SetSidPosition;
import netsiddev_builder.commands.TryDelay;
import netsiddev_builder.commands.TryRead;
import netsiddev_builder.commands.TryReset;
import netsiddev_builder.commands.TrySetSampling;
import netsiddev_builder.commands.TrySetSidCount;
import netsiddev_builder.commands.TrySetSidModel;
import netsiddev_builder.commands.TryWrite;

public class NetSIDConnection {
	private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	private static final int PORT = 6581;
	private static final String HOSTNAME = "127.0.0.1";

	private static final int CYCLES_TO_MILLIS = 1000;
	private static final int MAX_WRITE_CYCLES = 4096;
	private static final int CMD_BUFFER_SIZE = 4096;
	private static final int BUFFER_NEAR_FULL = MAX_WRITE_CYCLES * 3 >> 2;
	private static final int REGULAR_DELAY = 0xFFFF;

	byte VERSION;
	private EventScheduler context;
	private static Socket connectedSocket;
	private List<NetSIDPkg> commands = new ArrayList<>();
	private TryWrite tryWrite;
	private byte readResult;
	private String configName;
	private long lastSIDWriteTime;
	private int fastForwardFactor;
	private boolean startTimeReached;
	private static Map<Pair<ChipModel, String>, Byte> filterNameToSIDModel = new HashMap<>();

	private final Event event = new Event("JSIDDevice Delay") {

		@Override
		public void event() {
			// XXX we just delay and clock first SID for reading, does this have an impact?
			context.schedule(event, eventuallyDelay((byte) 0), Event.Phase.PHI2);
		}
	};

	/**
	 * Establish a single instance connection to a NetworkSIDDevice.
	 * 
	 * Always MAX_SIDS are reserved.
	 * 
	 * @param context
	 *            event context
	 */
	public NetSIDConnection(EventScheduler context) {
		this.context = context;
		try {
			if (connectedSocket == null || !connectedSocket.isConnected()) {
				connectedSocket = new Socket(HOSTNAME, PORT);
				VERSION = getNetworkProtocolVersion();
				// Get all available SIDs
				for (byte config = 0; config < getSIDCount(); config++) {
					Pair<ChipModel, String> filter = getSIDInfo(config);
					filterNameToSIDModel.put(new Pair<>(filter.getKey(), filter.getValue()), config);
				}
				// Initialize SIDs on server side
				commands.add(new TrySetSidCount((byte) PLA.MAX_SIDS));
				for (byte sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
					commands.add(new TrySetSidModel(sidNum, sidNum));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param model
	 *            chip model
	 * @return sorted filter names of the desired chip model (case-insensitive)
	 */
	public static List<String> getFilterNames(ChipModel model) {
		return filterNameToSIDModel.keySet().stream().filter(p -> p.getKey() == model).map(p -> p.getValue())
				.sorted((s1, s2) -> s1.compareToIgnoreCase(s2)).collect(Collectors.toList());
	}

	/**
	 * Lookup SID configuration of the desired name and configure
	 * NetworkSIDDevice accordingly.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param chipModel
	 *            SID chip model
	 * @param filterName
	 *            desired filter name
	 */
	public void setFilter(byte sidNum, final ChipModel chipModel, final String filterName) {
		send(() -> {
			Optional<Pair<ChipModel, String>> filter = filterNameToSIDModel.keySet().stream()
					.filter(p -> p.getKey() == chipModel && p.getValue().equals(filterName)).findFirst();
			if (filter.isPresent()) {
				return new TrySetSidModel(sidNum, filterNameToSIDModel.get(filter.get()));
			}
			System.err.println("Undefined Filter: " + filterName + ", will use first available filter, instead!");
			return new TrySetSidModel(sidNum, (byte) 0);
		});
	}

	private byte getNetworkProtocolVersion() {
		return sendReceive(() -> new GetVersion());
	}

	private byte getSIDCount() {
		return sendReceive(() -> new GetConfigCount());
	}

	private Pair<ChipModel, String> getSIDInfo(byte sidNum) {
		return sendReceiveConfig(() -> new GetConfigInfo(sidNum));
	}

	public void setClockFrequency(double cpuFrequency) {
		send(() -> new SetClocking(cpuFrequency));
	}

	public void setSampling(SamplingMethod sampling) {
		send(() -> new TrySetSampling(sampling));
	}

	public void flush() {
		send(() -> new Flush());
	}

	public void reset(byte volume) {
		send(() -> new Flush());
		send(() -> new TryReset(volume));
		if (!context.isPending(event)) {
			context.schedule(event, 0, Event.Phase.PHI2);
		}
	}

	public void setVoiceMute(byte sidNum, byte voice, boolean mute) {
		send(() -> new Mute(sidNum, voice, mute));
	}

	public void setVolume(byte sidNum, byte volume) {
		send(() -> new SetSidLevel(sidNum, volume));
	}

	public void setBalance(byte sidNum, float balance) {
		send(() -> new SetSidPosition(sidNum, balance));
	}

	public byte read(byte sidNum, byte addr) {
		if (!commands.isEmpty() && commands.get(0) instanceof TryWrite) {
			try {
				tryWrite.changeToTryRead(sidNum, clocksSinceLastAccess(), addr);
				flush(false);
				return readResult;
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			return sendReceive(() -> new TryRead(sidNum, clocksSinceLastAccess(), addr));
		}
	}

	public void write(byte sidNum, byte addr, byte data) {
		if (startTimeReached) {
			try {
				while (tryWrite(sidNum, clocksSinceLastAccess(), addr, data) == BUSY) {
					// Try_Write sleeps for us
				}
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Pair<ChipModel, String> sendReceiveConfig(Supplier<NetSIDPkg> cmdToAdd) {
		send(cmdToAdd);
		return new Pair<>(readResult == 1 ? ChipModel.MOS8580 : ChipModel.MOS6581, configName);
	}

	private byte sendReceive(Supplier<NetSIDPkg> cmdToAdd) {
		send(cmdToAdd);
		return readResult;
	}

	private final void send(Supplier<NetSIDPkg> cmdToAdd) {
		try {
			// deal with unsubmitted writes
			flush(false);

			commands.add(cmdToAdd.get());
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Add a SID write to the ring buffer, until it is full, then send it to
	 * JSIDPlay2 to be queued there and executed
	 */
	private Response tryWrite(byte sidNum, int cycles, byte reg, byte data) throws InterruptedException, IOException {
		/*
		 * flush writes after a bit of buffering. If no flush, then returns OK
		 * and we queue more. If flush attempt fails, we must cancel.
		 */
		if (maybeSendWritesToServer() == BUSY) {
			sleepDependingOnCyclesSent();
			return BUSY;
		}

		if (commands.isEmpty()) {
			/* start new write buffering sequence */
			tryWrite = new TryWrite();
			commands.add(tryWrite);
		}
		/* add write to queue */
		tryWrite.addWrite(cycles, (byte) (reg | (sidNum << 5)), data);
		/*
		 * NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case.
		 */
		maybeSendWritesToServer();

		return OK;
	}

	private Response flush(boolean giveUpIfBusy) throws IOException, InterruptedException {
		while (!commands.isEmpty()) {
			final NetSIDPkg cmd = commands.remove(0);

			connectedSocket.getOutputStream().write(cmd.toByteArrayWithLength());
			int rc = connectedSocket.getInputStream().read();
			if (rc == -1) {
				throw new RuntimeException("Server closed the connection!");
			}
			switch (Response.values()[rc]) {
			case VERSION:
			case READ:
			case COUNT:
				// version / read result / configuration count
				readResult = (byte) connectedSocket.getInputStream().read();
			case OK:
				continue;
			case BUSY:
				commands.add(0, cmd);
				if (giveUpIfBusy) {
					return BUSY;
				}
				sleepDependingOnCyclesSent();
				continue;
			case INFO:
				// chip model
				readResult = (byte) connectedSocket.getInputStream().read();
				// 0 terminated name
				if (rc == INFO.ordinal()) {
					byte[] configInfo = new byte[255];
					int chIdx;
					for (chIdx = 0; chIdx < configInfo.length; chIdx++) {
						connectedSocket.getInputStream().read(configInfo, chIdx, 1);
						if (configInfo[chIdx] <= 0)
							break;
					}
					configName = new String(configInfo, 0, chIdx, ISO_8859_1);
				}
				continue;
			default:
				throw new RuntimeException("Server error: Unexpected response!");
			}
		}
		return OK;
	}

	private void sleepDependingOnCyclesSent() throws InterruptedException {
		if (tryWrite.getCyclesSentToServer() > BUFFER_NEAR_FULL) {
			Thread.sleep(Math.max(1, tryWrite.getCyclesSentToServer() / CYCLES_TO_MILLIS - 3));
		}
	}

	private Response maybeSendWritesToServer() throws IOException, InterruptedException {
		/* flush writes after a bit of buffering */
		if (commands.size() == CMD_BUFFER_SIZE
				|| (tryWrite != null && tryWrite.getCyclesSentToServer() > MAX_WRITE_CYCLES)) {
			if (flush(true) == BUSY) {
				return BUSY;
			}
		}
		return OK;
	}

	private int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return diff >> fastForwardFactor;
	}

	protected long eventuallyDelay(byte sidNum) {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime) >> fastForwardFactor;
		// next writes must not be too soon, therefore * 2!
		if (diff > REGULAR_DELAY << 1) {
			lastSIDWriteTime += REGULAR_DELAY;
			send(() -> new TryDelay(sidNum, REGULAR_DELAY));
		}
		return REGULAR_DELAY;
	}

	public void start() {
		startTimeReached = true;
	}

	public void fastForward() {
		fastForwardFactor++;
	}

	public void normalSpeed() {
		fastForwardFactor = 0;
	}

	public boolean isFastForward() {
		return fastForwardFactor != 0;
	}

	public int getFastForwardBitMask() {
		return (1 << fastForwardFactor) - 1;
	}

}
