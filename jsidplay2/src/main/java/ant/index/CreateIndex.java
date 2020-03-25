package ant.index;

import static ui.entities.PersistenceProperties.CGSC_DS;
import static ui.entities.PersistenceProperties.HVSC_DS;
import static ui.musiccollection.MusicCollectionType.CGSC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import libsidutils.DebugUtil;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import sidplay.Player;
import sidplay.ini.IniConfig;
import sidplay.ini.IniDefaults;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.musiccollection.MusicCollectionType;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;

@Parameters(resourceBundle = "ant.index.CreateIndex")
public class CreateIndex {

	static {
		DebugUtil.init();
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--collectionType", "-c" }, descriptionKey = "COLLECTION_TYPE")
	private MusicCollectionType collectionType = MusicCollectionType.HVSC;

	@Parameter(descriptionKey = "FILENAME", required = true)
	private String filename;

	private volatile boolean ready;

	public static void main(String[] args) throws Exception {
		new CreateIndex().doCreateIndex(args);
	}

	private void doCreateIndex(String[] args) throws Exception {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		File rootFile = new TFile(filename);

		File dbFilename = new File(rootFile.getParentFile(), collectionType.toString());
		PersistenceProperties pp = new PersistenceProperties(dbFilename.getAbsolutePath(), "", "", Database.HSQL_FILE);
		EntityManagerFactory emFactory = Persistence
				.createEntityManagerFactory(collectionType == CGSC ? CGSC_DS : HVSC_DS, pp);
		EntityManager em = emFactory.createEntityManager();

		IniConfig config = IniDefaults.DEFAULTS;
		Player player = new Player(config);
		try {
			player.setSidDatabase(new SidDatabase(filename));
		} catch (IOException e) {
			if (collectionType == MusicCollectionType.HVSC) {
				System.err.println("WARNING: song length database can not be read: " + e.getMessage());
			}
		}
		try (InputStream input = new TFileInputStream(new TFile(filename, STIL.STIL_FILE))) {
			player.setSTIL(new STIL(input));
		} catch (IOException e) {
			if (collectionType == MusicCollectionType.HVSC) {
				System.err.println("WARNING: STIL can not be read: " + e.getMessage());
			}
		}

		SearchIndexCreator searchIndexCreator = new SearchIndexCreator(rootFile, player, em);
		Consumer<Void> searchStart = (x) -> searchIndexCreator.getSearchStart().accept(x);
		Consumer<File> searchHit = searchIndexCreator.getSearchHit();
		Consumer<Boolean> searchStop = (cancelled) -> {
			searchIndexCreator.getSearchStop().accept(cancelled);
			ready = true;
		};
		new SearchIndexerThread(rootFile, searchStart, searchHit, searchStop).start();

		System.out.println("Creating index, please wait a moment...");
		while (!ready) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while sleeping!");
			}
		}
		if (em != null) {
			em.close();
			em.getEntityManagerFactory().close();
		}
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
	}

}
