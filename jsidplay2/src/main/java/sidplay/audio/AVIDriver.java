package sidplay.audio;

import static java.lang.Boolean.TRUE;
import static java.lang.Short.BYTES;
import static java.lang.Short.SIZE;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.components.mos656x.VIC.MAX_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_WIDTH;
import static org.monte.media.AudioFormatKeys.ByteOrderKey;
import static org.monte.media.AudioFormatKeys.ChannelsKey;
import static org.monte.media.AudioFormatKeys.ENCODING_PCM_SIGNED;
import static org.monte.media.AudioFormatKeys.SampleRateKey;
import static org.monte.media.AudioFormatKeys.SampleSizeInBitsKey;
import static org.monte.media.AudioFormatKeys.SignedKey;
import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.KeyFrameIntervalKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.FormatKeys.MediaType.AUDIO;
import static org.monte.media.FormatKeys.MediaType.VIDEO;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.InterlaceKey;
import static org.monte.media.VideoFormatKeys.PixelAspectRatioKey;
import static org.monte.media.VideoFormatKeys.QualityKey;
import static org.monte.media.VideoFormatKeys.WidthKey;
import static org.monte.media.math.Rational.valueOf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;

import org.monte.media.Format;
import org.monte.media.FormatFormatter;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;

public class AVIDriver implements AudioDriver, VideoDriver {

	private AVIWriter aviWriter;
	private int videoTrack, audioTrack;

	private BufferedImage videoImage;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);
		aviWriter = new AVIWriter(new File(recordingFilename));

		Format videoFormat = new Format(MediaTypeKey, VIDEO, EncodingKey, ENCODING_AVI_MJPG, WidthKey, MAX_WIDTH,
				HeightKey, MAX_HEIGHT, DepthKey, 3/* RGB */ << 3, InterlaceKey, TRUE, QualityKey,
				audioSection.getAviCompressionQuality(), FrameRateKey, valueOf(cpuClock.getScreenRefresh()),
				KeyFrameIntervalKey, (int) (cpuClock.getScreenRefresh() * 60), PixelAspectRatioKey,
				pixelAspectRatio(cpuClock));
		System.out.println(FormatFormatter.toString(videoFormat));
		videoTrack = aviWriter.addTrack(videoFormat);

		Format audioFormat = new Format(MediaTypeKey, AUDIO, EncodingKey, ENCODING_PCM_SIGNED, SampleSizeInBitsKey,
				SIZE, ByteOrderKey, LITTLE_ENDIAN, SignedKey, TRUE, ChannelsKey, cfg.getChannels(), SampleRateKey,
				valueOf(cfg.getFrameRate()));
		System.out.println(FormatFormatter.toString(audioFormat));
		audioTrack = aviWriter.addTrack(audioFormat);

		videoImage = new BufferedImage(MAX_WIDTH, MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
		aviWriter.setPalette(videoTrack, videoImage.getColorModel());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void accept(VIC vic) {
		try {
			if (aviWriter.isDataLimitReached()) {
				throw new IOException("AVI file size limit reached!");
			}
			videoImage.setRGB(0, 0, MAX_WIDTH, MAX_HEIGHT, vic.getPixels().array(), 0, MAX_WIDTH);
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

	private Rational pixelAspectRatio(CPUClock cpuClock) {
		// http://hitmen.c02.at/temp/palstuff/
		return cpuClock == CPUClock.PAL ? Rational.valueOf(1 / 0.936) : Rational.valueOf(1 / 0.750);
	}

}
