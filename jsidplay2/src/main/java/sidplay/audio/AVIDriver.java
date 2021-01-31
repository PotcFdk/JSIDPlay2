package sidplay.audio;

import static org.monte.media.AudioFormatKeys.ChannelsKey;
import static org.monte.media.AudioFormatKeys.ENCODING_PCM_SIGNED;
import static org.monte.media.AudioFormatKeys.SampleRateKey;
import static org.monte.media.AudioFormatKeys.SampleSizeInBitsKey;
import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.QualityKey;
import static org.monte.media.VideoFormatKeys.WidthKey;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;

public class AVIDriver implements AudioDriver, VideoDriver {

	private float aviVideoQuality = 0.97f;

	private AVIWriter aviWriter;
	private int videoTrack, audioTrack;

	private BufferedImage videoImage;
	private ByteBuffer sampleBuffer;

	@Override
	public void configure(IAudioSection audioSection, EventScheduler context) {
		aviVideoQuality = audioSection.getAviCompressionQuality();
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		aviWriter = new AVIWriter(new File(recordingFilename));

		videoTrack = aviWriter.addTrack(
				new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey, 24, QualityKey, aviVideoQuality, MediaTypeKey,
						MediaType.VIDEO, FrameRateKey, new Rational((long) cpuClock.getScreenRefresh(), 1), WidthKey,
						VIC.MAX_WIDTH, HeightKey, VIC.MAX_HEIGHT));

		audioTrack = aviWriter.addTrack(new Format(SampleRateKey, new Rational(cfg.getFrameRate(), 1), ChannelsKey,
				cfg.getChannels(), SampleSizeInBitsKey, Short.SIZE, EncodingKey, ENCODING_PCM_SIGNED));

		videoImage = new BufferedImage(VIC.MAX_WIDTH, VIC.MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
		aviWriter.setPalette(videoTrack, videoImage.getColorModel());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void accept(VIC vic) {
		try {
			if (aviWriter.isDataLimitReached()) {
				throw new IOException("AVI file size limit reached!");
			}
			videoImage.setRGB(0, 0, VIC.MAX_WIDTH, VIC.MAX_HEIGHT, vic.getPixels().array(), 0, VIC.MAX_WIDTH);
			aviWriter.write(videoTrack, videoImage, 1);
		} catch (IOException e) {
			throw new RuntimeException("Error writing AVI video stream", e);
		}
	}

	@Override
	public void write() throws InterruptedException {
		try {
			if (aviWriter.isDataLimitReached()) {
				throw new IOException("AVI file size limit reached!");
			}
			aviWriter.writeSamples(audioTrack, sampleBuffer.position() >> 2/* / (Short.BYTES * cfg.getChannels()) */,
					sampleBuffer.array(), 0, sampleBuffer.position(), true);
		} catch (IOException e) {
			throw new RuntimeException("Error writing AVI video stream", e);
		}
	}

	@Override
	public void close() {
		try {
			if (aviWriter != null) {
				aviWriter.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error closing AVI", e);
		}
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".avi";
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
