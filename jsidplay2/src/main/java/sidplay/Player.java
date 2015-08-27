/**
 *                                 Main Library Code
 *                                SIDs Mixer Routines
 *                             Library Configuration Code
 *                    xa65 - 6502 cross assembler and utility suite
 *                          reloc65 - relocates 'o65' files
 *        Copyright (C) 1997 André Fachat (a.fachat@physik.tu-chemnitz.de)
 *        ----------------------------------------------------------------
 *  begin                : Fri Jun 9 2000
 *  copyright            : (C) 2000 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package sidplay;

import hardsid_builder.HardSIDBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.common.SIDListener;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.components.pla.PLA;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;
import libsidutils.SidDatabase;
import resid_builder.ReSIDBuilder;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.CmpMP3File;
import sidplay.audio.MP3Driver.MP3Stream;
import sidplay.ini.IniConfig;
import sidplay.player.PlayList;
import sidplay.player.State;
import sidplay.player.Timer;

/**
 * The player adds some music player capabilities to the HardwareEnsemble.
 * 
 * @author Ken Händel
 * 
 */
public class Player extends HardwareEnsemble {

	/**
	 * Delay in cycles, for normal RESET code path, before autostart commands
	 * are executed (~2.5 seconds).
	 */
	private static final int RESET_INIT_DELAY = 2500000;
	/**
	 * Timeout (in ms) for sleeping if player is paused.
	 */
	private static final int PAUSE_SLEEP_TIME = 250;
	/**
	 * Previous song select timeout (< 4 secs).
	 */
	private static final int PREV_SONG_TIMEOUT = 4;
	/**
	 * RAM location for a user typed-in command.
	 */
	private static final int RAM_COMAND = 0x277;
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
	private static final String RUN = "RUN\r", SYS = "SYS%d\r",
			LOAD = "LOAD\r";

	/**
	 * Music player state.
	 */
	private ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(
			State.STOPPED);
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
	private SidTune tune = SidTune.RESET;
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
	 * Audio driver.
	 */
	private AudioDriver audioDriver, oldAudioDriver;

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
	 * Create a filename to be used for recording.
	 */
	private Function<SidTune, String> recordingFilenameProvider = tune -> "jsidplay2";

	/**
	 * Create a Music Player.
	 */
	public Player(IConfig config) {
		super(config);
		this.playList = PlayList.getInstance(config, SidTune.RESET);
		this.timer = new Timer(this) {

			@Override
			public void start() {
				configureMixer(mixer -> mixer.start());
			}

			/**
			 * If a tune ends:
			 * <UL>
			 * <LI>Single tune or end of play list? Stop player (except loop)
			 * <LI>Else play next song
			 * </UL>
			 * 
			 * @see sidplay.player.Timer#end()
			 */
			@Override
			public void end() {
				if (tune != SidTune.RESET) {
					if (config.getSidplay2Section().isSingle()
							|| !playList.hasNext()) {
						stateProperty.set(getEndState());
					} else {
						nextSong();
					}
				}
			}

			/**
			 * If a tune starts playing, fade-in volume.
			 * 
			 * @see sidplay.player.Timer#fadeInStart(int)
			 */
			@Override
			public void fadeInStart(int fadeIn) {
				if (tune != SidTune.RESET) {
					configureMixer(mixer -> mixer.fadeIn(fadeIn));
				}
			}

			/**
			 * If a tune is short before stop time, fade-out volume.
			 * 
			 * @see sidplay.player.Timer#fadeOutStart(int)
			 */
			@Override
			public void fadeOutStart(int fadeOut) {
				if (tune != SidTune.RESET) {
					configureMixer(mixer -> mixer.fadeOut(fadeOut));
				}
			}

		};
		initializeTmpDir();
	}

