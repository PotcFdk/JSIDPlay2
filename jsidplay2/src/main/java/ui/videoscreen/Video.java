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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
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
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.mos656x.VIC;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.NumberToString;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.virtualKeyboard.Keyboard;

public class Video extends Tab implements UIPart, Consumer<int[]> {
	public static final String ID = "VIDEO";
	private static final double MARGIN_LEFT = 55;
	private static final double MARGIN_RIGHT = 55;
	private static final double MARGIN_TOP = 38;
	private static final double MARGIN_BOTTOM = 48;
	private static final int QUEUE_CAPACITY = 60;

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

	private Keyboard virtualKeyboard;
	private Timeline timer;

	private final BlockingQueue<int[]> frameQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
	private int vicFrames;

	private UIUtil util;

	private ScheduledService<Void> screenUpdateService = new ScheduledService<Void>() {
		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				public Void call() throws InterruptedException {
					int[] framePixels = frameQueue.take();
					WritableImage image = createImage(framePixels);
					final VIC vic = util.getPlayer().getC64().getVIC();
					screen.getGraphicsContext2D().drawImage(image, 0, 0, vic.getBorderWidth(), vic.getBorderHeight(),
							MARGIN_LEFT, MARGIN_TOP, screen.getWidth() - (MARGIN_LEFT + MARGIN_RIGHT),
							screen.getHeight() - (MARGIN_TOP + MARGIN_BOTTOM));
					return null;
				}

			};
		}
	};

	private ChangeListener<? super State> stateListener = (obj, oldValue, newValue) -> {
		if (newValue == State.START) {
			Platform.runLater(() -> {
				SidTune tune = util.getPlayer().getTune();
				EmulationSection emulationSection = util.getConfig().getEmulationSection();
				setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune).getRefresh());
				setVisibilityBasedOnChipType(tune);
			});
		} else if (newValue == State.QUIT || newValue == State.END) {
			frameQueue.clear();
		}
	};

	public Video(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		util.getPlayer().stateProperty().addListener(stateListener);

		scaling.setLabelFormatter(new NumberToString<>(2));
		scaling.valueProperty().bindBidirectional(sidplay2Section.videoScalingProperty());
		scalingValue.textProperty().bindBidirectional(sidplay2Section.videoScalingProperty(), new NumberToString<>(2));
		scaling.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (applyImmediately.isSelected()) {
				updateScaling();
			}
		});

		brightness.setLabelFormatter(new NumberToString<>(2));
		brightness.valueProperty().bindBidirectional(sidplay2Section.brightnessProperty());
		brightnessValue.textProperty().bindBidirectional(sidplay2Section.brightnessProperty(), new NumberToString<>(2));
		brightness.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setBrightness(newValue.floatValue()), applyImmediately.isSelected()));

		contrast.setLabelFormatter(new NumberToString<>(2));
		contrast.valueProperty().bindBidirectional(sidplay2Section.contrastProperty());
		contrastValue.textProperty().bindBidirectional(sidplay2Section.contrastProperty(), new NumberToString<>(2));
		contrast.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setContrast(newValue.floatValue()), applyImmediately.isSelected()));

		gamma.setLabelFormatter(new NumberToString<>(2));
		gamma.valueProperty().bindBidirectional(sidplay2Section.gammaProperty());
		gammaValue.textProperty().bindBidirectional(sidplay2Section.gammaProperty(), new NumberToString<>(2));
		gamma.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setGamma(newValue.floatValue()), applyImmediately.isSelected()));

		saturation.setLabelFormatter(new NumberToString<>(2));
		saturation.valueProperty().bindBidirectional(sidplay2Section.saturationProperty());
		saturationValue.textProperty().bindBidirectional(sidplay2Section.saturationProperty(), new NumberToString<>(2));
		saturation.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setSaturation(newValue.floatValue()), applyImmediately.isSelected()));

		phaseShift.setLabelFormatter(new NumberToString<>(2));
		phaseShift.valueProperty().bindBidirectional(sidplay2Section.phaseShiftProperty());
		phaseShiftValue.textProperty().bindBidirectional(sidplay2Section.phaseShiftProperty(), new NumberToString<>(2));
		phaseShift.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setPhaseShift(newValue.floatValue()), applyImmediately.isSelected()));

		offset.setLabelFormatter(new NumberToString<>(2));
		offset.valueProperty().bindBidirectional(sidplay2Section.offsetProperty());
		offsetValue.textProperty().bindBidirectional(sidplay2Section.offsetProperty(), new NumberToString<>(2));
		offset.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setOffset(newValue.floatValue()), applyImmediately.isSelected()));

		tint.setLabelFormatter(new NumberToString<>(2));
		tint.valueProperty().bindBidirectional(sidplay2Section.tintProperty());
		tintValue.textProperty().bindBidirectional(sidplay2Section.tintProperty(), new NumberToString<>(2));
		tint.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setTint(newValue.floatValue()), applyImmediately.isSelected()));

		blur.setLabelFormatter(new NumberToString<>(2));
		blur.valueProperty().bindBidirectional(sidplay2Section.blurProperty());
		blurValue.textProperty().bindBidirectional(sidplay2Section.blurProperty(), new NumberToString<>(2));
		blur.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setLuminanceC(newValue.floatValue()), applyImmediately.isSelected()));

		bleed.setLabelFormatter(new NumberToString<>(2));
		bleed.valueProperty().bindBidirectional(sidplay2Section.bleedProperty());
		bleedValue.textProperty().bindBidirectional(sidplay2Section.bleedProperty(), new NumberToString<>(2));
		bleed.valueProperty().addListener((observable, oldValue, newValue) -> updateVICChipConfiguration(
				vic -> vic.getPalette().setDotCreep(newValue.floatValue()), applyImmediately.isSelected()));

		showMonitorBorder.selectedProperty().bindBidirectional(sidplay2Section.showMonitorProperty());

		SidTune tune = util.getPlayer().getTune();
		setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune).getRefresh());
		setVisibilityBasedOnChipType(tune);

		setupKeyboard();

		updatePeripheralImages();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(stateListener);
		util.getPlayer().configureVICs(vic -> vic.setPixelConsumer(pixels -> frameQueue.clear()));
		screenUpdateService.cancel();
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
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
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
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
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
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(screen.getScene().getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(CartridgeType.CRT, file);
				util.getPlayer().play(SidTune.RESET);

			} catch (IOException | SidTuneError e) {
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
	private void setupVideoScreen(final double refresh) {
		ScheduledExecutorService schdExctr = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = defaultThreadFactory.newThread(r);
				t.setPriority(Thread.MAX_PRIORITY);
				return t;
			}
		});
		screenUpdateService.setExecutor(schdExctr);
		screenUpdateService.setPeriod(Duration.millis(1000. / refresh));
		frameQueue.clear();
		vicFrames = 0;

		screen.getGraphicsContext2D().clearRect(0, 0, screen.widthProperty().get(), screen.heightProperty().get());
		screen.setWidth(util.getPlayer().getC64().getVIC().getBorderWidth());
		screen.setHeight(util.getPlayer().getC64().getVIC().getBorderHeight());
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
					scale * screen.getWidth() / (imageView.getImage().getWidth() + MARGIN_LEFT + MARGIN_RIGHT));
			imageView.setScaleY(
					scale * screen.getHeight() / (imageView.getImage().getHeight() + MARGIN_TOP + MARGIN_BOTTOM));
		}
	}

	/**
	 * Connect Keyboard with C64 keyboard.
	 */
	private void setupKeyboard() {
		monitor.setOnKeyPressed(event -> {
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
				event.consume();
			}

			if (event.isShiftDown()) {
				releaseC64Key(KeyTableEntry.SHIFT_LEFT);
			}
			if (event.isControlDown()) {
				releaseC64Key(KeyTableEntry.COMMODORE);
			}
		});
	}

	private void pressC64Key(final KeyTableEntry key) {
		util.getPlayer().getC64().getEventScheduler()
				.scheduleThreadSafe(new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						util.getPlayer().getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	private void releaseC64Key(final KeyTableEntry key) {
		util.getPlayer().getC64().getEventScheduler()
				.scheduleThreadSafe(new Event("Virtual Keyboard Key Released: " + key.name()) {
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

		screenUpdateService.start();
	}

	/**
	 * Make breadbox/pc64 image visible, if the internal SID player is used.
	 */
	private void setVisibilityBasedOnChipType(final SidTune sidTune) {
		util.getPlayer().configureVICs(vic -> vic.setPixelConsumer(pixels -> frameQueue.clear()));
		if (sidTune != SidTune.RESET && sidTune.getInfo().getPlayAddr() != 0) {
			// SID Tune is loaded and uses internal player?
			screen.setVisible(false);
			monitorBorder.setVisible(false);
			if (ChipModel.getChipModel(util.getConfig().getEmulationSection(), sidTune, 0) == ChipModel.MOS6581) {
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
			util.getPlayer().configureVICs(vic -> vic.setPixelConsumer(this));
		}
	}

	private void updateVICChipConfiguration(Consumer<VIC> action, boolean apply) {
		util.getPlayer().configureVICs(action.andThen(vic -> {
			if (apply)
				vic.updatePalette();
		}));
	}

	/**
	 * Queue an image per frame of VIC screen output.
	 * 
	 * Fast forward skips frames and produces output for each Xth frame (X = 1x,
	 * 2x, 3x, ... , 32x).
	 * 
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(int[] pixels) {
		try {
			int fastForwardBitMask = util.getPlayer().getMixerInfo(m -> m.getFastForwardBitMask(), 0);
			if ((vicFrames++ & fastForwardBitMask) == fastForwardBitMask) {
				vicFrames = 0;
				frameQueue.put(pixels);
			}
		} catch (InterruptedException e) {
		}
	}

	public Image getVicImage() {
		return createImage(frameQueue.peek());
	}

	private WritableImage createImage(int[] pixels) {
		final VIC vic = util.getPlayer().getC64().getVIC();
		WritableImage lastFrameImage = new WritableImage(vic.getBorderWidth(), vic.getBorderHeight());
		lastFrameImage.getPixelWriter().setPixels(0, 0, vic.getBorderWidth(), vic.getBorderHeight(),
				PixelFormat.getIntArgbInstance(), pixels, 0, vic.getBorderWidth());
		return lastFrameImage;
	}

}
