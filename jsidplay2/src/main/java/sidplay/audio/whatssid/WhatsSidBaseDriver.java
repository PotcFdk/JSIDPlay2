package sidplay.audio.whatssid;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import sidplay.audio.AudioDriver;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the main algorithm. Use WhatsSidDriver and WhatsSidMatcherDriver.
 * 
 * @author ken
 *
 */
public abstract class WhatsSidBaseDriver implements AudioDriver {

	public final int UPPER_LIMIT = 300;
	public final int LOWER_LIMIT = 40;

	int[] RANGE = IntStream.range(0, (UPPER_LIMIT - LOWER_LIMIT) / 40).map(i -> (LOWER_LIMIT + i * 40)).toArray();

	private static final int FUZ_FACTOR = 2;

	// TODO Store externally!!!
	private static Map<Long, List<DataPoint>> hashMap = new HashMap<Long, List<DataPoint>>();
	protected static long nrSong = 0;
	protected static Map<Long, String> songNrToNameMap = new HashMap<>();

	private Map<Integer, Map<Integer, Integer>> matchMap;

	protected void makeSpectrum(ByteArrayOutputStream out, long songId, boolean isMatching) {
		byte audio[] = out.toByteArray();

		int amountPossible = audio.length / 4096;

		// When turning into frequency domain we'll need complex numbers:
		Complex[][] results = new Complex[amountPossible][];

		// For all the chunks:
		for (int times = 0; times < amountPossible; times++) {
			Complex[] complex = new Complex[4096];
			for (int i = 0; i < 4096; i++) {
				// Put the time domain data into a complex number with imaginary
				// part as 0:
				complex[i] = new Complex(audio[(times * 4096) + i], 0);
			}
			// Perform FFT analysis on the chunk:
			results[times] = FFT.fft(complex);
		}
		determineKeyPoints(results, songId, isMatching);
	}

	private void determineKeyPoints(Complex[][] results, long songId, boolean isMatching) {
		matchMap = new HashMap<Integer, Map<Integer, Integer>>();

		double highscores[][] = new double[results.length][RANGE.length];
		long points[][] = new long[results.length][RANGE.length];

		for (int t = 0; t < results.length; t++) {
			for (int freq = LOWER_LIMIT; freq < RANGE[RANGE.length - 1]; freq++) {
				// Get the magnitude:
				double mag = Math.log(results[t][freq].abs() + 1);

				// Find out which range we are in:
				int index = getIndex(freq);

				// Save the highest magnitude and corresponding frequency:
				if (mag > highscores[t][index]) {
					highscores[t][index] = mag;
					points[t][index] = freq;
				}
			}

			long h = hash(points[t][0], points[t][1], points[t][2], points[t][3]);

			if (isMatching) {
				List<DataPoint> listPoints;
				if ((listPoints = hashMap.get(h)) != null) {
					for (DataPoint dP : listPoints) {
						int offset = Math.abs(dP.getTime() - t);
						Map<Integer, Integer> tmpMap = null;
						if ((tmpMap = matchMap.get(dP.getSongId())) == null) {
							tmpMap = new HashMap<Integer, Integer>();
							tmpMap.put(offset, 1);
							matchMap.put(dP.getSongId(), tmpMap);
						} else {
							Integer count = tmpMap.get(offset);
							if (count == null) {
								tmpMap.put(offset, new Integer(1));
							} else {
								tmpMap.put(offset, new Integer(count + 1));
							}
						}
					}
				}
			} else {
				List<DataPoint> listPoints = null;
				if ((listPoints = hashMap.get(h)) == null) {
					listPoints = new ArrayList<DataPoint>();
					hashMap.put(h, listPoints);
				}
				listPoints.add(new DataPoint((int) songId, t));
			}
		}
	}

	protected void match() {
		int bestCount = 0;
		int bestSong = -1;

		for (int id = 0; id < nrSong; id++) {

//			System.out.println("Match For song id: " + id);
			Map<Integer, Integer> tmpMap = matchMap.get(id);
			int bestCountForSong = 0;

			for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
				if (entry.getValue() > bestCountForSong) {
					bestCountForSong = entry.getValue();
				}
//				System.out.println("Time offset = " + entry.getKey()
//						+ ", Count = " + entry.getValue());
			}

			if (/* bestCountForSong > 3 && */ bestCountForSong > bestCount) {
				bestCount = bestCountForSong;
				bestSong = id;
				// TODO create list of possible candidates and Print top 10!

				System.out.printf("Tune %s(%d) matched %dx\n", songNrToNameMap.get(Long.valueOf(bestSong)), bestSong,
						bestCount);
			}
		}

		System.out.printf("Best tune is %s(%d)\n", songNrToNameMap.get(Long.valueOf(bestSong)), bestSong);
	}

	private long hash(long p1, long p2, long p3, long p4) {
		return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR)) * 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
				+ (p1 - (p1 % FUZ_FACTOR));
	}

	private int getIndex(int freq) {
		int i = 0;
		while (RANGE[i] < freq)
			i++;
		return i;
	}

}
