package libsidutils.fingerprinting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.common.SamplingRate;
import libsidplay.sidtune.MD5Method;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.WhatsSidDriver;
import sidplay.ini.IniConfig;
import ui.entities.PersistenceProperties;
import ui.entities.whatssid.service.WhatsSidService;
import ui.filefilter.TuneFileFilter;

/**
 * WhatsSID? is a Shazam like feature. It analyzes tunes to recognize a
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

	@Parameter(names = { "--whatsSIDDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", required = true)
	private String whatsSidDatabaseDriver;

	@Parameter(names = { "--whatsSIDDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", required = true)
	private String whatsSidDatabaseUrl;

	@Parameter(names = { "--whatsSIDDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", required = true)
	private String whatsSidDatabaseUsername;

	@Parameter(names = { "--whatsSIDDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", required = true)
	private String whatsSidDatabasePassword;

	@Parameter(names = { "--whatsSIDDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", required = true)
	private String whatsSidDatabaseDialect;

	@Parameter(names = { "--createIni" }, descriptionKey = "CREATE_INI", arity = 1)
	private Boolean createIni = Boolean.FALSE;

	@Parameter(names = { "--deleteAll" }, descriptionKey = "DELETE_ALL", arity = 1)
	private Boolean deleteAll = Boolean.FALSE;

	@Parameter(names = { "--previousDirectory" }, descriptionKey = "PREVIOUS_DIRECTORY")
	private String previousDirectory;

	@Parameter(description = "directory")
	private String directory;

	@ParametersDelegate
	private IniConfig config = new IniConfig(true, null);

	private Player player;

	private WhatsSidDriver whatsSidDriver;

	private SidDatabase previousSidDatabase;

	private void execute(String[] args) throws IOException, SidTuneError {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		if (config.getSidplay2Section().getHvsc() == null) {
			System.out.println("Parameter --hvsc must be present!");
			System.exit(1);
		}
		config.getAudioSection().setSamplingRate(SamplingRate.VERY_LOW);
		config.getSidplay2Section().setDefaultPlayLength(180);
		config.getSidplay2Section().setEnableDatabase(true);

		whatsSidDriver = new WhatsSidDriver();

		player = new Player(config);
		player.setAudioDriver(whatsSidDriver);
		player.setSidDatabase(new SidDatabase(config.getSidplay2Section().getHvsc()));

		if (previousDirectory != null) {
			previousSidDatabase = new SidDatabase(previousDirectory);
		}

		EntityManager em = Persistence.createEntityManagerFactory(PersistenceProperties.WHATSSID_DS,
				new PersistenceProperties(whatsSidDatabaseDriver, whatsSidDatabaseUrl, whatsSidDatabaseUsername,
						whatsSidDatabasePassword, whatsSidDatabaseDialect))
				.createEntityManager();
		WhatsSidService whatsSidService = new WhatsSidService(em);

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
		File[] listFiles = Optional.ofNullable(dir.listFiles()).orElse(new File[0]);
		Arrays.sort(listFiles);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				processDirectory(file, em);
			} else if (file.isFile()) {
				if (TUNE_FILE_FILTER.accept(file)) {
					SidTune tune = SidTune.load(file);
					if (previousDirectory != null) {
						copyRecordingsOfPreviousDirectory(file, tune);
					}
					whatsSidDriver.setTuneFile(file);
					player.setRecordingFilenameProvider(
							theTune -> getRecordingFilename(file, theTune, theTune.getInfo().getCurrentSong()));
					player.setTune(tune);
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

	private void copyRecordingsOfPreviousDirectory(File file, SidTune tune) throws IOException, SidTuneError {
		File theCollectionFile = new File(config.getSidplay2Section().getHvsc());
		String collectionName = PathUtils.getCollectionName(theCollectionFile, file);

		File previousFile = new File(previousDirectory, collectionName);
		if (previousFile.exists()) {
			SidTune previousTune = SidTune.load(previousFile);
			if (Objects.equals(tune.getMD5Digest(MD5Method.MD5_CONTENTS),
					previousTune.getMD5Digest(MD5Method.MD5_CONTENTS))
					&& player.getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.) == previousSidDatabase
							.getTuneLength(previousTune)) {
				for (int i = 1; i < tune.getInfo().getSongs(); i++) {
					File wavFile = new File(getRecordingFilename(file, tune, i) + whatsSidDriver.getExtension());
					File previousWavFile = new File(
							getRecordingFilename(previousFile, previousTune, i) + whatsSidDriver.getExtension());
					if (!wavFile.exists() && previousWavFile.exists()) {
						System.out.println(String.format("Tune is unchanged, copy %s to %s", previousWavFile, wavFile));
						Files.copy(previousFile.toPath(), wavFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}

	private String getRecordingFilename(File file, SidTune theTune, int song) {
		String filename = PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath());
		if (theTune.getInfo().getSongs() > 1) {
			filename += String.format("-%02d", song);
		}
		return filename;
	}

	public static void main(String[] args) throws IOException, SidTuneError {
		new FingerPrintingCreator().execute(args);
	}

}
