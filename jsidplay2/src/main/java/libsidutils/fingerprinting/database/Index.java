package libsidutils.fingerprinting.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import libsidutils.fingerprinting.fingerprint.Fingerprint;
import libsidutils.fingerprinting.fingerprint.Hash;
import libsidutils.fingerprinting.fingerprint.Link;
import libsidutils.fingerprinting.model.Match;
import libsidutils.fingerprinting.model.SongMatch;

/**
 * Created by hsyecheng on 2015/6/12.
 */
public class Index {

	private MysqlDB sqlDB;

	private long maxId;
	private int maxCount, maxTime;

	private HashMap<Long, Match> hashMap;

	public Index() {
		hashMap = new HashMap<>(400000);
		maxId = -1;
		maxCount = -1;
		maxTime = -1;
	}

	public void setDatabase(MysqlDB mysql) {
		sqlDB = mysql;
	}

	public SongMatch search(Fingerprint fp, int minHit) {
		ArrayList<Link> linkList = fp.getLinkList();
		int[] linkHash = new int[linkList.size()];
		int[] linkTime = new int[linkList.size()];
		for (int i = 0; i < linkHash.length; i++) {
			linkHash[i] = Hash.hash(linkList.get(i));
			linkTime[i] = linkList.get(i).getStart().getIntTime();
		}

		return search(linkTime, linkHash, minHit);
	}

	public SongMatch search(int[] linkTime, int[] linkHash, int minHit) {
		HashMap<Integer, Integer> linkHashMap = new HashMap<>(linkHash.length);
		for (int i = 0; i < linkHash.length; i++) {
			linkHashMap.put(linkHash[i], linkTime[i]);
		}

		try (ResultSet rs = sqlDB.searchAll(linkHash)) {

			if (rs == null) {
				return null;
			}
			while (rs.next()) {
				int hash = rs.getInt(2);
				int id = rs.getInt(3);
				int time = rs.getInt(4);

				Long idHash = idHash(id, linkHashMap.get(hash) - time);
				Match count = hashMap.get(idHash);
				if (count == null) {
					count = new Match(0, linkHashMap.get(hash) - time);
				}
				count.updateCount();
				hashMap.put(idHash, count);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return new SongMatch(-1, new Match(-1, -1));
		}

		hashMap.forEach((hash, countTime) -> {
			if (countTime.getCount() > minHit && countTime.getCount() > maxCount) {
				maxId = hash;
				maxCount = countTime.getCount();
				maxTime = countTime.getTime();
			}
		});
		Integer offset = -maxTime;
		return new SongMatch(Hash2id(maxId), new Match(maxCount, offset));
	}

	public static Long idHash(int id, int time) {
		return (long) ((id << 16) + time + (1 << 15));
	}

	public static int Hash2id(Long idHash) {
		return (int) (idHash >> 16);
	}
}
