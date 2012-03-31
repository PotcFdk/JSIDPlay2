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

import static libsidplay.common.ISID2Types.sid2_model_t.SID2_MODEL_CORRECT;
import static libsidplay.common.ISID2Types.sid2_model_t.SID2_MOS6581;
import static libsidplay.common.ISID2Types.sid2_model_t.SID2_MOS8580;
import static libsidplay.common.ISID2Types.sid2_playback_t.sid2_mono;
import static libsidplay.common.ISID2Types.sid2_playback_t.sid2_stereo;
import static libsidplay.mem.IBasic.BASIC;
import static libsidplay.mem.IChar.CHAR;
import static libsidplay.mem.IKernal.KERNAL;

import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types.sid2_clock_t;
import libsidplay.common.ISID2Types.sid2_config_t;
import libsidplay.common.ISID2Types.sid2_model_t;
import libsidplay.components.mos6510.CPUEnvironment;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.C64CIA;
import libsidplay.components.mos6526.CIAEnvironment;
import libsidplay.components.mos656x.OldMOS656X;
import libsidplay.components.mos656x.VICEnvironment;
import libsidplay.components.sidtune.SidTune;
import libsidplay.components.sidtune.SidTuneInfo;
import libsidplay.mem.IKernal;
import resid_builder.ReSID;

public class Player implements CPUEnvironment, VICEnvironment, CIAEnvironment {
	/** C64 RAM area */
	private final byte m_ram[] = new byte[65536];
	/** C64 ROM area */
	private final byte m_colorRam[] = new byte[1024];

	/**
	 * Read memory as seen by CPU, respecting IO map.
	 */
	public byte cpuReadMemory(final int addr) {
		final byte value;
		switch (addr >> 12) {
		case 0x0:
			switch (addr) {
			case 0:
				value = pport.getDirRead();
				break;
			case 1:
				value = pport.getDataRead();
				break;
			default:
				value = m_ram[addr];
				break;
			}
			break;

		default:
			value = m_ram[addr];
			break;

		case 0xa:
		case 0xb:
			if (pport.isBasic()) {
				value = BASIC[addr & 0x1fff];
			} else {
				value = m_ram[addr];
			}
			break;

		case 0xd:
			if (pport.isIoArea()) {
				switch (addr >> 8) {
				default:
					value = -1;
					break;

				case 0xd0:
				case 0xd1:
				case 0xd2:
				case 0xd3:
					value = vic.read(addr & 0x3f);
					break;

				case 0xd4:
				case 0xd5:
				case 0xd6:
				case 0xd7: {
					final int i = m_sidmapper[addr >> 5 & SID2_MAPPER_SIZE - 1];
					value = sid[i].read(addr & 0x1f);
					break;
				}

				case 0xd8:
				case 0xd9:
				case 0xda:
				case 0xdb:
					value = m_colorRam[addr & 0x3ff];
					break;

				case 0xdc:
					value = cia1.read(addr & 0x0f);
					break;
				case 0xdd:
					value = cia2.read(addr & 0x0f);
					break;

				case 0xde:
				case 0xdf:
					value = (byte) 0xff;
					break;
				}
			} else if (pport.isCharacter()) {
				value = CHAR[addr & 0xfff];
			} else {
				value = m_ram[addr];
			}
			break;

		case 0xe:
		case 0xf:
			if (pport.isKernal()) {
				value = KERNAL[addr & 0x1fff];
			} else {
				value = m_ram[addr];
			}
			break;
		}

		return value;
	}

	/**
	 * Write memory as seen by CPU, respecting IO map.
	 */
	public void cpuWriteMemory(final int addr, final byte value) {
		switch (addr >> 12) {
		case 0:
			switch (addr) {
			case 0:
				pport.setDir(value);
				break;

			case 1:
				pport.setData(value);
				break;

			default:
				m_ram[addr] = value;
				break;
			}
			break;

		default:
			m_ram[addr] = value;
			break;

		case 0xd:
			if (pport.isIoArea()) {
				switch (addr >> 8) {
				case 0xd0:
				case 0xd1:
				case 0xd2:
				case 0xd3:
					vic.write(addr & 0x3f, value);
					break;

				case 0xd4:
				case 0xd5:
				case 0xd6:
				case 0xd7:
					final int i = m_sidmapper[addr >> 5 & SID2_MAPPER_SIZE - 1];
					sid[i].write(addr & 0x1f, value);
					break;

				case 0xd8:
				case 0xd9:
				case 0xda:
				case 0xdb:
					m_colorRam[addr & 0x3ff] = (byte) (value & 0xf);
					break;

				case 0xdc:
					cia1.write(addr & 0x0f, value);
					break;

				case 0xdd:
					cia2.write(addr & 0x0f, value);
					break;

				case 0xde:
				case 0xdf:
					/* 0xdexx, 0xdfxx are expansion cartridge areas. Writes go nowhere, reads read bus junk. */
					break;
				}
			} else {
				m_ram[addr] = value;
			}
			break;
		}
	}

