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
import libsidplay.components.cart.supported.REU;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
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

	/**
	 * Auto-start commands.
	 */
	private static final String LOAD_8_1_RUN = "LOAD\"*\",8,1\rRUN\r", LOAD_RUN = "LOAD\rRUN\r";

	private static final String ZIP_EXT = ".zip";
	private final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private final CartFileFilter cartFileFilter = new CartFileFilter();

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
	 * Auto-start C64 bundle (ZIP containing well-known formats or un-zipped
	 * entry). Attach specific disk/tape/cartridge and automatically start
	 * entry.<BR>
	 * 
	 * Note: temporary files are removed or marked to be removed on exit.
	 * 
	 * @param url
	 *            URL to open
	 * @param isMediaToAttach
	 *            tester for media to attach
	 * @param autoStartFile
	 *            if media to attach is a disk/tape/cartridge this tune is
	 *            loaded after attaching the media (null means just reset C64,
	 *            instead).
	 * @throws IOException
	 *             image read error
	 * @throws SidTuneError
	 *             invalid tune
	 */
	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, File autoStartFile)
			throws IOException, SidTuneError, URISyntaxException {
		String tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
		String name = file.getName();
		TFile zip = null;
		File toAttach = null;
		try {
			if (name.toLowerCase(Locale.US).endsWith(ZIP_EXT)) {
				// uncompress zip
				zip = new TFile(file);
				TFile.cp_rp(zip, new File(tmpDir), TArchiveDetector.ALL);
				// search media file to attach
				toAttach = getToAttach(tmpDir, zip, isMediaToAttach, null);
			} else if (isSupportedMedia(file)) {
				toAttach = file;
			}
		} finally {
			if (zip != null) {
				TFile.rm_r(zip);
			}
		}
		if (toAttach != null) {
			if (tuneFileFilter.accept(toAttach)) {
				if (!(player.getC64().getCartridge() instanceof REU)) {
					player.getC64().ejectCartridge();
				}
				player.play(SidTune.load(toAttach));
				autoStartedFile.accept(toAttach);
				return true;
			} else if (diskFileFilter.accept(toAttach)) {
				player.getC64().ejectCartridge();
				player.insertDisk(toAttach);
				autoStart(autoStartFile, LOAD_8_1_RUN);
				return true;
			} else if (tapeFileFilter.accept(toAttach)) {
				player.getC64().ejectCartridge();
				player.insertTape(toAttach);
				autoStart(autoStartFile, LOAD_RUN);
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
	 * Check if the file is supported for auto-start.
	 * 
	 * @param file
	 *            file to be checked
	 * @return we support the file to auto-start
	 */
	public boolean isSupported(File file) {
		return file.getName().toLowerCase(Locale.US).endsWith(ZIP_EXT) || tuneFileFilter.accept(file)
				|| diskFileFilter.accept(file) || tapeFileFilter.accept(file) || cartFileFilter.accept(file);

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
			if (memberFile.isFile() && isSupportedMedia(memberFile) && mediaTester.test(memberFile, toAttach)) {

				if (memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
					try {
						player.insertCartridge(CartridgeType.REU, memberFile);
					} catch (IOException | SidTuneError e) {
						e.printStackTrace();
					}
				} else {
					toAttach = memberFile;
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
	private boolean isSupportedMedia(File file) {
		return cartFileFilter.accept(file) || tuneFileFilter.accept(file) || diskFileFilter.accept(file)
				|| tapeFileFilter.accept(file);
	}

}