	/**
	 * Create temporary directory, if it does not exist.<BR>
	 * E.g. Recordings, converted tapes and HardSID libraries are saved here!
	 */
	private void initializeTmpDir() {
		File tmpDir = new File(config.getSidplay2Section().getTmpDir());
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	/**
	 * Power-on C64 system.
	 */
	protected void reset() {
		super.reset();
		timer.reset();
		configureMixer(mixer -> mixer.reset());

		// According to the configuration, the SIDs must be updated.
		createOrUpdateSIDs();

		if (tune == SidTune.RESET) {
			// Normal reset code path executing auto-start command
			c64.getEventScheduler().schedule(new Event("Auto-start event") {
				@Override
				public void event() throws InterruptedException {
					if (command != null) {
						if (command.startsWith(LOAD)) {
							// Auto-start tape needs someone to press play
							datasette.control(Control.START);
						}
						typeInCommand(command);
						command = null;
					}
				}
			}, RESET_INIT_DELAY);
		} else {
			// reset code path for tunes
			c64.getEventScheduler().schedule(new Event("Tune init event") {
				@Override
				public void event() throws InterruptedException {
					int driverAddress = tune.placeProgramInMemory(c64.getRAM());
					if (driverAddress != -1) {
						// Start SID player driver, if available
						c64.getCPU().forcedJump(driverAddress);
					} else {
						// Start basic program or machine code routine
						final int loadAddr = tune.getInfo().getLoadAddr();
						command = loadAddr == 0x0801 ? RUN : String.format(SYS,
								loadAddr);
						typeInCommand(command);
						command = null;
					}
				}
			}, tune.getInitDelay());
			// Set play-back address to feedback call frames counter.
			c64.setPlayAddr(tune.getInfo().getPlayAddr());
		}
	}

	/**
	 * Simulate a user typed-in command.
	 */
	public void typeInCommand(String command) {
		byte[] ram = c64.getRAM();
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		for (int charNum = 0; charNum < length; charNum++) {
			ram[RAM_COMAND + charNum] = (byte) command.charAt(charNum);
		}
		ram[RAM_COMMAND_LEN] = (byte) length;
	}

	/**
	 * What is the current playing time in secs.
	 * 
	 * @return the current playing time in secs
	 */
	public final int time() {
		final EventScheduler c = c64.getEventScheduler();
		return (int) (c.getTime(Phase.PHI2) / c.getCyclesPerSecond());
	}

	/**
	 * Enter basic command after reset.
	 * 
	 * @param command
	 *            basic command after reset
	 */
	public final void setCommand(final String command) {
		this.command = command;
	}

	/**
	 * Get current play-list.
	 */
	public final PlayList getPlayList() {
		return playList;
	}

	/**
	 * Get current timer.
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
	 * @param tune
	 *            tune to play
	 */
	public final void setTune(final SidTune tune) {
		this.tune = tune;
	}

	/**
	 * Configure all available SIDs.
	 * 
	 * @param action
	 *            SID chip consumer
	 */
	public final void configureSIDs(BiConsumer<Integer, SIDEmu> action) {
		for (int chipNum = 0; chipNum < PLA.MAX_SIDS; chipNum++) {
			final SIDEmu sid = c64.getPla().getSID(chipNum);
			if (sid != null) {
				action.accept(chipNum, sid);
			}
		}
	}

	/**
	 * Configure one specific SID.
	 * 
	 * @param chipNum
	 *            SID chip number
	 * @param action
	 *            SID chip consumer
	 */
	public final void configureSID(int chipNum, Consumer<SIDEmu> action) {
		final SIDEmu sid = c64.getPla().getSID(chipNum);
		if (sid != null) {
			action.accept(sid);
		}
	}

	/**
	 * Configure the mixer, optionally implemented by SID builder.
	 * 
	 * @param action
	 *            mixer consumer
	 */
	public final void configureMixer(Consumer<Mixer> action) {
		if (sidBuilder instanceof Mixer) {
			action.accept((Mixer) sidBuilder);
		}
	}

	/**
	 * Start player thread.
	 */
	public final void startC64() {
		if (playerThread == null || !playerThread.isAlive()) {
			playerThread = new Thread(playerRunnable);
			playerThread.setPriority(Thread.MAX_PRIORITY);
			playerThread.start();
		}
	}

	/**
	 * Stop player thread.
	 */
	public final void stopC64() {
		try {
			while (playerThread != null && playerThread.isAlive()) {
				quit();
				playerThread.join(3000);
				// If the player can not be stopped clean:
				playerThread.interrupt();
			}
		} catch (InterruptedException e) {
			// unclean player thread halt (interrupted)
		}
	}

	/**
	 * Wait for termination of the player thread.
	 * 
	 * @throws InterruptedException
	 */
	public final void waitForC64() throws InterruptedException {
		while (playerThread != null && playerThread.isAlive()) {
			Thread.sleep(1000);
		}
	}

	/**
	 * Set a hook to be called when the player has opened a tune.
	 * 
	 * @param menuHook
	 *            menu hook
	 */
	public final void setMenuHook(Consumer<Player> menuHook) {
		this.menuHook = menuHook;
	}

	/**
	 * Set a hook to be called when the player has played a chunk.
	 * 
	 * @param interactivityHook
	 */
	public final void setInteractivityHook(Consumer<Player> interactivityHook) {
		this.interactivityHook = interactivityHook;
	}

	/**
	 * Get the player's state
	 * 
	 * @return the player's state
	 */
	public final ReadOnlyObjectProperty<State> stateProperty() {
		return stateProperty;
	}

	/**
	 * Player runnable to play music in a background thread.
	 */
	private Runnable playerRunnable = () -> {
		// Run until the player gets stopped
		do {
			try {
				// Open tune
				open();
				menuHook.accept(Player.this);
				// Play next chunk of sound data, until it gets stopped
				stateProperty.set(State.RUNNING);
				while (play()) {
					// Pause? sleep for awhile
					if (stateProperty.get() == State.PAUSED) {
						Thread.sleep(PAUSE_SLEEP_TIME);
					}
					interactivityHook.accept(Player.this);
				}
			} catch (InterruptedException e) {
			} finally {
				// Don't forget to close
				close();
			}
			// "Play it once, Sam. For old times' sake."
		} while (stateProperty.get() == State.RESTART);
	};

	/**
	 * Open player, that means basically: Reset C64 and start playing the tune.
	 * 
	 * @throws InterruptedException
	 */
	private void open() throws InterruptedException {
		if (stateProperty.get() == State.RESTART) {
			stateProperty.set(State.STOPPED);
		}
		playList = PlayList.getInstance(config, tune);

		CPUClock cpuClock = CPUClock.getCPUClock(config.getEmulationSection(),
				tune);
		setClock(cpuClock);

		AudioConfig audioConfig = AudioConfig.getInstance(config
				.getAudioSection());

		// Audio driver different to Audio enum members are on hold!
		if (audioDriver == null
				|| Arrays.stream(Audio.values()).anyMatch(
						audio -> audio.getAudioDriver() == audioDriver)) {
			audioDriver = config.getAudioSection().getAudio().getAudioDriver();
		}

		// replace driver settings for mp3
		audioDriver = handleMP3(config, tune, audioDriver);

		// create SID builder for hardware or emulation
		sidBuilder = createSIDBuilder(cpuClock, audioConfig);

		// open audio driver
		try {
			String recordingFilename = recordingFilenameProvider.apply(tune);
			audioDriver.open(audioConfig, recordingFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		reset();
		stateProperty.set(State.START);
	}

	/**
	 * Create configured SID chip implementation (software/hardware).
	 */
	private SIDBuilder createSIDBuilder(CPUClock cpuClock,
			AudioConfig audioConfig) {
		final Engine engine = config.getEmulationSection().getEngine();
		switch (engine) {
		case EMULATION:
			return new ReSIDBuilder(c64.getEventScheduler(), config,
					audioConfig, cpuClock, audioDriver);
		case HARDSID:
			return new HardSIDBuilder(c64.getEventScheduler(), config);
		default:
			throw new RuntimeException("Unknown engine type: " + engine);
		}
	}

	/**
	 * Get the currently used audio driver.
	 * 
	 * @return current audio driver
	 */
	public AudioDriver getAudioDriver() {
		return audioDriver;
	}

	/**
	 * Set alternative audio driver (not contained in {@link Audio}).<BR>
	 * For example, If it is required to use a new instance of audio driver each
	 * time the player plays a tune.
	 * 
	 * @param driver
	 *            for example {@link MP3Stream}
	 */
	public final void setAudioDriver(final AudioDriver driver) {
		this.audioDriver = driver;
	}

	/**
	 * MP3 play-back is using the COMPARE audio driver. Old settings are saved
	 * (playing mp3) and restored (next time normal tune is played).
	 */
	private AudioDriver handleMP3(final IConfig config, final SidTune tune,
			AudioDriver audioDriver) {
		AudioDriver newAudioDriver = audioDriver;
		if (oldAudioDriver == null && tune instanceof MP3Tune) {
			// save settings before MP3 gets played
			oldAudioDriver = audioDriver;
		} else if (oldAudioDriver != null && !(tune instanceof MP3Tune)) {
			// restore settings after MP3 has been played last time
			newAudioDriver = oldAudioDriver;
			oldAudioDriver = null;
		}
		if (tune instanceof MP3Tune) {
			// Change driver settings to use comparison driver for MP3 play-back
			MP3Tune mp3Tune = (MP3Tune) tune;
			newAudioDriver = new CmpMP3File();
			config.getAudioSection().setPlayOriginal(true);
			config.getAudioSection().setMp3File(mp3Tune.getMP3Filename());
		}
		if (newAudioDriver instanceof CmpMP3File) {
			// Configure compare driver settings
			CmpMP3File cmp = (CmpMP3File) newAudioDriver;
			cmp.setPlayOriginal(config.getAudioSection().isPlayOriginal());
			cmp.setMp3File(new File(config.getAudioSection().getMp3File()));
		}
		return newAudioDriver;
	}

	/**
	 * Change SIDs according to the current tune.
	 */
	public final void createOrUpdateSIDs() {
		IEmulationSection emulation = config.getEmulationSection();
		c64.getPla().clearSIDAddresses();
		for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			SIDEmu sid = c64.getPla().getSID(sidNum);
			if (SidTune.isSIDUsed(emulation, tune, sidNum)) {
				sid = sidBuilder.lock(sid, sidNum, tune);
				c64.getPla().setSID(sidNum, sid);
				int base = SidTune.getSIDAddress(emulation, tune, sidNum);
				c64.getPla().setSIDAddress(sidNum, base);
			} else if (sid != null) {
				// Safely remove SIDs no more in use
				sidBuilder.unlock(sid);
				c64.getPla().setSID(sidNum, null);
			}
		}
	}

	/**
	 * Register a SID write register listener for all SID chips in use.
	 * 
	 * @param sidListener
	 *            SID write register listener
	 */
	public void setSidWriteListener(SIDListener sidListener) {
		IEmulationSection emulation = config.getEmulationSection();
		for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			if (SidTune.isSIDUsed(emulation, tune, sidNum)) {
				c64.getPla().setSidWriteListener(sidNum, sidListener);
			} else {
				c64.getPla().setSidWriteListener(sidNum, null);
			}
		}
	}

	/**
	 * Play routine (clock chips until audio buffer is filled completely or
	 * player is paused).
	 * 
	 * @throws InterruptedException
	 *             audio production interrupted
	 */
	private boolean play() throws InterruptedException {
		for (int i = 0; stateProperty.get() == State.RUNNING
				&& i < config.getAudioSection().getBufferSize(); i++) {
			c64.getEventScheduler().clock();
		}
		return stateProperty.get() == State.RUNNING
				|| stateProperty.get() == State.PAUSED;
	}

	/**
	 * Get end state according to the configuration.<BR>
	 * Looping tunes restart the player, otherwise it gets stopped.
	 * 
	 * @return end state of the player
	 */
	private State getEndState() {
		return config.getSidplay2Section().isLoop() ? State.RESTART
				: State.EXIT;
	}

	/**
	 * Close player.
	 */
	private void close() {
		// Safely remove ALL SIDs
		configureSIDs((sidNum, sid) -> {
			sidBuilder.unlock(sid);
			c64.getPla().setSID(sidNum, null);
		});
		audioDriver.close();
	}

	/**
	 * Play tune.
	 * 
	 * @param tune
	 *            file to play the tune (SidTune.RESET means just reset C64)
	 */
	public final void play(final SidTune tune) {
		stopC64();
		setTune(tune);
		startC64();
	}

	/**
	 * Pause player.
	 */
	public final void pause() {
		if (stateProperty.get() == State.QUIT) {
			play(tune);
		} else if (stateProperty.get() == State.PAUSED) {
			stateProperty.set(State.RUNNING);
		} else {
			stateProperty.set(State.PAUSED);
			audioDriver.pause();
		}
	}

	/**
	 * Play next song.
	 */
	public final void nextSong() {
		playList.next();
		stateProperty.set(State.RESTART);
	}

	/**
	 * Play previous song.<BR>
	 * <B>Note:</B> After {@link #PREV_SONG_TIMEOUT} has been reached, the
	 * current tune is restarted instead.
	 */
	public final void previousSong() {
		if (time() < PREV_SONG_TIMEOUT) {
			playList.previous();
		}
		stateProperty.set(State.RESTART);
	}

	/**
	 * Play first song.
	 */
	public final void firstSong() {
		playList.first();
		stateProperty.set(State.RESTART);
	}

	/**
	 * Play last song.
	 */
	public final void lastSong() {
		playList.last();
		stateProperty.set(State.RESTART);
	}

	/**
	 * Get mixer info.
	 * 
	 * @param function
	 *            mixer function to apply
	 * @param defaultValue
	 *            default value, if SIDBuilder does not implement a mixer
	 * @return mixer info
	 */
	public final <T> T getMixerInfo(Function<Mixer, T> function, T defaultValue) {
		boolean isMixer = sidBuilder instanceof Mixer;
		return isMixer ? function.apply((Mixer) sidBuilder) : defaultValue;
	}

	/**
	 * Quit player.
	 */
	public final void quit() {
		stateProperty.set(State.QUIT);
	}

	/**
	 * Set song length database.
	 * 
	 * @param sidDatabase
	 *            song length database
	 */
	public final void setSidDatabase(SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	/**
	 * Get song length database info.
	 * 
	 * @param function
	 *            SidDatabase function to apply
	 * @param defaultValue
	 *            default value, if database is not set
	 * @return song length database info
	 */
	public final <T> T getSidDatabaseInfo(Function<SidDatabase, T> function,
			T defaultValue) {
		return sidDatabase != null ? function.apply(sidDatabase) : defaultValue;
	}

	/**
	 * Set Sid Tune Information List (STIL)
	 * 
	 * @param stil
	 *            Sid Tune Information List
	 */
	public final void setSTIL(STIL stil) {
		this.stil = stil;
	}

	/**
	 * Get Sid Tune Information List info.
	 * 
	 * @param collectionName
	 *            entry path to get infos for
	 * @return Sid Tune Information List info
	 */
	public final STILEntry getStilEntry(String collectionName) {
		return stil != null && collectionName != null ? stil
				.getSTILEntry(collectionName) : null;
	}

	/**
	 * Set provider of recording filenames.
	 * 
	 * @param recordingFilenameProvider
	 *            provider of recording filenames
	 */
	public void setRecordingFilenameProvider(
			Function<SidTune, String> recordingFilenameProvider) {
		this.recordingFilenameProvider = recordingFilenameProvider;
	}

	/**
	 * The credits for the authors of many parts of this emulator.
	 * 
	 * @return the credits
	 */
	public final String getCredits(Properties properties) {
		final StringBuffer credits = new StringBuffer();
		credits.append("Java Version and User Interface v");
		credits.append(properties.getProperty("version"));
		credits.append(":\n");
		credits.append("\tCopyright (©) 2007-2015 Ken Händel\n");
		credits.append("\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Distortion Simulation and development: Antti S. Lankila\n");
		credits.append("\thttp://bel.fi/~alankila/c64-sw/\n");
		credits.append("Network SID Device:\n");
		credits.append("\tSupported by Wilfred Bos, The Netherlands\n");
		credits.append("\thttp://www.acid64.com\n");
		credits.append("Testing and Feedback: Nata, founder of proNoise\n");
		credits.append("\thttp://www.nata.netau.net/\n");
		credits.append("graphical output:\n" + "\t(©) 2007 Joakim Eriksson\n");
		credits.append("\t(©) 2009, 2010 Antti S. Lankila\n");
		credits.append("MP3 encoder/decoder (jump3r), based on Lame\n");
		credits.append("\tCopyright (©) 2010-2011  Ken Händel\n");
		credits.append("\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("This product uses the database of Game Base 64 (GB64)\n");
		credits.append("\thttp://www.gb64.com/\n");
		credits.append("Command Line Parser (JCommander):\n");
		credits.append("\tCopyright (©) 2010-2014 Cédric Beust\n");
		credits.append("\thttp://jcommander.org/\n");
		credits.append("MP3 downloads from Stone Oakvalley's Authentic SID MusicCollection (SOASC=):\n");
		credits.append("\thttp://www.6581-8580.com/\n");
		credits.append("6510 cross assembler (Kickassembler V3.36):\n");
		credits.append("\tCopyright (©) 2006-2014 Mads Nielsen\n");
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
		credits.append(resid_builder.resid.ReSID.credits());
		credits.append(resid_builder.residfp.ReSIDfp.credits());
		credits.append(hardsid_builder.HardSID.credits());
		return credits.toString();
	}

	/**
	 * Test main: Play a tune.
	 * 
	 * @param args
	 *            the filename of the tune is the first arg
	 * @throws SidTuneError
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException,
			SidTuneError {
		if (args.length < 1) {
			System.err.println("Missing argument: <filename>");
			System.exit(-1);
		}
		// Load tune
		final SidTune tune = SidTune.load(new File(args[0]));

		// Create player
		final Player player = new Player(new IniConfig());

		// start C64
		player.play(tune);
	}

}
