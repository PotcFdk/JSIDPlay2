package ui.menubar;

import static libsidplay.sidtune.SidTune.RESET;
import static ui.common.properties.BindingUtils.bindBidirectional;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.FloppyType;
import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.DesktopIntegration;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.player.PlayList;
import sidplay.player.State;
import ui.JSidPlay2;
import ui.JSidPlay2Main;
import ui.about.About;
import ui.asm.Asm;
import ui.assembly64.Assembly64;
import ui.audiosettings.AudioSettings;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.Toast;
import ui.common.UIPart;
import ui.common.filefilter.CartFileExtensions;
import ui.common.filefilter.DiskFileExtensions;
import ui.common.filefilter.TapeFileExtensions;
import ui.common.filefilter.TuneFileExtensions;
import ui.console.Console;
import ui.disassembler.Disassembler;
import ui.diskcollection.DiskCollection;
import ui.diskcollection.DiskCollectionType;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.C1541Section;
import ui.entities.config.Configuration;
import ui.entities.config.PrinterSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.ViewEntity;
import ui.favorites.Favorites;
import ui.favorites.PlaybackType;
import ui.gamebase.GameBase;
import ui.joysticksettings.JoystickSettings;
import ui.musiccollection.MusicCollection;
import ui.musiccollection.MusicCollectionType;
import ui.oscilloscope.Oscilloscope;
import ui.printer.Printer;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.ultimate64.Ultimate64Window;
import ui.update.Update;
import ui.videoscreen.Video;
import ui.webview.WebView;
import ui.webview.WebViewType;
import ui.whatssidsettings.WhatsSidSettings;

public class MenuBar extends C64VBox implements UIPart {
	/** NUVIE video player */
	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static byte[] NUVIE_PLAYER;
	/** Empty disk image */
	private static final String EMPTY_D64 = "/libsidplay/components/c1541/empty.d64";
	private static byte[] EMPTY_DISK;

	private static final String ACTION_REPLAY_MKVI = "/libsidplay/components/cart/AR_60PAL.CRT";

	static {
		try (DataInputStream is2 = new DataInputStream(MenuBar.class.getResourceAsStream(EMPTY_D64));
				DataInputStream is = new DataInputStream(MenuBar.class.getResourceAsStream(NUVIE_PLAYER_PRG))) {
			URL us3 = JSidPlay2Main.class.getResource(EMPTY_D64);
			EMPTY_DISK = new byte[us3.openConnection().getContentLength()];
			is2.readFully(EMPTY_DISK);
			URL us2 = JSidPlay2Main.class.getResource(NUVIE_PLAYER_PRG);
			NUVIE_PLAYER = new byte[us2.openConnection().getContentLength()];
			is.readFully(NUVIE_PLAYER);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@FXML
	protected CheckMenuItem pauseContinue, turboTape, driveOn, driveSoundOn, parCable, installJiffyDos, expand2000,
			expand4000, expand6000, expand8000, expandA000, turnPrinterOn;

	@FXML
	protected RadioMenuItem fastForward, normalSpeed;

	@FXML
	protected Menu help;

	@FXML
	protected MenuItem save, previous, next;

	@FXML
	protected Button previous2, next2, nextFavorite;

	@FXML
	private ToggleGroup floppyGroup, extensionGroup;

	@FXML
	private ToggleButton pauseContinue2, fastForward2, minimizeMaximize;

	@FXML
	protected Tooltip previous2ToolTip, next2ToolTip;

	@FXML
	protected Label tracks, whatssidPositioner;

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> {
				nextFavoriteDisabledState.set(sidTune == RESET || event.getNewValue() == State.QUIT);
				if (event.getNewValue() == State.START) {
					setCurrentTrack(sidTune);
					updatePlayerButtons(util.getPlayer().getPlayList());

					final Tab selectedItem = jSidPlay2.getTabbedPane().getSelectionModel().getSelectedItem();
					boolean doNotSwitch = selectedItem != null
							&& (MusicCollection.class.isAssignableFrom(selectedItem.getContent().getClass())
									|| Favorites.class.isAssignableFrom(selectedItem.getContent().getClass()));
					if (sidTune == RESET || !MP3Tune.class.isAssignableFrom(sidTune.getClass())
							&& sidTune.getInfo().getPlayAddr() == 0 && !doNotSwitch) {
						video();
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

	private JSidPlay2 jSidPlay2;
	private BooleanProperty nextFavoriteDisabledState;
	private int hardcopyCounter;
	private StateChangeListener propertyChangeListener;

	public MenuBar() {
		super();
	}

	public MenuBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final C1541Section c1541Section = config.getC1541Section();
		final PrinterSection printer = config.getPrinterSection();

		jSidPlay2 = (JSidPlay2) util.getWindow();

		pauseContinue.selectedProperty().bindBidirectional(pauseContinue2.selectedProperty());
		fastForward2.selectedProperty().bindBidirectional(fastForward.selectedProperty());
		nextFavoriteDisabledState = new SimpleBooleanProperty(true);
		nextFavorite.disableProperty().bind(nextFavoriteDisabledState);
		turboTape.selectedProperty().bindBidirectional(sidplay2Section.turboTapeProperty());
		driveOn.selectedProperty().bindBidirectional(c1541Section.driveOnProperty());
		parCable.selectedProperty().bindBidirectional(c1541Section.parallelCableProperty());
		installJiffyDos.selectedProperty().bindBidirectional(c1541Section.jiffyDosInstalledProperty());
		driveSoundOn.selectedProperty().bindBidirectional(c1541Section.driveSoundOnProperty());
		turnPrinterOn.selectedProperty().bindBidirectional(printer.printerOnProperty());

		bindBidirectional(floppyGroup, c1541Section.floppyTypeProperty(), FloppyType.class);

		bindBidirectional(extensionGroup, c1541Section.extendImagePolicyProperty(), ExtendImagePolicy.class);

		expand2000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled0Property());
		expand4000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled1Property());
		expand6000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled2Property());
		expand8000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled3Property());
		expandA000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled4Property());

		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		updatePlayerButtons(util.getPlayer().getPlayList());

