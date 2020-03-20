package libsidutils.fingerprinting;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.fingerprint.Fingerprint;
import libsidutils.fingerprinting.fingerprint.Hash;
import libsidutils.fingerprinting.fingerprint.Link;
import libsidutils.fingerprinting.model.FingerprintedSampleData;
import libsidutils.fingerprinting.model.MusicInfoWithConfidence;
import libsidutils.fingerprinting.model.SongMatch;
import libsidutils.fingerprinting.rest.FingerPrintingApi;
import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import libsidutils.fingerprinting.rest.client.FingerprintingClient;

public class FingerPrinting {

	private static final int MIN_HIT = 15;

	private FingerPrintingApi fingerPrintingClient = new FingerprintingClient();

	public void insert(ByteBuffer sampleData, SidTune tune, String recordingFilename) {
		if (sampleData.limit() > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(sampleData);
			fingerprintedSampleData.setMetaInfo(tune, recordingFilename);

			MusicInfoBean musicInfoBean = new MusicInfoBean();
			musicInfoBean.setTitle(fingerprintedSampleData.getTitle());
			musicInfoBean.setArtist(fingerprintedSampleData.getArtist());
			musicInfoBean.setAlbum(fingerprintedSampleData.getAlbum());
			musicInfoBean.setFileDir(recordingFilename);
			musicInfoBean.setInfoDir("");
			musicInfoBean.setAudioLength(fingerprintedSampleData.getAudioLength());

			IdBean id = fingerPrintingClient.insertTune(musicInfoBean);

			HashBeans hashBeans = new HashBeans();
			hashBeans.setHashes(new ArrayList<>());

			Fingerprint fp = fingerprintedSampleData.getFingerprint();
			for (Link link : fp.getLinkList()) {
				HashBean hashBean = new HashBean();
				hashBean.setHash(Hash.hash(link));
				hashBean.setId(id.getId());
				hashBean.setTime(link.getStart().getIntTime());
				hashBeans.getHashes().add(hashBean);
			}
			fingerPrintingClient.insertHashes(hashBeans);
		}
	}

	public MusicInfoWithConfidence match(ByteBuffer sampleData) {
		if (sampleData.limit() > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(sampleData);

			Index index = new Index();
			index.setFingerPrintingClient(fingerPrintingClient);

			SongMatch songMatch = index.search(fingerprintedSampleData.getFingerprint(), MIN_HIT);

			if (songMatch != null && songMatch.getIdSong() != -1) {

				SongNoBean songNoBean = new SongNoBean();
				songNoBean.setSongNo(songMatch.getIdSong());
				MusicInfoBean musicInfoBean = fingerPrintingClient.findTune(songNoBean);
				MusicInfoWithConfidence result = new MusicInfoWithConfidence();
				result.setMusicInfoBean(musicInfoBean);
				result.setSongMatch(fingerprintedSampleData, songMatch);

				return result;
			}
		}
		return null;
	}
}
