package ui.ultimate64;

import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_ON;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_ON;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import libsidplay.Ultimate64;
import libsidplay.common.CPUClock;
import libsidplay.common.VICChipModel;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.components.mos656x.Palette;
import libsidplay.config.IWhatsSidSection;
import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.client.FingerprintingClient;
import sidplay.Player;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.fingerprinting.IFingerprintMatcher;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.fingerprinting.WavBean;
import sidplay.fingerprinting.WhatsSidBuffer;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.common.Toast;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class Ultimate64Window extends C64Window implements Ultimate64 {

	private static final int SCREEN_HEIGHT = 272;
	private static final int SCREEN_WIDTH = 384;

	private static final int FRAME_RATE = 48000;
	private static final int CHANNELS = 2;
	private static final int AUDIO_BUFFER_SIZE = 192;

	private StreamingPlayer audioPlayer = new StreamingPlayer() {

		private DatagramSocket serverSocket;
		private JavaSound javaSound = new JavaSound();
		private Thread whatsSidMatcherThread;

		@Override
		protected void open() throws IOException, LineUnavailableException, InterruptedException {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();
			IWhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();
			String url = whatsSidSection.getUrl();
			String username = whatsSidSection.getUsername();
			String password = whatsSidSection.getPassword();

			AudioConfig audioConfig = new AudioConfig(FRAME_RATE, CHANNELS, 0, audioBufferSize.getValue());
			javaSound.open(audioConfig, null, CPUClock.PAL);

			whatsSidEnabled = whatsSidSection.isEnable();
			whatsSidBuffer = new WhatsSidBuffer(FRAME_RATE, whatsSidSection.getCaptureTime());
			fingerPrintMatcher = new FingerPrinting(new IniFingerprintConfig(),
					new FingerprintingClient(url, username, password));

			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingAudioPort());
			serverSocket.setSoTimeout(SOCKET_CONNECT_TIMEOUT);
			startStreaming(emulationSection, SOCKET_CMD_AUDIOSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
					+ ":" + emulationSection.getUltimate64StreamingAudioPort(), 0);
		}

		@Override
		protected void play() throws IOException, InterruptedException {
			IWhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

			byte[] receiveData = new byte[2 + (AUDIO_BUFFER_SIZE << 2)];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
//			int sequenceNo = ((receivePacket.getData()[1] & 0xff) << 8) | (receivePacket.getData()[0] & 0xff);
			/* left ch, right ch (16 bits each) */
			ShortBuffer shortBuffer = ByteBuffer.wrap(receiveData, 2, receiveData.length - 2)
					.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			while (shortBuffer.hasRemaining()) {
				short valL = shortBuffer.get();
				short valR = shortBuffer.get();
				javaSound.buffer().putShort(valL);
				if (!javaSound.buffer().putShort(valR).hasRemaining()) {
					javaSound.write();
					javaSound.buffer().clear();
				}
				if (whatsSidEnabled) {
					if (whatsSidBuffer.output(valL, valR)) {
						matchTune(whatsSidSection);
					}
				}
			}
		}

		private void matchTune(IWhatsSidSection whatsSidSection) {
			// We need the state of the emulation time, therefore here
			final byte[] whatsSidSamples = whatsSidBuffer.getWhatsSidBufferSamples();
			if (whatsSidMatcherThread == null || !whatsSidMatcherThread.isAlive()) {
				whatsSidMatcherThread = new Thread(() -> {
					try {
						if (whatsSidSamples.length > 0) {
							WavBean wavBean = new WavBean(whatsSidSamples);
							MusicInfoWithConfidenceBean result = fingerPrintMatcher.match(wavBean);
							if (result != null && !result.equals(lastWhatsSidMatch) && result
									.getRelativeConfidence() > whatsSidSection.getMinimumRelativeConfidence()) {
								lastWhatsSidMatch = result;
								Platform.runLater(() -> {
									System.out.println("WhatsSid? " + result);
									Toast.makeText(getStage(), result.toString(), 5000, 500, 500);
								});
							}
						}
					} catch (Exception e) {
						// server not available? silently ignore!
					}
				});
				whatsSidMatcherThread.setPriority(Thread.MIN_PRIORITY);
				whatsSidMatcherThread.start();
			}
		}

		@Override
		protected void close() {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			stopStreaming(emulationSection, SOCKET_CMD_AUDIOSTREAM_OFF);
			javaSound.close();
			if (serverSocket != null) {
				serverSocket.close();
			}
			audioStreaming.setSelected(false);
			whatsSidMatcherThread = null;
		}
	};

	private StreamingPlayer videoPlayer = new StreamingPlayer() {
		private DatagramSocket serverSocket;
		private WritableImage image;
		private boolean frameStart;

		@Override
		protected void open() throws IOException, LineUnavailableException {
			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			palEmulation = new PALEmulation(VICChipModel.MOS6569R3);
			palEmulation.setPalEmulationEnable(enablePalEmulation.isSelected());
			Palette palette = palEmulation.getPalette();
			palette.setBrightness(sidplay2Section.getBrightness());
			palette.setContrast(sidplay2Section.getContrast());
			palette.setGamma(sidplay2Section.getGamma());
			palette.setSaturation(sidplay2Section.getSaturation());
			palette.setPhaseShift(sidplay2Section.getPhaseShift());
			palette.setOffset(sidplay2Section.getOffset());
			palette.setTint(sidplay2Section.getTint());
			palette.setLuminanceC(sidplay2Section.getBlur());
			palette.setDotCreep(sidplay2Section.getBleed());
			palEmulation.updatePalette();

			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingVideoPort());
			serverSocket.setSoTimeout(SOCKET_CONNECT_TIMEOUT);
			image = new WritableImage(SCREEN_WIDTH, SCREEN_HEIGHT);
			startStreaming(emulationSection, SOCKET_CMD_VICSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
					+ ":" + emulationSection.getUltimate64StreamingVideoPort(), 0);
			frameStart = false;
		}

		@Override
		protected void play() throws IOException, InterruptedException {
			byte[] receiveData = new byte[780];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			serverSocket.receive(receivePacket);
//			int sequenceNo = ((receivePacket.getData()[1] & 0xff) << 8) | (receivePacket.getData()[0] & 0xff);
//			int frameNo = ((receivePacket.getData()[3] & 0xff) << 8) | (receivePacket.getData()[2] & 0xff);
			int lineNo = ((receivePacket.getData()[5] & 0xff) << 8) | (receivePacket.getData()[4] & 0xff);
			boolean isLastPacketOfFrame = (lineNo & (1 << 15)) != 0;
			lineNo &= lineNo & ~(1 << 15);
			int pixelsPerLine = ((receivePacket.getData()[7] & 0xff) << 8) | (receivePacket.getData()[6] & 0xff);
			int linesPerPacket = receivePacket.getData()[8] & 0xff;
			int bitsPerPixel = receivePacket.getData()[9] & 0xff;
			int encodingType = ((receivePacket.getData()[11] & 0xff) << 8) | (receivePacket.getData()[10] & 0xff);
			byte[] pixelData = new byte[linesPerPacket * pixelsPerLine * bitsPerPixel / 8];
			System.arraycopy(receivePacket.getData(), 12, pixelData, 0, pixelData.length);
			assert (pixelsPerLine == SCREEN_WIDTH);
			assert (linesPerPacket == 4);
			assert (bitsPerPixel == 4);
			assert (encodingType == 0); // or later 1 for RLE?

			if (frameStart) {
				IntBuffer pixels = IntBuffer.allocate(pixelsPerLine << 2 /* linesPerPacket */);

				int graphicsDataBuffer = 0;
				int pixelDataOffset = 0;
				for (int y = 0; y < linesPerPacket; y++) {
					int rasterY = lineNo + y;
					palEmulation.determineCurrentPalette(rasterY, rasterY == 0);

					for (int x = 0; x < pixelsPerLine; x++) {
						graphicsDataBuffer <<= 4;
						graphicsDataBuffer |= (pixelData[pixelDataOffset + x >> 1] >> ((x & 1) << 2)) & 0xf;
						if (((x + 1) & 0x7) == 0) {
							palEmulation.drawPixels(graphicsDataBuffer, color -> pixels.put(color));
						}
					}
					pixelDataOffset += pixelsPerLine;
				}
				image.getPixelWriter().setPixels(0, lineNo, pixelsPerLine, linesPerPacket,
						PixelFormat.getIntArgbInstance(), pixels.array(), 0, pixelsPerLine);
				if (isLastPacketOfFrame) {
					imageQueue.add(image);
				}
			}
			if (isLastPacketOfFrame) {
				frameStart = true;
			}
		}

		@Override
		protected void close() {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			imageQueue.clear();
			stopStreaming(emulationSection, SOCKET_CMD_VICSTREAM_OFF);
			if (serverSocket != null) {
				serverSocket.close();
			}
			videoStreaming.setSelected(false);
		}

	};

	@FXML
	private Canvas screen;

	@FXML
	private ToggleButton audioStreaming, videoStreaming;

	@FXML
	private ComboBox<Integer> audioBufferSize;

	@FXML
	private CheckBox enablePalEmulation;

	private boolean whatsSidEnabled;
	private WhatsSidBuffer whatsSidBuffer;
	private IFingerprintMatcher fingerPrintMatcher;
	private static MusicInfoWithConfidenceBean lastWhatsSidMatch;

	private PALEmulation palEmulation;

	private ImageQueue imageQueue;

	private PauseTransition pauseTransition;
	private SequentialTransition sequentialTransition;

	public Ultimate64Window() {
		super();
	}

	public Ultimate64Window(Player player) {
		super(player);
	}

	@FXML
	protected void initialize() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		// TODO configure values
		audioBufferSize.setValue(2048);
		enablePalEmulation.setSelected(true);

		pauseTransition = new PauseTransition();
		sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
			Image image = imageQueue.poll();
			if (image != null) {
				screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
				screen.getGraphicsContext2D().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, 0,
						screen.getWidth(), screen.getHeight());
			}
		});
		sequentialTransition.setCycleCount(Timeline.INDEFINITE);

		imageQueue = new ImageQueue();

		SidTune tune = util.getPlayer().getTune();
		setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune));

		sequentialTransition.playFromStart();
	}

	@FXML
	private void enableDisableAudioStreaming() {
		if (audioStreaming.isSelected()) {
			audioPlayer.start();
		} else {
			audioPlayer.stop();
		}
	}

	@FXML
	private void enableDisableVideoStreaming() {
		if (videoStreaming.isSelected()) {
			videoPlayer.start();
		} else {
			videoPlayer.stop();
		}
	}

	@FXML
	private void setAudioBufferSize() {
		if (audioStreaming.isSelected()) {
			audioPlayer.stop();
			audioPlayer.start();
			audioStreaming.setSelected(true);
		}
	}

	@FXML
	private void setEnablePalEmulation() {
		palEmulation.setPalEmulationEnable(enablePalEmulation.isSelected());
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen(final CPUClock cpuClock) {
		pauseTransition.setDuration(Duration.millis(1000. / cpuClock.getScreenRefresh()));

		screen.setWidth(SCREEN_WIDTH);
		screen.setHeight(SCREEN_HEIGHT);
		screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
	}

	@Override
	public void doClose() {
		audioPlayer.stop();
		videoPlayer.stop();
	}
}