	public static final double CLOCK_FREQ_NTSC = 1022727.14;

	public static final double CLOCK_FREQ_PAL = 985248.4;

	private static final double VIC_FREQ_PAL = 50.0;

	private static final double VIC_FREQ_NTSC = 60.0;

	/**
	 * MOS6510 / SID6510 cpu.
	 * SID extensions are enabled only for not real c64 environmentss.
	 */
	private final MOS6510 cpu;

	private final C64CIA.C64CIA1 cia1;

	private final C64CIA.C64CIA2 cia2;

	private final OldMOS656X vic;

	private final ReSID sid[] = new ReSID[SID2_MAX_SIDS];

	/**
	 * Mapping table in d4xx-d7xx
	 */
	private final int m_sidmapper[] = new int[32];

	protected class MixerEvent extends Event {
		/* Scheduling time for next sample mixing event. 2500 is roughly 2.5 ms,
		 * and this rate is needed to support 96 kHz stereo tunes. */
		protected static final int MIXER_EVENT_RATE = 2500;

		private int fastForwardFactor;
		private int sampleIndex;
		private short[] sampleBuffer;
		
		protected MixerEvent() {
			super("Mixer");
		}
		
		protected void begin(short[] buffer) {
			sampleIndex = 0;
			sampleBuffer = buffer;
		}
		
		protected boolean notFinished() {
			return sampleIndex != sampleBuffer.length;
		}
		
		protected int samplesGenerated() {
			return sampleIndex;
		}
		
		@Override
		public void event() {
			final ReSID chip1 = sid[0];
			final ReSID chip2 = sid[1];
			
			/* this clocks the SID to the present moment, if it isn't already. */
			chip1.clock();
			final short[] buf1 = chip1.buffer();
			short[] buf2 = null;
			if (chip2 != null) {
				chip2.clock();
				buf2 = chip2.buffer();
			}
			
			/* extract buffer info now that the SID is updated.
			 * clock() may update bufferpos. */
			final int samples = chip1.bufferpos();
			/* If chip2 exists, its bufferpos is expected to be identical to chip1's. */
			
			int i = 0;
			while (i < samples) {
				/* Handle whatever output the sid has generated so far */
				if (sampleIndex == sampleBuffer.length) {
					break;
				}
				/* Are there enough samples to generate the next one? */
				if (i + fastForwardFactor >= samples) {
					break;
				}
			
				/* This is a crude boxcar low-pass filter to
				 * reduce aliasing during fast forward, something I commonly do. */
				int sample1 = 0;
				int sample2 = 0;
				int j;
				for (j = 0; j < fastForwardFactor; j += 1) {
					if (buf1 != null) {
						sample1 += buf1[i + j];
					}
					if (buf2 != null) {
						sample2 += buf2[i + j];
					}
				}
				/* increment i to mark we ate some samples, finish the boxcar thing. */
				i += j;
				sample1 /= j;
				sample2 /= j;
				/* mono mix. */
				if (buf2 != null && m_cfg.playback != sid2_stereo) {
					sample1 = (sample1 + sample2) >> 1;
				}
				/* stereo clone, for people who keep stereo on permanently. */
				if (buf2 == null && m_cfg.playback == sid2_stereo) {
					sample2 = sample1;
				}
			
				sampleBuffer[sampleIndex ++] = (short) sample1;
				if (m_cfg.playback == sid2_stereo) {
					sampleBuffer[sampleIndex ++] = (short) sample2;
				}
			}
			/* move the unhandled data to start of buffer, if any. */
			int j = 0;
			for (j = 0; j < samples - i; j += 1) {
				if (buf1 != null) {
					buf1[j] = buf1[i + j];
				}
				if (buf2 != null) {
					buf2[j] = buf2[i + j];
				}
			}
			chip1.bufferpos(j);
			if (chip2 != null) {
				chip2.bufferpos(j);
			}
			
			/* Post a callback to ourselves. */
			context.schedule(this, MIXER_EVENT_RATE, Phase.PHI2);
		}

