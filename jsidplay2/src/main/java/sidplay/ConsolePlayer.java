package sidplay;

import hardsid_builder.HardSID;
import hardsid_builder.HardSIDBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.mos6510.IMOS6510Disassembler;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
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
import sidplay.ini.intf.IEmulationSection;

public class ConsolePlayer {
	/** Previous song select timeout (< 4 secs) **/
	private static final int SID2_PREV_SONG_TIMEOUT = 4;
	private static final int MAX_SPEED = 32;

	private final IConfig config;
	private final Player player = new Player();
	private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(
			State.STOPPED);

	private final Timer timer = new Timer();
	private final Track track = new Track();
	private final DriverSettings driverSettings = new DriverSettings();

	private SidTune tune;
	private String outputFilename;
	private int currentSpeed = 1;
	private int quietLevel;
	private int verboseLevel;

	private Thread fPlayerThread;

	private STIL stil;
	private SidDatabase sidDatabase;
	private SIDBuilder sidBuilder;
	private IMOS6510Disassembler disassembler;

	private IExtendImageListener policy;

	protected String filename;
	private DriverSettings oldDriverSettings;

	public ConsolePlayer(IConfig config) {
		this.config = config;
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

		track.setSingle(this.config.getSidplay2().isSingle());
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

		CPUClock cpuFreq = CPUClock.getCPUClock(this.config, tune);
		player.setClock(cpuFreq);

		final IAudioSection audio = this.config.getAudio();
		if (oldDriverSettings != null) {
			// restore settings after MP3 has been played last time
			driverSettings.restore(oldDriverSettings);
			oldDriverSettings = null;
		}
		if (file != null
				&& file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			oldDriverSettings = driverSettings.save();

			driverSettings.setOutput(Output.OUT_COMPARE);
			driverSettings.setEmulation(Emulation.EMU_RESID);
			audio.setPlayOriginal(true);
			audio.setMp3File(file.getAbsolutePath());
		}

		driverSettings.configure(this.config, tune, player);

		final AudioConfig audioConfig = AudioConfig.getInstance(
				this.config.getAudio(), driverSettings.getChannels());
		audioConfig.setTuneFilename(file);
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

		player.drivesEnabledProperty().set(this.config.getC1541().isDriveOn());
		player.connectC64AndC1541WithParallelCableProperty().set(
				this.config.getC1541().isParallelCable());

		player.reset();

		// Initialize floppies
		for (C1541 floppy : player.getFloppies()) {
			floppy.setFloppyType(this.config.getC1541().getFloppyType());
			floppy.setRamExpansion(0, this.config.getC1541()
					.isRamExpansionEnabled0());
			floppy.setRamExpansion(1, this.config.getC1541()
					.isRamExpansionEnabled1());
			floppy.setRamExpansion(2, this.config.getC1541()
					.isRamExpansionEnabled2());
			floppy.setRamExpansion(3, this.config.getC1541()
					.isRamExpansionEnabled3());
			floppy.setRamExpansion(4, this.config.getC1541()
					.isRamExpansionEnabled4());
		}
		player.turnPrinterOnOff(this.config.getPrinter().isPrinterOn());

		// As yet we don't have a required songlength
		// so try the songlength database
		setSongLengthTimer(this.config.getSidplay2().isEnableDatabase());

		if (timer.isValid()) {
			// Length relative to start
			timer.setStop(timer.getStop() + timer.getStart());
		} else {
			// Check to make start time dosen't exceed end
			if (timer.getStop() > 0 && timer.getStart() >= timer.getStop()) {
				displayError("ERROR: Start time exceeds song length!");
				return false;
			}
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
	 * Enable/disable song length timer according to the song length database.
	 * 
	 * @param enable
	 *            enable song length timer
	 */
	public void setSongLengthTimer(boolean enable) {
		if (enable) {
			final int length = getSongLength(tune);
			if (length >= 0 && !timer.isValid()) {
				// length==0 means forever
				// this is used for tunes
				// of unknown length
				timer.setDefaultLength(length);
			}
		} else {
			timer.setDefaultLength(0);
		}
		// Set up the play timer
		timer.setStop(timer.getDefaultLength());
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
				if (sidBuilder instanceof ReSIDBuilder) {
					((ReSIDBuilder) sidBuilder).setOutput(getOutput()
							.getDriver());
				}
			}

			if (this.config.getSidplay2().isEnableDatabase()
					&& timer.getStop() != 0 && seconds >= timer.getStop()) {
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

	private void displayError(final String error) {
		System.err.println(this + ": " + error);
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

	public void setSingle(boolean single) {
		track.setSingle(single);
	}

	public void setLoop(boolean loop) {
		track.setLoop(loop);
	}

	public void setSongs(int songs) {
		track.setSongs(songs);
	}

	public void setSelected(int selected) {
		track.setSelected(selected);
	}

	public void setDefaultLength(long time) {
		timer.setDefaultLength(time);
		timer.setValid(true);
	}

	public void setEnableDatabase(boolean enable) {
		this.config.getSidplay2().setEnableDatabase(true);
	}

	public void setOutputFilename(String filename) {
		outputFilename = filename;
	}

	public void setDebug(boolean debug) {
		disassembler = CPUParser.getInstance();
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
		final IEmulationSection emulation = this.config.getEmulation();
		emulation.setFilter(false);
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

	public void setForceStereoTune(boolean force) {
		this.config.getEmulation().setForceStereoTune(force);
	}

	public void setFrequency(Integer frequency) {
		this.config.getAudio().setFrequency(frequency);
	}

	public void setUserSidModel(ChipModel chipModel) {
		this.config.getEmulation().setUserSidModel(chipModel);
	}

	public void setUserClockSpeed(CPUClock cpuClock) {
		this.config.getEmulation().setUserClockSpeed(cpuClock);
	}

	public void setDefaultClockSpeed(CPUClock cpuClock) {
		this.config.getEmulation().setDefaultClockSpeed(cpuClock);
	}

	public void setInFile(String infile) {
		filename = infile;
	}

	public int args(String[] args) {
		int rc = new CmdParser(this).args(args);

		if (rc != 1) {
			return rc;
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

		if (filename == null) {
			return -1;
		}
		try {
			try (InputStream stream = new URL(filename).openConnection()
					.getInputStream()) {
				// load from URL
				tune = SidTune.load(stream);
			} catch (MalformedURLException e) {
				// load from file
				tune = SidTune.load(new File(filename));
			}
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
		configureTrack(tune);
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
				timer.setDefaultLength(this.config.getSidplay2()
						.getPlayLength());
				if (driverSettings.getOutput().isFileBased()) {
					timer.setDefaultLength(this.config.getSidplay2()
							.getRecordLength());
				}
			}
		}
		return 0;
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

	public boolean isLoop() {
		return track.isLoop();
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

	public boolean isSingle() {
		return track.isSingle();
	}

	public long getStop() {
		return timer.getStop();
	}

	public boolean isValid() {
		return timer.isValid();
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
		if (player.args(args) < 0) {
			System.exit(1);
		}
		String hvscRoot = config.getSidplay2().getHvsc();
		if (hvscRoot != null) {
			File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
			try (FileInputStream input = new FileInputStream(file)) {
				player.setSidDatabase(new SidDatabase(input));
			} catch (IOException e) {
				// silently ignored!
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
		getPlayer().enableFloppyDiskDrives(true);
		this.config.getC1541().setDriveOn(true);
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
