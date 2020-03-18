package libsidutils.fingerprinting;

import java.nio.ByteBuffer;

import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.data.FingerprintedSampleData;
import libsidutils.fingerprinting.database.DBMatch;
import libsidutils.fingerprinting.database.Index;
import libsidutils.fingerprinting.database.MysqlDB;
import libsidutils.fingerprinting.model.SongMatch;

public class FingerPrinting {

	private static final int MIN_HIT = 15;

	private MysqlDB database = new MysqlDB();

	public void insert(ByteBuffer sampleData, SidTune tune, String recordingFilename) {
		if (sampleData.limit() > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(sampleData);
			fingerprintedSampleData.setMetaInfo(tune, recordingFilename);

			database.insert(fingerprintedSampleData, recordingFilename);
		}
	}

	public DBMatch match(ByteBuffer sampleData) {
		if (sampleData.limit() > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(sampleData);

			Index index = new Index();
			index.setDatabase(database);

			SongMatch songMatch = index.search(fingerprintedSampleData.getFingerprint(), MIN_HIT);

			if (songMatch != null && songMatch.getIdSong() != -1) {

				DBMatch result = database.getByID(songMatch.getIdSong());
				result.setSongMatch(fingerprintedSampleData, songMatch);

				return result;
			}
		}
		return null;
	}
}
