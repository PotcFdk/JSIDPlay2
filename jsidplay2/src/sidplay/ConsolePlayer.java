package sidplay;

import hardsid_builder.HardSID;
import hardsid_builder.HardSIDBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import libsidplay.Player;
import libsidplay.common.ISID2Types.Clock;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1541.C1541;
import libsidplay.components.mos6510.IMOS6510Disassembler;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.SidDatabase;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.AudioNull;
import sidplay.audio.CmpMP3File;
import sidplay.audio.JavaSound;
import sidplay.audio.MP3File;
import sidplay.audio.NaturalFinishedException;
import sidplay.audio.ProxyDriver;
import sidplay.audio.WavFile;
import sidplay.ini.IniAudioSection;
import sidplay.ini.IniConfig;
import sidplay.ini.IniConsoleSection;
import sidplay.ini.IniEmulationSection;
import applet.disassembler.CPUParser;

public class ConsolePlayer {
	/** Previous song select timeout (3 secs) **/
	public static final int SID2_PREV_SONG_TIMEOUT = 4;

	// Player states

	public static final int playerRunning = 1;
	public static final int playerPaused = 2;
	public static final int playerStopped = 3;
	public static final int playerRestart = 4;
	public static final int playerExit = 5;
	public static final int playerFast = 128;
	public static final int playerFastRestart = playerRestart | playerFast;
	public static final int playerFastExit = playerExit | playerFast;

	public enum SIDEMUS {
		/* Same as EMU_DEFAULT except no soundcard. Still allows wav generation */
		EMU_NONE,
		/* The following require a soundcard */
		EMU_RESID,
		/* The following should disable the soundcard */
		EMU_HARDSID
	}

	public enum OUTPUTS {
		/**
		 * No audio.
		 */
		OUT_NULL(false, new AudioNull()),
		/**
		 * Java Sound API.
		 */
		OUT_SOUNDCARD(false, new JavaSound()),
		/**
		 * WAV file write.
		 */
		OUT_WAV(true, new WavFile()),
		/**
		 * MP3 file write.
		 */
		OUT_MP3(true, new MP3File()),
		/**
		 * Java Sound API plus WAV file write.
		 */
		OUT_LIVE_WAV(true, new ProxyDriver(new JavaSound(), new WavFile())),
		/**
		 * Java Sound API and MP3 file write.
		 */
		OUT_LIVE_MP3(true, new ProxyDriver(new JavaSound(), new MP3File())),
		/**
		 * Java Sound API plus recording playback.
		 */
		OUT_COMPARE(false, new CmpMP3File());

		private final boolean fileBased;
		private final AudioDriver drv;

		OUTPUTS(boolean fileBased, AudioDriver drv) {
			this.fileBased = fileBased;
			this.drv = drv;
		}

		public boolean isFileBased() {
			return fileBased;
		}

		public AudioDriver getDriver() {
			return drv;
		}
	}

	public static class DriverSettings {
		/** Default SID emulation */
		protected SIDEMUS sid = SIDEMUS.EMU_RESID;
		/** Default output */
		protected OUTPUTS output = OUTPUTS.OUT_SOUNDCARD;
		/** Number of output channels */
		protected int channels;

		public final SIDEMUS getSid() {
			return sid;
		}

		public final void setSid(final SIDEMUS sid) {
			this.sid = sid;
		}

		public final OUTPUTS getOutput() {
			return output;
		}

		public final void setOutput(final OUTPUTS output) {
			this.output = output;
		}

		public final AudioDriver getDevice() {
			return output.getDriver();
		}
	}

	public static class Timer {
		protected long start;
		protected long current;
		protected long stop;
		protected long defaultLength; // 0 - FOREVER
		protected boolean valid;

		public void setDefaultLength(final long length) {
			this.defaultLength = length;
		}
	}

	public static class Track {
		/**
		 * First song number of the play-list. 0 is used, to reset the play-list
		 * start to the start tune, if a different tune is loaded.
		 */
		protected int first;
		/**
		 * Current song number. If first > 0 it wraps around the count of songs
		 * 0 means use start song of the tune file.
		 */
		protected int selected;
		/**
		 * Number of songs in the play-list.
		 */
		protected int songs;
		/**
		 * Loop, if the play-list is played.
		 */
		protected boolean loop;
		/**
		 * Always plays a single song (start song)
		 */
		protected boolean single;

		public int getCurrentSong() {
			return selected;
		}

