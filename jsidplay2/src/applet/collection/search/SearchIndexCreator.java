/**
 * 
 */
package applet.collection.search;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;

import sidplay.ini.intf.IConfig;
import applet.entities.collection.service.HVSCEntryService;
import applet.entities.collection.service.STILService;
import applet.entities.collection.service.VersionService;

public final class SearchIndexCreator implements ISearchListener {

	private EntityManager em;
	private HVSCEntryService hvscEntryService;
	private STILService stilService;
	private VersionService versionService;

	protected IConfig config;
	private File root;

	public SearchIndexCreator(File root, final IConfig cfg,
			final EntityManager em) {
		this.root = root;
		this.config = cfg;
		this.em = em;
		this.hvscEntryService = new HVSCEntryService(em);
		this.stilService = new STILService(em);
		this.versionService = new VersionService(em);
	}

	public void searchStart() {
		// Clear current database
		em.getTransaction().begin();
		try {
			versionService.clear();
			stilService.clear();
			hvscEntryService.clear();
			em.getTransaction().commit();
			em.clear();
		} catch (Throwable e) {
			e.printStackTrace();
			em.getTransaction().rollback();
		}

		em.getTransaction().begin();
	}

	public void searchHit(final File matchFile) {
		try {
			hvscEntryService.add(config, root, matchFile);
		} catch (final IOException e) {
			System.err.println("Indexing failure on: "
					+ matchFile.getAbsolutePath() + ": " + e.getMessage());
		}
	}

	public void searchStop(final boolean canceled) {
		versionService.setExpectedVersion();
		em.getTransaction().commit();
	}
}