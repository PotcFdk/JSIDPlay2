package applet.entities.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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

	public HVSCEntries search(int field, String fieldValue,
			boolean caseSensitive, boolean fForward) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> q = cb.createQuery(String.class);

		if (field == 0) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.name LIKE <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h.get("name");
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive);
			q.where(like);
		} else if (field == 1) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.path LIKE <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h.get("path");
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive);
			q.where(like);
		} else if (field - 2 < SidTuneInfoCache.SIDTUNE_INFOS.length) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName> LIKE
			// <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h
					.get(convertSearchCriteriaToEntityFieldName(SidTuneInfoCache.SIDTUNE_INFOS[field - 2]));
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive);
			q.where(like);
		} else if (field - 2 < SidTuneInfoCache.SIDTUNE_INFOS.length
				+ STIL.STIL_INFOS.length) {
			int stilIdx = field - 2 - SidTuneInfoCache.SIDTUNE_INFOS.length;
			if (stilIdx == 0) {
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName>
				// LIKE <value>
				Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
				Path<String> fieldName = h
						.get(convertSearchCriteriaToEntityFieldName(STIL.STIL_INFOS[stilIdx]));
				Predicate like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue, caseSensitive);
				q.where(like);
			} else {
				// SELECT distinct h.path FROM HVSCEntry h INNER JOIN h.stil s
				// WHERE s.<fieldName> Like <value>
				Root<HVSCEntry> person = q.from(HVSCEntry.class);
				Path<String> path = person.get("path");
				q.select(path).distinct(true);
				Join<HVSCEntry, applet.entities.STIL> stil = person.join(
						"stil", JoinType.INNER);
				Path<String> fieldName = stil
						.<String> get(convertSearchCriteriaToEntityFieldName(STIL.STIL_INFOS[stilIdx]));
				Predicate like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue, caseSensitive);
				q.where(like);
			}
		} else {
			throw new RuntimeException("Search criteria is not supported: "
					+ field);
		}
		return new HVSCEntries(em.createQuery(q).getResultList(), fForward);
	}

	private Root<HVSCEntry> selectDistinctPathFromHVSCEntry(
			CriteriaQuery<String> query) {
		Root<HVSCEntry> h = query.from(HVSCEntry.class);
		Path<String> path = h.get("path");
		query.select(path).distinct(true);
		return h;
	}

	private Predicate fieldNameLikeFieldValue(CriteriaBuilder cb,
			Path<String> fieldNm, String fieldValue, boolean caseSensitive) {
		Predicate like;
		if (!caseSensitive) {
			like = cb.like(cb.lower(fieldNm), "%" + fieldValue.toLowerCase()
					+ "%");
		} else {
			like = cb.like(fieldNm, "%" + fieldValue + "%");
		}
		return like;
	}

	private String convertSearchCriteriaToEntityFieldName(String fieldName) {
		String[] fieldNameParts = fieldName.toLowerCase().split("_");
		StringBuilder fieldNameResult = new StringBuilder();
		boolean first = true;
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
		return fieldNameResult.toString();
	}

	public void clear() {
		em.createQuery("DELETE from HVSCEntry").executeUpdate();
	}

}
