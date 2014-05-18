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
package libsidplay;

import hardsid_builder.HardSIDBuilder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.C1541Runner;
import libsidplay.components.c1541.DisconnectedParallelCable;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.c1541.IParallelCable;
import libsidplay.components.c1541.SameThreadC1541Runner;
import libsidplay.components.c1541.VIACore;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.iec.IECBus;
import libsidplay.components.iec.SerialIECDevice;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.player.DriverSettings;
import libsidplay.player.Emulation;
import libsidplay.player.FakeStereo;
import libsidplay.player.State;
import libsidplay.player.Timer;
import libsidplay.player.PlayList;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PRG2TAP;
import libsidutils.PRG2TAPProgram;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;
import libsidutils.SidDatabase;
import libsidutils.cpuparser.CPUParser;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ChipModel;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.NaturalFinishedException;
import sidplay.ini.intf.IConfig;

/**
 * The player contains a C64 computer and additional peripherals.<BR>
 * It is meant as a complete setup (C64, tape/disk drive, carts and more).<BR>
 * It also has some music player capabilities.
 * 
 * @author Ken Händel
 * 
 */
public class Player {
	private static final int PAUSE_SLEEP_TIME = 250;
	private static final int NUM_EVENTS_TO_PLAY = 10000;
	/** Previous song select timeout (< 4 secs) **/
	private static final int SID2_PREV_SONG_TIMEOUT = 4;

	/**
	 * Configuration.
	 */
	private final IConfig config;

	/**
	 * C64 computer.
	 */
	private final C64 c64;
	/**
	 * C1530 datasette.
	 */
	private final Datasette datasette;
	/**
	 * IEC bus.
	 */
	private final IECBus iecBus;
	/**
	 * Additional serial devices like a printer (except of the floppies).
	 */
	private final SerialIECDevice[] serialDevices;
	/**
	 * C1541 floppy disk drives.
	 */
	private final C1541[] floppies;
	/**
	 * Responsible to keep C64 and C1541 in sync.
	 */
	private final C1541Runner c1541Runner;
	/**
	 * MPS803 printer.
	 */
	private final MPS803 printer;
	/**
	 * Music player state.
	 */
	private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(
			State.STOPPED);
	/**
	 * Play timer.
	 */
	private final Timer timer = new Timer();
	/**
	 * Play list.
	 */
	private PlayList playList = PlayList.NONE;
	/**
	 * Currently played tune.
	 */
	private SidTune tune;
	/**
	 * Autostart command to be typed-in after reset.
	 */
	private String command;
	/**
	 * Music player thread.
	 */
	private Thread fPlayerThread;
	/**
	 * Called each time a tune starts to play.
	 */
	private Consumer<Player> menuHook = (player) -> {
	};
	/**
	 * Called each time a chunk of music data has been played.
	 */
	private Consumer<Player> interactivityHook = (player) -> {
	};
	/**
	 * Audio driver and emulation setting.
	 */
	private DriverSettings driverSettings = new DriverSettings(Audio.SOUNDCARD,
			Emulation.RESID);
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
	 * Disk image extension policy (track count greater than 35).
	 */
	private IExtendImageListener policy;

