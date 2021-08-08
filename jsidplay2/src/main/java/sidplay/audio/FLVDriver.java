package sidplay.audio;

import static com.xuggle.xuggler.IAudioSamples.Format.FMT_S16;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_MP3;
import static com.xuggle.xuggler.IContainer.Type.WRITE;
import static com.xuggle.xuggler.IPixelFormat.Type.YUV420P;
import static com.xuggle.xuggler.IStreamCoder.Flags.FLAG_QSCALE;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.common.SamplingRate.HIGH;
import static libsidplay.common.SamplingRate.LOW;
import static libsidplay.common.SamplingRate.MEDIUM;
import static libsidplay.common.SamplingRate.VERY_LOW;
import static libsidplay.components.mos656x.VIC.MAX_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_WIDTH;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.exceptions.IniConfigException;

/**
 * Allows FLV file write and as an alternative creating a real-time video stream
 * via RTMP protocol e.g. "rtmp://localhost/live/test" in conjunction with nginx
 * server with installed RTMP module.
 * 
 * Follow instructions here to setup a RTMP enabled web-server:
 * https://programmer.ink/think/5e368f92922ac.html
 * 
 * Supported formats:
 * https://stackoverflow.com/questions/9727590/what-codecs-does-xuggler-support
 * 
 * @author ken
 *
 */
public abstract class FLVDriver implements AudioDriver, VideoDriver {

