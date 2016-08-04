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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.sound.sampled.LineUnavailableException;

import hardsid_builder.HardSIDBuilder;
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
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.STILEntry;
import resid_builder.ReSIDBuilder;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.CmpMP3File;
import sidplay.audio.MP3Driver;
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
	 * Timeout (in ms) for sleeping, if player is paused.
	 */
	private static final int PAUSE_SLEEP_TIME = 250;
	/**
	 * Timeout (in ms) for quitting the player.
	 */
	private static final int PAUSE_QUIT_TIME = 1000;
	
	/**
	 * Previous song select timeout (< 4 secs).
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
	private ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(State.QUIT);
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
	 * Currently used audio driver.
	 */
	private AudioDriver audioDriver = Audio.SOUNDCARD.getAudioDriver();
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
	private Function<SidTune, String> recordingFilenameProvider = tune -> "jsidplay2";
	/**
	 * Insert required SIDs. use SID builder to create/destroy SIDs.
	 */
	private BiFunction<Integer, SIDEmu, SIDEmu> requiredSIDs = (sidNum, sidEmu) -> {
		if (SidTune.isSIDUsed(config.getEmulationSection(), tune, sidNum)) {
			return sidBuilder.lock(sidEmu, sidNum, tune);
		} else if (sidEmu != SIDEmu.NONE) {
			sidBuilder.unlock(sidEmu);
		}
		return SIDEmu.NONE;
	};
	/**
	 * Eject all SIDs.
	 */
	private BiFunction<Integer, SIDEmu, SIDEmu> noSIDs = (sidNum, sidEmu) -> SIDEmu.NONE;
	/**
	 * Set base address of required SIDs.
	 */
	private IntFunction<Integer> sidLocator = sidNum -> SidTune.getSIDAddress(config.getEmulationSection(), tune,
			sidNum);

	/**
	 * Create a Music Player.
	 * 
	 * @param config
	 *            configuration
	 */
	public Player(final IConfig config) {
		this(config, MOS6510.class);
	}

	/**
	 * Create a Music Player.
	 * 
	 * @param config
	 *            configuration
	 * @param cpuClass
	 *            CPU class implementation
	 */
	public Player(final IConfig config, final Class<? extends MOS6510> cpuClass) {
		super(config, cpuClass);
		this.playList = PlayList.getInstance(config, SidTune.RESET);
		this.timer = new Timer(this) {

			@Override
			public void start() {
				c64.insertSIDChips(requiredSIDs, sidLocator);
				configureMixer(mixer -> mixer.start());
			}

			/**
			 * If a tune ends, there are these possibilities:
			 * <OL>
			 * <LI>Play next song (except singles)
			 * <LI>Play again looping song
			 * <LI>End tune
			 * </OL>
			 * 
			 * @see sidplay.player.Timer#end()
			 */
			@Override
			public void end() {
				if (tune != SidTune.RESET) {
					if (!config.getSidplay2Section().isSingle() && playList.hasNext()) {
						nextSong();
					} else if (config.getSidplay2Section().isLoop()) {
						stateProperty.set(State.RESTART);
					} else {
						stateProperty.set(State.END);
					}
				}
			}

			/**
			 * If a tune starts playing, fade-in volume.
			 * 
			 * @see sidplay.player.Timer#fadeInStart(int)
			 */
			@Override
			public void fadeInStart(final int fadeIn) {
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
			public void fadeOutStart(final int fadeOut) {
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

	@Override
	protected final void setClock(final CPUClock cpuFreq) {
		super.setClock(cpuFreq);
		sidBuilder = createSIDBuilder(cpuFreq);
	}

	/**
	 * Create configured SID chip implementation (software/hardware).
	 * 
	 * @param cpuClock
	 *            CPU clock frequency
	 * @return SID builder
	 */
	private SIDBuilder createSIDBuilder(final CPUClock cpuClock) {
		final Engine engine = config.getEmulationSection().getEngine();
		switch (engine) {
		case EMULATION:
			return new ReSIDBuilder(c64.getEventScheduler(), config, cpuClock, audioDriver);
		case HARDSID:
			return new HardSIDBuilder(c64.getEventScheduler(), config);
		default:
			throw new RuntimeException("Unknown engine type: " + engine);
		}
	}

	/**
	 * Call to update SID chips each time SID configuration has been changed.
	 */
	public final void updateSIDChipConfiguration() {
		c64.getEventScheduler().scheduleThreadSafe(new Event("Update SID Chip Configuration") {
			@Override
			public void event() throws InterruptedException {
				c64.insertSIDChips(requiredSIDs, sidLocator);
			}
		});
	}

	/**
	 * Power-on C64 system.
	 */
	protected final void reset() {
		super.reset();
		timer.reset();
		configureMixer(mixer -> mixer.reset());

		c64.getEventScheduler().schedule(new Event("Auto-start") {
			@Override
			public void event() throws InterruptedException {
				if (tune != SidTune.RESET) {
					// for tunes: Install player into RAM
					Integer driverAddress = tune.placeProgramInMemory(c64.getRAM());
					if (driverAddress != null) {
						// Set play address to feedback call frames counter.
						c64.setPlayAddr(tune.getInfo().getPlayAddr());
						// Start SID player driver
						c64.getCPU().forcedJump(driverAddress);
					} else {
						// No player: Start basic program or assembler code
						final int loadAddr = tune.getInfo().getLoadAddr();
						command = loadAddr == 0x0801 ? RUN : String.format(SYS, loadAddr);
					}
				}
				if (command != null) {
					if (command.startsWith(LOAD)) {
						// Load from tape needs someone to press play
						datasette.control(Control.START);
					}
					// Enter basic command
					typeInCommand(command);
				}
			}
		}, SidTune.getInitDelay(tune));
	}

	/**
	 * Simulate a user typed-in command.
	 * 
	 * @param command
	 *            command to type-in
	 */
	public final void typeInCommand(final String command) {
		byte[] ram = c64.getRAM();
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		for (int charNum = 0; charNum < length; charNum++) {
			ram[RAM_COMMAND + charNum] = (byte) command.charAt(charNum);
		}
		ram[RAM_COMMAND_LEN] = (byte) length;
	}

	/**
	 * Enter basic command after reset.
	 * 
	 * @param command
	 *            basic command after reset
	 */
	private void setCommand(final String command) {
		this.command = command;
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
	 * @param tune
	 *            tune to play
	 */
	public final void setTune(final SidTune tune) {
		this.tune = tune;
	}

	/**
	 * Configure the mixer, optionally implemented by SID builder.
	 * 
	 * @param action
	 *            mixer consumer
	 */
	public final void configureMixer(final Consumer<Mixer> action) {
		if (sidBuilder instanceof Mixer) {
			action.accept((Mixer) sidBuilder);
		}
	}

	/**
	 * Start player thread.
	 */
	public final void startC64() {
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
	 * @param quitOrWait
	 *            quit player (true) or wait for termination, only (false)
	 */
	public final void stopC64(final boolean quitOrWait) {
		try {
			while (playerThread != null && playerThread.isAlive()) {
				if (quitOrWait) {
					quit();
				}
				playerThread.join(PAUSE_QUIT_TIME);
				if (quitOrWait && playerThread.isAlive()) {
					// emergency break, if audio driver is locked
					playerThread.interrupt();
				}
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Set a hook to be called when the player has opened a tune.
	 * 
	 * @param menuHook
	 *            menu hook
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
	 * Get the player's state,
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
				open();
				stateProperty.set(State.START);
				menuHook.accept(Player.this);
				// Play next chunk of sound data
				stateProperty.set(State.PLAY);
				while (play()) {
					interactivityHook.accept(Player.this);
				}
			} catch (CmpMP3File.MP3Termination e) {
				stateProperty.set(State.END);
			} catch (InterruptedException | IOException | LineUnavailableException e) {
				throw new RuntimeException(e.getMessage());
			} finally {
				close();
			}
			// "Play it once, Sam. For old times' sake."
		} while (stateProperty.get() == State.RESTART);
	};

	/**
	 * Open player, that means basically: Reset C64 and start playing the tune.
	 * 
	 * <B>Note:</B> Audio driver different to {@link Audio} members are on hold!
	 * 
	 * @throws InterruptedException
	 *             audio play-back interrupted
	 * @throws LineUnavailableException
	 *             audio line currently in use
	 * @throws IOException
	 *             audio output file cannot be written
	 */
	private void open() throws InterruptedException, IOException, LineUnavailableException {
		playList = PlayList.getInstance(config, tune);

		IAudioSection audioSection = config.getAudioSection();
		if (Arrays.stream(Audio.values()).anyMatch(audio -> audio.getAudioDriver().equals(audioDriver))) {
			audioDriver = audioSection.getAudio().getAudioDriver(audioSection, tune);
		}
		// open audio driver
		AudioConfig audioConfig = AudioConfig.getInstance(audioSection);
		audioDriver.open(audioConfig, recordingFilenameProvider.apply(tune));

		// PAL/NTSC
		setClock(CPUClock.getCPUClock(config.getEmulationSection(), tune));

		reset();
	}

	/**
	 * Set alternative audio driver (not contained in {@link Audio}).<BR>
	 * For example, If it is required to use a new instance of audio driver each
	 * time the player plays a tune (e.g. {@link MP3Stream})
	 * 
	 * @param driver
	 *            for example {@link MP3Driver.MP3Stream}
	 */
	public final void setAudioDriver(final AudioDriver driver) {
		this.audioDriver = driver;
	}

	/**
	 * Play routine (clock chips until audio buffer is filled completely or
	 * player gets paused).
	 * 
	 * @return continue to play next time?
	 * 
	 * @throws InterruptedException
	 *             audio production interrupted
	 */
	private boolean play() throws InterruptedException {
		if (stateProperty.get() == State.PLAY) {
			for (int i = 0; i < config.getAudioSection().getBufferSize(); i++) {
				c64.getEventScheduler().clock();
			}
		}
		if (stateProperty.get() == State.PAUSE) {
			Thread.sleep(PAUSE_SLEEP_TIME);
		}
		return stateProperty.get() == State.PLAY || stateProperty.get() == State.PAUSE;
	}

	/**
	 * Close player.
	 */
	private void close() {
		c64.insertSIDChips(noSIDs, sidLocator);
		audioDriver.close();
	}

	/**
	 * Play tune.
	 * 
	 * @param tune
	 *            tune to play (SidTune.RESET means just reset C64)
	 */
	public final void play(final SidTune tune) {
		play(tune, null);
	}

	/**
	 * Reset C64 and enter basic command.
	 * 
	 * @param command
	 *            basic command to be entered after a normal reset
	 */
	public final void resetC64(String command) {
		play(SidTune.RESET, command);
	}

	/**
	 * Turn C64 off and on, load a tune and enter basic command.
	 * 
	 * @param tune
	 *            tune to play (SidTune.RESET means just reset C64)
	 * @param command
	 *            basic command to be entered after a normal reset
	 */
	private void play(final SidTune tune, final String command) {
		stopC64();
		setTune(tune);
		setCommand(command);
		startC64();
	}

	/**
	 * Pause or continue the player.
	 */
	public final void pauseContinue() {
		if (stateProperty.get() == State.QUIT || stateProperty.get() == State.END) {
			play(tune);
		} else if (stateProperty.get() == State.PAUSE) {
			stateProperty.set(State.PLAY);
			// audio driver continues automatically, next call of write!
		} else {
			stateProperty.set(State.PAUSE);
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
	 * @param <T>
	 *            mixer info return type
	 * @param defaultValue
	 *            default value, if SIDBuilder does not implement a mixer
	 * @return mixer info
	 */
	public final <T> T getMixerInfo(final Function<Mixer, T> function, final T defaultValue) {
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
	public final void setSidDatabase(final SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	/**
	 * Get song length database info.
	 * 
	 * @param function
	 *            SidDatabase function to apply
	 * @param <T>
	 *            SidDatabase return type
	 * @param defaultValue
	 *            default value, if database is not set
	 * @return song length database info
	 */
	public final <T> T getSidDatabaseInfo(final Function<SidDatabase, T> function, final T defaultValue) {
		return sidDatabase != null ? function.apply(sidDatabase) : defaultValue;
	}

	/**
	 * Set SID Tune Information List (STIL).
	 * 
	 * @param stil
	 *            SID Tune Information List
	 */
	public final void setSTIL(final STIL stil) {
		this.stil = stil;
	}

	/**
	 * Get SID Tune Information List info.
	 * 
	 * @param collectionName
	 *            entry path to get infos for
	 * @return SID Tune Information List info
	 */
	public final STILEntry getStilEntry(final String collectionName) {
		return stil != null && collectionName != null ? stil.getSTILEntry(collectionName) : null;
	}

	/**
	 * Set provider of recording filenames.
	 * 
	 * @param recordingFilenameProvider
	 *            provider of recording filenames
	 */
	public final void setRecordingFilenameProvider(final Function<SidTune, String> recordingFilenameProvider) {
		this.recordingFilenameProvider = recordingFilenameProvider;
	}

	/**
	 * The credits for the authors of many parts of this emulator.
	 * 
	 * @param properties
	 *            containing dynamic values for the credits
	 * @return the credits
	 */
	public final String getCredits(final Properties properties) {
		final StringBuffer credits = new StringBuffer();
		credits.append("Java Version and User Interface v");
		credits.append(properties.getProperty("version"));
		credits.append(":\n");
		credits.append("\tCopyright (©) 2007-2016 Ken Händel\n");
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
		credits.append("6510 cross assembler (Kickassembler V4.1):\n");
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
	 *             SID tune error
	 * @throws IOException
	 *             tune file cannot be read
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
