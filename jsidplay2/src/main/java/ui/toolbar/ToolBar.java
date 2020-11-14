package ui.toolbar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.common.Connectors.HTTP;
import static server.restful.common.Connectors.HTTPS;
import static server.restful.common.Connectors.HTTP_HTTPS;
import static ui.entities.config.OnlineSection.JSIDPLAY2_APP_URL;
import static ui.entities.config.OnlineSection.ONLINE_PLAYER_URL;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer.Info;

import builder.netsiddev.NetSIDDevConnection;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.Ultimate64Mode;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidutils.DesktopIntegration;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.Connectors;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.EnumToStringConverter;
import ui.common.MixerInfoToStringConverter;
import ui.common.PositiveNumberToStringConverter;
import ui.common.ThreadSafeBindings;
import ui.common.TimeToStringConverter;
import ui.common.UIPart;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class ToolBar extends C64VBox implements UIPart {

	private StateChangeListener propertyChangeListener;

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			Platform.runLater(() -> {
				if (event.getNewValue() == State.START) {
					saveRecordingLabel.setDisable(true);
				}
				if ((event.getNewValue() == State.END || event.getNewValue() == State.QUIT)
						&& util.getPlayer().getAudioDriver().isRecording()) {
					saveRecordingLabel.setDisable(false);
				}
			});
		}

	}

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box, audioBufferSize;
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
	private TextField bufferSize, defaultTime, proxyHostname, proxyPort, hostname, port, ultimate64Hostname,
			ultimate64Port, ultimate64SyncDelay, appServerPort, appServerSecurePort, appServerKeyStorePassword,
			appServerKeyAlias, appServerKeyPassword, ultimate64StreamingTarget, ultimate64StreamingAudioPort,
			ultimate64StreamingVideoPort, sidBlasterMapping0, sidBlasterMapping1, sidBlasterMapping2;
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

	private boolean duringInitialization;

	/**
	 * JSIPlay2 REST based web-services
	 */
	private JSIDPlay2Server jsidplay2Server;

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
		ThreadSafeBindings.bindBidirectional(samplingRateBox.valueProperty(), audioSection.samplingRateProperty());

		videoStandardBox.setConverter(new EnumToStringConverter<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());
		audioBufferSize.valueProperty().bindBidirectional(audioSection.audioBufferSizeProperty());

		engineBox.setConverter(new EnumToStringConverter<Engine>(bundle));
		engineBox.setItems(FXCollections.<Engine>observableArrayList(Engine.values()));
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

		Bindings.bindBidirectional(defaultTime.textProperty(), sidplay2Section.defaultPlayLengthProperty(),
				new TimeToStringConverter());
		sidplay2Section.defaultPlayLengthProperty().addListener((obj, o, n) -> checkTextField(defaultTime,
				n.intValue() != -1, "DEFAULT_LENGTH_TIP", "DEFAULT_LENGTH_FORMAT"));
		sidplay2Section.defaultPlayLengthProperty().addListener((obj, o, n) -> {
			if (n.intValue() != -1) {
				util.getPlayer().getTimer().updateEnd();
			}
		});
		Bindings.bindBidirectional(bufferSize.textProperty(), audioSection.bufferSizeProperty(),
				new PositiveNumberToStringConverter<>(2048));
		audioSection.bufferSizeProperty().addListener((obj, o, n) -> checkTextField(bufferSize, n.intValue() >= 2048,
				"BUFFER_SIZE_TIP", "BUFFER_SIZE_FORMAT"));

		sidBlasterMapping0.textProperty().bindBidirectional(emulationSection.sidBlaster0ModelProperty());
		emulationSection.sidBlaster0ModelProperty().addListener((obj, o, n) -> checkTextField(sidBlasterMapping0,
				checkSidBlasterMapping(n), "SIDBLASTER_MAPPING_TIP", "SIDBLASTER_MAPPING_FORMAT"));
		sidBlasterMapping1.textProperty().bindBidirectional(emulationSection.sidBlaster1ModelProperty());
		emulationSection.sidBlaster1ModelProperty().addListener((obj, o, n) -> checkTextField(sidBlasterMapping1,
				checkSidBlasterMapping(n), "SIDBLASTER_MAPPING_TIP", "SIDBLASTER_MAPPING_FORMAT"));
		sidBlasterMapping2.textProperty().bindBidirectional(emulationSection.sidBlaster2ModelProperty());
		emulationSection.sidBlaster2ModelProperty().addListener((obj, o, n) -> checkTextField(sidBlasterMapping2,
				checkSidBlasterMapping(n), "SIDBLASTER_MAPPING_TIP", "SIDBLASTER_MAPPING_FORMAT"));

		proxyEnable.selectedProperty().bindBidirectional(sidplay2Section.enableProxyProperty());
		proxyHostname.textProperty().bindBidirectional(sidplay2Section.proxyHostnameProperty());
		Bindings.bindBidirectional(proxyPort.textProperty(), sidplay2Section.proxyPortProperty(),
				new IntegerStringConverter());

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		Bindings.bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(),
				new IntegerStringConverter());

		ultimate64Modes = FXCollections.<Ultimate64Mode>observableArrayList(Ultimate64Mode.values());
		ultimate64Box.setConverter(new EnumToStringConverter<Ultimate64Mode>(bundle));
		ultimate64Box.valueProperty().bindBidirectional(emulationSection.ultimate64ModeProperty());
		ultimate64Box.setItems(ultimate64Modes);

		ultimate64Hostname.textProperty().bindBidirectional(emulationSection.ultimate64HostProperty());
		Bindings.bindBidirectional(ultimate64Port.textProperty(), emulationSection.ultimate64PortProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(ultimate64SyncDelay.textProperty(), emulationSection.ultimate64SyncDelayProperty(),
				new IntegerStringConverter());

		Bindings.bindBidirectional(appServerPort.textProperty(), emulationSection.appServerPortProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(appServerSecurePort.textProperty(), emulationSection.appServerSecurePortProperty(),
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

	private void checkTextField(TextField textField, boolean valueCorrect, String tipKey, String formatKey) {
		final Tooltip tooltip = new Tooltip();
		textField.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		if (valueCorrect) {
			tooltip.setText(util.getBundle().getString(tipKey));
			textField.setTooltip(tooltip);
			textField.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(util.getBundle().getString(formatKey));
			textField.setTooltip(tooltip);
			textField.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	private boolean checkSidBlasterMapping(String mapping) {
		if (mapping.isEmpty()) {
			return true;
		}
		String[] keyValue = mapping.split("=");
		if (keyValue.length != 2 || keyValue[0].length() != 8) {
			return false;
		}
		return Arrays.asList(ChipModel.values()).stream().map(ChipModel::toString)
				.filter(model -> Objects.equals(model, keyValue[1])).findFirst().isPresent();
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
			fileDialog.setInitialDirectory(sidplay2Section.getLastDirectoryFolder());
			File directory = fileDialog.showDialog(getScene().getWindow());
			if (directory != null) {
				sidplay2Section.setLastDirectory(directory.getAbsolutePath());

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
	private void setSidBlasterMapping0() {
		restart();
	}

	@FXML
	private void setSidBlasterMapping1() {
		restart();
	}

	@FXML
	private void setSidBlasterMapping2() {
		restart();
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
			util.getConfig().getEmulationSection().setAppServerKeystoreFile(file.getAbsolutePath());
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
			util.getConfig().getAudioSection().setMp3File(file.getAbsolutePath());
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
		DesktopIntegration.browse(ONLINE_PLAYER_URL);
	}

	@FXML
	private void downloadApp() {
		DesktopIntegration.browse(JSIDPLAY2_APP_URL);
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
		stopAppServer();
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
		Alert alert = new Alert(AlertType.ERROR, "");
		alert.initStyle(StageStyle.UTILITY);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.getDialogPane().setHeaderText(msg);
		alert.showAndWait();
	}

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

}
