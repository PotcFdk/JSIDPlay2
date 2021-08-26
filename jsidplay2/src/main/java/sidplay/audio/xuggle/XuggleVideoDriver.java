package sidplay.audio.xuggle;

import static com.xuggle.xuggler.IAudioSamples.Format.FMT_S16;
import static com.xuggle.xuggler.IContainer.Type.WRITE;
import static com.xuggle.xuggler.IPixelFormat.Type.YUV420P;
import static com.xuggle.xuggler.IStreamCoder.Flags.FLAG_QSCALE;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.components.mos656x.VIC.MAX_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_WIDTH;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingRate;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;
import sidplay.audio.exceptions.IniConfigException;

/**
 * The Kush Gauge: To find a decent bitrate simply multiply the target pixel
 * count by the frame rate; then multiply the result by a factor of 1, 2 or 4,
 * depending on the amount of motion in the video; and then multiply that result
 * by 0.07 to get the bit rate in bps. <br>
 * 
 * <pre>
 * PAL low motion:		63 * 312 * 50,1246 * 1 * 0,07	=  68.967
 * PAL high motion:		63 * 312 * 50,1246 * 4 * 0,07	= 275.870
 * NTSC LOW motion:		65 * 263 * 59,83   * 1 * 0,07	=  71.596
 * NTSC high motion:	65 * 263 * 59,83   * 4 * 0,07	= 286.382
 * </pre>
 * 
 * E.g. stream video<BR>
 * {@code
 * http://127.0.1.1:8080/jsidplay2service/JSIDPlay2REST/convert/Demos/Instinct+BoozeDesign%20-%20Andropolis/Instinct+BoozeDesign%20-%20Andropolis.d64?defaultLength=06:00&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&startTime=00:60
 * 
 * rtmp://localhost/live/test
 * http://localhost:9080/hls/test.m3u8
 * }
 *
 * @author ken
 *
 */
public abstract class XuggleVideoDriver implements AudioDriver, VideoDriver {

