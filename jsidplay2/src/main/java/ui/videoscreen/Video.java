package ui.videoscreen;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
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
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.virtualKeyboard.Keyboard;
import de.schlichtherle.truezip.file.TFile;

public class Video extends Tab implements UIPart, Consumer<int[]> {
	public static final String ID = "VIDEO";
	private static final double MONITOR_MARGIN_LEFT = 35;
	private static final double MONITOR_MARGIN_RIGHT = 35;
	private static final double MONITOR_MARGIN_TOP = 28;
	private static final double MONITOR_MARGIN_BOTTOM = 40;

	@FXML
	private TitledPane monitor;
	@FXML
	private Canvas screen;
	@FXML
	private ImageView monitorBorder, breadbox, pc64;
	@FXML
	private Slider brightness, contrast, gamma, saturation, phaseShift, offset,
			tint, blur, bleed;
	@FXML
	private Label brightnessValue, contrastValue, gammaValue, saturationValue,
			phaseShiftValue, offsetValue, tintValue, blurValue, bleedValue;
	@FXML
	private ImageView datasetteOff, datasetteLoad, datasetteSave, c1541Off,
			c1541On, c1541Load, c1541IIOff, c1541IIOn, c1541IILoad;
	@FXML
	private Label tapeName, diskName, cartridgeName;

	private UIUtil util;

	private WritableImage vicImage;
	private Keyboard virtualKeyboard;
	private Timeline timer;

	private int marginLeft, marginRight, marginTop, marginBottom;
	private WritablePixelFormat<IntBuffer> pixelFormat;

