package ui.entities.collection.service;

import static ui.entities.HSQLBuiltInFunctions.DAY_OF_MONTH;
import static ui.entities.HSQLBuiltInFunctions.MONTH;
import static ui.entities.HSQLBuiltInFunctions.YEAR;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.StilEntry;
import ui.entities.collection.StilEntry_;

public class HVSCEntryService {

	public class HVSCEntries {
		HVSCEntries(List<String> list, boolean fForward) {
			this.list = list;
			this.listIdx = fForward ? -1 : list.size();
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

	public HVSCEntryService(EntityManager em) {
		this.em = em;
	}

	public HVSCEntry add(Player player, final String path, final File tuneFile) throws IOException, SidTuneError {
		SidTune tune = tuneFile.isFile() ? SidTune.load(tuneFile) : null;
		HVSCEntry hvscEntry = new HVSCEntry(() -> player.getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.), path,
				tuneFile, tune);
		try {
			em.persist(hvscEntry);
		} catch (Throwable e) {
			System.err.println("Tune: " + path);
			System.err.println(e.getMessage());
		}
		return hvscEntry;
	}

	@SuppressWarnings("unchecked")
	public HVSCEntries search(SingularAttribute<?, ?> field, Object fieldValue, boolean caseSensitive,
			boolean fForward) {
		Class<?> entityType = field.getDeclaringType().getJavaType();
		Class<?> fieldType = field.getJavaType();

		assert entityType == HVSCEntry.class || entityType == StilEntry.class;

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> q = cb.createQuery(String.class);
		Predicate predicate;

		if (entityType == HVSCEntry.class) {
			Root<HVSCEntry> h = q.from(HVSCEntry.class);
			Path<String> path = h.get(HVSCEntry_.path);
			q.select(path).distinct(true);
			if (fieldType == LocalDateTime.class) {
				assert fieldValue.getClass() == LocalDateTime.class;
				Path<LocalDateTime> fieldName = h.get((SingularAttribute<HVSCEntry, LocalDateTime>) field);
				if (fieldValue instanceof LocalDate) {
					// SELECT distinct h.path FROM HVSCEntry h WHERE
					// YEAR(h.<fieldName>) = <value> and MONTH(h.<fieldName>) = <value> and
					// DAY(h.<fieldName>) = <value>
					LocalDate localDate = (LocalDate) fieldValue;
					Expression<Integer> yearFunction = cb.function(YEAR, Integer.class, fieldName);
					Expression<Integer> monthFunction = cb.function(MONTH, Integer.class, fieldName);
					Expression<Integer> dayFunction = cb.function(DAY_OF_MONTH, Integer.class, fieldName);
					predicate = cb.and(cb.equal(yearFunction, localDate.getYear()),
							cb.equal(monthFunction, localDate.getMonthValue()),
							cb.equal(dayFunction, localDate.getDayOfMonth()));
				} else if (fieldValue instanceof YearMonth) {
					// SELECT distinct h.path FROM HVSCEntry h WHERE
					// YEAR(h.<fieldName>) = <value> and MONTH(h.<fieldName>) = <value>
					YearMonth yearMonth = (YearMonth) fieldValue;
					Expression<Integer> yearFunction = cb.function(YEAR, Integer.class, fieldName);
					Expression<Integer> monthFunction = cb.function(MONTH, Integer.class, fieldName);
					predicate = cb.and(cb.equal(yearFunction, yearMonth.getYear()),
							cb.equal(monthFunction, yearMonth.getMonthValue()));
				} else if (fieldValue instanceof Year) {
					// SELECT distinct h.path FROM HVSCEntry h WHERE
					// YEAR(h.<fieldName>) = <value>
					Expression<Integer> yearFunction = cb.function(YEAR, Integer.class, fieldName);
					Year year = (Year) fieldValue;
					predicate = cb.equal(yearFunction, year.getValue());
				} else {
					// SELECT distinct h.path FROM HVSCEntry h
					predicate = cb.and();
				}
			} else if (fieldType == String.class) {
				assert fieldValue.getClass() == String.class;
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName>
				// LIKE <value>
				Path<String> fieldName = h.get((SingularAttribute<HVSCEntry, String>) field);
				predicate = fieldNameLikeFieldValue(cb, fieldName, fieldValue.toString(), caseSensitive, fieldType);
			} else {
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName> =
				// <value>
				Path<Object> fieldName = h.get((SingularAttribute<HVSCEntry, Object>) field);
				predicate = cb.equal(fieldName, fieldValue);
			}
		} else {
			// if (entityType == StilEntry.class)
			assert fieldValue.getClass() == String.class;
			Root<StilEntry> s = q.from(StilEntry.class);
			Join<ui.entities.collection.StilEntry, HVSCEntry> h = s.join(StilEntry_.hvscEntry, JoinType.INNER);
			Path<String> path = h.get(HVSCEntry_.path);
			q.select(path).distinct(true);

			if (fieldType == String.class) {
				assert fieldValue.getClass() == String.class;
				// SELECT distinct h.path FROM STIL s INNER JOIN s.hvscEntry h
				// WHERE s.<fieldName> LIKE <value>
				Path<String> fieldName = s.get((SingularAttribute<StilEntry, String>) field);
				predicate = fieldNameLikeFieldValue(cb, fieldName, fieldValue.toString(), caseSensitive, fieldType);
			} else {
				// SELECT distinct h.path FROM STIL s INNER JOIN s.hvscEntry h
				// WHERE s.<fieldName> = <value>
				Path<Object> fieldName = s.get((SingularAttribute<StilEntry, Object>) field);
				predicate = cb.equal(fieldName, fieldValue);
			}
		}
		return new HVSCEntries(em.createQuery(q.where(predicate)).getResultList(), fForward);
	}

	private Predicate fieldNameLikeFieldValue(CriteriaBuilder cb, Path<String> fieldNm, String fieldValue,
			boolean caseSensitive, Class<?> type) {
		if (!caseSensitive) {
			return cb.like(cb.lower(fieldNm), "%" + fieldValue.toLowerCase(Locale.GERMAN) + "%");
		} else {
			return cb.like(fieldNm, "%" + fieldValue + "%");
		}
	}

	public void clear() {
		em.createQuery("DELETE from HVSCEntry").executeUpdate();
	}

}
