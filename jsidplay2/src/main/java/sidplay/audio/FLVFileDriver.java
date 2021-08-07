package sidplay.audio;

import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
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
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;

/**
 * Allows FLD file write and as an alternative creating a real-time video stream
 * via RTMP protocol e.g. "rtmp://localhost/live/test" in conjunction with nginx
 * server with installed RTMP module.
 * 
 * Follow instructions here to setup a RTMP enabled web-server:
 * https://programmer.ink/think/5e368f92922ac.html
 * 
 * <pre>
 * Uncomment // recordingFilename = "rtmp://localhost/live/test"
 * and start:
 * sudo /usr/local/nginx/sbin/nginx
 * </pre>
 * 
 * @author ken
 *
 */
public class FLVFileDriver implements AudioDriver, VideoDriver {

	private EventScheduler context;
	private AudioConfig cfg;

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private IRational videoFrameRate, audioFrameRate;

	private int frameNo;
	private long firstTimeStamp, firstTimeStamp2;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.cfg = new AudioConfig(audioSection);
		this.context = context;

		// Produce live video stream
//		recordingFilename = "rtmp://localhost/live/test";
		// or Produce file
		new File(recordingFilename).delete();
		System.out.println("Recording, file=" + recordingFilename);

		container = IContainer.make();
		IContainerFormat containerFormat_live = IContainerFormat.make();
		containerFormat_live.setOutputFormat("flv", recordingFilename, null);
		container.setInputBufferLength(0);
		int retVal = container.open(recordingFilename, IContainer.Type.WRITE, containerFormat_live);
		if (retVal < 0) {
			throw new IOException("Could not open output container for live stream");
		}
		videoFrameRate = IRational.make((int) cpuClock.getScreenRefresh(), 1);
		audioFrameRate = IRational.make(cfg.getFrameRate(), 1);

		IStream stream = container.addNewStream(ICodec.ID.CODEC_ID_H264);
		videoCoder = stream.getStreamCoder();
		videoCoder.setNumPicturesInGroupOfPictures(12);
		videoCoder.setBitRate(250000);
		videoCoder.setBitRateTolerance(10000);
		videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
		videoCoder.setHeight(VIC.MAX_HEIGHT);
		videoCoder.setWidth(VIC.MAX_WIDTH);
		videoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
		videoCoder.setGlobalQuality(0);
		videoCoder.setFrameRate(videoFrameRate);
		videoCoder.setTimeBase(IRational.make(videoFrameRate.getDenominator(), videoFrameRate.getNumerator()));
		presets("libx264-normal.ffpreset");
//		presets("libx264-hq.ffpreset");
		videoCoder.setAutomaticallyStampPacketsForStream(true);
		videoCoder.open(null, null);

		IStream audioStream = container.addNewStream(ICodec.ID.CODEC_ID_MP3);
		audioCoder = audioStream.getStreamCoder();
		audioCoder.setChannels(cfg.getChannels());
		audioCoder.setSampleFormat(IAudioSamples.Format.FMT_S16);
		audioCoder.setSampleRate(cfg.getFrameRate());
		audioCoder.open(null, null);

		container.writeHeader();

		frameNo = 0;
		firstTimeStamp = 0;
		firstTimeStamp2 = 0;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		int numSamples = sampleBuffer.position() / 4;

		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp2 == 0) {
			firstTimeStamp2 = context.getTime(Phase.PHI2);
		}
		long timeStamp = (now - firstTimeStamp2); // convert to microseconds

		IPacket packet = IPacket.make();
		IAudioSamples samples = IAudioSamples.make(numSamples, cfg.getChannels(), IAudioSamples.Format.FMT_S16);
		((Buffer) sampleBuffer).flip();
		samples.getData().put(sampleBuffer.array(), 0, 0, sampleBuffer.remaining());
		samples.setTimeBase(audioFrameRate);
		samples.setTimeStamp(timeStamp);
		samples.setComplete(true, numSamples, cfg.getFrameRate(), cfg.getChannels(), IAudioSamples.Format.FMT_S16, 0);

		int samplesConsumed = 0;
		while (samplesConsumed < samples.getNumSamples()) {
			int retval = audioCoder.encodeAudio(packet, samples, samplesConsumed);
			if (retval < 0) {
				throw new InterruptedException("変換失敗");
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
			firstTimeStamp = context.getTime(Phase.PHI2);
		}
		long timeStamp = (now - firstTimeStamp); // convert to microseconds

		BufferedImage image = new BufferedImage(VIC.MAX_WIDTH, VIC.MAX_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		to3ByteGBR(vic.getPixels(), image.getRaster());

		IPacket packet = IPacket.make();
		IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		IVideoPicture outFrame = converter.toPicture(image, timeStamp);
		if (frameNo == 0) {
			// make first frame keyframe
			outFrame.setKeyFrame(true);
		}
		outFrame.setQuality(0);
		videoCoder.encodeVideo(packet, outFrame, 0);
		outFrame.delete();
		if (packet.isComplete()) {
			container.writePacket(packet);
		}
		container.flushPackets();
	}

	@Override
	public void close() {
		container.writeTrailer();
		audioCoder.close();
		videoCoder.close();
		container.close();
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

	private void presets(String presetName) {
		Properties props = new Properties();
		InputStream is = FLVFileDriver.class.getResourceAsStream(presetName);
		try {
			props.load(is);
		} catch (IOException e) {
			System.err.println(
					"You need the libx264-normal.ffpreset file from the Xuggle distribution in your classpath.");
			System.exit(1);
		}
		Configuration.configure(props, videoCoder);
	}

	private void to3ByteGBR(IntBuffer pixels, WritableRaster writableRaster) {
		byte[] src = new byte[MAX_WIDTH * MAX_HEIGHT * 3];

		((Buffer) pixels).clear();
		ByteBuffer pictureBuffer = ByteBuffer.wrap(src);
		while (pixels.hasRemaining()) {
			int pixel = pixels.get();
			// ignore ALPHA channel (ARGB channel order)
			pictureBuffer.put((byte) ((pixel & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 8 & 0xff)));
			pictureBuffer.put((byte) ((pixel >> 16 & 0xff)));
		}
		System.arraycopy(src, 0, ((DataBufferByte) writableRaster.getDataBuffer()).getData(), 0, src.length);
	}

}