		public int getCurrentSongCount() {
			return songs;
		}

		public void setCurrentSingle(boolean s) {
			single = s;
		}

	}

	protected static class Speed {
		protected short current = 1;
		protected final short max = 32;
	}

	private final Player player = new Player();

	public final Player getPlayer() {
		return player;
	}
	
	private SidTune tune;

	private int state;

	public final int getState() {
		return state;
	}

	private String outputFilename;

	private final IniConfig iniCfg = new IniConfig();

	public final IniConfig getConfig() {
		return iniCfg;
	}

	private final DriverSettings driver = new DriverSettings();

	public final DriverSettings getDriverSettings() {
		return driver;
	}

	private final Timer timer = new Timer();

	public final Timer getTimer() {
		return timer;
	}

	private final Track track = new Track();

	public final Track getTrack() {
		return track;
	}

	private final Speed speed = new Speed();

	private int quietLevel;
	private int verboseLevel;

	private boolean v1mute, v2mute, v3mute;
	private boolean filterEnable;

	private IMOS6510Disassembler disassembler;
	private SIDBuilder sidEmuFactory;

	// MP3 saved settings:
	private OUTPUTS lastOutput;
	private SIDEMUS lastSidEmu;
	private boolean lastPlayOriginal;
	private boolean lastTimeMP3;

	public ConsolePlayer() {
		state = playerStopped;

		final IniEmulationSection emulation = iniCfg.emulation();
		filterEnable = emulation.isFilter();
		track.single = iniCfg.sidplay2().isSingle();
	}

