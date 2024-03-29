package ui.tools;

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
import libsidutils.PathUtils;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.ini.IniConfig;
import sidplay.ini.converter.FileToStringConverter;
import ui.common.filefilter.TuneFileFilter;
import ui.common.util.DebugUtil;
import ui.entities.PersistenceProperties;
import ui.entities.whatssid.service.WhatsSidService;
import ui.tools.audio.WhatsSidDriver;

/**
 * WhatsSID? is a Shazam like feature. It analyzes tunes to recognize a
 * currently played tune.
 *
 * This is the main class to create or Update a WhatsSID database.
 *
 * This is the program to create the fingerprintings for all tunes of a
 * collection.
 *
 * @author ken
 *
 */
@Parameters(resourceBundle = "ui.tools.FingerPrintingCreator")
public class FingerPrintingCreator {

	static {
		DebugUtil.init();
	}

	private static final TuneFileFilter TUNE_FILE_FILTER = new TuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = {
			"--whatsSIDDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", required = true, order = 10001)
	private String whatsSidDatabaseDriver;

	@Parameter(names = {
			"--whatsSIDDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", required = true, order = 10002)
	private String whatsSidDatabaseUrl;

	@Parameter(names = {
			"--whatsSIDDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", required = true, order = 10003)
	private String whatsSidDatabaseUsername;

	@Parameter(names = {
			"--whatsSIDDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", required = true, order = 10004)
	private String whatsSidDatabasePassword;

	@Parameter(names = {
			"--whatsSIDDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", required = true, order = 10005)
	private String whatsSidDatabaseDialect;

	@Parameter(names = { "--createIni" }, descriptionKey = "CREATE_INI", arity = 1, order = 10006)
	private Boolean createIni = Boolean.FALSE;

	@Parameter(names = { "--deleteAll" }, descriptionKey = "DELETE_ALL", arity = 1, order = 10007)
	private Boolean deleteAll = Boolean.FALSE;

	@Parameter(names = {
			"--previousDirectory" }, descriptionKey = "PREVIOUS_DIRECTORY", converter = FileToStringConverter.class, order = 10008)
	private File previousDirectory;

	@Parameter(description = "directory", converter = FileToStringConverter.class)
	private File directory;

	@ParametersDelegate
	private IniConfig config = new IniConfig(false);

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
		config.getSidplay2Section().setSingle(false);
		config.getSidplay2Section().setLoop(false);

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
			deleteAllFingerprintings(whatsSidService);
		}

		try {
			if (directory != null) {
				System.out.println(
						"Create fingerprintings... (press q <return> to abort after the current tune has been fingerprinted)");

				processDirectory(directory, em);
			}
		} finally {
			whatsSidService.close();
			System.exit(0);
		}
	}

	private void deleteAllFingerprintings(WhatsSidService whatsSidService) throws IOException {
		System.out.println("Delete all fingerprintings...");
		switch (proceed()) {
		case 'y':
		case 'Y':
			whatsSidService.deleteAll();
			System.out.println("Done!");
			break;

		default:
			System.out.println("Aborted by user!");
			break;

		}
	}

	private int proceed() throws IOException {
		System.out.println(
				"You are about to delete all fingerprintings from the database. Are you sure to proceed? (y/N)");
		return System.in.read();
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
					String collectionName = PathUtils.getCollectionName(config.getSidplay2Section().getHvsc(), file);

					if (previousDirectory != null) {
						copyRecordingsOfPreviousDirectory(file, tune, collectionName);
					}

					whatsSidDriver.setCollectionName(collectionName);
					whatsSidDriver.setTune(tune);

					player.setRecordingFilenameProvider(
							theTune -> getRecordingFilename(file, theTune, theTune.getInfo().getCurrentSong()));
					player.setTune(tune);
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

	private void copyRecordingsOfPreviousDirectory(File file, SidTune tune, String collectionName)
			throws IOException, SidTuneError {
		File previousFile = new File(previousDirectory, collectionName);
		if (previousFile.exists()) {
			SidTune previousTune = SidTune.load(previousFile);
			if (Objects.equals(tune.getMD5Digest(MD5Method.MD5_CONTENTS),
					previousTune.getMD5Digest(MD5Method.MD5_CONTENTS))
					&& player.getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.) == previousSidDatabase
							.getTuneLength(previousTune)) {
				for (int songNo = 1; songNo <= tune.getInfo().getSongs(); songNo++) {
					File wavFile = new File(getRecordingFilename(file, tune, songNo) + whatsSidDriver.getExtension());
					File previousWavFile = new File(
							getRecordingFilename(previousFile, previousTune, songNo) + whatsSidDriver.getExtension());
					if (!wavFile.exists() && previousWavFile.exists()) {
						System.out.println(String.format("Tune is unchanged, copy %s to %s", previousWavFile, wavFile));
						Files.copy(previousWavFile.toPath(), wavFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}

	private String getRecordingFilename(File file, SidTune tune, int song) {
		String filename = PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath());
		if (tune.getInfo().getSongs() > 1) {
			filename += String.format("-%02d", song);
		}
		return filename;
	}

	public static void main(String[] args) throws IOException, SidTuneError {
		new FingerPrintingCreator().execute(args);
	}

}
