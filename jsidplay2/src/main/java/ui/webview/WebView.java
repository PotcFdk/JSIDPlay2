package ui.webview;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.web.WebHistory;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TuneFileFilter;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

public class WebView extends Tab implements UIPart {
	public static final String CSDB_ID = "CSDB";

	private static final String CSDB_URL = "http://csdb.dk/";

	@FXML
	private javafx.scene.web.WebView webView;

	private ObjectProperty<WebViewType> type;
	private String url;
	private final TuneFileFilter tuneFilter = new TuneFileFilter();
	private final FileFilter diskFileFilter = new DiskFileFilter();

	private UIUtil util;

	public WebView(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	public void setType(WebViewType type) {
		switch (type) {
		case CSDB:
			setId(CSDB_ID);
			setURL(CSDB_URL);
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
		type.addListener((observable, oldValue, newValue) -> webView
				.getEngine().load(url));
		webView.getEngine().locationProperty()
				.addListener((observable, oldValue, newValue) -> {
					System.out.println("location=" + newValue);
					handleDownload(newValue);
				});
	}

	@FXML
	private void backward() {
		WebHistory history = webView.getEngine().getHistory();
		history.go(-1);
	}

	@FXML
	private void reload() {
		webView.getEngine().reload();
	}

	@FXML
	private void forward() {
		WebHistory history = webView.getEngine().getHistory();
		history.go(1);
	}

	public final String getCollectionURL() {
		return url;
	}

	public final void setURL(String url) {
		this.url = url;
	}

	private void handleDownload(String url) {
		File file = new File(url);
		try (InputStream in = new URL(url).openConnection()
				.getInputStream()) {
			if (file.getName().endsWith(".zip")) {
				TFile dst = new TFile(util.getPlayer().getConfig()
						.getSidplay2().getTmpDir(), file.getName());
				TFile.cp(in, dst);
				TFile.cp_rp(dst, new File(util.getPlayer().getConfig()
						.getSidplay2().getTmpDir()), TArchiveDetector.ALL);
				File toAttach = null;
				for (TFile member : dst.listFiles()) {
					File memberFile = new File(util.getPlayer().getConfig()
							.getSidplay2().getTmpDir(), member.getName());
					memberFile.deleteOnExit();
					if (toAttach == null
							|| member.getName().compareTo(toAttach.getName()) < 0) {
						toAttach = memberFile;
					}
				}
				TFile.rm_r(dst);
				attachAndRunDisk(toAttach);
			} else if (tuneFilter.accept(file)) {
				util.getPlayer().play(SidTune.load(url, in));
			} else if (diskFileFilter.accept(file)) {
				File dst = new File(util.getPlayer().getConfig().getSidplay2()
						.getTmpDir(), file.getName());
				dst.deleteOnExit();
				TFile.cp(in, dst);
				attachAndRunDisk(dst);
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(e.getMessage());
		}
	}

	private void attachAndRunDisk(File dst) throws IOException, SidTuneError {
		util.getPlayer().getC64().ejectCartridge();
		util.getPlayer().insertDisk(dst, null);
		util.getPlayer().setCommand("LOAD\"*\",8,1\rRUN\r");
		util.getPlayer().play(null);
		util.getPlayer().getConfig().getSidplay2()
				.setLastDirectory(dst.getParent());
	}
}
