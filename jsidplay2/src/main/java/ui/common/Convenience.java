package ui.common;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.Extract7Zip;
import sidplay.Player;
import ui.filefilter.CartFileFilter;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TapeFileFilter;
import ui.filefilter.TuneFileFilter;

/**
 * Automation for the Player.
 * 
 * @author Ken Händel
 *
 */
public class Convenience {

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
	 * @param file
	 *            file to open
	 * @param isMediaToAttach
	 *            tester for media to attach
	 * @param autoStartFile
	 *            if media to attach is a disk/tape/cartridge this tune is loaded
	 *            after attaching the media (null means just reset C64, instead).
	 * @throws IOException
	 *             image read error
	 * @throws SidTuneError
	 *             invalid tune
	 */
	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, File autoStartFile)
			throws IOException, SidTuneError, URISyntaxException {
		player.getC64().ejectCartridge();
		String tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
		TFile zip = new TFile(file);
		File toAttach = null;
		if (zip.isArchive()) {
			// uncompress zip
			TFile.cp_rp(zip, new File(tmpDir), TArchiveDetector.ALL);
			// search media file to attach
			toAttach = getToAttach(tmpDir, zip, isMediaToAttach, null);
			TFile.rm_r(zip);
		} else if (file.getName().toLowerCase(Locale.US).endsWith("7z")) {
			Extract7Zip extract7Zip = new Extract7Zip(zip, new File(tmpDir));
			extract7Zip.extract();
			toAttach = getToAttach(tmpDir, extract7Zip.getZipFile(), isMediaToAttach, null);
		} else if (isSupportedMedia(file)) {
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
				try {
					player.insertCartridge(CartridgeType.REU, toAttach);
				} catch (IOException e) {
					e.printStackTrace();
				}
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
	 * @param file
	 *            tune file to load and play
	 * @param command
	 *            command to type-in after reset (if no file is specified)
	 * @throws IOException
	 *             image read error
	 * @throws SidTuneError
	 *             invalid tune
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
	 * @param dir
	 *            directory where the files are located
	 * @param file
	 *            file to get traversed and searched for media
	 * @param mediaTester
	 *            predicate to check desired media
	 * @param toAttach
	 *            current media to attach
	 * @return media to attach
	 */
	private File getToAttach(String dir, File file, BiPredicate<File, File> mediaTester, File toAttach) {
		final File[] listFiles = file.listFiles();
		if (listFiles == null) {
			return toAttach;
		}
		for (File member : listFiles) {
			File memberFile = new File(dir, member.getName());
			memberFile.deleteOnExit();
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
				File toAttachChild = getToAttach(memberFile.getPath(), new TFile(memberFile), mediaTester, toAttach);
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
	 * @param file
	 *            file to check
	 * @return is it a well-known format
	 */
	public boolean isSupportedMedia(File file) {
		return cartFileFilter.accept(file) || tuneFileFilter.accept(file) || diskFileFilter.accept(file)
				|| tapeFileFilter.accept(file);
	}

}