	/**
	 * Create a complete setup (C64, tape/disk drive, carts and more).
	 */
	public Player(IConfig config) {
		this.config = config;
		initializeTmpDir();
		this.iecBus = new IECBus();

		this.printer = new MPS803(this.iecBus, (byte) 4, (byte) 7) {
			@Override
			public void setBusy(final boolean flag) {
				c64.cia2.setFlag(flag);
			}

			@Override
			public long clk() {
				return c64.context.getTime(Phase.PHI2);
			}

		};

		this.c64 = new C64() {

			@Override
			public void printerUserportWriteData(final byte data) {
				if (config.getPrinter().isPrinterOn()) {
					printer.printerUserportWriteData(data);
				}
			}

			@Override
			public void printerUserportWriteStrobe(final boolean strobe) {
				if (config.getPrinter().isPrinterOn()) {
					printer.printerUserportWriteStrobe(strobe);
				}
			}

			@Override
			public byte readFromIECBus() {
				if (config.getC1541().isDriveOn()) {
					c1541Runner.synchronize(0);
					return iecBus.readFromIECBus();
				}
				return (byte) 0x80;
			}

			@Override
			public void writeToIECBus(final byte data) {
				if (config.getC1541().isDriveOn()) {
					// more elegant solution to
					// assure a one cycle write delay
					c1541Runner.synchronize(1);
					iecBus.writeToIECBus(data);
				}
			}

			@Override
			public boolean getTapeSense() {
				return datasette.getTapeSense();
			}

			@Override
			public void setMotor(final boolean state) {
				datasette.setMotor(state);
			}

			@Override
			public void toggleWriteBit(final boolean state) {
				datasette.toggleWriteBit(state);
			}

		};

		this.datasette = new Datasette(c64.getEventScheduler()) {
			@Override
			public void setFlag(final boolean flag) {
				c64.cia1.setFlag(flag);
			}
		};

		final C1541 c1541 = new C1541(iecBus, 8, FloppyType.C1541);

		this.floppies = new C1541[] { c1541 };
		this.serialDevices = new SerialIECDevice[] { printer };

		this.iecBus.setFloppies(floppies);
		this.iecBus.setSerialDevices(serialDevices);
		this.c1541Runner = new SameThreadC1541Runner(c64.getEventScheduler(),
				c1541.getEventScheduler());
	}

