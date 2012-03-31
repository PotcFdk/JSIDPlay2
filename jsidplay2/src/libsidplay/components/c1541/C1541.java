/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package libsidplay.components.c1541;

import static libsidplay.mem.IC1541_1.C1541_C000;
import static libsidplay.mem.IC1541_2.C1541_E000;
import static libsidplay.mem.IC1541_II_1.C1541_II_C000;
import static libsidplay.mem.IC1541_II_2.C1541_II_E000;

import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.OvImageIcon;
import libsidplay.components.iec.IECBus;
import libsidplay.components.mos6510.MOS6510;

/**
 * C1541 instance which manages the Floppy CPU and the two VIAs.<br>
 * <br>
 * A good German documentation on the 1541 floppy can be found at<br>
 * http://www.trikaliotis.net/download/DieFloppy1541-v4.pdf<br>
 * or<br>
 * http://www.softwolves.pp.se/idoc/alternative/vc1541_de<br>
 * Good English ROM listings can be found at<br>
 * http://www.ffd2.com/fridge/docs/1541dis.html<br>
 * or<br>
 * http://www.the-dreams.de/aay1541.txt<br>
 * 
 * TODO Half-tracks are not supported, yet maybe the code looks like it does,
 * but this is not the case.
 * 
 * @author Jörg Jahnke (joergjahnke@users.sourceforge.net)
 * @author Ken Händel
 */
public final class C1541 {
	/**
	 * Icon: C1541 turned off.
	 */
	protected static final OvImageIcon FLOPPYICON = new OvImageIcon(
			C1541.class.getResource("icons/cbm1541c.png"));
	/**
	 * Icon: C1541 turned on.
	 */
	protected static final OvImageIcon FLOPPYICON_G = new OvImageIcon(
			C1541.class.getResource("icons/cbm1541c_g.png"));
	/**
	 * Icon: C1541 turned on and disk activity.
	 */
	protected static final OvImageIcon FLOPPYICON_RG = new OvImageIcon(
			C1541.class.getResource("icons/cbm1541c_rg.png"));
	/**
	 * Icon: C1541-II turned off.
	 */
	protected static final OvImageIcon FLOPPYICON_II = new OvImageIcon(
			C1541.class.getResource("icons/1541_ii.png"));
	/**
	 * Icon: C1541-II turned on.
	 */
	protected static final OvImageIcon FLOPPYICON_II_R = new OvImageIcon(
			C1541.class.getResource("icons/1541_ii_r.png"));
	/**
	 * Icon: C1541-II turned on and disk activity.
	 */
	protected static final OvImageIcon FLOPPYICON_II_RG = new OvImageIcon(
			C1541.class.getResource("icons/1541_ii_rg.png"));

	/**
	 * Size of the floppy ROM.
	 */
	private static final int ROM_SIZE = 0x4000;
	/**
	 * Size of the floppy RAM.
	 */
	private static final int RAM_SIZE = 0x800;
	/**
	 * Size of the floppy RAM expansion.
	 */
	private static final int RAM_EXP_SIZE = 0x2000;

	/**
	 * Specific floppy type.
	 * 
	 * @author Ken Händel
	 * 
	 */
	public enum FloppyType {
		/**
		 * Floppy C-1541.
		 */
		C1541,
		/**
		 * Floppy C1541-II.
		 */
		C1541_II
	}

	/**
	 * Maximum number of activated 8KB Ram expansions.
	 */
	protected static final int EXP_RAM_BANKS = 5;

	/**
	 * The disk drive is turned on?
	 */
	private boolean powerOn;
	/**
	 * Floppy device number (8-11).
	 */
	private final int id;
	/**
	 * C1541 or C1541-II?
	 */
	private FloppyType floppyType;
	/**
	 * Event context.
	 */
	private final EventScheduler context = new EventScheduler();
	/**
	 * CPU of this device.
	 */
	private final MOS6510 cpu;
	/**
	 * VIA Bus controller.
	 */
	private final VIA6522BC viaBc;
	/**
	 * VIA Disk Controller.
	 */
	private final VIA6522DC viaDc;
	/**
	 * RAM.
	 */
	private final byte[] ram = new byte[RAM_SIZE];
	/**
	 * Simplified, joined ROM image.
	 */
	private final byte[] rom = new byte[ROM_SIZE];

	/**
	 * Custom Kernal ROM to be used.
	 */
	private byte[] customC1541Rom;
	
	/**
	 * Array of 8KB RAM expansions (0x2000-0x3FFF, 0x4000-0x5FFF, 0x6000-0x7FFF,
	 * 0x8000-0x9FFF and 0xA000-0xBFFF).
	 */
	private final byte[][] ramExpand = new byte[EXP_RAM_BANKS][];
	/**
	 * Which RAM expansion is enabled?
	 */
	private final boolean[] ramExpEnabled = new boolean[EXP_RAM_BANKS];
	/**
	 * Number of VIA chips asserting IRQ.
	 */
	private int irqCount;

