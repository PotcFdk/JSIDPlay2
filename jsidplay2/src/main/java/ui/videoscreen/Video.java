package ui.videoscreen;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.c1530.Datasette.DatasetteStatus;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyStatus;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.mos656x.VIC;
import libsidplay.sidtune.SidTune;
import resid_builder.resid.ISIDDefs.ChipModel;
import ui.common.C64Tab;
import ui.entities.config.SidPlay2Section;
import ui.events.IInsertMedia;
import ui.events.IPlayerPlays;
import ui.events.UIEvent;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.virtualKeyboard.Keyboard;

public class Video extends C64Tab implements PropertyChangeListener {
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

	private WritableImage vicImage;
	private Keyboard virtualKeyboard;
	private Timeline timer;
	private boolean isVisible;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		for (Slider slider : Arrays.asList(brightness, contrast, gamma,
				saturation, phaseShift, offset, tint, blur, bleed)) {
			slider.getStyleClass().add("knobStyle");
		}
		brightness.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float brightnessNewValue = round((newValue.floatValue() - 100.f) / 100.f);
				brightnessValue.textProperty().set(
						String.valueOf(brightnessNewValue));
				getVIC().getPalette().setBrightness(brightnessNewValue);
			}
		});
		contrast.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float contrastNewValue = round(newValue.floatValue() / 100.f);
				contrastValue.textProperty().set(
						String.valueOf(contrastNewValue));
				getVIC().getPalette().setContrast(contrastNewValue);
			}
		});
		gamma.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float gammaNewValue = round((newValue.floatValue() + 180) / 100.f);
				gammaValue.textProperty().set(String.valueOf(gammaNewValue));
				getVIC().getPalette().setGamma(gammaNewValue);
			}
		});
		saturation.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float saturationNewValue = round(newValue.floatValue() / 100.f);
				saturationValue.textProperty().set(
						String.valueOf(saturationNewValue));
				getVIC().getPalette().setSaturation(saturationNewValue);
			}
		});
		phaseShift.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float phaseShiftNewValue = round((newValue.floatValue() - 45.f) / 100.f);
				phaseShiftValue.textProperty().set(
						String.valueOf(phaseShiftNewValue));
				getVIC().getPalette().setPhaseShift(phaseShiftNewValue);
			}
		});
		offset.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float offsetNewValue = round((newValue.floatValue() + 10.f) / 100.f);
				offsetValue.textProperty().set(String.valueOf(offsetNewValue));
				getVIC().getPalette().setOffset(offsetNewValue);
			}
		});
		tint.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float tintNewValue = round((newValue.floatValue() - 10.f) / 100.f);
				tintValue.textProperty().set(String.valueOf(tintNewValue));
				getVIC().getPalette().setTint(tintNewValue);
			}
		});
		blur.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float blurNewValue = round((newValue.floatValue() + 50.f) / 100.f);
				blurValue.textProperty().set(String.valueOf(blurNewValue));
				getVIC().getPalette().setLuminanceC(blurNewValue);
			}
		});
		bleed.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				float bleedNewValue = round(newValue.floatValue() / 10.f);
				bleedValue.textProperty().set(String.valueOf(bleedNewValue));
				getVIC().getPalette().setDotCreep(bleedNewValue);
			}
		});
		brightness.setValue(getVIC().getPalette().getBrightness() * 100 + 100.);
		contrast.setValue(getVIC().getPalette().getContrast() * 100);
		gamma.setValue(getVIC().getPalette().getGamma() * 100 - 180.);
		saturation.setValue(getVIC().getPalette().getSaturation() * 100);
		phaseShift.setValue(getVIC().getPalette().getPhaseShift() + 45.);
		offset.setValue(getVIC().getPalette().getOffset() * 100 - 10.);
		tint.setValue(getVIC().getPalette().getTint() * 100 + 10.);
		blur.setValue(getVIC().getPalette().getLuminanceC() * 100 - 50.);
		bleed.setValue(getVIC().getPalette().getDotCreep() * 10);

		setupVideoScreen();

		setupKeyboard();

		updatePeripheralImages();

		isVisible = true;
		getTabPane().getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Tab>() {
					@Override
					public void changed(
							ObservableValue<? extends Tab> observable,
							Tab oldValue, Tab newValue) {
						// performance optimizations!
						if (Video.this.equals(newValue)) {
							isVisible = true;
						} else {
							isVisible = false;
						}
					}
				});
	}

	@FXML
	private void showVirtualKeyboard() {
		try {
			if (virtualKeyboard == null) {
				virtualKeyboard = new Keyboard();
				virtualKeyboard.setPlayer(getPlayer());
				virtualKeyboard.setConfig(getConfig());
				virtualKeyboard.open();
			} else if (virtualKeyboard.isShowing()) {
				virtualKeyboard.hide();
			} else {
				virtualKeyboard.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TapeFileExtensions.DESCRIPTION,
						TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

				@Override
				public MediaType getMediaType() {
					return MediaType.TAPE;
				}

				@Override
				public File getSelectedMedia() {
					return file;
				}

				@Override
				public File getAutostartFile() {
					return null;
				}

				@Override
				public Object getComponent() {
					return Video.this;
				}
			});
		}
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(DiskFileExtensions.DESCRIPTION,
						DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

				@Override
				public MediaType getMediaType() {
					return MediaType.DISK;
				}

				@Override
				public File getSelectedMedia() {
					return file;
				}

				@Override
				public File getAutostartFile() {
					return null;
				}

				@Override
				public Object getComponent() {
					return Video.this;
				}
			});
		}
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(screen.getScene()
				.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

				@Override
				public MediaType getMediaType() {
					return MediaType.CART;
				}

				@Override
				public File getSelectedMedia() {
					return file;
				}

				@Override
				public File getAutostartFile() {
					return null;
				}

				@Override
				public Object getComponent() {
					return Video.this;
				}
			});
		}
	}

	@FXML
	private void updatePalette() {
		getVIC().updatePalette();
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen() {
		vicImage = new WritableImage(getVIC().getBorderWidth(), getVIC()
				.getBorderHeight());
		getVIC().addPropertyChangeListener(this);
	}

	/**
	 * Connect Keyboard with C64 keyboard.
	 */
	private void setupKeyboard() {
		monitor.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				KeyTableEntry keyTableEntry = getConfig().getKeyTabEntry(
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
			}
		});
	}

	private float round(float f) {
		return (int) (f * 100) / 100f;
	}

	private void updatePeripheralImages() {
		final Duration duration = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(duration,
				new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent evt) {
						DatasetteStatus datasetteStatus = getPlayer()
								.getDatasette().getStatus();
						tapeName.setText(getPlayer().getDatasette()
								.getTapeImage().getName());
						switch (datasetteStatus) {
						case OFF:
							datasetteOff.setVisible(true);
							for (ImageView imageView : Arrays.asList(
									datasetteLoad, datasetteSave)) {
								imageView.setVisible(false);
							}
							break;
						case LOAD:
							datasetteLoad.setVisible(true);
							for (ImageView imageView : Arrays.asList(
									datasetteOff, datasetteSave)) {
								imageView.setVisible(false);
							}
							break;
						case SAVE:
							datasetteSave.setVisible(true);
							for (ImageView imageView : Arrays.asList(
									datasetteOff, datasetteLoad)) {
								imageView.setVisible(false);
							}
							break;

						default:
							throw new RuntimeException(
									"Unexpected datasette status: "
											+ datasetteStatus);
						}
						final C1541 firstC1541 = getPlayer().getFloppies()[0];
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
							throw new RuntimeException(
									"Unexpected floppy status: " + floppyStatus);
						}
						cartridgeName.setText(getC64().getPla().getCartridge()
								.toString());
					}
				});
		timer = TimelineBuilder.create().cycleCount(Animation.INDEFINITE)
				.keyFrames(oneFrame).build();
		timer.playFromStart();
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

	/**
	 * Make C64 image visible, if the internal player is used.
	 */
	private void setupScreenBasedOnChipType(final SidTune sidTune) {
		if (sidTune != null && sidTune.getInfo().playAddr != 0) {
			if (getChipModel(sidTune) == ChipModel.MOS6581) {
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

	private ChipModel getChipModel(SidTune sidTune) {
		ChipModel userSidModel = getConfig().getEmulation().getUserSidModel();
		if (userSidModel != null) {
			return userSidModel;
		} else {
			if (sidTune != null) {
				switch (sidTune.getInfo().sid1Model) {
				case MOS6581:
					return ChipModel.MOS6581;
				case MOS8580:
					return ChipModel.MOS8580;
				default:
					return getConfig().getEmulation().getDefaultSidModel();
				}
			} else {
				return getConfig().getEmulation().getDefaultSidModel();
			}
		}
	}

	public WritableImage getVicImage() {
		return vicImage;
	}

	private VIC getVIC() {
		return getPlayer().getC64().getVIC();
	}

	private C64 getC64() {
		return getPlayer().getC64();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (isVisible && VIC.PROP_PIXELS.equals(evt.getPropertyName())) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					double screenScale = ((SidPlay2Section) getConfig()
							.getSidplay2()).getVideoScaling();
					screen.setWidth(screenScale * getVIC().getBorderWidth());
					screen.setHeight(screenScale * getVIC().getBorderHeight());
					vicImage.getPixelWriter().setPixels(0, 0,
							getVIC().getBorderWidth(),
							getVIC().getBorderHeight(),
							PixelFormat.getIntArgbInstance(),
							getVIC().getPixels(), 0, getVIC().getBorderWidth());
					screen.getGraphicsContext2D()
							.drawImage(
									vicImage,
									0,
									0,
									getVIC().getBorderWidth(),
									getVIC().getBorderHeight(),
									MONITOR_MARGIN_LEFT * screenScale,
									MONITOR_MARGIN_TOP * screenScale,
									screen.getWidth()
											- (MONITOR_MARGIN_LEFT + MONITOR_MARGIN_RIGHT)
											* screenScale,
									screen.getHeight()
											- (MONITOR_MARGIN_TOP + MONITOR_MARGIN_BOTTOM)
											* screenScale);
					for (ImageView imageView : Arrays.asList(monitorBorder,
							breadbox, pc64)) {
						imageView.setScaleX(screen.getWidth()
								/ imageView.getImage().getWidth());
						imageView.setScaleY(screen.getHeight()
								/ imageView.getImage().getHeight());
					}

				}
			});
		}
	}

	@Override
	public void notify(UIEvent evt) {
		if (evt.isOfType(IPlayerPlays.class)) {
			setupVideoScreen();
			setupScreenBasedOnChipType(getPlayer().getTune());
		}
	}

}
