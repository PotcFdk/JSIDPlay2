package ui.soundsettings;

import java.io.File;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.player.DriverSettings;
import libsidplay.player.Emulation;
import resid_builder.resid.SamplingMethod;
import sidplay.audio.CmpMP3File;
import sidplay.audio.Output;
import sidplay.ini.intf.IAudioSection;
import ui.common.C64Window;

public class SoundSettings extends C64Window {

	@FXML
	protected TextField mp3, proxyHost, proxyPort;
	@FXML
	private CheckBox proxyEnable;
	@FXML
	protected ComboBox<String> soundDevice;
	@FXML
	private ComboBox<Integer> hardsid6581, hardsid8580, samplingRate;
	@FXML
	private ComboBox<SamplingMethod> samplingMethod;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	private Button mp3Browse;

	private ObservableList<resid_builder.resid.SamplingMethod> samplingMethods;

	private ObservableList<String> soundDevices;

	private boolean duringInitialization;

	public SoundSettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		duringInitialization = true;
		soundDevices = FXCollections.<String> observableArrayList();
		soundDevice.setItems(soundDevices);
		soundDevices.addAll(util.getBundle().getString("SOUNDCARD"), util
				.getBundle().getString("HARDSID4U"), util.getBundle()
				.getString("WAV_RECORDER"),
				util.getBundle().getString("MP3_RECORDER"), util.getBundle()
						.getString("COMPARE_TO_MP3"));
		Output output = util.getPlayer().getDriverSettings().getOutput();
		Emulation sid = util.getPlayer().getDriverSettings().getEmulation();
		if (output == Output.OUT_SOUNDCARD && sid == Emulation.EMU_RESID) {
			soundDevice.getSelectionModel().select(0);
		} else if (output == Output.OUT_NULL && sid == Emulation.EMU_HARDSID) {
			soundDevice.getSelectionModel().select(1);
		} else if (output == Output.OUT_LIVE_WAV && sid == Emulation.EMU_RESID) {
			soundDevice.getSelectionModel().select(2);
		} else if (output == Output.OUT_LIVE_MP3 && sid == Emulation.EMU_RESID) {
			soundDevice.getSelectionModel().select(3);
		} else if (output == Output.OUT_COMPARE && sid == Emulation.EMU_RESID) {
			soundDevice.getSelectionModel().select(4);
		} else {
			soundDevice.getSelectionModel().select(0);
		}
		hardsid6581.getSelectionModel().select(
				Integer.valueOf(util.getConfig().getEmulation()
						.getHardsid6581()));
		hardsid8580.getSelectionModel().select(
				Integer.valueOf(util.getConfig().getEmulation()
						.getHardsid8580()));
		samplingRate.getSelectionModel().select(
				Integer.valueOf(util.getConfig().getAudio().getFrequency()));
		samplingMethods = FXCollections
				.<resid_builder.resid.SamplingMethod> observableArrayList();
		samplingMethod.setItems(samplingMethods);
		samplingMethods
				.addAll(SamplingMethod.DECIMATE, SamplingMethod.RESAMPLE);
		samplingMethod.getSelectionModel().select(
				util.getConfig().getAudio().getSampling());
		mp3.setText(util.getConfig().getAudio().getMp3File());
		playMP3.setSelected(util.getConfig().getAudio().isPlayOriginal());
		playEmulation
				.setSelected(!util.getConfig().getAudio().isPlayOriginal());

		proxyEnable.setSelected(util.getConfig().getSidplay2().isEnableProxy());
		proxyHost.setText(util.getConfig().getSidplay2().getProxyHostname());
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setText(String.valueOf(util.getConfig().getSidplay2()
				.getProxyPort()));
		proxyPort.setEditable(proxyEnable.isSelected());

		duringInitialization = false;
	}

	@FXML
	private void setSoundDevice() {
		stop();
		switch (soundDevice.getSelectionModel().getSelectedIndex()) {
		case 0:
			util.getPlayer().setDriverSettings(
					new DriverSettings(Output.OUT_SOUNDCARD,
							Emulation.EMU_RESID));
			break;

		case 1:
			util.getPlayer().setDriverSettings(
					new DriverSettings(Output.OUT_NULL, Emulation.EMU_HARDSID));
			break;

		case 2:
			util.getPlayer()
					.setDriverSettings(
							new DriverSettings(Output.OUT_LIVE_WAV,
									Emulation.EMU_RESID));
			break;

		case 3:
			util.getPlayer()
					.setDriverSettings(
							new DriverSettings(Output.OUT_LIVE_MP3,
									Emulation.EMU_RESID));
			break;
		case 4:
			util.getPlayer()
					.setDriverSettings(
							new DriverSettings(Output.OUT_COMPARE,
									Emulation.EMU_RESID));
			break;

		}
		restart();
	}

	@FXML
	private void setSid6581() {
		util.getConfig()
				.getEmulation()
				.setHardsid6581(
						hardsid6581.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSid8580() {
		util.getConfig()
				.getEmulation()
				.setHardsid8580(
						hardsid8580.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSamplingRate() {
		util.getConfig()
				.getAudio()
				.setFrequency(
						samplingRate.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSamplingMethod() {
		util.getConfig()
				.getAudio()
				.setSampling(
						samplingMethod.getSelectionModel().getSelectedItem());
		IAudioSection audio = util.getConfig().getAudio();
		CPUClock systemFrequency = CPUClock.getCPUClock(util.getConfig(), util
				.getPlayer().getTune());
		util.getPlayer().setSampling(systemFrequency, audio.getFrequency(),
				samplingMethod.getSelectionModel().getSelectedItem());
	}

	@FXML
	private void playEmulatedSound() {
		setPlayOriginal(false);
	}

	@FXML
	private void playRecordedSound() {
		setPlayOriginal(true);
	}

	@FXML
	private void setRecording() {
		util.getConfig().getAudio().setMp3File(mp3.getText());
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				"MP3 file (*.mp3)", "*.mp3");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(mp3.getScene().getWindow());
		if (file != null) {
			mp3.setText(file.getAbsolutePath());
			util.getConfig().getAudio().setMp3File(mp3.getText());
			restart();
		}
	}

	@FXML
	private void setEnableProxy() {
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setEditable(proxyEnable.isSelected());
		util.getConfig().getSidplay2().setEnableProxy(proxyEnable.isSelected());
	}

	@FXML
	private void setProxyHost() {
		util.getConfig().getSidplay2().setProxyHostname(proxyHost.getText());
	}

	@FXML
	private void setProxyPort() {
		util.getConfig()
				.getSidplay2()
				.setProxyPort(
						proxyPort.getText().length() > 0 ? Integer
								.valueOf(proxyPort.getText()) : 80);
	}

	protected void stop() {
		// stop for safe replacement of the audio driver
		if (!duringInitialization) {
			util.getPlayer().stopC64();
		}
	}

	protected void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getPlayer().playTune(util.getPlayer().getTune(), null);
		}
	}

	protected void setPlayOriginal(final boolean playOriginal) {
		util.getConfig().getAudio().setPlayOriginal(playOriginal);
		if (util.getPlayer().getDriverSettings().getOutput().getDriver() instanceof CmpMP3File) {
			((CmpMP3File) util.getPlayer().getDriverSettings().getOutput()
					.getDriver()).setPlayOriginal(playOriginal);
		}
	}

}
