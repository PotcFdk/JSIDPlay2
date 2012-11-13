package applet.siddump;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import libsidplay.sidtune.SidTune;
import libsidutils.SIDDump.Player;
import libsidutils.SidDatabase;
import netsiddev.InvalidCommandException;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import sidplay.audio.AudioConfig;
import sidplay.ini.IniReader;
import applet.entities.config.Configuration;
import applet.events.IPlayTune;
import applet.events.IStopTune;
import applet.events.ITuneStateChanged;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;

public class SIDDump extends XDialog implements UIEventListener {

	private SwingEngine swix;

	protected JButton loadDump, saveDump, replayAll, replaySelected;
	protected JCheckBox startStop, timeInSeconds, lowResolutionMode;
	protected JTextField firstFrame, noteSpacing, maxRecordLength,
			patternSpacing, oldNoteFactor, tableFontSize, baseFreq, baseNote,
			callsPerFrame;
	protected JComboBox<String> regPlayer;
	protected JTable dumpTable;

	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	protected libsidplay.Player player;
	protected Configuration config;
	protected final SidTune tune;
	protected libsidutils.SIDDump siddump = new libsidutils.SIDDump();
	protected File fLastDir;
	protected long firstFrameVal;
	protected int noteSpacingVal, patternSpacingVal, fontSizeVal = 8,
			baseFreqVal, baseNoteVal = 0xb0, loadAddress, initAddress,
			playerAddress, subTune, seconds;
	protected float oldNoteFactorVal = 1.f;
	protected ArrayList<Player> players;

