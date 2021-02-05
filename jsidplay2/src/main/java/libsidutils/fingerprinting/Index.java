package libsidutils.fingerprinting;

import java.util.ArrayList;
import java.util.HashMap;

import libsidutils.fingerprinting.fingerprint.Fingerprint;
import libsidutils.fingerprinting.fingerprint.Hash;
import libsidutils.fingerprinting.fingerprint.Link;
import libsidutils.fingerprinting.model.Match;
import libsidutils.fingerprinting.model.SongMatch;
import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;

/**
 * Created by hsyecheng on 2015/6/12.
 */
public class Index {

	private FingerPrintingDataSource fingerPrintingDataSource;

	private long maxId;
	private int maxCount, maxTime;

	private HashMap<Long, Match> hashMap;

	public Index() {
		hashMap = new HashMap<>(400000);
		maxId = -1;
		maxCount = -1;
		maxTime = -1;
	}

	public void setFingerPrintingClient(FingerPrintingDataSource fingerPrintingClient) {
		this.fingerPrintingDataSource = fingerPrintingClient;
	}

	public SongMatch search(Fingerprint fp, int minHit) {
		ArrayList<Link> linkList = fp.getLinkList();
		Integer[] linkHash = new Integer[linkList.size()];
		Integer[] linkTime = new Integer[linkList.size()];
		for (int i = 0; i < linkHash.length; i++) {
			linkHash[i] = Hash.hash(linkList.get(i));
			linkTime[i] = linkList.get(i).getStart().getIntTime();
		}

		return search(linkTime, linkHash, minHit);
	}

	private SongMatch search(Integer[] linkTime, Integer[] linkHash, int minHit) {
		HashMap<Integer, Integer> linkHashMap = new HashMap<>(linkHash.length);
		for (int i = 0; i < linkHash.length; i++) {
			linkHashMap.put(linkHash[i], linkTime[i]);
		}

		IntArrayBean intArray = new IntArrayBean(linkHash);
		HashBeans res = fingerPrintingDataSource.findHashes(intArray);
		for (HashBean hashBean : res.getHashes()) {
			int hash = hashBean.getHash();
			int id = hashBean.getId();
			int time = hashBean.getTime();

			Long idHash = idHash(id, linkHashMap.get(hash) - time);
			Match count = hashMap.get(idHash);
			if (count == null) {
				count = new Match(0, linkHashMap.get(hash) - time);
			}
			count.updateCount();
			hashMap.put(idHash, count);
		}

		hashMap.forEach((hash, countTime) -> {
			if (countTime.getCount() > minHit && countTime.getCount() > maxCount) {
				maxId = hash;
				maxCount = countTime.getCount();
				maxTime = countTime.getTime();
			}
		});
		Integer offset = -maxTime;
		return new SongMatch(hash2id(maxId), maxCount, offset);
	}

	private static Long idHash(int id, int time) {
		return ((long) id << 16) + time + (1 << 15);
	}

	private static int hash2id(Long idHash) {
		return (int) (idHash >> 16);
	}

}
