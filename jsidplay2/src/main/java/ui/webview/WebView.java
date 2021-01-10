package ui.webview;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;
import org.w3c.dom.html.HTMLImageElement;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.stage.Modality;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.download.DownloadThread;
import ui.common.download.IDownloadListener;
import ui.tuneinfos.TuneInfos;

public class WebView extends C64VBox implements UIPart {

	public class HyperlinkRedirectListener implements ChangeListener<Worker.State>, EventListener {

		private static final String IMG_TAG = "img";
		private static final String CLICK_EVENT = "click";
		private static final String ANCHOR_TAG = "a";

		@Override
		public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
			if (State.SUCCEEDED.equals(newValue)) {
				Document document = webView.getEngine().getDocument();
				if (document == null) {
					return;
				}
				NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
				for (int i = 0; i < anchors.getLength(); i++) {
					Node node = anchors.item(i);
					if (node instanceof EventTarget) {
						EventTarget eventTarget = (EventTarget) node;
						if (eventTarget.toString().matches("^(https?|ftp)://.*$")) {
							eventTarget.addEventListener(CLICK_EVENT, this, false);
						}
					}
				}
				if (type == WebViewType.USERGUIDE) {
					NodeList nodeList = document.getElementsByTagName(IMG_TAG);
					for (int i = 0; i < nodeList.getLength(); i++) {
						HTMLImageElement n = (HTMLImageElement) nodeList.item(i);
						URL m = WebView.class.getResource(WebViewType.toAbsoluteUrl(n.getSrc()));
						if (m != null) {
							n.setSrc(m.toExternalForm());
						}
					}
				}
			}
		}

		@Override
		public void handleEvent(Event event) {
			if (event.getCurrentTarget() instanceof HTMLAnchorElement) {
				HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
				String href = anchorElement.getHref();
				if (href == null) {
					return;
				}
				try {
					if (convenience.isSupportedMedia(new File(href))) {
						// prevent to open media embedded in the browser window
						event.preventDefault();
					}
					// we try to download even unsupported media links
					// (HTTP redirection is very likely)
					new DownloadThread(util.getConfig(), new IDownloadListener() {

						@Override
						public void downloadStop(File downloadedFile) {
							try {
								isDownloading = false;
								Platform.runLater(() -> webView.setCursor(Cursor.DEFAULT));
								if (downloadedFile != null && convenience.autostart(downloadedFile,
										Convenience.LEXICALLY_FIRST_MEDIA, null)) {
									downloadedFile.deleteOnExit();
									Platform.runLater(() -> util.setPlayingTab(WebView.this));
								} else if (downloadedFile != null) {
									downloadedFile.delete();
								}
							} catch (IOException | SidTuneError e) {
								// ignore
							}
						}

						@Override
						public void downloadStep(int step) {
							isDownloading = true;
							Platform.runLater(() -> {
								webView.setCursor(Cursor.WAIT);
								DoubleProperty progressProperty = util.progressProperty(webView.getScene());
								progressProperty.setValue(step / 100.f);
							});
						}
					}, new URL(href), false).start();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}

	}

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
	private WebViewType type;
	private WebEngine engine;
	private boolean isDownloading;

	private ChangeListener<? super Number> progressListener;

	private ChangeListener<? super String> locationListener;

	private HyperlinkRedirectListener hyperlinkRedirectListener;

	private ChangeListener<? super Number> historyListener;

	private boolean showTuneInfos;

	public WebView() {
		super();
	}

	public WebView(final C64Window window, final Player player) {
		super(window, player);
	}

	public void setType(WebViewType type) {
		this.type = type;
		home();
	}

	@FXML
	@Override
	protected void initialize() {
		progressListener = (observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				DoubleProperty progressProperty = util.progressProperty(webView.getScene());
				progressProperty.setValue(newValue);
			});
		};
		locationListener = (observable, oldValue, newValue) -> urlField.setText(newValue);
		hyperlinkRedirectListener = new HyperlinkRedirectListener();
		historyListener = (observable, oldValue, newValue) -> {
			backward.setDisable(newValue.intValue() <= 0);
			forward.setDisable(newValue.intValue() + 1 >= engine.getHistory().getEntries().size());

		};
		convenience = new Convenience(util.getPlayer());
		convenience.setAutoStartedFile(file -> {
			if (showTuneInfos && PathUtils.getFilenameSuffix(file.getName()).equalsIgnoreCase(".sid")) {
				showTuneInfos(util.getPlayer().getTune(), file);
			}
		});
		webView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (isDownloading) {
				event.consume();
			}
		});
		engine = webView.getEngine();
		engine.getHistory().currentIndexProperty().addListener(historyListener);
		engine.locationProperty().addListener(locationListener);
		engine.getLoadWorker().stateProperty().addListener(hyperlinkRedirectListener);
		engine.getLoadWorker().progressProperty().addListener(progressListener);
		zoom.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getConfig().getOnlineSection().setZoom(newValue.doubleValue());
			webView.setZoom(newValue.doubleValue());
		});
		zoom.setValue(util.getConfig().getOnlineSection().getZoom());
	}

	@Override
	public void doClose() {
		engine.getHistory().currentIndexProperty().removeListener(historyListener);
		engine.getLoadWorker().stateProperty().removeListener(hyperlinkRedirectListener);
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
		urlField.setText(type.getUrl());
		setUrl();
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

}
