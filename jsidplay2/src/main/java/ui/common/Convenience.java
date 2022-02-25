package ui.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.common.util.Extract7ZipUtil;
import ui.menubar.MenuBar;

/**
 * Automation for the Player.
 *
 * @author Ken Händel
 *
 */
public class Convenience {

	private static final Comparator<? super File> TOP_LEVEL_FIRST_COMPARATOR = (f1, f2) -> {
		if (f1.isDirectory() && !f2.isDirectory()) {
			return 1;
		}
		if (!f1.isDirectory() && f2.isDirectory()) {
			return -1;
		}
		String ext1 = PathUtils.getFilenameSuffix(f1.getAbsolutePath());
		String ext2 = PathUtils.getFilenameSuffix(f2.getAbsolutePath());

		if (ext1.endsWith(".sid") && !ext2.endsWith(".sid")) {
			return 1;
		}
		if (!ext1.endsWith(".sid") && ext2.endsWith(".sid")) {
			return -1;
		}
		return f1.compareTo(f2);
	};

	/** NUVIE video player */
	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static byte[] NUVIE_PLAYER;

	static {
		try (DataInputStream is = new DataInputStream(MenuBar.class.getResourceAsStream(NUVIE_PLAYER_PRG))) {
			URL us2 = JSidPlay2Main.class.getResource(NUVIE_PLAYER_PRG);
			NUVIE_PLAYER = new byte[us2.openConnection().getContentLength()];
			is.readFully(NUVIE_PLAYER);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Useless Apple directory.
	 */
	private static final String MACOSX = "__MACOSX";

	private static final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	/**
	 * Magically chooses files to be attached, rules are: Attach first supported
	 * file, eventually replace by lexically first disk or tape (e.g. side A, not
	 * B).
	 */
	public static final BiPredicate<File, File> LEXICALLY_FIRST_MEDIA = (file, toAttach) -> toAttach == null
			|| !tuneFileFilter.accept(file) && file.getName().compareTo(toAttach.getName()) < 0;

	/**
	 * Auto-start commands.
	 */
	private static final String LOAD_8_1_RUN = "LOAD\"*\",8,1\rRUN\r", LOAD_RUN = "LOAD\rRUN\r";

	private Player player;
	private Consumer<File> autoStartedFile = file -> {
	};

	public Convenience(Player player) {
		this.player = player;
	}

	public void setAutoStartedFile(Consumer<File> autoStartedFile) {
		this.autoStartedFile = autoStartedFile;
	}

	/**
	 * Auto-start C64 bundle (ZIP containing well-known formats or un-zipped entry).
	 * Attach specific disk/tape/cartridge and automatically start entry.<BR>
	 *
	 * Note: temporary files are removed or marked to be removed on exit.
	 *
	 * @param file            file to open
	 * @param isMediaToAttach tester for media to attach
	 * @param autoStartFile   if media to attach is a disk/tape/cartridge this tune
	 *                        is loaded after attaching the media (null means just
	 *                        reset C64, instead).
	 * @throws IOException  image read error
	 * @throws SidTuneError invalid tune
	 */
	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, File autoStartFile)
			throws IOException, SidTuneError {
		player.getC64().ejectCartridge();
		File tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
		TFile zip = new TFile(file);
		File toAttach = null;
		if (zip.isArchive()) {
			// uncompress zip
			TFile.cp_rp(zip, tmpDir, TArchiveDetector.ALL);
			// search media file to attach
			toAttach = getToAttach(tmpDir, zip, isMediaToAttach, null, true);
			TFile.rm_r(zip);
		} else if (file.getName().toLowerCase(Locale.US).endsWith("7z")) {
			Extract7ZipUtil extract7Zip = new Extract7ZipUtil(zip, tmpDir);
			extract7Zip.extract();
			toAttach = getToAttach(tmpDir, extract7Zip.getZipFile(), isMediaToAttach, null, true);
			TFile.rm_r(zip);
		} else if (zip.isEntry()) {
			// uncompress zip entry
			File zipEntry = new File(tmpDir, zip.getName());
			zipEntry.deleteOnExit();
			TFile.cp_rp(zip, zipEntry, TArchiveDetector.ALL);
			// search media file to attach
			toAttach = getToAttach(tmpDir, zipEntry.getParentFile(), (f1, f2) -> {
				return false;
			}, null, false);
			toAttach = zipEntry;
		} else if (isSupportedMedia(file)) {
			toAttach = getToAttach(file.getParentFile(), file.getParentFile(), (f1, f2) -> {
				return false;
			}, null, false);
			toAttach = file;
		}
		if (toAttach != null) {
			if (tuneFileFilter.accept(toAttach)) {
				player.play(SidTune.load(toAttach));
				autoStartedFile.accept(toAttach);
				return true;
			} else if (diskFileFilter.accept(toAttach)) {
				player.insertDisk(toAttach);
				autoStart(autoStartFile, LOAD_8_1_RUN);
				return true;
			} else if (tapeFileFilter.accept(toAttach)) {
				player.insertTape(toAttach);
				autoStart(autoStartFile, LOAD_RUN);
				return true;
			} else if (toAttach.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
				playVideo(toAttach);
				return true;
			} else if (cartFileFilter.accept(toAttach)) {
				player.insertCartridge(CartridgeType.CRT, toAttach);
				autoStart(autoStartFile, null);
				return true;
			}
		}
		return false;
	}

	/**
	 * Load tune or reset C64 and type-in command automatically
	 *
	 * @param file    tune file to load and play
	 * @param command command to type-in after reset (if no file is specified)
	 * @throws IOException  image read error
	 * @throws SidTuneError invalid tune
	 */
	private void autoStart(File file, String command) throws IOException, SidTuneError {
		if (file != null) {
			player.play(SidTune.load(file));
			autoStartedFile.accept(file);
		} else {
			player.resetC64(command);
		}
	}

	/**
	 * Get media file to attach, search recursively.<BR>
	 *
	 * Note: all files and folders are marked to be deleted.
	 *
	 * @param dir         directory where the files are located
	 * @param file        file to get traversed and searched for media
	 * @param mediaTester predicate to check desired media
	 * @param toAttach    current media to attach
	 * @return media to attach
	 */
	private File getToAttach(File dir, File file, BiPredicate<File, File> mediaTester, File toAttach,
			boolean deleteOnExit) {
		final File[] listFiles = file.listFiles();
		if (listFiles == null) {
			return toAttach;
		}
		final List<File> asList = Arrays.asList(listFiles);
		asList.sort(TOP_LEVEL_FIRST_COMPARATOR);
		for (File member : asList) {
			File memberFile = new File(dir, member.getName());
			if (deleteOnExit) {
				memberFile.deleteOnExit();
			}
			if (memberFile.isFile() && isSupportedMedia(memberFile)) {
				if (memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
					try {
						player.insertCartridge(CartridgeType.REU, memberFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".crt")) {
					try {
						player.insertCartridge(CartridgeType.CRT, memberFile);
						toAttach = memberFile;
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					if (mediaTester.test(memberFile, toAttach)) {
						toAttach = memberFile;
					}
				}
			} else if (memberFile.isDirectory() && !memberFile.getName().equals(MACOSX)) {
				File toAttachChild = getToAttach(memberFile, new TFile(memberFile), mediaTester, toAttach,
						deleteOnExit);
				if (toAttachChild != null) {
					toAttach = toAttachChild;
				}
			}
		}
		return toAttach;
	}

	/**
	 * Check well-known disk/tape/cartridge file extension
	 *
	 * @param file file to check
	 * @return is it a well-known format
	 */
	public boolean isSupportedMedia(File file) {
		return cartFileFilter.accept(file) || tuneFileFilter.accept(file) || diskFileFilter.accept(file)
				|| tapeFileFilter.accept(file);
	}

	private void playVideo(File file) {
		final File tmpFile = new File(player.getConfig().getSidplay2Section().getTmpDir(), "nuvieplayer-v1.0.prg");
		tmpFile.deleteOnExit();
		try (DataOutputStream os = new DataOutputStream(new FileOutputStream(tmpFile))) {
			os.write(NUVIE_PLAYER);
			player.insertCartridge(CartridgeType.REU, file);
			player.play(SidTune.load(tmpFile));
		} catch (IOException | SidTuneError e) {
			System.err.println();
		}
	}
}
