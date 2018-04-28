package ui.toolbar;

import static ui.entities.config.OnlineSection.JSIDPLAY2_APP_URL;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer.Info;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.C64;
import libsidplay.common.CPUClock;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidutils.DesktopIntegration;
import netsiddev_builder.NetSIDDevConnection;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.MixerInfoToString;
import ui.common.TimeToStringConverter;
import ui.common.UIPart;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;
import ui.servlets.JSIDPlay2Server;

public class ToolBar extends C64VBox implements UIPart {

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box;
	@FXML
	private ComboBox<SamplingRate> samplingRateBox;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private ComboBox<Info> devicesBox;
	@FXML
	private ComboBox<Engine> engineBox;
	@FXML
	private CheckBox enableSldb, singleSong, proxyEnable;
	@FXML
	private TextField defaultTime, proxyHostname, proxyPort, hostname, port, appServerPort;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	ToggleGroup playSourceGroup, appServerGroup;
	@FXML
	protected Button volumeButton, mp3Browse;
	@FXML
	private Label hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label, appIpAddress, appHostname;

	private boolean duringInitialization;

	/**
	 * JSIPlay2 REST based web-services
	 */
	private JSIDPlay2Server jsidplay2Server = new JSIDPlay2Server();

	public ToolBar() {
		super();
	}

	public ToolBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	protected void initialize() {
		this.duringInitialization = true;

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		audioBox.setConverter(new EnumToString<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(Audio.SOUNDCARD, Audio.LIVE_WAV, Audio.LIVE_MP3,
				Audio.COMPARE_MP3));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setConverter(new MixerInfoToString());
		devicesBox.setItems(FXCollections.<Info>observableArrayList(JavaSound.getDevices()));
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToString<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToString<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		samplingRateBox.valueProperty().bindBidirectional(audioSection.samplingRateProperty());

		videoStandardBox.setConverter(new EnumToString<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());

		engineBox.setConverter(new EnumToString<Engine>(bundle));
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
		sidplay2Section.defaultPlayLengthProperty().addListener((obj, o, n) -> {
			final Tooltip tooltip = new Tooltip();
			defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			if (n.intValue() != -1) {
				util.getPlayer().getTimer().updateEnd();
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_OK);
			} else {
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
			}
		});

		proxyEnable.selectedProperty().bindBidirectional(sidplay2Section.enableProxyProperty());
		proxyHostname.textProperty().bindBidirectional(sidplay2Section.proxyHostnameProperty());
		Bindings.bindBidirectional(proxyPort.textProperty(), sidplay2Section.proxyPortProperty(),
				new IntegerStringConverter());

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		Bindings.bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(),
				new IntegerStringConverter());

		Bindings.bindBidirectional(appServerPort.textProperty(), emulationSection.appServerPortProperty(),
				new IntegerStringConverter());

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		Platform.runLater(() -> {
			appHostname.setText(util.getBundle().getString("APP_SERVER_HOSTNAME") + " " + getHostname());
			appIpAddress.setText(util.getBundle().getString("APP_SERVER_IP") + " " + getIpAddresses());
		});
		this.duringInitialization = false;
	}

	@FXML
	private void setAudio() {
		restart();
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
	private void startAppServer() {
		try {
			jsidplay2Server.start(util.getConfig());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void stopAppServer() {
		try {
			jsidplay2Server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void doEnableSldb() {
		final C64 c64 = util.getPlayer().getC64();
		final EventScheduler ctx = c64.getEventScheduler();
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
	private void downloadApp() {
		DesktopIntegration.browse(JSIDPLAY2_APP_URL);
	}

	@Override
	public void doClose() {
		stopAppServer();
	}

	private String getHostname() {
		try {
			Process proc = Runtime.getRuntime().exec("hostname");
			try (Scanner s = new Scanner(proc.getInputStream())) {
				s.useDelimiter("\\A");
				return s.hasNext() ? s.next().trim() : "";
			}
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

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

}
