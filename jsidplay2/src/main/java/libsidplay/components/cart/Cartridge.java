package libsidplay.components.cart;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


import libsidplay.common.Event;
import libsidplay.components.Directory;
import libsidplay.components.cart.supported.ActionReplay;
import libsidplay.components.cart.supported.AtomicPower;
import libsidplay.components.cart.supported.Comal80;
import libsidplay.components.cart.supported.EasyFlash;
import libsidplay.components.cart.supported.EpyxFastLoad;
import libsidplay.components.cart.supported.Expert;
import libsidplay.components.cart.supported.FinalV1;
import libsidplay.components.cart.supported.FinalV3;
import libsidplay.components.cart.supported.MikroAss;
import libsidplay.components.cart.supported.Normal;
import libsidplay.components.cart.supported.Rex;
import libsidplay.components.cart.supported.Zaxxon;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * Cartridge base class.
 * 
 * @author Antti Lankila
 */
public class Cartridge {
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

	/** CCS64 cartridge type map */
	enum Type {
		NORMAL,
		ACTION_REPLAY,
        KCS_POWER_CARTRIDGE,
        FINAL_CARTRIDGE_III,
        SIMONS_BASIC,
        OCEAN_TYPE_1,
        EXPERT_CARTRIDGE,
        FUN_PLAY__POWER_PLAY,

        SUPER_GAMES,
        ATOMIC_POWER,
        EPYX_FASTLOAD,
        WESTERMANN_LEARNING,
        REX_UTILITY,
        FINAL_CARTRIDGE_I,
        MAGIC_FORMEL,
        C64_GAME_SYSTEM__SYSTEM_3,
        
        WARPSPEED,
        DINAMIC,
        ZAXXON__SUPER_ZAXXON,
        MAGIC_DESK__DOMARK__HES_AUSTRALIA,
        SUPER_SNAPSHOT_5,
        COMAL_80,
        STRUCTURED_BASIC,
        ROSS,
        
        DELA_EP64,
        DELA_EP7X8,
        DELA_EP256,
        REX_EP256,
        MIKRO_ASSEMBLER,
        RESERVED,
        ACTION_REPLAY_4,
        STARDOS,
        
        EASYFLASH,
	}
	
	protected Cartridge(final PLA pla) {
		this.pla = pla;
	}
	
	/**
	 * Instance of the system's PLA chip.
	 */
	public final PLA pla;

	/** Current state of cartridge-asserted NMI */
	private boolean nmiState;

	/** Current state of cartridge-asserted IRQ */
	private boolean irqState;

	/**
	 * Get currently active ROML bank.
	 * 
	 * @return ROML bank
	 */
	public Bank getRoml() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Get currently active ROMH bank.
	 * 
	 * @return ROMH bank
	 */
	public Bank getRomh() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * In Ultimax mode, the main memory between 0x1000-0xffff is disconnected.
	 * This allows carts to export their own memory for those regions, excluding
	 * the areas that will be mapped to ROML, IO and ROMH, though.
	 * 
	 * @return Memory bank for Ultimax mode
	 */
	public Bank getUltimaxMemory() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Acquire the IO1 bank
	 * 
	 * @return The bank responding to IO1 line.
	 */
	public Bank getIO1() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Acquire the IO2 bank.
	 * 
	 * @return The bank responding to IO2 line.
	 */
	public Bank getIO2() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Load a cartridge.
	 * 
	 * @param pla
	 *            Instance of the system's PLA chip
	 * @param is
	 *            stream to load from
	 * @return a cartridge instance
	 */
	public static Cartridge readImage(final PLA pla, final InputStream is)
	throws IOException {
		final DataInputStream dis = new DataInputStream(is);
		
		final byte[] header = new byte[0x40];
		dis.readFully(header);
		
		if (! new String(header, 0, 0x10, ISO88591).equals("C64 CARTRIDGE   ")) {
			throw new RuntimeException("File is not a .CRT file");
		}

		Type type = Type.values()[(header[0x16] & 0xff) << 8 | (header[0x17] & 0xff)];

		switch (type) {
		case ACTION_REPLAY:
			return new ActionReplay(dis, pla);
		case NORMAL:
			return new Normal(dis, pla);
		case FINAL_CARTRIDGE_III:
			return new FinalV3(dis, pla);
		case EXPERT_CARTRIDGE:
			return new Expert(dis, pla);
		case ATOMIC_POWER:
			return new AtomicPower(dis, pla);
		case EPYX_FASTLOAD:
			return new EpyxFastLoad(dis, pla);
		case REX_UTILITY:
			return new Rex(dis, pla);
		case FINAL_CARTRIDGE_I:
			return new FinalV1(dis, pla);
		case ZAXXON__SUPER_ZAXXON:
			return new Zaxxon(dis, pla);
		case COMAL_80:
			return new Comal80(dis, pla);
		case MIKRO_ASSEMBLER:
			return new MikroAss(dis, pla);
		case EASYFLASH:
			return new EasyFlash(dis, pla);
		default:
			throw new RuntimeException("Cartridges of format: " + type + " unsupported");
		}
	}

