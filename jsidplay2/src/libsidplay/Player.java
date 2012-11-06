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
import java.io.IOException;
import java.io.InputStream;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types.CPUClock;
import libsidplay.common.SIDEmu;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.DisconnectedParallelCable;
import libsidplay.components.c1541.IParallelCable;
import libsidplay.components.c1541.VIACore;
import libsidplay.components.iec.IECBus;
import libsidplay.components.iec.SerialIECDevice;
import libsidplay.components.mos6510.IMOS6510Disassembler;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.VIC;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.sidtune.SidTune;
import resid_builder.ReSID;

/**
 * The player contains a C64 computer and additional peripherals.<BR>
 * It is meant as a complete setup (C64, tape/disk drive, carts and more).
 * 
 * @author Ken
 * 
 */
public class Player {

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
	protected String command;
	/**
	 * Is the printer enabled?
	 */
	protected boolean printerEnabled;
	/**
	 * Are the floppy disk drives enabled?
	 */
	protected boolean drivesEnabled;

	/**
	 * Create a complete setup (C64, tape/disk drive, carts and more).
	 */
	public Player() {
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
				if (printerEnabled) {
					printer.printerUserportWriteData(data);
				}
			}

			@Override
			public void printerUserportWriteStrobe(final boolean strobe) {
				if (printerEnabled) {
					printer.printerUserportWriteStrobe(strobe);
				}
			}

			@Override
			public byte readFromIECBus() {
				if (drivesEnabled) {
					c1541Runner.synchronize(0);
					return iecBus.readFromIECBus();
				}
				return (byte) 0x80;
			}