	public Video(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		util.getPlayer().stateProperty().addListener((arg0, arg1, arg2) -> {
			if (arg2 == State.START) {
				Platform.runLater(() -> {
					setupVideoScreen();
					setVisibilityBasedOnChipType(util.getPlayer().getTune());
				});
			}
		});
		for (Slider slider : Arrays.asList(brightness, contrast, gamma,
				saturation, phaseShift, offset, tint, blur, bleed)) {
			slider.getStyleClass().add("knobStyle");
		}
		brightness.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float brightnessNewValue = round((newValue
									.floatValue() - 100.f) / 100.f);
							brightnessValue.textProperty().set(
									String.valueOf(brightnessNewValue));
							getC64().getPalVIC().getPalette()
									.setBrightness(brightnessNewValue);
							getC64().getNtscVIC().getPalette()
									.setBrightness(brightnessNewValue);
						});
		contrast.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float contrastNewValue = round(newValue
									.floatValue() / 100.f);
							contrastValue.textProperty().set(
									String.valueOf(contrastNewValue));
							getC64().getPalVIC().getPalette()
									.setContrast(contrastNewValue);
							getC64().getNtscVIC().getPalette()
									.setContrast(contrastNewValue);
						});
		gamma.valueProperty().addListener((observable, oldValue, newValue) -> {
			float gammaNewValue = round((newValue.floatValue() + 180) / 100.f);
			gammaValue.textProperty().set(String.valueOf(gammaNewValue));
			getC64().getPalVIC().getPalette().setGamma(gammaNewValue);
			getC64().getNtscVIC().getPalette().setGamma(gammaNewValue);
		});
		saturation.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float saturationNewValue = round(newValue
									.floatValue() / 100.f);
							saturationValue.textProperty().set(
									String.valueOf(saturationNewValue));
							getC64().getPalVIC().getPalette()
									.setSaturation(saturationNewValue);
							getC64().getNtscVIC().getPalette()
									.setSaturation(saturationNewValue);
						});
		phaseShift.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float phaseShiftNewValue = round((newValue
									.floatValue() - 45.f) / 100.f);
							phaseShiftValue.textProperty().set(
									String.valueOf(phaseShiftNewValue));
							getC64().getPalVIC().getPalette()
									.setPhaseShift(phaseShiftNewValue);
							getC64().getNtscVIC().getPalette()
									.setPhaseShift(phaseShiftNewValue);
						});
		offset.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float offsetNewValue = round((newValue.floatValue() + 10.f) / 100.f);
							offsetValue.textProperty().set(
									String.valueOf(offsetNewValue));
							getC64().getPalVIC().getPalette()
									.setOffset(offsetNewValue);
							getC64().getNtscVIC().getPalette()
									.setOffset(offsetNewValue);
						});
		tint.valueProperty().addListener((observable, oldValue, newValue) -> {
			float tintNewValue = round((newValue.floatValue() - 10.f) / 100.f);
			tintValue.textProperty().set(String.valueOf(tintNewValue));
			getC64().getPalVIC().getPalette().setTint(tintNewValue);
			getC64().getNtscVIC().getPalette().setTint(tintNewValue);
		});
		blur.valueProperty().addListener((observable, oldValue, newValue) -> {
			float blurNewValue = round((newValue.floatValue() + 50.f) / 100.f);
			blurValue.textProperty().set(String.valueOf(blurNewValue));
			getC64().getPalVIC().getPalette().setLuminanceC(blurNewValue);
			getC64().getNtscVIC().getPalette().setLuminanceC(blurNewValue);
		});
		bleed.valueProperty().addListener((observable, oldValue, newValue) -> {
			float bleedNewValue = round(newValue.floatValue() / 10.f);
			bleedValue.textProperty().set(String.valueOf(bleedNewValue));
			getC64().getPalVIC().getPalette().setDotCreep(bleedNewValue);
			getC64().getNtscVIC().getPalette().setDotCreep(bleedNewValue);
		});
		brightness
				.setValue(getC64().getPalVIC().getPalette().getBrightness() * 100 + 100.);
		contrast.setValue(getC64().getPalVIC().getPalette().getContrast() * 100);
		gamma.setValue(getC64().getPalVIC().getPalette().getGamma() * 100 - 180.);
		saturation
				.setValue(getC64().getPalVIC().getPalette().getSaturation() * 100);
		phaseShift
				.setValue(getC64().getPalVIC().getPalette().getPhaseShift() + 45.);
		offset.setValue(getC64().getPalVIC().getPalette().getOffset() * 100 - 10.);
		tint.setValue(getC64().getPalVIC().getPalette().getTint() * 100 + 10.);
		blur.setValue(getC64().getPalVIC().getPalette().getLuminanceC() * 100 - 50.);
		bleed.setValue(getC64().getPalVIC().getPalette().getDotCreep() * 10);

		setupVideoScreen();
		setVisibilityBasedOnChipType(util.getPlayer().getTune());

		setupKeyboard();

		updatePeripheralImages();
	}

	@Override
	public void doClose() {
		getC64().getPalVIC().setPixelConsumer(pixels -> {
		});
		getC64().getNtscVIC().setPixelConsumer(pixels -> {
		});
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
				util.getPlayer().insertTape(new TFile(file));
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
				util.getPlayer().insertDisk(new TFile(file));
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
				util.getPlayer().insertCartridge(CartridgeType.CRT,
						new TFile(file));
				util.getPlayer().play(SidTune.RESET);

			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void updatePalette() {
		getC64().getPalVIC().updatePalette();
		getC64().getNtscVIC().updatePalette();
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen() {
		double screenScale = ((SidPlay2Section) util.getConfig()
				.getSidplay2Section()).getVideoScaling();
		marginLeft = (int) (MONITOR_MARGIN_LEFT * screenScale);
		marginRight = (int) (MONITOR_MARGIN_RIGHT * screenScale);
		marginTop = (int) (MONITOR_MARGIN_TOP * screenScale);
		marginBottom = (int) (MONITOR_MARGIN_BOTTOM * screenScale);
		screen.setWidth(screenScale * getC64().getVIC().getBorderWidth());
		screen.setHeight(screenScale * getC64().getVIC().getBorderHeight());
		for (ImageView imageView : Arrays.asList(monitorBorder, breadbox, pc64)) {
			imageView.setScaleX(screen.getWidth()
					/ imageView.getImage().getWidth());
			imageView.setScaleY(screen.getHeight()
					/ imageView.getImage().getHeight());
		}
		vicImage = new WritableImage(getC64().getVIC().getBorderWidth(),
				getC64().getVIC().getBorderHeight());
		pixelFormat = PixelFormat.getIntArgbInstance();
		getC64().getPalVIC().setPixelConsumer(pixels -> {
		});
		getC64().getNtscVIC().setPixelConsumer(pixels -> {
		});
		getC64().getVIC().setPixelConsumer(this);
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

	private float round(float f) {
		return (int) (f * 100) / 100f;
	}

	private void updatePeripheralImages() {
		final Duration duration = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(
				duration,
				(evt) -> {
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
	 * Make C64 image visible, if the internal util.getPlayer() is used.
	 */
	private void setVisibilityBasedOnChipType(final SidTune sidTune) {
		if (sidTune != null && sidTune.getInfo().getPlayAddr() != 0) {
			if (ChipModel.getChipModel(util.getConfig().getEmulationSection(),
					sidTune, 0) == ChipModel.MOS6581) {
				// Old SID chip model? Show breadbox
				breadbox.setVisible(true);
				for (Node node : Arrays.asList(screen, monitorBorder, pc64)) {
					node.setVisible(false);
				}
			} else {
				// New SID chip model? Show PC 64
				pc64.setVisible(true);
				for (Node node : Arrays.asList(screen, monitorBorder, breadbox)) {
					node.setVisible(false);
				}
			}
		} else {
			// Show video screen and monitor
			for (Node node : Arrays.asList(screen, monitorBorder)) {
				node.setVisible(true);
			}
			for (Node node : Arrays.asList(breadbox, pc64)) {
				node.setVisible(false);
			}
		}
	}

	@Override
	public void accept(int[] pixels) {
		Platform.runLater(() -> {
			VIC vic = getC64().getVIC();
			vicImage.getPixelWriter().setPixels(0, 0, vic.getBorderWidth(),
					vic.getBorderHeight(), pixelFormat, pixels, 0,
					vic.getBorderWidth());
			screen.getGraphicsContext2D().drawImage(vicImage, 0, 0,
					vic.getBorderWidth(), vic.getBorderHeight(), marginLeft,
					marginTop, screen.getWidth() - (marginLeft + marginRight),
					screen.getHeight() - (marginTop + marginBottom));
		});
	}

	public WritableImage getVicImage() {
		return vicImage;
	}

	private C64 getC64() {
		return util.getPlayer().getC64();
	}

}
