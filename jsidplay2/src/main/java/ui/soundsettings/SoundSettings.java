package ui.soundsettings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.ConsolePlayer.DriverSettings;
import sidplay.ConsolePlayer.OUTPUTS;
import sidplay.ConsolePlayer.SIDEMUS;
import sidplay.audio.CmpMP3File;
import sidplay.ini.IniReader;
import ui.common.C64Stage;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.events.IMadeProgress;
import ui.events.IPlayTune;
import ui.events.IReplayTune;
import ui.events.UIEvent;

public class SoundSettings extends C64Stage implements IDownloadListener {

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private TextField defaultTime, mp3, proxyHost, proxyPort, dwnlUrl6581R2,
			dwnlUrl6581R4, dwnlUrl8580R5;
	@FXML
	private CheckBox enableSldb, singleSong, proxyEnable;
	@FXML
	private ComboBox<String> soundDevice;
	@FXML
	private ComboBox<Integer> hardsid6581, hardsid8580, samplingRate;
	@FXML
	private ComboBox<SamplingMethod> samplingMethod;
	@FXML
	private RadioButton playMP3, playEmulation;
	@FXML
	private Button mp3Browse, download6581R2Btn, download6581R4Btn,
			download8580R5Btn;
	@FXML
	private Label playerId, tuneSpeed;

	private ObservableList<resid_builder.resid.ISIDDefs.SamplingMethod> samplingMethods = FXCollections
			.<resid_builder.resid.ISIDDefs.SamplingMethod> observableArrayList();

	private ObservableList<String> soundDevices = FXCollections
			.<String> observableArrayList();

