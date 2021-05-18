package ui.videoscreen;

import static sidplay.ini.IniDefaults.DEFAULT_BLEED;
import static sidplay.ini.IniDefaults.DEFAULT_BLUR;
import static sidplay.ini.IniDefaults.DEFAULT_BRIGHTNESS;
import static sidplay.ini.IniDefaults.DEFAULT_CONTRAST;
import static sidplay.ini.IniDefaults.DEFAULT_GAMMA;
import static sidplay.ini.IniDefaults.DEFAULT_OFFSET;
import static sidplay.ini.IniDefaults.DEFAULT_PHASE_SHIFT;
import static sidplay.ini.IniDefaults.DEFAULT_SATURATION;
import static sidplay.ini.IniDefaults.DEFAULT_TINT;
import static ui.entities.config.SidPlay2Section.DEFAULT_VIDEO_SCALING;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.util.Duration;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.components.c1530.Datasette.DatasetteStatus;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyStatus;
import libsidplay.components.c1541.FloppyType;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.mos656x.VIC;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import sidplay.audio.VideoDriver;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.common.UIPart;
import ui.common.converter.NumberToStringConverter;
import ui.common.filefilter.CartFileExtensions;
import ui.common.filefilter.DiskFileExtensions;
import ui.common.filefilter.TapeFileExtensions;
import ui.entities.config.EmulationSection;
import ui.entities.config.KeyTableEntity;
import ui.entities.config.SidPlay2Section;
import ui.virtualKeyboard.Keyboard;

public class Video extends C64VBox implements UIPart, VideoDriver {

	public static final String ID = "VIDEO";

	// monitorBorder transformation parameters
	private static final double SCALE_X = 1.2;
	private static final double PAL_SCALE_Y = 1.2;
	private static final double NTSC_SCALE_Y = 1.0;
	private static final int TRANSLATE_Y = 8;

	@FXML
	private TitledPane monitor;
	@FXML
	private Canvas screen;
	@FXML
	private ImageView monitorBorder, breadbox, pc64;
	@FXML
	private Slider scaling, brightness, contrast, gamma, saturation, phaseShift, offset, tint, blur, bleed;
	@FXML
	private CheckBox palEmulation, applyImmediately, showMonitorBorder;
	@FXML
	private Label scalingValue, brightnessValue, contrastValue, gammaValue, saturationValue, phaseShiftValue,
			offsetValue, tintValue, blurValue, bleedValue;
	@FXML
	private ImageView datasetteOff, datasetteLoad, datasetteSave, c1541Off, c1541On, c1541Load, c1541IIOff, c1541IIOn,
			c1541IILoad;
	@FXML
	private Label tapeName, diskName, cartridgeName;

	private Keyboard virtualKeyboard;

	private Timeline timer;

	private ImageQueue<Image> imageQueue;

	/**
	 * Note: volatile, because Screen Updater thread writes it and javafx thread
	 * reads it!
	 */
	private volatile Image currentImage;

	private double scaleY;

	private PauseTransition pauseTransition;
	private SequentialTransition sequentialTransition;

	private PropertyChangeListener stateListener;

	public Video() {
		super();
	}

