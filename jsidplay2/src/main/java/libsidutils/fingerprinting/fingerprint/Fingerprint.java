package libsidutils.fingerprinting.fingerprint;

import java.util.ArrayList;

import libsidutils.fingerprinting.ini.IFingerprintConfig;
import libsidutils.fingerprinting.ini.IFingerprintSection;
import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.spectrogram.Spectrogram;
import libsidutils.fingerprinting.spectrogram.Window;

/**
 * <B>Working principle:</B><BR>
 *
 * The algorithm is similar to Shazam. First, I calculate the spectrum of the
 * audio. The spectrum is divided into several sub-bands according to the
 * frequency, and several peak points are searched for each sub-band. The
 * subband of the algorithm is based on the Mel frequency.
 *
 * The peak points to be obtained are grouped according to the frequency and
 * time range.
 *
 * The point-to-frequency range of the algorithm is within the sub-band, the
 * purpose of which is to reduce the number of pairs of points and improve the
 * distributed capability. The time range for taking a pair is 1s-4s. You can
 * modify these parameters as needed.<BR>
 * <BR>
 *
 * <B>Performance and effects:</B><BR>
 * <i> Data volume: </i>The music library is 1500 songs, the number of
 * fingerprints is about 24 million, and the server takes up about 340M after
 * being stable. <BR>
 * <BR>
 * <i> Speed: </i>Processor i7-3632QM, adding 1500 songs takes about 1919
 * seconds, and a song takes about 1.3 seconds. It takes about 0.2 seconds to
 * find a 10s song using the server (regardless of the time the client reads the
 * file). <BR>
 * <BR>
 * <i> Accuracy: </i> has a high recognition rate for low-noise audio, and close
 * to commercial accuracy for higher noise, but relatively speaking, if there is
 * a song that does not appear in the music library, there is a certain error.
 * Report rate. <BR>
 * <BR>
 * <i> Anti-noise: </i> can resist strong distortion and noise, you can refer to
 * the test audio I gave.<BR>
 * <BR>
 * <B>Contact information:</B><BR>
 * EMAIL: hsyecheng@hotmail.com<BR>
 * <BR>
 * <B>Note:</B> A music fingerprinting system that uses JAVA and requires a
 * MySQL database (although it is not required, but the system uses it to save
 * fingerprints and music information). You may need to modify the
 * max_allowed_packet parameter, because adding a song requires sending a larger
 * package. The parameter I am using is 32M
 *
 * @see <a href=
 *      "https://github.com/JPery/Audio-Fingerprinting">Audio-Fingerprinting</a>
 * @see <a href=
 *      "http://www.ee.columbia.edu/~dpwe/papers/Wang03-shazam.pdf">Shazam</a>
 * @see <a href="https://en.wikipedia.org/wiki/Mel_scale">Mel scale</a>
 *
 * @author hsyecheng on 2015/6/11.
 */
public class Fingerprint {

	static final int SAMPLE_RATE = 8000;

	private int dataLen;
	private int nPeaks, fftSize, overlap, c, peakRange;
	private float[] range_time, range_freq;
	private int[] Band;
	private int minFreq, maxFreq, minPower;

	private final ArrayList<Peak> peakList = new ArrayList<>();
	private final ArrayList<Link> linkList = new ArrayList<>();
	private final float[] freq, time;

