package ui.ultimate64;

import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_ON;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_ON;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.IntBuffer;

import javax.sound.sampled.LineUnavailableException;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import libsidplay.Ultimate64;
import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.components.mos656x.Palette;
import libsidplay.components.mos656x.VIC.Model;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.ini.IniDefaults;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.entities.config.EmulationSection;

public class Ultimate64Window extends C64Window implements Ultimate64 {
	private static final int FRAME_RATE = 48000;
	private static final int CHANNELS = 2;
	private static final int AUDIO_BUFFER_SIZE = 192;

	private static final int SCREEN_HEIGHT = 272;
	private static final int SCREEN_WIDTH = 384;
	private static int[] VIC_PALETTE = new int[] { 0xff000000, 0xffffffff, 0xff880000, 0xffaaffee, 0xffcc44cc,
			0xff00cc55, 0xff0000aa, 0xffeeee77, 0xffdd8855, 0xff664400, 0xffff7777, 0xff333333, 0xff777777, 0xffaaff66,
			0xff0088ff, 0xffbbbbbb };

	private StreamingPlayer audioPlayer = new StreamingPlayer() {
		private DatagramSocket serverSocket;
		private JavaSound javaSound = new JavaSound();

		@Override
		protected void open() throws IOException, LineUnavailableException {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			javaSound.open(new AudioConfig(FRAME_RATE, CHANNELS, -1, audioBufferSize.getValue()), null);
			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingAudioPort());
			serverSocket.setSoTimeout(3000);
			startStreaming(util.getConfig(), SOCKET_CMD_AUDIOSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
					+ ":" + emulationSection.getUltimate64StreamingAudioPort(), 0);
		}

		@Override
		protected void play() throws IOException, InterruptedException {
			byte[] receiveData = new byte[2
					/* header */ + AUDIO_BUFFER_SIZE * 2/* channels */ * 16 / 8/* bits, signed, LE */];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
//			int sequenceNo = ((receivePacket.getData()[1] & 0xff) << 8) | (receivePacket.getData()[0] & 0xff);
			byte[] audioData = new byte[768];
			/* left ch, right ch (16 bits each) */
			System.arraycopy(receivePacket.getData(), 2, audioData, 0, audioData.length);
			for (byte b : audioData) {
				if (!javaSound.buffer().put(b).hasRemaining()) {
					javaSound.write();
					javaSound.buffer().clear();
				}
			}
		}

		@Override
		protected void close() {
			stopStreaming(util.getConfig(), SOCKET_CMD_AUDIOSTREAM_OFF);
			javaSound.close();
			if (serverSocket != null) {
				serverSocket.close();
			}
			audioStreaming.setSelected(false);
		}
	};

	private StreamingPlayer videoPlayer = new StreamingPlayer() {
		private DatagramSocket serverSocket;
		private WritableImage image;
		private PALEmulation palEmulation;
		private boolean frameStart;

		@Override
		protected void open() throws IOException, LineUnavailableException {
			palEmulation = new PALEmulation(Model.MOS6569R3);
			Palette palette = palEmulation.getPalette();
			palette.setBrightness(IniDefaults.DEFAULT_BRIGHTNESS);
			palette.setContrast(IniDefaults.DEFAULT_CONTRAST);
			palette.setGamma(IniDefaults.DEFAULT_GAMMA);
			palette.setSaturation(IniDefaults.DEFAULT_SATURATION);
			palette.setPhaseShift(IniDefaults.DEFAULT_PHASE_SHIFT);
			palette.setOffset(IniDefaults.DEFAULT_OFFSET);
			palette.setTint(IniDefaults.DEFAULT_TINT);
			palette.setLuminanceC(IniDefaults.DEFAULT_BLUR);
			palette.setDotCreep(IniDefaults.DEFAULT_BLEED);
			palEmulation.updatePalette();

			EmulationSection emulationSection = util.getConfig().getEmulationSection();
			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingVideoPort());
			serverSocket.setSoTimeout(3000);
			image = new WritableImage(SCREEN_WIDTH, SCREEN_HEIGHT);
			startStreaming(util.getConfig(), SOCKET_CMD_VICSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
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

//				int graphicsDataBuffer = 0;
//				for (int y = 0; y < linesPerPacket; y++) {
//					int rasterY = lineNo + y;
//					palEmulation.determineCurrentPalette(rasterY, rasterY == 0);
//
//					for (int x = 0; x < pixelsPerLine; x++) {
//						graphicsDataBuffer <<= 4;
//						graphicsDataBuffer |= (pixelData[x >> 1] >> ((x & 1) << 2)) & 0xf;
//						if (((x + 1) & 0x7) == 0) {
//							palEmulation.drawPixels(graphicsDataBuffer, (b, i) -> pixels.put(i));
//						}
//					}
//				}
				for (int x = 0; x < pixelsPerLine << 2/* linesPerPacket */; x++) {
					pixels.put(VIC_PALETTE[(pixelData[x >> 1] >> ((x & 1) << 2)) & 0xf]);
				}
				image.getPixelWriter().setPixels(0, lineNo, pixelsPerLine, linesPerPacket,
						PixelFormat.getIntArgbInstance(), pixels.array(), 0, pixelsPerLine);
				if (isLastPacketOfFrame) {
					imageQueue.add(image);
					image = new WritableImage(SCREEN_WIDTH, SCREEN_HEIGHT);
				}
			}
			if (isLastPacketOfFrame) {
				frameStart = true;
			}
		}

		@Override
		protected void close() {
			imageQueue.clear();
			stopStreaming(util.getConfig(), SOCKET_CMD_VICSTREAM_OFF);
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

		audioBufferSize.setValue(192);

		pauseTransition = new PauseTransition();
		sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
			Image image = imageQueue.poll();
			if (image != null) {
				screen.getGraphicsContext2D().clearRect(0, 0, screen.widthProperty().get(),
						screen.heightProperty().get());
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

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen(final CPUClock cpuClock) {
		pauseTransition.setDuration(Duration.millis(1000. / cpuClock.getScreenRefresh()));

		screen.setWidth(SCREEN_WIDTH);
		screen.setHeight(SCREEN_HEIGHT);
		screen.getGraphicsContext2D().clearRect(0, 0, screen.widthProperty().get(), screen.heightProperty().get());
	}

	@Override
	public void doClose() {
		audioPlayer.stop();
		videoPlayer.stop();
	}
}