	/**
	 * If the cartridge needs to listen to write activity on specific banks,
	 * it can install the requisite hooks into the bank here.
	 * 
	 * @param cpuReadMap
	 * @param cpuWriteMap
	 */
	public void installBankHooks(Bank[] cpuReadMap, Bank[] cpuWriteMap) {
	}
	
	/**
	 * Return an instance of cartridge when no real cartridge is connected.
	 * 
	 * @return the null cartridge
	 */
	public static Cartridge nullCartridge(final PLA pla) {
		return new Cartridge(pla) {
			@Override
			public String toString() {
				return "";
			}
		};
	}

	/**
	 * Bring the cart to power-on state. If overridden,
	 * remember to call the superclass method.
	 */
	public void reset() {
		nmiState = false;
		irqState = false;
	}
	
	/**
	 * Push cartridge's "freeze" button.
	 * 
	 * Because this is an UI-method, we use thread-safe scheduling
	 * to delay the freezing to occur at some safe later time.
	 * 
	 * Subclasses need to override doFreeze().
	 */
	public final void freeze() {
		pla.getCPU().getEventScheduler().scheduleThreadSafe(new Event("Freeze TS") {
			@Override
			public void event() {
				Cartridge.this.doFreeze();
			}
		});
	}

	/**
	 * Handle pressing of the freeze button.
	 */
	protected void doFreeze() {
	}

	/**
	 * Callback to notify cartridge of current state of NMI signal on the system bus.
	 * The boolean value is active high.
	 * 
	 * @param state
	 */
	public void changedNMI(boolean state) {
	}

	/**
	 * Callback to notify cartridge of current state of IRQ signal on the system bus.
	 * The boolean value is active high.
	 * 
	 * @param state
	 */
	public void changedIRQ(boolean state) {
	}

	/**
	 * Callback to notify cartridge of current state of BA signal on the system bus.
	 * The boolean value is active high.
	 *
	 * @param state
	 */
	public void changedBA(boolean state) {
	}
	
	/**
	 * Assert NMI (= electrically pull NMI low) on the system bus.
	 * The boolean value is active high. Method is meant for subclasses only.
	 * 
	 * @param state
	 */
	public void setNMI(boolean state) {
		if (state ^ nmiState) {
			pla.setNMI(state);
			nmiState = state;
		}
	}
	
	/**
	 * Assert IRQ (= electrically pull IRQ low) on the system bus.
	 * The boolean value is active high. Method is meant for subclasses only.
	 * 
	 * @param state
	 */
	public void setIRQ(boolean state) {
		if (state ^ irqState) {
			pla.setIRQ(state);
			irqState = state;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
	
	public static Directory getDirectory(File file) throws IOException {
		Directory dir = new Directory();
		try (DataInputStream dis = new DataInputStream(
				new FileInputStream(file))) {
			final byte[] header = new byte[0x40];
			dis.readFully(header);
			if (!new String(header, 0, 0x10, ISO88591)
					.equals("C64 CARTRIDGE   ")) {
				return dir;
			}
			Type type = Type.values()[(header[0x16] & 0xff) << 8
					| (header[0x17] & 0xff)];
			// directory title: cartridge type
			dir.setTitle(type.toString().replace('_', '-').getBytes(ISO88591));
			// directory id: size in KB
			dir.setId(String.valueOf(file.length() >> 10).getBytes(ISO88591));
		}
		return dir;
	}
}