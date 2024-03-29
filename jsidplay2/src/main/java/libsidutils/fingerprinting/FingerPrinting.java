package libsidutils.fingerprinting;

import static libsidplay.config.IWhatsSidSystemProperties.FRAME_MAX_LENGTH;

import java.io.IOException;

import libsidutils.fingerprinting.fingerprint.Fingerprint;
import libsidutils.fingerprinting.fingerprint.FingerprintCreator;
import libsidutils.fingerprinting.ini.IFingerprintConfig;
import libsidutils.fingerprinting.model.SongMatch;
import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;

public class FingerPrinting implements IFingerprintMatcher, IFingerprintInserter {

	private static final int MIN_HIT = 15;

	private IFingerprintConfig config;

	private FingerPrintingDataSource fingerPrintingDataSource;

	public FingerPrinting(IFingerprintConfig config, FingerPrintingDataSource fingerPrintingDataSource) {
		this.config = config;
		this.fingerPrintingDataSource = fingerPrintingDataSource;
	}

	@Override
	public void insert(MusicInfoBean musicInfoBean, WAVBean wavBean) throws IOException {
		if (wavBean != null && wavBean.getWav().length > 0) {

			if (!fingerPrintingDataSource.tuneExists(musicInfoBean)) {
				Fingerprint fingerprint = new FingerprintCreator().createFingerprint(config, wavBean, Long.MAX_VALUE);
				musicInfoBean.setAudioLength(fingerprint.getAudioLength());

				IdBean id = fingerPrintingDataSource.insertTune(musicInfoBean);
				fingerPrintingDataSource.insertHashes(fingerprint.toHashBeans(id));
			}
		}
	}

	@Override
	public MusicInfoWithConfidenceBean match(WAVBean wavBean) throws IOException {
		if (wavBean != null && wavBean.getWav().length > 0) {

			Fingerprint fingerprint = new FingerprintCreator().createFingerprint(config, wavBean, FRAME_MAX_LENGTH);

			Index index = new Index();
			index.setFingerPrintingClient(fingerPrintingDataSource);
			SongMatch songMatch = index.search(fingerprint, MIN_HIT);

			if (songMatch != null && songMatch.getIdSong() != -1) {
				SongNoBean songNoBean = new SongNoBean();
				songNoBean.setSongNo(songMatch.getIdSong());
				MusicInfoBean musicInfoBean = fingerPrintingDataSource.findTune(songNoBean);

				if (musicInfoBean != null) {
					MusicInfoWithConfidenceBean result = new MusicInfoWithConfidenceBean();
					result.setMusicInfo(musicInfoBean);
					result.setConfidence(songMatch.getCount());
					result.setRelativeConfidence(
							((double) songMatch.getCount() / fingerprint.getLinkList().size()) * 100);
					result.setOffset(songMatch.getTime());
					result.setOffsetSeconds(songMatch.getTime() * 0.03225806451612903);

					return result;
				}
			}
		}
		return null;
	}
}