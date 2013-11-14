/**
 * 
 */
package ui.musiccollection.search;

import java.io.File;

import javax.persistence.EntityManager;

import libsidutils.PathUtils;
import sidplay.ConsolePlayer;
import ui.entities.collection.service.HVSCEntryService;
import ui.entities.collection.service.STILService;
import ui.entities.collection.service.VersionService;

public final class SearchIndexCreator implements ISearchListener {

	private EntityManager em;
	private HVSCEntryService hvscEntryService;
	private STILService stilService;
	private VersionService versionService;

	private ConsolePlayer cp;
	private File root;

	public SearchIndexCreator(File root, final ConsolePlayer cp,
			final EntityManager em) {
		this.root = root;
		this.cp = cp;
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
		if (!matchFile.isFile()) {
			return;
		}
		try {
			String collectionRelName = PathUtils.getCollectionName(root,
					matchFile);
			if (collectionRelName != null) {
				hvscEntryService.add(cp, collectionRelName, matchFile);
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