		public void setFastForward(int i) {
			fastForwardFactor = i;
		}
	}
	
	private final MixerEvent mixerEvent = new MixerEvent();

	private SidTune m_tune;

	private sid2_config_t m_cfg = new sid2_config_t();

	private double m_cpuFreq;

	//
	// C64 environment settings
	//

	private final MMU pport;

	/**
	 * Clock speed changes due to loading a new song
	 * 
	 * @param userClock
	 * @param defaultClock
	 * @param forced
	 * @return
	 */
	private double clockSpeed(final sid2_clock_t clockSpeed, final boolean forced) {
		final double defaultSpeed = clockSpeed != sid2_clock_t.SID2_CLOCK_NTSC ? CLOCK_FREQ_PAL : CLOCK_FREQ_NTSC;

		/* forced: select based on given default clock.
		 * Use PAL unless NTSC is requested, and in case user tries to force "CORRECT",
		 * who cares what we do. ConsolePlayer doesn't allow this option. */
		if (m_tune == null || forced) {
			return defaultSpeed;
		}

		/* select based on sidtune. */
		final SidTuneInfo m_tuneInfo = m_tune.getInfo();

		// Detect the Correct Song Speed
		// Determine song speed when unknown
		switch (m_tuneInfo.clockSpeed) {
		case NTSC:
			return CLOCK_FREQ_NTSC;

		case PAL:
			return CLOCK_FREQ_PAL;

		default:
			return defaultSpeed;
		}
	}

	private void reset() {
		context.reset();
		context.schedule(mixerEvent, MixerEvent.MIXER_EVENT_RATE, Event.Phase.PHI2);

		cpu.triggerRST();
		cia1.reset();
		cia2.reset();
		vic.reset();

		// Initialize Memory
		pport.reset();

		// Initialize RAM with powerup pattern
		Arrays.fill(m_ram, (byte) 0);
		for (int i = 0x07c0; i < 0x10000; i += 128) {
			Arrays.fill(m_ram, i, i + 64, (byte) 0xff);
		}
		Arrays.fill(m_colorRam, (byte) 0);

		for (ReSID s : sid) {
			if (s != null) {
				s.reset((byte) 0xf);
			}
		}

		/* bypass memory check by setting this byte to a unusual value for a short time. */
		IKernal.KERNAL[0x1d69] = (byte) 0x9f;
		context.schedule(new Event("Reset kernal byte") {
			@Override
			public void event() {
				IKernal.KERNAL[0x1d69] = 3;
			}
		}, 7000, Event.Phase.PHI2);

		/* Autostart program, if we have one. */
		if (m_tune != null) {
			context.schedule(new Event("Load SID event") {
				@Override
				public void event() {
					final int address = m_tune.placeProgramInMemory(m_ram);
					if (address != -1) {
						/* Set initial volume for psid.
						 * Ideally the driver would do this for us, but whatever...
						 */
						for (final ReSID s : sid) {
							if (s != null) {
								s.write(0x18, (byte) 0xf);
							}
						}
						cpu.forcedJump(address);
					} else {
						m_ram[0x30c] = (byte) (m_tune.getInfo().currentSong - 1);
						m_ram[0x277] = (byte) 'R';
						m_ram[0x278] = (byte) 'U';
						m_ram[0x279] = (byte) 'N';
						m_ram[0x27a] = (byte) ':';
						m_ram[0x27b] = 13;
						m_ram[0xc6] = 5;
					}
				}
			}, 136000, Event.Phase.PHI2);
		}
	}

	public void mute(final int sidNum, final int voice, final boolean enable) {
		if (sid[sidNum] != null) {
			sid[sidNum].voice(voice, enable);
		}
	}