	public Fingerprint(IFingerprintConfig config, float[] data) {
		dataLen = data.length;
		IFingerprintSection fingerPrintSection = config.getFingerPrintSection();
		nPeaks = fingerPrintSection.getNPeaks();
		fftSize = fingerPrintSection.getFftSize();
		overlap = fingerPrintSection.getOverlap();
		c = fingerPrintSection.getC();
		peakRange = fingerPrintSection.getPeakRange();
		range_time = fingerPrintSection.getRangeTime();
		range_freq = fingerPrintSection.getRangeFreq();
		Band = fingerPrintSection.getBand();
		minFreq = fingerPrintSection.getMinFreq();
		maxFreq = fingerPrintSection.getMaxFreq();
		minPower = fingerPrintSection.getMinPower();

		Spectrogram spectrogram = new Spectrogram(data, Window.HANN, fftSize, overlap, SAMPLE_RATE);
		ArrayList<float[]> stft = spectrogram.stft;
		freq = spectrogram.freq;
		time = spectrogram.time;

		ArrayList<Peak> tmp = new ArrayList<>(c * nPeaks);
		int size = stft.size();
		int bandNum = Band.length - 1;
		for (int b = 0; b < bandNum; b++) {
			for (int i = 0; i < size; i++) {
				if (i != 0) {
					if (i % c == 0 || i == size - 1) {
						// Filter
						tmp.removeIf(peak -> {
							float peakFreq = freq[peak.getIntFreq()];
							return peakFreq < minFreq || peakFreq > maxFreq;
						});
						tmp.removeIf(peak -> peak.getPower() <= minPower);

						tmp.sort((o1, o2) -> Double.compare(o2.getPower(), o1.getPower()));

						int end = tmp.size() < nPeaks ? tmp.size() : nPeaks;
						peakList.addAll(tmp.subList(0, end));
						tmp.clear();
					}
				}

				float[] fft = stft.get(i);
				int len = Band[b + 1] - Band[b];
				int start = Band[b] * 2;
				len *= 2;
				float[] fft_band = new float[len];
				System.arraycopy(fft, start, fft_band, 0, len);
				FindPeaks find = new FindPeaks(nPeaks);
				find.findComplexPeaks(fft_band, peakRange);
				float[] power = find.getPower();
				int[] loc = find.getLocate();

				for (int j = 0; j < power.length; j++) {
					loc[j] += Band[b];
				}

				for (int j = 0; j < nPeaks; j++) {
					if (loc[j] == -1) {
						continue;
					}
					Peak p = new Peak();
					p.setIntFreq(loc[j]);
					p.setIntTime(i);
					p.setPower(power[j]);

					tmp.add(p);
				}
			}
		}

		peakList.sort((o1, o2) -> o1.getIntTime() - o2.getIntTime());
		link(true);
	}

	private int inBand(int intFreq) {
		int size = Band.length;
		if (intFreq < Band[0] || intFreq > Band[size - 1]) {
			return -1;
		}
		for (int i = 0; i < size - 1; i++) {
			if (Band[i + 1] > intFreq) {
				return i;
			}
		}
		return -1;
	}

	private void link(boolean band) {
		int n = peakList.size();
		for (int i = 0; i < n; i++) {
			Peak p1 = peakList.get(i);
			if (p1 == null) {
				continue;
			}

			// time start|end
			int k;
			for (k = i + 1; k < n; k++) {
				float t = time[p1.getIntTime()];
				float t2 = time[peakList.get(k).getIntTime()];
				if (t2 - t >= range_time[0]) {
					break;
				}
			}
			int tStart = k;
			for (; k < n; k++) {
				float t = time[p1.getIntTime()];
				float t2 = time[peakList.get(k).getIntTime()];
				if (t2 - t >= range_time[1]) {
					break;
				}
			}
			int tEnd = k;
			// freq start|end
			float fstart = freq[p1.getIntFreq()] + range_freq[0];
			float fend = freq[p1.getIntFreq()] + range_freq[1];

			for (int i2 = tStart; i2 < tEnd; i2++) {
				Peak p2 = peakList.get(i2);
				if (p2 == null) {
					continue;
				}

				if (band) {
					int b1 = inBand(p1.getIntFreq());
					int b2 = inBand(p2.getIntFreq());

					if (b1 == b2 && b1 != -1) {
						Link l = new Link(p1, p2);
						linkList.add(l);
					}
				} else {
					if (freq[p2.getIntFreq()] >= fstart && freq[p2.getIntFreq()] <= fend) {
						Link l = new Link(p1, p2);
						linkList.add(l);
					}
				}
			}
		}
	}

	public ArrayList<Link> getLinkList() {
		return linkList;
	}

	public ArrayList<Peak> getPeakList() {
		return peakList;
	}

	public HashBeans toHashBeans(IdBean id) {
		HashBeans result = new HashBeans();
		for (Link link : linkList) {
			HashBean hashBean = new HashBean();
			hashBean.setHash(Hash.hash(link));
			hashBean.setId(id.getId());
			hashBean.setTime(link.getStart().getIntTime());
			result.getHashes().add(hashBean);
		}
		return result;
	}

	public double getAudioLength() {
		return dataLen / (float) SAMPLE_RATE;
	}
}