	public Action doLoadDump = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new JFileChooser(fLastDir);
			final int rc = fileDialog.showOpenDialog(SIDDump.this);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				fLastDir = fileDialog.getSelectedFile().getParentFile();
				final String name = fileDialog.getSelectedFile()
						.getAbsolutePath();
				final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
						.getModel();
				sidDumpModel.load(name);
				noteSpacing.setText(String.valueOf(sidDumpModel
						.getNoteSpacing()));
				noteSpacingVal = sidDumpModel.getNoteSpacing();
				patternSpacing.setText(String.valueOf(sidDumpModel
						.getPatternSpacing()));
				patternSpacingVal = sidDumpModel.getPatternSpacing();
				firstFrame
						.setText(String.valueOf(sidDumpModel.getFirstFrame()));
				firstFrameVal = sidDumpModel.getFirstFrame();
				lowResolutionMode.setSelected(sidDumpModel.getLowRes());
				loadAddress = sidDumpModel.getLoadAddress();
				initAddress = sidDumpModel.getInitAddress();
				playerAddress = sidDumpModel.getPlayerAddress();
				subTune = sidDumpModel.getCurrentSong();
				timeInSeconds.setSelected(sidDumpModel.getTimeInSeconds());
			}
		}
	};

	public Action doSaveDump = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new JFileChooser(fLastDir);
			final int rc = fileDialog.showSaveDialog(SIDDump.this);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				fLastDir = fileDialog.getSelectedFile().getParentFile();
				final String name = fileDialog.getSelectedFile()
						.getAbsolutePath();
				final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
						.getModel();
				sidDumpModel.save(name);
			}
		}
	};

	public Action doReplayAll = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
					.getModel();
			sidDumpModel.setAborted();
			replayTune(0, dumpTable.getRowCount() - 1);
		}
	};

	public Action doReplaySelected = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
					.getModel();
			sidDumpModel.setAborted();
			final int[] rows = dumpTable.getSelectedRows();
			replayTune(rows[0], rows[rows.length - 1]);
		}
	};

	public Action doStartStop = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
					.getModel();
			if (startStop.isSelected()) {
				// replay last tune
				uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {
					@Override
					public boolean switchToVideoTab() {
						return false;
					}

					@Override
					public File getFile() {
						if (player.getTune() != null)
							return player.getTune().getInfo().file;
						else {
							return null;
						}
					}

					@Override
					public Component getComponent() {
						return null;
					}
				});
				setTune(tune);
			} else {
				sidDumpModel.stop();
				uiEvents.fireEvent(IStopTune.class, new IStopTune() {
				});
			}
		}
	};

	public Action choosePlayer = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (regPlayer.getSelectedIndex() == -1
					|| regPlayer.getSelectedIndex() >= players.size()) {
				return;
			}
			final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
					.getModel();
			sidDumpModel.setRegOrder(players.get(regPlayer.getSelectedIndex())
					.getBytes());
		}
	};

	public SIDDump(libsidplay.Player pl, Configuration cfg) {
		this.player = pl;
		this.config = cfg;
		this.tune = pl.getTune();
		uiEvents.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				uiEvents.removeListener(SIDDump.this);
			}
		});
		try {
			swix = new SwingEngine(this);
			swix.insert(SIDDump.class.getResource("SIDDump.xml"), this);

			fillComboBoxes();
			setDefaultsAndActions();
			setTune(tune);

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));

			if (players.size() > 0) {
				// initialize with first item
				final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
						.getModel();
				sidDumpModel.setRegOrder(players.get(0).getBytes());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SwingEngine getSwix() {
		return swix;
	}

	public void setTune(SidTune sidTune) {
		final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable.getModel();
		sidDumpModel.setPlayer(player,
				AudioConfig.getInstance(config.getAudio(), 2));
		if (tune == null) {
			startStop.setEnabled(false);
			return;
		}
		startStop.setEnabled(sidTune.getInfo().playAddr != 0);

		loadAddress = tune.getInfo().loadAddr;
		sidDumpModel.setLoadAddress(loadAddress);
		initAddress = tune.getInfo().initAddr;
		sidDumpModel.setInitAddress(initAddress);
		playerAddress = tune.getInfo().playAddr;
		sidDumpModel.setPayerAddress(playerAddress);
		subTune = tune.getInfo().currentSong;
		sidDumpModel.setCurrentSong(subTune);
		sidDumpModel.setFirstFrame(firstFrameVal);
		if (seconds == 0) {
			int songLength = getSongLength(tune);
			if (songLength <= 0) {
				songLength = config.getSidplay2().getPlayLength();
				if (songLength == 0) {
					// default
					songLength = 60;
				}
			}
			maxRecordLength.setText(String.format("%02d:%02d",
					(songLength / 60 % 100), (songLength % 60)));
			sidDumpModel.setRecordLength(songLength);
		} else {
			sidDumpModel.setRecordLength(seconds);
		}
		sidDumpModel.setTimeInSeconds(timeInSeconds.isSelected());
		sidDumpModel.setOldNoteFactor(oldNoteFactorVal);
		sidDumpModel.setBaseFreq(baseFreqVal);
		sidDumpModel.setBaseNote(baseNoteVal);
		sidDumpModel.setPatternSpacing(patternSpacingVal);
		sidDumpModel.setNoteSpacing(noteSpacingVal);
		sidDumpModel.setLowRes(lowResolutionMode.isSelected());

		startStop.setEnabled(tune.getInfo().playAddr != 0);
		if (tune.getInfo().playAddr == 0) {
			startStop.setSelected(false);
			startStop.setToolTipText(swix.getLocalizer().getString(
					"NOT_AVAILABLE"));
		} else {
			startStop.setToolTipText(null);
		}

		sidDumpModel.init();
		final boolean enable = startStop.isSelected();
		if (enable) {
			// Only mono tunes are supported here!
			player.getC64().setSidWriteListener(0, sidDumpModel);
			player.getC64().setPlayRoutineObserver(sidDumpModel);
		} else {
			player.getC64().setSidWriteListener(0, null);
			player.getC64().setPlayRoutineObserver(null);
		}

	}

	private void setDefaultsAndActions() {
		final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable.getModel();
		sidDumpModel.setLocalizer(swix.getLocalizer());

		replaySelected.setEnabled(false);
		{
			noteSpacing.setText(String.valueOf(noteSpacingVal));
			noteSpacing.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = noteSpacing.getText();
					try {
						noteSpacingVal = Integer.parseInt(first);
						if (noteSpacingVal >= 0) {
							noteSpacing.setToolTipText(getSwix().getLocalizer()
									.getString("NOTE_SPACING_TIP"));
							noteSpacing.setBackground(Color.white);
						} else {
							noteSpacing.setToolTipText(getSwix().getLocalizer()
									.getString("NOTE_SPACING_NEG"));
							noteSpacing.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						noteSpacing.setToolTipText(getSwix().getLocalizer()
								.getString("NOTE_SPACING_NEG"));
						noteSpacing.setBackground(Color.RED);
					}
				}

			});
		}
		{
			maxRecordLength.setText(String.format("%02d:%02d",
					(seconds / 60 % 100), (seconds % 60)));
			maxRecordLength.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String time = maxRecordLength.getText();
					seconds = IniReader.parseTime(time);
					if (seconds != -1) {
						maxRecordLength.setToolTipText(getSwix().getLocalizer()
								.getString("MAX_RECORD_LENGTH_TIP"));
						maxRecordLength.setBackground(Color.white);
					} else {
						maxRecordLength.setToolTipText(getSwix().getLocalizer()
								.getString("MAX_RECORD_LENGTH_FORMAT"));
						maxRecordLength.setBackground(Color.RED);
					}
				}

			});
		}
		{
			patternSpacing.setText(String.valueOf(patternSpacingVal));
			patternSpacing.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = patternSpacing.getText();
					try {
						patternSpacingVal = Integer.parseInt(first);
						if (patternSpacingVal >= 0) {
							patternSpacing.setToolTipText(getSwix()
									.getLocalizer().getString(
											"PATTERN_SPACING_TIP"));
							patternSpacing.setBackground(Color.white);
						} else {
							patternSpacing.setToolTipText(getSwix()
									.getLocalizer().getString(
											"PATTERN_SPACING_NEG"));
							patternSpacing.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						patternSpacing.setToolTipText(getSwix().getLocalizer()
								.getString("PATTERN_SPACING_NEG"));
						patternSpacing.setBackground(Color.RED);
					}
				}

			});
		}
		{
			oldNoteFactor.setText(String.valueOf(oldNoteFactorVal));
			oldNoteFactor.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = oldNoteFactor.getText();
					try {
						oldNoteFactorVal = Float.parseFloat(first);
						if (oldNoteFactorVal >= 1) {
							oldNoteFactor.setToolTipText(getSwix()
									.getLocalizer().getString(
											"OLD_NOTE_FACTOR_TIP"));
							oldNoteFactor.setBackground(Color.white);
						} else {
							oldNoteFactor.setToolTipText(getSwix()
									.getLocalizer().getString(
											"OLD_NOTE_FACTOR_NEG"));
							oldNoteFactor.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						oldNoteFactor.setToolTipText(getSwix().getLocalizer()
								.getString("OLD_NOTE_FACTOR_NEG"));
						oldNoteFactor.setBackground(Color.RED);
					}
				}
			});
		}
		{
			baseFreq.setText(String.format("0x%02x", baseFreqVal));
			baseFreq.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = baseFreq.getText();
					try {
						baseFreqVal = Integer.decode(first);
						if (baseFreqVal != -1 && baseFreqVal >= 0) {
							baseFreq.setToolTipText(getSwix().getLocalizer()
									.getString("BASE_FREQ_TIP"));
							baseFreq.setBackground(Color.white);
						} else {
							baseFreq.setToolTipText(getSwix().getLocalizer()
									.getString("BASE_FREQ_HEX"));
							baseFreq.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						baseFreq.setToolTipText(getSwix().getLocalizer()
								.getString("BASE_FREQ_HEX"));
						baseFreq.setBackground(Color.RED);
					}
				}

			});
		}
		{
			dumpTable.setFont(new Font("Courier", dumpTable.getFont()
					.getStyle(), fontSizeVal));
			dumpTable.getTableHeader().setFont(
					new Font("Courier", dumpTable.getTableHeader().getFont()
							.getStyle(), fontSizeVal));
			tableFontSize.setText("" + fontSizeVal);
			tableFontSize.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = tableFontSize.getText();
					try {
						fontSizeVal = Integer.parseInt(first);
						if (fontSizeVal > 0) {
							tableFontSize.setToolTipText(getSwix()
									.getLocalizer().getString(
											"TABLE_FONT_SIZE_TIP"));
							tableFontSize.setBackground(Color.white);
							dumpTable.setFont(new Font("Courier", dumpTable
									.getFont().getStyle(), fontSizeVal));
							dumpTable.getTableHeader().setFont(
									new Font("Courier", dumpTable
											.getTableHeader().getFont()
											.getStyle(), fontSizeVal));
						} else {
							tableFontSize.setToolTipText(getSwix()
									.getLocalizer().getString(
											"TABLE_FONT_SIZE_NEG"));
							tableFontSize.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						tableFontSize.setToolTipText(getSwix().getLocalizer()
								.getString("TABLE_FONT_SIZE_NEG"));
						tableFontSize.setBackground(Color.RED);
					}
				}

			});
		}
		{
			baseNote.setText(String.format("0x%02x", baseNoteVal));
			baseNote.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = baseNote.getText();
					try {
						baseNoteVal = Integer.decode(first);
						if (baseNoteVal != -1 && baseNoteVal >= 128
								&& baseNoteVal <= 223) {
							baseNote.setToolTipText(getSwix().getLocalizer()
									.getString("BASE_NOTE_TIP"));
							baseNote.setBackground(Color.white);
						} else {
							baseNote.setToolTipText(getSwix().getLocalizer()
									.getString("BASE_NOTE_HEX"));
							baseNote.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						baseNote.setToolTipText(getSwix().getLocalizer()
								.getString("BASE_NOTE_HEX"));
						baseNote.setBackground(Color.RED);
					}
				}
			});
		}
		{
			callsPerFrame.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = callsPerFrame.getText();
					try {
						final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
								.getModel();
						final int speed = Integer.parseInt(first);
						if (speed >= 1) {
							callsPerFrame.setToolTipText(getSwix()
									.getLocalizer().getString(
											"CALLS_PER_FRAME_TIP"));
							callsPerFrame.setBackground(Color.white);
							sidDumpModel.setReplayFrequency(speed * 50);
						} else {
							callsPerFrame.setToolTipText(getSwix()
									.getLocalizer().getString(
											"CALLS_PER_FRAME_NEG"));
							callsPerFrame.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						callsPerFrame.setToolTipText(getSwix().getLocalizer()
								.getString("CALLS_PER_FRAME_NEG"));
						callsPerFrame.setBackground(Color.RED);
					}

				}
			});
		}
		{
			firstFrame.setText(String.valueOf(firstFrameVal));
			firstFrame.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(final KeyEvent e) {
					final String first = firstFrame.getText();
					try {
						firstFrameVal = Long.parseLong(first);
						if (firstFrameVal >= 0) {
							firstFrame.setToolTipText(getSwix().getLocalizer()
									.getString("FIRST_FRAME_TIP"));
							firstFrame.setBackground(Color.white);
						} else {
							firstFrame.setToolTipText(getSwix().getLocalizer()
									.getString("FIRST_FRAME_NEG"));
							firstFrame.setBackground(Color.RED);
						}
					} catch (final NumberFormatException e2) {
						firstFrame.setToolTipText(getSwix().getLocalizer()
								.getString("FIRST_FRAME_NEG"));
						firstFrame.setBackground(Color.RED);
					}
				}

			});
		}
		{
			dumpTable.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {

						@Override
						public void valueChanged(final ListSelectionEvent e) {
							replaySelected.setEnabled(dumpTable
									.getSelectedRowCount() > 0);
						}
					});
		}
	}

	private int getSongLength(final SidTune sidTuneMod) {
		SidDatabase database = SidDatabase.getInstance(config.getSidplay2()
				.getHvsc());
		if (database != null) {
			return database.length(sidTuneMod);
		}
		return -1;
	}

	private void fillComboBoxes() {
		players = siddump.getPlayers();
		for (final Player player : players) {
			regPlayer.addItem(player.getName());
		}
	}

	@Override
	public void notify(final UIEvent evt) {
		if (evt.isOfType(ITuneStateChanged.class)) {
			final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable
					.getModel();
			replaySelected.setEnabled(dumpTable.getSelectedRowCount() > 0);
			replayAll.setEnabled(true);
			sidDumpModel.stop();
		}
	}

	protected void replayTune(final int from, final int to) {
		final SIDDumpModel sidDumpModel = (SIDDumpModel) dumpTable.getModel();
		sidDumpModel.setReplayRange(from, to);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sidDumpModel.replay();
				} catch (InvalidCommandException e) {
					throw new RuntimeException(e);
				}
			}

		}).start();
	}

}
