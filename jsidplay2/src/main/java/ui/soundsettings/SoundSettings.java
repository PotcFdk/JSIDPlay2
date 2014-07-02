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
import libsidplay.player.Emulation;
import resid_builder.resid.SamplingMethod;
import sidplay.audio.Audio;
import sidplay.audio.CmpMP3File;
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
				.getBundle().getString("SOUNDCARD_RESIDFP"), util.getBundle()
				.getString("HARDSID4U"),
				util.getBundle().getString("WAV_RECORDER"), util.getBundle()
						.getString("MP3_RECORDER"),
				util.getBundle().getString("COMPARE_TO_MP3"));
		Audio audio = util.getConfig().getAudio().getAudio();
		Emulation emu = util.getConfig().getEmulation().getEmulation();
		if (audio == Audio.SOUNDCARD && emu == Emulation.RESID) {
			soundDevice.getSelectionModel().select(0);
		} else if (audio == Audio.SOUNDCARD && emu == Emulation.RESIDFP) {
			soundDevice.getSelectionModel().select(1);
		} else if (audio == Audio.NONE && emu == Emulation.HARDSID) {
			soundDevice.getSelectionModel().select(2);
		} else if (audio == Audio.LIVE_WAV && emu == Emulation.RESID) {
			soundDevice.getSelectionModel().select(3);
		} else if (audio == Audio.LIVE_MP3 && emu == Emulation.RESID) {
			soundDevice.getSelectionModel().select(4);
		} else if (audio == Audio.COMPARE_MP3 && emu == Emulation.RESID) {
			soundDevice.getSelectionModel().select(5);
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
			util.getConfig().getEmulation().setEmulation(Emulation.RESID);
			util.getConfig().getAudio().setAudio(Audio.SOUNDCARD);
			break;

		case 1:
			util.getConfig().getEmulation().setEmulation(Emulation.RESIDFP);
			util.getConfig().getAudio().setAudio(Audio.SOUNDCARD);
			break;

		case 2:
			util.getConfig().getEmulation().setEmulation(Emulation.HARDSID);
			util.getConfig().getAudio().setAudio(Audio.NONE);
			break;

		case 3:
			util.getConfig().getEmulation().setEmulation(Emulation.RESID);
			util.getConfig().getAudio().setAudio(Audio.LIVE_WAV);
			break;

		case 4:
			util.getConfig().getEmulation().setEmulation(Emulation.RESID);
			util.getConfig().getAudio().setAudio(Audio.LIVE_MP3);
			break;
		case 5:
			util.getConfig().getEmulation().setEmulation(Emulation.RESID);
			util.getConfig().getAudio().setAudio(Audio.COMPARE_MP3);
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
		IAudioSection audio = util.getConfig().getAudio();
		audio.setSampling(samplingMethod.getSelectionModel().getSelectedItem());
		CPUClock systemFrequency = CPUClock.getCPUClock(util.getConfig(), util
				.getPlayer().getTune());
		util.getPlayer().configureSIDs(
				(num, sid) -> sid.setSampling(
						systemFrequency.getCpuFrequency(),
						audio.getFrequency(), audio.getSampling()));
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
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	protected void setPlayOriginal(final boolean playOriginal) {
		util.getConfig().getAudio().setPlayOriginal(playOriginal);
		if (util.getConfig().getAudio().getAudio().getAudioDriver() instanceof CmpMP3File) {
			((CmpMP3File) util.getConfig().getAudio().getAudio()
					.getAudioDriver()).setPlayOriginal(playOriginal);
		}
	}

}
