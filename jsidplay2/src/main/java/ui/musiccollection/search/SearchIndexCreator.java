/**
 * 
 */
package ui.musiccollection.search;

import java.io.File;
import java.util.function.Consumer;

import javax.persistence.EntityManager;

import libsidutils.PathUtils;
import sidplay.Player;
import ui.entities.collection.service.HVSCEntryService;
import ui.entities.collection.service.STILService;
import ui.entities.collection.service.VersionService;

public final class SearchIndexCreator {

	private EntityManager em;
	private HVSCEntryService hvscEntryService;
	private STILService stilService;
	private VersionService versionService;

	private Player player;
	private File root;

	public SearchIndexCreator(File root, final Player player,
			final EntityManager em) {
		this.root = root;
		this.player = player;
		this.em = em;
		this.hvscEntryService = new HVSCEntryService(em);
		this.stilService = new STILService(em);
		this.versionService = new VersionService(em);
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

	public Consumer<Void> getSearchStart() {
		return x -> {
			clearPreviousSearchIndex();

			em.getTransaction().begin();
		};
	}

	public Consumer<File> getSearchHit() {
		return file -> {
			if (!file.isFile()) {
				return;
			}
			try {
				String collectionRelName = PathUtils.getCollectionName(root,
						file.getPath());
				if (collectionRelName != null) {
					hvscEntryService.add(player, collectionRelName, file);
				}
			} catch (final Exception e) {
				System.err.println("Indexing failure on: "
						+ file.getAbsolutePath() + ": " + e.getMessage());
			}
		};
	}

	public Consumer<Boolean> getSearchStop() {
		return cancelled -> {
			versionService.setExpectedVersion();
			em.getTransaction().commit();
		};
	}
}