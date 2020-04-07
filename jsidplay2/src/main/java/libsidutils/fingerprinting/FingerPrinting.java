package libsidutils.fingerprinting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.model.FingerprintedSampleData;
import libsidutils.fingerprinting.model.SongMatch;
import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import sidplay.fingerprinting.FingerPrintMatcher;
import sidplay.fingerprinting.MusicInfoBean;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.fingerprinting.WavBean;

public class FingerPrinting implements FingerPrintMatcher {

	private static final int MIN_HIT = 20;

	private FingerPrintingDataSource fingerPrintingDataSource;

	public FingerPrinting(FingerPrintingDataSource fingerPrintingDataSource) {
		this.fingerPrintingDataSource = fingerPrintingDataSource;
	}

	public void insert(SidTune tune, String collectionFilename, String recordingFilename) throws IOException {
		FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData();
		fingerprintedSampleData.setMetaInfo(tune, recordingFilename, collectionFilename);

		if (!fingerPrintingDataSource.tuneExists(fingerprintedSampleData.toMusicInfoBean())) {
			WavBean wavBean = new WavBean(Files.readAllBytes(Paths.get(recordingFilename)));
			if (wavBean.getWav().length > 0) {
				fingerprintedSampleData.setWav(wavBean);
				IdBean id = fingerPrintingDataSource.insertTune(fingerprintedSampleData.toMusicInfoBean());
				fingerPrintingDataSource.insertHashes(fingerprintedSampleData.getFingerprint().toHashBeans(id));
			}
		}
	}

	public MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException {
		if (wavBean != null && wavBean.getWav().length > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData();
			fingerprintedSampleData.setWav(wavBean);

			Index index = new Index();
			index.setFingerPrintingClient(fingerPrintingDataSource);
			SongMatch songMatch = index.search(fingerprintedSampleData.getFingerprint(), MIN_HIT);

			if (songMatch != null && songMatch.getIdSong() != -1) {
				SongNoBean songNoBean = new SongNoBean();
				songNoBean.setSongNo(songMatch.getIdSong());
				MusicInfoBean musicInfoBean = fingerPrintingDataSource.findTune(songNoBean);

				MusicInfoWithConfidenceBean result = new MusicInfoWithConfidenceBean();
				result.setMusicInfo(musicInfoBean);
				result.setSongMatch(fingerprintedSampleData, songMatch);

				return result;
			}
		}
		return null;
	}
}
