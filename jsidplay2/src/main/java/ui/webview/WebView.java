package ui.webview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class WebView extends Tab implements UIPart {
	public static final String CSDB_ID = "CSDB";

	private static final String CSDB_URL = "http://csdb.dk/";

	@FXML
	private javafx.scene.web.WebView webView;

	private ObjectProperty<WebViewType> type;
	private String url;

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
		webView.getEngine()
				.locationProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							System.out.println("url=" + newValue);
							if (newValue.endsWith(".prg")) {
								try (InputStream stream = new URL(newValue)
										.openConnection().getInputStream()) {
									util.getPlayer().play(
											SidTune.load(newValue, stream));
								} catch (IOException | SidTuneError e) {
									System.err.println(e.getMessage());
								}
							}
						});
	}

	public String getCollectionURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

}
