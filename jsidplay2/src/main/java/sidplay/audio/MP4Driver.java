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

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
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
	private String recordingFilename;
	private File h264VideoFile;
	private File aacAudioFile;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		System.out.println("Recording, file=" + recordingFilename);

		String recordingBaseName = PathUtils.getFilenameWithoutSuffix(recordingFilename);
		this.h264VideoFile = new File(recordingBaseName + ".h264");
		this.aacAudioFile = new File(recordingBaseName + ".aac");
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
				byte[] data = Files.readAllBytes(Paths.get(pcmAudioFile.getAbsolutePath()));
				try (OutputStream out = new FileOutputStream(aacAudioFile)) {
					AACAudioOutput output = aacEncoder.encode(WAVAudioInput.pcms16le(data, data.length));
					out.write(output.data(), 0, output.length());
				}
				pcmAudioFile.delete();
				Movie movie = MovieCreator.build(h264VideoFile.getAbsolutePath());
				movie.addTrack(new AACTrackImpl(new FileDataSourceImpl(aacAudioFile)));
				Container mp4file = new DefaultMp4Builder().build(movie);
				try (FileOutputStream out = new FileOutputStream(new File(recordingFilename))) {
					mp4file.writeContainer(out.getChannel());
				}
				aacAudioFile.delete();
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