			@Override
			public void writeToIECBus(final byte data) {
				if (drivesEnabled) {
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
		connectC64AndC1541WithParallelCable(false);
	}

	/**
	 * Set frequency (PAL/NTSC)
	 * 
	 * @param cpuFreq
	 *            frequency (PAL/NTSC)
	 */
	public void setClock(final CPUClock cpuFreq) {
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
	public void reset() throws InterruptedException {
		c64.reset();
		iecBus.reset();
		datasette.reset();

		// Reset Floppies
		for (final C1541 floppy : floppies) {
			floppy.reset();
		}
		enableFloppyDiskDrives(drivesEnabled);
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
	public void play(final int numOfEvents) throws InterruptedException {
		for (int i = 0; i < numOfEvents; i++) {
			c64.getEventScheduler().clock();
		}
	}

	/**
	 * What is the current playing time.
	 * 
	 * @return the current playing time
	 */
	public int time() {
		final EventScheduler c = c64.getEventScheduler();
		return (int) (c.getTime(Phase.PHI2) / c.getCyclesPerSecond());
	}

	/**
	 * Mute a specific voice of a SID chip
	 * 
	 * @param sidNum
	 *            SID chip number (0..1)
	 * @param voice
	 *            voice to mute (0..2)
	 * @param enable
	 *            mute enable
	 */
	public void mute(final int sidNum, final int voice, final boolean enable) {
		final SIDEmu s = c64.getSID(sidNum);
		if (s != null) {
			s.setEnabled(voice, enable);
		}
	}

	public void turnPrinterOnOff(final boolean on) {
		printerEnabled = on;
		printer.turnPrinterOnOff(on);
	}

	/**
	 * Enable floppy disk drives.
	 * 
	 * @param on
	 *            floppy disk drives enable
	 */
	public void enableFloppyDiskDrives(final boolean on) {
		for (final C1541 floppy : floppies) {
			floppy.setPowerOn(on);
		}
		if (on) {
			c64.getEventScheduler().scheduleThreadSafe(
					new Event("Begin C64-C1541 sync") {
						@Override
						public void event() {
							c1541Runner.reset();
							drivesEnabled = on;
						}
					});
		} else {
			c64.getEventScheduler().scheduleThreadSafe(
					new Event("End C64-C1541 sync") {
						@Override
						public void event() {
							c1541Runner.cancel();
							drivesEnabled = on;
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
	public void connectC64AndC1541WithParallelCable(final boolean connected) {
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
	private final IParallelCable makeCableBetweenC64AndC1541() {
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
	public void installJiffyDOS(final InputStream c64KernalStream,
			final InputStream c1541KernalStream) throws IOException {
		DataInputStream dis = null;
		byte[] c64Kernal = new byte[0x2000];
		try {
			dis = new DataInputStream(c64KernalStream);
			dis.readFully(c64Kernal);
			getC64().setCustomKernal(c64Kernal);
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
	public void uninstallJiffyDOS() throws IOException {
		getC64().setCustomKernal(null);
		for (final C1541 floppy : floppies) {
			floppy.setCustomKernalRom(null);
		}
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
	public Datasette getDatasette() {
		return datasette;
	}

	/**
	 * Get C1541 floppies.
	 * 
	 * @return C1541 floppies
	 */
	public C1541[] getFloppies() {
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
	public void setCommand(final String cmd) {
		this.command = cmd;
	}

	/**
	 * Load a program to play.
	 * 
	 * @param tune
	 *            program to play
	 */
	public void setTune(final SidTune tune) {
		this.tune = tune;
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
	public String getCredits() {
		final StringBuffer credits = new StringBuffer();
		credits.append("Java Version and User Interface (v2.5):\n"
				+ "\tCopyright (C) 2007-2011 Ken Händel\n"
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
		credits.append("MP3 downloads from Stone Oakvalley's Authentic SID Collection (SOASC=):\n"
				+ "\thttp://www.6581-8580.com/\n");
		credits.append("This product includes software developed by the SWIXML Project\n"
				+ "\thttp://www.swixml.org/\n");
		credits.append("Drag and Drop support: Robert Harder\n"
				+ "\thttp://iharder.sourceforge.net\n");
		credits.append("PSID to PRG converter (PSID64 v0.9):\n"
				+ "\tCopyright (C) 2001-2007  Roland Hermans\n"
				+ "\thttp://sourceforge.net/projects/psid64/\n");
		credits.append("Video-Capture (QuickTimeDemo):\n"
				+ "\tCopyright (C) 2008-2010  Werner Randelshofer\n"
				+ "\thttp://www.randelshofer.ch/\n");
		credits.append("An Optimizing Hybrid LZ77 RLE Data Compression Program (Pucrunch 22.11.2008):\n"
				+ "\tCopyright (C) 1997-2008 Pasi 'Albert' Ojala\n"
				+ "\thttp://www.cs.tut.fi/~albert/Dev/pucrunch/\n");
		credits.append("SID dump file (SIDDump V1.04):\n"
				+ "\tCopyright (C) 2007 Lasse Öörni\n");
		credits.append("HVSC playroutine identity scanner (SIDId V1.07):\n"
				+ "\tCopyright (C) 2007 Lasse Öörni\n");
		credits.append("High Voltage Music Engine Collection (HVMEC V1.0):\n"
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

}

abstract class C1541Runner extends Event {
	protected final EventScheduler c64Context, c1541Context;
	private int conversionFactor, accum;
	private long c64LastTime;

	public C1541Runner(final EventScheduler c64Context,
			final EventScheduler c1541Context) {
		super("C64 permits C1541 to continue");
		this.c64Context = c64Context;
		this.c1541Context = c1541Context;
		this.c64LastTime = c64Context.getTime(Phase.PHI2);
	}

	/**
	 * Return the number of clock ticks that 1541 should advance.
	 * 
	 * @param offset
	 *            adjust C64 cycles
	 * 
	 * @return
	 */
	protected int updateSlaveTicks(long offset) {
		final long oldC64Last = c64LastTime;
		c64LastTime = c64Context.getTime(Phase.PHI2) + offset;

		accum += conversionFactor * (int) (c64LastTime - oldC64Last);
		int wholeClocks = accum >> 16;
		accum &= 0xffff;

		return wholeClocks;
	}

	protected void setClockDivider(final CPUClock clock) {
		conversionFactor = (int) (1000000.0 / clock.getCpuFrequency() * 65536.0 + 0.5);
	}

	protected void reset() {
		c64LastTime = c64Context.getTime(Phase.PHI2);
	}

	abstract protected void cancel();

	abstract protected void synchronize(long offset);
}

class SameThreadC1541Runner extends C1541Runner {
	protected boolean notTerminated;

	private final Event terminationEvent = new Event("Pause C1541") {
		@Override
		public void event() {
			notTerminated = false;
		}
	};

	protected SameThreadC1541Runner(final EventScheduler c64Context,
			final EventScheduler c1541Context) {
		super(c64Context, c1541Context);
	}

	private void clockC1541Context(long offset) {
		final int targetTime = updateSlaveTicks(offset);
		if (targetTime <= 0) {
			return;
		}

		c1541Context.schedule(terminationEvent, targetTime, Event.Phase.PHI2);
		notTerminated = true;

		/*
		 * This should actually never throw InterruptedException, because the
		 * only kind of Events that throw it are related to audio production.
		 */
		try {
			while (notTerminated) {
				c1541Context.clock();
			}
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void reset() {
		super.reset();
		cancel();
		c64Context.schedule(this, 0, Event.Phase.PHI2);
	}

	@Override
	protected void cancel() {
		c64Context.cancel(this);
	}

	@Override
	protected void synchronize(long offset) {
		clockC1541Context(offset);
	}

	@Override
	public void event() throws InterruptedException {
		synchronize(0);
		c64Context.schedule(this, 2000);
	}

}