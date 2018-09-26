package sidplay.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import javax.sound.sampled.LineUnavailableException;

import org.jcodec.api.transcode.AudioFrameWithPacket;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.api.transcode.PixelStoreImpl;
import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.VideoFrameWithPacket;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;

public class MP4Driver implements AudioDriver, Consumer<int[]> {

	public class MP4SequenceEncoder {
		private Transform transform;
		private int frameNo;
		private int timestamp;
		private Rational fps;
		private Sink sink;
		private PixelStore pixelStore;

		public MP4SequenceEncoder(SeekableByteChannel out, Rational fps, Format outputFormat, Codec outputVideoCodec,
				Codec outputAudioCodec) throws IOException {
			this.fps = fps;

			this.sink = SinkImpl.createWithStream(out, outputFormat, outputVideoCodec, outputAudioCodec);
			this.sink.init();

			this.transform = ColorUtil.getTransform(ColorSpace.RGB, sink.getInputColor());

			this.pixelStore = new PixelStoreImpl();
		}

		public void encodeNativeFrame(Picture pic) throws IOException {
			ColorSpace sinkColor = sink.getInputColor();
			LoanerPicture toEncode;
			toEncode = pixelStore.getPicture(pic.getWidth(), pic.getHeight(), sinkColor);
			transform.transform(pic, toEncode.getPicture());

			Packet pkt = Packet.createPacket(null, timestamp, fps.getNum(), fps.getDen(), frameNo, FrameType.KEY, null);
			sink.outputVideoFrame(new VideoFrameWithPacket(pkt, toEncode));

			pixelStore.putBack(toEncode);

			timestamp += fps.getDen();
			frameNo++;
		}

		public void encodeAudioFrame(AudioBuffer audio) throws IOException {
			Packet pkt = Packet.createPacket(null, 0, fps.getNum(), -1, 0, FrameType.KEY, null);
			sink.outputAudioFrame(new AudioFrameWithPacket(audio, pkt));
		}

		public void finish() throws IOException {
			sink.finish();
		}
	}

	private FileChannelWrapper channel;
	private MP4SequenceEncoder encoder;
	private byte[][] pictureData;
	private AudioFormat audioFormat;
	private ByteBuffer sampleBuffer;
	private OutputStream out;
	private File audioDataFile;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		final Rational rational = Rational.R((int) cpuClock.getScreenRefresh(), 1);
		System.out.println("Recording, file=" + recordingFilename);
		this.channel = NIOUtils.writableChannel(new File(recordingFilename));
		this.encoder = new MP4SequenceEncoder(this.channel, rational, Format.MOV, Codec.H264, Codec.PCM);

		this.pictureData = new byte[1][3 * VIC.MAX_WIDTH * VIC.MAX_HEIGHT];

		this.audioFormat = getAudioFormat(cfg);
		this.sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);

		this.audioDataFile = File.createTempFile("mp4audio", ".pcm", new File(recordingFilename).getParentFile());
		this.audioDataFile.deleteOnExit();
		this.out = new FileOutputStream(audioDataFile);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			out.write(buffer().array(), 0, buffer().capacity());
			buffer().clear();
		} catch (IOException e) {
			throw new RuntimeException("Error during encodeAudioFrame");
		}
	}

	@Override
	public void accept(int[] bgraData) {
		try {
			this.encoder.encodeNativeFrame(createPicture(bgraData));
		} catch (IOException e) {
			throw new RuntimeException("Error during encodeNativeFrame");
		}
	}

	@Override
	public void close() {
		try {
			if (encoder != null) {
				if (audioDataFile.exists()) {
					byte[] data = Files.readAllBytes(Paths.get(audioDataFile.getAbsolutePath()));
					if (data != null && data.length > 0) {
						System.out.println("  embedding non-standard PCM audio (not supported by all video players)!");
						encoder.encodeAudioFrame(getAudioBuffer(ByteBuffer.wrap(data)));
					}
					audioDataFile.delete();
				}
				encoder.finish();
				encoder = null;
				if (channel != null) {
					this.channel.close();
					this.channel = null;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error finishing encoder");
		}
	}

	private AudioFormat getAudioFormat(AudioConfig cfg) {
		switch (cfg.getFrameRate()) {
		case 44100:
			return AudioFormat.STEREO_44K_S16_LE;
		case 48000:
			return AudioFormat.STEREO_48K_S16_LE;
		default:
			throw new RuntimeException("Unsupported sampling rate for video recording");
		}
	}

	private Picture createPicture(int[] bgraData) {
		byte[] rgbData = pictureData[0];
		for (int bgraDataIdx = 0, rgbDataIdx = 0; bgraDataIdx < bgraData.length; bgraDataIdx++) {
			rgbData[rgbDataIdx] = (byte) (((bgraData[bgraDataIdx] >> 16) & 0xff) - 128);
			rgbData[rgbDataIdx + 1] = (byte) (((bgraData[bgraDataIdx] >> 8) & 0xff) - 128);
			rgbData[rgbDataIdx + 2] = (byte) (((bgraData[bgraDataIdx]) & 0xff) - 128);
			rgbDataIdx += 3;
		}
		return Picture.createPicture(VIC.MAX_WIDTH, VIC.MAX_HEIGHT, pictureData, ColorSpace.RGB);
	}

	private AudioBuffer getAudioBuffer(ByteBuffer sampleBuffer) {
		buffer().clear();
		return new AudioBuffer(sampleBuffer, audioFormat, 0);
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}
}
