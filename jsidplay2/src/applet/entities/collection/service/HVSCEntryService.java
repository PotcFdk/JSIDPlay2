package applet.entities.collection.service;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
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
import applet.entities.collection.STIL;

public class HVSCEntryService {
	public static final String[] SEARCH_FIELDS = new String[] { "NAME", "PATH",
			"TITLE", "AUTHOR", "RELEASED", "FORMAT", "PLAYER_ID",
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

	public HVSCEntries search(int fieldIdx, Object fieldValue,
			boolean caseSensitive, boolean fForward) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> q = cb.createQuery(String.class);
		Predicate like;

		if (fieldIdx < 29) {
			// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName> LIKE
			// <value>
			Root<HVSCEntry> h = q.from(HVSCEntry.class);
			Path<String> path = h.get("path");
			q.select(path).distinct(true);
			if (SEARCH_FIELD_TYPES[fieldIdx] == Date.class) {
				assert (fieldValue.getClass() == Date.class);
				// for dates: compare year, only
				Path<Date> fieldName = h
						.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
				Expression<Integer> year = cb.function("YEAR", Integer.class,
						fieldName);
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) fieldValue);
				like = cb.equal(year, cal.get(Calendar.YEAR));
			} else if (SEARCH_FIELD_TYPES[fieldIdx] == String.class) {
				assert (fieldValue.getClass() == String.class);
				Path<String> fieldName = h
						.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
				like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue.toString(), caseSensitive,
						SEARCH_FIELD_TYPES[fieldIdx]);
			} else {
				Path<Object> fieldName = h
						.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
				like = cb.equal(fieldName, fieldValue);
			}
		} else if (fieldIdx < SEARCH_FIELDS.length) {
			assert (fieldValue.getClass() == String.class);
			// SELECT distinct h.path FROM STIL s INNER JOIN s.hvscEntry h
			Root<STIL> s = q.from(STIL.class);
			Join<applet.entities.collection.STIL, HVSCEntry> h = s.join(
					"hvscEntry", JoinType.INNER);
			Path<String> path = h.get("path");
			q.select(path).distinct(true);

			Path<String> fieldName = s
					.get(convertSearchCriteriaToEntityFieldName(SEARCH_FIELDS[fieldIdx]));
			like = fieldNameLikeFieldValue(cb, fieldName,
					fieldValue.toString(), caseSensitive,
					SEARCH_FIELD_TYPES[fieldIdx]);
		} else {
			throw new RuntimeException("Search criteria is not supported: "
					+ fieldIdx);
		}
		return new HVSCEntries(em.createQuery(q.where(like)).getResultList(),
				fForward);
	}

	private Predicate fieldNameLikeFieldValue(CriteriaBuilder cb,
			Path<String> fieldNm, String fieldValue, boolean caseSensitive,
			Class<?> type) {
		if (!caseSensitive) {
			return cb.like(cb.lower(fieldNm), "%" + fieldValue.toLowerCase()
					+ "%");
		} else {
			return cb.like(fieldNm, "%" + fieldValue + "%");
		}
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
