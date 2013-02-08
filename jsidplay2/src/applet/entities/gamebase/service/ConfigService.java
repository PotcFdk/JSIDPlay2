package applet.entities.gamebase.service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import applet.entities.gamebase.Config;

public class ConfigService {

	private static final int EXPCT_MAJOR_VERSION = 2;
	private static final int EXPCT_MINOR_VERSION = 8;
	private static final int EXPCT_OFFICIAL_UPDATE = 9;

	private EntityManager em;

	public ConfigService(EntityManager em) {
		this.em = em;
	};

	public boolean checkVersion() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Config> query = cb.createQuery(Config.class);
		query.select(query.from(Config.class));
		Config cfg = em.createQuery(query).getSingleResult();
		return cfg.getMajorVersion() == EXPCT_MAJOR_VERSION
				&& cfg.getMinorVersion() == EXPCT_MINOR_VERSION
				&& cfg.getOfficialUpdate() == EXPCT_OFFICIAL_UPDATE;
	}
}