		for (ViewEntity view : config.getViews()) {
			Platform.runLater(() -> {
				if (jSidPlay2.getTabbedPane() != null) {
					addView(view.getFxId());
				}
			});
		}

		// preview view does not provide a window
		if (util.getWindow() != null) {
			Stage stage = util.getWindow().getStage();
			stage.maximizedProperty()
					.addListener((observable, oldValue, newValue) -> minimizeMaximize.setDisable(newValue));
			minimizeMaximize.selectedProperty().bindBidirectional(sidplay2Section.minimizedProperty());
			minimizeMaximize.selectedProperty().addListener((observable, oldValue, newValue) -> {
				hideMainTabbedPane(stage, newValue);
				if (newValue) {
					sidplay2Section.setMinimizedX(getScene().getWindow().getX());
					sidplay2Section.setMinimizedY(getScene().getWindow().getY());
					sidplay2Section.setMinimizedWidth(getScene().getWindow().getWidth());
					sidplay2Section.setMinimizedHeight(getScene().getWindow().getHeight());
					resizeToMinHeight(stage);
				} else {
					getScene().getWindow().setX(sidplay2Section.getMinimizedX());
					getScene().getWindow().setY(sidplay2Section.getMinimizedY());
					getScene().getWindow().setWidth(sidplay2Section.getMinimizedWidth());
					getScene().getWindow().setHeight(sidplay2Section.getMinimizedHeight());
				}
			});
			if (sidplay2Section.isMinimized()) {
				Platform.runLater(() -> {
					hideMainTabbedPane(stage, true);
					resizeToMinHeight(stage);
				});
			}
		}
		Platform.runLater(() -> {
			getScene().setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			});
			getScene().setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					try {
						video();
						util.setPlayingTab(jSidPlay2.getTabbedPane().getTabs().stream()
								.filter(tab -> tab.getId().equals(Video.ID)).findFirst().get().getContent());
						new Convenience(util.getPlayer()).autostart(files.get(0), Convenience.LEXICALLY_FIRST_MEDIA,
								null);
					} catch (IOException | SidTuneError e) {
						openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
					}
				}
				event.setDropCompleted(success);
				event.consume();
			});
			jSidPlay2.getTabbedPane().requestFocus();
		});

		util.getPlayer().setWhatsSidHook(musicInfoWithConfidence -> {
			Platform.runLater(() -> {
				System.out.println(musicInfoWithConfidence);
				Toast.makeText("whatssid", whatssidPositioner, musicInfoWithConfidence.toString(), 5);
			});
		});
		util.getPlayer().startC64();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
	}

	private void hideMainTabbedPane(Stage stage, Boolean hide) {
		Node node = getScene().lookup("#tabbedPane");
		node.setVisible(!hide);
		node.setManaged(!hide);
		stage.setResizable(!hide);
	}

	private void resizeToMinHeight(Stage stage) {
		stage.sizeToScene();
		// For Mac OSX:
		Platform.runLater(() -> minimizeMaximize.setDisable(false));
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			try {
				playTune(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	@FXML
	private void save() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showSaveDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			try {
				util.getPlayer().getTune().save(file.getAbsolutePath());
			} catch (IOException e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	@FXML
	private void playVideo() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			final File tmpFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), "nuvieplayer-v1.0.prg");
			tmpFile.deleteOnExit();
			try (DataOutputStream os = new DataOutputStream(new FileOutputStream(tmpFile))) {
				os.write(NUVIE_PLAYER);
				util.getPlayer().insertCartridge(CartridgeType.REU, file);
				util.getPlayer().play(SidTune.load(tmpFile));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	@FXML
	private void reset() {
		playTune(RESET);
	}

	@FXML
	private void quit() {
		util.getWindow().close();
	}

	@FXML
	private void pause() {
		util.getPlayer().pauseContinue();
	}

	@FXML
	private void previousSong() {
		util.getPlayer().previousSong();
	}

	@FXML
	private void nextSong() {
		util.getPlayer().nextSong();
	}

	@FXML
	private void playNormalSpeed() {
		util.getPlayer().configureMixer(mixer -> mixer.normalSpeed());
	}

	@FXML
	private void playFastForward() {
		util.getPlayer().configureMixer(mixer -> mixer.fastForward());
	}

	@FXML
	private void fastForward() {
		if (util.getPlayer().getMixerInfo(mixer -> mixer.isFastForward(), false)) {
			util.getPlayer().configureMixer(mixer -> mixer.normalSpeed());
			normalSpeed.setSelected(true);
		} else {
			util.getPlayer().configureMixer(mixer -> mixer.fastForward());
			fastForward.setSelected(true);
		}
	}

	@FXML
	private void nextFavorite() {
		final C64 c64 = util.getPlayer().getC64();
		final EventScheduler ctx = c64.getEventScheduler();
		ctx.scheduleThreadSafe(new Event("Timer End To Play Next Favorite!") {
			@Override
			public void event() {
				if (util.getPlayer().stateProperty().get() == State.PLAY) {
					util.getPlayer().getTimer().end();
				}
			}
		});
	}

	@FXML
	private void stopSong() {
		util.getPlayer().quit();
	}

	@FXML
	private void doHardcopyGif() {
		createHardCopy("gif");
	}

	@FXML
	private void doHardcopyJpg() {
		createHardCopy("jpg");
	}

	@FXML
	private void doHardcopyPng() {
		createHardCopy("png");
	}

	@FXML
	private void ultimate64() {
		new Ultimate64Window(util.getPlayer()).open();
	}

	@FXML
	private void audioSettings() {
		new AudioSettings(util.getPlayer()).open();
	}

	@FXML
	private void emulationSettings() {
		new EmulationSettings(util.getPlayer()).open();
	}

	@FXML
	private void whatsSidSettings() {
		new WhatsSidSettings(util.getPlayer()).open();
	}

	@FXML
	private void joystickSettings() {
		new JoystickSettings(util.getPlayer()).open();
	}

	@FXML
	private void record() {
		util.getPlayer().getDatasette().control(Datasette.Control.RECORD);
	}

	@FXML
	private void play() {
		util.getPlayer().getDatasette().control(Datasette.Control.START);
	}

	@FXML
	private void rewind() {
		util.getPlayer().getDatasette().control(Datasette.Control.REWIND);
	}

	@FXML
	private void forward() {
		util.getPlayer().getDatasette().control(Datasette.Control.FORWARD);
	}

	@FXML
	private void stop() {
		util.getPlayer().getDatasette().control(Datasette.Control.STOP);
	}

	@FXML
	private void resetCounter() {
		util.getPlayer().getDatasette().control(Datasette.Control.RESET_COUNTER);
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TapeFileExtensions.DESCRIPTION, TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertTape(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void ejectTape() {
		try {
			util.getPlayer().getDatasette().ejectTape();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void turnDriveOn() {
		util.getPlayer().enableFloppyDiskDrives(util.getConfig().getC1541Section().isDriveOn());
	}

	@FXML
	private void parallelCable() {
		util.getPlayer().connectC64AndC1541WithParallelCable(util.getConfig().getC1541Section().isParallelCable());
	}

	@FXML
	private void installJiffyDos() {
		reset();
	}

	@FXML
	private void floppyTypeC1541() {
		getFirstFloppy().setFloppyType(FloppyType.C1541);
		util.getConfig().getC1541Section().setFloppyType(FloppyType.C1541);
	}

	@FXML
	private void floppyTypeC1541_II() {
		getFirstFloppy().setFloppyType(FloppyType.C1541_II);
		util.getConfig().getC1541Section().setFloppyType(FloppyType.C1541_II);
	}

	@FXML
	private void extendNever() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_NEVER);
	}

	@FXML
	private void extendAsk() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_ASK);
	}

	@FXML
	private void extendAccess() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_ACCESS);
	}

	@FXML
	private void expansion0x2000() {
		getFirstFloppy().setRamExpansion(0, util.getConfig().getC1541Section().isRamExpansionEnabled0());
	}

	@FXML
	private void expansion0x4000() {
		getFirstFloppy().setRamExpansion(1, util.getConfig().getC1541Section().isRamExpansionEnabled1());
	}

	@FXML
	private void expansion0x6000() {
		getFirstFloppy().setRamExpansion(2, util.getConfig().getC1541Section().isRamExpansionEnabled2());
	}

	@FXML
	private void expansion0x8000() {
		getFirstFloppy().setRamExpansion(3, util.getConfig().getC1541Section().isRamExpansionEnabled3());
	}

	@FXML
	private void expansion0xA000() {
		getFirstFloppy().setRamExpansion(4, util.getConfig().getC1541Section().isRamExpansionEnabled4());
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(DiskFileExtensions.DESCRIPTION, DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void ejectDisk() {
		try {
			getFirstFloppy().getDiskController().ejectDisk();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@FXML
	private void resetDrive() {
		getFirstFloppy().reset();
	}

	@FXML
	private void insertEmptyDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters().add(new ExtensionFilter("Disk Image (D64)", "*.d64"));
		fileDialog.setTitle(util.getBundle().getString("INSERT_EMPTY_DISK"));
		final File file = fileDialog.showSaveDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			File target = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()) + ".d64");
			try (DataOutputStream os = new DataOutputStream(new FileOutputStream(target))) {
				os.write(EMPTY_DISK);
			} catch (IOException e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_WRITE_ERROR"), e.getMessage()));
			}
			video();
			try {
				util.getPlayer().insertDisk(target);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", target.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void printerOn() {
		util.getPlayer().enablePrinter(util.getConfig().getPrinterSection().isPrinterOn());
	}

	@FXML
	private void insertCartridge() {
		chooseCartridge(CartridgeType.CRT);
	}

	@FXML
	private void insertGeoRAM() {
		chooseCartridge(CartridgeType.GEORAM);
	}

	@FXML
	private void insertGeoRAM64() {
		insertCartridge(CartridgeType.GEORAM, 64);
	}

	@FXML
	private void insertGeoRAM128() {
		insertCartridge(CartridgeType.GEORAM, 128);
	}

	@FXML
	private void insertGeoRAM256() {
		insertCartridge(CartridgeType.GEORAM, 256);
	}

	@FXML
	private void insertGeoRAM512() {
		insertCartridge(CartridgeType.GEORAM, 512);
	}

	@FXML
	private void insertGeoRAM1024() {
		insertCartridge(CartridgeType.GEORAM, 1024);
	}

	@FXML
	private void insertGeoRAM2048() {
		insertCartridge(CartridgeType.GEORAM, 2048);
	}

	@FXML
	private void insertREU() {
		chooseCartridge(CartridgeType.REU);
	}

	@FXML
	private void insertREU128() {
		insertCartridge(CartridgeType.REU, 128);
	}

	@FXML
	private void insertREU256() {
		insertCartridge(CartridgeType.REU, 256);
	}

	@FXML
	private void insertREU512() {
		insertCartridge(CartridgeType.REU, 512);
	}

	@FXML
	private void insertREU2048() {
		insertCartridge(CartridgeType.REU, 2048);
	}

	@FXML
	private void insertREU16384() {
		insertCartridge(CartridgeType.REU, 16384);
	}

	@FXML
	private void insertARMKVI() {
		insertCartridge(MenuBar.class.getResourceAsStream(ACTION_REPLAY_MKVI));
	}

	@FXML
	private void ejectCartridge() {
		util.getPlayer().getC64().ejectCartridge();
		reset();
	}

	@FXML
	private void freezeCartridge() {
		util.getPlayer().getC64().getCartridge().freeze();
	}

	@FXML
	private void video() {
		if (!tabAlreadyOpen(Video.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Video.ID), new Video(util.getWindow(), util.getPlayer()));
			tab.setId(Video.ID);
			addTab(tab);
		}
	}

	@FXML
	private void oscilloscope() {
		if (!tabAlreadyOpen(Oscilloscope.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Oscilloscope.ID),
					new Oscilloscope(util.getWindow(), util.getPlayer()));
			tab.setId(Oscilloscope.ID);
			addTab(tab);
		}
	}

	@FXML
	private void hvsc() {
		if (!tabAlreadyOpen(MusicCollectionType.HVSC.name())) {
			MusicCollection collection = new MusicCollection(util.getWindow(), util.getPlayer());
			collection.setType(MusicCollectionType.HVSC);
			Tab tab = new Tab(util.getBundle().getString(MusicCollectionType.HVSC.name()), collection);
			tab.setId(MusicCollectionType.HVSC.name());
			addTab(tab);
		}
	}

	@FXML
	private void cgsc() {
		if (!tabAlreadyOpen(MusicCollectionType.CGSC.name())) {
			MusicCollection collection = new MusicCollection(util.getWindow(), util.getPlayer());
			collection.setType(MusicCollectionType.CGSC);
			Tab tab = new Tab(util.getBundle().getString(MusicCollectionType.CGSC.name()), collection);
			tab.setId(MusicCollectionType.CGSC.name());
			addTab(tab);
		}
	}

	@FXML
	private void hvmec() {
		if (!tabAlreadyOpen(DiskCollectionType.HVMEC.name())) {
			DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
			collection.setType(DiskCollectionType.HVMEC);
			Tab tab = new Tab(util.getBundle().getString(DiskCollectionType.HVMEC.name()), collection);
			tab.setId(DiskCollectionType.HVMEC.name());
			addTab(tab);
		}
	}

	@FXML
	private void demos() {
		if (!tabAlreadyOpen(DiskCollectionType.DEMOS.name())) {
			DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
			collection.setType(DiskCollectionType.DEMOS);
			Tab tab = new Tab(util.getBundle().getString(DiskCollectionType.DEMOS.name()), collection);
			tab.setId(DiskCollectionType.DEMOS.name());
			addTab(tab);
		}
	}

	@FXML
	private void mags() {
		if (!tabAlreadyOpen(DiskCollectionType.MAGS.name())) {
			DiskCollection collection = new DiskCollection(util.getWindow(), util.getPlayer());
			collection.setType(DiskCollectionType.MAGS);
			Tab tab = new Tab(util.getBundle().getString(DiskCollectionType.MAGS.name()), collection);
			tab.setId(DiskCollectionType.MAGS.name());
			addTab(tab);
		}
	}

	@FXML
	private void favorites() {
		if (!tabAlreadyOpen(Favorites.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Favorites.ID),
					new Favorites(util.getWindow(), util.getPlayer()));
			tab.setId(Favorites.ID);
			addTab(tab);
		}
	}

	@FXML
	private void gamebase() {
		if (!tabAlreadyOpen(GameBase.ID)) {
			Tab tab = new Tab(util.getBundle().getString(GameBase.ID),
					new GameBase(util.getWindow(), util.getPlayer()));
			tab.setId(GameBase.ID);
			addTab(tab);
		}
	}

	@FXML
	private void asm() {
		if (!tabAlreadyOpen(Asm.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Asm.ID), new Asm(util.getWindow(), util.getPlayer()));
			tab.setId(Asm.ID);
			addTab(tab);
		}
	}

	@FXML
	private void printer() {
		if (!tabAlreadyOpen(Printer.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Printer.ID), new Printer(util.getWindow(), util.getPlayer()));
			tab.setId(Printer.ID);
			addTab(tab);
		}
	}

	@FXML
	private void console() {
		if (!tabAlreadyOpen(Console.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Console.ID), new Console(util.getWindow(), util.getPlayer()));
			tab.setId(Console.ID);
			addTab(tab);
		}
	}

	@FXML
	private void sidDump() {
		if (!tabAlreadyOpen(SidDump.ID)) {
			Tab tab = new Tab(util.getBundle().getString(SidDump.ID), new SidDump(util.getWindow(), util.getPlayer()));
			tab.setId(SidDump.ID);
			addTab(tab);
		}
	}

	@FXML
	private void sidRegisters() {
		if (!tabAlreadyOpen(SidReg.ID)) {
			Tab tab = new Tab(util.getBundle().getString(SidReg.ID), new SidReg(util.getWindow(), util.getPlayer()));
			tab.setId(SidReg.ID);
			addTab(tab);
		}
	}

	@FXML
	private void disassembler() {
		if (!tabAlreadyOpen(Disassembler.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Disassembler.ID),
					new Disassembler(util.getWindow(), util.getPlayer()));
			tab.setId(Disassembler.ID);
			addTab(tab);
		}
	}

	@FXML
	private void assembly64() {
		if (!tabAlreadyOpen(Assembly64.ID)) {
			Tab tab = new Tab(util.getBundle().getString(Assembly64.ID),
					new Assembly64(util.getWindow(), util.getPlayer()));
			tab.setId(Assembly64.ID);
			addTab(tab);
		}
	}

	@FXML
	private void csdb() {
		if (!tabAlreadyOpen(WebViewType.CSDB.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.CSDB);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.CSDB.name()), collection);
			tab.setId(WebViewType.CSDB.name());
			addTab(tab);
		}
	}

	@FXML
	private void codebase64() {
		if (!tabAlreadyOpen(WebViewType.CODEBASE64.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.CODEBASE64);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.CODEBASE64.name()), collection);
			tab.setId(WebViewType.CODEBASE64.name());
			addTab(tab);
		}
	}

	@FXML
	private void remixKweqOrg() {
		if (!tabAlreadyOpen(WebViewType.REMIX_KWED_ORG.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.REMIX_KWED_ORG);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.REMIX_KWED_ORG.name()), collection);
			tab.setId(WebViewType.REMIX_KWED_ORG.name());
			addTab(tab);
		}
	}

	@FXML
	private void c64() {
		if (!tabAlreadyOpen(WebViewType.C64_SK.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.C64_SK);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.C64_SK.name()), collection);
			tab.setId(WebViewType.C64_SK.name());
			addTab(tab);
		}
	}

	@FXML
	private void forum64() {
		if (!tabAlreadyOpen(WebViewType.FORUM64_DE.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.FORUM64_DE);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.FORUM64_DE.name()), collection);
			tab.setId(WebViewType.FORUM64_DE.name());
			addTab(tab);
		}
	}

	@FXML
	private void lemon64() {
		if (!tabAlreadyOpen(WebViewType.LEMON64_COM.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.LEMON64_COM);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.LEMON64_COM.name()), collection);
			tab.setId(WebViewType.LEMON64_COM.name());
			addTab(tab);
		}
	}

	@FXML
	private void soasc() {
		if (!tabAlreadyOpen(WebViewType.SOASC.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.SOASC);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.SOASC.name()), collection);
			tab.setId(WebViewType.SOASC.name());
			addTab(tab);
		}
	}

	@FXML
	private void jsidplay2() {
		if (!tabAlreadyOpen(WebViewType.JSIDPLAY2_SRC.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.JSIDPLAY2_SRC);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.JSIDPLAY2_SRC.name()), collection);
			tab.setId(WebViewType.JSIDPLAY2_SRC.name());
			addTab(tab);
		}
	}

	@FXML
	private void jsidplay2Javadoc() {
		if (!tabAlreadyOpen(WebViewType.JSIDPLAY2_JAVADOC.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.JSIDPLAY2_JAVADOC);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.JSIDPLAY2_JAVADOC.name()), collection);
			tab.setId(WebViewType.JSIDPLAY2_JAVADOC.name());
			addTab(tab);
		}
	}

	@FXML
	private void userguide() {
		if (!tabAlreadyOpen(WebViewType.USERGUIDE.name())) {
			WebView collection = new WebView(util.getWindow(), util.getPlayer());
			collection.setType(WebViewType.USERGUIDE);
			Tab tab = new Tab(util.getBundle().getString(WebViewType.USERGUIDE.name()), collection);
			tab.setId(WebViewType.USERGUIDE.name());
			addTab(tab);
		}
	}

	@FXML
	private void updateCheck() {
		new Update(util.getPlayer()).open();
	}

	@FXML
	private void about() {
		new About(util.getPlayer()).open();
	}

	private void addView(String id) {
		if (Video.ID.equals(id)) {
			video();
		} else if (Asm.ID.equals(id)) {
			asm();
		} else if (Oscilloscope.ID.equals(id)) {
			oscilloscope();
		} else if (MusicCollectionType.HVSC.name().equals(id)) {
			hvsc();
		} else if (MusicCollectionType.CGSC.name().equals(id)) {
			cgsc();
		} else if (DiskCollectionType.HVMEC.name().equals(id)) {
			hvmec();
		} else if (DiskCollectionType.DEMOS.name().equals(id)) {
			demos();
		} else if (DiskCollectionType.MAGS.name().equals(id)) {
			mags();
		} else if (GameBase.ID.equals(id)) {
			gamebase();
		} else if (Favorites.ID.equals(id)) {
			favorites();
		} else if (Printer.ID.equals(id)) {
			printer();
		} else if (Console.ID.equals(id)) {
			console();
		} else if (SidDump.ID.equals(id)) {
			sidDump();
		} else if (SidReg.ID.equals(id)) {
			sidRegisters();
		} else if (Disassembler.ID.equals(id)) {
			disassembler();
		} else if (Assembly64.ID.equals(id)) {
			assembly64();
		} else if (WebViewType.CSDB.name().equals(id)) {
			csdb();
		} else if (WebViewType.CODEBASE64.name().equals(id)) {
			codebase64();
		} else if (WebViewType.REMIX_KWED_ORG.name().equals(id)) {
			remixKweqOrg();
		} else if (WebViewType.C64_SK.name().equals(id)) {
			c64();
		} else if (WebViewType.FORUM64_DE.name().equals(id)) {
			forum64();
		} else if (WebViewType.LEMON64_COM.name().equals(id)) {
			lemon64();
		} else if (WebViewType.SOASC.name().equals(id)) {
			soasc();
		} else if (WebViewType.JSIDPLAY2_SRC.name().equals(id)) {
			jsidplay2();
		} else if (WebViewType.JSIDPLAY2_JAVADOC.name().equals(id)) {
			jsidplay2Javadoc();
		} else if (WebViewType.USERGUIDE.name().equals(id)) {
			userguide();
		}
	}

	private void addTab(Tab tab) {
		final List<ViewEntity> views = util.getConfig().getViews();
		if (!views.stream().anyMatch(tool -> tool.getFxId().equals(tab.getId()))) {
			views.add(new ViewEntity(tab.getId()));
		}
		tab.setOnClosed(evt -> {
			util.getWindow().close(tab.getContent());
			views.removeIf(view -> view.getFxId().equals(tab.getId()));
		});
		jSidPlay2.getTabbedPane().getTabs().add(tab);
		jSidPlay2.getTabbedPane().getSelectionModel().select(tab);
	}

	private boolean tabAlreadyOpen(String fxId) {
		Optional<Tab> alreadyOpened = jSidPlay2.getTabbedPane().getTabs().stream()
				.filter(tab -> tab.getId().equals(fxId)).findFirst();
		if (alreadyOpened.isPresent()) {
			jSidPlay2.getTabbedPane().getSelectionModel().select(alreadyOpened.get());
		}
		return alreadyOpened.isPresent();
	}

	private void chooseCartridge(final CartridgeType type) {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(type, file);
				util.getPlayer().play(RESET);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert file '%s' as cartridge of type '%s'.",
						file.getAbsolutePath(), type.name()));
			}
		}
	}

	private void insertCartridge(CartridgeType type, int sizeKB) {
		try {
			util.getPlayer().insertCartridge(type, sizeKB);
			util.getPlayer().play(RESET);
		} catch (IOException ex) {
			System.err.println(
					String.format("Cannot insert cartridge of type '%s' and size '%d'KB.", type.name(), sizeKB));
		}
	}

	private void insertCartridge(InputStream is) {
		try {
			util.getPlayer().insertCartridgeCRT(is);
			util.getPlayer().play(RESET);
		} catch (IOException ex) {
			System.err.println("Cannot insert cartridge of type 'CRT'.");
		}
	}

	private void playTune(final SidTune tune) {
		video();
		util.setPlayingTab(jSidPlay2.getTabbedPane().getTabs().stream().filter(tab -> tab.getId().equals(Video.ID))
				.findFirst().get().getContent());
		util.getPlayer().play(tune);
	}

	private C1541 getFirstFloppy() {
		return util.getPlayer().getFloppies()[0];
	}

	private void createHardCopy(String format) {
		video();
		try {
			Tab tab = jSidPlay2.getTabbedPane().getTabs().stream().filter(tab2 -> tab2.getId().equals(Video.ID))
					.findFirst().get();
			Video videoScreen = (Video) tab.getContent();
			Image vicImage = videoScreen.getVicImage();
			if (vicImage != null) {
				File file = new File(util.getConfig().getSidplay2Section().getTmpDir(),
						"screenshot" + (++hardcopyCounter) + "." + format);
				// Workaround Java8 bug (Remove alpha-channel from jpg)
				if (format.equals("jpg")) {
					BufferedImage image = SwingFXUtils.fromFXImage(vicImage, null);
					BufferedImage vicImageRGB = new BufferedImage(image.getWidth(), image.getHeight(),
							Transparency.OPAQUE);
					Graphics2D graphics = vicImageRGB.createGraphics();
					graphics.drawImage(image, 0, 0, null);
					ImageIO.write(vicImageRGB, format, file);
					graphics.dispose();
				} else {
					ImageIO.write(SwingFXUtils.fromFXImage(vicImage, null), format, file);
				}
				DesktopIntegration.open(file);
			} else {
				System.err.println("Screenshot not possible, there is currently no frame!");
			}
		} catch (IOException e) {
			openErrorDialog(String.format(util.getBundle().getString("ERR_IO_WRITE_ERROR"), e.getMessage()));
		}
	}

	private void updatePlayerButtons(PlayList playList) {
		pauseContinue.setSelected(false);
		normalSpeed.setSelected(true);

		previous.setDisable(!playList.hasPrevious());
		previous2.setDisable(previous.isDisable());
		next.setDisable(!playList.hasNext());
		next2.setDisable(next.isDisable());

		previous.setText(String.format(util.getBundle().getString("PREVIOUS2") + " (%d/%d)", playList.getPrevious(),
				playList.getLength()));
		previous2ToolTip.setText(previous.getText());
		next.setText(String.format(util.getBundle().getString("NEXT2") + " (%d/%d)", playList.getNext(),
				playList.getLength()));
		next2ToolTip.setText(next.getText());

		save.setDisable(util.getPlayer().getTune() == RESET);
	}

	private void playNextRandomHVSC() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		String rndPath = util.getPlayer().getSidDatabaseInfo(db -> db.getRandomPath(), null);
		if (rndPath != null) {
			File file = PathUtils.getFile(rndPath, sidPlay2Section.getHvsc(), sidPlay2Section.getCgsc());
			hvsc();
			util.setPlayingTab(jSidPlay2.getTabbedPane().getTabs().stream()
					.filter(tab -> tab.getId().equals(MusicCollectionType.HVSC.name())).findFirst().get().getContent());
			try {
				util.getPlayer().play(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	private void setCurrentTrack(SidTune sidTune) {
		StringBuilder trackInfo = new StringBuilder();
		StringBuilder trackInfoToolTip = new StringBuilder();
		if (sidTune != RESET) {
			SidTuneInfo info = sidTune.getInfo();
			Iterator<String> detail = info.getInfoString().iterator();
			if (detail.hasNext()) {
				String title = detail.next();
				trackInfo.append(title).append(' ');
				trackInfoToolTip.append(title).append('\n');
			}
			if (detail.hasNext()) {
				trackInfoToolTip.append(detail.next()).append('\n');
			}
			if (detail.hasNext()) {
				trackInfoToolTip.append(detail.next());
			}
			trackInfo.append(String.format("%2d/%2d", info.getCurrentSong(), info.getSongs()));
		}
		tracks.setText(trackInfo.toString());
		tracks.setTooltip(new Tooltip(trackInfoToolTip.toString()));
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}
}
