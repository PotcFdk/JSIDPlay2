package libsidutils.fingerprinting.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.fingerprint.Fingerprint;
import libsidutils.fingerprinting.rest.beans.WavBean;

/**
 * Created by hsyecheng on 2015/6/13. Generate Fingerprints. The sampling rate
 * of the sample data should be 8000.
 */
public class FingerprintedSampleData {

	private static final int SAMPLE_RATE = 8000;

	private Fingerprint fingerprint;
	private String title, album, artist;
	private double audioLength;

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

	public FingerprintedSampleData(WavBean wavBean) {
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBean.getWav()));
			if (stream.getFormat().getSampleSizeInBits() != Short.SIZE) {
				throw new RuntimeException("Sample size in bits must be " + Short.SIZE);
			}
			if (stream.getFormat().getChannels() != 2) {
				throw new RuntimeException("Channels must be 2");
			}
			if (stream.getFormat().getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
				throw new RuntimeException("Encoding must be PCM_SIGNED");
			}
			if (stream.getFormat().getSampleRate() != SAMPLE_RATE) {
				throw new RuntimeException("SampleRate must be " + SAMPLE_RATE);
			}

			byte[] bytes = new byte[(int) stream.getFrameLength() << 2];
			stream.read(bytes);

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
			throw new RuntimeException(e);
		}
	}

	public void setMetaInfo(SidTune tune, String recordingFilename) {
		if (tune != SidTune.RESET && tune.getInfo().getInfoString().size() == 3) {
			Iterator<String> description = tune.getInfo().getInfoString().iterator();
			this.title = description.next();
			this.artist = description.next();
			this.album = description.next();
		} else {
			this.title = new File(PathUtils.getFilenameWithoutSuffix(recordingFilename)).getName();
			this.artist = "???";
			this.album = "???";
		}
	}

}
