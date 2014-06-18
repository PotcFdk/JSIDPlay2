package ui.webview;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import libsidplay.Player;
import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TapeFileFilter;
import ui.filefilter.TuneFileFilter;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

public class WebView extends Tab implements UIPart {
	public static final String CSDB_ID = "CSDB";
	public static final String CODEBASE64_ID = "CODEBASE64";

	private static final String CSDB_URL = "http://csdb.dk/";
	private static final String CODEBASE64_URL = "http://codebase64.org/";

	private static final String ZIP_EXT = ".zip";
	private static final String CRT_EXT = ".crt";

	@FXML
	private javafx.scene.web.WebView webView;

	private ObjectProperty<WebViewType> type;
	private String url;
	private WebEngine engine;
	private final TuneFileFilter tuneFilter = new TuneFileFilter();
	private final FileFilter diskFileFilter = new DiskFileFilter();
	private final FileFilter tapeFileFilter = new TapeFileFilter();

	private UIUtil util;

	private ChangeListener<? super Number> changeListener = (observable,
			oldValue, newValue) -> {
		DoubleProperty progressProperty = util.progressProperty(webView);
		if (progressProperty != null) {
			progressProperty.setValue(newValue);
		}
	};

	public WebView(final C64Window window, final Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	public void setType(WebViewType type) {
		switch (type) {
		case CSDB:
			setId(CSDB_ID);
			setURL(CSDB_URL);
			break;
		case CODEBASE64:
			setId(CODEBASE64_ID);
			setURL(CODEBASE64_URL);
			break;
		default:
			break;
		}
		setText(util.getBundle().getString(getId()));
		this.type.set(type);
	}

	@FXML
	private void initialize() {
		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> engine.load(url));
		engine = webView.getEngine();
		engine.locationProperty().addListener(
				(observable, oldValue, newValue) -> handleDownload(newValue));
		engine.getLoadWorker().progressProperty().addListener(changeListener);
	}

	@Override
	public void doClose() {
		engine.getLoadWorker().progressProperty()
				.removeListener(changeListener);
	}

	@FXML
	private void backward() {
		if (engine.getHistory().getCurrentIndex() > 0) {
			engine.getHistory().go(-1);
		}
	}

	@FXML
	private void reload() {
		engine.reload();
	}

	@FXML
	private void forward() {
		if (engine.getHistory().getCurrentIndex() < engine.getHistory()
				.getMaxSize()) {
			engine.getHistory().go(1);
		}
	}

	public final String getCollectionURL() {
		return url;
	}

	public final void setURL(String url) {
		this.url = url;
	}

	/**
	 * Analyze URL to download and run SidTune, Programs, Disks,Tapes and
	 * cartridges.
	 * 
	 * @param url
	 *            download URL
	 */
	private void handleDownload(String url) {
		File file = new File(url);
		try (InputStream in = new URL(url).openConnection().getInputStream()) {
			if (file.getName().toLowerCase(Locale.US).endsWith(ZIP_EXT)) {
				TFile zip = copyZipToTmp(in, file.getName());
				TFile.cp_rp(zip, new File(util.getPlayer().getConfig()
						.getSidplay2().getTmpDir()), TArchiveDetector.ALL);
				File first = getFirstMedia(util.getPlayer().getConfig()
						.getSidplay2().getTmpDir(), zip, null);
				TFile.rm_r(zip);
				if (first.getName().toLowerCase(Locale.US).endsWith(CRT_EXT)) {
					attachAndRunCartridge(first);
				} else if (diskFileFilter.accept(first)) {
					attachAndRunDisk(first);
				} else if (tapeFileFilter.accept(first)) {
					attachAndRunTape(first);
				}
			} else if (file.getName().toLowerCase(Locale.US).endsWith(CRT_EXT)) {
				attachAndRunCartridge(copyToTmp(in, file.getName()));
			} else if (tuneFilter.accept(file)) {
				playTune(url, in);
			} else if (diskFileFilter.accept(file)) {
				attachAndRunDisk(copyToTmp(in, file.getName()));
			} else if (tapeFileFilter.accept(file)) {
				attachAndRunTape(copyToTmp(in, file.getName()));
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Get first media file to attach (lexically first one), search recursively.
	 */
	private File getFirstMedia(String dir, TFile file, File toAttach) {
		for (TFile member : file.listFiles()) {
			File memberFile = new File(dir, member.getName());
			memberFile.deleteOnExit();
			if (memberFile.isFile()) {
				if (toAttach == null
						|| member.getName().compareTo(toAttach.getName()) < 0) {
					toAttach = memberFile;
				}
			} else if (memberFile.isDirectory()) {
				return getFirstMedia(memberFile.getPath(),
						new TFile(memberFile), toAttach);
			}
		}
		return toAttach;
	}

	/**
	 * Copy archive to temporary directory.
	 */
	private TFile copyZipToTmp(InputStream in, String out) throws IOException {
		TFile dst = new TFile(util.getPlayer().getConfig().getSidplay2()
				.getTmpDir(), out);
		if (!dst.exists()) {
			TFile.cp(in, dst);
		}
		return dst;
	}

	/**
	 * Copy file to temporary directory
	 */
	private File copyToTmp(InputStream in, String out) throws IOException {
		File dst = new File(util.getPlayer().getConfig().getSidplay2()
				.getTmpDir(), out);
		dst.deleteOnExit();
		if (!dst.exists()) {
			TFile.cp(in, dst);
		}
		return dst;
	}

	private void playTune(String url, InputStream in) throws IOException,
			SidTuneError {
		util.getPlayer().play(SidTune.load(url, in));
	}

	private void attachAndRunDisk(File dst) throws IOException, SidTuneError {
		util.getPlayer().getC64().ejectCartridge();
		util.getPlayer().insertDisk(dst, null);
		util.getPlayer().setCommand("LOAD\"*\",8,1\rRUN\r");
		util.getPlayer().play(null);
		util.getPlayer().getConfig().getSidplay2()
				.setLastDirectory(dst.getParent());
	}

	private void attachAndRunTape(File dst) throws IOException, SidTuneError {
		util.getPlayer().getC64().ejectCartridge();
		util.getPlayer().insertTape(dst, null);
		util.getPlayer().setCommand("LOAD\rRUN\r");
		util.getPlayer().play(null);
		util.getPlayer().getConfig().getSidplay2()
				.setLastDirectory(dst.getParent());
	}

	private void attachAndRunCartridge(File dst) throws IOException,
			SidTuneError {
		util.getPlayer().insertCartridge(CartridgeType.CRT, dst, null);
	}
}