	private long lastUpdate;
	private String hvscName;
	private int currentSong;
	private DownloadThread downloadThread;
	private boolean duringInitialization;
	private Timeline timer;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		duringInitialization = true;
		final int seconds = getConfig().getSidplay2().getPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60,
				seconds % 60));
		enableSldb.setDisable("".equals(getConfig().getSidplay2().getHvsc()));
		enableSldb.setSelected(getConfig().getSidplay2().isEnableDatabase());
		singleSong.setSelected(getConfig().getSidplay2().isSingle());
		soundDevice.setItems(soundDevices);
		soundDevices.addAll(getBundle().getString("SOUNDCARD"), getBundle()
				.getString("HARDSID4U"), getBundle().getString("WAV_RECORDER"),
				getBundle().getString("MP3_RECORDER"),
				getBundle().getString("COMPARE_TO_MP3"));
		DriverSettings driverSettings = getConsolePlayer().getDriverSettings();
		OUTPUTS out = driverSettings.getOutput();
		SIDEMUS sid = driverSettings.getSid();
		if (out == OUTPUTS.OUT_SOUNDCARD && sid == SIDEMUS.EMU_RESID) {
			soundDevice.getSelectionModel().select(0);
		} else if (out == OUTPUTS.OUT_NULL && sid == SIDEMUS.EMU_HARDSID) {
			soundDevice.getSelectionModel().select(1);
		} else if (out == OUTPUTS.OUT_LIVE_WAV && sid == SIDEMUS.EMU_RESID) {
			soundDevice.getSelectionModel().select(2);
		} else if (out == OUTPUTS.OUT_LIVE_MP3 && sid == SIDEMUS.EMU_RESID) {
			soundDevice.getSelectionModel().select(3);
		} else if (out == OUTPUTS.OUT_COMPARE && sid == SIDEMUS.EMU_RESID) {
			soundDevice.getSelectionModel().select(4);
		} else {
			soundDevice.getSelectionModel().select(0);
		}
		hardsid6581.getSelectionModel().select(
				Integer.valueOf(getConfig().getEmulation().getHardsid6581()));
		hardsid8580.getSelectionModel().select(
				Integer.valueOf(getConfig().getEmulation().getHardsid8580()));
		samplingRate.getSelectionModel().select(
				Integer.valueOf(getConfig().getAudio().getFrequency()));
		samplingMethod.setItems(samplingMethods);
		samplingMethods
				.addAll(SamplingMethod.DECIMATE, SamplingMethod.RESAMPLE);
		samplingMethod.getSelectionModel().select(
				getConfig().getAudio().getSampling());
		mp3.setText(getConfig().getAudio().getMp3File());
		playMP3.setSelected(getConfig().getAudio().isPlayOriginal());
		playEmulation.setSelected(!getConfig().getAudio().isPlayOriginal());

		proxyEnable.setSelected(getConsolePlayer().getConfig().getSidplay2()
				.isEnableProxy());
		proxyHost.setText(getConsolePlayer().getConfig().getSidplay2()
				.getProxyHostname());
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setText(String.valueOf(getConsolePlayer().getConfig()
				.getSidplay2().getProxyPort()));
		proxyPort.setEditable(proxyEnable.isSelected());
		dwnlUrl6581R2.setText(getConfig().getOnline().getSoasc6581R2());
		dwnlUrl6581R4.setText(getConfig().getOnline().getSoasc6581R4());
		dwnlUrl8580R5.setText(getConfig().getOnline().getSoasc8580R5());
		setTune(getPlayer().getTune());

		final Duration oneFrameAmt = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(oneFrameAmt,
				new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent evt) {
						C64 c64 = getPlayer().getC64();

						final EventScheduler ctx = c64.getEventScheduler();
						final ISID2Types.CPUClock systemClock = c64.getClock();
						if (systemClock != null) {
							final double waitClocks = systemClock
									.getCpuFrequency();

							final long now = ctx.getTime(Event.Phase.PHI1);
							final double interval = now - lastUpdate;
							if (interval < waitClocks) {
								return;
							}
							lastUpdate = now;

							final double callsSinceLastRead = c64
									.callsToPlayRoutineSinceLastTime()
									* waitClocks / interval;
							/* convert to number of calls per frame */
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									tuneSpeed.setText(String.format(
											"%.1f "
													+ getBundle().getString(
															"CALLS_PER_FRAME"),
											callsSinceLastRead
													/ systemClock.getRefresh()));
								}
							});
						}
					}
				});
		timer = TimelineBuilder.create().cycleCount(Animation.INDEFINITE)
				.keyFrames(oneFrame).build();
		timer.playFromStart();

		duringInitialization = false;
	}

	@Override
	protected void doCloseWindow() {
		timer.stop();
	}

	@FXML
	private void doEnableSldb() {
		getConfig().getSidplay2().setEnableDatabase(enableSldb.isSelected());
		getConsolePlayer().setSLDb(enableSldb.isSelected());
	}

	@FXML
	private void playSingleSong() {
		getConfig().getSidplay2().setSingle(singleSong.isSelected());
		getConsolePlayer().getTrack().setCurrentSingle(singleSong.isSelected());
	}

	@FXML
	private void setSoundDevice() {
		switch (soundDevice.getSelectionModel().getSelectedIndex()) {
		case 0:
			setOutputDevice(OUTPUTS.OUT_SOUNDCARD, SIDEMUS.EMU_RESID);
			break;

		case 1:
			setOutputDevice(OUTPUTS.OUT_NULL, SIDEMUS.EMU_HARDSID);
			break;

		case 2:
			setOutputDevice(OUTPUTS.OUT_LIVE_WAV, SIDEMUS.EMU_RESID);
			break;

		case 3:
			setOutputDevice(OUTPUTS.OUT_LIVE_MP3, SIDEMUS.EMU_RESID);
			break;
		case 4:
			setOutputDevice(OUTPUTS.OUT_COMPARE, SIDEMUS.EMU_RESID);
			break;

		}
		restart();
	}

	@FXML
	private void setSid6581() {
		getConfig().getEmulation().setHardsid6581(
				hardsid6581.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSid8580() {
		getConfig().getEmulation().setHardsid8580(
				hardsid8580.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSamplingRate() {
		getConfig().getAudio().setFrequency(
				samplingRate.getSelectionModel().getSelectedItem());
		restart();
	}

	@FXML
	private void setSamplingMethod() {
		getConfig().getAudio().setSampling(
				samplingMethod.getSelectionModel().getSelectedItem());
		getConsolePlayer().updateSidEmulation();
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
		getConfig().getAudio().setMp3File(mp3.getText());
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
			getConfig().getAudio().setMp3File(mp3.getText());
			restart();
		}
	}

	@FXML
	private void setEnableProxy() {
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setEditable(proxyEnable.isSelected());
		getConfig().getSidplay2().setEnableProxy(proxyEnable.isSelected());
	}

	@FXML
	private void setProxyHost() {
		getConfig().getSidplay2().setProxyHostname(proxyHost.getText());
	}

	@FXML
	private void setProxyPort() {
		getConfig().getSidplay2().setProxyPort(
				proxyPort.getText().length() > 0 ? Integer.valueOf(proxyPort
						.getText()) : 80);
	}

	@FXML
	private void setDownloadUrl6581R2() {
		getConfig().getOnline().setSoasc6581R2(dwnlUrl6581R2.getText());
	}

	@FXML
	private void startDownload6581R2() {
		final String url = getConfig().getOnline().getSoasc6581R2();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void setDownloadUrl6581R4() {
		getConfig().getOnline().setSoasc6581R4(dwnlUrl6581R4.getText());
	}

	@FXML
	private void startDownload6581R4() {
		final String url = getConfig().getOnline().getSoasc6581R4();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void setDownloadUrl8580R5() {
		getConfig().getOnline().setSoasc6581R4(dwnlUrl8580R5.getText());
	}

	@FXML
	private void startDownload8580R5() {
		final String url = getConfig().getOnline().getSoasc8580R5();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			getConsolePlayer().getTimer().setDefaultLength(secs);
			getConfig().getSidplay2().setPlayLength(secs);
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_FORMAT"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	private void setTune(SidTune sidTune) {
		lastUpdate = 0;

		if (sidTune == null) {
			return;
		}
		final SidTuneInfo tuneInfo = sidTune.getInfo();
		String name = PathUtils.getCollectionName(new File(getConfig()
				.getSidplay2().getHvsc()), tuneInfo.file);
		if (name != null) {
			hvscName = name.replace(".sid", "");
			currentSong = tuneInfo.currentSong;
		}
		tuneSpeed.setText("");
		final StringBuilder ids = new StringBuilder();
		for (final String s : sidTune.identify()) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(s);
		}
		playerId.setText(ids.toString());
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			getUiEvents().fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	}

	private void downloadStart(String url) {
		System.out.println("Download URL: <" + url + ">");
		try {
			downloadThread = new DownloadThread(getConfig(),
					SoundSettings.this, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void setOutputDevice(final OUTPUTS device, final SIDEMUS emu) {
		getConsolePlayer().getDriverSettings().setOutput(device);
		getConsolePlayer().getDriverSettings().setSid(emu);
	}

	private void setPlayOriginal(final boolean playOriginal) {
		getConfig().getAudio().setPlayOriginal(playOriginal);
		if (getConsolePlayer().getDriverSettings().getDevice() instanceof CmpMP3File) {
			((CmpMP3File) getConsolePlayer().getDriverSettings().getDevice())
					.setPlayOriginal(playOriginal);
		}
	}

	@Override
	public void downloadStep(final int pct) {
		getUiEvents().fireEvent(IMadeProgress.class, new IMadeProgress() {

			@Override
			public int getPercentage() {
				return pct;
			}
		});
	}

	@Override
	public void downloadStop(File downloadedFile) {
		downloadThread = null;

		if (downloadedFile == null) {
			getUiEvents().fireEvent(IMadeProgress.class, new IMadeProgress() {

				@Override
				public int getPercentage() {
					return 100;
				}
			});
		} else {
			soundDevice.getSelectionModel().select(4);
			mp3.setText(downloadedFile.getAbsolutePath());
			getConfig().getAudio().setMp3File(mp3.getText());
			setPlayOriginal(true);
			playMP3.setSelected(true);
			restart();
		}
	}

	@Override
	public void notify(UIEvent evt) {
		if (evt.isOfType(IPlayTune.class)) {
			final IPlayTune ifObj = (IPlayTune) evt.getUIEventImpl();
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					setTune(ifObj.getSidTune());
				}
			});
		}
	}

}
