/**
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package sidplay;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.sidtune.SidTune.RESET;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.player.State.END;
import static sidplay.player.State.OPEN;
import static sidplay.player.State.PAUSE;
import static sidplay.player.State.PLAY;
import static sidplay.player.State.QUIT;
import static sidplay.player.State.RESTART;
import static sidplay.player.State.START;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.sound.sampled.LineUnavailableException;

import builder.exsid.ExSIDBuilder;
import builder.hardsid.HardSIDBuilder;
import builder.netsiddev.NetSIDDevBuilder;
import builder.resid.ReSIDBuilder;
import builder.resid.SIDMixer;
import builder.sidblaster.SIDBlasterBuilder;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.HardwareSIDBuilder;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.common.SIDListener;
import libsidplay.common.Ultimate64Mode;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.mos6510.IMOS6510Extension;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.fingerprinting.IFingerprintMatcher;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.STILEntry;
import sidplay.audio.Audio;
import sidplay.audio.AudioDriver;
import sidplay.audio.MP3Driver.MP3StreamDriver;
import sidplay.audio.VideoDriver;
import sidplay.audio.exceptions.IniConfigException;
import sidplay.audio.exceptions.SongEndException;
import sidplay.ini.IniConfig;
import sidplay.player.ObjectProperty;
import sidplay.player.PlayList;
import sidplay.player.State;
import sidplay.player.Timer;
import sidplay.player.WhatsSidEvent;

/**
 * The player adds some music player capabilities to the HardwareEnsemble.
 *
 * @author Ken Händel
 *
 */
public class Player extends HardwareEnsemble implements VideoDriver, SIDListener, IMOS6510Extension {

	/** Build date calculated from our own modify time */
	public static Calendar LAST_MODIFIED;