	/**
	 * Integrate SID emulation from the builder class into libsidplay2
	 * 
	 * @param builder
	 * @param userModel
	 * @param defaultModel
	 * @param j 
	 * @return
	 */
	private void sidCreate(sid2_model_t userModel,
			final sid2_model_t defaultModel, int channels) {

		if (m_tune != null) {
			final SidTuneInfo m_tuneInfo = m_tune.getInfo();
			// Detect the Correct SID model
			// Determine model when unknown
			if (m_tuneInfo.sidModel == SidTune.Model.UNKNOWN) {
				switch (defaultModel) {
				case SID2_MOS6581:
					m_tuneInfo.sidModel = SidTune.Model.MOS6581;
					break;
				case SID2_MOS8580:
					m_tuneInfo.sidModel = SidTune.Model.MOS8580;
					break;
				case SID2_MODEL_CORRECT:
					// No default so base it on emulation clock
					m_tuneInfo.sidModel = SidTune.Model.ANY;
				}
			}

			// Since song will run correct on any sid model
			// set it to the current emulation
			if (m_tuneInfo.sidModel == SidTune.Model.ANY) {
				if (userModel == SID2_MODEL_CORRECT) {
					userModel = defaultModel;
				}
				
				switch (userModel) {
				case SID2_MOS8580:
					m_tuneInfo.sidModel = SidTune.Model.MOS8580;
					break;
				case SID2_MOS6581:
				default:
					m_tuneInfo.sidModel = SidTune.Model.MOS6581;
					break;
				}
			}

			switch (userModel) {
			case SID2_MODEL_CORRECT:
				switch (m_tuneInfo.sidModel) {
				case MOS8580:
					userModel = SID2_MOS8580;
					break;
				case MOS6581:
					userModel = SID2_MOS6581;
					break;
				default:
					break;
				}
				break;
				// Fixup tune information if model is forced
			case SID2_MOS6581:
				m_tuneInfo.sidModel = SidTune.Model.MOS6581;
				break;
			case SID2_MOS8580:
				m_tuneInfo.sidModel = SidTune.Model.MOS8580;
				break;
			}
		}

		for (int i = 0; i < channels; i++) {
			sid[i] = new ReSID(context);
		}
	}

	/**
	 * CPU IRQ line control. IRQ is asserted if any source asserts IRQ.
	 * (Maintains a counter of calls with state=true vs. state=false.)
	 */
	public void interruptIRQ(final boolean state) {
		if (state) {
			cpu.triggerIRQ();
		} else {
			cpu.clearIRQ();
		}
	}

	/**
	 * CPU NMI line control. NMI is asserted if any source asserts NMI.
	 * (Maintains a counter of calls with state=true vs. state=false.)
	 */
	public void interruptNMI(final boolean state) {
		if (state) {
			cpu.triggerNMI();
		} else {
			cpu.clearNMI();
		}
	}

	public void signalAEC(final boolean state) {
		cpu.aecSignal(state);
	}

	public void lightpen(boolean state) {
		if (state) {
			vic.lightpen();
		}
	}

	private final EventScheduler context;
	
	/**
	 * Set the ICs environment variable to point to this player
	 */
	public Player() {
		context = new EventScheduler();

		cia1 = new C64CIA.C64CIA1(context, this);
		cia2 = new C64CIA.C64CIA2(context, this);
		vic = new OldMOS656X(context, this);
		cpu = new MOS6510(context, this);
		pport = new MMU();
		mixerEvent.setFastForward(1);

		config(m_cfg);
	}
		
	public final sid2_config_t config() {
		return m_cfg;
	}

	/**
	 * Build C64 system capable of playing the loaded tune.
	 * Call this method after load(), because settings requested
	 * by SidTune may affect the system that is being built.
	 * 
	 * @param cfg
	 * @return
	 */
	public void config(final sid2_config_t cfg) {
		/* Determine whether we need to build a PAL or NTSC C64 */
		m_cpuFreq = clockSpeed(cfg.clockSpeed, cfg.clockForced);
		if (m_cpuFreq == CLOCK_FREQ_PAL) {
			vic.chip(OldMOS656X.Model.MOS6569);
			cia1.setDayOfTimeRate(m_cpuFreq / VIC_FREQ_PAL);
			cia2.setDayOfTimeRate(m_cpuFreq / VIC_FREQ_PAL);
		} else {
			vic.chip(OldMOS656X.Model.MOS6567R8);
			cia1.setDayOfTimeRate(m_cpuFreq / VIC_FREQ_NTSC);
			cia2.setDayOfTimeRate(m_cpuFreq / VIC_FREQ_NTSC);
		}

		/* Find out how many SIDs we need */
		cfg.playback = sid2_mono;
		Arrays.fill(m_sidmapper, 0);
		if (m_tune != null) {
			final SidTuneInfo m_tuneInfo = m_tune.getInfo();

			if (m_tuneInfo.sidChipBase2 != 0) {
				cfg.playback = sid2_stereo;
				m_sidmapper[m_tuneInfo.sidChipBase2 >> 5 & SID2_MAPPER_SIZE - 1] = 1;
			}
		}
		if (cfg.forceStereoTune) {
			cfg.playback = sid2_stereo;
			m_sidmapper[cfg.dualSidBase >> 5 & SID2_MAPPER_SIZE - 1] = 1;
		}
		sidCreate(cfg.sidModel, cfg.sidDefault, cfg.playback == sid2_stereo ? 2 : 1);

		/* inform freshly-created chips of their desired sampling rate */
		for (ReSID s : sid) {
			if (s != null) {
				s.sampling((float) m_cpuFreq, cfg.frequency);
			}
		}

		/* Reset all event schedulers */
		reset();

		// Update Configuration
		m_cfg = cfg;
	}