	/**
	 * File based driver to create a FLV file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLVFileDriver extends FLVDriver {

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	/**
	 * Driver to write into an FLV output stream.<BR>
	 *
	 * E.g "rtmp://localhost/live/test" <B>Note:</B> RTMP enabled web-server must be
	 * running (e.g. nging + rtmp module)
	 *
	 *
	 * <B>Note:</B> RTMP enabled web-server must be started beforehand (e.g. sudo
	 * /usr/local/nginx/sbin/nginx)
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLVStreamDriver extends FLVDriver {

		private String recordingFilename;

		public FLVStreamDriver(String rtmpUrl) {
			this.recordingFilename = rtmpUrl;
		}

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			// Note: a local recording file name is overridden by RTMP URL
			return this.recordingFilename;
		}

	}

	private CPUClock cpuClock;
	private EventScheduler context;
	private AudioConfig cfg;

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private int frameNo;
	private long firstTimeStamp;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.cpuClock = cpuClock;
		this.context = context;
		this.cfg = new AudioConfig(audioSection);

		recordingFilename = getRecordingFilename(recordingFilename);
		File recordingFile = new File(recordingFilename);
		if (recordingFile.isFile() && recordingFile.exists()) {
			recordingFile.delete();
		}
		if (audioSection.getSamplingRate() == VERY_LOW || audioSection.getSamplingRate() == MEDIUM
				|| audioSection.getSamplingRate() == HIGH) {
			throw new IniConfigException("Sampling rate is not supported by FLV encoder, use default",
					() -> audioSection.setSamplingRate(LOW));
		}
		container = IContainer.make();
		IContainerFormat containerFormat = IContainerFormat.make();
		containerFormat.setOutputFormat("flv", recordingFilename, null);
		container.setInputBufferLength(0);
		if (container.open(recordingFilename, WRITE, containerFormat) < 0) {
			throw new IOException("Could not open output container for live stream");
		}
		IRational videoFrameRate = IRational.make((int) cpuClock.getCpuFrequency(), cpuClock.getCyclesPerFrame());

		IStream stream = container.addNewStream(CODEC_ID_H264);
		videoCoder = stream.getStreamCoder();
		videoCoder.setNumPicturesInGroupOfPictures(12);
		videoCoder.setBitRate(250000);
		videoCoder.setBitRateTolerance(10000);
		videoCoder.setFrameRate(videoFrameRate);
		videoCoder.setTimeBase(IRational.make(videoFrameRate.getDenominator(), videoFrameRate.getNumerator()));
		videoCoder.setPixelType(YUV420P);
		videoCoder.setHeight(MAX_HEIGHT);
		videoCoder.setWidth(MAX_WIDTH);
		videoCoder.setFlag(FLAG_QSCALE, true);
		videoCoder.setGlobalQuality(0);
//		configurePresets("libx264-normal.ffpreset");
		configurePresets("libx264-hq.ffpreset");
		videoCoder.open(null, null);

		IStream audioStream = container.addNewStream(CODEC_ID_MP3);
		audioCoder = audioStream.getStreamCoder();
		audioCoder.setChannels(cfg.getChannels());
		audioCoder.setSampleFormat(FMT_S16);
		audioCoder.setBitRate(128000);
		audioCoder.setBitRateTolerance(audioCoder.getBitRate() / 2);
		audioCoder.setSampleRate(cfg.getFrameRate());
		audioCoder.setTimeBase(IRational.make(1, cfg.getFrameRate()));
		audioCoder.setGlobalQuality(0);
		audioCoder.open(null, null);

		container.writeHeader();

		frameNo = 0;
		firstTimeStamp = 0;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp == 0) {
			firstTimeStamp = now;
		}
		long timeStamp = now - firstTimeStamp;

		IPacket packet = IPacket.make();
		int numSamples = sampleBuffer.position() >> 2;
		IAudioSamples samples = IAudioSamples.make(numSamples, cfg.getChannels(), FMT_S16);
		samples.getData().put(sampleBuffer.array(), 0, 0, sampleBuffer.position());
		samples.setTimeBase(IRational.make(1, (int) cpuClock.getCpuFrequency()));
		samples.setTimeStamp(timeStamp);
		samples.setComplete(true, numSamples, cfg.getFrameRate(), cfg.getChannels(), FMT_S16, 0);

		int samplesConsumed = 0;
		while (samplesConsumed < samples.getNumSamples()) {
			int retval = audioCoder.encodeAudio(packet, samples, samplesConsumed);
			if (retval < 0) {
				throw new RuntimeException("Error writing audio stream");
			}
			samplesConsumed += retval;
			if (packet.isComplete()) {
				container.writePacket(packet);
			}
		}
	}

	@Override
	public void accept(VIC vic) {
		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp == 0) {
			firstTimeStamp = now;
		}
		long timeStamp = now - firstTimeStamp;

		IPacket packet = IPacket.make();
		BufferedImage image = new BufferedImage(MAX_WIDTH, MAX_HEIGHT, TYPE_3BYTE_BGR);
		to3ByteGBR(vic.getPixels(), image.getRaster());
		IVideoPicture outFrame = ConverterFactory.createConverter(image, YUV420P).toPicture(image, timeStamp);
		outFrame.setTimeBase(IRational.make(1, (int) cpuClock.getCpuFrequency()));
		outFrame.setKeyFrame(frameNo++ == 0);
		outFrame.setQuality(0);

		if (videoCoder.encodeVideo(packet, outFrame, 0) < 0) {
			throw new RuntimeException("Error writing video stream");
		}
		if (packet.isComplete()) {
			if (container.writePacket(packet) < 0) {
				throw new RuntimeException("Could not write packet!");
			}
		}
		outFrame.delete();
	}

	@Override
	public void close() {
		if (container != null) {
			container.flushPackets();
			container.writeTrailer();
			if (audioCoder != null) {
				audioCoder.close();
			}
			if (videoCoder != null) {
				videoCoder.close();
			}
			container.close();
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".flv";
	}

	private void configurePresets(String presetName) {
		Properties props = new Properties();
		try (InputStream is = FLVDriver.class.getResourceAsStream(presetName)) {
			props.load(is);
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("You need the " + presetName + " in your classpath.");
		}
		Configuration.configure(props, videoCoder);
	}

	private void to3ByteGBR(IntBuffer pixels, WritableRaster writableRaster) {
		((Buffer) pixels).clear();
		ByteBuffer pictureBuffer = ByteBuffer.wrap(((DataBufferByte) writableRaster.getDataBuffer()).getData());
		while (pixels.hasRemaining()) {
			int pixel = pixels.get();
			// ignore ALPHA channel (ARGB channel order)
			pictureBuffer.put((byte) ((pixel & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 8 & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 16 & 0xff)));
		}
	}

	protected abstract String getRecordingFilename(String recordingFilename);
}
