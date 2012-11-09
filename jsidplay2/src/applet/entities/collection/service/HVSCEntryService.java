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
import javax.persistence.metamodel.SingularAttribute;

import sidplay.ini.intf.IConfig;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.StilEntry;

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

	@SuppressWarnings("unchecked")
	public HVSCEntries search(SingularAttribute<?, ?> field, Object fieldValue,
			boolean caseSensitive, boolean fForward) {
		Class<?> entityType = field.getDeclaringType().getJavaType();
		Class<?> fieldType = field.getJavaType();

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> q = cb.createQuery(String.class);
		Predicate like;

		if (entityType == HVSCEntry.class) {
			Root<HVSCEntry> h = q.from(HVSCEntry.class);
			Path<String> path = h.get("path");
			q.select(path).distinct(true);
			if (fieldType == Date.class) {
				assert (fieldValue.getClass() == Date.class);
				// SELECT distinct h.path FROM HVSCEntry h WHERE
				// YEAR(h.<fieldName>) = <value>
				Path<Date> fieldName = h
						.get((SingularAttribute<HVSCEntry, Date>) field);
				Expression<Integer> year = cb.function("YEAR", Integer.class,
						fieldName);
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) fieldValue);
				like = cb.equal(year, cal.get(Calendar.YEAR));
			} else if (fieldType == String.class) {
				assert (fieldValue.getClass() == String.class);
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName>
				// LIKE <value>
				Path<String> fieldName = h
						.get((SingularAttribute<HVSCEntry, String>) field);
				like = fieldNameLikeFieldValue(cb, fieldName,
						fieldValue.toString(), caseSensitive, fieldType);
			} else {
				// SELECT distinct h.path FROM HVSCEntry h WHERE h.<fieldName> =
				// <value>
				Path<Object> fieldName = h
						.get((SingularAttribute<HVSCEntry, Object>) field);
				like = cb.equal(fieldName, fieldValue);
			}
		} else {
			if (entityType == StilEntry.class) {
				assert (fieldValue.getClass() == String.class);
				// SELECT distinct h.path FROM STIL s INNER JOIN s.hvscEntry h
				Root<StilEntry> s = q.from(StilEntry.class);
				Join<applet.entities.collection.StilEntry, HVSCEntry> h = s
						.join("hvscEntry", JoinType.INNER);
				Path<String> path = h.get("path");
				q.select(path).distinct(true);

				if (fieldType == String.class) {
					assert (fieldValue.getClass() == String.class);
					Path<String> fieldName = s
							.get((SingularAttribute<StilEntry, String>) field);
					like = fieldNameLikeFieldValue(cb, fieldName,
							fieldValue.toString(), caseSensitive, fieldType);
				} else {
					throw new RuntimeException("Unsupported field type: "
							+ fieldType);
				}
			} else {
				throw new RuntimeException(
						"Unsupported entity type for search: " + entityType);
			}
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

	public void clear() {
		em.createQuery("DELETE from HVSCEntry").executeUpdate();
	}

}
