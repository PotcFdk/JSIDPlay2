package ui.webview;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.BiPredicate;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import libsidplay.Player;
import libsidplay.sidtune.SidTuneError;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class WebView extends Tab implements UIPart {
	public static final String CSDB_ID = "CSDB";
	public static final String CODEBASE64_ID = "CODEBASE64";

	private static final String CSDB_URL = "http://csdb.dk/";
	private static final String CODEBASE64_URL = "http://codebase64.org/";

	private static final BiPredicate<File, File> LEXICALLY_FIRST_MEDIA = (file,
			toAttach) -> toAttach == null
			|| file.getName().compareTo(toAttach.getName()) < 0;

	@FXML
	private javafx.scene.web.WebView webView;

	private ObjectProperty<WebViewType> type;
	private String url;
	private WebEngine engine;

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
		engine.locationProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							try {
								util.getPlayer().autostart(
										new URL(newValue),
										LEXICALLY_FIRST_MEDIA, null);
							} catch (IOException | SidTuneError
									| URISyntaxException e) {
								System.err.println(e.getMessage());
							}
						});
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

}
