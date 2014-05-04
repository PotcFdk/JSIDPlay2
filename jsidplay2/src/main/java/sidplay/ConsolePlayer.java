package sidplay;

import hardsid_builder.HardSID;
import hardsid_builder.HardSIDBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PRG2TAP;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import libsidutils.cpuparser.CPUParser;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ChipModel;
import sidplay.audio.AudioConfig;
import sidplay.audio.NaturalFinishedException;
import sidplay.consoleplayer.CmdParser;
import sidplay.consoleplayer.ConsoleIO;
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

public class ConsolePlayer {
	/** Previous song select timeout (< 4 secs) **/
	private static final int SID2_PREV_SONG_TIMEOUT = 4;
	private static final int MAX_SPEED = 32;

	private final IConfig config;
	private final Player player;
	private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(
			State.STOPPED);

	private final Timer timer = new Timer();
	private final Track track = new Track();
	private final DriverSettings driverSettings = new DriverSettings();
	private DriverSettings oldDriverSettings;

	private SidTune tune;
	private String outputFilename;
	private int currentSpeed = 1;
	private int quietLevel;
	private int verboseLevel;

	private Thread fPlayerThread;

	private STIL stil;
	private SidDatabase sidDatabase;
	private SIDBuilder sidBuilder;

	private IExtendImageListener policy;

	public ConsolePlayer(IConfig config) {
		this.config = config;
		player = new Player(config);
	}

	public final Player getPlayer() {
		return player;
	}

	public final ObjectProperty<State> stateProperty() {
		return stateProperty;
	}

