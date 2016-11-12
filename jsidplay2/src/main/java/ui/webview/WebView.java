package ui.webview;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.web.WebEngine;
import javafx.stage.Modality;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.tuneinfos.TuneInfos;

public class WebView extends Tab implements UIPart {

	@FXML
	private Button backward, forward;
	@FXML
	private javafx.scene.web.WebView webView;
	@FXML
	private TextField urlField;
	@FXML
	private ToggleButton showTuneInfoButton;
	@FXML
	private Slider zoom;

	private Convenience convenience;
	private ObjectProperty<WebViewType> type;
	private String url;
	private WebEngine engine;

	private UIUtil util;

	private ChangeListener<? super Number> progressListener = (observable, oldValue, newValue) -> {
		Platform.runLater(() -> {
			DoubleProperty progressProperty = util.progressProperty(webView);
			progressProperty.setValue(newValue);
		});
	};

	private ChangeListener<? super String> locationListener = (observable, oldValue, newValue) -> {
		try {
			if (!convenience.isSupportedMedia(new File(newValue)))
				return;
			urlField.setText(newValue);
			new DownloadThread(util.getConfig(), new IDownloadListener() {

				@Override
				public void downloadStop(File downloadedFile) {
					try {
						if (downloadedFile != null
								&& convenience.autostart(downloadedFile, Convenience.LEXICALLY_FIRST_MEDIA, null)) {
							downloadedFile.deleteOnExit();
							Platform.runLater(() -> {
								util.setPlayingTab(WebView.this);
							});
						}
					} catch (IOException | SidTuneError | URISyntaxException e) {
						// ignore
					}
				}

				@Override
				public void downloadStep(int step) {
					DoubleProperty progressProperty = util.progressProperty(webView);
					progressProperty.setValue(step / 100.f);
				}
			}, new URL(newValue), false).start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	};

	private ChangeListener<? super Number> historyListener = (observable, oldValue, newValue) -> {
		backward.setDisable(newValue.intValue() <= 0);
		forward.setDisable(newValue.intValue() + 1 >= engine.getHistory().getEntries().size());

	};

	private ChangeListener<? super WebViewType> loadUrlListener = (observable, oldValue, newValue) -> home();

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
		convenience.setAutoStartedFile(file -> {
			if (showTuneInfos && PathUtils.getFilenameSuffix(file.getName()).equalsIgnoreCase(".sid")) {
				showTuneInfos(util.getPlayer().getTune(), file);
			}
		});
		type = new SimpleObjectProperty<>();
		type.addListener(loadUrlListener);
		engine = webView.getEngine();
		engine.getHistory().currentIndexProperty().addListener(historyListener);
		engine.locationProperty().addListener(locationListener);
		engine.getLoadWorker().progressProperty().addListener(progressListener);

		zoom.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getConfig().getOnlineSection().setZoom(newValue.doubleValue());
			webView.setZoom(newValue.doubleValue());
		});
		zoom.setValue(util.getConfig().getOnlineSection().getZoom());
	}

	@Override
	public void doClose() {
		type.removeListener(loadUrlListener);
		engine.getHistory().currentIndexProperty().removeListener(historyListener);
		engine.locationProperty().removeListener(locationListener);
		engine.getLoadWorker().progressProperty().removeListener(progressListener);
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
		Platform.runLater(() -> {
			TuneInfos tuneInfos = new TuneInfos(util.getPlayer());

			tuneInfos.getStage().initModality(Modality.WINDOW_MODAL);
			tuneInfos.getStage().initOwner(urlField.getScene().getWindow());

			tuneInfos.open();
			tuneInfos.showTuneInfos(tuneFile, sidTune);
		});
	}

	public final String getCollectionURL() {
		return url;
	}

	public final void setURL(String url) {
		this.url = url;
	}

}
