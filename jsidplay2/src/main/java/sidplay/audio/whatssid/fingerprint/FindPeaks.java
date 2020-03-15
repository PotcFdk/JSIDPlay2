package sidplay.audio.whatssid.fingerprint;

/**
 * Created by hsyec_000 on 2015/6/11. Find local peaks in fft data.
 */
public class FindPeaks {

	private final int nPeaks;
	private final float[] power;
	private final int[] locate;

	public FindPeaks(int nPeaks) {
		this.nPeaks = nPeaks;
		this.power = new float[nPeaks];
		this.locate = new int[nPeaks];
	}

	public float[] getPower() {
		return power;
	}

	public int[] getLocate() {
		return locate;
	}

	public void findComplexPeaks(float[] data, int neighborRange) {
		int dataLength = data.length / 2;
		for (int i = 0; i < nPeaks; i++) {
			power[i] = -500;
			locate[i] = -1;
		}

		float[] dataPower = new float[dataLength];

		for (int i = 0; i < dataLength; i++) {
			dataPower[i] = (float) (10 * Math.log10(data[2 * i] * data[2 * i] + data[2 * i + 1] * data[2 * i + 1]));
		}

		for (int k = 0; k < dataLength; k++) {
			float pi = dataPower[k];
			boolean add = true;
			for (int j = 0; j < neighborRange; j++) {
				float pl;
				if (k - neighborRange >= 0) {
					pl = dataPower[k - neighborRange];
				} else {
					pl = pi - 1;
				}
				float pr;
				if (k + neighborRange < dataLength) {
					pr = dataPower[k + neighborRange];
				} else {
					pr = pi - 1;
				}
				if (pi < pl && pi < pr) {
					add = false;
				}
			}
			if (add) {
				add(pi, k);
			}
		}
	}

	private void add(float p, int loc) {
		for (int i = 0; i < power.length; i++) {
			if (power[i] < p) {
				for (int j = power.length - 1; j > i; j--) {
					power[j] = power[j - 1];
					locate[j] = locate[j - 1];
				}
				power[i] = p;
				locate[i] = loc;
				break;
			}
		}
	}

}
