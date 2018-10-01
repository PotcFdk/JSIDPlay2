package sidplay.audio;

import static sidplay.audio.Audio.MP4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.sound.sampled.LineUnavailableException;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.MemoryDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.sheinbergon.aac.encoder.AACAudioEncoder;
import org.sheinbergon.aac.encoder.AACAudioOutput;
import org.sheinbergon.aac.encoder.WAVAudioInput;
import org.sheinbergon.aac.encoder.util.AACEncodingProfile;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;
import libsidutils.PathUtils;

public class MP4Driver implements AudioDriver, Consumer<int[]> {

	private AACAudioEncoder aacEncoder;
	private SequenceEncoder sequenceEncoder;
	private byte[][] pictureData;
	private ByteBuffer sampleBuffer;
	private OutputStream pcmAudioStream;
	private File pcmAudioFile;
	private File h264VideoFile;
	private String recordingFilename;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		try {
			System.out.println("Recording, file=" + recordingFilename);
			String recordingBaseName = PathUtils.getFilenameWithoutSuffix(recordingFilename);
			this.h264VideoFile = new File(recordingBaseName + ".h264");
			this.pcmAudioFile = new File(recordingBaseName + ".pcm");
			this.pcmAudioStream = new FileOutputStream(pcmAudioFile);
			this.recordingFilename = recordingFilename;

			this.aacEncoder = AACAudioEncoder.builder().channels(2).sampleRate(cfg.getFrameRate())
					.profile(AACEncodingProfile.AAC_LC).build();
			this.sequenceEncoder = SequenceEncoder.createWithFps(NIOUtils.writableChannel(h264VideoFile),
					Rational.R((int) cpuClock.getScreenRefresh(), 1));

			this.pictureData = new byte[1][3 * VIC.MAX_WIDTH * VIC.MAX_HEIGHT];

			this.sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
					.order(ByteOrder.LITTLE_ENDIAN);
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Error: 64-bit Java for Windows or Linux is required to use " + MP4 + " video driver!");
			throw e;
		}
	}

	@Override
	public void write() throws InterruptedException {
		try {
			this.pcmAudioStream.write(buffer().array());
		} catch (IOException e) {
			throw new RuntimeException("Error writing PCM audio stream", e);
		}
	}

	@Override
	public void accept(int[] bgraData) {
		try {
			this.sequenceEncoder.encodeNativeFrame(createPicture(bgraData));
		} catch (IOException e) {
			throw new RuntimeException("Error writing H264 video stream", e);
		}
	}

	@Override
	public void close() {
		try {
			if (sequenceEncoder != null) {
				sequenceEncoder.finish();
				sequenceEncoder = null;
			}
			if (pcmAudioStream != null) {
				pcmAudioStream.close();
				pcmAudioStream = null;
			}
			if (h264VideoFile.exists() && h264VideoFile.canRead()) {
				try (FileInputStream h264VideoInputStream = new FileInputStream(h264VideoFile);
						FileRandomAccessSourceImpl h264VideoRandomAccessSource = new FileRandomAccessSourceImpl(
								new RandomAccessFile(h264VideoFile, "r"));
						FileOutputStream mp4VideoOutputStream = new FileOutputStream(recordingFilename)) {
					Movie movie = MovieCreator.build(h264VideoInputStream.getChannel(), h264VideoRandomAccessSource,
							h264VideoFile.getAbsolutePath());
					if (pcmAudioFile.exists() && pcmAudioFile.canRead() && pcmAudioFile.length() > 0) {
						byte[] data = Files.readAllBytes(Paths.get(pcmAudioFile.getAbsolutePath()));
						AACAudioOutput output = aacEncoder.encode(WAVAudioInput.pcms16le(data, data.length));
						movie.addTrack(new AACTrackImpl(
								new MemoryDataSourceImpl(ByteBuffer.wrap(output.data(), 0, output.length()))));
						pcmAudioFile.delete();
					}
					new DefaultMp4Builder().build(movie).writeContainer(mp4VideoOutputStream.getChannel());
					// remove remaining temporary files of mp4parser :-(
					File tmpDir = new File(System.getProperty("java.io.tmpdir"));
					Arrays.asList(tmpDir.list((dir, name) -> name.startsWith("MediaDataBox")
							&& (System.currentTimeMillis() - new File(dir, name).lastModified() > 10 * 60 * 1000)))
							.stream().forEach(name -> new File(tmpDir, name).delete());
				}
				h264VideoFile.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error creating MP4", e);
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

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}
}
