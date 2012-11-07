package applet.entities.collection.service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;
import sidplay.ini.intf.IConfig;
import applet.entities.collection.HVSCEntry;

public class HVSCEntryService {
	public static final String[] SEARCH_FIELDS = new String[] { "FILE_NAME",
			"FULL_PATH", "TITLE", "AUTHOR", "RELEASED", "FORMAT", "PLAYER_ID",
			"NO_OF_SONGS", "START_SONG", "CLOCK_FREQ", "SPEED", "SID_MODEL_1",
			"SID_MODEL_2", "COMPATIBILITY", "TUNE_LENGTH", "AUDIO",
			"SID_CHIP_BASE_1", "SID_CHIP_BASE_2", "DRIVER_ADDRESS",
			"LOAD_ADDRESS", "LOAD_LENGTH", "INIT_ADDRESS", "PLAYER_ADDRESS",
			"FILE_DATE", "FILE_SIZE_KB", "TUNE_SIZE_B", "RELOC_START_PAGE",
			"RELOC_NO_PAGES", "STIL_GLB_COMMENT", "STIL_NAME", "STIL_AUTHOR",
			"STIL_TITLE", "STIL_ARTIST", "STIL_COMMENT" };

	public static Class<?> SEARCH_FIELD_TYPES[] = new Class[] { String.class,
			String.class, String.class, String.class, String.class,
			String.class, String.class, Integer.class, Integer.class,
			Clock.class, Speed.class, Model.class, Model.class,
			Compatibility.class, Long.class, String.class, Integer.class,
			Integer.class, Integer.class, Integer.class, Integer.class,
			Integer.class, Integer.class, Date.class, Integer.class,
			Integer.class, Short.class, Short.class, String.class,
			String.class, String.class, String.class, String.class,
			String.class, };

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

	public HVSCEntries search(int fieldIdx, String fieldValue,
			boolean caseSensitive, boolean fForward) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> q = cb.createQuery(String.class);

		if (fieldIdx == 0) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.name LIKE <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h.get("name");
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive, SEARCH_FIELD_TYPES[fieldIdx]);
			q.where(like);
		} else if (fieldIdx == 1) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.path LIKE <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h.get("path");
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive, SEARCH_FIELD_TYPES[fieldIdx]);
			q.where(like);
		} else if (fieldIdx < 28) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName> LIKE
			// <value>
			Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
			Path<String> fieldName = h
					.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
			Predicate like = fieldNameLikeFieldValue(cb, fieldName, fieldValue,
					caseSensitive, SEARCH_FIELD_TYPES[fieldIdx]);
			q.where(like);
		} else if (fieldIdx < SEARCH_FIELDS.length) {
			if (SEARCH_FIELDS[fieldIdx].equals("STIL_GLB_COMMENT")) {
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName>
				// LIKE <value>
				Root<HVSCEntry> h = selectDistinctPathFromHVSCEntry(q);
				Path<String> fieldName = h
						.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
				Predicate like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue, caseSensitive, SEARCH_FIELD_TYPES[fieldIdx]);
				q.where(like);
			} else {
				// SELECT distinct h.path FROM HVSCEntry h INNER JOIN h.stil s
				// WHERE s.<fieldName> Like <value>
				Root<HVSCEntry> person = selectDistinctPathFromHVSCEntry(q);
				Join<HVSCEntry, applet.entities.collection.STIL> stil = person
						.join("stil", JoinType.INNER);
				Path<String> fieldName = stil
						.<String> get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
				Predicate like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue, caseSensitive, SEARCH_FIELD_TYPES[fieldIdx]);
				q.where(like);
			}
		} else {
			throw new RuntimeException("Search criteria is not supported: "
					+ fieldIdx);
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
			Path<String> fieldNm, String fieldValue, boolean caseSensitive,
			Class<?> type) {
		Predicate predicate;
		if (type == String.class) {
			if (!caseSensitive) {
				predicate = cb.like(cb.lower(fieldNm),
						"%" + fieldValue.toLowerCase() + "%");
			} else {
				predicate = cb.like(fieldNm, "%" + fieldValue + "%");
			}
		} else if (type == Short.class) {
			predicate = cb.equal(fieldNm, Short.valueOf(fieldValue));
		} else if (type == Integer.class) {
			predicate = cb.equal(fieldNm, Integer.valueOf(fieldValue));
		} else if (type == Long.class) {
			predicate = cb.equal(fieldNm, Long.valueOf(fieldValue));
		} else if (Enum.class.isAssignableFrom(type)) {
			if (type == Clock.class) {
				predicate = cb.equal(fieldNm, Clock.valueOf(fieldValue));
			} else if (type == Speed.class) {
				predicate = cb.equal(fieldNm, Speed.valueOf(fieldValue));
			} else if (type == Model.class) {
				predicate = cb.equal(fieldNm, Model.valueOf(fieldValue));
			} else if (type == Compatibility.class) {
				predicate = cb
						.equal(fieldNm, Compatibility.valueOf(fieldValue));
			} else {
				throw new RuntimeException("Enum type is not supported: "
						+ type);
			}
		} else if (type == Date.class) {
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				Date date = simpleDateFormat.parse(fieldValue);
				predicate = cb.equal(fieldNm, date);
			} catch (ParseException e) {
				e.printStackTrace();
				System.err
						.println("Illegal Date Format (yyyy-MM-dd HH:mm:ss): "
								+ fieldValue);
				predicate = cb.equal(fieldNm, new Date());
			}
		} else {
			throw new RuntimeException("Search type is not supported: " + type);
		}
		return predicate;
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
