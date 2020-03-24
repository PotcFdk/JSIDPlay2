package libsidutils.fingerprinting.fingerprint;

import java.util.ArrayList;

import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.spectrogram.Spectrogram;
import libsidutils.fingerprinting.spectrogram.Window;

/**
 * Created by hsyecheng on 2015/6/11.
 */
public class Fingerprint {
	private final int NPeaks = 3;
	private final int fftSize = 512;
	private final int overlap = 256;
	private final int C = 32;
	private final int peakRange = 5;

	private final ArrayList<Peak> peakList = new ArrayList<>();
	private final ArrayList<Link> linkList = new ArrayList<>();
	private final float[] freq;
	private final float[] time;

	private final float[] range_time = { 1f, 3f };
	private final float[] range_freq = { -600f, 600f };
	private final int[] Band = { 11, 22, 35, 50, 69, 91, 117, 149, 187, 231 };

	private final float minFreq = 100;
	private final float maxFreq = 2000;
	private final float minPower = 0;

	public Fingerprint(float[] data, float fs) {
		Spectrogram spectrogram = new Spectrogram(data, Window.HANN, fftSize, overlap, fs);
		ArrayList<float[]> stft = spectrogram.stft;
		freq = spectrogram.freq;
		time = spectrogram.time;

		ArrayList<Peak> tmp = new ArrayList<>(C * NPeaks);
		int size = stft.size();
		int bandNum = Band.length - 1;
		for (int b = 0; b < bandNum; b++) {
			for (int i = 0; i < size; i++) {
				if (i != 0) {
					if (i % C == 0 || i == size - 1) {
						// Filter
						tmp.removeIf(peak -> {
							float peakFreq = freq[peak.getIntFreq()];
							return peakFreq < minFreq || peakFreq > maxFreq;
						});
						tmp.removeIf(peak -> peak.getPower() <= minPower);

						tmp.sort((o1, o2) -> Double.compare(o2.getPower(), o1.getPower()));

						int end = tmp.size() < NPeaks ? tmp.size() : NPeaks;
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
				FindPeaks find = new FindPeaks(NPeaks);
				find.findComplexPeaks(fft_band, peakRange);
				float[] power = find.getPower();
				int[] loc = find.getLocate();

				for (int j = 0; j < power.length; j++) {
					loc[j] += Band[b];
				}

				for (int j = 0; j < NPeaks; j++) {
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
				if (t2 - t >= range_time[0])
					break;
			}
			int tStart = k;
			for (; k < n; k++) {
				float t = time[p1.getIntTime()];
				float t2 = time[peakList.get(k).getIntTime()];
				if (t2 - t >= range_time[1])
					break;
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
}