	public Video(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		stateListener = event -> {
			if (event.getNewValue() == State.START) {
				Platform.runLater(() -> {
					SidTune tune = util.getPlayer().getTune();
					setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune));
					setVisibilityBasedOnChipType(tune);
				});
			}
		};
		util.getPlayer().stateProperty().addListener(stateListener);

		scaling.setLabelFormatter(new NumberToStringConverter<>(2));
		scaling.valueProperty().bindBidirectional(sidplay2Section.videoScalingProperty());
		scalingValue.textProperty().bindBidirectional(sidplay2Section.videoScalingProperty(),
				new NumberToStringConverter<>(2));
		scaling.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (applyImmediately.isSelected()) {
				updateScaling();
			}
		});

		palEmulation.selectedProperty().bindBidirectional(sidplay2Section.palEmulationProperty());
		palEmulation.selectedProperty().addListener((observable, oldValue, newValue) -> util.getPlayer()
				.configureVICs(vic -> vic.getPalEmulation().setPalEmulationEnable(newValue)));

		brightness.setLabelFormatter(new NumberToStringConverter<>(2));
		brightness.valueProperty().bindBidirectional(sidplay2Section.brightnessProperty());
		brightnessValue.textProperty().bindBidirectional(sidplay2Section.brightnessProperty(),
				new NumberToStringConverter<>(2));
		brightness.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setBrightness(newValue.floatValue()),
						applyImmediately.isSelected()));

		contrast.setLabelFormatter(new NumberToStringConverter<>(2));
		contrast.valueProperty().bindBidirectional(sidplay2Section.contrastProperty());
		contrastValue.textProperty().bindBidirectional(sidplay2Section.contrastProperty(),
				new NumberToStringConverter<>(2));
		contrast.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setContrast(newValue.floatValue()),
						applyImmediately.isSelected()));

		gamma.setLabelFormatter(new NumberToStringConverter<>(2));
		gamma.valueProperty().bindBidirectional(sidplay2Section.gammaProperty());
		gammaValue.textProperty().bindBidirectional(sidplay2Section.gammaProperty(), new NumberToStringConverter<>(2));
		gamma.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setGamma(newValue.floatValue()),
						applyImmediately.isSelected()));

		saturation.setLabelFormatter(new NumberToStringConverter<>(2));
		saturation.valueProperty().bindBidirectional(sidplay2Section.saturationProperty());
		saturationValue.textProperty().bindBidirectional(sidplay2Section.saturationProperty(),
				new NumberToStringConverter<>(2));
		saturation.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setSaturation(newValue.floatValue()),
						applyImmediately.isSelected()));

		phaseShift.setLabelFormatter(new NumberToStringConverter<>(2));
		phaseShift.valueProperty().bindBidirectional(sidplay2Section.phaseShiftProperty());
		phaseShiftValue.textProperty().bindBidirectional(sidplay2Section.phaseShiftProperty(),
				new NumberToStringConverter<>(2));
		phaseShift.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setPhaseShift(newValue.floatValue()),
						applyImmediately.isSelected()));

		offset.setLabelFormatter(new NumberToStringConverter<>(2));
		offset.valueProperty().bindBidirectional(sidplay2Section.offsetProperty());
		offsetValue.textProperty().bindBidirectional(sidplay2Section.offsetProperty(),
				new NumberToStringConverter<>(2));
		offset.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setOffset(newValue.floatValue()),
						applyImmediately.isSelected()));

		tint.setLabelFormatter(new NumberToStringConverter<>(2));
		tint.valueProperty().bindBidirectional(sidplay2Section.tintProperty());
		tintValue.textProperty().bindBidirectional(sidplay2Section.tintProperty(), new NumberToStringConverter<>(2));
		tint.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setTint(newValue.floatValue()),
						applyImmediately.isSelected()));

		blur.setLabelFormatter(new NumberToStringConverter<>(2));
		blur.valueProperty().bindBidirectional(sidplay2Section.blurProperty());
		blurValue.textProperty().bindBidirectional(sidplay2Section.blurProperty(), new NumberToStringConverter<>(2));
		blur.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setLuminanceC(newValue.floatValue()),
						applyImmediately.isSelected()));

		bleed.setLabelFormatter(new NumberToStringConverter<>(2));
		bleed.valueProperty().bindBidirectional(sidplay2Section.bleedProperty());
		bleedValue.textProperty().bindBidirectional(sidplay2Section.bleedProperty(), new NumberToStringConverter<>(2));
		bleed.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
						vic -> vic.getPalEmulation().getPalette().setDotCreep(newValue.floatValue()),
						applyImmediately.isSelected()));

		showMonitorBorder.selectedProperty().bindBidirectional(sidplay2Section.showMonitorProperty());

		pauseTransition = new PauseTransition();
		sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
			Image image = imageQueue.pull();
			if (image != null) {
				currentImage = image;
				// memory leak prevention!?
				// https://github.com/kasemir/org.csstudio.display.builder/issues/174
				screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
				screen.getGraphicsContext2D().drawImage(image, 0, 0);
			}
		});
		sequentialTransition.setCycleCount(Animation.INDEFINITE);

		imageQueue = new ImageQueue<>();

		SidTune tune = util.getPlayer().getTune();
		setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune));
		setVisibilityBasedOnChipType(tune);

		setupKeyboard();

		updatePeripheralImages();

		sequentialTransition.playFromStart();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(stateListener);
		util.getPlayer().removeVideoDriver(this);
		sequentialTransition.stop();
		timer.stop();
		imageQueue.dispose();
		currentImage = null;
	}

	@FXML
	private void showVirtualKeyboard() {
		virtualKeyboard = new Keyboard(util.getPlayer());
		virtualKeyboard.getStage().initModality(Modality.WINDOW_MODAL);
		virtualKeyboard.getStage().initOwner(screen.getScene().getWindow());
		virtualKeyboard.open();
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TapeFileExtensions.DESCRIPTION, TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(screen.getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertTape(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(DiskFileExtensions.DESCRIPTION, DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(screen.getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(screen.getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(CartridgeType.CRT, file);
				util.getPlayer().play(SidTune.RESET);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void apply() {
		updateScaling();
		util.getPlayer().configureVICs(vic -> vic.getPalEmulation().updatePalette());
	}

	@FXML
	private void showMonitorBorder() {
		util.getConfig().getSidplay2Section().setShowMonitor(showMonitorBorder.isSelected());
		setVisibilityBasedOnChipType(util.getPlayer().getTune());
	}

	@FXML
	private void defaultPalette() {
		applyImmediately.setSelected(false);

		scaling.setValue(DEFAULT_VIDEO_SCALING);
		brightness.setValue(DEFAULT_BRIGHTNESS);
		contrast.setValue(DEFAULT_CONTRAST);
		gamma.setValue(DEFAULT_GAMMA);
		saturation.setValue(DEFAULT_SATURATION);
		phaseShift.setValue(DEFAULT_PHASE_SHIFT);
		offset.setValue(DEFAULT_OFFSET);
		tint.setValue(DEFAULT_TINT);
		blur.setValue(DEFAULT_BLUR);
		bleed.setValue(DEFAULT_BLEED);
		apply();

		util.getConfig().getSidplay2Section().setShowMonitor(true);
		showMonitorBorder();

		applyImmediately.setSelected(true);
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen(final CPUClock cpuClock) {
		scaleY = (cpuClock == CPUClock.PAL) ? PAL_SCALE_Y : NTSC_SCALE_Y;
		pauseTransition.setDuration(Duration.millis(1000. / cpuClock.getScreenRefresh()));

		screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
		screen.setWidth(util.getPlayer().getC64().getVIC().getBorderWidth());
		screen.setHeight(util.getPlayer().getC64().getVIC().getBorderHeight());
		updateScaling();
	}

	private void updateScaling() {
		final SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		double scale = sidplay2Section.getVideoScaling();
		for (Node node : Arrays.asList(monitorBorder, screen, breadbox, pc64)) {
			node.setScaleX(scale);
			node.setScaleY(scale);
		}
		monitor.setPrefHeight(Integer.MAX_VALUE);
		// adjust monitorBorder to surround screen
		monitorBorder.setScaleX(monitorBorder.getScaleX() * SCALE_X);
		monitorBorder.setScaleY(monitorBorder.getScaleY() * scaleY);
		monitorBorder.setTranslateY(TRANSLATE_Y * monitorBorder.getScaleY());
	}

	/**
	 * Connect Keyboard with C64 keyboard.
	 */
	private void setupKeyboard() {
		monitor.setOnKeyPressed(event -> {
			event.consume();
			Optional<KeyTableEntry> keyTableEntry = util.getConfig().getKeyCodeMap().stream()
					.filter(keyCode -> keyCode.getKeyCodeName().equals(event.getCode().getName()))
					.map(KeyTableEntity::getEntry).findFirst();

			if (event.isShiftDown()) {
				pressC64Key(KeyTableEntry.SHIFT_LEFT);
			}
			if (event.isControlDown()) {
				pressC64Key(KeyTableEntry.COMMODORE);
			}

			if (keyTableEntry.isPresent()) {
				pressC64Key(keyTableEntry.get());
				releaseC64Key(keyTableEntry.get());
			}

			if (event.isControlDown()) {
				releaseC64Key(KeyTableEntry.COMMODORE);
			}
			if (event.isShiftDown()) {
				releaseC64Key(KeyTableEntry.SHIFT_LEFT);
			}
			// prevent focus traversal using cursor keys or tab!
			monitor.requestFocus();
		});
	}

	private void pressC64Key(final KeyTableEntry key) {
		util.getPlayer().getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						util.getPlayer().getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	private void releaseC64Key(final KeyTableEntry key) {
		util.getPlayer().getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						util.getPlayer().getC64().getKeyboard().keyReleased(key);
					}
				});
	}

	private void updatePeripheralImages() {
		final Duration duration = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(duration, evt -> {
			DatasetteStatus datasetteStatus = util.getPlayer().getDatasette().getStatus();
			tapeName.setText(util.getPlayer().getDatasette().getTapeImage().getName());
			switch (datasetteStatus) {
			case OFF:
				datasetteOff.setVisible(true);
				for (ImageView imageView : Arrays.asList(datasetteLoad, datasetteSave)) {
					imageView.setVisible(false);
				}
				break;
			case LOAD:
				datasetteLoad.setVisible(true);
				for (ImageView imageView : Arrays.asList(datasetteOff, datasetteSave)) {
					imageView.setVisible(false);
				}
				break;
			case SAVE:
				datasetteSave.setVisible(true);
				for (ImageView imageView : Arrays.asList(datasetteOff, datasetteLoad)) {
					imageView.setVisible(false);
				}
				break;

			default:
				throw new RuntimeException("Unexpected datasette status: " + datasetteStatus);
			}
			final C1541 firstC1541 = util.getPlayer().getFloppies()[0];
			diskName.setText(firstC1541.getDiskName());
			FloppyStatus floppyStatus = firstC1541.getStatus();
			switch (floppyStatus) {
			case OFF:
				c1541Off.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
				c1541IIOff.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
				for (ImageView imageView : Arrays.asList(c1541On, c1541IIOn, c1541Load, c1541IILoad)) {
					imageView.setVisible(false);
				}
				break;
			case ON:
				c1541On.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
				c1541IIOn.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
				for (ImageView imageView : Arrays.asList(c1541Off, c1541IIOff, c1541Load, c1541IILoad)) {
					imageView.setVisible(false);
				}
				break;
			case LOAD:
				c1541Load.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
				c1541IILoad.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
				for (ImageView imageView : Arrays.asList(c1541Off, c1541IIOff, c1541On, c1541IIOn)) {
					imageView.setVisible(false);
				}
				break;

			default:
				throw new RuntimeException("Unexpected floppy status: " + floppyStatus);
			}
			cartridgeName.setText(util.getPlayer().getC64().getCartridge().toString());
		});
		timer = new Timeline(oneFrame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	/**
	 * Make breadbox/pc64 image visible, if the internal SID player is used.
	 */
	private void setVisibilityBasedOnChipType(final SidTune sidTune) {
		util.getPlayer().removeVideoDriver(this);
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		if (sidTune != SidTune.RESET && sidTune.getInfo().getPlayAddr() != 0) {
			// SID Tune is loaded and uses internal player?
			screen.setVisible(false);
			monitorBorder.setVisible(false);
			if (ChipModel.getChipModel(emulationSection, sidTune, 0) == ChipModel.MOS6581) {
				// Old SID chip model? Show breadbox
				breadbox.setVisible(true);
				pc64.setVisible(false);
			} else {
				// New SID chip model? Show PC 64
				pc64.setVisible(true);
				breadbox.setVisible(false);
			}
		} else {
			// Normal RESET: Show video screen and monitor
			breadbox.setVisible(false);
			pc64.setVisible(false);
			screen.setVisible(true);
			monitorBorder.setVisible(showMonitorBorder.isSelected());
			util.getPlayer().addVideoDriver(this);
		}
	}

	private void updateVICChipConfiguration(Consumer<VIC> action, boolean apply) {
		util.getPlayer().configureVICs(action.andThen(vic -> {
			if (apply) {
				vic.getPalEmulation().updatePalette();
			}
		}));
	}

	/**
	 * Create an image per frame of VIC screen output.
	 *
	 * @see java.util.function.BiConsumer#accept(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void accept(VIC vic) {
		WritableImage image = new WritableImage(vic.getBorderWidth(), vic.getBorderHeight());
		image.getPixelWriter().setPixels(0, 0, vic.getBorderWidth(), vic.getBorderHeight(),
				PixelFormat.getIntArgbInstance(), vic.getPixels().array(), 0, vic.getBorderWidth());
		imageQueue.push(image);
	}

	/**
	 * @return VIC image with current frame
	 */
	public Image getVicImage() {
		return currentImage;
	}

}