	/**
	 * Create the sid emulation
	 * 
	 * @param emu
	 * @param tuneInfo
	 * @return
	 */
	public boolean createSidEmu(final SIDEMUS emu, AudioConfig audioConfig, double cpuFrequency) {
		sidEmuFactory = null;

		switch (emu) {
		case EMU_RESID: {
			final ReSIDBuilder rs = new ReSIDBuilder(audioConfig, cpuFrequency);
			sidEmuFactory = rs;
			break;
		}

		case EMU_HARDSID:
			final HardSIDBuilder hs = new HardSIDBuilder();
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

	public boolean open() throws InterruptedException {
		if ((state & ~playerFast) == playerRestart) {
			state = playerStopped;
		}

		// Select the required song
		SidTuneInfo tuneInfo = null;
		if (tune != null) {
			track.selected = tune.selectSong(track.selected);
			if (track.first == 0) {
				// A different tune is opened (resetTrack was called)?
				// We mark a new play-list start
				track.first = track.selected;
			}
			tuneInfo = tune.getInfo();
		}
		player.setTune(tune);

		int songs = 1;
		int currentSong = 1;
		File file = null;
		if (tuneInfo != null) {
			track.songs = tuneInfo.songs;
			songs = tuneInfo.songs;
			currentSong = tuneInfo.currentSong;
			file = tuneInfo.file;
		}

		Clock cpuFreq = iniCfg.emulation().getUserClockSpeed();
		if (cpuFreq == null) {
			cpuFreq = iniCfg.emulation().getDefaultClockSpeed();
			if (tuneInfo != null) {
				switch (tuneInfo.clockSpeed) {
				case UNKNOWN:
				case ANY:
					cpuFreq = iniCfg.emulation().getDefaultClockSpeed();
					break;
				case PAL:
				case NTSC:
					cpuFreq = Clock.valueOf(tuneInfo.clockSpeed.toString());
					break;
				}
			}
		}
		player.setClock(cpuFreq);

		final IniAudioSection audio = iniCfg.audio();
		if (lastTimeMP3) {
			// restore settings after MP3 has been played last time
			driver.setOutput(lastOutput);
			driver.setSid(lastSidEmu);
			audio.setPlayOriginal(lastPlayOriginal);
			lastTimeMP3 = false;
		}
		if (file != null && file.getName().toLowerCase().endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			lastOutput = driver.getOutput();
			lastSidEmu = driver.getSid();
			lastPlayOriginal = audio.isPlayOriginal();
			lastTimeMP3 = true;
			driver.setOutput(OUTPUTS.OUT_COMPARE);
			driver.setSid(SIDEMUS.EMU_RESID);
			audio.setPlayOriginal(true);
			audio.setMp3File(file);
		}
		if (driver.getDevice() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			((CmpMP3File) driver.getDevice()).setPlayOriginal(audio
					.isPlayOriginal());
			((CmpMP3File) driver.getDevice()).setMp3File(audio.getMp3File());
		}

		/* Determine number of SIDs */
		int secondAddress = 0;
		driver.channels = 1;
		{
			if (iniCfg.emulation().isForceStereoTune()) {
				secondAddress = iniCfg.emulation().getDualSidBase();
			} else if (tuneInfo != null) {
				if (tuneInfo.sidChipBase2 != 0) {
					secondAddress = tuneInfo.sidChipBase2;
				}
			}
		}

		if (secondAddress != 0) {
			driver.channels = 2;
			if (secondAddress != 0xd400) {
				player.getC64().setSecondSIDAddress(secondAddress);
			}
		}
		final AudioConfig audioConfig = iniCfg.audio().toAudioConfig(driver.channels);
		audioConfig.setTuneFilename(file);
		audioConfig.setSongCount(songs);
		audioConfig.setCurrentSong(currentSong);
		audioConfig.setOutputfilename(outputFilename);
		try {
			driver.getDevice().open(audioConfig);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		if (!createSidEmu(driver.sid, audioConfig, cpuFreq.getCpuFrequency())) {
			return false;
		}

		if (sidEmuFactory instanceof ReSIDBuilder) {
			((ReSIDBuilder) sidEmuFactory).setSIDVolume(0, dB2factor(iniCfg.audio().getLeftVolume()));
			((ReSIDBuilder) sidEmuFactory).setSIDVolume(1, dB2factor(iniCfg.audio().getRightVolume()));
			((ReSIDBuilder) sidEmuFactory).setOutput(OUTPUTS.OUT_NULL.getDriver());
		}

		// According to the configuration, the SIDs must be updated.
		updateSidEmulation();

		/* We should have our SIDs configured now. */

		/* Stereo SID at 0xd400 hack */
		if (secondAddress == 0xd400) {
			final SIDEmu s1 = player.getC64().getSID(0);
			final SIDEmu s2 = player.getC64().getSID(1);

			player.getC64().setSID(0, new SIDEmu(player.getC64().getEventScheduler()) {
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
		speed.current = speed.max;
		driver.getDevice().setFastForward(speed.current);
		v1mute = v2mute = v3mute = false;

		player.reset();

		// Initialize floppies
		player.enableFloppyDiskDrives(getConfig().c1541().isDriveOn());
		player.connectC64AndC1541WithParallelCable(getConfig().c1541()
				.isParallelCable());
		for (int driveNum = 0; driveNum < player.getFloppies().length; driveNum++) {
			final C1541 floppy = player.getFloppies()[driveNum];
			floppy.setFloppyType(getConfig().c1541().getFloppyType());
			floppy.setRamExpansion(0, getConfig().c1541()
					.isRamExpansionEnabled(0));
			floppy.setRamExpansion(1, getConfig().c1541()
					.isRamExpansionEnabled(1));
			floppy.setRamExpansion(2, getConfig().c1541()
					.isRamExpansionEnabled(2));
			floppy.setRamExpansion(3, getConfig().c1541()
					.isRamExpansionEnabled(3));
			floppy.setRamExpansion(4, getConfig().c1541()
					.isRamExpansionEnabled(4));
		}
		player.turnPrinterOnOff(getConfig().printer().isPrinterOn());

		// As yet we don't have a required songlength
		// so try the songlength database
		if (tune != null) {
			SidDatabase database = SidDatabase.getInstance(iniCfg.sidplay2()
					.getHvsc());
			if (database != null && !timer.valid
					&& iniCfg.sidplay2().isEnableDatabase()) {
				final int length = database.length(tune);
				if (length >= 0) {
					// length==0 means forever
					// this is used for tunes
					// of unknown length
					timer.defaultLength = length;
				}
			}
		} else {
			// normal reset? Disable timer
			timer.defaultLength = 0;
		}
		// Set up the play timer
		timer.stop = 0;
		timer.stop += timer.defaultLength;
		if (timer.valid) {
			// Length relative to start
			timer.stop += timer.start;
		} else {
			// Check to make start time dosen't exceed end
			if ((timer.stop & (timer.start >= timer.stop ? 1 : 0)) != 0) {
				displayError("ERROR: Start time exceeds song length!");
				return false;
			}
		}

		timer.current = ~0;
		state = playerRunning;
		return true;
	}

	private float dB2factor(float volume) {
		return (float) Math.pow(10, volume/20.0f);
	}

	public void close() {
		if (state == playerExit) {
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

		driver.getDevice().close();
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
	 * Reset track is called to play a different tune file
	 */
	public void resetTrack() {
		// 0 means set first song of play-list, next time open() is called
		track.first = 0;
		// 0 means use start song next time open() is called
		track.selected = 0;
	}

	/**
	 * Enable/disable song length database
	 * 
	 * @param enableSLDb
	 *            enable SLDb
	 */
	public void setSLDb(final boolean enableSLDb) {
		if (!timer.valid) {
			if (enableSLDb && tune != null) {
				SidDatabase database = SidDatabase.getInstance(iniCfg.sidplay2()
						.getHvsc());
				final int length = database.length(tune);
				if (length > 0) {
					timer.defaultLength = length;
					timer.stop = timer.defaultLength;
				}
			} else {
				timer.defaultLength = 0;
				timer.stop = 0;
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
		if (seconds != timer.current) {
			timer.current = seconds;

			if (seconds == timer.start) {
				normalSpeed();
				player.setDebug(disassembler);
				if (sidEmuFactory instanceof ReSIDBuilder) {
					((ReSIDBuilder) sidEmuFactory).setOutput(driver.getDevice());
				}
			}

			if (iniCfg.sidplay2().isEnableDatabase() && timer.stop != 0
					&& seconds >= timer.stop) {
				// Single song?
				if (track.single) {
					state = playerExit;
				} else {
					nextSong();

					// Check play-list end
					if (track.selected == track.first && !track.loop) {
						state = playerExit;
					}
				}
				if (state == playerExit) {
					// Natural finish
					if (sidEmuFactory instanceof HardSIDBuilder) {
						((HardSIDBuilder) sidEmuFactory).flush();
						((HardSIDBuilder) sidEmuFactory).reset();
					}
				}
			}
		}

		if (state == playerRunning) {
			try {
				player.play(10000);
			} catch (NaturalFinishedException e) {
				state = playerExit;
				throw e;
			}
		}

		return state == playerRunning || state == playerPaused;
	}

	private void decodeKeys() {
		try {
			final int key = System.in.read();
			switch (key) {
			case 'h':
				state = playerFastRestart;
				track.selected = 1;
				break;

			case 'e':
				state = playerFastRestart;
				track.selected = track.songs;
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
		if (state == playerPaused) {
			state = playerRunning;
		} else {
			state = playerPaused;
			driver.getDevice().pause();
		}
	}

	public void nextSong() {
		state = playerFastRestart;
		track.selected++;
		if (track.selected > track.songs) {
			track.selected = 1;
		}
	}

	public void previousSong() {
		state = playerFastRestart;
		if (player.time() < SID2_PREV_SONG_TIMEOUT) {
			track.selected--;
			if (track.selected < 1) {
				track.selected = track.songs;
			}
		}
	}

	public void restart() {
		state = playerFastRestart;
	}

	public void fastForward() {
		speed.current *= 2;
		if (speed.current > speed.max) {
			speed.current = speed.max;
		}
		driver.getDevice().setFastForward(speed.current);
	}

	public void normalSpeed() {
		speed.current = 1;
		driver.getDevice().setFastForward(1);
	}

	public void quit() {
		state = playerFastExit;
	}

	public void displayError(final String error) {
		System.err.println(this + ": " + error);
	}

	/**
	 * Convert time from integer
	 * 
	 * @param str
	 * @param time
	 * @return
	 */
	long parseTime(final String str) {
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
		driver.output = OUTPUTS.OUT_SOUNDCARD;
		driver.sid = SIDEMUS.EMU_RESID;

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
					timer.start = time;
				} else if (argv[i].equals("-fd")) {
					// Override sidTune and enable the second sid
					iniCfg.emulation().setForceStereoTune(true);
				} else if (argv[i].startsWith("-f")) {
					if (argv[i].length() == 2) {
						err = true;
					}
					iniCfg.audio().setFrequency(
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
						iniCfg.emulation().setUserSidModel(ChipModel.MOS8580);
						break;
					// No new sid so use old one (6581)
					case '0':
						iniCfg.emulation().setUserSidModel(ChipModel.MOS6581);
						break;
					default:
						err = true;
					}
				}

				// Track options
				else if (argv[i].startsWith("-nols")) {
					track.loop = true;
					track.single = true;
					track.first = Integer.valueOf(argv[i].substring(4));
				} else if (argv[i].startsWith("-ol")) {
					track.loop = true;
					track.first = Integer.valueOf(argv[i].substring(3));
				} else if (argv[i].startsWith("-os")) {
					track.single = true;
					track.first = Integer.valueOf(argv[i].substring(3));
				} else if (argv[i].startsWith("-o")) {
					// User forgot track number ?
					if (argv[i].length() == 2) {
						err = true;
					}
					track.first = Integer.valueOf(argv[i].substring(2));
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
					timer.defaultLength = time;
					timer.valid = true;
					iniCfg.sidplay2().setEnableDatabase(true);
				}

				// Video/Verbose Options
				else if (argv[i].equals("-vnf")) {
					iniCfg.emulation().setUserClockSpeed(Clock.NTSC);
				} else if (argv[i].equals("-vpf")) {
					iniCfg.emulation().setUserClockSpeed(Clock.PAL);
				} else if (argv[i].equals("-vn")) {
					iniCfg.emulation().setDefaultClockSpeed(Clock.NTSC);
				} else if (argv[i].equals("-vp")) {
					iniCfg.emulation().setDefaultClockSpeed(Clock.PAL);
				} else if (argv[i].startsWith("-v")) {
					if (argv[i].length() == 2) {
						verboseLevel = 1;
					} else {
						verboseLevel = Integer.valueOf(argv[i].substring(2));
					}
				}

				// File format conversions
				else if (argv[i].equals("-m")) {
					driver.output = OUTPUTS.OUT_MP3;
					outputFilename = argv[++i];
				} else if (argv[i].equals("-w") || argv[i].equals("--wav")) {
					driver.output = OUTPUTS.OUT_WAV;
					outputFilename = argv[++i];
				} else if (argv[i].equals("-lm")) {
					driver.output = OUTPUTS.OUT_LIVE_MP3;
					i++;
					outputFilename = argv[i];
				} else if (argv[i].equals("-lw") || argv[i].equals("-l")) {
					driver.output = OUTPUTS.OUT_LIVE_WAV;
					i++;
					outputFilename = argv[i];
				}

				// Hardware selection
				else if (argv[i].equals("--hardsid")) {
					driver.sid = SIDEMUS.EMU_HARDSID;
					driver.output = OUTPUTS.OUT_NULL;
				}

				// These are for debug
				else if (argv[i].equals("--none")) {
					driver.sid = SIDEMUS.EMU_NONE;
					driver.output = OUTPUTS.OUT_NULL;
				} else if (argv[i].equals("--nosid")) {
					driver.sid = SIDEMUS.EMU_NONE;
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
		if (driver.output.isFileBased()) {
			track.loop = false;
		}

		// Check to see if we are trying to generate an audio file
		// whilst using a hardware emulation
		if (driver.output.isFileBased() && driver.sid == SIDEMUS.EMU_HARDSID) {
			displayError("ERROR: Cannot generate audio files using hardware emulations");
			return -1;
		}

		if (infile == -1) {
			return -1;
		}
		try {
			if (new URL(argv[infile]).getProtocol().equals("file")) {
				tune = loadTune(new File(argv[infile]));
			} else {
				tune = loadTune(new URL(argv[infile]));
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return -1;
		}
		if (tune == null) {
			return -1;
		}

		// Select the desired track
		// and also mark the play-list start
		track.first = tune.selectSong(track.first);
		track.selected = track.first;
		if (track.single) {
			track.songs = 1;
		}

		// If user provided no time then load songlength database
		// and set default lengths incase it's not found in there.
		{
			if (driver.output.isFileBased() && timer.valid
					&& timer.defaultLength == 0) {
				// Time of 0 provided for wav generation
				displayError("ERROR: -t0 invalid in record mode");
				return -1;
			}
			if (!timer.valid) {
				timer.defaultLength = iniCfg.sidplay2().getPlayLength();
				if (driver.output.isFileBased()) {
					timer.defaultLength = iniCfg.sidplay2().getRecordLength();
				}
			}
		}
		return 1;
	}

	public SidTune loadTune (final URL url) {
		// Load the tune
		try {
			InputStream stream = null;
			try {
				// load from URL (applet version)
				stream = AccessController
						.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
							public InputStream run() throws IOException {
								return url.openConnection().getInputStream();
							}
						});
				tune = SidTune.load(stream);
				// XXX what to set if URL?
				tune.getInfo().file = null;
			} catch (PrivilegedActionException e) {
				// e.getException() should be an instance of
				// IOException,
				// as only "checked" exceptions will be "wrapped" in a
				// PrivilegedActionException.
				throw e.getException();
			} finally {
				if (stream != null) {
					stream.close();
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			tune = null;
			return null;
		}
		return tune;
	}
	/**
	 * Load a tune, this is either an absolute path name or a URL of an applet
	 * version.
	 * 
	 * @param resource
	 *            URL or filename
	 * @return 0 - OK, -1 means load error
	 */
	public SidTune loadTune(final File f) {
		// Next time player is used, the track is reset
		resetTrack();
		
		if (f == null) {
			tune = null;
			track.first = 1;
			track.songs = 0;
			return tune;
		}

		// Load the tune
		try {
			// load from file
			tune = SidTune.load(f);
		} catch (final Exception e) {
			e.printStackTrace();
			tune = null;
			return null;
		}
		return tune;
	}

	void displayArgs(final String arg) {
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
				
				+ " -f<num>      set frequency in Hz (default: " + iniCfg.audio().getFrequency() + ")"
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

	void displayDebugArgs() {
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

	void menu() {
		final SidTuneInfo tuneInfo = tune.getInfo();
		if (quietLevel > 1) {
			return;
		}

		final IniConsoleSection console = iniCfg.console();

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
					console.getVertical(),
					tuneInfo.file.getName(),
					console.getVertical()));
		}
		System.out.print(String.format("%c Playlist     : ",
				console.getVertical()));
		{ // This will be the format used for playlists
			int i = 1;
			if (!track.single) {
				i = track.selected;
				i -= track.first - 1;
				if (i < 1) {
					i += track.songs;
				}
			}
			System.out.println(String.format("%37s %c", i + "/" + track.songs
					+ " (tune " + tuneInfo.currentSong + "/" + tuneInfo.songs
					+ "[" + tuneInfo.startSong + "])"
					+ (track.loop ? " [LOOPING]" : ""), console.getVertical()));
		}
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			System.out.println(String.format("%c Song Speed   : %37s %c",
					console.getVertical(), tune.getSongSpeed(track.selected),
					console.getVertical()));
		}
		System.out.print(String.format("%c Song Length  : ",
				console.getVertical()));
		if (timer.stop != 0) {
			final String time = String.format("%02d:%02d",
					(timer.stop / 60 % 100), (timer.stop % 60));
			System.out.print(String.format("%37s %c", "" + time,
					console.getVertical()));
		} else if (timer.valid) {
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
		final ConsolePlayer player = new ConsolePlayer();
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
							&& (player.getState() == playerPaused || System.in
									.available() != 0)) {
						player.decodeKeys();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			
			player.close();
			if ((player.getState() & ~playerFast) == playerRestart) {
				continue main_restart;
			}
			break;
		}
	}

	/**
	 * Replace old SIDs with new SIDs.
	 * 
	 */
	public void updateSidEmulation() {
		if (sidEmuFactory != null) {

			/* Find first chip model. */
			ChipModel chipModel = determineSIDModel(iniCfg.emulation()
					.getUserSidModel(),
					iniCfg.emulation().getDefaultSidModel(),
					tune != null ? tune.getInfo().sid1Model : null);
			updateSIDEmu(0, chipModel);

			if (driver.channels == 2) {
				ChipModel stereoChipModel = determineSIDModel(iniCfg
						.emulation().getStereoSidModel(), chipModel,
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
	public static ChipModel determineSIDModel(ChipModel userForcedChoice,
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

		if (s == null) {
			s = sidEmuFactory.lock(player.getC64().getEventScheduler(), model);
		}

		if (s instanceof HardSID) {
			((HardSID) s).write(0x18, (byte) 0x00);
			((HardSID) s).flush();
			s.reset((byte) 0);

			/* Change SID because we are probably switching from 6581 to 8580. */
			sidEmuFactory.unlock(s);
			s = sidEmuFactory.lock(player.getC64().getEventScheduler(), model);
		}

		if (s instanceof ReSID) {
			((ReSID) s).model(model);
			((ReSID) s).filter(iniCfg.filter(ChipModel.MOS6581),
					iniCfg.filter(ChipModel.MOS8580));
		}

		player.getC64().setSID(chipNum, s);
	}

	public void setSIDVolume(int i, float volume) {
		if (sidEmuFactory instanceof ReSIDBuilder) {
			((ReSIDBuilder) sidEmuFactory).setSIDVolume(i, volume);
		}
	}
}
