package sidplay.audio;

import static libsidplay.components.mos656x.VIC.MAX_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_WIDTH;
import static sidplay.audio.Audio.MP4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Optional;

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
import org.sheinbergon.aac.encoder.AACAudioOutput.Accumulator;
import org.sheinbergon.aac.encoder.util.AACEncodingProfile;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.OS;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import libsidutils.PathUtils;

public class MP4FileDriver implements AudioDriver, VideoDriver {

	private String recordingFilename;

	private File videoFile;
	private SequenceEncoder sequenceEncoder;
	private Picture picture;

	private AACAudioEncoder aacEncoder;
	private Accumulator aacAccumulator;
	private int factor;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		this.recordingFilename = recordingFilename;
		try {
			System.out.println("Recording, file=" + recordingFilename);
			new File(recordingFilename).delete();
			String recordingBaseName = PathUtils.getFilenameWithoutSuffix(recordingFilename);
			videoFile = new File(recordingBaseName + "_video.mp4");

			sequenceEncoder = SequenceEncoder.createWithFps(NIOUtils.writableChannel(videoFile), valueOf(cpuClock));
			picture = Picture.createPicture(MAX_WIDTH, MAX_HEIGHT,
					new byte[1][ColorSpace.RGB.nComp * MAX_WIDTH * MAX_HEIGHT], ColorSpace.RGB);

			aacEncoder = AACAudioEncoder.builder().channels(cfg.getChannels()).sampleRate(cfg.getFrameRate())
					.profile(AACEncodingProfile.AAC_LC).build();
			aacAccumulator = AACAudioOutput.accumulator();

			sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
					.order(ByteOrder.LITTLE_ENDIAN);

			factor = Math.max(1, sampleBuffer.capacity() / aacEncoder.inputBufferSize());

		} catch (UnsatisfiedLinkError e) {
			System.err.println("Error: Java for Windows, Linux or OSX is required to use " + MP4 + " video driver!");
			if (OS.get() == OS.LINUX) {
				System.err.println("Try to install it yourself, use the following command and start JSIDPlay2 again:");
				System.err.println("sudo apt-get install libfdk-aac-dev");
			}
			throw e;
		}
	}

	@Override
	public void write() throws InterruptedException {
		int offset = 0;
		for (int i = 0; i < factor; i++) {

			byte[] buffer = new byte[Math.min(sampleBuffer.position() - offset, aacEncoder.inputBufferSize())];
			System.arraycopy(sampleBuffer.array(), offset, buffer, 0, buffer.length);

			aacEncoder.encode(aacAccumulator, buffer);

			offset += aacEncoder.inputBufferSize();
		}
	}

	@Override
	public void accept(VIC vic) {
		try {
			setPictureData(vic.getPixels());
			sequenceEncoder.encodeNativeFrame(picture);
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
			if (encoded && videoFile.exists() && videoFile.canRead()
					&& videoFile.length() > 56/* empty container size */) {

				try (FileInputStream h264VideoInputStream = new FileInputStream(videoFile);
						FileOutputStream mp4VideoOutputStream = new FileOutputStream(recordingFilename);
						FileRandomAccessSourceImpl h264RandomAccessSource = new FileRandomAccessSourceImpl(
								new RandomAccessFile(videoFile, "r"))) {

					Movie movie = MovieCreator.build(h264VideoInputStream.getChannel(), h264RandomAccessSource,
							videoFile.getName());

					byte[] aacData = aacAccumulator.done().data();
					if (aacData != null) {
						movie.addTrack(new AACTrackImpl(new MemoryDataSourceImpl(ByteBuffer.wrap(aacData))));
					}
					movie.addTrack(getSubtitles());

					new DefaultMp4Builder().build(movie).writeContainer(mp4VideoOutputStream.getChannel());

				} finally {
					videoFile.delete();
					// hack: remove remaining temporary files of mp4parser :-(
					File tmpDir = new File(System.getProperty("java.io.tmpdir"));
					Arrays.asList(Optional
							.ofNullable(tmpDir.list((dir,
									name) -> name.startsWith("MediaDataBox") && System.currentTimeMillis()
											- new File(dir, name).lastModified() > 10 * 60 * 1000))
							.orElse(new String[0])).stream().forEach(name -> new File(tmpDir, name).delete());
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

	private Rational valueOf(CPUClock cpuClock) {
		// greatest common divisor is 72 for PAL
		return Rational.R((int) cpuClock.getCpuFrequency() / 72, cpuClock.getCyclesPerFrame() / 72);
	}

	private TextTrackImpl getSubtitles() {
		TextTrackImpl textTrack = new TextTrackImpl();
		textTrack.getSubs().add(new TextTrackImpl.Line(0, 3 * 1000 /* ms */, "Recorded by JSIDPlay2"));
		return textTrack;
	}

	private final void setPictureData(IntBuffer pixels) {
		((Buffer) pixels).clear();
		ByteBuffer pictureBuffer = ByteBuffer.wrap(picture.getPlaneData(0));
		while (pixels.hasRemaining()) {
			int pixel = pixels.get();
			// ignore ALPHA channel (ARGB channel order), picture data is -128 shifted
			pictureBuffer.put((byte) ((pixel >> 16 & 0xff) - 128));
			pictureBuffer.put((byte) ((pixel >> 8 & 0xff) - 128));
			pictureBuffer.put((byte) ((pixel & 0xff) - 128));
		}
	}
}
