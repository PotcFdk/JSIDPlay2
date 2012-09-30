package applet.entities.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.IniConfig;
import applet.entities.HVSCEntry;
import applet.sidtuneinfo.SidTuneInfoCache;

public class HVSCEntryService {
	public class HVSCEntries {
		HVSCEntries(List<String> list, boolean fForward) {
			this.list = list;
			this.listIdx = (fForward ? -1 : list.size());
		}

		private int listIdx;
		private List<String> list;

		public boolean prev() {
			if (listIdx - 1 < 0) {
				return false;
			}
			listIdx = listIdx - 1;
			return true;
		}

		public boolean next() {
			if (listIdx + 1 == list.size()) {
				return false;
			}
			listIdx = listIdx + 1;
			return true;
		}

		public String getPath() {
			return list.get(listIdx);
		}

	}

	private EntityManager em;
	private STILService stilService;

	public HVSCEntryService(EntityManager em) {
		this.em = em;
		this.stilService = new STILService(em);
	};

	public HVSCEntry add(final IniConfig config, final File root,
			final File tuneFile) throws IOException {
		HVSCEntry hvscEntry = new HVSCEntry();
		hvscEntry.setPath(makeRelative(root, tuneFile));
		hvscEntry.setName(tuneFile.getName());
		try {
			final SidTune tune = SidTune.load(tuneFile);
			if (tune != null) {
				tune.selectSong(1);
				SidTuneInfo info = tune.getInfo();

				hvscEntry.setTitle(info.infoString[0]);
				hvscEntry.setAuthor(info.infoString[1]);
				hvscEntry.setReleased(info.infoString[2]);
				hvscEntry.setFormat(tune.getClass().getSimpleName());
				hvscEntry.setPlayerId(getPlayer(tune));
				hvscEntry.setNoOfSongs(info.songs);
				hvscEntry.setStartSong(info.startSong);
				hvscEntry.setClockFreq(info.clockSpeed);
				hvscEntry.setSpeed(tune.getSongSpeed(1));
				hvscEntry.setCompatibility(info.compatibility);
				hvscEntry.setTuneLength(getTuneLength(config, tune));
				hvscEntry.setAudio(getAudio(info.sidChipBase2));
				hvscEntry.setSidChipBase1(info.sidChipBase1);
				hvscEntry.setSidChipBase2(info.sidChipBase2);
				hvscEntry.setDriverAddress(info.determinedDriverAddr);
				hvscEntry.setLoadAddress(info.loadAddr);
				hvscEntry.setLoadLength(info.c64dataLen);
				hvscEntry.setInitAddress(info.initAddr);
				hvscEntry.setPlayerAddress(info.playAddr);
				hvscEntry.setFileDate(new Date(tuneFile.lastModified()));
				hvscEntry.setFileSizeKb(tuneFile.length());
				hvscEntry.setTuneLength(tuneFile.length());
				hvscEntry.setRelocStartPage(info.relocStartPage);
				hvscEntry.setRelocNoPages(info.relocPages);
			}
		} catch (Exception e) {
			// Ignore invalid tunes!
		}

		stilService.add(config, root, tuneFile, hvscEntry);

		try {
			em.persist(hvscEntry);
		} catch (Throwable e) {
			System.err.println("Tune: " + tuneFile.getAbsolutePath());
			System.err.println(e.getMessage());
		}
		return hvscEntry;
	}

	private String makeRelative(final File root, final File matchFile)
			throws IOException {
		if (matchFile instanceof ZipEntryFileProxy) {
			final String path = ((ZipEntryFileProxy) matchFile).getPath();
			return path.substring(path.indexOf('/') + 1);
		}
		final String canonicalPath = matchFile.getCanonicalPath();
		final String rootCanonicalPath = root.getCanonicalPath();
		if (canonicalPath.startsWith(rootCanonicalPath)) {
			final String name = canonicalPath.substring(
					rootCanonicalPath.length())
					.replace(File.separatorChar, '/');
			if (name.startsWith("/")) {
				return name.substring(1);
			}
		}
		return matchFile.getCanonicalPath();
	}

	private String getPlayer(SidTune tune) {
		StringBuilder ids = new StringBuilder();
		for (String s : tune.identify()) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(s);
		}
		return ids.toString();
	}

	private long getTuneLength(final IniConfig config, final SidTune tune) {
		final SidDatabase sldb = SidDatabase.getInstance(config.sidplay2()
				.getHvsc());
		long fullLength;
		if (sldb != null) {
			fullLength = sldb.getFullSongLength(tune);
		} else {
			fullLength = 0;
		}
		return fullLength;
	}

	private String getAudio(int sidChipBase2) {
		return sidChipBase2 != 0 ? "Stereo" : "Mono";
	}

	@SuppressWarnings("unchecked")
	public HVSCEntries search(int field, String fieldValue,
			boolean caseSensitive, boolean fForward) {
		String fieldName;
		String from;
		String where;
		if (field == 0) {
			fieldName = "h.name";
			from = "HVSCEntry AS h";
			where = "";
		} else if (field == 1) {
			fieldName = "h.path";
			from = "HVSCEntry AS h";
			where = "";
		} else if (field - 2 < SidTuneInfoCache.SIDTUNE_INFOS.length) {
			fieldName = "h." + SidTuneInfoCache.SIDTUNE_INFOS[field - 2];
			from = "HVSCEntry AS h";
			where = "";
		} else if (field - 2 < SidTuneInfoCache.SIDTUNE_INFOS.length
				+ STIL.STIL_INFOS.length) {
			int stilIdx = field - 2 - SidTuneInfoCache.SIDTUNE_INFOS.length;
			if (stilIdx == 0) {
				fieldName = "h." + STIL.STIL_INFOS[stilIdx];
				from = "HVSCEntry AS h";
				where = "";
			} else {
				fieldName = "s." + STIL.STIL_INFOS[stilIdx];
				from = "HVSCEntry AS h, STIL AS s";
				where = "h.path=s.hvscEntry.path and ";
			}
		} else {
			throw new RuntimeException("Search criteria is not supported: "
					+ field);
		}
		String selectFromWhere = "select distinct h.path from " + from
				+ " where " + where;
		fieldName = convertSearchCriteriaToEntityFieldName(fieldName);

		String queryString;
		if (!caseSensitive) {
			queryString = selectFromWhere + " lower(" + fieldName
					+ ") Like :value";
			fieldValue = "%" + fieldValue.toLowerCase() + "%";
		} else {
			queryString = selectFromWhere + " " + fieldName + " Like :value";
			fieldValue = "%" + fieldValue + "%";
		}
		Query q = em.createQuery(queryString);
		q.setParameter("value", fieldValue);
		return new HVSCEntries(q.getResultList(), fForward);
	}

	private String convertSearchCriteriaToEntityFieldName(String fieldName) {
		StringBuilder fieldNameResult = new StringBuilder();
		boolean first = true;
		String[] fieldNameParts = fieldName.toLowerCase().split("_");
		for (String fieldNamePart : fieldNameParts) {
			if (!first) {
				fieldNameResult.append(
						Character.toUpperCase(fieldNamePart.charAt(0))).append(
						fieldNamePart.substring(1));
			} else {
				fieldNameResult.append(fieldNamePart);
			}
			first = false;
		}
		fieldName = fieldNameResult.toString();
		return fieldName;
	}

	public void clear() {
		em.createQuery("DELETE from HVSCEntry").executeUpdate();
	}

}
