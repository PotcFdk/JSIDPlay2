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
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.player.PlayList;
import sidplay.player.State;
import ui.JSidPlay2Main;
import ui.about.About;
import ui.asm.Asm;
import ui.assembly64.Assembly64;
import ui.audiosettings.AudioSettings;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Toast;
import ui.common.UIPart;
import ui.common.filefilter.CartFileExtensions;
import ui.common.filefilter.DiskFileExtensions;
import ui.common.filefilter.TapeFileExtensions;
import ui.common.filefilter.TuneFileExtensions;
import ui.common.util.DesktopUtil;
import ui.console.Console;
import ui.disassembler.Disassembler;
import ui.diskcollection.DiskCollectionType;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.C1541Section;
import ui.entities.config.Configuration;
import ui.entities.config.PrinterSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.ViewEntity;
import ui.favorites.Favorites;
import ui.gamebase.GameBase;
import ui.joysticksettings.JoystickSettings;
import ui.musiccollection.MusicCollectionType;
import ui.oscilloscope.Oscilloscope;
import ui.printer.Printer;
import ui.proxysettings.ProxySettings;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.ultimate64.Ultimate64Window;
import ui.update.Update;
import ui.videoscreen.Video;
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
	protected MenuItem video, oscilloscope, favorites, hvsc, cgsc, hvmec, demos, mags, sidDump, sidRegisters, asm,
			disassembler, assembly64, csdb, remixKwedOrg, lemon64, forum64, c64Sk, soasc, codebase64, gamebase,
			jsidplay2Src, printer, console, jsidplay2userGuide, jsidplay2Javadoc, save, previous, next;

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
				}
			});
		}

	}

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

		((ObservableList<ViewEntity>) util.getConfig().getViews())
				.addListener((ListChangeListener<ViewEntity>) c -> updateMenuItems());
		updateMenuItems();

		updatePlayerButtons(util.getPlayer().getPlayList());

		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		util.getPlayer().setWhatsSidHook(musicInfoWithConfidence -> {
			Platform.runLater(() -> {
				System.out.println(musicInfoWithConfidence);
				Toast.makeText("whatssid", whatssidPositioner, musicInfoWithConfidence.toString(), 5);
			});
		});
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
	}

	private void updateMenuItems() {
		for (MenuItem menuItem : Arrays.asList(video, oscilloscope, favorites, hvsc, cgsc, hvmec, demos, mags, sidDump,
				sidRegisters, asm, disassembler, assembly64, csdb, remixKwedOrg, lemon64, forum64, c64Sk, soasc,
				codebase64, gamebase, jsidplay2Src, printer, console, jsidplay2userGuide, jsidplay2Javadoc)) {
			menuItem.setDisable(false);
		}
		util.getConfig().getViews().stream().map(ViewEntity::getFxId).forEach(fxId -> {
			if (Video.ID.equals(fxId)) {
				video.setDisable(true);
			} else if (Oscilloscope.ID.equals(fxId)) {
				oscilloscope.setDisable(true);
			} else if (Favorites.ID.equals(fxId)) {
				favorites.setDisable(true);
			} else if (MusicCollectionType.HVSC.name().equals(fxId)) {
				hvsc.setDisable(true);
			} else if (MusicCollectionType.CGSC.name().equals(fxId)) {
				cgsc.setDisable(true);
			} else if (DiskCollectionType.HVMEC.name().equals(fxId)) {
				hvmec.setDisable(true);
			} else if (DiskCollectionType.DEMOS.name().equals(fxId)) {
				demos.setDisable(true);
			} else if (DiskCollectionType.MAGS.name().equals(fxId)) {
				mags.setDisable(true);
			} else if (SidDump.ID.equals(fxId)) {
				sidDump.setDisable(true);
			} else if (SidReg.ID.equals(fxId)) {
				sidRegisters.setDisable(true);
			} else if (Asm.ID.equals(fxId)) {
				asm.setDisable(true);
			} else if (Disassembler.ID.equals(fxId)) {
				disassembler.setDisable(true);
			} else if (Assembly64.ID.equals(fxId)) {
				assembly64.setDisable(true);
			} else if (WebViewType.CSDB.name().equals(fxId)) {
				csdb.setDisable(true);
			} else if (WebViewType.REMIX_KWED_ORG.name().equals(fxId)) {
				remixKwedOrg.setDisable(true);
			} else if (WebViewType.LEMON64_COM.name().equals(fxId)) {
				lemon64.setDisable(true);
			} else if (WebViewType.FORUM64_DE.name().equals(fxId)) {
				forum64.setDisable(true);
			} else if (WebViewType.C64_SK.name().equals(fxId)) {
				c64Sk.setDisable(true);
			} else if (WebViewType.SOASC.name().equals(fxId)) {
				soasc.setDisable(true);
			} else if (WebViewType.CODEBASE64.name().equals(fxId)) {
				codebase64.setDisable(true);
			} else if (GameBase.ID.equals(fxId)) {
				gamebase.setDisable(true);
			} else if (WebViewType.JSIDPLAY2_SRC.name().equals(fxId)) {
				jsidplay2Src.setDisable(true);
			} else if (Printer.ID.equals(fxId)) {
				printer.setDisable(true);
			} else if (Console.ID.equals(fxId)) {
				console.setDisable(true);
			} else if (WebViewType.USERGUIDE.name().equals(fxId)) {
				jsidplay2userGuide.setDisable(true);
			} else if (WebViewType.JSIDPLAY2_JAVADOC.name().equals(fxId)) {
				jsidplay2Javadoc.setDisable(true);
			} else {
				throw new RuntimeException("Unknown view ID: " + fxId);
			}
		});
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
				util.getPlayer().play(SidTune.load(file));
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
		util.getPlayer().play(RESET);
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
	private void proxySettings() {
		new ProxySettings(util.getPlayer()).open();
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
		addView(Video.ID);
	}

	@FXML
	private void oscilloscope() {
		addView(Oscilloscope.ID);
	}

	@FXML
	private void hvsc() {
		addView(MusicCollectionType.HVSC.name());
	}

	@FXML
	private void cgsc() {
		addView(MusicCollectionType.CGSC.name());
	}

	@FXML
	private void hvmec() {
		addView(DiskCollectionType.HVMEC.name());
	}

	@FXML
	private void demos() {
		addView(DiskCollectionType.DEMOS.name());
	}

	@FXML
	private void mags() {
		addView(DiskCollectionType.MAGS.name());
	}

	@FXML
	private void favorites() {
		addView(Favorites.ID);
	}

	@FXML
	private void gamebase() {
		addView(GameBase.ID);
	}

	@FXML
	private void asm() {
		addView(Asm.ID);
	}

	@FXML
	private void printer() {
		addView(Printer.ID);
	}

	@FXML
	private void console() {
		addView(Console.ID);
	}

	@FXML
	private void sidDump() {
		addView(SidDump.ID);
	}

	@FXML
	private void sidRegisters() {
		addView(SidReg.ID);
	}

	@FXML
	private void disassembler() {
		addView(Disassembler.ID);
	}

	@FXML
	private void assembly64() {
		addView(Assembly64.ID);
	}

	@FXML
	private void csdb() {
		addView(WebViewType.CSDB.name());
	}

	@FXML
	private void codebase64() {
		addView(WebViewType.CODEBASE64.name());
	}

	@FXML
	private void remixKweqOrg() {
		addView(WebViewType.REMIX_KWED_ORG.name());
	}

	@FXML
	private void c64Sk() {
		addView(WebViewType.C64_SK.name());
	}

	@FXML
	private void forum64() {
		addView(WebViewType.FORUM64_DE.name());
	}

	@FXML
	private void lemon64() {
		addView(WebViewType.LEMON64_COM.name());
	}

	@FXML
	private void soasc() {
		addView(WebViewType.SOASC.name());
	}

	@FXML
	private void jsidplay2Src() {
		addView(WebViewType.JSIDPLAY2_SRC.name());
	}

	@FXML
	private void jsidplay2Javadoc() {
		addView(WebViewType.JSIDPLAY2_JAVADOC.name());
	}

	@FXML
	private void userguide() {
		addView(WebViewType.USERGUIDE.name());
	}

	@FXML
	private void updateCheck() {
		new Update(util.getPlayer()).open();
	}

	@FXML
	private void about() {
		new About(util.getPlayer()).open();
	}

	private void addView(String fxId) {
		util.getConfig().getViews().add(new ViewEntity(fxId));
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

	private C1541 getFirstFloppy() {
		return util.getPlayer().getFloppies()[0];
	}

	private void createHardCopy(String format) {
		video();
		try {
			Image vicImage = Video.getVicImage();
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
				DesktopUtil.open(file);
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
