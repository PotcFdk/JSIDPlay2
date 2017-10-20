package ui.menubar;

import static ui.entities.config.SidPlay2Section.DEFAULT_FRAME_HEIGHT;
import static ui.entities.config.SidPlay2Section.DEFAULT_FRAME_HEIGHT_MINIMIZED;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.player.PlayList;
import sidplay.player.State;
import ui.JSidPlay2;
import ui.JSidPlay2Main;
import ui.about.About;
import ui.asm.Asm;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.UIUtil;
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
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.filefilter.TuneFileExtensions;
import ui.gamebase.GameBase;
import ui.joysticksettings.JoystickSettings;
import ui.musiccollection.MusicCollection;
import ui.musiccollection.MusicCollectionType;
import ui.oscilloscope.Oscilloscope;
import ui.printer.Printer;
import ui.proxysettings.ProxySettings;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.update.Update;
import ui.videoscreen.Video;
import ui.webview.WebView;
import ui.webview.WebViewType;

public class MenuBar extends VBox implements UIPart {
	/** NUVIE video player */
	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static byte[] NUVIE_PLAYER;
	/** Empty disk image */
	private static final String EMPTY_D64 = "/libsidplay/components/c1541/empty.d64";
	private static byte[] EMPTY_DISK;

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
	protected CheckMenuItem pauseContinue, driveOn, driveSoundOn, parCable, installJiffyDos, expand2000, expand4000,
			expand6000, expand8000, expandA000, turnPrinterOn;
	@FXML
	protected RadioMenuItem fastForward, normalSpeed, c1541, c1541_II, neverExtend, askExtend, accessExtend;
	@FXML
	protected MenuItem previous, next;

	@FXML
	protected Button previous2, next2, nextFavorite;

	@FXML
	private ToggleButton pauseContinue2, fastForward2, minimizeMaximize;

	@FXML
	protected Tooltip previous2ToolTip, next2ToolTip;

	@FXML
	protected Label tracks;

