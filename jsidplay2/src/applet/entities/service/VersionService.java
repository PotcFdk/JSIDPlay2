package applet.entities.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import applet.entities.Version;

public class VersionService {
	private static final int CORRECT_VERSION = 1;

	private EntityManager em;

	public VersionService(EntityManager em) {
		this.em = em;
	};

	@SuppressWarnings("unchecked")
	public boolean isExpectedVersion() {
		try {
			Query q = em.createQuery("from Version");
			List<Version> list = q.getResultList();
			return list.size() != 0
					&& list.get(0).getVersion() == CORRECT_VERSION;
		} catch (Exception e) {
			// database corrupt?
			return false;
		}
	}

	public void setExpectedVersion() {
		Version version = new Version();
		version.setVersion(CORRECT_VERSION);
		em.persist(version);
	}

	public void clear() {
		em.createQuery("DELETE from Version").executeUpdate();
	}
}