	/**
	 * Create temp directory, if it does not exist.
	 */
	private void initializeTmpDir() {
		File tmpDir = new File(config.getSidplay2().getTmpDir());
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	public final IConfig getConfig() {
		return config;
	}

	/**
	 * Set frequency (PAL/NTSC)
	 * 
	 * @param cpuFreq
	 *            frequency (PAL/NTSC)
	 */
	public final void setClock(final CPUClock cpuFreq) {
		c64.setClock(cpuFreq);
		c1541Runner.setClockDivider(cpuFreq);
		for (SerialIECDevice device : serialDevices) {
			device.setClock(cpuFreq);
		}
	}

	/**
	 * Power-on C64 system. Only play() calls should be made after this point.
	 * 
	 * @throws InterruptedException
	 */
	private final void reset() throws InterruptedException {
		c64.reset();
		iecBus.reset();
		datasette.reset();

		// Reset Floppies
		for (final C1541 floppy : floppies) {
			floppy.reset();
			floppy.setFloppyType(config.getC1541().getFloppyType());
			floppy.setRamExpansion(0, config.getC1541()
					.isRamExpansionEnabled0());
			floppy.setRamExpansion(1, config.getC1541()
					.isRamExpansionEnabled1());
			floppy.setRamExpansion(2, config.getC1541()
					.isRamExpansionEnabled2());
			floppy.setRamExpansion(3, config.getC1541()
					.isRamExpansionEnabled3());
			floppy.setRamExpansion(4, config.getC1541()
					.isRamExpansionEnabled4());
		}
		enableFloppyDiskDrives(config.getC1541().isDriveOn());
		connectC64AndC1541WithParallelCable(config.getC1541().isParallelCable());

		// Reset IEC devices
		for (final SerialIECDevice serialDevice : serialDevices) {
			serialDevice.reset();
		}

		enablePrinter(config.getPrinter().isPrinterOn());

		// Autostart program, if we have one.
		if (tune != null) {
			// Set playback addr to feedback call frames counter.
			c64.setPlayAddr(tune.getInfo().getPlayAddr());
			/*
			 * This is a bit ugly: starting PRG must be done after C64 system
			 * reset, while starting SID is done by CBM80 hook.
			 */
			c64.getEventScheduler().schedule(new Event("Tune init event") {
				@Override
				public void event() throws InterruptedException {
					final int address = tune.placeProgramInMemory(c64.getRAM());
					if (address != -1) {
						// Ideally the driver would set the volume, but whatever
						configureSIDs(sid -> sid.write(0x18, (byte) 0xf));
						c64.getCPU().forcedJump(address);
					} else {
						typeInCommand("RUN:\r");
					}
				}
			}, tune.getInitDelay());
		} else {
			// Normal reset code path using auto-start
			c64.getEventScheduler().schedule(new Event("Autostart event") {
				@Override
				public void event() throws InterruptedException {
					if (command != null) {
						typeInCommand(command);
						if (command.startsWith("LOAD\r")) {
							// Autostart tape needs someone to press play
							datasette.control(Control.START);
						}
						// command has been processed, forget it!
						command = null;
					}
				}
			}, 2500000);
		}
	}

	public final void typeInCommand(String runCommand) {
		byte[] ram = c64.getRAM();
		for (int i = 0; i < Math.min(runCommand.length(), 16); i++) {
			ram[0x277 + i] = (byte) runCommand.charAt(i);
		}
		ram[0xc6] = (byte) Math.min(runCommand.length(), 16);
	}

	/**
	 * What is the current playing time.
	 * 
	 * @return the current playing time
	 */
	public final int time() {
		final EventScheduler c = c64.getEventScheduler();
		return (int) (c.getTime(Phase.PHI2) / c.getCyclesPerSecond());
	}

	/**
	 * Enable floppy disk drives.
	 * 
	 * @param on
	 *            floppy disk drives enable
	 */
	public final void enableFloppyDiskDrives(final boolean on) {
		for (final C1541 floppy : floppies) {
			floppy.setPowerOn(on);
		}
		if (on) {
			c64.getEventScheduler().scheduleThreadSafe(
					new Event("Begin C64-C1541 sync") {
						@Override
						public void event() {
							c1541Runner.reset();
						}
					});
		} else {
			c64.getEventScheduler().scheduleThreadSafe(
					new Event("End C64-C1541 sync") {
						@Override
						public void event() {
							c1541Runner.cancel();
						}
					});
		}
	}

	/**
	 * Plug-in a parallel cable between the C64 user port and the C1541 floppy
	 * disk drive.
	 * 
	 * @param connected
	 *            connected enable
	 */
	public final void connectC64AndC1541WithParallelCable(
			final boolean connected) {
		final IParallelCable cable = connected ? makeCableBetweenC64AndC1541()
				: new DisconnectedParallelCable();
		c64.setParallelCable(cable);
		for (final C1541 floppy : floppies) {
			floppy.getBusController().setParallelCable(cable);
		}
	}

	/**
	 * Create a parallel cable between the C64 user port and the C1541 floppy
	 * disk drive.
	 * 
	 * @return parallel cable
	 */
	private IParallelCable makeCableBetweenC64AndC1541() {
		return new IParallelCable() {

			protected byte parallelCableCpuValue = (byte) 0xff;
			protected final byte parallelCableDriveValue[] = { (byte) 0xff,
					(byte) 0xff, (byte) 0xff, (byte) 0xff };

			@Override
			public void driveWrite(final byte data, final boolean handshake,
					final int dnr) {
				c64.cia2.setFlag(handshake);
				parallelCableDriveValue[dnr & ~0x08] = data;
			}

			@Override
			public byte driveRead(final boolean handshake) {
				c64.cia2.setFlag(handshake);
				return parallelCableValue();
			}

			/**
			 * Return the current state of the parallel cable.
			 * 
			 * @return the current state of the parallel cable
			 */
			private byte parallelCableValue() {
				byte val = parallelCableCpuValue;

				for (final C1541 floppy : floppies) {
					val &= parallelCableDriveValue[floppy.getID() & ~0x08];
				}
				return val;
			}

			@Override
			public void c64Write(final byte data) {
				c1541Runner.synchronize(0);
				parallelCableCpuValue = data;
			}

			@Override
			public byte c64Read() {
				c1541Runner.synchronize(0);
				return parallelCableValue();
			}

			@Override
			public void pulse() {
				c1541Runner.synchronize(0);
				for (final C1541 floppy : floppies) {
					floppy.getBusController().signal(VIACore.VIA_SIG_CB1,
							VIACore.VIA_SIG_FALL);
				}
			}
		};
	}

	/**
	 * Install Jiffy DOS floppy speeder.
	 * 
	 * Replace the Kernal ROM and replace the floppy ROM additionally. Note:
	 * Floppy kernal is replaced in all drives!
	 * 
	 * @param c64kernalFile
	 *            C64 Kernal replacement
	 * @param c1541kernalFile
	 *            C1541 Kernal replacement
	 * @throws IOException
	 *             error reading the ROMs
	 */
	public final void installJiffyDOS(final File c64kernalFile,
			final File c1541kernalFile, final File autostartFile)
			throws IOException, SidTuneError {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(
				c64kernalFile))) {
			byte[] c64Kernal = new byte[0x2000];
			dis.readFully(c64Kernal);
			c64.setCustomKernal(c64Kernal);
		}
		try (DataInputStream dis = new DataInputStream(new FileInputStream(
				c1541kernalFile))) {
			byte[] c1541Kernal = new byte[0x4000];
			dis.readFully(c1541Kernal);
			for (final C1541 floppy : floppies) {
				floppy.setCustomKernalRom(c1541Kernal);
			}
		}
		play(autostartFile != null ? SidTune.load(autostartFile) : null);
	}

	/**
	 * Uninstall Jiffy DOS floppy speeder.
	 */
	public final void uninstallJiffyDOS() {
		c64.setCustomKernal(null);
		for (final C1541 floppy : floppies) {
			floppy.setCustomKernalRom(null);
		}
	}

	public final void enablePrinter(boolean printerOn) {
		printer.turnPrinterOnOff(printerOn);
	}

	/**
	 * Enable CPU debugging (opcode stringifier).
	 * 
	 * @param cpuDebug
	 *            opcode stringifier to produce CPU debug output.
	 */
	public final void setDebug(final boolean cpuDebug) {
		c64.getCPU().setDebug(cpuDebug ? CPUParser.getInstance() : null);
	}

	/**
	 * Get C64.
	 * 
	 * @return C64
	 */
	public final C64 getC64() {
		return c64;
	}

	/**
	 * Get C1530 datasette.
	 * 
	 * @return C1530 datasette
	 */
	public final Datasette getDatasette() {
		return datasette;
	}

	/**
	 * Get C1541 floppies.
	 * 
	 * @return C1541 floppies
	 */
	public final C1541[] getFloppies() {
		return floppies;
	}

	/**
	 * Get MPS803 printer.
	 * 
	 * @return MPS803 printer
	 */
	public final MPS803 getPrinter() {
		return printer;
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

	public final PlayList getPlayList() {
		return playList;
	}

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
	 * Load a tune to play.
	 * 
	 * @param tune
	 *            tune to play
	 */
	public final void setTune(final SidTune tune) {
		this.tune = tune;
	}

	public final DriverSettings getDriverSettings() {
		return driverSettings;
	}

	/**
	 * Note: Before calling, you must safely call stopC64()!
	 */
	public final void setDriverSettings(DriverSettings driverSettings) {
		this.driverSettings = driverSettings;
	}

	/**
	 * The credits for the authors of many parts of this emulator.
	 * 
	 * @return the credits
	 */
	public final String getCredits() {
		final StringBuffer credits = new StringBuffer();
		credits.append("Java Version and User Interface v3.0:\n"
				+ "\tCopyright (©) 2007-2014 Ken Händel\n"
				+ "\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Distortion Simulation and development: Antti S. Lankila\n"
				+ "\thttp://bel.fi/~alankila/c64-sw/\n");
		credits.append("Network SID Device:\n"
				+ "\tSupported by Wilfred Bos, The Netherlands\n"
				+ "\thttp://www.acid64.com\n");
		credits.append("Testing and Feedback: Nata, founder of proNoise\n"
				+ "\thttp://www.nata.netau.net/\n");
		credits.append("graphical output:\n" + "\t(©) 2007 Joakim Eriksson\n"
				+ "\t(©) 2009, 2010 Antti S. Lankila\n");
		credits.append("MP3 encoder/decoder (jump3r), based on Lame\n"
				+ "\tCopyright (©) 2010-2011  Ken Händel\n"
				+ "\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("This product uses the database of Game Base 64 (GB64)\n"
				+ "\thttp://www.gb64.com/\n");
		credits.append("Command Line Parser (JCommander):\n"
				+ "\tCopyright (©) 2010-2014 Cédric Beust\n"
				+ "\thttp://jcommander.org/\n");
		credits.append("MP3 downloads from Stone Oakvalley's Authentic SID MusicCollection (SOASC=):\n"
				+ "\thttp://www.6581-8580.com/\n");
		credits.append("PSID to PRG converter (PSID64 v0.9):\n"
				+ "\tCopyright (©) 2001-2007  Roland Hermans\n"
				+ "\thttp://sourceforge.net/projects/psid64/\n");
		credits.append("An Optimizing Hybrid LZ77 RLE Data Compression Program (Pucrunch 22.11.2008):\n"
				+ "\tCopyright (©) 1997-2008 Pasi 'Albert' Ojala\n"
				+ "\thttp://www.cs.tut.fi/~albert/Dev/pucrunch/\n");
		credits.append("SID dump file (SIDDump V1.04):\n"
				+ "\tCopyright (©) 2007 Lasse Öörni\n");
		credits.append("HVSC playroutine identity scanner (SIDId V1.07):\n"
				+ "\tCopyright (©) 2007 Lasse Öörni\n");
		credits.append("High Voltage Music Engine MusicCollection (HVMEC V1.0):\n"
				+ "\tCopyright (©) 2011 by Stefano Tognon and Stephan Parth\n");
		credits.append("C1541 Floppy Disk Drive Emulation:\n"
				+ "\tCopyright (©) 2010 VICE (the Versatile Commodore Emulator)\n"
				+ "\thttp://www.viceteam.org/\n");
		credits.append("Based on libsidplay v2.1.1 engine:\n"
				+ "\tCopyright (©) 2000 Simon White sidplay2@yahoo.com\n"
				+ "\thttp://sidplay2.sourceforge.net\n");
		credits.append(MOS6510.credits());
		credits.append(MOS6526.credits());
		credits.append(VIC.credits());
		credits.append(HardSIDBuilder.credits());
		credits.append(ReSID.credits());
		return credits.toString();
	}

	public final void configureSIDs(Consumer<SIDEmu> action) {
		for (int chipNum = 0; chipNum < C64.MAX_SIDS; chipNum++) {
			configureSID(chipNum, action);
		}
	}

	public final void configureSID(int chipNum, Consumer<SIDEmu> action) {
		final SIDEmu s = c64.getSID(chipNum);
		if (s != null) {
			action.accept(s);
		}
	}

	public final void setMixerVolume(int i, float volumeInDb) {
		if (sidBuilder != null) {
			sidBuilder.setMixerVolume(i, volumeInDb);
		}
	}

	/**
	 * Start emulation (start player thread).
	 */
	public final void startC64() {
		fPlayerThread = new Thread(playerRunnable);
		fPlayerThread.setPriority(Thread.MAX_PRIORITY);
		fPlayerThread.start();
	}

	/**
	 * Stop emulation (stop player thread).
	 */
	public final void stopC64() {
		try {
			while (fPlayerThread != null && fPlayerThread.isAlive()) {
				quit();
				fPlayerThread.join(3000);
				// If the player can not be stopped clean:
				fPlayerThread.interrupt();
			}
		} catch (InterruptedException e) {
		}
	}

	public final void setMenuHook(Consumer<Player> menuHook) {
		this.menuHook = menuHook;
	}

	public final void setInteractivityHook(Consumer<Player> interactivityHook) {
		this.interactivityHook = interactivityHook;
	}

	public final ObjectProperty<State> stateProperty() {
		return stateProperty;
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
					// Open tune
					open();
					menuHook.accept(Player.this);
					// Play next chunk of sound data, until it gets stopped
					while (true) {
						// Pause? sleep for awhile
						if (stateProperty.get() == State.PAUSED) {
							Thread.sleep(PAUSE_SLEEP_TIME);
						}
						// Play a chunk
						if (!play()) {
							break;
						}
						interactivityHook.accept(Player.this);
					}
				} catch (InterruptedException e) {
				} finally {
					// Don't forget to close
					close();
				}

				// "Play it once, Sam. For old times' sake."
				if (stateProperty.get() == State.RESTART) {
					continue;
				}
				// Stop it
				break;
			}
		}
	};

	private void open() throws InterruptedException {
		if (stateProperty.get() == State.RESTART) {
			stateProperty.set(State.STOPPED);
		}
		playList = PlayList.getInstance(config, tune);

		CPUClock cpuClock = CPUClock.getCPUClock(config, tune);
		setClock(cpuClock);

		AudioConfig audioConfig = AudioConfig.create(config, tune);

		// 1. handle MP3 play-back (replaces audio driver and emulation)
		driverSettings.handleMP3(config, tune);

		// 2. Create SIDbuilder (may change audio driver to NIL audio driver)
		sidBuilder = createSIDBuilder(cpuClock, audioConfig);

		// 3. open audio driver (eventually NIL audio driver)
		try {
			driverSettings.getAudio().getAudioDriver()
					.open(audioConfig, config.getSidplay2().getTmpDir());
		} catch (LineUnavailableException | UnsupportedAudioFileException
				| IOException e) {
			throw new RuntimeException(e);
		}

		// According to the configuration, the SIDs must be updated.
		updateSIDs();

		// apply filter settings and stereo SID chip address
		configureSIDs(sid -> {
			sid.setFilter(config);
			sid.setFilterEnable(config.getEmulation().isFilter());
		});
		setStereoSIDAddress();

		updateStopTime();
		timer.setCurrent(-1);

		reset();

		stateProperty.set(State.RUNNING);
	}

	private SIDBuilder createSIDBuilder(CPUClock cpuClock,
			AudioConfig audioConfig) {
		switch (driverSettings.getEmulation()) {
		case RESID:
			return new ReSIDBuilder(config, driverSettings, audioConfig,
					cpuClock);

		case HARDSID:
			return new HardSIDBuilder(config);

		default:
			return null;
		}
	}

	private void setStereoSIDAddress() {
		Integer secondAddress = AudioConfig.getStereoAddress(config, tune);
		if (secondAddress != null) {
			if (Integer.valueOf(0xd400).equals(secondAddress)) {
				/** Stereo SID at 0xd400 hack */
				final SIDEmu s1 = c64.getSID(0);
				final SIDEmu s2 = c64.getSID(1);
				c64.setSID(0, new FakeStereo(c64.getEventScheduler(), s1, s2));
			} else {
				c64.setSecondSIDAddress(secondAddress);
			}
		}
	}

	/**
	 * Change SIDs according to the configured chip models. Note: Depending on
	 * the SIDBuilder implementation the SID chip could be reused or created
	 * from scratch.
	 */
	public final void updateSIDs() {
		EventScheduler eventScheduler = c64.getEventScheduler();
		if (sidBuilder != null) {
			ChipModel chipModel = ChipModel.getChipModel(config, tune);
			SIDEmu sid = c64.getSID(0);
			sid = sidBuilder.lock(eventScheduler, sid, chipModel);
			c64.setSID(0, sid);

			if (AudioConfig.isStereo(config, tune)) {
				ChipModel stereoModel = ChipModel.getStereoModel(config, tune);
				SIDEmu sid2 = c64.getSID(1);
				sid2 = sidBuilder.lock(eventScheduler, sid2, stereoModel);
				c64.setSID(1, sid2);
			}
		}
	}

	/**
	 * Set stop time according to the song length database (or use default
	 * length). If the user play length has been set, it overrides everything!
	 */
	public final void updateStopTime() {
		if (config.getSidplay2().getUserPlayLength() != 0) {
			// Use user defined fixed song length
			timer.setStop(config.getSidplay2().getUserPlayLength());
		} else {
			// default play default length or forever (0) ...
			timer.setStop(config.getSidplay2().getDefaultPlayLength());
			if (config.getSidplay2().isEnableDatabase()) {
				int length = tune != null ? getSidDatabaseInfo(db -> db
						.length(tune)) : 0;
				if (length > 0) {
					// ... or use song length of song length database
					timer.setStop(length);
				}
			}
		}
	}

	/**
	 * Play routine emulating a number of events (handle switches to next song
	 * etc.)
	 * 
	 * @throws InterruptedException
	 */
	private boolean play() throws InterruptedException {
		final int seconds = time();
		if (seconds != timer.getCurrent()) {
			timer.setCurrent(seconds);

			if (seconds == timer.getStart()) {
				if (sidBuilder != null) {
					// Switch from the NIL audio driver to the real audio driver
					sidBuilder.open();
				}
			}
			// Only for tunes: if play time is over loop or exit
			if (tune != null && timer.getStop() != 0
					&& seconds >= timer.getStart() + timer.getStop()) {
				if (config.getSidplay2().isSingle()) {
					stateProperty.set(getEndState());
				} else {
					// Check play-list end
					if (playList.hasNext()) {
						nextSong();
					} else {
						stateProperty.set(getEndState());
					}
				}
			}
		}
		if (stateProperty.get() == State.RUNNING) {
			try {
				for (int i = 0; i < NUM_EVENTS_TO_PLAY; i++) {
					c64.getEventScheduler().clock();
				}
			} catch (NaturalFinishedException e) {
				stateProperty.set(getEndState());
				throw e;
			}
		}
		return stateProperty.get() == State.RUNNING
		|| stateProperty.get() == State.PAUSED;
	}

	private State getEndState() {
		return config.getSidplay2().isLoop() ? State.RESTART : State.EXIT;
	}

	private void close() {
		if (sidBuilder != null) {
			for (int i = 0; i < C64.MAX_SIDS; i++) {
				SIDEmu s = c64.getSID(i);
				if (s != null) {
					sidBuilder.unlock(s);
					c64.setSID(i, null);
				}
			}
		}
		driverSettings.getAudio().getAudioDriver().close();
	}

	/**
	 * Play tune.
	 * 
	 * @param tune
	 *            file to play the tune (null means just reset C64)
	 */
	public final void play(final SidTune tune) {
		stopC64();
		setTune(tune);
		startC64();
	}

	public final void pause() {
		if (stateProperty.get() == State.PAUSED) {
			stateProperty.set(State.RUNNING);
		} else {
			stateProperty.set(State.PAUSED);
			driverSettings.getAudio().getAudioDriver().pause();
		}
	}

	public final void nextSong() {
		playList.next();
		stateProperty.set(State.RESTART);
	}

	public final void previousSong() {
		if (time() < SID2_PREV_SONG_TIMEOUT) {
			playList.previous();
		}
		stateProperty.set(State.RESTART);
	}

	public final void fastForward() {
		driverSettings.getAudio().getAudioDriver().fastForward();
	}

	public final void normalSpeed() {
		driverSettings.getAudio().getAudioDriver().normalSpeed();
	}

	public final void selectFirstTrack() {
		playList.first();
		stateProperty.set(State.RESTART);
	}

	public final void selectLastTrack() {
		playList.last();
		stateProperty.set(State.RESTART);
	}

	public final int getNumDevices() {
		return sidBuilder != null ? sidBuilder.getNumDevices() : 0;
	}

	public final void quit() {
		stateProperty.set(State.QUIT);
	}

	public final void setSidDatabase(SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	public final int getSidDatabaseInfo(ToIntFunction<SidDatabase> toIntFunction) {
		return sidDatabase != null ? toIntFunction.applyAsInt(sidDatabase) : 0;
	}

	public final void setSTIL(STIL stil) {
		this.stil = stil;
	}

	public final STILEntry getStilEntry(File file) {
		return stil != null && file != null ? stil.getSTILEntry(file) : null;
	}

	public final void setExtendImagePolicy(IExtendImageListener policy) {
		this.policy = policy;
	}

	/**
	 * Insert a disk into the first floppy disk drive.
	 * 
	 * @param file
	 *            disk file to insert
	 * @param autostartFile
	 *            auto-start tune after insertion (null means just do nothing)
	 * @throws IOException
	 *             image read error
	 */
	public final void insertDisk(final File file, final File autostartFile)
			throws IOException, SidTuneError {
		// automatically turn drive on
		config.getC1541().setDriveOn(true);
		enableFloppyDiskDrives(true);
		// attach selected disk into the first disk drive
		DiskImage disk = floppies[0].getDiskController().insertDisk(file);
		if (policy != null) {
			disk.setExtendImagePolicy(policy);
		}
		if (autostartFile != null) {
			play(SidTune.load(autostartFile));
		}
	}

	/**
	 * Insert a tape into the datasette.<BR>
	 * Note: If the file is different to the TAP format, it will be converted.
	 * 
	 * @param file
	 *            tape file to insert
	 * @param autostartFile
	 *            auto-start tune after insertion (null means just do nothing)
	 * @throws IOException
	 *             image read error
	 */
	public final void insertTape(final File file, final File autostartFile)
			throws IOException, SidTuneError {
		if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".tap")) {
			// Everything, which is not a tape convert to tape first
			final File convertedTape = new File(config.getSidplay2()
					.getTmpDir(), file.getName() + ".tap");
			convertedTape.deleteOnExit();
			SidTune prog = SidTune.load(file);
			String name = PathUtils.getBaseNameNoExt(file);
			PRG2TAPProgram program = new PRG2TAPProgram(prog, name);

			PRG2TAP prg2tap = new PRG2TAP();
			prg2tap.setTurboTape(true);
			prg2tap.open(convertedTape);
			prg2tap.add(program);
			prg2tap.close(convertedTape);

			datasette.insertTape(convertedTape);
		} else {
			datasette.insertTape(file);
		}
		if (autostartFile != null) {
			play(SidTune.load(autostartFile));
		}
	}

	/**
	 * Insert a cartridge of a given size with empty contents.
	 * 
	 * @param type
	 *            cartridge type
	 * @param sizeKB
	 *            size in KB
	 * @param autostartFile
	 *            auto-start tune after insertion (null means just reset)
	 * @throws IOException
	 *             never thrown here
	 */
	public final void insertCartridge(final CartridgeType type,
			final int sizeKB, final File autostartFile) throws IOException,
			SidTuneError {
		c64.ejectCartridge();
		c64.setCartridge(Cartridge.create(c64.getPla(), type, sizeKB));
		play(autostartFile != null ? SidTune.load(autostartFile) : null);
	}

	/**
	 * Insert a cartridge loading an image file.
	 * 
	 * @param type
	 *            cartridge type
	 * @param file
	 *            file to load the RAM contents
	 * @param autostartFile
	 *            auto-start tune after insertion (null means just reset)
	 * @throws IOException
	 *             image read error
	 */
	public final void insertCartridge(final CartridgeType type,
			final File file, final File autostartFile) throws IOException,
			SidTuneError {
		c64.ejectCartridge();
		c64.setCartridge(Cartridge.read(c64.getPla(), type, file));
		play(autostartFile != null ? SidTune.load(autostartFile) : null);
	}

}