	/**
	 * Create a new C1541 instance.
	 * 
	 * @param iecBus
	 *            IEC bus
	 * @param deviceID
	 *            floppy device number (8-11)
	 * @param type
	 *            C1541 or C1541-II?
	 */
	public C1541(final IECBus iecBus, final int deviceID, final FloppyType type) {
		this.id = deviceID;

		// Create a CPU for the floppy disk drive
		cpu = new MOS6510(context) {
			/**
			 * Read from address
			 * 
			 * Implementing chips: 74LS42 (binary-to-decimal decoder, with 4
			 * input bins of A10, A11, A12, A15 and 10 output values, where: 0,
			 * 1 -> RAM, 6 -> BC, 7 -> DC, giving the mapping 0, 0x400, 0x1800
			 * and 0x1c00 for the A0-A12 lines, and excluding the 74LS42 for A15
			 * lines (ROM).
			 * 
			 * Address lines such as 0x8000 - 0xffff result in select of ROM,
			 * with A13 line controlling which ROM image. CPU line A14 is not
			 * connected.
			 * 
			 * @param address
			 *            memory address
			 */
			@Override
			public byte cpuRead(final int address) {
				final int ramExpSelect = address >> 13;
				if (ramExpSelect > 0 && ramExpSelect <= EXP_RAM_BANKS
						&& getRAMExpEnabled()[ramExpSelect - 1]) {
					// 8KB Ram expansion selected
					return getRAMExpand()[ramExpSelect - 1][address & 0x1fff];
				}
				if (address < 0x8000) {
					final int chip = address & 0x1c00;
					if (chip < RAM_SIZE) {
						return getRAM()[address & 0x7ff];
					}
					if (chip == 0x1800) {
						return getBusController().read(address & 0xf);
					}
					if (chip == 0x1c00) {
						return getDiskController().read(address & 0xf);
					}
					/* Unconnected bus. */
					return (byte) 0xff;
				} else {
					return getROM()[address & 0x3fff];
				}
			}

			/**
			 * Write to address
			 * 
			 * @param address
			 *            memory address
			 */
			@Override
			public void cpuWrite(final int address, final byte data) {
				final int ramExpSelect = address >> 13;
				if (ramExpSelect > 0 && ramExpSelect <= EXP_RAM_BANKS
						&& getRAMExpEnabled()[ramExpSelect - 1]) {
					// 8KB Ram expansion selected
					getRAMExpand()[ramExpSelect - 1][address & 0x1fff] = data;
				}
				if (address < 0x8000) {
					final int chip = address & 0x1c00;
					if (chip < RAM_SIZE) {
						getRAM()[address & 0x7ff] = data;
					}
					if (chip == 0x1800) {
						getBusController().write(address & 0xf, data);
					}
					if (chip == 0x1c00) {
						getDiskController().write(address & 0xf, data);
					}
				}
			}

			/**
			 * The V flag is connected to the disk controller's byte ready
			 * -line. The controller sets the overflow flag during disk rotation
			 * whenever 8 bits have passed under the R/W head and the control
			 * line isn't masked.
			 */
			@Override
			public boolean getFlagV() {
				getDiskController().rotateDisk();
				return super.getFlagV();
			}

			@Override
			public void setFlagV(final boolean state) {
				getDiskController().rotateDisk();
				super.setFlagV(state);
			}
		};

		// Create the Bus Controller
		viaBc = new VIA6522BC(deviceID, iecBus) {
			@Override
			protected long cpuClk() {
				return getEventScheduler().getTime(Event.Phase.PHI2);
			}

			@Override
			protected void alarmSet(final Event alarm, final long ti) {
				getEventScheduler().scheduleAbsolute(alarm, ti,
						Event.Phase.PHI1);
			}

			@Override
			protected void alarmUnset(final Event alarm) {
				getEventScheduler().cancel(alarm);
			}

			@Override
			protected void setIRQ(final boolean state) {
				signalIRQ(state);
			}
		};
		// Create the Disk Controller
		viaDc = new VIA6522DC(deviceID, cpu) {
			@Override
			protected long cpuClk() {
				return getEventScheduler().getTime(Event.Phase.PHI2);
			}

			@Override
			protected void alarmSet(final Event alarm, final long ti) {
				getEventScheduler().scheduleAbsolute(alarm, ti,
						Event.Phase.PHI1);
			}

			@Override
			protected void alarmUnset(final Event alarm) {
				getEventScheduler().cancel(alarm);
			}

			@Override
			protected void setIRQ(final boolean state) {
				signalIRQ(state);
			}

			@Override
			public void diskAttachedDetached(String imageName, boolean attached) {
				final String name = attached ? imageName : null;
				FLOPPYICON.setImageName(name);
				FLOPPYICON_G.setImageName(name);
				FLOPPYICON_RG.setImageName(name);
				FLOPPYICON_II.setImageName(name);
				FLOPPYICON_II_R.setImageName(name);
				FLOPPYICON_II_RG.setImageName(name);
			}
		};

		// Setup specific floppy type
		setFloppyType(type);
		// Setup 8KB RAM expansions
		for (int i = 0; i < EXP_RAM_BANKS; i++) {
			ramExpand[i] = new byte[RAM_EXP_SIZE];
		}
	}

