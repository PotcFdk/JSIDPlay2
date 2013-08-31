package sidplay;

import hardsid_builder.HardSID;
import hardsid_builder.HardSIDBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.Player;
import libsidplay.common.ISID2Types.CPUClock;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.mos6510.IMOS6510Disassembler;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PRG2TAP;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import libsidutils.cpuparser.CPUParser;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.CmpMP3File;
import sidplay.audio.NaturalFinishedException;
import sidplay.consoleplayer.DriverSettings;
import sidplay.consoleplayer.Emulation;
import sidplay.consoleplayer.MediaType;
import sidplay.consoleplayer.Output;
import sidplay.consoleplayer.State;
import sidplay.consoleplayer.Timer;
import sidplay.consoleplayer.Track;
import sidplay.ini.IniConfig;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;

public class ConsolePlayer {
	/** Previous song select timeout (< 4 secs) **/
	private static final int SID2_PREV_SONG_TIMEOUT = 4;
	private static final int MAX_SPEED = 32;

	private SidTune tune;
	private IConfig iniCfg;
	private final Player player = new Player();
	private ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>();

	private final Timer timer = new Timer();
	private final Track track = new Track();
	private final DriverSettings driverSettings = new DriverSettings();

	private String outputFilename;
	private int currentSpeed = 1;
	private int quietLevel;
	private int verboseLevel;

	private boolean v1mute, v2mute, v3mute;
	private boolean filterEnable;

	private Thread fPlayerThread;

	private STIL stil;
	private SidDatabase sidDatabase;
	private SIDBuilder sidEmuFactory;
	private IMOS6510Disassembler disassembler;
	
	private IExtendImageListener policy;
	
	// MP3 saved settings:
	private Output lastOutput;
	private Emulation lastSidEmu;
	private boolean lastPlayOriginal;
	private boolean lastTimeMP3;

