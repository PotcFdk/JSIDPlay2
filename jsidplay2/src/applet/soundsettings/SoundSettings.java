package applet.soundsettings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import resid_builder.resid.ISIDDefs.SamplingMethod;
import sidplay.ConsolePlayer;
import sidplay.ConsolePlayer.DriverSettings;
import sidplay.ConsolePlayer.OUTPUTS;
import sidplay.ConsolePlayer.SIDEMUS;
import sidplay.audio.CmpMP3File;
import sidplay.ini.IniReader;
import applet.download.DownloadThread;
import applet.download.IDownloadListener;
import applet.entities.config.Configuration;
import applet.events.IMadeProgress;
import applet.events.IReplayTune;
import applet.events.IUpdateUI;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;
import applet.filefilter.MP3FileFilter;

public class SoundSettings extends XDialog implements IDownloadListener,
		UIEventListener {
	protected ConsolePlayer consolePl;
	protected Player player;
	protected Configuration config;

	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	private SwingEngine swix;

	protected JTextField defaultTime, mp3, proxyHost, proxyPort, dwnlUrl6581R2,
			dwnlUrl6581R4, dwnlUrl8580R5;
	protected JCheckBox enableSldb, singleSong, proxyEnable;
	protected JComboBox<String> soundDevice, samplingRate;
	protected JComboBox<Integer> hardsid6581, hardsid8580;
	protected JComboBox<SamplingMethod> samplingMethod;
	protected JRadioButton playMP3, playEmulation;
	protected JButton mp3Browse, download6581R2Btn, download6581R4Btn,
			download8580R5Btn;
	protected JLabel playerId, tuneSpeed;

	protected int hardsid6581Num = 1;
	protected int hardsid8580Num = 1;
	protected long lastUpdate;
	protected String hvscName;
	protected int currentSong;
	protected DownloadThread downloadThread;

	public Action doEnableSldb = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getSidplay2().setEnableDatabase(enableSldb.isSelected());
			consolePl.setSLDb(enableSldb.isSelected());
		}
	};

	public Action playSingleSong = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getSidplay2().setSingle(singleSong.isSelected());
			consolePl.getTrack().setCurrentSingle(singleSong.isSelected());
		}
	};

	public Action setSoundDevice = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (soundDevice.getSelectedIndex()) {
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
			// replay last tune
			uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	};

	public Action setSid6581 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			hardsid6581Num = (Integer) hardsid6581.getSelectedItem();
			config.getEmulation().setHardsid6581(hardsid6581Num);
			config.getEmulation().setHardsid8580(hardsid8580Num);
			// A restart is necessary to close/re-open the HardSID card
			consolePl.restart();
		}
	};

	public Action setSid8580 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			hardsid8580Num = (Integer) hardsid8580.getSelectedItem();
			config.getEmulation().setHardsid6581(hardsid6581Num);
			config.getEmulation().setHardsid8580(hardsid8580Num);
			// A restart is necessary to close/re-open the HardSID card
			consolePl.restart();
		}
	};

	public Action setSamplingRate = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getAudio().setFrequency(
					Integer.valueOf(samplingRate.getSelectedItem().toString()));
			// replay last tune
			uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	};

	public Action setSamplingMethod = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getAudio().setSampling(
					(SamplingMethod) samplingMethod.getSelectedItem());
			consolePl.updateSidEmulation();
		}
	};

	public Action playEmulatedSound = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setPlayOriginal(false);
		}
	};

	public Action playRecordedSound = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setPlayOriginal(true);
		}
	};

	public Action setRecording = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getAudio().setMp3File(mp3.getText());
		}
	};

	public Action doBrowse = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setFileFilter(new MP3FileFilter());
			final int result = fc.showOpenDialog(SoundSettings.this);
			if (result == JFileChooser.APPROVE_OPTION
					&& fc.getSelectedFile() != null) {
				mp3.setText(fc.getSelectedFile().getAbsolutePath());
				config.getAudio().setMp3File(mp3.getText());
				// replay last tune
				uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
				});
			}
		}
	};

	public Action setEnableProxy = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			proxyHost.setEnabled(proxyEnable.isSelected());
			proxyPort.setEnabled(proxyEnable.isSelected());
			config.getSidplay2().setEnableProxy(proxyEnable.isSelected());
		}
	};

	public Action setProxyHost = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getSidplay2().setProxyHostname(proxyHost.getText());
		}
	};

	public Action setProxyPort = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getSidplay2().setProxyPort(
					proxyPort.getText().length() > 0 ? Integer
							.valueOf(proxyPort.getText()) : 80);
		}
	};

	public Action download6581R2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getOnline().setSoasc6581R2(dwnlUrl6581R2.getText());
		}
	};

	public Action startDownload6581R2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final String url = config.getOnline().getSoasc6581R2();
			downloadStart(MessageFormat.format(url, hvscName, currentSong));
		}
	};

	public Action download6581R4 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getOnline().setSoasc6581R4(dwnlUrl6581R4.getText());
		}
	};

	public Action startDownload6581R4 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final String url = config.getOnline().getSoasc6581R4();
			downloadStart(MessageFormat.format(url, hvscName, currentSong));
		}
	};

	public Action download8580R5 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			config.getOnline().setSoasc6581R4(dwnlUrl8580R5.getText());
		}
	};

	public Action startDownload8580R5 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final String url = config.getOnline().getSoasc8580R5();
			downloadStart(MessageFormat.format(url, hvscName, currentSong));
		}
	};

	public SoundSettings(ConsolePlayer cp, Player pl, Configuration cfg) {
		this.consolePl = cp;
		this.player = pl;
		this.config = cfg;
		uiEvents.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				uiEvents.removeListener(SoundSettings.this);
			}
		});
		try {
			swix = new SwingEngine(this);
			swix.insert(SoundSettings.class.getResource("SoundSettings.xml"),
					this);

			fillComboBoxes();
			setDefaultsAndActions();
			setTune(player.getTune());

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public SwingEngine getSwix() {
		return swix;
	}

	protected final void fillComboBoxes() {
		this.soundDevice.removeActionListener(setSoundDevice);
		this.soundDevice.addItem(swix.getLocalizer().getString("SOUNDCARD"));
		this.soundDevice.addItem(swix.getLocalizer().getString("HARDSID4U"));
		this.soundDevice.addItem(swix.getLocalizer().getString("WAV_RECORDER"));
		this.soundDevice.addItem(swix.getLocalizer().getString("MP3_RECORDER"));
		this.soundDevice.addItem(swix.getLocalizer()
				.getString("COMPARE_TO_MP3"));
		this.soundDevice.addActionListener(setSoundDevice);
		// Put in enough devices for:
		// 4 HardSID PCI cards +
		// 4 HardSID USB chips +
		// 8 Network SID Devices
		hardsid6581.removeActionListener(setSid6581);
		hardsid8580.removeActionListener(setSid8580);
		for (int i = 1; i <= 16; i++) {
			this.hardsid6581.addItem(i);
			this.hardsid8580.addItem(i);
		}
		hardsid6581.addActionListener(setSid6581);
		hardsid8580.addActionListener(setSid8580);
		this.samplingRate.removeActionListener(setSamplingRate);
		this.samplingRate.addItem("44100");
		this.samplingRate.addItem("48000");
		this.samplingRate.addItem("96000");
		this.samplingRate.addActionListener(setSamplingRate);

		this.samplingMethod.removeActionListener(setSamplingMethod);
		this.samplingMethod.addItem(SamplingMethod.DECIMATE);
		this.samplingMethod.addItem(SamplingMethod.RESAMPLE);
		this.samplingMethod.addActionListener(setSamplingMethod);
	}

	private void setDefaultsAndActions() {
		{
			final int seconds = config.getSidplay2().getPlayLength();
			defaultTime.setText(String.format("%02d:%02d", seconds / 60,
					seconds % 60));
			defaultTime.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String time = defaultTime.getText();
					final int secs = IniReader.parseTime(time);
					if (secs != -1) {
						consolePl.getTimer().setDefaultLength(secs);
						config.getSidplay2().setPlayLength(secs);
						defaultTime.setToolTipText(getSwix().getLocalizer()
								.getString("DEFAULT_LENGTH_TIP"));
						defaultTime.setBackground(Color.white);
					} else {
						defaultTime.setToolTipText(getSwix().getLocalizer()
								.getString("DEFAULT_LENGTH_FORMAT"));
						defaultTime.setBackground(Color.RED);
					}
				}

			});
		}
		{
			enableSldb.setEnabled(!"".equals(config.getSidplay2().getHvsc()));
			enableSldb.setSelected(config.getSidplay2().isEnableDatabase());
			singleSong.setSelected(config.getSidplay2().isSingle());
		}
		{
			soundDevice.removeActionListener(setSoundDevice);
			DriverSettings driverSettings = consolePl.getDriverSettings();
			OUTPUTS out = driverSettings.getOutput();
			SIDEMUS sid = driverSettings.getSid();
			if (out == OUTPUTS.OUT_SOUNDCARD && sid == SIDEMUS.EMU_RESID) {
				soundDevice.setSelectedIndex(0);
			} else if (out == OUTPUTS.OUT_NULL && sid == SIDEMUS.EMU_HARDSID) {
				soundDevice.setSelectedIndex(1);
			} else if (out == OUTPUTS.OUT_LIVE_WAV && sid == SIDEMUS.EMU_RESID) {
				soundDevice.setSelectedIndex(2);
			} else if (out == OUTPUTS.OUT_LIVE_MP3 && sid == SIDEMUS.EMU_RESID) {
				soundDevice.setSelectedIndex(3);
			} else if (out == OUTPUTS.OUT_COMPARE && sid == SIDEMUS.EMU_RESID) {
				soundDevice.setSelectedIndex(4);
			} else {
				soundDevice.setSelectedIndex(0);
			}
			soundDevice.addActionListener(setSoundDevice);
		}
		{
			hardsid6581.removeActionListener(setSid6581);
			hardsid8580.removeActionListener(setSid8580);
			hardsid6581Num = config.getEmulation().getHardsid6581();
			hardsid6581.setSelectedIndex(hardsid6581Num);
			hardsid8580Num = config.getEmulation().getHardsid8580();
			hardsid8580.setSelectedIndex(hardsid8580Num);
			hardsid6581.addActionListener(setSid6581);
			hardsid8580.addActionListener(setSid8580);

			samplingRate.removeActionListener(setSamplingRate);
			samplingRate.setSelectedItem(String.valueOf(config.getAudio()
					.getFrequency()));
			samplingRate.addActionListener(setSamplingRate);
			samplingMethod.removeActionListener(setSamplingMethod);
			samplingMethod.setSelectedItem(config.getAudio().getSampling());
			samplingMethod.addActionListener(setSamplingMethod);

			mp3.setText(config.getAudio().getMp3File());
			playMP3.setSelected(config.getAudio().isPlayOriginal());
			playEmulation.setSelected(!config.getAudio().isPlayOriginal());

			proxyEnable.setSelected(consolePl.getConfig().getSidplay2()
					.isEnableProxy());
			proxyHost.setText(consolePl.getConfig().getSidplay2()
					.getProxyHostname());
			proxyPort.setText(String.valueOf(consolePl.getConfig()
					.getSidplay2().getProxyPort()));
			dwnlUrl6581R2.setText(config.getOnline().getSoasc6581R2());
			dwnlUrl6581R4.setText(config.getOnline().getSoasc6581R4());
			dwnlUrl8580R5.setText(config.getOnline().getSoasc8580R5());
		}
	}

	public void setTune(final SidTune sidTuneMod) {
		lastUpdate = 0;

		if (sidTuneMod == null) {
			return;
		}
		final SidTuneInfo tuneInfo = sidTuneMod.getInfo();
		final String name = PathUtils.getHVSCName(config.getSidplay2()
				.getHvsc(), sidTuneMod.getInfo().file);
		if (name != null) {
			hvscName = name.replace(".sid", "");
			currentSong = tuneInfo.currentSong;
		}
		tuneSpeed.setText("");
		final StringBuilder ids = new StringBuilder();
		for (final String s : sidTuneMod.identify()) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(s);
		}
		playerId.setText(ids.toString());
	}

	@Override
	public void notify(UIEvent evt) {
		if (evt.isOfType(IUpdateUI.class)) {
			C64 c64 = player.getC64();

			final EventScheduler ctx = c64.getEventScheduler();
			final ISID2Types.CPUClock systemClock = c64.getClock();
			if (systemClock != null) {
				final double waitClocks = systemClock.getCpuFrequency();

				final long now = ctx.getTime(Event.Phase.PHI1);
				final double interval = now - lastUpdate;
				if (interval < waitClocks) {
					return;
				}
				lastUpdate = now;

				final double callsSinceLastRead = c64
						.callsToPlayRoutineSinceLastTime()
						* waitClocks
						/ interval;
				/* convert to number of calls per frame */
				tuneSpeed.setText(String.format("%.1f "
						+ swix.getLocalizer().getString("CALLS_PER_FRAME"),
						callsSinceLastRead / systemClock.getRefresh()));
			}
		}
	}

	public void downloadStart(String url) {
		System.out.println("Download URL: <" + url + ">");
		try {
			downloadThread = new DownloadThread(config, SoundSettings.this,
					new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void downloadStep(final int pct) {
		uiEvents.fireEvent(IMadeProgress.class, new IMadeProgress() {

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
			uiEvents.fireEvent(IMadeProgress.class, new IMadeProgress() {

				@Override
				public int getPercentage() {
					return 100;
				}
			});
		} else {
			soundDevice.setSelectedIndex(4);
			mp3.setText(downloadedFile.getAbsolutePath());
			config.getAudio().setMp3File(mp3.getText());
			setPlayOriginal(true);
			playMP3.setSelected(true);
			// replay last tune
			uiEvents.fireEvent(IReplayTune.class, new IReplayTune() {
			});
		}
	}

	public void setOutputDevice(final OUTPUTS device, final SIDEMUS emu) {
		consolePl.getDriverSettings().setOutput(device);
		consolePl.getDriverSettings().setSid(emu);
	}

	public void setPlayOriginal(final boolean playOriginal) {
		config.getAudio().setPlayOriginal(playOriginal);
		if (consolePl.getDriverSettings().getDevice() instanceof CmpMP3File) {
			((CmpMP3File) consolePl.getDriverSettings().getDevice())
					.setPlayOriginal(playOriginal);
		}
	}

}