	private boolean open() throws InterruptedException {
		if (stateProperty.get() == State.RESTART) {
			stateProperty.set(State.STOPPED);
		}

		// Select the required song
		int songs = 1;
		int currentSong = 1;
		if (tune != null) {
			track.setSelected(tune.selectSong(track.getSelected()));
			if (track.getFirst() == 0) {
				// A different tune is opened?
				// We mark a new play-list start
				track.setFirst(track.getSelected());
			}
			songs = tune.getInfo().songs;
			currentSong = tune.getInfo().currentSong;
			track.setSongs(songs);
		}
		player.setTune(tune);

		CPUClock cpuFreq = CPUClock.getCPUClock(this.config, tune);
		player.setClock(cpuFreq);

		final IAudioSection audio = this.config.getAudio();
		if (oldDriverSettings != null) {
			// restore settings after MP3 has been played last time
			driverSettings.restore(oldDriverSettings);
			oldDriverSettings = null;
		}
		if (tune != null
				&& tune.getInfo().file.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			oldDriverSettings = driverSettings.save();

			driverSettings.setOutput(Output.OUT_COMPARE);
			driverSettings.setEmulation(Emulation.EMU_RESID);
			audio.setPlayOriginal(true);
			audio.setMp3File(tune.getInfo().file.getAbsolutePath());
		}

		driverSettings.configure(this.config, tune, player);

		final AudioConfig audioConfig = AudioConfig.getInstance(
				this.config.getAudio(), driverSettings.getChannels());
		audioConfig.setTuneFilename(tune != null ? tune.getInfo().file : null);
		audioConfig.setSongCount(songs);
		audioConfig.setCurrentSong(currentSong);
		audioConfig.setOutputfilename(outputFilename);
		try {
			driverSettings.getOutput().getDriver().open(audioConfig);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		createSidEmu(driverSettings.getEmulation(), audioConfig,
				cpuFreq.getCpuFrequency());

		// According to the configuration, the SIDs must be updated.
		updateChipModel();
		getPlayer().setFilter(config);

		/* We should have our SIDs configured now. */

		player.handleStereoSIDConflict(driverSettings.getSecondAddress());

		// Start the player. Do this by fast
		// forwarding to the start position
		this.currentSpeed = MAX_SPEED;
		driverSettings.getOutput().getDriver().setFastForward(currentSpeed);

		player.reset();

		if (config.getSidplay2().getUserPlayLength() != 0) {
			// Use user defined fixed song length
			timer.setStop(config.getSidplay2().getUserPlayLength()
					+ timer.getStart());
		} else {
			// Try the song length database
			setStopTime(config.getSidplay2().isEnableDatabase());
		}
		timer.setCurrent(-1);
		stateProperty.set(State.RUNNING);
		return true;
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
	private void createSidEmu(final Emulation emu, AudioConfig audioConfig,
			double cpuFrequency) {
		sidBuilder = null;
		switch (emu) {
		case EMU_RESID:
			sidBuilder = new ReSIDBuilder(audioConfig, cpuFrequency,
					this.config.getAudio().getLeftVolume(), this.config
							.getAudio().getRightVolume());
			break;

		case EMU_HARDSID:
			sidBuilder = new HardSIDBuilder(this.config);
			break;

		default:
			break;
		}
	}

	private void close() {
		if (sidBuilder != null) {
			for (int i = 0; i < C64.MAX_SIDS; i++) {
				SIDEmu s = player.getC64().getSID(i);
				if (s != null) {
					sidBuilder.unlock(s);
					player.getC64().setSID(i, null);
				}
			}
		}
		sidBuilder = null;
		driverSettings.getOutput().getDriver().close();
	}

	public final int getHardSIDCount() {
		return sidBuilder != null ? sidBuilder.getNumDevices() : 0;
	}

	/**
	 * Set stop time according to the song length database (or use default
	 * length)
	 * 
	 * @param enableSongLengthDatabase
	 *            enable song length database
	 */
	public void setStopTime(boolean enableSongLengthDatabase) {
		// play default length or forever (0) ...
		timer.setStop(config.getSidplay2().getDefaultPlayLength());
		if (enableSongLengthDatabase) {
			final int length = getSongLength(tune);
			if (length > 0) {
				// ... or use song length of song length database
				timer.setStop(length);
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
				if (sidBuilder instanceof ReSIDBuilder) {
					((ReSIDBuilder) sidBuilder).setOutput(getOutput()
							.getDriver());
				}
			}

			// Only for tunes: if play time is over loop or exit (single song or
			// whole tune)
			if (tune != null && timer.getStop() != 0
					&& seconds >= timer.getStop()) {
				State endState = config.getSidplay2().isLoop() ? State.RESTART
						: State.EXIT;
				if (config.getSidplay2().isSingle()) {
					stateProperty.set(endState);
				} else {
					nextSong();

					// Check play-list end
					if (track.getSelected() == track.getFirst()) {
						stateProperty.set(endState);
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

	public void pause() {
		if (stateProperty.get() == State.PAUSED) {
			stateProperty.set(State.RUNNING);
		} else {
			stateProperty.set(State.PAUSED);
			driverSettings.getOutput().getDriver().pause();
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

	public void fastForward() {
		this.currentSpeed = currentSpeed * 2;
		if (currentSpeed > MAX_SPEED) {
			this.currentSpeed = MAX_SPEED;
		}
		driverSettings.getOutput().getDriver().setFastForward(currentSpeed);
	}

	public void normalSpeed() {
		this.currentSpeed = 1;
		driverSettings.getOutput().getDriver().setFastForward(1);
	}

	public void quit() {
		stateProperty.set(State.QUIT);
	}

	/**
	 * Configure track according to the tune songs.
	 */
	private void configureTrack(final SidTune tune) {
		// Next time player is used, the track is reset
		// 0 means set first song of play-list, next time open() is called
		track.setFirst(0);
		// 0 means use start song next time open() is called
		track.setSelected(0);

		if (tune == null) {
			track.setFirst(1);
			track.setSongs(0);
		}
	}

	public void setStartTime(long time) {
		timer.setStart(time);
	}

	public void setFirst(Integer first) {
		track.setFirst(first);
	}

	public void setSelected(int selected) {
		track.setSelected(selected);
	}

	public void setOutputFilename(String filename) {
		outputFilename = filename;
	}

	public void setDebug(boolean debug) {
		player.setDebug(CPUParser.getInstance());
	}

	public Output getOutput() {
		return driverSettings.getOutput();
	}

	public void setOutput(Output output) {
		driverSettings.setOutput(output);
	}

	public Emulation getEmulation() {
		return driverSettings.getEmulation();
	}

	public void setEmulation(Emulation emu) {
		driverSettings.setEmulation(emu);
	}

	public void setDisableFilters() {
		config.getEmulation().setFilter(false);
	}

	public int getQuietLevel() {
		return quietLevel;
	}

	public void setQuietLevel(Integer valueOf) {
		quietLevel = valueOf;
	}

	public int getVerboseLevel() {
		return verboseLevel;
	}

	public void setVerboseLevel(Integer valueOf) {
		verboseLevel = valueOf;
	}

	public void setTune(SidTune tune) {
		this.tune = tune;
	}

	public boolean args(String[] args) {
		if (!new CmdParser(config).args(args)) {
			return false;
		}
		configureTrack(tune);
		if (tune == null) {
			return false;
		}

		// Select the desired track
		// and also mark the play-list start
		track.setFirst(tune.selectSong(track.getFirst()));
		track.setSelected(track.getFirst());
		if (config.getSidplay2().isSingle()) {
			track.setSongs(1);
		}
		return true;
	}

	private Consumer<Player> menuHook = (player) -> {
		if (tune != null && tune.getInfo().file != null) {
			System.out.println("Play File: <"
					+ tune.getInfo().file.getAbsolutePath() + ">");
		}
	};

	private Consumer<Player> interactivityHook = (player) -> {
	};

	public SidTune.Speed getSongSpeed(int selected) {
		return tune.getSongSpeed(selected);
	}

	public int getSongs() {
		return track.getSongs();
	}

	public int getFirst() {
		return track.getFirst();
	}

	public int getSelected() {
		return track.getSelected();
	}

	public long getStop() {
		return timer.getStop();
	}

	public void selectFirstTrack() {
		stateProperty.set(State.RESTART);
		track.setSelected(1);
	}

	public void selectLastTrack() {
		stateProperty.set(State.RESTART);
		track.setSelected(track.getSongs());
	}

	public static void main(final String[] args) throws InterruptedException {
		IniConfig config = new IniConfig(true);
		final ConsolePlayer player = new ConsolePlayer(config);
		if (!player.args(args)) {
			System.exit(1);
		}
		if (config.getSidplay2().getUserPlayLength() == 0) {
			String hvscRoot = config.getSidplay2().getHvsc();
			if (hvscRoot != null) {
				File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
				try (FileInputStream input = new FileInputStream(file)) {
					player.setSidDatabase(new SidDatabase(input));
				} catch (IOException e) {
					// silently ignored!
				}
			}
		}
		ConsoleIO consoleIO = new ConsoleIO(player);
		player.menuHook = (obj) -> consoleIO.menu(player.config,
				player.tune.getInfo());

		player.interactivityHook = (obj) -> {
			try {
				if (player.quietLevel < 2
						&& (player.stateProperty().get() == State.PAUSED || System.in
								.available() != 0)) {
					consoleIO.decodeKeys();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		};
		player.startC64();
	}

	/**
	 * Player runnable to play music in the background.
	 */
	private transient final Runnable playerRunnable = new Runnable() {
		@Override
		public void run() {
			// Run until the player gets stopped
			while (true) {
				try {
					// Open tune and play
					if (!open()) {
						return;
					}
					menuHook.accept(player);
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
						interactivityHook.accept(player);
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
			while (fPlayerThread != null && fPlayerThread.isAlive()) {
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
		tune = sidTune;
		// Stop previous run
		stopC64();
		// load tune
		configureTrack(sidTune);
		// track.
		// set command to type after reset
		getPlayer().setCommand(command);
		// Start emulation
		startC64();
	}

	public void insertMedia(File mediaFile, File autostartFile,
			MediaType mediaType) {
		try {
			this.config.getSidplay2().setLastDirectory(
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
		this.config.getC1541().setDriveOn(true);
		getPlayer().enableFloppyDiskDrives(true);
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
		if (!selectedTape.getName().toLowerCase(Locale.ENGLISH)
				.endsWith(".tap")) {
			// Everything, which is not a tape convert to tape first
			final File convertedTape = new File(this.config.getSidplay2()
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

	/**
	 * Set/Update chip model according to the configuration
	 */
	public void updateChipModel() {
		if (sidBuilder != null) {
			ChipModel chipModel = ChipModel.getChipModel(this.config, tune);
			updateChipModel(0, chipModel);

			if (driverSettings.getChannels() == 2) {
				ChipModel stereoChipModel = ChipModel.getStereoSIDModel(
						this.config, tune);
				updateChipModel(1, stereoChipModel);
			}
		}
	}

	/**
	 * Update SID model according to the settings.
	 * 
	 * @param chipNum
	 *            chip number (0 - mono, 1 - stereo)
	 * @param model
	 *            chip model to use
	 */
	private void updateChipModel(final int chipNum, final ChipModel model) {
		SIDEmu s = player.getC64().getSID(chipNum);

		if (s instanceof HardSID) {
			sidBuilder.unlock(s);
			s = null;
		}

		if (s == null) {
			s = sidBuilder.lock(player.getC64().getEventScheduler(), model);
		} else {
			s.setChipModel(model);
		}

		player.getC64().setSID(chipNum, s);
	}

	public void setSIDVolume(int i, float volumeDb) {
		if (sidBuilder != null) {
			sidBuilder.setSIDVolume(i, volumeDb);
		}
	}

}
