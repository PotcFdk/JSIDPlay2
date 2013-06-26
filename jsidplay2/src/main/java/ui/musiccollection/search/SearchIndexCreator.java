/**
 * 
 */
package ui.musiccollection.search;

import java.io.File;

import javax.persistence.EntityManager;

import ui.entities.collection.service.HVSCEntryService;
import ui.entities.collection.service.STILService;
import ui.entities.collection.service.VersionService;
import ui.entities.config.Configuration;

import libsidutils.PathUtils;

public final class SearchIndexCreator implements ISearchListener {

	private EntityManager em;
	private HVSCEntryService hvscEntryService;
	private STILService stilService;
	private VersionService versionService;

	protected Configuration config;
	private File root;

	public SearchIndexCreator(File root, final Configuration cfg,
			final EntityManager em) {
		this.root = root;
		this.config = cfg;
		this.em = em;
		this.hvscEntryService = new HVSCEntryService(em);
		this.stilService = new STILService(em);
		this.versionService = new VersionService(em);
	}

	@Override
	public void searchStart() {
		clearPreviousSearchIndex();

		em.getTransaction().begin();
	}

	@Override
	public void searchHit(final File matchFile) {
		try {
			String collectionRelName = PathUtils.getCollectionName(root,
					matchFile);
			if (collectionRelName != null) {
				hvscEntryService.add(config, root, collectionRelName, matchFile);
			}
		} catch (final Exception e) {
			System.err.println("Indexing failure on: "
					+ matchFile.getAbsolutePath() + ": " + e.getMessage());
		}
	}

	@Override
	public void searchStop(final boolean canceled) {
		versionService.setExpectedVersion();
		em.getTransaction().commit();
	}

	/**
	 * Clear current database.
	 */
	private void clearPreviousSearchIndex() {
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
	}

}