package build;

import static ui.entities.PersistenceProperties.CGSC_DS;
import static ui.entities.PersistenceProperties.HVSC_DS;
import static ui.musiccollection.MusicCollectionType.CGSC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
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
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import sidplay.Player;
import sidplay.ini.IniConfig;
import sidplay.ini.IniDefaults;
import ui.download.DownloadThread;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.musiccollection.MusicCollectionType;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;

@Parameters(resourceBundle = "build.OnlineContent")
public class OnlineContent {

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
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		int exitVal;

		if ("package".equals(phase)) {
			exitVal = moveProguardJar(deployDir, projectVersion);
			if (exitVal != 0) {
				System.exit(exitVal);
			}
		} else if ("install".equals(phase)) {
			if (upxExe != null) {
				exitVal = upx(upxExe, deployDir, projectVersion);
				if (exitVal != 0) {
					System.exit(exitVal);
				}
			}
			
			exitVal = zipJSIDDevice(deployDir, projectVersion);
			if (exitVal != 0) {
				System.exit(exitVal);
			}
			
			exitVal = createDemos(deployDir, baseDir);
			if (exitVal != 0) {
				System.exit(exitVal);
			}
			
			if (gb64 != null) {
				exitVal = gb64(deployDir, gb64);
				if (exitVal != 0) {
					System.exit(exitVal);
				}
			}
			
			if (hvmec != null) {
				exitVal = hvmec(deployDir, hvmec);
				if (exitVal != 0) {
					System.exit(exitVal);
				}
			}
			
			if (cgsc != null) {
				exitVal = cgsc(deployDir, cgsc);
				if (exitVal != 0) {
					System.exit(exitVal);
				}
			}
			
			if (hvsc != null) {
				exitVal = hvsc(deployDir, hvsc);
				if (exitVal != 0) {
					System.exit(exitVal);
				}
			}
			
			File versionFile = new File(baseDir, "latest.properties");
			try (Writer writer = new PrintWriter(versionFile, StandardCharsets.ISO_8859_1.toString())) {
				Properties properties = new Properties();
				properties.setProperty("version", projectVersion);
				properties.store(writer, null);
			}
		} else {
			throw new RuntimeException("parameter 'phase' must be equal to 'package' or 'install'!");
		}
	}

	private int moveProguardJar(String deployDir, String projectVersion) throws IOException {
		String proguardJar = deployDir + "/jsiddevice-" + projectVersion + "-proguard.jar";
		String jsidDeviceJar = deployDir + "/jsiddevice-" + projectVersion + ".jar";
		Files.move(Paths.get(proguardJar), Paths.get(jsidDeviceJar), StandardCopyOption.REPLACE_EXISTING);
		return 0;
	}

	private int upx(String upxExe, String deployDir, String projectVersion) throws IOException, InterruptedException {
		if (!new File(upxExe).exists() || !new File(upxExe).canExecute()) {
			throw new RuntimeException("UPX Program not found: " + upxExe);
		}
		Process proc = Runtime.getRuntime().exec(
				new String[] { upxExe, "--lzma", "--best", deployDir + "/jsiddevice-" + projectVersion + ".exe" });
		print(proc.getErrorStream());
		print(proc.getInputStream());
		return proc.waitFor();
	}

	private void print(InputStream stderr) throws IOException {
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
		while ((line = br.readLine()) != null)
			System.out.println(line);
	}

	private int zipJSIDDevice(String deployDir, String projectVersion) throws IOException {

		TFile zipFile = new TFile(deployDir, "jsiddevice-" + projectVersion + ".zip");

		TFile src;
		src = new TFile(deployDir, "JSIDDevice (Java11).desktop");
		src.mv(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, "JSIDDevice.desktop");
		src.mv(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, "jsidplay2.png");
		src.cp(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, "jsiddevice-" + projectVersion + ".jar", TArchiveDetector.NULL);
		src.mv(new TFile(zipFile, src.getName()));
		src = new TFile(deployDir, "jsiddevice-" + projectVersion + ".exe", TArchiveDetector.NULL);
		src.mv(new TFile(zipFile, src.getName()));
		TVFS.umount();

		return 0;
	}

	private int createDemos(String deployDir, String baseDir) throws IOException {
		File demosFile = new File(deployDir, "online/demos/Demos.zip");
		File crcFile = new File(deployDir, "online/demos/Demos.crc");

		new File(deployDir, "online/demos").mkdirs();

		Files.copy(Paths.get(new File(baseDir, "src/test/resources/demos/Demos.zip").toURI()),
				Paths.get(demosFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
		long checksum = DownloadThread.calculateCRC32(demosFile);
		long fileSize = demosFile.length();
		try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
			Properties properties = new Properties();
			properties.setProperty("filename", "Demos.zip");
			properties.setProperty("size", String.valueOf(fileSize));
			properties.setProperty("crc32", String.format("%8X", checksum).replace(' ', '0'));
			properties.store(writer, null);
		}
		return 0;
	}

	private int gb64(String deployDir, String mdbFilename) throws IOException {
		File mdbFile = new File(mdbFilename);
		if (mdbFile.exists()) {
			new File(deployDir, "online/gamebase").mkdirs();

			File mdbZipFile = new File(deployDir, "/online/gamebase/GameBase64.zip");
			File crcFile = new File(deployDir, "online/gamebase/GameBase64.crc");

			mdbZipFile.delete();
			TFile.cp_r(mdbFile, new TFile(mdbZipFile, "GameBase64.mdb"), TArchiveDetector.ALL);
			TVFS.umount();

			long checksum = DownloadThread.calculateCRC32(mdbZipFile);
			long fileSize = mdbZipFile.length();
			try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
				Properties properties = new Properties();
				properties.setProperty("filename", "GameBase64.zip");
				properties.setProperty("size", String.valueOf(fileSize));
				properties.setProperty("crc32", String.format("%8X", checksum).replace(' ', '0'));
				properties.store(writer, null);
			}
		}
		return 0;
	}

	private int hvmec(String deployDir, String hvmecFilename) throws IOException {
		File hvmecFile = new File(hvmecFilename);
		if (hvmecFile.exists()) {
			new File(deployDir, "online/hvmec").mkdirs();

			File hvmecZipFile = new File(deployDir, "/online/hvmec/HVMEC.zip");
			File crcFile = new File(deployDir, "online/hvmec/HVMEC.crc");

			hvmecZipFile.delete();
			TFile.cp_rp(hvmecFile, hvmecZipFile, TArchiveDetector.ALL);
			TVFS.umount();

			long checksum = DownloadThread.calculateCRC32(hvmecZipFile);
			long fileSize = hvmecZipFile.length();
			try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
				Properties properties = new Properties();
				properties.setProperty("filename", "GameBase64.zip");
				properties.setProperty("size", String.valueOf(fileSize));
				properties.setProperty("crc32", String.format("%8X", checksum).replace(' ', '0'));
				properties.store(writer, null);
			}
		}
		return 0;
	}

	private int cgsc(String deployDir, String cgsc7zFilename) throws Exception {
		File cgsc7zFile = new File(cgsc7zFilename);
		if (cgsc7zFile.exists()) {
			new File(deployDir, "online/cgsc").mkdirs();

			File cgscZipFile = new File(deployDir, "/online/cgsc/CGSC.zip");
			File crcFile = new File(deployDir, "online/cgsc/CGSC.crc");

			Extract7Zip extract7Zip = new Extract7Zip(new File(cgsc7zFilename), new File(deployDir, "online/cgsc"));
			extract7Zip.extract();

			cgscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/cgsc/CGSC"), new TFile(cgscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			Files.walkFileTree(Paths.get(new File(deployDir, "online/cgsc/CGSC").toURI()),
					new SimpleFileVisitor<Path>() {
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

			long checksum = DownloadThread.calculateCRC32(cgscZipFile);
			long fileSize = cgscZipFile.length();
			try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
				Properties properties = new Properties();
				properties.setProperty("filename", "CGSC.zip");
				properties.setProperty("size", String.valueOf(fileSize));
				properties.setProperty("crc32", String.format("%8X", checksum).replace(' ', '0'));
				properties.store(writer, null);
			}
			doCreateIndex(MusicCollectionType.CGSC, cgscZipFile.getAbsolutePath());
		}
		return 0;
	}

	private int hvsc(String deployDir, String hvsc7zFilename) throws Exception {
		File hvsc7zFile = new File(hvsc7zFilename);
		if (hvsc7zFile.exists()) {
			new File(deployDir, "online/hvsc").mkdirs();

			File hvscZipFile = new File(deployDir, "/online/hvsc/C64Music.zip");
			File crcFile = new File(deployDir, "online/hvsc/C64Music.crc");

			Extract7Zip extract7Zip = new Extract7Zip(new File(hvsc7zFilename), new File(deployDir, "online/hvsc"));
			extract7Zip.extract();

			hvscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/hvsc/C64Music"), new TFile(hvscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			Files.walkFileTree(Paths.get(new File(deployDir, "online/hvsc/C64Music").toURI()),
					new SimpleFileVisitor<Path>() {
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

			long checksum = DownloadThread.calculateCRC32(hvscZipFile);
			long fileSize = hvscZipFile.length();
			try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
				Properties properties = new Properties();
				properties.setProperty("filename", "C64Music.zip");
				properties.setProperty("size", String.valueOf(fileSize));
				properties.setProperty("crc32", String.format("%8X", checksum).replace(' ', '0'));
				properties.store(writer, null);
			}
			doCreateIndex(MusicCollectionType.HVSC, hvscZipFile.getAbsolutePath());

			doSplit(37748736, hvscZipFile.getAbsolutePath());
		
			hvscZipFile.delete();
		}
		return 0;
	}

	private void doCreateIndex(MusicCollectionType collectionType, String filename) throws Exception {
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
		}
		ready = false;
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
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

	public static void main(String[] args) throws Exception {
		new OnlineContent().create(args);
	}

}
