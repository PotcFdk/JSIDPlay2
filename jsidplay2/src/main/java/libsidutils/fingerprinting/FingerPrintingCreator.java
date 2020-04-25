package libsidutils.fingerprinting;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.common.SamplingRate;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.WhatsSidDriver;
import sidplay.ini.IniConfig;
import ui.entities.PersistenceProperties;
import ui.entities.whatssid.service.WhatsSidService;
import ui.filefilter.TuneFileFilter;

/**
 * WhatsSid? is a Shazam like feature. It analyzes tunes to recognize a
 * currently played tune
 * 
 * This is the program to create the fingerprintings for all tunes of a
 * collection.
 * 
 * @author ken
 *
 */
@Parameters(resourceBundle = "libsidutils.fingerprinting.FingerPrintingCreator")
public class FingerPrintingCreator {

	static {
		DebugUtil.init();
	}

	private static final TuneFileFilter TUNE_FILE_FILTER = new TuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--whatsSidDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", required = true)
	private String whatsSidDatabaseDriver;

	@Parameter(names = { "--whatsSidDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", required = true)
	private String whatsSidDatabaseUrl;

	@Parameter(names = { "--whatsSidDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", required = true)
	private String whatsSidDatabaseUsername;

	@Parameter(names = { "--whatsSidDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", required = true)
	private String whatsSidDatabasePassword;

	@Parameter(names = { "--whatsSidDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", required = true)
	private String whatsSidDatabaseDialect;

	@Parameter(names = { "--createIni" }, descriptionKey = "CREATE_INI", arity = 1)
	private Boolean createIni = Boolean.FALSE;

	@Parameter(names = { "--deleteAll" }, descriptionKey = "DELETE_ALL", arity = 1)
	private Boolean deleteAll = Boolean.FALSE;

	@Parameter(description = "directory")
	private String directory;

	@ParametersDelegate
	private IniConfig config = new IniConfig(true, null);

	private Player player;

	private WhatsSidDriver whatsSidDriver;

	private void execute(String[] args) throws IOException, SidTuneError, InterruptedException {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		config.getAudioSection().setAudio(Audio.WHATS_SID);
		config.getAudioSection().setSamplingRate(SamplingRate.VERY_LOW);
		config.getSidplay2Section().setDefaultPlayLength(180);
		config.getSidplay2Section().setEnableDatabase(true);
		String hvsc = config.getSidplay2Section().getHvsc();

		player = new Player(config);
		player.setSidDatabase(hvsc != null ? new SidDatabase(hvsc) : null);

		EntityManager em = Persistence.createEntityManagerFactory(PersistenceProperties.WHATSSID_DS,
				new PersistenceProperties(whatsSidDatabaseDriver, whatsSidDatabaseUrl, whatsSidDatabaseUsername,
						whatsSidDatabasePassword, whatsSidDatabaseDialect))
				.createEntityManager();
		WhatsSidService whatsSidService = new WhatsSidService(em);

		whatsSidDriver = (WhatsSidDriver) Audio.WHATS_SID.getAudioDriver();
		whatsSidDriver.setFingerprintInserter(new FingerPrinting(new IniFingerprintConfig(createIni), whatsSidService));

		if (Boolean.TRUE.equals(deleteAll)) {
			System.out.println("Delete all fingerprintings...");
			whatsSidService.deleteAll();
		}

		try {
			if (directory != null) {
				System.out.println(
						"Create fingerprintings... (press q <return> to abort after the current tune has been fingerprinted)");
				
				processDirectory(new File(directory), em);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			whatsSidService.close();
			System.exit(0);
		}
	}

	private void processDirectory(File dir, EntityManager em) throws IOException, SidTuneError {
		File[] listFiles = dir.listFiles();
		Arrays.sort(listFiles);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				processDirectory(file, em);
			} else if (file.isFile()) {
				if (TUNE_FILE_FILTER.accept(file)) {
					whatsSidDriver.setTuneFile(file);
					player.setRecordingFilenameProvider(tune -> {
						String filename = PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath());
						if (tune.getInfo().getSongs() > 1) {
							filename += String.format("-%02d", tune.getInfo().getCurrentSong());
						}
						return filename;
					});
					player.setTune(SidTune.load(file));
					player.getTune().getInfo().setSelectedSong(player.getTune().getInfo().getStartSong());
					player.startC64();
					player.stopC64(false);
					em.clear();
				}
				if (System.in.available() > 0) {
					final int key = System.in.read();
					if (key == 'q') {
						throw new IOException("Termination after pressing q");
					}
				}
			}
		}
	}

	public static void main(String[] args) throws IOException, SidTuneError, InterruptedException {
		new FingerPrintingCreator().execute(args);
	}

}