	public ConsolePlayer(IConfig config) {
		stateProperty.set(State.STOPPED);
		iniCfg = config;
		final IEmulationSection emulation = iniCfg.getEmulation();
		filterEnable = emulation.isFilter();
		track.setSingle(iniCfg.getSidplay2().isSingle());
		String hvscRoot = iniCfg.getSidplay2().getHvsc();
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				setSidDatabase(new SidDatabase(input));
			} catch (IOException e) {
				// silently ignored!
			}
		}
	}

	public SidTune getTune() {
		return tune;
	}

	public final IConfig getConfig() {
		return iniCfg;
	}

	public final Player getPlayer() {
		return player;
	}

	public final ObjectProperty<State> stateProperty() {
		return stateProperty;
	}

	public final DriverSettings getDriverSettings() {
		return driverSettings;
	}

	public final Timer getTimer() {
		return timer;
	}

	public final Track getTrack() {
		return track;
	}

	public int getCurrentSpeed() {
		return currentSpeed;
	}

	public void setCurrentSpeed(int current) {
		this.currentSpeed = current;
	}

	/**
	 * Create the SID emulation.
	 * 
	 * @param emu
	 *            The SID emulation to use (ReSID, HardSID, etc).
	 * @param audioConfig
	 *            The {@link AudioConfig} to use for the SID emulation.
	 * @param cpuFrequency
	 *            The CPU frequency to use for the SID.
	 * 
	 * @return True if the SID emulation could be created; false otherwise.
	 */
	private boolean createSidEmu(final Emulation emu, AudioConfig audioConfig,
			double cpuFrequency) {
		sidEmuFactory = null;

		switch (emu) {
		case EMU_RESID: {
			final ReSIDBuilder rs = new ReSIDBuilder(audioConfig, cpuFrequency);
			sidEmuFactory = rs;
			break;
		}

		case EMU_HARDSID:
			final HardSIDBuilder hs = new HardSIDBuilder(iniCfg);
			if (!hs.bool()) {
				displayError(hs.error());
				return false;
			}

			hs.setDevicesToUse(iniCfg);
			if (!hs.bool()) {
				displayError(hs.error());
				return false;
			}

			if (!hs.bool()) {
				displayError(hs.error());
				return false;
			}

			sidEmuFactory = hs;
			break;

		default:
			break;
		}

		return true;
	}

	protected boolean open() throws InterruptedException {
		if (stateProperty.get() == State.RESTART) {
			stateProperty.set(State.STOPPED);
		}

		// Select the required song
		SidTuneInfo tuneInfo = null;
		if (tune != null) {
			track.setSelected(tune.selectSong(track.getSelected()));
			if (track.getFirst() == 0) {
				// A different tune is opened (resetTrack was called)?
				// We mark a new play-list start
				track.setFirst(track.getSelected());
			}
			tuneInfo = tune.getInfo();
		}
		player.setTune(tune);

		int songs = 1;
		int currentSong = 1;
		File file = null;
		if (tuneInfo != null) {
			track.setSongs(tuneInfo.songs);
			songs = tuneInfo.songs;
			currentSong = tuneInfo.currentSong;
			file = tuneInfo.file;
		}

		CPUClock cpuFreq = iniCfg.getEmulation().getUserClockSpeed();
		if (cpuFreq == null) {
			cpuFreq = iniCfg.getEmulation().getDefaultClockSpeed();
			if (tuneInfo != null) {
				switch (tuneInfo.clockSpeed) {
				case UNKNOWN:
				case ANY:
					cpuFreq = iniCfg.getEmulation().getDefaultClockSpeed();
					break;
				case PAL:
				case NTSC:
					cpuFreq = CPUClock.valueOf(tuneInfo.clockSpeed.toString());
					break;
				}
			}
		}
		player.setClock(cpuFreq);

		final IAudioSection audio = iniCfg.getAudio();
		if (lastTimeMP3) {
			// restore settings after MP3 has been played last time
			driverSettings.setOutput(lastOutput);
			driverSettings.setEmulation(lastSidEmu);
			audio.setPlayOriginal(lastPlayOriginal);
			lastTimeMP3 = false;
		}
		if (file != null && file.getName().toLowerCase().endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			lastOutput = driverSettings.getOutput();
			lastSidEmu = driverSettings.getEmulation();
			lastPlayOriginal = audio.isPlayOriginal();
			lastTimeMP3 = true;
			driverSettings.setOutput(Output.OUT_COMPARE);
			driverSettings.setEmulation(Emulation.EMU_RESID);
			audio.setPlayOriginal(true);
			audio.setMp3File(file.getAbsolutePath());
		}
		if (driverSettings.getDevice() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			((CmpMP3File) driverSettings.getDevice()).setPlayOriginal(audio
					.isPlayOriginal());
			((CmpMP3File) driverSettings.getDevice()).setMp3File(new File(audio
					.getMp3File()));
		}

		/* Determine number of SIDs */
		int secondAddress = 0;
		driverSettings.setChannels(1);
		{
			if (iniCfg.getEmulation().isForceStereoTune()) {
				secondAddress = iniCfg.getEmulation().getDualSidBase();
			} else if (tuneInfo != null) {
				if (tuneInfo.sidChipBase2 != 0) {
					secondAddress = tuneInfo.sidChipBase2;
				}
			}
		}

		if (secondAddress != 0) {
			driverSettings.setChannels(2);
			if (secondAddress != 0xd400) {
				player.getC64().setSecondSIDAddress(secondAddress);
			}
		}
		final AudioConfig audioConfig = AudioConfig.getInstance(
				iniCfg.getAudio(), driverSettings.getChannels());
		audioConfig.setTuneFilename(file);
		audioConfig.setSongCount(songs);
		audioConfig.setCurrentSong(currentSong);
		audioConfig.setOutputfilename(outputFilename);
		try {
			driverSettings.getDevice().open(audioConfig);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		if (!createSidEmu(driverSettings.getEmulation(), audioConfig,
				cpuFreq.getCpuFrequency())) {
			return false;
		}

		if (sidEmuFactory instanceof ReSIDBuilder) {
			ReSIDBuilder reSIDBuilder = (ReSIDBuilder) sidEmuFactory;
			reSIDBuilder.setSIDVolume(0, dB2Factor(iniCfg.getAudio()
					.getLeftVolume()));
			reSIDBuilder.setSIDVolume(1, dB2Factor(iniCfg.getAudio()
					.getRightVolume()));
			reSIDBuilder.setOutput(Output.OUT_NULL.getDriver());
		}

		// According to the configuration, the SIDs must be updated.
		updateSidEmulation();

		/* We should have our SIDs configured now. */

		/* Stereo SID at 0xd400 hack */
		if (secondAddress == 0xd400) {
			final SIDEmu s1 = player.getC64().getSID(0);
			final SIDEmu s2 = player.getC64().getSID(1);

			player.getC64().setSID(0,
					new SIDEmu(player.getC64().getEventScheduler()) {
						@Override
						public void reset(byte volume) {
							s1.reset(volume);
						}

						@Override
						public byte read(int addr) {
							return s1.read(addr);
						}

						@Override
						public void write(int addr, byte data) {
							s1.write(addr, data);
							s2.write(addr, data);
						}

						@Override
						public byte readInternalRegister(int addr) {
							return s1.readInternalRegister(addr);
						}

						@Override
						public void clock() {
							s1.clock();
						}

						@Override
						public void setEnabled(int num, boolean mute) {
							s1.setEnabled(num, mute);
						}

						@Override
						public void setFilter(boolean enable) {
							s1.setFilter(enable);
						}

						@Override
						public ChipModel getChipModel() {
							return s1.getChipModel();
						}
					});
		}

		// Start the player. Do this by fast
		// forwarding to the start position
		setCurrentSpeed(MAX_SPEED);
		driverSettings.getDevice().setFastForward(getCurrentSpeed());
		v1mute = v2mute = v3mute = false;

		player.reset();

		// Initialize floppies
		player.enableFloppyDiskDrives(getConfig().getC1541().isDriveOn());
		player.connectC64AndC1541WithParallelCable(getConfig().getC1541()
				.isParallelCable());
		for (int driveNum = 0; driveNum < player.getFloppies().length; driveNum++) {
			final C1541 floppy = player.getFloppies()[driveNum];
			floppy.setFloppyType(getConfig().getC1541().getFloppyType());
			floppy.setRamExpansion(0, getConfig().getC1541()
					.isRamExpansionEnabled0());
			floppy.setRamExpansion(1, getConfig().getC1541()
					.isRamExpansionEnabled1());
			floppy.setRamExpansion(2, getConfig().getC1541()
					.isRamExpansionEnabled2());
			floppy.setRamExpansion(3, getConfig().getC1541()
					.isRamExpansionEnabled3());
			floppy.setRamExpansion(4, getConfig().getC1541()
					.isRamExpansionEnabled4());
		}
		player.turnPrinterOnOff(getConfig().getPrinter().isPrinterOn());

		// As yet we don't have a required songlength
		// so try the songlength database
		if (tune != null) {
			if (sidDatabase != null && !timer.isValid()
					&& iniCfg.getSidplay2().isEnableDatabase()) {
				final int length = getSongLength(tune);
				if (length >= 0) {
					// length==0 means forever
					// this is used for tunes
					// of unknown length
					timer.setDefaultLength(length);
				}
			}
		} else {
			// normal reset? Disable timer
			timer.setDefaultLength(0);
		}
		// Set up the play timer
		timer.setStop(0);
		timer.setStop(timer.getStop() + timer.getDefaultLength());
		if (timer.isValid()) {
			// Length relative to start
			timer.setStop(timer.getStop() + timer.getStart());
		} else {
			// Check to make start time dosen't exceed end
			if ((timer.getStop() & (timer.getStart() >= timer.getStop() ? 1 : 0)) != 0) {
				displayError("ERROR: Start time exceeds song length!");
				return false;
			}
		}

		timer.setCurrent(~0);
		stateProperty.set(State.RUNNING);
		return true;
	}

	protected void close() {
		if (stateProperty.get() == State.EXIT) {
			// Natural finish
			if (sidEmuFactory instanceof HardSIDBuilder) {
				((HardSIDBuilder) sidEmuFactory).flush();
				((HardSIDBuilder) sidEmuFactory).reset();
			}
		}

		if (sidEmuFactory != null) {
			for (int i = 0; i < 2; i++) {
				SIDEmu s = player.getC64().getSID(i);
				if (s != null) {
					sidEmuFactory.unlock(s);
					player.getC64().setSID(i, null);
				}
			}
		}
		sidEmuFactory = null;

		driverSettings.getDevice().close();
	}

	public final int getHardSIDCount() {
		SIDBuilder emu = sidEmuFactory;
		if (emu instanceof HardSIDBuilder) {
			return ((HardSIDBuilder) emu).devices();
		} else {
			return 0;
		}
	}

	/**
	 * Enable/disable song length database
	 * 
	 * @param enableSLDb
	 *            enable SLDb
	 */
	public void setSLDb(final boolean enableSLDb) {
		if (!timer.isValid()) {
			if (enableSLDb && tune != null && sidDatabase != null) {
				final int length = getSongLength(tune);
				if (length >= 0) {
					timer.setDefaultLength(length);
					timer.setStop(timer.getDefaultLength());
				}
			} else {
				timer.setDefaultLength(0);
				timer.setStop(0);
			}
		}
	}

	/**
	 * Out play loop to be externally called
	 * 
	 * @throws InterruptedException
	 */
	public boolean play() throws InterruptedException {
		/* handle switches to next song etc. */
		final int seconds = player.time();
		if (seconds != timer.getCurrent()) {
			timer.setCurrent(seconds);

			if (seconds == timer.getStart()) {
				normalSpeed();
				player.setDebug(disassembler);
				if (sidEmuFactory instanceof ReSIDBuilder) {
					((ReSIDBuilder) sidEmuFactory).setOutput(driverSettings
							.getDevice());
				}
			}

			if (iniCfg.getSidplay2().isEnableDatabase() && timer.getStop() != 0
					&& seconds >= timer.getStop()) {
				// Single song?
				if (track.isSingle()) {
					stateProperty.set(State.EXIT);
				} else {
					nextSong();

					// Check play-list end
					if (track.getSelected() == track.getFirst()
							&& !track.isLoop()) {
						stateProperty.set(State.EXIT);
					}
				}
				if (stateProperty.get() == State.EXIT) {
					// Natural finish
					if (sidEmuFactory instanceof HardSIDBuilder) {
						((HardSIDBuilder) sidEmuFactory).flush();
						((HardSIDBuilder) sidEmuFactory).reset();
					}
				}
			}
		}

		if (stateProperty.get() == State.RUNNING) {
			try {
				player.play(10000);
			} catch (NaturalFinishedException e) {
				stateProperty.set(State.EXIT);
				throw e;
			}
		}

		return stateProperty.get() == State.RUNNING
				|| stateProperty.get() == State.PAUSED;
	}

	private void decodeKeys() {
		try {
			final int key = System.in.read();
			switch (key) {
			case 'h':
				stateProperty.set(State.RESTART);
				track.setSelected(1);
				break;

			case 'e':
				stateProperty.set(State.RESTART);
				track.setSelected(track.getSongs());
				break;

			case '>':
				nextSong();
				break;

			case '<':
				previousSong();
				break;

			case '.':
				fastForward();
				break;

			case ',':
				normalSpeed();
				break;

			case 'p':
				pause();
				break;

			case '1':
				v1mute = !v1mute;
				player.mute(0, 0, v1mute);
				break;

			case '2':
				v2mute = !v2mute;
				player.mute(0, 1, v2mute);
				break;

			case '3':
				v3mute = !v3mute;
				player.mute(0, 2, v3mute);
				break;

			case '4':
				v1mute = !v1mute;
				player.mute(1, 0, v1mute);
				break;

			case '5':
				v2mute = !v2mute;
				player.mute(1, 1, v2mute);
				break;

			case '6':
				v3mute = !v3mute;
				player.mute(1, 2, v3mute);
				break;

			case 'f': {
				filterEnable ^= true;
				for (int i = 0; i < 2; i++) {
					SIDEmu s = player.getC64().getSID(i);
					if (s != null) {
						s.setFilter(filterEnable);
					}
				}
				break;
			}

			case 'q':
				quit();
				break;

			default:
				break;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void pause() {
		if (stateProperty.get() == State.PAUSED) {
			stateProperty.set(State.RUNNING);
		} else {
			stateProperty.set(State.PAUSED);
			driverSettings.getDevice().pause();
		}
	}

	public void nextSong() {
		stateProperty.set(State.RESTART);
		track.setSelected(track.getSelected() + 1);
		if (track.getSelected() > track.getSongs()) {
			track.setSelected(1);
		}
	}

	public void previousSong() {
		stateProperty.set(State.RESTART);
		if (player.time() < SID2_PREV_SONG_TIMEOUT) {
			track.setSelected(track.getSelected() - 1);
			if (track.getSelected() < 1) {
				track.setSelected(track.getSongs());
			}
		}
	}

	private void restart() {
		stateProperty.set(State.RESTART);
	}

	public void fastForward() {
		setCurrentSpeed(getCurrentSpeed() * 2);
		if (getCurrentSpeed() > MAX_SPEED) {
			setCurrentSpeed(MAX_SPEED);
		}
		driverSettings.getDevice().setFastForward(getCurrentSpeed());
	}

	public void normalSpeed() {
		setCurrentSpeed(1);
		driverSettings.getDevice().setFastForward(1);
	}

	public void quit() {
		stateProperty.set(State.QUIT);
	}

	private void displayError(final String error) {
		System.err.println(this + ": " + error);
	}

	/**
	 * Convert time from integer.
	 * 
	 * @param str
	 *            The time string to parse.
	 * @return The time as an integer.
	 */
	private long parseTime(final String str) {
		int sep;
		long _time;

		// Check for empty string
		if (str.length() == 0) {
			return -1;
		}

		sep = str.lastIndexOf(':');
		if (sep == -1) {
			// User gave seconds
			_time = Integer.valueOf(str);
		} else {
			// Read in MM:SS format
			int val;
			val = Integer.valueOf(str.substring(0, sep));
			if (val < 0 || val > 99) {
				return -1;
			}
			_time = (long) val * 60;
			val = Integer.valueOf(str.substring(sep + 1));
			if (val < 0 || val > 59) {
				return -1;
			}
			_time += val;
		}

		return _time;
	}

	/**
	 * Parse command line arguments
	 * 
	 * @param argv
	 *            The command line arguments.
	 * @return
	 */
	public int args(final String[] argv) {
		int infile = -1;
		int i = 0;
		boolean err = false;

		if (argv.length == 0) {
			displayArgs(null);
			return -1;
		}

		// default arg options
		driverSettings.setOutput(Output.OUT_SOUNDCARD);
		driverSettings.setEmulation(Emulation.EMU_RESID);

		// parse command line arguments
		while (i < argv.length) {
			if (argv[i].charAt(0) == '-' && argv[i].length() > 1) {
				// help options
				if (argv[i].charAt(1) == 'h' || argv[i].equals("--help")) {
					displayArgs(null);
					return 0;
				} else if (argv[i].equals("--help-debug")) {
					displayDebugArgs();
					return 0;
				}

				else if (argv[i].charAt(1) == 'b') {
					final long time = parseTime(argv[i].substring(2));
					if (time == -1) {
						err = true;
					}
					timer.setStart(time);
				} else if (argv[i].equals("-fd")) {
					// Override sidTune and enable the second sid
					iniCfg.getEmulation().setForceStereoTune(true);
				} else if (argv[i].startsWith("-f")) {
					if (argv[i].length() == 2) {
						err = true;
					}
					iniCfg.getAudio().setFrequency(
							Integer.valueOf(argv[i].substring(2)));
				}

				// New/No filter options
				else if (argv[i].startsWith("-nf")) {
					if (argv[i].length() == 3) {
						filterEnable = false;
					}
				}

				// Newer sid (8580)
				else if (argv[i].startsWith("-ns")) {
					switch (argv[i].charAt(3)) {
					case '1':
						iniCfg.getEmulation()
								.setUserSidModel(ChipModel.MOS8580);
						break;
					// No new sid so use old one (6581)
					case '0':
						iniCfg.getEmulation()
								.setUserSidModel(ChipModel.MOS6581);
						break;
					default:
						err = true;
					}
				}

				// Track options
				else if (argv[i].startsWith("-nols")) {
					track.setLoop(true);
					track.setSingle(true);
					track.setFirst(Integer.valueOf(argv[i].substring(4)));
				} else if (argv[i].startsWith("-ol")) {
					track.setLoop(true);
					track.setFirst(Integer.valueOf(argv[i].substring(3)));
				} else if (argv[i].startsWith("-os")) {
					track.setLoop(true);
					track.setFirst(Integer.valueOf(argv[i].substring(3)));
				} else if (argv[i].startsWith("-o")) {
					// User forgot track number ?
					if (argv[i].length() == 2) {
						err = true;
					}
					track.setFirst(Integer.valueOf(argv[i].substring(2)));
				}

				else if (argv[i].startsWith("-q")) {
					if (argv[i].length() == 2) {
						quietLevel = 1;
					} else {
						quietLevel = Integer.valueOf(argv[i].substring(2));
					}
				}

				else if (argv[i].startsWith("-t")) {
					final long time = parseTime(argv[i].substring(2));
					if (time == -1) {
						err = true;
					}
					timer.setDefaultLength(time);
					timer.setValid(true);
					iniCfg.getSidplay2().setEnableDatabase(true);
				}

				// Video/Verbose Options
				else if (argv[i].equals("-vnf")) {
					iniCfg.getEmulation().setUserClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vpf")) {
					iniCfg.getEmulation().setUserClockSpeed(CPUClock.PAL);
				} else if (argv[i].equals("-vn")) {
					iniCfg.getEmulation().setDefaultClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vp")) {
					iniCfg.getEmulation().setDefaultClockSpeed(CPUClock.PAL);
				} else if (argv[i].startsWith("-v")) {
					if (argv[i].length() == 2) {
						verboseLevel = 1;
					} else {
						verboseLevel = Integer.valueOf(argv[i].substring(2));
					}
				}

				// File format conversions
				else if (argv[i].equals("-m")) {
					driverSettings.setOutput(Output.OUT_MP3);
					outputFilename = argv[++i];
				} else if (argv[i].equals("-w") || argv[i].equals("--wav")) {
					driverSettings.setOutput(Output.OUT_WAV);
					outputFilename = argv[++i];
				} else if (argv[i].equals("-lm")) {
					driverSettings.setOutput(Output.OUT_LIVE_MP3);
					i++;
					outputFilename = argv[i];
				} else if (argv[i].equals("-lw") || argv[i].equals("-l")) {
					driverSettings.setOutput(Output.OUT_LIVE_WAV);
					i++;
					outputFilename = argv[i];
				}

				// Hardware selection
				else if (argv[i].equals("--hardsid")) {
					driverSettings.setEmulation(Emulation.EMU_HARDSID);
					driverSettings.setOutput(Output.OUT_NULL);
				}

				// These are for debug
				else if (argv[i].equals("--none")) {
					driverSettings.setEmulation(Emulation.EMU_NONE);
					driverSettings.setOutput(Output.OUT_NULL);
				} else if (argv[i].equals("--nosid")) {
					driverSettings.setEmulation(Emulation.EMU_NONE);
				} else if (argv[i].equals("--cpu-debug")) {
					disassembler = CPUParser.getInstance();
				}

				else {
					err = true;
				}

			} else {
				// Reading file name
				if (infile == -1) {
					infile = i;
				} else {
					err = true;
				}
			}

			if (err) {
				displayArgs(argv[i]);
				return -1;
			}

			i++; // next index
		}

		// Can only loop if not creating audio files
		if (driverSettings.getOutput().isFileBased()) {
			track.setLoop(false);
		}

		// Check to see if we are trying to generate an audio file
		// whilst using a hardware emulation
		if (driverSettings.getOutput().isFileBased()
				&& driverSettings.getEmulation() == Emulation.EMU_HARDSID) {
			displayError("ERROR: Cannot generate audio files using hardware emulations");
			return -1;
		}

		if (infile == -1) {
			return -1;
		}
		try {
			try (InputStream stream = new URL(argv[infile]).openConnection()
					.getInputStream()) {
				// load from URL
				loadTune(SidTune.load(stream));
			} catch (MalformedURLException e) {
				// load from file
				loadTune(SidTune.load(new File(argv[infile])));
			}
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
		if (tune == null) {
			return -1;
		}

		// Select the desired track
		// and also mark the play-list start
		track.setFirst(tune.selectSong(track.getFirst()));
		track.setSelected(track.getFirst());
		if (track.isSingle()) {
			track.setSongs(1);
		}

		// If user provided no time then load songlength database
		// and set default lengths incase it's not found in there.
		{
			if (driverSettings.getOutput().isFileBased() && timer.isValid()
					&& timer.getDefaultLength() == 0) {
				// Time of 0 provided for wav generation
				displayError("ERROR: -t0 invalid in record mode");
				return -1;
			}
			if (!timer.isValid()) {
				timer.setDefaultLength(iniCfg.getSidplay2().getPlayLength());
				if (driverSettings.getOutput().isFileBased()) {
					timer.setDefaultLength(iniCfg.getSidplay2()
							.getRecordLength());
				}
			}
		}
		return 1;
	}

	/**
	 * Load a tune, this is either an absolute path name or a URL of an ui
	 * version.
	 * 
	 * @param t
	 *            SidTune to load.
	 */
	private void loadTune(final SidTune t) {
		// Next time player is used, the track is reset
		// 0 means set first song of play-list, next time open() is called
		track.setFirst(0);
		// 0 means use start song next time open() is called
		track.setSelected(0);
		
		tune = t;
		if (t == null) {
			track.setFirst(1);
			track.setSongs(0);
		}
	}

	@SuppressWarnings("resource")
	private void displayArgs(final String arg) {
		final PrintStream out = arg != null ? System.err : System.out;

		if (arg != null) {
			out.println("Option Error: " + arg);
		} else {
			out.println("Syntax: java -jar jsidplay2_console.jar [-<option>...] <datafile>");
		}

		out.println("Options:" + "\n" + " --help|-h    display this screen"
				+ "\n" + " --help-debug debug help menu" + "\n"
				+ " -b<num>      set start time in [m:]s format (default 0)"
				+ "\n"

				+ " -f<num>      set frequency in Hz (default: "
				+ iniCfg.getAudio().getFrequency()
				+ ")"
				+ "\n"
				+ " -fd          force dual sid environment"
				+ "\n"

				+ " -nf[filter]  no/new SID filter emulation"
				+ "\n"
				+ " -ns[0|1]     (no) MOS 8580 waveforms (default: from tune or cfg)"
				+ "\n"

				+ " -o<l|s>      looping and/or single track"
				+ "\n"
				+ " -o<num>      start track (default: preset)"
				+ "\n"

				+ " -t<num>      set play length in [m:]s format (0 is endless)"
				+ "\n"

				+ " -<v[level]|q>       verbose (level=0,1,2) or quiet (no time display) output"
				+ "\n"
				+ " -v[p|n][f]   set VIC PAL/NTSC clock speed (default: defined by song)"
				+ "\n"
				+ "              Use 'f' to force the clock by preventing speed fixing"
				+ "\n"

				+ " -w name     create wav file"
				+ "\n -m name     create mp3 file"
				+ "\n -lm name    create mp3 file and Java Sound"
				+ "\n -lw name    create wav file and Java Sound");
		out.println(" --hardsid enable hardsid support\n");
		out.println("\n"
		// Changed to new homepage address
				+ "Home Page: http://jsidplay2.sourceforge.net/" + "\n");

		/*
		 * XXX: minor blemish, as we might not actually be interactive now. The
		 * REAL problem is with having args() method, which is used internally.
		 * We should just have "setXyz() method, not a cp.args(new String[] {
		 * "--xyz" });
		 */
		System.out.println("<press return>");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void displayDebugArgs() {
		final PrintStream out = System.out;

		out.println("Debug Options:" + "\n"
				+ " --cpu-debug   display cpu register and assembly dumps"
				+ "\n" + " --delay=<num> simulate c64 power on delay" + "\n"

				+ " --wav<file>   wav file output device" + "\n"
				+ " --none        no audio output device" + "\n"
				+ " --nosid       no sid emulation" + "\n");
		System.out.println("<press return>");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void menu() {
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (quietLevel > 1) {
			return;
		}

		final IConsoleSection console = iniCfg.getConsole();

		System.out.println(String.format("%c%s%c", console.getTopLeft(),
				setfill(console.getHorizontal(), 54), console.getTopRight()));
		System.out.println(String.format("%c%54s%c", console.getVertical(),
				" Java SIDPLAY - Music Player & C64 SID Chip Emulator  ",
				console.getVertical()));
		System.out.println(String.format("%c%s%c", console.getJunctionLeft(),
				setfill(console.getHorizontal(), 54),
				console.getJunctionRight()));
		if (tuneInfo.numberOfInfoStrings > 0) {
			if (tuneInfo.numberOfInfoStrings == 3) {
				System.out.println(String.format("%c Title        : %37s %c",
						console.getVertical(), tuneInfo.infoString[0],
						console.getVertical()));
				System.out.println(String.format("%c Author       : %37s %c",
						console.getVertical(), tuneInfo.infoString[1],
						console.getVertical()));
				System.out.println(String.format("%c Released     : %37s %c",
						console.getVertical(), tuneInfo.infoString[2],
						console.getVertical()));
			} else {
				for (int i = 0; i < tuneInfo.numberOfInfoStrings; i++) {
					System.out.println(String.format(
							"%c Description  : %37s %c", console.getVertical(),
							tuneInfo.infoString[i], console.getVertical()));
				}
			}
			System.out.println(String.format("%c%s%c",
					console.getJunctionLeft(),
					setfill(console.getHorizontal(), 54),
					console.getJunctionRight()));
		}
		if (verboseLevel != 0) {
			System.out.println(String.format("%c File format  : %37s %c",
					console.getVertical(), tuneInfo.getClass().getSimpleName(),
					console.getVertical()));
			System.out.println(String.format("%c Filename(s)  : %37s %c",
					console.getVertical(), tuneInfo.file.getName(),
					console.getVertical()));
		}
		System.out.print(String.format("%c Playlist     : ",
				console.getVertical()));
		{ // This will be the format used for playlists
			int i = 1;
			if (!track.isSingle()) {
				i = track.getSelected();
				i -= track.getFirst() - 1;
				if (i < 1) {
					i += track.getSongs();
				}
			}
			System.out.println(String.format("%37s %c",
					i + "/" + track.getSongs() + " (tune "
							+ tuneInfo.currentSong + "/" + tuneInfo.songs + "["
							+ tuneInfo.startSong + "])"
							+ (track.isLoop() ? " [LOOPING]" : ""),
					console.getVertical()));
		}
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			System.out.println(String.format("%c Song Speed   : %37s %c",
					console.getVertical(),
					tune.getSongSpeed(track.getSelected()),
					console.getVertical()));
		}
		System.out.print(String.format("%c Song Length  : ",
				console.getVertical()));
		if (timer.getStop() != 0) {
			final String time = String.format("%02d:%02d",
					(timer.getStop() / 60 % 100), (timer.getStop() % 60));
			System.out.print(String.format("%37s %c", "" + time,
					console.getVertical()));
		} else if (timer.isValid()) {
			System.out.print(String.format("%37s %c", "FOREVER",
					console.getVertical()));
		} else {
			System.out.print(String.format("%37s %c", "UNKNOWN",
					console.getVertical()));
		}
		System.out.println();
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			StringBuffer line = new StringBuffer();
			// Display PSID Driver location
			line.append("DRIVER = ");
			if (tuneInfo.determinedDriverAddr == 0) {
				line.append("NOT PRESENT");
			} else {
				line.append(String.format("$%04x",
						tuneInfo.determinedDriverAddr));
				line.append(String.format("-$%04x",
						tuneInfo.determinedDriverAddr
								+ tuneInfo.determinedDriverLength - 1));
			}
			if (tuneInfo.playAddr == 0xffff) {
				line.append(String.format(", SYS = $%04x", tuneInfo.initAddr));
			} else {
				line.append(String.format(", INIT = $%04x", tuneInfo.initAddr));
			}

			System.out.println(String.format("%c Addresses    : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));
			line = new StringBuffer();
			line.append(String.format("LOAD   = $%04x", tuneInfo.loadAddr));
			line.append(String.format("-$%04x", tuneInfo.loadAddr
					+ tuneInfo.c64dataLen - 1));
			if (tuneInfo.playAddr != 0xffff) {
				line.append(String.format(", PLAY = $%04x", tuneInfo.playAddr));
			}
			System.out.println(String.format("%c              : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			line = new StringBuffer();
			line.append(String.format("Filter = %s", (filterEnable ? "Yes"
					: "No")));
			/* XXX ignores 2nd SID */
			line.append(String.format(", Model = %s",
					(tuneInfo.sid1Model == SidTune.Model.MOS8580 ? "8580"
							: "6581")));
			System.out.println(String.format("%c SID Details  : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			if (verboseLevel > 1) {
				line = new StringBuffer();
				System.out.println(String.format("%c Delay        : %37s %c",
						console.getVertical(), line.toString(),
						console.getVertical()));
			}
		}

		System.out
				.println(String.format("%c%s%c", console.getBottomLeft(),
						setfill(console.getHorizontal(), 54),
						console.getBottomRight()));
		System.out.println("keyboard control (press enter after command):");
		System.out.println("< > - play previous/next tune");
		System.out.println(", . - normal/faster speed");
		System.out.println("p   - pause/continue player");
		System.out.println("h e - play first/last tune");
		System.out.println("1   - mute voice 1");
		System.out.println("2   - mute voice 2");
		System.out.println("3   - mute voice 3");
		System.out.println("4   - mute voice 1 (stereo)");
		System.out.println("5   - mute voice 2 (stereo)");
		System.out.println("6   - mute voice 3 (stereo)");
		System.out.println("f   - enable/disable filter");
		System.out.println("q   - quit player");
	}

	private String setfill(final char ch, final int length) {
		final StringBuffer ret = new StringBuffer();
		for (int i = 0; i < length; i++) {
			ret.append(ch);
		}
		return ret.toString();
	}

	public static void main(final String[] args) throws InterruptedException {
		final ConsolePlayer player = new ConsolePlayer(new IniConfig());
		if (player.args(args) < 0) {
			System.exit(1);
		}
		if (player.tune == null) {
			System.exit(0);
		}
		player.menu();

		main_restart: while (true) {
			if (!player.open()) {
				player.close();
				System.exit(0);
			}
			while (true) {
				if (!player.play()) {
					break;
				}
				try {
					if (player.quietLevel < 2
							&& (player.stateProperty().get() == State.PAUSED || System.in
									.available() != 0)) {
						player.decodeKeys();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			player.close();
			if (player.stateProperty().get() == State.RESTART) {
				continue main_restart;
			}
			break;
		}
	}

	/**
	 * Player runnable to play music in the background.
	 */
	private transient final Runnable playerRunnable = new Runnable() {
		@Override
		public void run() {
			if (getTune() != null && getTune().getInfo().file != null) {
				System.out.println("Play File: <"
						+ getTune().getInfo().file.getAbsolutePath() + ">");
			}
			// Run until the player gets stopped
			while (true) {
				try {
					// Open tune and play
					if (!open()) {
						return;
					}
					// Play next chunk of sound data, until it gets stopped
					while (true) {
						// Pause? sleep for awhile
						if (stateProperty().get() == State.PAUSED) {
							Thread.sleep(250);
						}
						// Play a chunk
						if (!play()) {
							break;
						}
					}
				} catch (InterruptedException e) {
				} finally {
					// Don't forget to close
					close();
				}

				// "Play it once, Sam. For old times' sake."
				if (stateProperty().get() == State.RESTART) {
					continue;
				}
				// Stop it
				break;

			}
		}

	};

	/**
	 * Start emulation (start player thread).
	 */
	public void startC64() {
		fPlayerThread = new Thread(playerRunnable);
		fPlayerThread.setPriority(Thread.MAX_PRIORITY);
		fPlayerThread.start();
	}

	/**
	 * Stop emulation (stop player thread).
	 */
	public void stopC64() {
		try {
			while (fPlayerThread.isAlive()) {
				quit();
				fPlayerThread.join(3000);
				// This is only the last option, if the player can not be
				// stopped clean
				fPlayerThread.interrupt();
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Play tune.
	 * 
	 * @param sidTune
	 *            file to play the tune (null means just reset C64)
	 * @param command
	 */
	public void playTune(final SidTune sidTune, String command) {
		// Stop previous run
		stopC64();
		// load tune
		loadTune(sidTune);
		// set command to type after reset
		getPlayer().setCommand(command);
		// Start emulation
		startC64();
	}

	public void insertMedia(File mediaFile, File autostartFile,
			MediaType mediaType) {
		try {
			getConfig().getSidplay2().setLastDirectory(
					mediaFile.getParentFile().getAbsolutePath());
			switch (mediaType) {
			case TAPE:
				insertTape(mediaFile, autostartFile);
				break;

			case DISK:
				insertDisk(mediaFile, autostartFile);
				break;

			case CART:
				insertCartridge(mediaFile);
				break;

			default:
				break;
			}
		} catch (IOException e) {
			System.err.println(String.format("Cannot attach file '%s'.",
					mediaFile.getAbsolutePath()));
		}
	}

	private void insertDisk(final File selectedDisk, final File autostartFile)
			throws IOException {
		// automatically turn drive on
		getPlayer().enableFloppyDiskDrives(true);
		getConfig().getC1541().setDriveOn(true);
		// attach selected disk into the first disk drive
		DiskImage disk = getPlayer().getFloppies()[0].getDiskController()
				.insertDisk(selectedDisk);
		if (policy != null) {
			disk.setExtendImagePolicy(policy);
		}
		if (autostartFile != null) {
			try {
				playTune(SidTune.load(autostartFile), null);
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	private void insertTape(final File selectedTape, final File autostartFile)
			throws IOException {
		if (!selectedTape.getName().toLowerCase().endsWith(".tap")) {
			// Everything, which is not a tape convert to tape first
			final File convertedTape = new File(getConfig().getSidplay2()
					.getTmpDir(), selectedTape.getName() + ".tap");
			convertedTape.deleteOnExit();
			String[] args = new String[] { selectedTape.getAbsolutePath(),
					convertedTape.getAbsolutePath() };
			PRG2TAP.main(args);
			getPlayer().getDatasette().insertTape(convertedTape);
		} else {
			getPlayer().getDatasette().insertTape(selectedTape);
		}
		if (autostartFile != null) {
			try {
				playTune(SidTune.load(autostartFile), null);
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	private void insertCartridge(final File selectedFile) throws IOException {
		// Insert a cartridge
		getPlayer().getC64().insertCartridge(selectedFile);
		// reset required after inserting the cartridge
		playTune(null, null);
	}

	public void setExtendImagePolicy(IExtendImageListener policy) {
		this.policy = policy;
	}

	/**
	 * Replace old SIDs with new SIDs.
	 * 
	 */
	public void updateSidEmulation() {
		if (sidEmuFactory != null) {

			/* Find first chip model. */
			ChipModel chipModel = determineSIDModel(iniCfg.getEmulation()
					.getUserSidModel(), iniCfg.getEmulation()
					.getDefaultSidModel(),
					tune != null ? tune.getInfo().sid1Model : null);
			updateSIDEmu(0, chipModel);

			if (driverSettings.getChannels() == 2) {
				ChipModel stereoChipModel = determineSIDModel(iniCfg
						.getEmulation().getStereoSidModel(), chipModel,
						Model.valueOf(chipModel.toString()));
				updateSIDEmu(1, stereoChipModel);
			}
		}
	}

	/**
	 * Determine SID model to be used. The following search order is used:
	 * <OL>
	 * <LI>Use the locked SID model
	 * <LI>Use model specified in the currently played tune
	 * <LI>User default SID model
	 * </OL>
	 * 
	 * @return the SID model to be used
	 */
	private ChipModel determineSIDModel(ChipModel userForcedChoice,
			ChipModel defaultChoice, Model model) {
		/* Determine SID type and construct them */
		ChipModel chipModel = userForcedChoice;
		if (chipModel == null) {
			chipModel = defaultChoice;
			if (model != null) {
				switch (model) {
				case MOS6581:
				case MOS8580:
					chipModel = ChipModel.valueOf(model.toString());
					break;
				default:
					break;
				}
			}
		}
		return chipModel;
	}

	/**
	 * Change SID model and sampling frequency. A current ReSID is re-used. If
	 * HardSID is used, a new chip is locked.
	 * 
	 * @param chipNum
	 *            chip number (0 - mono, 1 - stereo)
	 * @param model
	 *            chip model to use
	 */
	private void updateSIDEmu(final int chipNum, final ChipModel model) {
		SIDEmu s = player.getC64().getSID(chipNum);

		if (s == null && sidEmuFactory != null) {
			s = sidEmuFactory.lock(player.getC64().getEventScheduler(), model);
		}

		if (s instanceof HardSID) {
			// To change the HardSID it is necessary to restart!
			restart();
		}

		if (s instanceof ReSID) {
			((ReSID) s).model(model);
			IFilterSection f6581 = null;
			IFilterSection f8580 = null;
			List<? extends IFilterSection> filters = iniCfg.getFilter();
			for (IFilterSection iFilterSection : filters) {
				if (iFilterSection.getName().equals(
						iniCfg.getEmulation().getFilter6581())) {
					f6581 = iFilterSection;
				} else if (iFilterSection.getName().equals(
						iniCfg.getEmulation().getFilter8580())) {
					f8580 = iFilterSection;
				}
			}
			((ReSID) s).filter(f6581, f8580);
		}

		player.getC64().setSID(chipNum, s);
	}

	public void setSIDVolume(int i, float volumeDb) {
		if (sidEmuFactory instanceof ReSIDBuilder) {
			((ReSIDBuilder) sidEmuFactory).setSIDVolume(i, dB2Factor(volumeDb));
		}
	}

	private float dB2Factor(final float dB) {
		return (float) Math.pow(10, dB / 20);
	}

	public void setSidDatabase(SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	public int getSongLength(SidTune t) {
		if (t != null && sidDatabase != null) {
			return sidDatabase.length(t);
		}
		return -1;
	}

	public int getFullSongLength(SidTune t) {
		if (t != null && sidDatabase != null) {
			return sidDatabase.getFullSongLength(t);
		}
		return 0;
	}

	public void setSTIL(STIL stil) {
		this.stil = stil;
	}

	public STIL getStil() {
		return stil;
	}

}
