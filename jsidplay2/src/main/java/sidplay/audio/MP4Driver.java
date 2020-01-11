package sidplay.audio;

import static sidplay.audio.Audio.MP4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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
import org.mp4parser.muxer.tracks.TextTrackImpl;
import org.sheinbergon.aac.encoder.AACAudioEncoder;
import org.sheinbergon.aac.encoder.AACAudioOutput;
import org.sheinbergon.aac.encoder.WAVAudioInput;
import org.sheinbergon.aac.encoder.util.AACEncodingProfile;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;
import libsidutils.PathUtils;

public class MP4Driver implements AudioDriver, VideoDriver {

	private OutputStream pcmAudioStream;
	private File pcmAudioFile, videoFile;
	private String recordingFilename;
	private AACAudioEncoder aacEncoder;
	private SequenceEncoder sequenceEncoder;
	private ByteBuffer vicPixelBuffer, pictureBuffer, sampleBuffer;
	private Picture picture;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		try {
			System.out.println("Recording, file=" + recordingFilename);
			new File(recordingFilename).delete();
			String recordingBaseName = PathUtils.getFilenameWithoutSuffix(recordingFilename);
			this.videoFile = new File(recordingBaseName + "_video.mp4");
			this.pcmAudioFile = new File(recordingBaseName + ".pcm");
			this.pcmAudioStream = new FileOutputStream(pcmAudioFile);
			this.recordingFilename = recordingFilename;

			this.aacEncoder = AACAudioEncoder.builder().channels(2).sampleRate(cfg.getFrameRate())
					.profile(AACEncodingProfile.AAC_LC).build();
			this.sequenceEncoder = SequenceEncoder.createWithFps(NIOUtils.writableChannel(videoFile),
					Rational.R((int) cpuClock.getScreenRefresh(), 1));

			this.vicPixelBuffer = ByteBuffer.allocateDirect(VIC.MAX_WIDTH * VIC.MAX_HEIGHT * Integer.BYTES)
					.order(ByteOrder.BIG_ENDIAN);
			this.picture = Picture.createPicture(VIC.MAX_WIDTH, VIC.MAX_HEIGHT,
					new byte[1][3/* RGB */ * VIC.MAX_WIDTH * VIC.MAX_HEIGHT], ColorSpace.RGB);
			this.pictureBuffer = ByteBuffer.wrap(picture.getData()[0]);

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
			this.pcmAudioStream.write(sampleBuffer.array(), 0, sampleBuffer.position());
		} catch (IOException e) {
			throw new RuntimeException("Error writing PCM audio stream", e);
		}
	}

	@Override
	public void accept(VIC vic, int[] pixels) {
		try {
			setPictureData(pixels);
			this.sequenceEncoder.encodeNativeFrame(picture);
		} catch (IOException e) {
			throw new RuntimeException("Error writing H264 video stream", e);
		}
	}

	@Override
	public void close() {
		try {
			boolean encoded = sequenceEncoder != null;
			if (sequenceEncoder != null) {
				sequenceEncoder.finish();
				sequenceEncoder = null;
			}
			if (pcmAudioStream != null) {
				pcmAudioStream.close();
				pcmAudioStream = null;
			}
			if (encoded && videoFile.exists() && videoFile.canRead()
					&& videoFile.length() > 56/* empty container size */) {
				try (FileInputStream h264VideoInputStream = new FileInputStream(videoFile);
						FileOutputStream mp4VideoOutputStream = new FileOutputStream(recordingFilename);
						FileRandomAccessSourceImpl h264RandomAccessSource = new FileRandomAccessSourceImpl(
								new RandomAccessFile(videoFile, "r"))) {
					Movie movie = MovieCreator.build(h264VideoInputStream.getChannel(), h264RandomAccessSource,
							videoFile.getName());
					movie.addTrack(getSubtitles());
					if (pcmAudioFile.exists() && pcmAudioFile.canRead() && pcmAudioFile.length() > 0) {
						byte[] data = Files.readAllBytes(Paths.get(pcmAudioFile.getAbsolutePath()));
						AACAudioOutput output = aacEncoder.encode(WAVAudioInput.pcms16le(data, data.length));
						movie.addTrack(new AACTrackImpl(
								new MemoryDataSourceImpl(ByteBuffer.wrap(output.data(), 0, output.length()))));
						pcmAudioFile.delete();
					}
					new DefaultMp4Builder().build(movie).writeContainer(mp4VideoOutputStream.getChannel());
				} finally {
					videoFile.delete();
					// hack: remove remaining temporary files of mp4parser :-(
					File tmpDir = new File(System.getProperty("java.io.tmpdir"));
					Arrays.asList(tmpDir.list((dir, name) -> name.startsWith("MediaDataBox")
							&& (System.currentTimeMillis() - new File(dir, name).lastModified() > 10 * 60 * 1000)))
							.stream().forEach(name -> new File(tmpDir, name).delete());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error creating MP4", e);
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
		return ".mp4";
	}
	
	private TextTrackImpl getSubtitles() {
		TextTrackImpl textTrack = new TextTrackImpl();
		textTrack.getSubs().add(new TextTrackImpl.Line(0, 3 * 1000 /* ms */, "Recorded by JSIDPlay2"));
		return textTrack;
	}

	private void setPictureData(int[] pixels) {
		((Buffer) vicPixelBuffer).clear();
		vicPixelBuffer.asIntBuffer().put(pixels);
		((Buffer) pictureBuffer).clear();
		for (int channelIndex = 0; channelIndex < vicPixelBuffer.capacity(); channelIndex++) {
			byte pixelData = vicPixelBuffer.get();
			// ignore ALPHA channel (ARGB channel order)
			if (channelIndex % 4 != 0) {
				// picture data is -128 shifted!
				pictureBuffer.put((byte) (pixelData - 128));
			}
		}
	}
}
