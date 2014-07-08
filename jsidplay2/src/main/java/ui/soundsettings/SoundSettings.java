package ui.soundsettings;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import libsidplay.Player;
import sidplay.audio.CmpMP3File;
import ui.common.C64Window;

public class SoundSettings extends C64Window {

	@FXML
	protected TextField mp3, proxyHost, proxyPort;
	@FXML
	private CheckBox proxyEnable;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	private Button mp3Browse;

	private boolean duringInitialization;

	public SoundSettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		duringInitialization = true;
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
