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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.NumberToStringConverter;
import ui.common.UIPart;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.virtualKeyboard.Keyboard;

public class Video extends C64VBox implements UIPart, Consumer<int[]> {

	public static final String ID = "VIDEO";
	private static final double PAL_MARGIN_LEFT = 55;
	private static final double PAL_MARGIN_RIGHT = 55;
	private static final double PAL_MARGIN_TOP = 45;
	private static final double PAL_MARGIN_BOTTOM = 55;
	private static final double NTSC_MARGIN_LEFT = 55;
	private static final double NTSC_MARGIN_RIGHT = 55;
	private static final double NTSC_MARGIN_TOP = 38;
	private static final double NTSC_MARGIN_BOTTOM = 48;

	@FXML
	private TitledPane monitor;
	@FXML
	private Canvas screen;
	@FXML
	private ImageView monitorBorder, breadbox, pc64;
	@FXML
	private Slider scaling, brightness, contrast, gamma, saturation, phaseShift, offset, tint, blur, bleed;
	@FXML
	private CheckBox applyImmediately, showMonitorBorder;
	@FXML
	private Label scalingValue, brightnessValue, contrastValue, gammaValue, saturationValue, phaseShiftValue,
			offsetValue, tintValue, blurValue, bleedValue;
	@FXML
	private ImageView datasetteOff, datasetteLoad, datasetteSave, c1541Off, c1541On, c1541Load, c1541IIOff, c1541IIOn,
			c1541IILoad;
	@FXML
	private Label tapeName, diskName, cartridgeName;

	private VIC vic;
	private Keyboard virtualKeyboard;
	private Timeline timer;

	private List<Image> imageQueue;

	/**
	 * Note: volatile, because Screen Updater thread writes it and javafx thread
	 * reads it!
	 */
	private volatile Image currentImage;

	private double marginLeft, marginRight, marginTop, marginBottom;

	private ScheduledService<Void> screenUpdateService;

	private PropertyChangeListener stateListener;

	public Video() {
	}

	public Video(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	protected void initialize() {
		vic = util.getPlayer().getC64().getVIC();
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

		brightness.setLabelFormatter(new NumberToStringConverter<>(2));
		brightness.valueProperty().bindBidirectional(sidplay2Section.brightnessProperty());
		brightnessValue.textProperty().bindBidirectional(sidplay2Section.brightnessProperty(),
				new NumberToStringConverter<>(2));
		brightness.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setBrightness(newValue.floatValue()), applyImmediately.isSelected()));