	private class StateChangeListener implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> {
				nextFavoriteDisabledState.set(sidTune == SidTune.RESET || newValue == State.QUIT);
				if (newValue == State.START) {
					setCurrentTrack(sidTune);
					updatePlayerButtons(util.getPlayer().getPlayList());

					final Tab selectedItem = window.getTabbedPane().getSelectionModel().getSelectedItem();
					boolean doNotSwitch = selectedItem != null
							&& (MusicCollection.class.isAssignableFrom(selectedItem.getClass())
									|| Favorites.class.isAssignableFrom(selectedItem.getClass()));
					if (sidTune == SidTune.RESET || (!MP3Tune.class.isAssignableFrom(sidTune.getClass())
							&& sidTune.getInfo().getPlayAddr() == 0 && !doNotSwitch)) {
						video();
					}
				} else if (newValue.equals(State.END)) {
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

	private BooleanProperty nextFavoriteDisabledState;
	private int hardcopyCounter;

	private UIUtil util;
	private JSidPlay2 window;

	public MenuBar(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
		this.window = (JSidPlay2) window;
	}

	@FXML
	private void initialize() {
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final C1541Section c1541Section = config.getC1541Section();
		final PrinterSection printer = config.getPrinterSection();

		minimizeMaximize.selectedProperty().addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				getScene().lookup("#tabbedPane").setVisible(!newValue);
				getScene().getWindow().setHeight(newValue ? DEFAULT_FRAME_HEIGHT_MINIMIZED : DEFAULT_FRAME_HEIGHT);
			});
		});
		minimizeMaximize.selectedProperty().bindBidirectional(sidplay2Section.minimizedProperty());
		
		pauseContinue.selectedProperty().bindBidirectional(pauseContinue2.selectedProperty());
		fastForward2.selectedProperty().bindBidirectional(fastForward.selectedProperty());
		nextFavoriteDisabledState = new SimpleBooleanProperty(true);
		nextFavorite.disableProperty().bind(nextFavoriteDisabledState);
		driveOn.selectedProperty().bindBidirectional(c1541Section.driveOnProperty());
		parCable.selectedProperty().bindBidirectional(c1541Section.parallelCableProperty());
		installJiffyDos.selectedProperty().bindBidirectional(c1541Section.jiffyDosInstalledProperty());
		driveSoundOn.selectedProperty().bindBidirectional(c1541Section.driveSoundOnProperty());
		turnPrinterOn.selectedProperty().bindBidirectional(printer.printerOnProperty());

		FloppyType floppyType = c1541Section.getFloppyType();
		(floppyType == FloppyType.C1541 ? c1541 : c1541_II).setSelected(true);
		ExtendImagePolicy extendImagePolicy = c1541Section.getExtendImagePolicy();
		(extendImagePolicy == ExtendImagePolicy.EXTEND_NEVER ? neverExtend
				: extendImagePolicy == ExtendImagePolicy.EXTEND_ASK ? askExtend : accessExtend).setSelected(true);
		expand2000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled0Property());
		expand4000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled1Property());
		expand6000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled2Property());
		expand8000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled3Property());
		expandA000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled4Property());

		util.getPlayer().stateProperty().addListener(new StateChangeListener());

		updatePlayerButtons(util.getPlayer().getPlayList());

		for (ViewEntity view : config.getViews()) {
			Platform.runLater(() -> addView(view.getFxId()));
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
						util.setPlayingTab(window.getTabbedPane().getTabs().stream()
								.filter((tab) -> tab.getId().equals(Video.ID)).findFirst().get());
						new Convenience(util.getPlayer()).autostart(files.get(0), Convenience.LEXICALLY_FIRST_MEDIA,
								null);
					} catch (IOException | SidTuneError | URISyntaxException e) {
						openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
					}
				}
				event.setDropCompleted(success);
				event.consume();
			});
			window.getTabbedPane().requestFocus();
			util.getPlayer().startC64();
		});
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			try {
				playTune(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	@FXML
	private void playVideo() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
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
		playTune(SidTune.RESET);
	}

	@FXML
	private void quit() {
		window.close();
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
	private void proxySettings() {
		new ProxySettings(util.getPlayer()).open();
	}

	@FXML
	private void emulationSettings() {
		new EmulationSettings(util.getPlayer()).open();
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
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
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
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(DiskFileExtensions.DESCRIPTION, DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
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
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(new ExtensionFilter("Disk Image (D64)", "*.d64"));
		fileDialog.setTitle(util.getBundle().getString("INSERT_EMPTY_DISK"));
		final File file = fileDialog.showSaveDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			File target = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()) + ".d64");
			try (DataOutputStream os = new DataOutputStream(new FileOutputStream(target))) {
				os.write(EMPTY_DISK);
			} catch (IOException e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_WRITE_ERROR"), e.getMessage()));
			}
			video();
			try {
				util.getPlayer().insertDisk(target);
			} catch (IOException | SidTuneError e) {
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
			addTab(new Video(window, util.getPlayer()));
		}
	}

	@FXML
	private void oscilloscope() {
		if (!tabAlreadyOpen(Oscilloscope.ID)) {
			addTab(new Oscilloscope(window, util.getPlayer()));
		}
	}

	@FXML
	private void hvsc() {
		if (!tabAlreadyOpen(MusicCollection.HVSC_ID)) {
			MusicCollection tab = new MusicCollection(window, util.getPlayer());
			tab.setType(MusicCollectionType.HVSC);
			addTab(tab);
		}
	}

	@FXML
	private void cgsc() {
		if (!tabAlreadyOpen(MusicCollection.CGSC_ID)) {
			MusicCollection tab = new MusicCollection(window, util.getPlayer());
			tab.setType(MusicCollectionType.CGSC);
			addTab(tab);
		}
	}

	@FXML
	private void hvmec() {
		if (!tabAlreadyOpen(DiskCollection.HVMEC_ID)) {
			DiskCollection tab = new DiskCollection(window, util.getPlayer());
			tab.setType(DiskCollectionType.HVMEC);
			addTab(tab);
		}
	}

	@FXML
	private void demos() {
		if (!tabAlreadyOpen(DiskCollection.DEMOS_ID)) {
			DiskCollection tab = new DiskCollection(window, util.getPlayer());
			tab.setType(DiskCollectionType.DEMOS);
			addTab(tab);
		}
	}

	@FXML
	private void mags() {
		if (!tabAlreadyOpen(DiskCollection.MAGS_ID)) {
			DiskCollection tab = new DiskCollection(window, util.getPlayer());
			tab.setType(DiskCollectionType.MAGS);
			addTab(tab);
		}
	}

	@FXML
	private void favorites() {
		if (!tabAlreadyOpen(Favorites.ID)) {
			addTab(new Favorites(window, util.getPlayer()));
		}
	}

	@FXML
	private void gamebase() {
		if (!tabAlreadyOpen(GameBase.ID)) {
			addTab(new GameBase(window, util.getPlayer()));
		}
	}

	@FXML
	private void asm() {
		if (!tabAlreadyOpen(Asm.ID)) {
			addTab(new Asm(window, util.getPlayer()));
		}
	}

	@FXML
	private void printer() {
		if (!tabAlreadyOpen(Printer.ID)) {
			addTab(new Printer(window, util.getPlayer()));
		}
	}

	@FXML
	private void console() {
		if (!tabAlreadyOpen(Console.ID)) {
			addTab(new Console(window, util.getPlayer()));
		}
	}

	@FXML
	private void sidDump() {
		if (!tabAlreadyOpen(SidDump.ID)) {
			addTab(new SidDump(window, util.getPlayer()));
		}
	}

	@FXML
	private void sidRegisters() {
		if (!tabAlreadyOpen(SidReg.ID)) {
			addTab(new SidReg(window, util.getPlayer()));
		}
	}

	@FXML
	private void disassembler() {
		if (!tabAlreadyOpen(Disassembler.ID)) {
			addTab(new Disassembler(window, util.getPlayer()));
		}
	}

	@FXML
	private void csdb() {
		if (!tabAlreadyOpen(WebViewType.CSDB.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.CSDB);
			addTab(tab);
		}
	}

	@FXML
	private void codebase64() {
		if (!tabAlreadyOpen(WebViewType.CODEBASE64.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.CODEBASE64);
			addTab(tab);
		}
	}

	@FXML
	private void remixKweqOrg() {
		if (!tabAlreadyOpen(WebViewType.REMIX_KWED_ORG.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.REMIX_KWED_ORG);
			addTab(tab);
		}
	}

	@FXML
	private void sidOth4Com() {
		if (!tabAlreadyOpen(WebViewType.SID_OTH4_COM.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.SID_OTH4_COM);
			addTab(tab);
		}
	}

	@FXML
	private void c64() {
		if (!tabAlreadyOpen(WebViewType.C64_SK.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.C64_SK);
			addTab(tab);
		}
	}

	@FXML
	private void forum64() {
		if (!tabAlreadyOpen(WebViewType.FORUM64_DE.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.FORUM64_DE);
			addTab(tab);
		}
	}

	@FXML
	private void lemon64() {
		if (!tabAlreadyOpen(WebViewType.LEMON64_COM.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.LEMON64_COM);
			addTab(tab);
		}
	}

	@FXML
	private void soasc() {
		if (!tabAlreadyOpen(WebViewType.SOASC.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.SOASC);
			addTab(tab);
		}
	}

	@FXML
	private void jsidplay2() {
		if (!tabAlreadyOpen(WebViewType.JSIDPLAY2_SRC.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.JSIDPLAY2_SRC);
			addTab(tab);
		}
	}

	@FXML
	private void jsidplay2Javadoc() {
		if (!tabAlreadyOpen(WebViewType.JSIDPLAY2_JAVADOC.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.JSIDPLAY2_JAVADOC);
			addTab(tab);
		}
	}

	@FXML
	private void userguide() {
		if (!tabAlreadyOpen(WebViewType.USERGUIDE.name())) {
			WebView tab = new WebView(window, util.getPlayer());
			tab.setType(WebViewType.USERGUIDE);
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
		} else if (MusicCollection.HVSC_ID.equals(id)) {
			hvsc();
		} else if (MusicCollection.CGSC_ID.equals(id)) {
			cgsc();
		} else if (DiskCollection.HVMEC_ID.equals(id)) {
			hvmec();
		} else if (DiskCollection.DEMOS_ID.equals(id)) {
			demos();
		} else if (DiskCollection.MAGS_ID.equals(id)) {
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
		} else if (WebViewType.CSDB.name().equals(id)) {
			csdb();
		} else if (WebViewType.CODEBASE64.name().equals(id)) {
			codebase64();
		} else if (WebViewType.REMIX_KWED_ORG.name().equals(id)) {
			remixKweqOrg();
		} else if (WebViewType.SID_OTH4_COM.name().equals(id)) {
			sidOth4Com();
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
			window.close((UIPart) tab);
			views.removeIf(view -> view.getFxId().equals(tab.getId()));
		});
		window.getTabbedPane().getTabs().add(tab);
		window.getTabbedPane().getSelectionModel().select(tab);
	}

	private boolean tabAlreadyOpen(String fxId) {
		Optional<Tab> alreadyOpened = window.getTabbedPane().getTabs().stream().filter(tab -> tab.getId().equals(fxId))
				.findFirst();
		if (alreadyOpened.isPresent()) {
			window.getTabbedPane().getSelectionModel().select(alreadyOpened.get());
		}
		return alreadyOpened.isPresent();
	}

	private void chooseCartridge(final CartridgeType type) {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(type, file);
				util.getPlayer().play(SidTune.RESET);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert file '%s' as cartridge of type '%s'.",
						file.getAbsolutePath(), type.name()));
			}
		}
	}

	private void insertCartridge(CartridgeType type, int sizeKB) {
		try {
			util.getPlayer().insertCartridge(type, sizeKB);
			util.getPlayer().play(SidTune.RESET);
		} catch (IOException | SidTuneError ex) {
			System.err.println(
					String.format("Cannot insert cartridge of type '%s' and size '%d'KB.", type.name(), sizeKB));
		}
	}

	private void playTune(final SidTune tune) {
		video();
		util.setPlayingTab(window.getTabbedPane().getTabs().stream().filter((tab) -> tab.getId().equals(Video.ID))
				.findFirst().get());
		util.getPlayer().play(tune);
	}

	private C1541 getFirstFloppy() {
		return util.getPlayer().getFloppies()[0];
	}

	private void createHardCopy(String format) {
		video();
		try {
			Video videoScreen = (Video) window.getTabbedPane().getTabs().stream()
					.filter((tab) -> tab.getId().equals(Video.ID)).findFirst().get();
			Image vicImage = videoScreen.getVicImage();
			if (vicImage != null) {
				ImageIO.write(SwingFXUtils.fromFXImage(vicImage, null), format,
						new File(util.getConfig().getSidplay2Section().getTmpDir(),
								"screenshot" + (++hardcopyCounter) + "." + format));
			} else {
				System.err.println("Screenshot not possible, there is currently no frame!");
			}
		} catch (IOException e) {
			e.printStackTrace();
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
	}

	private void playNextRandomHVSC() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		String rndPath = util.getPlayer().getSidDatabaseInfo(db -> db.getRandomPath(), null);
		if (rndPath != null) {
			File file = PathUtils.getFile(rndPath, sidPlay2Section.getHvscFile(), sidPlay2Section.getCgscFile());
			hvsc();
			util.setPlayingTab(window.getTabbedPane().getTabs().stream()
					.filter((tab) -> tab.getId().equals(MusicCollection.HVSC_ID)).findFirst().get());
			try {
				util.getPlayer().play(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	private void setCurrentTrack(SidTune sidTune) {
		StringBuilder trackInfo = new StringBuilder();
		if (sidTune != SidTune.RESET) {
			SidTuneInfo info = sidTune.getInfo();
			Iterator<String> detail = info.getInfoString().iterator();
			if (detail.hasNext()) {
				trackInfo.append(detail.next()).append(' ');
			}
			trackInfo.append(String.format("%2d/%2d", info.getCurrentSong(), info.getSongs()));
		}
		tracks.setText(trackInfo.toString());
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR,"");
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.getDialogPane().setHeaderText(msg);
		alert.showAndWait();
	}
}