	/**
	 * Get event scheduler.
	 * 
	 * @return event scheduler
	 */
	public final EventScheduler getEventScheduler() {
		return context;
	}

	/**
	 * Get CPU of the floppy.
	 * 
	 * @return CPU of this floppy
	 */
	public final MOS6510 getCPU() {
		return cpu;
	}

	/**
	 * Get Bus controller of this floppy.
	 * 
	 * @return bus controller of this floppy
	 */
	public final VIA6522BC getBusController() {
		return viaBc;
	}

	/**
	 * Get disk controller of the floppy.
	 * 
	 * @return disk controller of this floppy
	 */
	public final VIA6522DC getDiskController() {
		return viaDc;
	}

	/**
	 * Get RAM of this floppy.
	 * 
	 * @return RAM of this floppy
	 */
	public final byte[] getRAM() {
		return ram;
	}

	/**
	 * Get ROM of this floppy.
	 * 
	 * @return ROM of this floppy
	 */
	public final byte[] getROM() {
		return rom;
	}

	/**
	 * Is RAM expand enabled of this floppy.
	 * 
	 * @return RAM expand enabled of this floppy
	 */
	public final boolean[] getRAMExpEnabled() {
		return ramExpEnabled;
	}

	/**
	 * Is RAM expansion of this floppy.
	 * 
	 * @return RAM expansion of this floppy
	 */
	public final byte[][] getRAMExpand() {
		return ramExpand;
	}

	/**
	 * Return the drive ID.
	 * 
	 * @return floppy device number (8-11)
	 */
	public final int getID() {
		return id;
	}

	/**
	 * The floppy has been turned on/off.
	 * 
	 * @param on
	 *            power on?
	 */
	public final void setPowerOn(final boolean on) {
		powerOn = on;
	}

	/**
	 * Set the actual type of floppy to be used (change ROM).
	 * 
	 * @param type
	 *            the type of the floppy
	 */
	public final void setFloppyType(final FloppyType type) {
		floppyType = type;
		setRom();
	}

	/**
	 * Enable 8K Ram expansion.
	 * 
	 * @param select
	 *            which 8KB RAM bank to expand (0-5), starting at 0x2000
	 *            increasing in 8KB steps up to 0xA000.
	 * @param expand
	 *            enable 8K Ram expansion
	 */
	public final void setRamExpansion(final int select, final boolean expand) {
		assert select < EXP_RAM_BANKS;
		this.ramExpEnabled[select] = expand;
	}

	/**
	 * Set/clear CPU IRQ state.
	 * 
	 * @param state
	 *            CPU IRQ state
	 */
	protected void signalIRQ(final boolean state) {
		if (state) {
			if (irqCount++ == 0) {
				cpu.triggerIRQ();
			}
		} else {
			if (--irqCount == 0) {
				cpu.clearIRQ();
			}
		}
	}

	/**
	 * Reset normally.
	 */
	public final void reset() {
		context.reset();
		cpu.triggerRST();
		viaBc.reset();
		viaDc.reset();
		irqCount = 0;
		Arrays.fill(ram, (byte) 0);
		for (int i = 0; i < EXP_RAM_BANKS; i++) {
			Arrays.fill(ramExpand[i], (byte) 0);
		}
	}

	/**
	 * Set ROM according to the floppy type.
	 */
	private void setRom() {
		if (customC1541Rom != null) {
			System.arraycopy(customC1541Rom, 0, rom, 0, ROM_SIZE);
		} else {
			if (floppyType == FloppyType.C1541) {
				System.arraycopy(C1541_C000, 0, rom, 0, ROM_SIZE >> 1);
				System.arraycopy(C1541_E000, 0, rom, ROM_SIZE >> 1,
						ROM_SIZE >> 1);
			} else {
				System.arraycopy(C1541_II_C000, 0, rom, 0, ROM_SIZE >> 1);
				System.arraycopy(C1541_II_E000, 0, rom, ROM_SIZE >> 1,
						ROM_SIZE >> 1);
			}
		}
	}

	/**
	 * Set a custom Kernal ROM to be used.
	 * 
	 * @param c1541Rom
	 *            kernal Rom (null means default Kernal)
	 */
	public final void setCustomKernalRom(final byte[] c1541Rom) {
		customC1541Rom = c1541Rom;
	}
	
	/**
	 * Get a status icon to display the floppies activity.
	 * 
	 * @return icon to show
	 */
	public final OvImageIcon getIcon() {
		if (floppyType == FloppyType.C1541) {
			if (powerOn) {
				if (viaDc.isLEDOn()) {
					return FLOPPYICON_RG;
				} else {
					return FLOPPYICON_G;
				}
			} else {
				return FLOPPYICON;
			}
		} else {
			if (powerOn) {
				if (viaDc.isLEDOn()) {
					return FLOPPYICON_II_RG;
				} else {
					return FLOPPYICON_II_R;
				}
			} else {
				return FLOPPYICON_II;
			}
		}
	}

}