		contrast.setLabelFormatter(new NumberToStringConverter<>(2));
		contrast.valueProperty().bindBidirectional(sidplay2Section.contrastProperty());
		contrastValue.textProperty().bindBidirectional(sidplay2Section.contrastProperty(),
				new NumberToStringConverter<>(2));
		contrast.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setContrast(newValue.floatValue()), applyImmediately.isSelected()));

		gamma.setLabelFormatter(new NumberToStringConverter<>(2));
		gamma.valueProperty().bindBidirectional(sidplay2Section.gammaProperty());
		gammaValue.textProperty().bindBidirectional(sidplay2Section.gammaProperty(), new NumberToStringConverter<>(2));
		gamma.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setGamma(newValue.floatValue()), applyImmediately.isSelected()));

		saturation.setLabelFormatter(new NumberToStringConverter<>(2));
		saturation.valueProperty().bindBidirectional(sidplay2Section.saturationProperty());
		saturationValue.textProperty().bindBidirectional(sidplay2Section.saturationProperty(),
				new NumberToStringConverter<>(2));
		saturation.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setSaturation(newValue.floatValue()), applyImmediately.isSelected()));

		phaseShift.setLabelFormatter(new NumberToStringConverter<>(2));
		phaseShift.valueProperty().bindBidirectional(sidplay2Section.phaseShiftProperty());
		phaseShiftValue.textProperty().bindBidirectional(sidplay2Section.phaseShiftProperty(),
				new NumberToStringConverter<>(2));
		phaseShift.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setPhaseShift(newValue.floatValue()), applyImmediately.isSelected()));

		offset.setLabelFormatter(new NumberToStringConverter<>(2));
		offset.valueProperty().bindBidirectional(sidplay2Section.offsetProperty());
		offsetValue.textProperty().bindBidirectional(sidplay2Section.offsetProperty(),
				new NumberToStringConverter<>(2));
		offset.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setOffset(newValue.floatValue()), applyImmediately.isSelected()));

		tint.setLabelFormatter(new NumberToStringConverter<>(2));
		tint.valueProperty().bindBidirectional(sidplay2Section.tintProperty());
		tintValue.textProperty().bindBidirectional(sidplay2Section.tintProperty(), new NumberToStringConverter<>(2));
		tint.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setTint(newValue.floatValue()), applyImmediately.isSelected()));

		blur.setLabelFormatter(new NumberToStringConverter<>(2));
		blur.valueProperty().bindBidirectional(sidplay2Section.blurProperty());
		blurValue.textProperty().bindBidirectional(sidplay2Section.blurProperty(), new NumberToStringConverter<>(2));
		blur.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setLuminanceC(newValue.floatValue()), applyImmediately.isSelected()));

		bleed.setLabelFormatter(new NumberToStringConverter<>(2));
		bleed.valueProperty().bindBidirectional(sidplay2Section.bleedProperty());
		bleedValue.textProperty().bindBidirectional(sidplay2Section.bleedProperty(), new NumberToStringConverter<>(2));
		bleed.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setDotCreep(newValue.floatValue()), applyImmediately.isSelected()));

		showMonitorBorder.selectedProperty().bindBidirectional(sidplay2Section.showMonitorProperty());

		screenUpdateService = new ScheduledService<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {

					public Void call() throws InterruptedException {
						synchronized (imageQueue) {
							// prevent image buffer overflow, if we run out of sync by dropping frames
							int size = imageQueue.size() / 10;
							for (int i = 0; size > 1 && i < imageQueue.size(); i += imageQueue.size() / size) {
								imageQueue.remove(i);
							}
							if (!imageQueue.isEmpty()) {
								currentImage = imageQueue.remove(0);
							}
						}
						screen.getGraphicsContext2D().drawImage(currentImage, 0, 0, currentImage.getWidth(),
								currentImage.getHeight(), marginLeft, marginTop,
								screen.getWidth() - (marginLeft + marginRight),
								screen.getHeight() - (marginTop + marginBottom));
						return null;
					}

				};
			}
		};

		imageQueue = new ArrayList<>();
		SidTune tune = util.getPlayer().getTune();
		setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune));
		setVisibilityBasedOnChipType(tune);

		setupKeyboard();

		updatePeripheralImages();

		screenUpdateService.start();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(stateListener);
		util.getPlayer().removePixelConsumer(this);
		screenUpdateService.cancel();
		timer.stop();
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
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
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
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(DiskFileExtensions.DESCRIPTION, DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(screen.getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
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
		util.getPlayer().configureVICs(vic -> vic.updatePalette());
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
		if (cpuClock == CPUClock.PAL) {
			marginLeft = PAL_MARGIN_LEFT;
			marginRight = PAL_MARGIN_RIGHT;
			marginTop = PAL_MARGIN_TOP;
			marginBottom = PAL_MARGIN_BOTTOM;
		} else {
			marginLeft = NTSC_MARGIN_LEFT;
			marginRight = NTSC_MARGIN_RIGHT;
			marginTop = NTSC_MARGIN_TOP;
			marginBottom = NTSC_MARGIN_BOTTOM;
		}

		ScheduledExecutorService schdExctr = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = defaultThreadFactory.newThread(runnable);
				thread.setDaemon(true);
				thread.setPriority(Thread.MAX_PRIORITY);
				return thread;
			}
		});
		imageQueue.clear();
		screenUpdateService.setExecutor(schdExctr);
		screenUpdateService.setPeriod(Duration.millis(1000. / cpuClock.getScreenRefresh()));

		screen.getGraphicsContext2D().clearRect(0, 0, screen.widthProperty().get(), screen.heightProperty().get());
		screen.setWidth(vic.getBorderWidth());
		screen.setHeight(vic.getBorderHeight());
		updateScaling();
	}

	private void updateScaling() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		double scale = sidplay2Section.getVideoScaling();
		screen.setScaleX(scale);
		screen.setScaleY(scale);
		monitor.setPrefHeight(Integer.MAX_VALUE);
		for (ImageView imageView : Arrays.asList(monitorBorder, breadbox, pc64)) {
			imageView.setScaleX(
					scale * screen.getWidth() / (imageView.getImage().getWidth() + marginLeft + marginRight));
			imageView.setScaleY(
					scale * screen.getHeight() / (imageView.getImage().getHeight() + marginTop + marginBottom));
		}
	}

	/**
	 * Connect Keyboard with C64 keyboard.
	 */
	private void setupKeyboard() {
		monitor.setOnKeyPressed(event -> {
			event.consume();
			KeyTableEntry keyTableEntry = util.getConfig().getKeyTabEntry(event.getCode().getName());

			if (event.isShiftDown()) {
				pressC64Key(KeyTableEntry.SHIFT_LEFT);
			}
			if (event.isControlDown()) {
				pressC64Key(KeyTableEntry.COMMODORE);
			}

			if (keyTableEntry != null) {
				pressC64Key(keyTableEntry);
				releaseC64Key(keyTableEntry);

				if (util.getConfig().getEmulationSection().isEnableUltimate64()) {
					Platform.runLater(() -> {
						util.getPlayer().sendCommand(util.getConfig(), String.valueOf(event.getText()));
					});
				}
			}

			if (event.isShiftDown()) {
				releaseC64Key(KeyTableEntry.SHIFT_LEFT);
			}
			if (event.isControlDown()) {
				releaseC64Key(KeyTableEntry.COMMODORE);
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
		util.getPlayer().removePixelConsumer(this);
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
			util.getPlayer().addPixelConsumer(this);
		}
	}

	private void updateVICChipConfiguration(Consumer<VIC> action, boolean apply) {
		util.getPlayer().configureVICs(action.andThen(vic -> {
			if (apply)
				vic.updatePalette();
		}));
	}

	/**
	 * Create an image per frame of VIC screen output.
	 * 
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(int[] pixels) {
		synchronized (imageQueue) {
			if (imageQueue.size() > 150) {
				// prevent OutOfMemoryError, just in case!
				imageQueue.clear();
			}
			imageQueue.add(createImage(pixels));
		}
	}

	private Image createImage(int[] pixels) {
		WritableImage image = new WritableImage(vic.getBorderWidth(), vic.getBorderHeight());
		image.getPixelWriter().setPixels(0, 0, vic.getBorderWidth(), vic.getBorderHeight(),
				PixelFormat.getIntArgbInstance(), pixels, 0, vic.getBorderWidth());
		return image;
	}

	/**
	 * @return VIC image with current frame
	 */
	public Image getVicImage() {
		return currentImage;
	}

}
