package ui.videoscreen;

import static libsidplay.config.ISidPlay2Section.DEFAULT_BLEED;
import static libsidplay.config.ISidPlay2Section.DEFAULT_BLUR;
import static libsidplay.config.ISidPlay2Section.DEFAULT_BRIGHTNESS;
import static libsidplay.config.ISidPlay2Section.DEFAULT_CONTRAST;
import static libsidplay.config.ISidPlay2Section.DEFAULT_GAMMA;
import static libsidplay.config.ISidPlay2Section.DEFAULT_OFFSET;
import static libsidplay.config.ISidPlay2Section.DEFAULT_PHASE_SHIFT;
import static libsidplay.config.ISidPlay2Section.DEFAULT_SATURATION;
import static libsidplay.config.ISidPlay2Section.DEFAULT_TINT;
import static ui.entities.config.SidPlay2Section.DEFAULT_VIDEO_SCALING;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.util.Duration;
import libsidplay.C64;
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

	@FXML
	private TitledPane monitor;
	@FXML
	private Canvas screen;
	@FXML
	private ImageView monitorBorder, breadbox, pc64;
	@FXML
	private Slider scaling, brightness, contrast, gamma, saturation,
			phaseShift, offset, tint, blur, bleed;
	@FXML
	private CheckBox applyImmediately;
	@FXML
	private Label scalingValue, brightnessValue, contrastValue, gammaValue,
			saturationValue, phaseShiftValue, offsetValue, tintValue,
			blurValue, bleedValue;
	@FXML
	private ImageView datasetteOff, datasetteLoad, datasetteSave, c1541Off,
			c1541On, c1541Load, c1541IIOff, c1541IIOn, c1541IILoad;
	@FXML
	private Label tapeName, diskName, cartridgeName;

	private WritableImage vicImage;
	private Keyboard virtualKeyboard;
	private Timeline timer;

	private WritablePixelFormat<IntBuffer> pixelFormat;

	private UIUtil util;

	private ChangeListener<? super State> stateListener = (arg0, arg1, arg2) -> {
		if (arg2 == State.START) {
			Platform.runLater(() -> {
				setupVideoScreen();
				setVisibilityBasedOnChipType(util.getPlayer().getTune());
			});
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

		util.getPlayer().stateProperty().addListener(stateListener);
		NumberToString<Double> doubleToString = new NumberToString<Double>(2);

		scaling.setLabelFormatter(doubleToString);
		scaling.setValue(sidplay2Section.getVideoScaling());
		scaling.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setVideoScaling(newValue.floatValue());
					if (applyImmediately.isSelected()) {
						updateScaling();
					}
				});
		brightness.setLabelFormatter(doubleToString);
		brightness.setValue(sidplay2Section.getBrightness());
		brightness.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setBrightness(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setBrightness(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		contrast.setLabelFormatter(doubleToString);
		contrast.setValue(sidplay2Section.getContrast());
		contrast.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setContrast(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setContrast(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		gamma.setLabelFormatter(doubleToString);
		gamma.setValue(sidplay2Section.getGamma());
		gamma.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setGamma(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setGamma(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		saturation.setLabelFormatter(doubleToString);
		saturation.setValue(sidplay2Section.getSaturation());
		saturation.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setSaturation(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setSaturation(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		phaseShift.setLabelFormatter(doubleToString);
		phaseShift.setValue(sidplay2Section.getPhaseShift());
		phaseShift.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setPhaseShift(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setPhaseShift(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		offset.setLabelFormatter(doubleToString);
		offset.setValue(sidplay2Section.getOffset());
		offset.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setOffset(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setOffset(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		tint.setLabelFormatter(doubleToString);
		tint.setValue(sidplay2Section.getTint());
		tint.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setTint(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setTint(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		blur.setLabelFormatter(doubleToString);
		blur.setValue(sidplay2Section.getBlur());
		blur.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setBlur(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setLuminanceC(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});
		bleed.setLabelFormatter(doubleToString);
		bleed.setValue(sidplay2Section.getBleed());
		bleed.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					sidplay2Section.setBleed(newValue.floatValue());
					getC64().configureVICs(
							vic -> vic.getPalette().setDotCreep(
									newValue.floatValue()));
					if (applyImmediately.isSelected()) {
						apply();
					}
				});

		NumberToString<Float> floatToString = new NumberToString<Float>(2);
		scalingValue.textProperty().bindBidirectional(
				sidplay2Section.videoScalingProperty(), floatToString);
		brightnessValue.textProperty().bindBidirectional(
				sidplay2Section.brightnessProperty(), floatToString);
		contrastValue.textProperty().bindBidirectional(
				sidplay2Section.contrastProperty(), floatToString);
		gammaValue.textProperty().bindBidirectional(
				sidplay2Section.gammaProperty(), floatToString);
		saturationValue.textProperty().bindBidirectional(
				sidplay2Section.saturationProperty(), floatToString);
		phaseShiftValue.textProperty().bindBidirectional(
				sidplay2Section.phaseShiftProperty(), floatToString);
		offsetValue.textProperty().bindBidirectional(
				sidplay2Section.offsetProperty(), floatToString);
		tintValue.textProperty().bindBidirectional(
				sidplay2Section.tintProperty(), floatToString);
		blurValue.textProperty().bindBidirectional(
				sidplay2Section.blurProperty(), floatToString);
		bleedValue.textProperty().bindBidirectional(
				sidplay2Section.bleedProperty(), floatToString);

		setupVideoScreen();
		setVisibilityBasedOnChipType(util.getPlayer().getTune());

		setupKeyboard();

		updatePeripheralImages();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(stateListener);
		getC64().configureVICs(vic -> vic.setPixelConsumer(pixels -> {
		}));
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TapeFileExtensions.DESCRIPTION,
						TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertTape(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(DiskFileExtensions.DESCRIPTION,
						DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(CartridgeType.CRT, file);
				util.getPlayer().play(SidTune.RESET);

			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void apply() {
		updateScaling();
		getC64().getEventScheduler().scheduleThreadSafe(
				new Event("Update Palette") {
					@Override
					public void event() throws InterruptedException {
						getC64().configureVICs(vic -> vic.updatePalette());
					}
				});
	}

	@FXML
	private void defaultPalette() {
		boolean selected = applyImmediately.isSelected();
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

		applyImmediately.setSelected(selected);
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen() {
		screen.getGraphicsContext2D().clearRect(0, 0,
				screen.widthProperty().get(), screen.heightProperty().get());
		screen.setWidth(getC64().getVIC().getBorderWidth());
		screen.setHeight(getC64().getVIC().getBorderHeight());
		vicImage = new WritableImage(getC64().getVIC().getBorderWidth(),
				getC64().getVIC().getBorderHeight());
		pixelFormat = PixelFormat.getIntArgbInstance();
		updateScaling();
	}

	private void updateScaling() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		double scale = sidplay2Section.getVideoScaling();
		screen.setScaleX(scale);
		screen.setScaleY(scale);
		for (ImageView imageView : Arrays.asList(monitorBorder, breadbox, pc64)) {
			imageView
					.setScaleX(scale
							* screen.getWidth()
							/ (imageView.getImage().getWidth() + MARGIN_LEFT + MARGIN_RIGHT));
			imageView
					.setScaleY(scale
							* screen.getHeight()
							/ (imageView.getImage().getHeight() + MARGIN_TOP + MARGIN_BOTTOM));
		}
	}

	/**
	 * Connect Keyboard with C64 keyboard.
	 */
	private void setupKeyboard() {
		monitor.setOnKeyPressed((event) -> {
			KeyTableEntry keyTableEntry = util.getConfig().getKeyTabEntry(
					event.getCode().getName());

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
		getC64().getEventScheduler().scheduleThreadSafe(
				new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	private void releaseC64Key(final KeyTableEntry key) {
		getC64().getEventScheduler().scheduleThreadSafe(
				new Event("Virtual Keyboard Key Released: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyReleased(key);
					}
				});
	}

	private void updatePeripheralImages() {
		final Duration duration = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(
				duration,
				evt -> {
					DatasetteStatus datasetteStatus = util.getPlayer()
							.getDatasette().getStatus();
					tapeName.setText(util.getPlayer().getDatasette()
							.getTapeImage().getName());
					switch (datasetteStatus) {
					case OFF:
						datasetteOff.setVisible(true);
						for (ImageView imageView : Arrays.asList(datasetteLoad,
								datasetteSave)) {
							imageView.setVisible(false);
						}
						break;
					case LOAD:
						datasetteLoad.setVisible(true);
						for (ImageView imageView : Arrays.asList(datasetteOff,
								datasetteSave)) {
							imageView.setVisible(false);
						}
						break;
					case SAVE:
						datasetteSave.setVisible(true);
						for (ImageView imageView : Arrays.asList(datasetteOff,
								datasetteLoad)) {
							imageView.setVisible(false);
						}
						break;

					default:
						throw new RuntimeException(
								"Unexpected datasette status: "
										+ datasetteStatus);
					}
					final C1541 firstC1541 = util.getPlayer().getFloppies()[0];
					diskName.setText(firstC1541.getDiskName());
					FloppyStatus floppyStatus = firstC1541.getStatus();
					switch (floppyStatus) {
					case OFF:
						c1541Off.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
						c1541IIOff.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
						for (ImageView imageView : Arrays.asList(c1541On,
								c1541IIOn, c1541Load, c1541IILoad)) {
							imageView.setVisible(false);
						}
						break;
					case ON:
						c1541On.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
						c1541IIOn.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
						for (ImageView imageView : Arrays.asList(c1541Off,
								c1541IIOff, c1541Load, c1541IILoad)) {
							imageView.setVisible(false);
						}
						break;
					case LOAD:
						c1541Load.setVisible(firstC1541.getFloppyType() == FloppyType.C1541);
						c1541IILoad.setVisible(firstC1541.getFloppyType() == FloppyType.C1541_II);
						for (ImageView imageView : Arrays.asList(c1541Off,
								c1541IIOff, c1541On, c1541IIOn)) {
							imageView.setVisible(false);
						}
						break;

					default:
						throw new RuntimeException("Unexpected floppy status: "
								+ floppyStatus);
					}
					cartridgeName.setText(getC64().getCartridge().toString());
				});
		timer = new Timeline(oneFrame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	/**
	 * Make breadbox/pc64 image visible, if the internal SID player is used.
	 */
	private void setVisibilityBasedOnChipType(final SidTune sidTune) {
		getC64().configureVICs(vic -> vic.setPixelConsumer(pixels -> {
		}));
		if (sidTune != SidTune.RESET && sidTune.getInfo().getPlayAddr() != 0) {
			// SID Tune is loaded and uses internal player?
			screen.setVisible(false);
			monitorBorder.setVisible(false);
			if (ChipModel.getChipModel(util.getConfig().getEmulationSection(),
					sidTune, 0) == ChipModel.MOS6581) {
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
			monitorBorder.setVisible(true);
			getC64().getVIC().setPixelConsumer(this);
		}
	}

	@Override
	public void accept(int[] pixels) {
		Platform.runLater(() -> {
			final VIC vic = getC64().getVIC();
			// sanity check: don't update during change of CPUClock
			if (vicImage.getHeight() == vic.getBorderHeight()) {
				vicImage.getPixelWriter().setPixels(0, 0, vic.getBorderWidth(),
						vic.getBorderHeight(), pixelFormat, pixels, 0,
						vic.getBorderWidth());
				screen.getGraphicsContext2D().drawImage(vicImage, 0, 0,
						vic.getBorderWidth(), vic.getBorderHeight(),
						MARGIN_LEFT, MARGIN_TOP,
						screen.getWidth() - (MARGIN_LEFT + MARGIN_RIGHT),
						screen.getHeight() - (MARGIN_TOP + MARGIN_BOTTOM));
			}
		});
	}

	public WritableImage getVicImage() {
		return vicImage;
	}

	private C64 getC64() {
		return util.getPlayer().getC64();
	}

}
