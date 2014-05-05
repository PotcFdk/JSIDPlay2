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

import hardsid_builder.HardSID;
import hardsid_builder.HardSIDBuilder;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import libsidplay.components.iec.IECBus;
import libsidplay.components.iec.SerialIECDevice;
import libsidplay.components.mos6510.IMOS6510Disassembler;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.player.DriverSettings;
import libsidplay.player.Emulation;
import libsidplay.player.MediaType;
import libsidplay.player.State;
import libsidplay.player.Timer;
import libsidplay.player.Track;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PRG2TAP;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import resid_builder.ReSID;
import resid_builder.ReSIDBuilder;
import resid_builder.resid.ChipModel;
import resid_builder.resid.SamplingMethod;
import sidplay.audio.AudioConfig;
import sidplay.audio.CmpMP3File;
import sidplay.audio.NaturalFinishedException;
import sidplay.audio.Output;
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
	/** Previous song select timeout (< 4 secs) **/
	private static final int SID2_PREV_SONG_TIMEOUT = 4;
	private static final int MAX_SPEED = 32;

	private IConfig config;

	/**
	 * C64 computer.
	 */
	protected final C64 c64;
	/**
	 * C1530 datasette.
	 */
	protected final Datasette datasette;
	/**
	 * IEC bus.
	 */
	protected final IECBus iecBus;
	/**
	 * Additional serial devices like a printer (except of the floppies).
	 */
	protected final SerialIECDevice[] serialDevices;
	/**
	 * C1541 floppy disk drives.
	 */
	protected final C1541[] floppies;
	/**
	 * Responsible to keep C64 and C1541 in sync.
	 */
	protected final C1541Runner c1541Runner;
	/**
	 * MPS803 printer.
	 */
	protected final MPS803 printer;
	/**
	 * Currently played tune.
	 */
	protected SidTune tune;
	/**
	 * Autostart command to be typed-in after reset.
	 */
	private String command;

	private Thread fPlayerThread;
	private Consumer<Player> menuHook = (player) -> {
	};
	private Consumer<Player> interactivityHook = (player) -> {
	};
	private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<State>(
			State.STOPPED);
	private final Timer timer = new Timer();
	private final Track track = new Track();
	private int currentSpeed = 1;

	private DriverSettings driverSettings = new DriverSettings(
			Output.OUT_SOUNDCARD, Emulation.EMU_RESID);
	private DriverSettings oldDriverSettings;
	private SIDBuilder sidBuilder;

	private STIL stil;
	private SidDatabase sidDatabase;
	private IExtendImageListener policy;

	/**
	 * Create a complete setup (C64, tape/disk drive, carts and more).
	 */
	public Player(IConfig config) {
		this.config = config;
		iecBus = new IECBus();

		printer = new MPS803(iecBus, (byte) 4, (byte) 7) {
			@Override
			public void setBusy(final boolean flag) {
				c64.cia2.setFlag(flag);
			}

			@Override
			public long clk() {
				return c64.context.getTime(Phase.PHI2);
			}

		};

		c64 = new C64() {

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

		datasette = new Datasette(c64.getEventScheduler()) {
			@Override
			public void setFlag(final boolean flag) {
				c64.cia1.setFlag(flag);
			}
		};

		final C1541 c1541 = new C1541(iecBus, 8, FloppyType.C1541);

		floppies = new C1541[] { c1541 };
		serialDevices = new SerialIECDevice[] { printer };

		iecBus.setFloppies(floppies);
		iecBus.setSerialDevices(serialDevices);
		c1541Runner = new SameThreadC1541Runner(c64.getEventScheduler(),
				c1541.getEventScheduler());
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
	public final void reset() throws InterruptedException {
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
		enablePrinter(config.getPrinter().isPrinterOn());

		enableFloppyDiskDrives(config.getC1541().isDriveOn());
		connectC64AndC1541WithParallelCable(config.getC1541().isParallelCable());
		// Reset IEC devices
		for (final SerialIECDevice serialDevice : serialDevices) {
			serialDevice.reset();
		}

		/* Autostart program, if we have one. */
		if (tune != null) {
			/* Set playback addr to feedback call frames counter. */
			c64.setPlayAddr(tune.getInfo().playAddr);
			/*
			 * This is a bit ugly: starting PRG must be done after C64 system
			 * reset, while starting SID is done by CBM80 hook.
			 */
			c64.getEventScheduler().schedule(new Event("Tune init event") {
				@Override
				public void event() throws InterruptedException {
					final byte[] ram = c64.getRAM();
					final int address = tune.placeProgramInMemory(ram);
					if (address != -1) {
						/*
						 * Set initial volume for psid. Ideally the driver would
						 * do this for us, but whatever...
						 */
						for (int i = 0; i < C64.MAX_SIDS; i++) {
							final SIDEmu s = c64.getSID(i);
							if (s != null) {
								s.write(0x18, (byte) 0xf);
							}
						}
						c64.getCPU().forcedJump(address);
					} else {
						ram[0x277] = (byte) 'R';
						ram[0x278] = (byte) 'U';
						ram[0x279] = (byte) 'N';
						ram[0x27a] = (byte) ':';
						ram[0x27b] = 13;
						ram[0xc6] = 5;
					}
				}
			}, tune.getInitDelay());
		} else {
			// Normal reset code path
			// However we want an autostart anyway
			c64.getEventScheduler().schedule(new Event("Autostart event") {
				@Override
				public void event() throws InterruptedException {
					if (command != null) {
						final byte[] ram = c64.getRAM();
						for (int i = 0; i < Math.min(command.length(), 16); i++) {
							ram[0x277 + i] = (byte) command.charAt(i);
						}
						ram[0xc6] = (byte) Math.min(command.length(), 16);
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

	/**
	 * Run C64 emulation for a specific amount of events.
	 * 
	 * @throws InterruptedException
	 */
	public final void play(final int numOfEvents) throws InterruptedException {
		for (int i = 0; i < numOfEvents; i++) {
			c64.getEventScheduler().clock();
		}
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
	 * @param c64KernalStream
	 *            C64 Kernal replacement
	 * @param c1541KernalStream
	 *            C1541 Kernal replacement
	 * @throws IOException
	 *             error reading the ROMs
	 */
	public final void installJiffyDOS(final InputStream c64KernalStream,
			final InputStream c1541KernalStream) throws IOException {
		DataInputStream dis = null;
		byte[] c64Kernal = new byte[0x2000];
		try {
			dis = new DataInputStream(c64KernalStream);
			dis.readFully(c64Kernal);
			c64.setCustomKernal(c64Kernal);
		} catch (IOException e) {
			throw e;
		} finally {
			if (dis != null) {
				dis.close();
			}
		}
		dis = null;
		byte[] c1541Kernal = new byte[0x4000];
		try {
			dis = new DataInputStream(c1541KernalStream);
			dis.readFully(c1541Kernal);
			for (final C1541 floppy : floppies) {
				floppy.setCustomKernalRom(c1541Kernal);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (dis != null) {
				dis.close();
			}
		}
	}

	/**
	 * Uninstall Jiffy DOS floppy speeder.
	 */
	public final void uninstallJiffyDOS() throws IOException {
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
	 * @param disassembler
	 *            opcode stringifier to produce CPU debug output.
	 */
	public final void setDebug(final IMOS6510Disassembler disassembler) {
		c64.getCPU().setDebug(disassembler);
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
	 * @param cmd
	 *            basic command after reset
	 */
	public final void setCommand(final String cmd) {
		this.command = cmd;
	}

	/**
	 * Load a program to play.
	 * 
	 * @param tune
	 *            program to play
	 */
	public final void setTune(final SidTune tune) {
		this.tune = tune;
	}

	public final Track getTrack() {
		return track;
	}

	public final Timer getTimer() {
		return timer;
	}

	/**
	 * Get the currently played program.
	 * 
	 * @return the currently played program
	 */
	public final SidTune getTune() {
		return tune;
	}

	/**
	 * The credits for the authors of many parts of this emulator.
	 * 
	 * @return the credits
	 */
	public final String getCredits() {
		final StringBuffer credits = new StringBuffer();
		credits.append("Java Version and User Interface v3.0:\n"
				+ "\tCopyright (C) 2007-2014 Ken Händel\n"
				+ "\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("Distortion Simulation and development: Antti S. Lankila\n"
				+ "\thttp://bel.fi/~alankila/c64-sw/\n");
		credits.append("Network SID Device:\n"
				+ "\tSupported by Wilfred Bos, The Netherlands\n"
				+ "\thttp://www.acid64.com\n");
		credits.append("Testing and Feedback: Nata, founder of proNoise\n"
				+ "\thttp://www.nata.netau.net/\n");
		credits.append("graphical output:\n" + "\t(C) 2007 Joakim Eriksson\n"
				+ "\t(C) 2009, 2010 Antti S. Lankila\n");
		credits.append("MP3 encoder/decoder (jump3r), based on Lame\n"
				+ "\tCopyright (C) 2010-2011  Ken Händel\n"
				+ "\thttp://sourceforge.net/projects/jsidplay2/\n");
		credits.append("This product uses the database of Game Base 64 (GB64)\n"
				+ "\thttp://www.gb64.com/\n");
		credits.append("MP3 downloads from Stone Oakvalley's Authentic SID MusicCollection (SOASC=):\n"
				+ "\thttp://www.6581-8580.com/\n");
		credits.append("PSID to PRG converter (PSID64 v0.9):\n"
				+ "\tCopyright (C) 2001-2007  Roland Hermans\n"
				+ "\thttp://sourceforge.net/projects/psid64/\n");
		credits.append("An Optimizing Hybrid LZ77 RLE Data Compression Program (Pucrunch 22.11.2008):\n"
				+ "\tCopyright (C) 1997-2008 Pasi 'Albert' Ojala\n"
				+ "\thttp://www.cs.tut.fi/~albert/Dev/pucrunch/\n");
		credits.append("SID dump file (SIDDump V1.04):\n"
				+ "\tCopyright (C) 2007 Lasse Öörni\n");
		credits.append("HVSC playroutine identity scanner (SIDId V1.07):\n"
				+ "\tCopyright (C) 2007 Lasse Öörni\n");
		credits.append("High Voltage Music Engine MusicCollection (HVMEC V1.0):\n"
				+ "\tCopyright (C) 2011 by Stefano Tognon and Stephan Parth\n");
		credits.append("C1541 Floppy Disk Drive Emulation:\n"
				+ "\tCopyright (C) 2010 VICE (the Versatile Commodore Emulator)\n"
				+ "\thttp://www.viceteam.org/\n");
		credits.append("Based on libsidplay v2.1.1 engine:\n"
				+ "\tCopyright (C) 2000 Simon White sidplay2@yahoo.com\n"
				+ "\thttp://sidplay2.sourceforge.net\n");
		credits.append(MOS6510.credits());
		credits.append(MOS6526.credits());
		credits.append(VIC.credits());
		credits.append(HardSIDBuilder.credits());
		credits.append(ReSID.credits());
		return credits.toString();
	}

	/** Stereo SID at 0xd400 hack */
	private void handleStereoSIDConflict(Integer secondAddress) {
		if (Integer.valueOf(0xd400).equals(secondAddress)) {
			final SIDEmu s1 = c64.getSID(0);
			final SIDEmu s2 = c64.getSID(1);
			c64.setSID(0, new SIDEmu(c64.getEventScheduler()) {
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
				public void setVoiceMute(int num, boolean mute) {
					s1.setVoiceMute(num, mute);
				}

				@Override
				public void setFilter(IConfig config) {
					s1.setFilter(config);
				}

				@Override
				public void setFilter(boolean enable) {
					s1.setFilter(enable);
				}

				@Override
				public ChipModel getChipModel() {
					return s1.getChipModel();
				}

				@Override
				public void setChipModel(ChipModel model) {
					s1.setChipModel(model);
					s2.setChipModel(model);
				}

				@Override
				public void setSampling(double cpuFrequency, float frequency,
						SamplingMethod sampling) {
					s1.setSampling(cpuFrequency, frequency, sampling);
					s2.setSampling(cpuFrequency, frequency, sampling);
				}

				@Override
				public void input(int input) {
					s1.input(input);
					s2.input(input);
				}
			});
		}
	}

	public final void setSampling(CPUClock systemFrequency, float cpuFreq,
			SamplingMethod method) {
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu s = c64.getSID(i);
			if (s != null) {
				s.setSampling(systemFrequency.getCpuFrequency(), cpuFreq,
						method);
			}
		}
	}

	public final void setDigiBoost(boolean selected) {
		final int input = selected ? 0x7FF : 0;
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu s = c64.getSID(i);
			if (s != null) {
				s.input(input);
			}
		}
	}

	public final void setFilterEnable(boolean filterEnable) {
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu s = c64.getSID(i);
			if (s != null) {
				s.setFilter(filterEnable);
			}
		}
	}

	public final void setFilter() {
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu s = c64.getSID(i);
			if (s != null) {
				s.setFilter(config);
			}
		}
	}

	public final void setMute(int chipNum, int voiceNum, boolean mute) {
		final SIDEmu s = c64.getSID(chipNum);
		if (s != null) {
			s.setVoiceMute(voiceNum, mute);
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
				// This is only the last option, if the player can not be
				// stopped clean
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

	public final void setDriverSettings(DriverSettings driverSettings) {
		this.driverSettings = driverSettings;
	}

	public final DriverSettings getDriverSettings() {
		return driverSettings;
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
					// Open tune and play
					open();
					menuHook.accept(Player.this);
					// Play next chunk of sound data, until it gets stopped
					while (true) {
						// Pause? sleep for awhile
						if (stateProperty.get() == State.PAUSED) {
							Thread.sleep(250);
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

	/**
	 * Out play loop to be externally called
	 * 
	 * @throws InterruptedException
	 */
	private boolean play() throws InterruptedException {
		/* handle switches to next song etc. */
		final int seconds = time();
		if (seconds != timer.getCurrent()) {
			timer.setCurrent(seconds);

			if (seconds == timer.getStart()) {
				normalSpeed();
				sidBuilder.setDriver(driverSettings.getOutput().getDriver(),
						config.getSidplay2().getTmpDir());
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
				play(10000);
			} catch (NaturalFinishedException e) {
				stateProperty.set(State.EXIT);
				throw e;
			}
		}
		return stateProperty.get() == State.RUNNING
				|| stateProperty.get() == State.PAUSED;
	}

	private void open() throws InterruptedException {
		if (stateProperty.get() == State.RESTART) {
			stateProperty.set(State.STOPPED);
		}

		// Select the required song
		if (tune != null) {
			track.setSelected(tune.selectSong(track.getSelected()));
			if (track.getFirst() == 0) {
				// A different tune is opened?
				// We mark a new play-list start
				track.setFirst(track.getSelected());
			}
		}
		setTune(tune);

		CPUClock cpuFreq = CPUClock.getCPUClock(config, tune);
		setClock(cpuFreq);

		if (oldDriverSettings != null) {
			// restore settings after MP3 has been played last time
			driverSettings.restore(oldDriverSettings);
			oldDriverSettings = null;
		}
		if (tune != null
				&& tune.getInfo().file != null
				&& tune.getInfo().file.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".mp3")) {
			// MP3 play-back? Save settings, then change to MP3 compare driver
			oldDriverSettings = driverSettings.save();

			driverSettings.setOutput(Output.OUT_COMPARE);
			driverSettings.setEmulation(Emulation.EMU_RESID);
			config.getAudio().setPlayOriginal(true);
			config.getAudio().setMp3File(tune.getInfo().file.getAbsolutePath());
		}
		if (driverSettings.getOutput().getDriver() instanceof CmpMP3File) {
			// Set MP3 comparison settings
			CmpMP3File cmpMp3Driver = (CmpMP3File) driverSettings.getOutput()
					.getDriver();
			cmpMp3Driver.setPlayOriginal(config.getAudio().isPlayOriginal());
			cmpMp3Driver.setMp3File(new File(config.getAudio().getMp3File()));
		}
		int channels = isStereo() ? 2 : 1;
		final AudioConfig audioConfig = AudioConfig.getInstance(
				config.getAudio(), channels);
		audioConfig.configure(tune);
		try {
			driverSettings.getOutput().getDriver()
					.open(audioConfig, config.getSidplay2().getTmpDir());
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		sidBuilder = null;
		switch (driverSettings.getEmulation()) {
		case EMU_RESID:
			sidBuilder = new ReSIDBuilder(audioConfig,
					cpuFreq.getCpuFrequency(), config.getAudio()
							.getLeftVolume(), config.getAudio()
							.getRightVolume());
			break;

		case EMU_HARDSID:
			sidBuilder = new HardSIDBuilder(config);
			break;

		default:
			break;
		}

		// According to the configuration, the SIDs must be updated.
		updateChipModel();
		setFilter();

		/* We should have our SIDs configured now. */

		Integer secondAddress = getSecondAddress();
		if (secondAddress != null && secondAddress != 0xd400) {
			c64.setSecondSIDAddress(secondAddress);
		}

		handleStereoSIDConflict(secondAddress);

		// Start the player. Do this by fast
		// forwarding to the start position
		currentSpeed = MAX_SPEED;
		driverSettings.getOutput().getDriver().setFastForward(currentSpeed);

		reset();

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
	}

	private boolean isStereo() {
		return config.getEmulation().isForceStereoTune() || tune != null
				&& tune.getInfo().sidChipBase2 != 0;
	}

	private Integer getSecondAddress() {
		if (isStereo()) {
			if (config.getEmulation().isForceStereoTune()) {
				return config.getEmulation().getDualSidBase();
			} else if (tune != null && tune.getInfo().sidChipBase2 != 0) {
				return tune.getInfo().sidChipBase2;
			}
		}
		return null;
	}

	/**
	 * Set/Update chip model according to the configuration
	 */
	public final void updateChipModel() {
		if (sidBuilder != null) {
			ChipModel chipModel = ChipModel.getChipModel(config, tune);
			updateChipModel(0, chipModel);

			if (isStereo()) {
				ChipModel stereoChipModel = ChipModel.getStereoSIDModel(config,
						tune);
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
		SIDEmu s = c64.getSID(chipNum);
		if (s instanceof HardSID) {
			sidBuilder.unlock(s);
			s = null;
		}
		if (s == null) {
			s = sidBuilder.lock(c64.getEventScheduler(), model);
		} else {
			s.setChipModel(model);
		}
		c64.setSID(chipNum, s);
	}

	/**
	 * Set stop time according to the song length database (or use default
	 * length)
	 * 
	 * @param enableSongLengthDatabase
	 *            enable song length database
	 */
	public final void setStopTime(boolean enableSongLengthDatabase) {
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
		driverSettings.getOutput().getDriver().close();
	}

	/**
	 * Play tune.
	 * 
	 * @param sidTune
	 *            file to play the tune (null means just reset C64)
	 * @param command
	 */
	public final void playTune(final SidTune sidTune, String command) {
		tune = sidTune;
		// Stop previous run
		stopC64();
		// 0 means use start song next time open() is called
		track.setSelected(0);
		if (tune != null) {
			// A different tune is opened?
			// We mark a new play-list start
			track.setFirst(0);
			track.setSongs(tune.getInfo().songs);
		} else {
			track.setFirst(1);
			track.setSongs(0);
		}
		// set command to type after reset
		setCommand(command);
		// Start emulation
		startC64();
	}

	public final void pause() {
		if (stateProperty.get() == State.PAUSED) {
			stateProperty.set(State.RUNNING);
		} else {
			stateProperty.set(State.PAUSED);
			driverSettings.getOutput().getDriver().pause();
		}
	}

	public final void nextSong() {
		stateProperty.set(State.RESTART);
		track.setSelected(track.getSelected() + 1);
		if (track.getSelected() > track.getSongs()) {
			track.setSelected(1);
		}
	}

	public final void previousSong() {
		stateProperty.set(State.RESTART);
		if (time() < SID2_PREV_SONG_TIMEOUT) {
			track.setSelected(track.getSelected() - 1);
			if (track.getSelected() < 1) {
				track.setSelected(track.getSongs());
			}
		}
	}

	public final void fastForward() {
		currentSpeed = currentSpeed * 2;
		if (currentSpeed > MAX_SPEED) {
			currentSpeed = MAX_SPEED;
		}
		driverSettings.getOutput().getDriver().setFastForward(currentSpeed);
	}

	public final void normalSpeed() {
		currentSpeed = 1;
		driverSettings.getOutput().getDriver().setFastForward(1);
	}

	public final void selectFirstTrack() {
		stateProperty.set(State.RESTART);
		track.setSelected(1);
	}

	public final void selectLastTrack() {
		stateProperty.set(State.RESTART);
		track.setSelected(track.getSongs());
	}

	public final int getCurrentSong() {
		return track.getSelected();
	}

	public final int getNumDevices() {
		return sidBuilder != null ? sidBuilder.getNumDevices() : 0;
	}

	public final void quit() {
		stateProperty.set(State.QUIT);
	}

	public final void setSIDVolume(int i, float volumeDb) {
		if (sidBuilder != null) {
			sidBuilder.setSIDVolume(i, volumeDb);
		}
	}

	public final void setSidDatabase(SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	public final int getSongLength(SidTune t) {
		if (t != null && sidDatabase != null) {
			return sidDatabase.length(t);
		}
		return -1;
	}

	public final int getFullSongLength(SidTune t) {
		if (t != null && sidDatabase != null) {
			return sidDatabase.getFullSongLength(t);
		}
		return 0;
	}

	public final void setSTIL(STIL stil) {
		this.stil = stil;
	}

	public final STIL getStil() {
		return stil;
	}

	public final void setExtendImagePolicy(IExtendImageListener policy) {
		this.policy = policy;
	}

	public final void insertMedia(File mediaFile, File autostartFile,
			MediaType mediaType) {
		try {
			config.getSidplay2().setLastDirectory(
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
		config.getC1541().setDriveOn(true);
		enableFloppyDiskDrives(true);
		// attach selected disk into the first disk drive
		DiskImage disk = getFloppies()[0].getDiskController().insertDisk(
				selectedDisk);
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
			final File convertedTape = new File(config.getSidplay2()
					.getTmpDir(), selectedTape.getName() + ".tap");
			convertedTape.deleteOnExit();
			String[] args = new String[] { selectedTape.getAbsolutePath(),
					convertedTape.getAbsolutePath() };
			PRG2TAP.main(args);
			getDatasette().insertTape(convertedTape);
		} else {
			getDatasette().insertTape(selectedTape);
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
		c64.insertCartridge(selectedFile);
		// reset required after inserting the cartridge
		playTune(null, null);
	}

}