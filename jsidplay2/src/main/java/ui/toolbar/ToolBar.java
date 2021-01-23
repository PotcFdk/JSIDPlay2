package ui.toolbar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javafx.beans.binding.Bindings.bindBidirectional;
import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.common.Connectors.HTTP;
import static server.restful.common.Connectors.HTTPS;
import static server.restful.common.Connectors.HTTP_HTTPS;
import static ui.common.BindingUtils.bindBidirectionalThreadSafe;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer.Info;

import builder.netsiddev.NetSIDDevConnection;
import builder.sidblaster.SidBlasterBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.Ultimate64Mode;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.DesktopIntegration;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.Connectors;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.ini.IniConfig;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.converter.EnumToStringConverter;
import ui.common.converter.MixerInfoToStringConverter;
import ui.common.converter.PositiveNumberToStringConverter;
import ui.common.converter.TimeToStringConverter;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.DeviceMapping;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class ToolBar extends C64VBox implements UIPart {

	private StateChangeListener propertyChangeListener;

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			Platform.runLater(() -> {
				if (event.getNewValue() == State.OPEN) {
					if (testPlayer != null) {
						testPlayer.stopC64();
					}
				} else if (event.getNewValue() == State.START) {
					saveRecordingLabel.setDisable(true);
					util.getPlayer()
							.configureMixer(mixer -> Platform.runLater(() -> setActiveSidBlasterDevices(mixer)));
				} else if ((event.getNewValue() == State.END || event.getNewValue() == State.QUIT)
						&& util.getPlayer().getAudioDriver().isRecording()) {
					saveRecordingLabel.setDisable(false);
				}
			});
		}

	}

	private static final String SIDBLASTER_TEST_SID = "/builder/sidblaster/sidblaster_test.sid";

	@FXML
	private ToggleGroup testButtonGroup;
	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box, audioBufferSize, sidBlasterWriteBufferSize;
	@FXML
	private ComboBox<SamplingRate> samplingRateBox;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private Label saveRecordingLabel;
	@FXML
	private ComboBox<Info> devicesBox;
	@FXML
	private ComboBox<Engine> engineBox;
	@FXML
	private ComboBox<Connectors> appServerConnectorsBox;
	@FXML
	private ComboBox<Ultimate64Mode> ultimate64Box;
	@FXML
	private CheckBox enableSldb, singleSong, proxyEnable;
	@FXML
	private TextField bufferSize, defaultPlayLength, proxyHostname, proxyPort, hostname, port, ultimate64Hostname,
			ultimate64Port, ultimate64SyncDelay, appServerPort, appServerSecurePort, appServerKeyStorePassword,
			appServerKeyAlias, appServerKeyPassword, ultimate64StreamingTarget, ultimate64StreamingAudioPort,
			ultimate64StreamingVideoPort;
	@FXML
	private ScrollPane sidBlasterScrollPane;
	@FXML
	private VBox sidBlasterDeviceParent;
	@FXML
	protected RadioButton playMP3, playEmulation, startAppServer, stopAppServer;
	@FXML
	protected ToggleGroup playSourceGroup, appServerGroup;
	@FXML
	protected Button volumeButton, mp3Browse, keystoreBrowse;
	@FXML
	private Label hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label, appIpAddress, appHostname,
			appServerPortLbl, appServerSecurePortLbl, appServerKeyStorePasswordLbl, appServerKeyAliasLbl,
			appServerKeyPasswordLbl;
	@FXML
	private Hyperlink appServerUsage, onlinePlayer, downloadApp;

	@FXML
	protected ProgressBar progress;

	private ObservableList<Ultimate64Mode> ultimate64Modes;

	private JSIDPlay2Server jsidplay2Server;

	private Player testPlayer;

	private boolean duringInitialization;

	public ToolBar() {
		super();
	}

	public ToolBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		this.duringInitialization = true;

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		jsidplay2Server = JSIDPlay2Server.getInstance(config);

		audioBox.setConverter(new EnumToStringConverter<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(Audio.SOUNDCARD, Audio.LIVE_WAV, Audio.LIVE_MP3,
				Audio.LIVE_AVI, Audio.LIVE_MP4, Audio.LIVE_SID_REG, Audio.COMPARE_MP3));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setConverter(new MixerInfoToStringConverter());
		devicesBox.setItems(FXCollections.<Info>observableArrayList(JavaSound.getDevices()));
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToStringConverter<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToStringConverter<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		bindBidirectionalThreadSafe(samplingRateBox.valueProperty(), audioSection.samplingRateProperty());

		videoStandardBox.setConverter(new EnumToStringConverter<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());
		audioBufferSize.valueProperty().bindBidirectional(audioSection.audioBufferSizeProperty());

		engineBox.setConverter(new EnumToStringConverter<Engine>(bundle));
		engineBox.setItems(FXCollections.<Engine>observableArrayList(Engine.EMULATION, Engine.NETSID, Engine.HARDSID,
				Engine.SIDBLASTER));
		engineBox.valueProperty().addListener((obj, o, n) -> {
			hardsid6581Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid6581Label.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Label.setDisable(!Engine.HARDSID.equals(n));
			hostnameLabel.setDisable(!Engine.NETSID.equals(n));
			hostname.setDisable(!Engine.NETSID.equals(n));
			portLabel.setDisable(!Engine.NETSID.equals(n));
			port.setDisable(!Engine.NETSID.equals(n));
		});
		engineBox.valueProperty().bindBidirectional(emulationSection.engineProperty());

		bindBidirectional(defaultPlayLength.textProperty(), sidplay2Section.defaultPlayLengthProperty(),
				new TimeToStringConverter());
		sidplay2Section.defaultPlayLengthProperty()
				.addListener((obj, o, n) -> util.checkTextField(defaultPlayLength, () -> n.intValue() != -1,
						() -> util.getPlayer().getTimer().updateEnd(), "DEFAULT_LENGTH_TIP", "DEFAULT_LENGTH_FORMAT"));
		bindBidirectional(bufferSize.textProperty(), audioSection.bufferSizeProperty(),
				new PositiveNumberToStringConverter<>(2048));
		audioSection.bufferSizeProperty()
				.addListener((obj, o, n) -> util.checkTextField(bufferSize, () -> n.intValue() >= 2048, () -> {
				}, "BUFFER_SIZE_TIP", "BUFFER_SIZE_FORMAT"));

		sidBlasterWriteBufferSize.valueProperty()
				.bindBidirectional(emulationSection.sidBlasterWriteBufferSizeProperty());
		emulationSection.getSidBlasterDeviceList().stream().forEach(this::addSidBlasterDeviceMapping);

		proxyEnable.selectedProperty().bindBidirectional(sidplay2Section.enableProxyProperty());
		proxyHostname.textProperty().bindBidirectional(sidplay2Section.proxyHostnameProperty());
		bindBidirectional(proxyPort.textProperty(), sidplay2Section.proxyPortProperty(), new IntegerStringConverter());

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(), new IntegerStringConverter());

		ultimate64Modes = FXCollections.<Ultimate64Mode>observableArrayList(Ultimate64Mode.values());
		ultimate64Box.setConverter(new EnumToStringConverter<Ultimate64Mode>(bundle));
		ultimate64Box.valueProperty().bindBidirectional(emulationSection.ultimate64ModeProperty());
		ultimate64Box.setItems(ultimate64Modes);

		ultimate64Hostname.textProperty().bindBidirectional(emulationSection.ultimate64HostProperty());
		bindBidirectional(ultimate64Port.textProperty(), emulationSection.ultimate64PortProperty(),
				new IntegerStringConverter());
		bindBidirectional(ultimate64SyncDelay.textProperty(), emulationSection.ultimate64SyncDelayProperty(),
				new IntegerStringConverter());

		bindBidirectional(appServerPort.textProperty(), emulationSection.appServerPortProperty(),
				new IntegerStringConverter());
		bindBidirectional(appServerSecurePort.textProperty(), emulationSection.appServerSecurePortProperty(),
				new IntegerStringConverter());

		appServerConnectorsBox.setConverter(new EnumToStringConverter<Connectors>(bundle));
		appServerConnectorsBox.valueProperty().addListener((obj, o, n) -> {
			switch (n) {
			case HTTP_HTTPS:
				for (Node node : Arrays.asList(appServerPortLbl, appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerPort, appServerSecurePort,
						keystoreBrowse, appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;
			case HTTPS:
				for (Node node : Arrays.asList(appServerPortLbl, appServerPort)) {
					node.setVisible(false);
					node.setManaged(false);
				}
				for (Node node : Arrays.asList(appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerSecurePort, keystoreBrowse,
						appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;

			case HTTP:
			default:
				for (Node node : Arrays.asList(appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerSecurePort, keystoreBrowse,
						appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(false);
					node.setManaged(false);
				}
				for (Node node : Arrays.asList(appServerPortLbl, appServerPort)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;
			}
		});
		appServerConnectorsBox.setItems(FXCollections.<Connectors>observableArrayList(HTTP, HTTP_HTTPS, HTTPS));
		appServerConnectorsBox.valueProperty().bindBidirectional(emulationSection.appServerConnectorsProperty());
		appServerKeyStorePassword.textProperty()
				.bindBidirectional(emulationSection.appServerKeystorePasswordProperty());
		appServerKeyPassword.textProperty().bindBidirectional(emulationSection.appServerKeyPasswordProperty());
		appServerKeyAlias.textProperty().bindBidirectional(emulationSection.appServerKeyAliasProperty());

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		appHostname.setText(util.getBundle().getString("APP_SERVER_HOSTNAME") + " " + getHostname());
		appIpAddress.setText(util.getBundle().getString("APP_SERVER_IP") + " " + getIpAddresses());
		startAppServer.selectedProperty().addListener((observable, oldValue, newValue) -> {
			appServerUsage.setDisable(!newValue);
			onlinePlayer.setDisable(!newValue);
			downloadApp.setDisable(!newValue);
		});

		ultimate64StreamingTarget.textProperty()
				.bindBidirectional(emulationSection.ultimate64StreamingTargetProperty());
		ultimate64StreamingAudioPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingAudioPortProperty(), new IntegerStringConverter());
		ultimate64StreamingVideoPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingVideoPortProperty(), new IntegerStringConverter());

		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		this.duringInitialization = false;
	}

	@FXML
	private void setAudio() {
		restart();
	}

	@FXML
	private void saveRecording() {
		try {
			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
			final DirectoryChooser fileDialog = new DirectoryChooser();
			fileDialog.setTitle(util.getBundle().getString("SAVE_RECORDING"));
			fileDialog.setInitialDirectory(sidplay2Section.getLastDirectory());
			File directory = fileDialog.showDialog(getScene().getWindow());
			if (directory != null) {
				sidplay2Section.setLastDirectory(directory);

				Path sourcePath = Paths.get(util.getPlayer().getRecordingFilename());
				Path targetPath = new File(directory, sourcePath.toFile().getName()).toPath();
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				sourcePath.toFile().deleteOnExit();
				System.out.println("Recording Saved to: " + targetPath);
			}
		} catch (IOException e) {
			openErrorDialog(e.getMessage());
		}
	}

	@FXML
	public void setDevice() {
		int deviceIndex = devicesBox.getSelectionModel().getSelectedIndex();
		util.getConfig().getAudioSection().setDevice(deviceIndex);
		restart();
	}

	@FXML
	private void setSampling() {
		restart();
	}

	@FXML
	private void setSamplingRate() {
		restart();
	}

	@FXML
	private void setEngine() {
		restart();
	}

	@FXML
	private void addSidBlaster() {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();

		DeviceMapping deviceMapping = new DeviceMapping("", ChipModel.MOS8580, true);
		emulationSection.getSidBlasterDeviceList().add(deviceMapping);
		addSidBlasterDeviceMapping(deviceMapping);
	}

	@FXML
	private void autodetect() {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();
		try {
			if (SidBlasterBuilder.getSerialNumbers() == null) {
				triggerFetchSerialNumbers();
			}
			// overwrite device list
			emulationSection.getSidBlasterDeviceList().clear();
			sidBlasterDeviceParent.getChildren().clear();
			for (String serialNumber : SidBlasterBuilder.getSerialNumbers()) {
				DeviceMapping deviceMapping = new DeviceMapping(serialNumber, ChipModel.MOS8580, true);
				emulationSection.getSidBlasterDeviceList().add(deviceMapping);
				addSidBlasterDeviceMapping(deviceMapping);
			}
		} catch (Error error) {
			openErrorDialog(error.getMessage());
		}
	}

	@FXML
	private void setSid6581() {
		restart();
	}

	@FXML
	private void setSid8580() {
		restart();
	}

	@FXML
	private void setHostname() {
		NetSIDDevConnection.getInstance().invalidate();
		restart();
	}

	@FXML
	private void setPort() {
		NetSIDDevConnection.getInstance().invalidate();
		restart();
	}

	@FXML
	private void setUltimate64() {
		restart();
	}

	@FXML
	private void setUltimate64Hostname() {
		restart();
	}

	@FXML
	private void setUltimate64Port() {
		restart();
	}

	@FXML
	private void setUltimate64SyncDelay() {
		restart();
	}

	@FXML
	private void setAudioBufferSize() {
		restart();
	}

	@FXML
	private void setUltimate64StreamingTarget() {
		restart();
	}

	@FXML
	private void setUtimate64StreamingAudioPort() {
		restart();
	}

	@FXML
	private void setUtimate64StreamingVideoPort() {
		restart();
	}

	@FXML
	private void doKeystoreBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Keystore file (*.ks)", "*.ks");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getEmulationSection().setAppServerKeystoreFile(file);
		}
	}

	@FXML
	private void startAppServer() {
		try {
			jsidplay2Server.start();
		} catch (Exception e) {
			openErrorDialog(e.getMessage());
		}
	}

	@FXML
	private void stopAppServer() {
		try {
			jsidplay2Server.stop();
		} catch (Exception e) {
			openErrorDialog(e.getMessage());
		}
	}

	@FXML
	private void gotoRestApiUsage() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		Connectors appServerConnectors = emulationSection.getAppServerConnectors();
		int port = appServerConnectors.getPreferredProtocol().equals("http") ? emulationSection.getAppServerPort()
				: emulationSection.getAppServerSecurePort();
		DesktopIntegration.browse(appServerConnectors.getPreferredProtocol() + "://127.0.0.1:" + port);
	}

	@FXML
	private void doEnableSldb() {
		final EventScheduler ctx = util.getPlayer().getC64().getEventScheduler();
		ctx.scheduleThreadSafe(new Event("Update Play Timer!") {
			@Override
			public void event() {
				util.getPlayer().getTimer().updateEnd();
			}
		});
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MP3 file (*.mp3)", "*.mp3");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getAudioSection().setMp3(file);
			if (util.getPlayer().getTune() instanceof MP3Tune) {
				util.getPlayer().setTune(SidTune.RESET);
			}
			restart();
		}
	}

	@FXML
	private void showVolume() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			SidPlay2Section section = util.getConfig().getSidplay2Section();
			int x = section.getFrameX() + section.getFrameWidth() / 2;
			try {
				Runtime.getRuntime().exec("sndvol -f " + x);
			} catch (IOException e) {
				try {
					Runtime.getRuntime().exec("sndvol32");
				} catch (IOException e1) {
					String toolTip = "For Windows: sndvol or sndvol32 not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("nux") >= 0) {
			try {
				Runtime.getRuntime().exec("pavucontrol");
			} catch (IOException e2) {
				try {
					Runtime.getRuntime().exec("kmix");
				} catch (IOException e3) {
					String toolTip = "For Linux: pavucontrol(PulseAudio) or kmix(ALSA) not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("mac") >= 0) {
			String toolTip = "For OSX: N.Y.I!";
			volumeButton.setDisable(true);
			volumeButton.setTooltip(new Tooltip(toolTip));
			System.err.println(toolTip);
		}
	}

	@FXML
	private void setVideoStandard() {
		restart();
	}

	@FXML
	private void onlinePlayer() {
		DesktopIntegration.browse(util.getConfig().getOnlineSection().getOnlinePlayerUrl());
	}

	@FXML
	private void downloadApp() {
		DesktopIntegration.browse(util.getConfig().getOnlineSection().getAppUrl());
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
		stopAppServer();
	}

	private void addSidBlasterDeviceMapping(DeviceMapping deviceMapping) {
		SidBlasterDeviceMapping sidBlasterDeviceMapping = new SidBlasterDeviceMapping(util.getWindow(),
				util.getPlayer());
		sidBlasterDeviceMapping.init(deviceMapping, this::testSidBlasterDevice, this::removeSidBlasterDeviceMapping,
				testButtonGroup);
		sidBlasterDeviceParent.getChildren().add(sidBlasterDeviceMapping);

		// scroll to bottom automatically
		Platform.runLater(() -> {
			sidBlasterDeviceParent.requestLayout();
			Platform.runLater(() -> sidBlasterScrollPane.setVvalue(1.0));
		});
	}

	private void removeSidBlasterDeviceMapping(DeviceMapping deviceMapping) {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();

		sidBlasterDeviceParent.getChildren().remove(emulationSection.getSidBlasterDeviceList().indexOf(deviceMapping));
		emulationSection.getSidBlasterDeviceList().remove(deviceMapping);
	}

	private void testSidBlasterDevice(DeviceMapping deviceMapping, Boolean isSelected) {
		try {
			if (testPlayer == null) {
				testPlayer = new Player(new IniConfig(false, null));
				testPlayer.getConfig().getEmulationSection().setEngine(Engine.SIDBLASTER);
			} else {
				testPlayer.stopC64();
			}
			if (SidBlasterBuilder.getSerialNumbers() == null) {
				triggerFetchSerialNumbers();
			}
			if (isSelected) {
				util.getPlayer().stopC64(true);

				setActiveSidBlasterDevice(serial -> Objects.equals(deviceMapping.getSerialNum(), serial));

				testPlayer.getConfig().getEmulationSection().setSidBlasterSerialNumber(deviceMapping.getSerialNum());
				testPlayer.play(
						SidTune.load("sidblaster_test.sid", ToolBar.class.getResourceAsStream(SIDBLASTER_TEST_SID)));
			}
		} catch (IOException | SidTuneError e) {
			openErrorDialog(e.getMessage());
		}
	}

	private void triggerFetchSerialNumbers() {
		new SidBlasterBuilder(null, util.getConfig(), null);
	}

	private void setActiveSidBlasterDevices(Mixer mixer) {
		List<String> serialNumbers = new ArrayList<>();
		if (mixer instanceof SidBlasterBuilder) {
			SidBlasterBuilder sidBlasterBuilder = (SidBlasterBuilder) mixer;
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				serialNumbers.add(sidBlasterBuilder.getDeviceName(sidNum));
			}
		}
		setActiveSidBlasterDevice(serialNoOfDevice -> serialNumbers.contains(serialNoOfDevice));
	}

	private void setActiveSidBlasterDevice(Predicate<String> serialNoSelector) {
		for (Node node : sidBlasterDeviceParent.getChildren()) {
			SidBlasterDeviceMapping sidBlasterDeviceMapping = (SidBlasterDeviceMapping) node;
			String serialNoOfDevice = sidBlasterDeviceMapping.getSerialNo();

			node.getStyleClass().remove("active");
			if (serialNoSelector.test(serialNoOfDevice)) {
				node.getStyleClass().add("active");
			}
		}
	}

	private String getHostname() {
		try {
			Process proc = Runtime.getRuntime().exec("hostname");
			return ZipFileUtils.convertStreamToString(proc.getInputStream(), UTF_8.name());
		} catch (IOException e) {
			return "?hostname?";
		}
	}

	private String getIpAddresses() {
		try {
			return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
					.flatMap(iface -> Collections.list(iface.getInetAddresses()).stream())
					.filter(address -> !address.isLoopbackAddress() && address.isSiteLocalAddress())
					.map(address -> address.getHostAddress()).collect(Collectors.joining("\n"));
		} catch (SocketException ex) {
			return "?ip?";
		}
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

}
