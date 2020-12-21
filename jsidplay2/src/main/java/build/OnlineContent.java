package build;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ui.entities.PersistenceProperties.CGSC_DS;
import static ui.entities.PersistenceProperties.HVSC_DS;
import static ui.musiccollection.MusicCollectionType.CGSC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TVFS;
import libsidutils.DebugUtil;
import libsidutils.Extract7Zip;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import sidplay.Player;
import sidplay.ini.IniDefaults;
import ui.download.DownloadThread;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.musiccollection.MusicCollectionType;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;

@Parameters(resourceBundle = "build.OnlineContent")
public class OnlineContent {

	private static final int MAX_ZIP_FILESIZE = 37748736;

	private static final int CHUNK_SIZE = 1 << 20;

	static {
		DebugUtil.init();
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--phase" }, descriptionKey = "PHASE")
	private String phase;

	@Parameter(names = { "--deployDir" }, descriptionKey = "DEPLOY_DIR")
	private String deployDir;

	@Parameter(names = { "--projectVersion" }, descriptionKey = "PROJECT_VERSION")
	private String projectVersion;

	@Parameter(names = { "--upxExe" }, descriptionKey = "UPX_EXE")
	private String upxExe;

	@Parameter(names = { "--baseDir" }, descriptionKey = "BASE_DIR")
	private String baseDir;

	@Parameter(names = { "--gb64" }, descriptionKey = "GB64")
	private String gb64;

	@Parameter(names = { "--hvmec" }, descriptionKey = "HVMEC")
	private String hvmec;

	@Parameter(names = { "--cgsc" }, descriptionKey = "CGSC")
	private String cgsc;

	@Parameter(names = { "--hvsc" }, descriptionKey = "HVSC")
	private String hvsc;

	private volatile boolean ready;

	private void create(String[] args) throws Exception {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(Arrays.asList(args).stream().map(arg -> arg == null ? "" : arg).toArray(String[]::new));
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		if ("package".equals(phase)) {
			moveProguardJar();

		} else if ("install".equals(phase)) {
			if (upxExe != null) {
				upx();
			}
			zipJSidDevice();

			createDemos();

			if (gb64 != null) {
				gb64();
			}
			if (hvmec != null) {
				hvmec();
			}
			if (cgsc != null) {
				cgsc();
			}
			if (hvsc != null) {
				hvsc();
			}
			latestVersion();

		} else {
			throw new RuntimeException("parameter 'phase' must be equal to 'package' or 'install'!");
		}
	}

	private void moveProguardJar() throws IOException {
		String source = deployDir + "/jsiddevice-" + projectVersion + "-proguard.jar";
		String target = deployDir + "/jsiddevice-" + projectVersion + ".jar";
		if (new File(source).exists()) {
			Files.move(Paths.get(source), Paths.get(target), REPLACE_EXISTING);
		}
	}

	private void upx() throws IOException, InterruptedException {
		if (!new File(upxExe).exists() || !new File(upxExe).canExecute()) {
			System.err.println("Warning: UPX Program not found or not executable: " + upxExe);
			return;
		}
		Process proc = Runtime.getRuntime().exec(new String[] { upxExe, "--lzma", "--best",
				/* "--ultra-brute", */deployDir + "/jsiddevice-" + projectVersion + ".exe" });

		ZipFileUtils.copy(proc.getErrorStream(), System.err);
		ZipFileUtils.copy(proc.getInputStream(), System.out);

		proc.waitFor();
	}

	private void zipJSidDevice() throws IOException {
		String jsidDeviceArtifact = "jsiddevice-" + projectVersion;

		TFile zipFile = new TFile(deployDir, jsidDeviceArtifact + ".zip");
		TFile src;
		src = new TFile(deployDir, "JSIDDevice.desktop");
		src.mv(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, "jsiddevice.png");
		src.cp(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, jsidDeviceArtifact + ".jar", TArchiveDetector.NULL);
		src.cp(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, jsidDeviceArtifact + ".exe", TArchiveDetector.NULL);
		src.cp(new TFile(zipFile, src.getName()));
		TVFS.umount();
	}

	private void createDemos() throws IOException {
		new File(deployDir, "online/demos").mkdirs();

		File demosZipFile = new File(deployDir, "online/demos/Demos.zip");
		File source = new File(baseDir, "src/test/resources/demos/Demos.zip");
		Files.copy(Paths.get(source.toURI()), Paths.get(demosZipFile.toURI()), REPLACE_EXISTING);

		createCRC(demosZipFile, new File(deployDir, "online/demos/Demos.crc"));
	}

	private void gb64() throws IOException {
		File mdbFile = new File(gb64);
		if (mdbFile.exists()) {
			new File(deployDir, "online/gamebase").mkdirs();

			File mdbZipFile = new File(deployDir, "/online/gamebase/GameBase64.zip");
			mdbZipFile.delete();
			TFile.cp_rp(mdbFile, new TFile(mdbZipFile, mdbFile.getName()), TArchiveDetector.ALL);
			TVFS.umount();

			createCRC(mdbZipFile, new File(deployDir, "online/gamebase/GameBase64.crc"));
		}
	}

	private void hvmec() throws IOException {
		File hvmecFile = new File(hvmec);
		if (hvmecFile.exists()) {
			new File(deployDir, "online/hvmec").mkdirs();

			File hvmecZipFile = new File(deployDir, "/online/hvmec/HVMEC.zip");
			hvmecZipFile.delete();
			TFile.cp_rp(hvmecFile, hvmecZipFile, TArchiveDetector.ALL);
			TVFS.umount();

			createCRC(hvmecZipFile, new File(deployDir, "online/hvmec/HVMEC.crc"));
		}
	}

	private void cgsc() throws Exception {
		File cgsc7zFile = new File(cgsc);
		if (cgsc7zFile.exists()) {
			new File(deployDir, "online/cgsc").mkdirs();

			System.out.println("Extracting archive, please wait a moment...");
			File cgscZipFile = new File(deployDir, "/online/cgsc/CGSC.zip");
			Extract7Zip extract7Zip = new Extract7Zip(new File(cgsc), new File(deployDir, "online/cgsc"));
			extract7Zip.extract();

			cgscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/cgsc/CGSC"), new TFile(cgscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			deleteDirectory(new File(deployDir, "online/cgsc/CGSC"));

			createCRC(cgscZipFile, new File(deployDir, "online/cgsc/CGSC.crc"));

			doCreateIndex(MusicCollectionType.CGSC, cgscZipFile.getAbsolutePath());
		}
	}

	private void hvsc() throws Exception {
		File hvsc7zFile = new File(hvsc);
		if (hvsc7zFile.exists()) {
			new File(deployDir, "online/hvsc").mkdirs();

			System.out.println("Extracting archive, please wait a moment...");
			File hvscZipFile = new File(deployDir, "/online/hvsc/C64Music.zip");
			Extract7Zip extract7Zip = new Extract7Zip(new File(hvsc), new File(deployDir, "online/hvsc"));
			extract7Zip.extract();

			hvscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/hvsc/C64Music"), new TFile(hvscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			deleteDirectory(new File(deployDir, "online/hvsc/C64Music"));

			createCRC(hvscZipFile, new File(deployDir, "online/hvsc/C64Music.crc"));

			doCreateIndex(MusicCollectionType.HVSC, hvscZipFile.getAbsolutePath());

			doSplit(MAX_ZIP_FILESIZE, hvscZipFile.getAbsolutePath());

			hvscZipFile.delete();
		}
	}

	private void latestVersion() throws IOException, FileNotFoundException, UnsupportedEncodingException {
		File versionFile = new File(baseDir, "latest.properties");
		versionFile.delete();
		versionFile.createNewFile();
		try (Writer writer = new PrintWriter(versionFile, StandardCharsets.ISO_8859_1.toString())) {
			writer.append("version=" + projectVersion);
		}
	}

	private void doCreateIndex(MusicCollectionType collectionType, String zipFile) throws Exception {
		File rootFile = new TFile(zipFile);

		String persistenceUnitName = collectionType == CGSC ? CGSC_DS : HVSC_DS;
		File dbFilename = new File(rootFile.getParentFile(), collectionType.toString());
		EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName,
				new PersistenceProperties(Database.HSQL_FILE, "", "", dbFilename.getAbsolutePath()));
		EntityManager em = emFactory.createEntityManager();

		Player player = new Player(IniDefaults.DEFAULTS);
		if (collectionType == MusicCollectionType.HVSC) {
			setSIDDatabase(player, zipFile);
			setSTIL(player, zipFile);
		}

		SearchIndexCreator searchIndexCreator = new SearchIndexCreator(rootFile, player, em);
		Consumer<Void> searchStart = v -> searchIndexCreator.getSearchStart().accept(v);
		Consumer<File> searchHit = searchIndexCreator.getSearchHit();
		Consumer<Boolean> searchStop = cancelled -> {
			searchIndexCreator.getSearchStop().accept(cancelled);
			ready = true;
		};
		ready = false;
		new SearchIndexerThread(rootFile, searchStart, searchHit, searchStop).start();

		System.out.println("Creating index, please wait a moment...");
		while (!ready) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while sleeping!");
			}
		}
		emFactory.close();
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
	}

	private void setSTIL(Player player, String zipFile) throws NoSuchFieldException, IllegalAccessException {
		try (InputStream input = new TFileInputStream(new TFile(zipFile, STIL.STIL_FILE))) {
			player.setSTIL(new STIL(input));
		} catch (IOException e) {
			System.err.println("WARNING: STIL can not be read: " + e.getMessage());
		}
	}

	private void setSIDDatabase(Player player, String zipFile) {
		try {
			player.setSidDatabase(new SidDatabase(zipFile));
		} catch (IOException e) {
			System.err.println("WARNING: song length database can not be read: " + e.getMessage());
		}
	}

	private void doSplit(int maxFileSize, String filename) throws IOException {
		int partNum = 1;
		String output = createOutputFilename(filename, partNum);

		byte[] buffer = new byte[CHUNK_SIZE];
		BufferedOutputStream os = null;
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(new File(filename)), CHUNK_SIZE)) {
			int bytesRead = 0, totalBytesRead = 0;
			os = createOutputStream(output);
			int len = Math.min(buffer.length, maxFileSize - totalBytesRead);
			while ((bytesRead = is.read(buffer, 0, len)) >= 0) {
				os.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				len = Math.min(buffer.length, maxFileSize - totalBytesRead);
				if (totalBytesRead == maxFileSize) {
					os.close();
					++partNum;
					output = createOutputFilename(filename, partNum);
					os = createOutputStream(output);
					totalBytesRead = 0;
				}
			}
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String createOutputFilename(String filename, int partNum) {
		return PathUtils.getFilenameWithoutSuffix(filename) + String.format(".%03d", partNum);
	}

	private BufferedOutputStream createOutputStream(String filename) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(new File(filename)), CHUNK_SIZE);
	}

	private void createCRC(File demosZipFile, File crcFile)
			throws IOException, FileNotFoundException, UnsupportedEncodingException {
		try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
			Properties properties = new Properties();
			properties.setProperty("filename", demosZipFile.getName());
			properties.setProperty("size", String.valueOf(demosZipFile.length()));
			properties.setProperty("crc32", DownloadThread.calculateCRC32(demosZipFile));
			properties.store(writer, null);
		}
	}

	private void deleteDirectory(File directory) throws IOException {
		Files.walkFileTree(Paths.get(directory.toURI()), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void main(String[] args) throws Exception {
		new OnlineContent().create(args);
	}

}
