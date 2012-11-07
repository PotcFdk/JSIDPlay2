package applet.entities.collection.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import libsidutils.STIL;
import sidplay.ini.intf.IConfig;
import applet.entities.collection.HVSCEntry;
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

	public HVSCEntry add(final IConfig config, final String path,
			final File tuneFile) throws IOException {
		HVSCEntry hvscEntry = HVSCEntry.create(config, path, tuneFile);

		stilService.add(config, tuneFile, hvscEntry);

		try {
			em.persist(hvscEntry);
		} catch (Throwable e) {
			System.err.println("Tune: " + tuneFile.getAbsolutePath());
			System.err.println(e.getMessage());
		}
		return hvscEntry;
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
				Join<HVSCEntry, applet.entities.collection.STIL> stil = person
						.join("stil", JoinType.INNER);
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