	public void fastForward(int factor) {
		if (factor < 1) {
			factor = 1;
		}
		if (factor > 32) {
			factor = 32;
		}
		mixerEvent.setFastForward(factor);
	}

	/**
	 * Load a program to play.
	 * 
	 * @param tune
	 * @return 0 on success
	 */
	public void load(final SidTune tune) {
		m_tune = tune;
	}

	public int play(final short[] buffer) throws InterruptedException {
		mixerEvent.begin(buffer);

		// Start the player loop
		while (mixerEvent.notFinished()) {
			context.clock();
		}

		return mixerEvent.samplesGenerated();
	}

	public int time() {
		return (int) (context.getTime(Event.Phase.PHI1) / m_cpuFreq);
	}

	private final static int SID2_MAX_SIDS = 2;

	private final static int SID2_MAPPER_SIZE = 32;
}

class MMU {
    /* Value written to processor port.  */
	private byte dir;
	private byte data;

    /* Value read from processor port.  */
    private byte data_read;

    /* State of processor port pins.  */
    private byte data_out;

    // TODO some wired stuff with data_set_bit6 and data_set_bit7
    
    private boolean kernal;
	private boolean basic;
	private boolean ioArea;
	private boolean character;

	/* Tape motor status.  */
	private byte old_port_data_out;
	/* Tape write line status.  */
	private byte old_port_write_bit;

	protected MMU() {
	}
	
	public boolean isKernal() {
		return kernal;
	}

	public boolean isCharacter() {
		return character;
	}

	public boolean isIoArea() {
		return ioArea;
	}

	public boolean isBasic() {
		return basic;
	}

	public void setData(byte value) {
        if (data != value) {
            data = value;
            mem_pla_config_changed();
        }
	}

	protected void setDir(byte value) {
        if (dir != value) {
            dir = value;
            mem_pla_config_changed();
        }
	}

	protected void reset() {
		old_port_data_out = (byte) 0xff;
		old_port_write_bit = (byte) 0xff;
		data = 0x3f;
	    data_out = 0x3f;
	    data_read = 0x3f;
	    dir = 0;
	}

	private void mem_pla_config_changed() {
		final int mem_config = (data | ~dir) & 0x7;
		/*   B I K C
		 * 0 . . . .
		 * 1 . . . *
		 * 2 . . * *
		 * 3 * . * *
		 * 4 . . . .
		 * 5 . * . .
		 * 6 . * * .
		 * 7 * * * .
		 */
		basic = (mem_config & 3) == 3;
		ioArea = mem_config > 4;
		kernal = (mem_config & 2) != 0;
		character = (mem_config ^ 4) > 4;

	    c64pla_config_changed((byte) 0x17);
	}

	private void c64pla_config_changed(byte pullup) {
		data_out = (byte) ((data_out & ~dir) | (data & dir));

		data_read = (byte) ((data | ~dir) & (data_out | pullup));

		if (0 == (dir & 0x20)) {
			data_read &= 0xdf;
		}

		if (((dir & data) & 0x20) != old_port_data_out) {
			old_port_data_out = (byte) ((dir & data) & 0x20);
		}

		if (((~dir | data) & 0x8) != old_port_write_bit) {
			old_port_write_bit = (byte) ((~dir | data) & 0x8);
		}
	}

	protected byte getDirRead() {
		return dir;
	}

	protected byte getDataRead() {
		return data_read;
	}
}

