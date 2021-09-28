package ui;

import static libsidplay.sidtune.SidTune.RESET;
import static sidplay.Player.LAST_MODIFIED;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.player.State;
import ui.asm.Asm;
import ui.assembly64.Assembly64;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.console.Console;
import ui.disassembler.Disassembler;
import ui.diskcollection.DiskCollection;
import ui.diskcollection.DiskCollectionType;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.ViewEntity;
import ui.favorites.Favorites;
import ui.favorites.PlaybackType;
import ui.gamebase.GameBase;
import ui.musiccollection.MusicCollection;
import ui.musiccollection.MusicCollectionType;
import ui.oscilloscope.Oscilloscope;
import ui.printer.Printer;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.videoscreen.Video;
import ui.webview.WebView;
import ui.webview.WebViewType;

public class JSidPlay2 extends C64Window implements IExtendImageListener {

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> {
				if (event.getNewValue() == State.START) {

					final Tab selectedItem = tabbedPane.getSelectionModel().getSelectedItem();
					boolean doNotSwitch = selectedItem != null
							&& (MusicCollection.class.isAssignableFrom(selectedItem.getContent().getClass())
									|| Favorites.class.isAssignableFrom(selectedItem.getContent().getClass()));
					if (sidTune == RESET || !MP3Tune.class.isAssignableFrom(sidTune.getClass())
							&& sidTune.getInfo().getPlayAddr() == 0 && !doNotSwitch) {
						showView(Video.ID);
					}
				} else if (event.getNewValue().equals(State.END)) {
					SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
					PlaybackType pt = sidplay2Section.getPlaybackType();

					if (!sidplay2Section.isLoop()) {
						if (pt == PlaybackType.RANDOM_HVSC) {
							playNextRandomHVSC();
						}
					}
				}
			});
		}

	}

	@FXML
	protected TabPane tabbedPane;

	private StateChangeListener propertyChangeListener;

	public JSidPlay2() {
		super();
	}

	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	@Override
	protected void initialize() {
		Platform.runLater(() -> {
			// must be delayed, otherwise overridden by C64Window
			String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(LAST_MODIFIED.getTime());
			getStage().setTitle(util.getBundle().getString("TITLE") + String.format(", %s: %s %s",
					util.getBundle().getString("RELEASE"), date, util.getBundle().getString("AUTHOR")));
		});

		((ObservableList<ViewEntity>) util.getConfig().getViews())
				.addListener((ListChangeListener.Change<? extends ViewEntity> c) -> {
					while (c.next()) {
						if (c.wasAdded()) {
							c.getAddedSubList().forEach(view -> addTab(view.getFxId()));
						}
					}
				});
		for (ViewEntity view : util.getConfig().getViews()) {
			Platform.runLater(() -> {
				if (tabbedPane != null) {
					addTab(view.getFxId());
				}
			});
		}

		Platform.runLater(() -> {
			tabbedPane.getScene().setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			});
			tabbedPane.getScene().setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					try {
						showView(Video.ID);
						util.setPlayingTab(tabbedPane.getTabs().stream().filter(tab -> tab.getId().equals(Video.ID))
								.findFirst().get().getContent());
						new Convenience(util.getPlayer()).autostart(files.get(0), Convenience.LEXICALLY_FIRST_MEDIA,
								null);
					} catch (IOException | SidTuneError e) {
						openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
					}
				}
				event.setDropCompleted(success);
				event.consume();
			});
			tabbedPane.requestFocus();
		});

		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		util.getPlayer().setExtendImagePolicy(this);
		util.getPlayer().startC64();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
		util.getPlayer().quit();
		Platform.exit();
	}

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			String msg = util.getBundle().getString("EXTEND_DISK_IMAGE_TO_40_TRACKS");

			// EXTEND_ASK
			Alert alert = new Alert(AlertType.CONFIRMATION, msg);
			alert.setTitle(util.getBundle().getString("EXTEND_DISK_IMAGE"));
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && result.get() == ButtonType.OK;
		} else if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
			// EXTEND_ACCESS
			return true;
		} else {
			// EXTEND_NEVER
			return false;
		}
	}

	private void addTab(String fxId) {
		if (!tabAlreadyOpen(fxId)) {
			if (Video.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Video(util.getWindow(), util.getPlayer())), fxId);
			} else if (Oscilloscope.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Oscilloscope(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (Favorites.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Favorites(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (MusicCollectionType.HVSC.name().equals(fxId)) {
				MusicCollection collection = new MusicCollection(util.getWindow(), util.getPlayer());
				collection.setType(MusicCollectionType.HVSC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (MusicCollectionType.CGSC.name().equals(fxId)) {
				MusicCollection collection = new MusicCollection(util.getWindow(), util.getPlayer());
				collection.setType(MusicCollectionType.CGSC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (DiskCollectionType.HVMEC.name().equals(fxId)) {
				DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
				collection.setType(DiskCollectionType.HVMEC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (DiskCollectionType.DEMOS.name().equals(fxId)) {
				DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
				collection.setType(DiskCollectionType.DEMOS);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (DiskCollectionType.MAGS.name().equals(fxId)) {
				DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
				collection.setType(DiskCollectionType.MAGS);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (SidDump.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new SidDump(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (SidReg.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new SidReg(util.getWindow(), util.getPlayer())), fxId);
			} else if (Asm.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Asm(util.getWindow(), util.getPlayer())), fxId);
			} else if (Disassembler.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Disassembler(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (Assembly64.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Assembly64(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (WebViewType.CSDB.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.CSDB);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.REMIX_KWED_ORG.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.REMIX_KWED_ORG);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.LEMON64_COM.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.LEMON64_COM);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.FORUM64_DE.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.FORUM64_DE);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.C64_SK.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.C64_SK);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.SOASC.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.SOASC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.CODEBASE64.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.CODEBASE64);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (GameBase.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new GameBase(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (WebViewType.JSIDPLAY2_SRC.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.JSIDPLAY2_SRC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (Printer.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Printer(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (Console.ID.equals(fxId)) {
				addTab(new Tab(util.getBundle().getString(fxId), new Console(util.getWindow(), util.getPlayer())),
						fxId);
			} else if (WebViewType.USERGUIDE.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.USERGUIDE);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else if (WebViewType.JSIDPLAY2_JAVADOC.name().equals(fxId)) {
				WebView collection = new WebView(util.getWindow(), util.getPlayer());
				collection.setType(WebViewType.JSIDPLAY2_JAVADOC);
				addTab(new Tab(util.getBundle().getString(fxId), collection), fxId);
			} else {
				throw new RuntimeException("Unknown view ID: " + fxId);
			}
		}
		selectTab(fxId);
	}

	private boolean tabAlreadyOpen(String fxId) {
		return tabbedPane.getTabs().stream().map(Tab::getId).filter(fxId::equals).findFirst().isPresent();
	}

	private void addTab(Tab tab, String fxId) {
		tab.setId(fxId);
		tab.setOnClosed(evt -> {
			util.getWindow().close(tab.getContent());
			util.getConfig().getViews().removeIf(view -> view.getFxId().equals(tab.getId()));
		});
		tabbedPane.getTabs().add(tab);
	}

	private void selectTab(String fxId) {
		tabbedPane.getTabs().stream().filter(tab -> tab.getId().equals(fxId)).findFirst()
				.ifPresent(tabbedPane.getSelectionModel()::select);
	}

	private void playNextRandomHVSC() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		String rndPath = util.getPlayer().getSidDatabaseInfo(db -> db.getRandomPath(), null);
		if (rndPath != null) {
			File file = PathUtils.getFile(rndPath, sidPlay2Section.getHvsc(), sidPlay2Section.getCgsc());
			showView(MusicCollectionType.HVSC.name());
			util.setPlayingTab(tabbedPane.getTabs().stream()
					.filter(tab -> tab.getId().equals(MusicCollectionType.HVSC.name())).findFirst().get().getContent());
			try {
				util.getPlayer().play(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	private void showView(String fxId) {
		if (!util.getConfig().getViews().stream().map(ViewEntity::getFxId).filter(fxId::equals).findFirst()
				.isPresent()) {
			util.getConfig().getViews().add(new ViewEntity(fxId));
		}
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}

}
