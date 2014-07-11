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
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.web.WebEngine;
import javafx.stage.Modality;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.tuneinfos.TuneInfos;

public class WebView extends Tab implements UIPart {
	private static final BiPredicate<File, File> LEXICALLY_FIRST_MEDIA = (file,
			toAttach) -> toAttach == null
			|| file.getName().compareTo(toAttach.getName()) < 0;

	@FXML
	private Button backward, forward;
	@FXML
	private javafx.scene.web.WebView webView;
	@FXML
	private TextField urlField;
	@FXML
	private ToggleButton showTuneInfoButton;
	
	private Convenience convenience;
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

	private boolean showTuneInfos;

	public WebView(final C64Window window, final Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	public void setType(WebViewType type) {
		setId(type.name());
		setURL(type.getUrl());
		setText(util.getBundle().getString(getId()));
		this.type.set(type);
	}

	@FXML
	private void initialize() {
		convenience = new Convenience(util.getPlayer());
		convenience.setAutoStartedFile((file) -> {
			if (showTuneInfos
					&& PathUtils.getExtension(file.getName()).equalsIgnoreCase(
							".sid")) {
				showTuneInfos(util.getPlayer().getTune(), file);
			}
		});
		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> engine.load(url));
		engine = webView.getEngine();
		engine.getHistory()
				.currentIndexProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							backward.setDisable(newValue.intValue() <= 0);
							forward.setDisable(newValue.intValue() + 1 >= engine
									.getHistory().getEntries().size());

						});
		engine.locationProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							urlField.setText(newValue);
							try {
								if (convenience.autostart(new URL(newValue),
										LEXICALLY_FIRST_MEDIA, null)) {
									util.setPlayingTab(this);
								}
							} catch (IOException | SidTuneError
									| URISyntaxException e) {
								// ignore
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
		engine.getHistory().go(-1);
	}

	@FXML
	private void reload() {
		engine.reload();
	}

	@FXML
	private void home() {
		engine.load(url);
	}

	@FXML
	private void forward() {
		engine.getHistory().go(1);
	}

	@FXML
	private void setUrl() {
		engine.load(urlField.getText());
	}

	@FXML
	private void setShowTuneInfos() {
		showTuneInfos = showTuneInfoButton.isSelected();
	}

	private void showTuneInfos(SidTune sidTune, File tuneFile) {
		TuneInfos tuneInfos = new TuneInfos(util.getPlayer());

		tuneInfos.getStage().initModality(Modality.WINDOW_MODAL);
		tuneInfos.getStage().initOwner(urlField.getScene().getWindow());

		tuneInfos.open();
		tuneInfos.showTuneInfos(tuneFile, sidTune);
	}

	public final String getCollectionURL() {
		return url;
	}

	public final void setURL(String url) {
		this.url = url;
	}

}
