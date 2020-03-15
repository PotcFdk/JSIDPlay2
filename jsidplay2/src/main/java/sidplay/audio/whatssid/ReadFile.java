package sidplay.audio.whatssid;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import sidplay.audio.whatssid.fingerprint.Fingerprint;

/**
 * Created by hsyecheng on 2015/6/13. Read WAV file and generate Fingerprints.
 * The sampling rate of the WAV file should be 8000.
 */
public class ReadFile {

	private static final int SAMPLE_RATE = 8000;
	public Fingerprint fingerprint;
	public String Title;
	public String Album;
	public String Artist;
	public double audio_length;

	public ReadFile() {
		super();
	}

	public void getMetaInfo(SidTune tune, String recordingFilename) {
		if (tune.getInfo().getInfoString().size() == 3) {
			Iterator<String> description = tune.getInfo().getInfoString().iterator();
			Title = description.next();
			Artist = description.next();
			Album = description.next();
		} else {
			Title = new File(PathUtils.getFilenameWithoutSuffix(recordingFilename)).getName();
			Artist = "???";
			Album = "???";
		}
	}

	public void readFile(byte[] sampleData) {
		int len = (int) (sampleData.length / 4/* bytes * channels */);

		float[] dataL = new float[len];
		float[] dataR = new float[len];

		ByteBuffer buf = ByteBuffer.allocate(4 * len);
		buf.put(sampleData);
		buf.rewind();

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

		this.audio_length = len / (float) SAMPLE_RATE;
		fingerprint = new Fingerprint(data, SAMPLE_RATE);
	}
}