	static {
		try {
			URL us = Player.class.getProtectionDomain().getCodeSource().getLocation();
			LAST_MODIFIED = Calendar.getInstance();
			LAST_MODIFIED.setTime(new Date(us.openConnection().getLastModified()));
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Timeout (in ms) for sleeping, if player is paused.
	 */
	private static final int PAUSE_SLEEP_TIME = 250;
	/**
	 * Timeout (in ms) for quitting the player.
	 */
	private static final int QUIT_MAX_WAIT_TIME = 1000;

	/**
	 * Previous song select timeout (&lt; 4 secs).
	 */
	private static final int PREV_SONG_TIMEOUT = 4;
	/**
	 * RAM location for a user typed-in command.
	 */
	private static final int RAM_COMMAND = 0x277;
	/**
	 * RAM location for a user typed-in command length.
	 */
	private static final int RAM_COMMAND_LEN = 0xc6;
	/**
	 * Maximum length for a user typed-in command.
	 */
	private static final int MAX_COMMAND_LEN = 16;
	/**
	 * Auto-start commands.
	 */
	private static final String RUN = "RUN\r", SYS = "SYS%d\r", LOAD = "LOAD\r";

	/**
	 * Music player state.
	 */
	private ObjectProperty<State> stateProperty;
	/**
	 * Play timer.
	 */
	private Timer timer;
	/**
	 * Play list.
	 */
	private PlayList playList;
	/**
	 * Currently played tune.
	 */
	private SidTune tune;
	/**
	 * Auto-start command to be typed-in after reset.
	 */
	private String command;
	/**
	 * Music player thread.
	 */
	private Thread playerThread;
	/**
	 * Called each time a tune starts to play.
	 */
	private Consumer<Player> menuHook = player -> {
	};
	/**
	 * Called each time a chunk of music has been played.
	 */
	private Consumer<Player> interactivityHook = player -> {
	};
	/**
	 * Called after WhatsSID has detected a tune.
	 */
	private Consumer<MusicInfoWithConfidenceBean> whatsSidHook = musicInfoWithConfidence -> {
	};
	/**
	 * Currently used audio and corresponding audio driver.
	 *
	 * <B>Note:</B> If audio driver has been set externally by
	 * {@link Player#setAudioDriver(AudioDriver)}, audio is null!
	 */
	private SimpleImmutableEntry<Audio, AudioDriver> audioAndDriver;
	/**
	 * SID builder being used to create SID chips (real hardware or emulation).
	 */
	private SIDBuilder sidBuilder;
	/**
	 * SID tune information list.
	 */
	private STIL stil;
	/**
	 * Song length database.
	 */
	private SidDatabase sidDatabase;
	/**
	 * Create a base name of a filename to be used for recording.
	 */
	private Function<SidTune, String> recordingFilenameProvider;
	/**
	 * Insert required SIDs. use SID builder to create/destroy SIDs.
	 */
	private BiFunction<Integer, SIDEmu, SIDEmu> requiredSIDs = (sidNum, sidEmu) -> {
		if (SidTune.isSIDUsed(config.getEmulationSection(), tune, sidNum)) {
			return sidBuilder.lock(sidEmu, sidNum, tune);
		} else if (sidEmu != NONE) {
			sidBuilder.unlock(sidEmu);
		}
		return NONE;
	};
	/**
	 * Eject all SIDs.
	 */
	private BiFunction<Integer, SIDEmu, SIDEmu> noSIDs = (sidNum, sidEmu) -> {
		if (sidEmu != NONE) {
			sidBuilder.unlock(sidEmu);
		}
		return NONE;
	};
	/**
	 * Set base address of required SIDs.
	 */
	private IntFunction<Integer> sidLocator = sidNum -> SidTune.getSIDAddress(config.getEmulationSection(), tune,
			sidNum);
	/**
	 * Player paused? Stop audio production.
	 */
	private PropertyChangeListener pauseListener = event -> {
		if (event.getNewValue() == PAUSE) {
			getAudioDriver().pause();
			configureMixer(mixer -> mixer.pause());
			// audio driver continues automatically, next call of write!
		}
	};

	/**
	 * Consumer for VIC screen output as ARGB data
	 */
	private List<VideoDriver> videoDrivers = new CopyOnWriteArrayList<>();

	/**
	 * Consumer for SID register writes
	 */
	private List<SIDListener> sidListeners = new CopyOnWriteArrayList<>();

	/**
	 * Consumer for CPU JMP/JSR instructions
	 */
	private List<IMOS6510Extension> mos6510Extension = new CopyOnWriteArrayList<>();

	/**
	 * Fast forward: skipped VIC frames.
	 */
	private int fastForwardVICFrames;

	/**
	 * WhatsSID?
	 */
	private IFingerprintMatcher fingerPrintMatcher;

	/**
	 * Regularly scheduled event for tune recognition.
	 */
	private WhatsSidEvent whatsSidEvent;

	/**
	 * Create a Music Player.
	 *
	 * @param config configuration
	 */
	public Player(final IConfig config) {
		this(config, MOS6510.class);
	}

	/**
	 * Create a Music Player.
	 *
	 * @param config   configuration
	 * @param cpuClass CPU class implementation
	 */
	public Player(final IConfig config, final Class<? extends MOS6510> cpuClass) {
		super(config, cpuClass);
		this.playList = PlayList.getInstance(config, tune);
		this.timer = new Timer(this) {

			/**
			 * Start time reached? Insert SID chips, connect audio/video driver and start
			 * tune recognition.
			 * 
			 * @see sidplay.player.Timer#start()
			 */
			@Override
			public void start() {
				c64.insertSIDChips(requiredSIDs, sidLocator);
				configureMixer(mixer -> mixer.start());
				if (getAudioDriver() instanceof VideoDriver) {
					addVideoDriver((VideoDriver) getAudioDriver());
				}
				if (getAudioDriver() instanceof SIDListener) {
					addSidListener((SIDListener) getAudioDriver());
				}
				if (getAudioDriver() instanceof IMOS6510Extension) {
					addMOS6510Extension((IMOS6510Extension) getAudioDriver());
				}
				if (sidBuilder instanceof SIDMixer) {
					whatsSidEvent = new WhatsSidEvent(Player.this, ((SIDMixer) sidBuilder).getWhatsSidSupport());
				}
			}

			/**
			 * If a tune ends, there are these possibilities:
			 * <OL>
			 * <LI>Play next song (except singles)
			 * <LI>Play again looping song
			 * <LI>End tune
			 * </OL>
			 *
			 * <B>Note:</B> Our streaming audio drivers must never play infinitely (e.g. MP4
			 * recording)
			 *
			 * @see sidplay.player.Timer#end()
			 */
			@Override
			public void end() {
				if (tune != RESET || getAudio() == null) {
					if (!config.getSidplay2Section().isSingle() && playList.hasNext()) {
						nextSong();
					} else if (config.getSidplay2Section().isLoop()) {
						stateProperty.set(RESTART);
					} else {
						stateProperty.set(END);
					}
				}
			}

			/**
			 * If a tune starts playing, fade-in volume.
			 *
			 * @see sidplay.player.Timer#fadeInStart(int)
			 */
			@Override
			public void fadeInStart(final double fadeIn) {
				if (tune != RESET) {
					configureMixer(mixer -> mixer.fadeIn(fadeIn));
				}
			}

			/**
			 * If a tune is short before stop time, fade-out volume.
			 *
			 * @see sidplay.player.Timer#fadeOutStart(int)
			 */
			@Override
			public void fadeOutStart(final double fadeOut) {
				if (tune != RESET) {
					configureMixer(mixer -> mixer.fadeOut(fadeOut));
				}
			}

		};
		initializeTmpDir(config);
		stateProperty = new ObjectProperty<>(State.class.getSimpleName(), QUIT);
		audioAndDriver = new SimpleImmutableEntry<>(DEFAULT_AUDIO, DEFAULT_AUDIO.getAudioDriver());
		recordingFilenameProvider = tune -> new File(config.getSidplay2Section().getTmpDir(), "jsidplay2")
				.getAbsolutePath();
	}

	/**
	 * Create temporary directory, if it does not exist.<BR>
	 * E.g. Recordings and converted tapes are saved here!
	 */
	public static void initializeTmpDir(IConfig config) {
		File tmpDir = config.getSidplay2Section().getTmpDir();
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	/**
	 * Get a fingerprint matcher.
	 *
	 * @return fingerprint matcher
	 */
	public IFingerprintMatcher getFingerPrintMatcher() {
		return fingerPrintMatcher;
	}

	/**
	 * Set a fingerprint matcher.
	 *
	 * @param fingerPrintMatcher a fingerprint matcher
	 */
	public void setFingerPrintMatcher(IFingerprintMatcher fingerPrintMatcher) {
		this.fingerPrintMatcher = fingerPrintMatcher;
	}

	/**
	 * Call to update SID chips each time SID configuration has been changed
	 * thread-safe.
	 */
	public final void updateSIDChipConfiguration() {
		executeInPlayerThread("Update SID Chip Configuration", () -> c64.insertSIDChips(requiredSIDs, sidLocator));
	}

	/**
	 * Call to configure VIC chips thread-safe.
	 *
	 * @param action VIC configuration action
	 */
	public final void configureVICs(Consumer<VIC> action) {
		executeInPlayerThread("Configure VICs", () -> c64.configureVICs(action));
	}

	/**
	 * Configure all available SIDs thread-safe.
	 *
	 * @param action SID chip consumer
	 */
	public final void configureSIDs(BiConsumer<Integer, SIDEmu> action) {
		executeInPlayerThread("Configure SIDs", () -> c64.configureSIDs(action));
	}

	/**
	 * Configure one specific SID thread-safe.
	 *
	 * @param chipNum SID chip number
	 * @param action  SID chip consumer
	 */
	public final void configureSID(int chipNum, Consumer<SIDEmu> action) {
		executeInPlayerThread("Configure SID", () -> c64.configureSID(chipNum, action));
	}

	/**
	 * Configure the mixer, optionally implemented by SID builder thread-safe.
	 *
	 * @param action mixer consumer
	 */
	public final void configureMixer(final Consumer<Mixer> action) {
		executeInPlayerThread("Configure Mixer", () -> {
			if (sidBuilder instanceof Mixer) {
				action.accept((Mixer) sidBuilder);
			}
		});
	}

	/**
	 * The runnable is executed immediately in player thread or scheduled
	 * thread-safe.
	 *
	 * @param eventName event name for scheduling
	 * @param runnable  runnable to execute in player thread
	 */
	private void executeInPlayerThread(String eventName, Runnable runnable) {
		if (Thread.currentThread().equals(playerThread)) {
			runnable.run();
		} else {
			c64.getEventScheduler().scheduleThreadSafe(new Event(eventName) {
				@Override
				public void event() throws InterruptedException {
					runnable.run();
				}
			});
		}
	}

	/**
	 * Power-on C64 system.
	 */
	@Override
	protected final void reset() {
		super.reset();
		timer.reset();
		if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF && tune == RESET) {
			sendReset(config, tune);
		}
		c64.getEventScheduler().schedule(new Event("Auto-start") {
			@Override
			public void event() throws InterruptedException {
				if (tune != RESET) {
					// for tunes: Install player into RAM
					Integer driverAddress = tune.placeProgramInMemory(c64.getRAM());
					if (driverAddress != null) {
						if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF) {
							sendRamAndSys(config, tune, c64.getRAM(), driverAddress);
						}
						if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.STANDALONE) {
							// Set play address to feedback call frames counter.
							c64.setPlayAddr(tune.getInfo().getPlayAddr());
							// Start SID player driver
							c64.getCPU().forcedJump(driverAddress);
						}
					} else {
						// No player: Start basic program or assembler code
						final int loadAddr = tune.getInfo().getLoadAddr();
						if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF) {
							if (loadAddr == 0x0801) {
								sendRamAndRun(config, tune, c64.getRAM());
							} else {
								sendRamAndSys(config, tune, c64.getRAM(), loadAddr);
							}
						}
						command = loadAddr == 0x0801 ? RUN : String.format(SYS, loadAddr);
					}
				}
				if (command != null) {
					if (command.startsWith(LOAD)) {
						// Load from tape needs someone to press play
						datasette.control(Control.START);
					}
					// Enter basic command
					if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.STANDALONE) {
						typeInCommand(command);
					}
					if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF && tune == RESET) {
						sendWait(config, 300);
						sendCommand(config, command);
					}
				}
			}
		}, SidTune.getInitDelay(tune));
	}

	/**
	 * Simulate a user typed-in command.
	 *
	 * @param command command to type-in
	 */
	public final void typeInCommand(final String command) {
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		System.arraycopy(command.getBytes(US_ASCII), 0, c64.getRAM(), RAM_COMMAND, length);
		c64.getRAM()[RAM_COMMAND_LEN] = (byte) length;
	}

	/**
	 * Enter basic command after reset.
	 *
	 * @param command basic command after reset
	 */
	private void setCommand(final String command) {
		this.command = command;
	}

	/**
	 * What is the current playing time in secs.
	 *
	 * @return the current playing time in secs
	 */
	public final double time() {
		final EventScheduler c = c64.getEventScheduler();
		return c.getTime(Phase.PHI2) / c.getCyclesPerSecond();
	}

	/**
	 * Get current play-list.
	 *
	 * @return current tune-based play list
	 */
	public final PlayList getPlayList() {
		return playList;
	}

	/**
	 * Get current timer.
	 *
	 * @return song length timer
	 */
	public final Timer getTimer() {
		return timer;
	}

	/**
	 * Get the currently played tune.
	 *
	 * @return the currently played tune
	 */
	public final SidTune getTune() {
		return tune;
	}

	/**
	 * Set a tune to play.
	 *
	 * @param tune tune to play
	 */
	public final void setTune(final SidTune tune) {
		this.tune = tune;
	}

	/**
	 * Start player thread.
	 */
	public synchronized final void startC64() {
		if (playerThread == null || !playerThread.isAlive()) {
			playerThread = new Thread(playerRunnable, "Player");
			playerThread.setPriority(Thread.MAX_PRIORITY);
			playerThread.start();
		}
	}

	/**
	 * Stop player thread.
	 */
	public final void stopC64() {
		stopC64(true);
	}

	/**
	 * Stop or wait for player thread.
	 *
	 * @param quitOrWait quit player (true) or wait for termination, only (false)
	 */
	public synchronized final void stopC64(final boolean quitOrWait) {
		try {
			while (playerThread != null && playerThread.isAlive()) {
				if (quitOrWait) {
					quit();
				}
				playerThread.join(QUIT_MAX_WAIT_TIME);
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Set a hook to be called when the player has opened a tune.
	 *
	 * @param menuHook menu hook
	 */
	public final void setMenuHook(final Consumer<Player> menuHook) {
		this.menuHook = menuHook;
	}

	/**
	 * Set a hook to be called when the player has played a chunk.
	 *
	 * @param interactivityHook
	 */
	public final void setInteractivityHook(final Consumer<Player> interactivityHook) {
		this.interactivityHook = interactivityHook;
	}

	/**
	 * Get a hook to be called when WhatsSid has detected a tune.
	 * 
	 * @return hook to be called when WhatsSid has detected a tune
	 */
	public Consumer<MusicInfoWithConfidenceBean> getWhatsSidHook() {
		return whatsSidHook;
	}

	/**
	 * Set a hook to be called when WhatsSid has detected a tune.
	 *
	 * @param whatsSidHook
	 */
	public void setWhatsSidHook(Consumer<MusicInfoWithConfidenceBean> whatsSidHook) {
		this.whatsSidHook = whatsSidHook;
	}

	/**
	 * Get the player's state,
	 *
	 * @return the player's state
	 */
	public final ObjectProperty<State> stateProperty() {
		return stateProperty;
	}

	/**
	 * Player runnable to play music in a background thread.
	 */
	private Runnable playerRunnable = () -> {
		// Run until the player gets stopped
		do {
			try {
				stateProperty.set(OPEN);
				open();
				stateProperty.set(START);
				menuHook.accept(Player.this);
				stateProperty.set(PLAY);
				// Play next chunk of sound data
				while (play()) {
					interactivityHook.accept(Player.this);
				}
			} catch (SongEndException e) {
				timer.end();
			} catch (IniConfigException e) {
				System.err.println(e.getMessage());
				stateProperty.set(RESTART);
				e.getConfigRepairer().run();
			} catch (InterruptedException | IOException | LineUnavailableException | RuntimeException
					| UnsatisfiedLinkError e) {
				stateProperty.set(QUIT);
				throw new RuntimeException(e.getMessage(), e);
			} finally {
				close();
			}
			// "Play it once, Sam. For old times' sake."
		} while (stateProperty.get() == RESTART);
	};

	/**
	 * Open player, that means basically: Reset C64 and start playing the tune.
	 *
	 * <B>Note:</B> Audio driver different to {@link Audio} members are on hold!
	 *
	 * @throws LineUnavailableException audio line currently in use
	 * @throws IOException              audio output file cannot be written
	 * @throws InterruptedException
	 */
	private void open() throws IOException, LineUnavailableException, InterruptedException {
		final ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		final IEmulationSection emulationSection = config.getEmulationSection();
		final IAudioSection audioSection = config.getAudioSection();

		emulationSection.getOverrideSection().reset();

		playList = PlayList.getInstance(config, tune);
		timer.setStart(sidplay2Section.getStartTime());

		// PAL/NTSC
		setClock(CPUClock.getCPUClock(emulationSection, tune));

		// Audio configuration, if audio driver has not been set by setAudioDriver()!
		if (getAudio() != null) {
			setAudioAndDriver(audioSection.getAudio(), audioSection.getAudio().getAudioDriver(audioSection, tune));
			if (getAudioDriver().isRecording() && sidplay2Section.isLoop()) {
				sidplay2Section.setLoop(false);
				System.out.println("Warning: Loop has been disabled during recording!");
			}
		}
		verifyConfiguration(sidplay2Section);
		// open audio driver
		getAudioDriver().open(audioSection, getRecordingFilename(), c64.getClock(), c64.getEventScheduler());

		sidBuilder = createSIDBuilder(c64.getClock());

		configureMixer(mixer -> mixer.setAudioDriver(getAudioDriver()));
		configureVICs(vic -> vic.setVideoDriver(this));
		c64.setSIDListener(this);
		c64.setPlayRoutineObserver(this);

		fastForwardVICFrames = 0;

		stateProperty.addListener(pauseListener);

		reset();
	}

	/**
	 * Create configured SID chip implementation (software/hardware).
	 *
	 * @param cpuClock CPU clock frequency
	 * @return SID builder
	 */
	private SIDBuilder createSIDBuilder(final CPUClock cpuClock) {
		switch (Engine.getEngine(config.getEmulationSection(), tune)) {
		case EMULATION:
			return new ReSIDBuilder(c64.getEventScheduler(), config, cpuClock);
		case NETSID:
			return new NetSIDDevBuilder(c64.getEventScheduler(), config, cpuClock);
		case HARDSID:
			return new HardSIDBuilder(c64.getEventScheduler(), config, cpuClock);
		case SIDBLASTER:
			return new SIDBlasterBuilder(c64.getEventScheduler(), config, cpuClock);
		case EXSID:
			return new ExSIDBuilder(c64.getEventScheduler(), config, cpuClock);
		default:
			throw new RuntimeException("Unknown engine type: " + config.getEmulationSection().getEngine());
		}
	}

	/**
	 * Check the configuration.
	 *
	 * @throws IOException configuration error
	 */
	private void verifyConfiguration(ISidPlay2Section sidplaySection) throws IOException {
		if (getAudioDriver().isRecording() && sidplaySection.getDefaultPlayLength() <= 0
				&& getSidDatabaseInfo(db -> db.getSongLength(tune), 0.) == 0) {
			throw new IniConfigException("Unknown song length in record mode, use default",
					() -> sidplaySection.setDefaultPlayLength(180));
		}
		if (getAudio() == Audio.LIVE_SID_DUMP && (tune == RESET || tune.getInfo().getPlayAddr() == 0)) {
			throw new RuntimeException("SIDDump audio driver requires a well-known player address of the tune");
		}
	}

	/**
	 * Get Audio of audio driver or null for custom audio drivers.
	 * 
	 * @return audio audio
	 */
	private Audio getAudio() {
		return audioAndDriver.getKey();
	}

	/**
	 * Get currently used audio driver.
	 *
	 * @return currently used audio driver
	 */
	public AudioDriver getAudioDriver() {
		return this.audioAndDriver.getValue();
	}

	/**
	 * Set alternative audio driver (not contained in {@link Audio}).<BR>
	 * For example, If it is required to use a new instance of audio driver each
	 * time the player plays a tune (e.g. {@link MP3StreamDriver})
	 *
	 * @param audioDriver for example {@link MP3StreamDriver}
	 * @throws IOException configuration error
	 */
	public final void setAudioDriver(final AudioDriver audioDriver) throws IOException {
		setAudioAndDriver(null, audioDriver);
	}

	/**
	 * Set audio for play-back
	 *
	 * @param audio audio for play-back
	 * @throws IOException configuration error
	 */
	private void setAudioAndDriver(final Audio audio, final AudioDriver audioDriver) throws IOException {
		this.audioAndDriver = new SimpleImmutableEntry<>(audio, audioDriver);
	}

	/**
	 * Play routine (clock chips until audio buffer is filled completely or player
	 * gets paused).
	 *
	 * @return continue to play next time?
	 *
	 * @throws InterruptedException audio production interrupted
	 */
	private boolean play() throws InterruptedException {
		for (int i = 0; i < config.getAudioSection().getBufferSize(); i++) {
			if (stateProperty.get() == PLAY) {
				c64.getEventScheduler().clock();
			}
		}
		if (stateProperty.get() == PAUSE) {
			c64.getEventScheduler().clockThreadSafeEvents();
			Thread.sleep(PAUSE_SLEEP_TIME);
		}
		return stateProperty.get() == PLAY || stateProperty.get() == PAUSE;
	}

	/**
	 * Close player, that means basically plugging out SID chips and close audio
	 * driver.
	 */
	private void close() {
		try {
			stateProperty.removeListener(pauseListener);
			c64.insertSIDChips(noSIDs, sidLocator);
			if (getAudioDriver() instanceof VideoDriver) {
				removeVideoDriver((VideoDriver) getAudioDriver());
			}
			if (getAudioDriver() instanceof SIDListener) {
				removeSidListener((SIDListener) getAudioDriver());
			}
			if (getAudioDriver() instanceof IMOS6510Extension) {
				removeMOS6510Extension((IMOS6510Extension) getAudioDriver());
			}
			// save still unwritten sound data
			if ((getAudioDriver().isRecording() || stateProperty.get() != QUIT) && getAudioDriver().buffer() != null) {
				getAudioDriver().write();
			}
		} catch (Throwable e) {
			// ignore exceptions near close
		} finally {
			if (whatsSidEvent != null) {
				whatsSidEvent.setAbort(true);
			}
			getAudioDriver().close();
		}
	}

	/**
	 * Play tune.
	 *
	 * @param tune tune to play (RESET means just reset C64)
	 */
	public final void play(final SidTune tune) {
		play(tune, null);
	}

	/**
	 * Reset C64 and enter basic command.
	 *
	 * @param command basic command to be entered after a normal reset
	 */
	public final void resetC64(String command) {
		play(RESET, command);
	}

	/**
	 * Turn C64 off and on, load a tune and enter basic command.
	 *
	 * @param tune    tune to play (RESET means just reset C64)
	 * @param command basic command to be entered after a normal reset
	 */
	private void play(final SidTune tune, final String command) {
		// prevent (re)starting a tune during IniConfigException
		if (stateProperty().get() != OPEN) {
			stopC64();
			setTune(tune);
			setCommand(command);
			startC64();
		}
	}

	/**
	 * Pause or continue the player.
	 */
	public final void pauseContinue() {
		if (stateProperty.get() == QUIT || stateProperty.get() == END) {
			play(tune);
		} else {
			executeInPlayerThread("PauseContinue", () -> {
				if (stateProperty.get() == PAUSE) {
					stateProperty.set(PLAY);
				} else {
					stateProperty.set(PAUSE);
				}
			});
		}
	}

	/**
	 * Play next song.
	 */
	public final void nextSong() {
		executeInPlayerThread("Play next song", () -> {
			playList.next();
			stateProperty.set(RESTART);
		});
	}

	/**
	 * Play previous song.<BR>
	 * <B>Note:</B> After {@link #PREV_SONG_TIMEOUT} has been reached, the current
	 * tune is restarted instead.
	 */
	public final void previousSong() {
		executeInPlayerThread("Play previous song", () -> {
			if (time() < PREV_SONG_TIMEOUT) {
				playList.previous();
			}
			stateProperty.set(RESTART);
		});
	}

	/**
	 * Play first song.
	 */
	public final void firstSong() {
		executeInPlayerThread("Play first song", () -> {
			playList.first();
			stateProperty.set(RESTART);
		});
	}

	/**
	 * Play last song.
	 */
	public final void lastSong() {
		executeInPlayerThread("Play last song", () -> {
			playList.last();
			stateProperty.set(RESTART);
		});
	}

	/**
	 * Get mixer info.
	 *
	 * @param function     mixer function to apply
	 * @param defaultValue default value, if SIDBuilder does not implement a mixer
	 * @return mixer info
	 */
	public final <T> T getMixerInfo(final Function<Mixer, T> function, final T defaultValue) {
		return sidBuilder instanceof Mixer ? function.apply((Mixer) sidBuilder) : defaultValue;
	}

	/**
	 * Get hardware SID builder info.
	 *
	 * @param function     hardware SID builder function to apply
	 * @param defaultValue default value, if SIDBuilder does not implement a
	 *                     hardware SID builder
	 * @return hardware SID builder info
	 */
	public final <T> T getHardwareSIDBuilderInfo(final Function<HardwareSIDBuilder, T> function, final T defaultValue) {
		return sidBuilder instanceof HardwareSIDBuilder ? function.apply((HardwareSIDBuilder) sidBuilder)
				: defaultValue;
	}

	/**
	 * Quit player.
	 */
	public final void quit() {
		executeInPlayerThread("Quit", () -> stateProperty.set(QUIT));
		if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF) {
			sendReset(config, tune);
		}
	}

	/**
	 * Set song length database.
	 *
	 * @param sidDatabase song length database
	 */
	public final void setSidDatabase(final SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	/**
	 * Get song length database info.
	 *
	 * @param function     SidDatabase function to apply
	 * @param              <T> SidDatabase return type
	 * @param defaultValue default value, if database is not set
	 * @return song length database info
	 */
	public final <T> T getSidDatabaseInfo(final Function<SidDatabase, T> function, final T defaultValue) {
		return sidDatabase != null ? function.apply(sidDatabase) : defaultValue;
	}

	/**
	 * Set SID Tune Information List (STIL).
	 *
	 * @param stil SID Tune Information List
	 */
	public final void setSTIL(final STIL stil) {
		this.stil = stil;
	}

	/**
	 * Get SID Tune Information List info.
	 *
	 * @param collectionName entry path to get infos for
	 * @return SID Tune Information List info
	 */
	public final STILEntry getStilEntry(final String collectionName) {
		return stil != null && collectionName != null ? stil.getSTILEntry(collectionName) : null;
	}

	/**
	 * Get recording filename, add audio related file extension (if known).
	 *
	 * @return recording filename
	 */
	public String getRecordingFilename() {
		AudioDriver audioDriver = getAudioDriver();
		if (audioDriver.getExtension() != null) {
			return recordingFilenameProvider.apply(tune) + audioDriver.getExtension();
		} else {
			return recordingFilenameProvider.apply(tune);
		}
	}

	/**
	 * Set provider of recording filenames.
	 *
	 * @param recordingFilenameProvider provider of recording filenames
	 */
	public final void setRecordingFilenameProvider(final Function<SidTune, String> recordingFilenameProvider) {
		this.recordingFilenameProvider = recordingFilenameProvider;
	}

	/**
	 * Add consumer of VIC screen output as ARGB data
	 *
	 * @param consumer consumer of C64 screen pixels as ARGB data
	 */
	public void addVideoDriver(VideoDriver consumer) {
		videoDrivers.add(consumer);
	}

	/**
	 * Remove consumer of VIC screen output as ARGB data
	 *
	 * @param consumer consumer of C64 screen pixels as ARGB data
	 */
	public void removeVideoDriver(VideoDriver consumer) {
		videoDrivers.remove(consumer);
	}

	/**
	 * Add consumer of SID register writes
	 *
	 * @param consumer consumer of SID register writes
	 */
	public void addSidListener(SIDListener consumer) {
		sidListeners.add(consumer);
	}

	/**
	 * Remove consumer of SID register writes
	 *
	 * @param consumer consumer of SID register writes
	 */
	public void removeSidListener(SIDListener consumer) {
		sidListeners.remove(consumer);
	}

	/**
	 * Add consumer of MOS6510 JMP/JSR instructions.
	 *
	 * @param mos6510Extension consumer of MOS6510 JMP/JSR instructions
	 */
	public void addMOS6510Extension(IMOS6510Extension mos6510Extension) {
		this.mos6510Extension.add(mos6510Extension);
	}

	/**
	 * Remove consumer of MOS6510 JMP/JSR instructions.
	 *
	 * @param mos6510Extension of MOS6510 JMP/JSR instructions
	 */
	public void removeMOS6510Extension(IMOS6510Extension mos6510Extension) {
		this.mos6510Extension.remove(mos6510Extension);
	}

	/**
	 * Fast forward skips frames and produces output for each Xth frame (X = 1x, 2x,
	 * 4x, ... , 32x).
	 */
	@Override
	public void accept(VIC vic) {
		// skip frame(s) on fast forward
		int fastForwardBitMask = getMixerInfo(m -> m.getFastForwardBitMask(), 0);
		if ((fastForwardVICFrames++ & fastForwardBitMask) == fastForwardBitMask) {
			Iterator<VideoDriver> iterator = videoDrivers.iterator();
			while (iterator.hasNext()) {
				iterator.next().accept(vic);
			}
		}
	}

	@Override
	public void write(int addr, byte data) {
		Iterator<SIDListener> iterator = sidListeners.iterator();
		while (iterator.hasNext()) {
			iterator.next().write(addr, data);
		}
	}

	@Override
	public void jmpJsr() {
		Iterator<IMOS6510Extension> iterator = mos6510Extension.iterator();
		while (iterator.hasNext()) {
			iterator.next().jmpJsr();
		}
	}

	/**
	 * The credits for the authors of many parts of this emulator.
	 *
	 * @param version containing version of JSIDPlay2
	 * @return the credits
	 */
	public final String getCredits(final String version) {
		final StringBuffer credits = new StringBuffer();
		credits.append("Operating System:\n");
		credits.append(System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")\n");
		credits.append("\nJava version:\n");
		credits.append(System.getProperty("java.runtime.version"));
		credits.append("\n" + System.getProperty("sun.arch.data.model") + " bits");
		credits.append("\n\nJava Version and User Interface v");
		credits.append(version);
		credits.append(":\n");
		credits.append("\tCopyright (©) 2007-" + LAST_MODIFIED.get(Calendar.YEAR) + " Ken Händel\n");
		credits.append("\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Icon used from Wikimedia\n");
		credits.append("\tCreative Commons License Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)\n");
		credits.append("\thttps://creativecommons.org/licenses/by-sa/3.0/legalcode\n");
		credits.append("Distortion Simulation and development: Antti S. Lankila\n");
		credits.append("\thttp://bel.fi/~alankila/c64-sw/\n");
		credits.append("Network SID Device:\n");
		credits.append("\tCopyright (©) 2011 Antti S. Lankila <alankila@bel.fi>\n");
		credits.append("\tSupported by Wilfred Bos, The Netherlands\n");
		credits.append("\thttp://www.acid64.com\n");
		credits.append("Network SID Client:\n");
		credits.append("\tCopyright (©) 2017 Ken Händel\n");
		credits.append("\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Testing and Feedback: Nata, founder of proNoise\n");
		credits.append("\thttp://www.nata.netau.net/\n");
		credits.append("Music recognition system\n");
		credits.append("\tCopyright (©) 2019 J. Pery <hsyecheng@hotmail.com>\n");
		credits.append("\thttps://github.com/JPery/Audio-Fingerprinting\n");
		credits.append("graphical output:\n" + "\tCopyright (©) 2007 Joakim Eriksson\n");
		credits.append("\tCopyright (©) 2009, 2010 Antti S. Lankila\n");
		credits.append("MP3 encoder/decoder (jump3r), based on Lame\n");
		credits.append("\tCopyright (©) 2010-2011 Ken Händel\n");
		credits.append("\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Assembly64:\n");
		credits.append("\tCopyright (©) " + LAST_MODIFIED.get(Calendar.YEAR) + " Fredrik Åberg\n");
		credits.append("\thttp://hackerswithstyle.ddns.net/assembly/\n");
		credits.append("This product uses the database of Game Base 64 (GB64)\n");
		credits.append("\thttp://www.gb64.com/\n");
		credits.append("Command Line Parser (JCommander):\n");
		credits.append("\tCopyright (©) 2010-2014 Cédric Beust\n");
		credits.append("\thttp://jcommander.org/\n");
		credits.append("Audio/Video Encoder (Xuggler):\n");
		credits.append("\tCopyright (©) 2011 and All Rights Reserved by ConnectSolutions, LLC.\n");
		credits.append("\thttp://www.xuggle.com/xuggler/\n");
		credits.append("MP3 downloads from Stone Oakvalley's Authentic SID MusicCollection (SOASC=):\n");
		credits.append("\thttp://www.6581-8580.com/\n");
		credits.append("6510 cross assembler (Kickassembler V5.15):\n");
		credits.append("\tCopyright (©) 2006-" + LAST_MODIFIED.get(Calendar.YEAR) + " Mads Nielsen\n");
		credits.append("\thttp://www.theweb.dk/KickAssembler/\n");
		credits.append("PSID to PRG converter (PSID64 v0.9):\n");
		credits.append("\tCopyright (©) 2001-2007 Roland Hermans\n");
		credits.append("\thttp://sourceforge.net/projects/psid64/\n");
		credits.append("An Optimizing Hybrid LZ77 RLE Data Compression Program (Pucrunch 22.11.2008):\n");
		credits.append("\tCopyright (©) 1997-2008 Pasi 'Albert' Ojala\n");
		credits.append("\thttp://www.cs.tut.fi/~albert/Dev/pucrunch/\n");
		credits.append("SID dump file (SIDDump V1.04):\n");
		credits.append("\tCopyright (©) 2007 Lasse Öörni\n");
		credits.append("HVSC playroutine identity scanner (SIDId V1.07):\n");
		credits.append("\tCopyright (©) 2007 Lasse Öörni\n");
		credits.append("High Voltage Music Engine MusicCollection (HVMEC V1.0):\n");
		credits.append("\tCopyright (©) 2011 by Stefano Tognon and Stephan Parth\n");
		credits.append("C1541 Floppy Disk Drive Emulation:\n");
		credits.append("\tCopyright (©) 2010 VICE (the Versatile Commodore Emulator)\n");
		credits.append("\thttp://www.viceteam.org/\n");
		credits.append("Based on libsidplay v2.1.1 and ReSID v0.0.2 engine:\n");
		credits.append("\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n");
		credits.append("\thttp://sidplay2.sourceforge.net\n");
		credits.append(MOS6510.credits());
		credits.append(MOS6526.credits());
		credits.append(VIC.credits());
		credits.append(builder.resid.resid.ReSID.credits());
		credits.append(builder.resid.residfp.ReSIDfp.credits());
		credits.append(builder.exsid.ExSIDEmu.credits());
		credits.append(builder.sidblaster.SIDBlasterEmu.credits());
		credits.append(builder.hardsid.HardSIDEmu.credits());
		return credits.toString();
	}

	/**
	 * Test main: Play a tune.
	 *
	 * @param args the filename of the tune is the first arg
	 * @throws SidTuneError SID tune error
	 * @throws IOException  tune file cannot be read
	 */
	public static void main(final String[] args) throws IOException, SidTuneError {
		if (args.length < 1) {
			System.err.println("Missing argument: <filename>");
			System.exit(-1);
		}
		final SidTune tune = SidTune.load(new File(args[0]));
		final Player player = new Player(new IniConfig());
		player.play(tune);
	}

}
