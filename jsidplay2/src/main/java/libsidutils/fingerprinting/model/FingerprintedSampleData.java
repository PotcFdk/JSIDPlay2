package libsidutils.fingerprinting.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.Random;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.fingerprint.Fingerprint;
import sidplay.fingerprinting.MusicInfoBean;
import sidplay.fingerprinting.WavBean;

/**
 * Created by hsyecheng on 2015/6/13. Generate Fingerprints. The sampling rate
 * of the sample data should be 8000.
 */
public class FingerprintedSampleData {

	private static final int SAMPLE_RATE = 8000;

	private Fingerprint fingerprint;
	private String title, album, artist;
	private double audioLength;

	private final Random RANDOM = new Random();
	private int oldRandomValue;

	private String fileDir;

	private String infoDir;

	public Fingerprint getFingerprint() {
		return fingerprint;
	}

	public String getTitle() {
		return title;
	}

	public String getArtist() {
		return artist;
	}

	public String getAlbum() {
		return album;
	}

	public double getAudioLength() {
		return audioLength;
	}

	public void setWav(WavBean wavBean) throws IOException {
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBean.getWav()));
			if (stream.getFormat().getSampleSizeInBits() != Short.SIZE) {
				throw new IOException("Sample size in bits must be " + Short.SIZE);
			}
			if (stream.getFormat().getEncoding() != Encoding.PCM_SIGNED) {
				throw new IOException("Encoding must be " + Encoding.PCM_SIGNED);
			}
			if (stream.getFormat().isBigEndian()) {
				throw new IOException("LittleEndian expected");
			}

			// 1. mono to stereo conversion
			byte[] bytes;
			if (stream.getFormat().getChannels() == 1) {
				bytes = new byte[(int) stream.getFrameLength() << 1];
				stream.read(bytes);
				ShortBuffer monoSamples = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				ByteBuffer stereoBuffer = ByteBuffer.allocate(bytes.length << 1).order(ByteOrder.LITTLE_ENDIAN);
				ShortBuffer stereoSamples = stereoBuffer.asShortBuffer();

				while (monoSamples.hasRemaining()) {
					short mono = monoSamples.get();
					stereoSamples.put(mono);
					stereoSamples.put(mono);
				}
				bytes = stereoBuffer.array();
			} else if (stream.getFormat().getChannels() == 2) {
				bytes = new byte[(int) stream.getFrameLength() << 2];
				stream.read(bytes);
			} else {
				throw new IOException("Number of channels must be one or two");
			}

			// 2. down sampling to 8KHz
			if (stream.getFormat().getSampleRate() != SAMPLE_RATE) {
				Resampler downSamplerL = Resampler.createResampler(stream.getFormat().getSampleRate(),
						SamplingMethod.RESAMPLE, SamplingRate.VERY_LOW.getFrequency(),
						SamplingRate.VERY_LOW.getMiddleFrequency());
				Resampler downSamplerR = Resampler.createResampler(stream.getFormat().getSampleRate(),
						SamplingMethod.RESAMPLE, SamplingRate.VERY_LOW.getFrequency(),
						SamplingRate.VERY_LOW.getMiddleFrequency());

				ByteBuffer result = ByteBuffer.allocate(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
				ShortBuffer sb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				while (sb.hasRemaining()) {
					short valL = sb.get();
					short valR = sb.get();

					int downSamplerDither = triangularDithering();
					if (downSamplerL.input(valL)) {
						result.putShort((short) Math.max(
								Math.min(downSamplerL.output() + downSamplerDither, Short.MAX_VALUE), Short.MIN_VALUE));
					}
					if (downSamplerR.input(valR)) {
						if (!result.putShort(
								(short) Math.max(Math.min(downSamplerR.output() + downSamplerDither, Short.MAX_VALUE),
										Short.MIN_VALUE))
								.hasRemaining()) {
							((Buffer) result).flip();
						}
					}
				}
				bytes = result.array();
			}

			// 3. convert to float mono mix
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			int len = buf.limit() >> 2/* bytes * channels */;
			float[] dataL = new float[len];
			float[] dataR = new float[len];
			for (int i = 0; i < len; i++) {
				buf.order(ByteOrder.LITTLE_ENDIAN);
				dataL[i] = buf.getShort() / 32768f;
				dataR[i] = buf.getShort() / 32768f;
			}
			float[] data = new float[len];
			for (int i = 0; i < len; i++) {
				data[i] = dataL[i] + dataR[i];
				data[i] /= 2;
			}

			this.audioLength = len / (float) SAMPLE_RATE;
			this.fingerprint = new Fingerprint(data, SAMPLE_RATE);
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new IOException(e);
		}
	}

	public void setMetaInfo(SidTune tune, String recordingFilename, String collectionFilename) {
		if (tune != SidTune.RESET && tune.getInfo().getInfoString().size() == 3) {
			Iterator<String> description = tune.getInfo().getInfoString().iterator();
			this.title = description.next();
			this.artist = description.next();
			this.album = description.next();
		} else {
			this.title = new File(PathUtils.getFilenameWithoutSuffix(collectionFilename)).getName();
			this.artist = "???";
			this.album = "???";
		}
		fileDir = recordingFilename;
		infoDir = collectionFilename;
	}

	public MusicInfoBean toMusicInfoBean() {
		MusicInfoBean musicInfoBean = new MusicInfoBean();
		musicInfoBean.setTitle(title);
		musicInfoBean.setArtist(artist);
		musicInfoBean.setAlbum(album);
		musicInfoBean.setAudioLength(audioLength);
		musicInfoBean.setFileDir(fileDir);
		musicInfoBean.setInfoDir(infoDir);
		return musicInfoBean;
	}

	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x1;
		return oldRandomValue - prevValue;
	}
}