	private EventScheduler context;

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private IConverter converter;
	private BufferedImage vicImage;
	private long frameNo, framesPerKeyFrames, firstTimeStamp;
	private double ticksPerMicrosecond;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);
		recordingFilename = getRecordingFilename(recordingFilename);
		//recordingFilename = "rtmp://localhost/live/test";
		if (!getSupportedSamplingRates().contains(audioSection.getSamplingRate())) {
			throw new IniConfigException("Sampling rate is not supported by encoder, use default",
					() -> audioSection.setSamplingRate(getDefaultSamplingRate()));
		}
		container = IContainer.make();
		IContainerFormat containerFormat = IContainerFormat.make();
		containerFormat.setOutputFormat(getOutputFormatName(), recordingFilename, null);
		container.setInputBufferLength(0);
		if (container.open(recordingFilename, WRITE, containerFormat) < 0) {
			throw new IOException("Could not open output container");
		}
		IStream stream = container.addNewStream(getVideoCodec());
		videoCoder = stream.getStreamCoder();
		videoCoder.setNumPicturesInGroupOfPictures(audioSection.getVideoCoderNumPicturesInGroupOfPictures());
		videoCoder.setBitRate(audioSection.getVideoCoderBitRate());
		videoCoder.setBitRateTolerance(audioSection.getVideoCoderBitRateTolerance());
		videoCoder.setTimeBase(IRational.make(1 / cpuClock.getScreenRefresh()));
		videoCoder.setPixelType(YUV420P);
		videoCoder.setHeight(MAX_HEIGHT);
		videoCoder.setWidth(MAX_WIDTH);
		videoCoder.setFlag(FLAG_QSCALE, true);
		videoCoder.setGlobalQuality(audioSection.getVideoCoderGlobalQuality());
		configurePresets(audioSection.getVideoCoderPreset().getPresetName());
		videoCoder.open(null, null);

		vicImage = new BufferedImage(MAX_WIDTH, MAX_HEIGHT, TYPE_3BYTE_BGR);
		converter = ConverterFactory.createConverter(vicImage, YUV420P);

		IStream audioStream = container.addNewStream(getAudioCodec());
		audioCoder = audioStream.getStreamCoder();
		audioCoder.setChannels(cfg.getChannels());
		audioCoder.setSampleFormat(FMT_S16);
		audioCoder.setBitRate(audioSection.getAudioCoderBitRate());
		audioCoder.setBitRateTolerance(audioSection.getAudioCoderBitRateTolerance());
		audioCoder.setSampleRate(cfg.getFrameRate());
		audioCoder.open(null, null);

		container.writeHeader();

		frameNo = 0;
		ticksPerMicrosecond = cpuClock.getCpuFrequency() / 1000000;
		framesPerKeyFrames = (int) cpuClock.getScreenRefresh();
		firstTimeStamp = 0;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		long timeStamp = getTimeStamp();

		int numSamples = sampleBuffer.position() >> 2;
		IAudioSamples audioSamples = IAudioSamples.make(numSamples, audioCoder.getChannels(), FMT_S16);
		audioSamples.getData().put(sampleBuffer.array(), 0, 0, sampleBuffer.position());
		audioSamples.setComplete(true, numSamples, audioCoder.getSampleRate(), audioCoder.getChannels(), FMT_S16,
				timeStamp);

		int samplesConsumed = 0;
		IPacket packet = IPacket.make();
		while (samplesConsumed < audioSamples.getNumSamples()) {
			int retval = audioCoder.encodeAudio(packet, audioSamples, samplesConsumed);
			if (retval < 0) {
				throw new RuntimeException("Error writing audio stream");
			}
			samplesConsumed += retval;
			if (packet.isComplete()) {
				if (container.writePacket(packet) < 0) {
					throw new RuntimeException("Could not write audio packet!");
				}
			}
		}
		audioSamples.delete();
		packet.delete();
	}

	@Override
	public void accept(VIC vic) {
		long timeStamp = getTimeStamp();

		to3ByteGBR(vic.getPixels());

		IVideoPicture videoPicture = converter.toPicture(vicImage, timeStamp);
		videoPicture.setKeyFrame((frameNo++ % framesPerKeyFrames) == 0);

		IPacket packet = IPacket.make();
		if (videoCoder.encodeVideo(packet, videoPicture, 0) < 0) {
			throw new RuntimeException("Error writing video stream");
		}
		if (packet.isComplete()) {
			if (container.writePacket(packet) < 0) {
				throw new RuntimeException("Could not write video packet!");
			}
		}
		videoPicture.delete();
		packet.delete();
	}

	@Override
	public void close() {
		if (audioCoder != null) {
			IPacket audioPacket = IPacket.make();
			// flush any data it was keeping a hold of
			if (audioCoder.encodeAudio(audioPacket, null, 0) < 0) {
				throw new RuntimeException("Error writing audio stream");
			}
			audioPacket.delete();
		}
		if (videoCoder != null) {
			IPacket videoPacket = IPacket.make();
			// flush any data it was keeping a hold of
			if (videoCoder.encodeVideo(videoPacket, null, 0) < 0) {
				throw new RuntimeException("Error writing video stream");
			}
			videoPacket.delete();
		}
		if (container != null) {
			container.flushPackets();
			container.writeTrailer();
			if (audioCoder != null) {
				audioCoder.close();
				audioCoder = null;
			}
			if (videoCoder != null) {
				videoCoder.close();
				videoCoder = null;
			}
			container.close();
			container = null;
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

	private void configurePresets(String presetName) {
		Properties props = new Properties();
		try (InputStream is = XuggleVideoDriver.class.getResourceAsStream(presetName)) {
			props.load(is);
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("You need the " + presetName + " in your classpath.");
		}
		Configuration.configure(props, videoCoder);
	}

	private long getTimeStamp() {
		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp == 0) {
			firstTimeStamp = now;
		}
		return (long) ((now - firstTimeStamp) / ticksPerMicrosecond);
	}

	private void to3ByteGBR(IntBuffer pixels) {
		ByteBuffer pictureBuffer = ByteBuffer.wrap(((DataBufferByte) vicImage.getRaster().getDataBuffer()).getData());
		((Buffer) pixels).clear();
		while (pixels.hasRemaining()) {
			int pixel = pixels.get();
			// ignore ALPHA channel (ARGB channel order)
			pictureBuffer.put((byte) ((pixel & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 8 & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 16 & 0xff)));
		}
	}

	protected abstract String getOutputFormatName();

	protected abstract List<SamplingRate> getSupportedSamplingRates();

	protected abstract SamplingRate getDefaultSamplingRate();

	protected abstract ID getVideoCodec();

	protected abstract ID getAudioCodec();

	protected abstract String getRecordingFilename(String recordingFilename);
}
