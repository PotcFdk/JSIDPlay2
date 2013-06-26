/*
 * mmc64.c - Cartridge handling, MMC64 cart.
 *
 * Written by
 *  Markus Stehr <bastetfurry@ircnet.de>
 *  Marco van den Heuvel <blackystardust68@yahoo.com>
 *  Groepaz <groepaz@gmx.net>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */ package libsidplay.components.cart.supported;

import static libsidplay.mem.IMMC_BIOS104.BIOS_1_04;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.SDCard;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * @author Ken Händel
 * XXX this is not finished, yet!
 * c:/Users/Ken/vice-2.1/data>x64 -mmc64bios e:/usb/develop/workspace/jsidplay2/test/cart/mmc64/bios.bin -mmc64image e:/usb/develop/workspace/jsidplay2/test/cart/mmc64/test.IMA -mmc64
 */
public class MMC64 extends Cartridge {

	/**
	 * MMC64 enable.
	 */
	protected boolean enabled;

	/**
	 * MMC64 clockport enable.
	 */
	protected boolean clockportEnabled;

	/**
	 * MMC64 clockport base address.
	 */
	protected int hwClockport = 0xde02;

	/**
	 * MMC64 bios writable.
	 */
	protected int biosWrite;

	/**
	 * Image file name.
	 */
	protected String imageFilename;

	/**
	 * Image file.
	 */
	protected RandomAccessFile imageFile;

	/**
	 * Pointer inside image.
	 */
	protected int imagePointer;

	/**
	 * Write sequence counter.
	 */
	protected int writeSequence;

	/**
	 * Command buffer.
	 */
	protected byte cmdBuffer[] = new byte[9];
	protected int cmdBufferPointer;

	/**
	 * $DF11 bit 7 unlock flag.
	 */
	protected boolean bit7Unlocked;

	/**
	 * Unlocking sequences buffer.
	 */
	protected byte unlocking[] = new byte[] { 0, 0 };

	/**
	 * BIOS changed flag.
	 */
	protected boolean biosChanged;

	/**
	 * Flash jumper flag.
	 */
	protected boolean hwFlashJumper;

	/**
	 * Write protect flag.
	 */
	protected boolean hwWriteProtect;

	/* Variables of the various control bits */
	protected byte active;
	protected byte spiMode;
	protected byte extrom;
	protected byte flashMode;
	protected byte cport;
	protected byte speed;
	protected byte cardsel;
	protected byte biossel;
	
	/* Status Bits */
	/* $DF12 (R): MMC64 status register */
	/**
	 * bit 5: 0 = flash jumper not set, 1 = flash jumper set.
	 */
	public static final int MMC_FLASHJMP = 0x20;
	/**
	 * bit 4: 0 = card write enabled, 1 = card write disabled.
	 */
	public static final int MMC_WRITEPROT = 0x10;
	/**
	 * bit 3: 0 = card inserted, 1 = no card inserted.
	 */
	public static final int MMC_CARDPRS = 0x08;
	/**
	 * bit 2: external EXROM line.
	 */
	public static final int MMC_EXTEXROM = 0x04;
	/**
	 * bit 1: external GAME line.
	 */
	public static final int MMC_EXTGAME = 0x02;
	/**
	 * bit 0: 0 = SPI ready, 1 = SPI busy.
	 */
	public static final int MMC_SPISTAT = 0x01;

	/* Variables of the various status bits */
	protected byte flashJumper;
	protected boolean extExrom;
	protected boolean extGame;
	protected int revision;
	protected int sdType;
	protected boolean imageFileReadonly;
	protected byte[] bios = BIOS_1_04 /* or BIOS_1_10 */;

	SDCard card = new SDCard();

	public MMC64(PLA pla, final File imgFile) {
		super(pla);

		setImageFilename(imgFile.getAbsolutePath());
		setEnabled(true);
	}

	public int cartActive()

	{
	    if (enabled && 0==active && 0==biossel) {
	        return 1;
	    }
	    return 0;
	}

	/*
	 * reset
	 */
	@Override
	public void reset() {
		active = 0;
		spiMode = 0;
		extrom = 0;
		flashMode = 0;
		cport = 0;
		speed = 0;
		cardsel = 0;
		biossel = 0;

//		extExrom = 0x04;
//		extGame = 0x02;

	    if (!clockportEnabled) {
	        clockportEnabled = true;
	    }
		if (enabled) {
			pla.setGameExrom(true, false);
		}
	}

	private void activate() {
		biosChanged = false;
	   try {
		card.mmc_open_card_image(imageFilename, !hwWriteProtect);
	} catch (IOException e) {
		e.printStackTrace();
	} 
	}

	private void deactivate() {
	    card.closeCardImage(); 
	    if (biosChanged && biosWrite!=0) {
			// XXX save changed bios to disk
	    }
	}

	private void setEnabled(boolean en) {
		if (!en) {
			if (enabled) {
				deactivate();
			}
			enabled = false;
			pla.setGameExrom(true, true);
		} else {
			if (!enabled) {
				activate();
			}
			enabled = true;
			pla.setGameExrom(true, false);
		}
	}

	public void setReadOnly(boolean ro) {
		if (!imageFileReadonly) {
			hwWriteProtect = ro;
		} else {
			hwWriteProtect = true;
		}
	}

	public void setFlashJumper(boolean val) {
		hwFlashJumper = val;
		flashJumper = (byte) (val ? MMC_FLASHJMP : 0);
	}

	public void setRevision(int val) {
		revision = val;
	}

	public void setSdType(int val) {
		sdType = val;
		card.setCardType(val);
	}
	 
	public void setBiosWrite(int val) {
		biosWrite = val;
	}

	public void configInit() {
		active = 0;
		spiMode = 0;
		extrom = 0;
		flashMode = 0;
		cport = 0;
		speed = 0;
		cardsel = 0;
		biossel = 0;

		/* for now external exrom and game are constantly   *
		    * high until the pass-through port support is made */
		extExrom = true;//pla.getExromPHI1();
		extGame = true;//pla.getGamePHI1();

		if (enabled) {
			pla.setGameExrom(true, false);
		}
	}

	void passthrough_changed()

	{
		extExrom = true;//pla.getExromPHI1();
		extGame = true;//pla.getGamePHI1();
//	    LOG(("MMC64 passthrough changed exrom: %d game: %d (mmc64_active: %d)", mmc64_extexrom, mmc64_extgame, mmc64_active));
//	    if (0==active) {
//	        cart_set_port_game_slot0(mmc64_extgame);
//	        cart_port_config_changed_slot0();
//	    } else {
//	        /* MMC64 is completely disabled */
//	        cart_config_changed_slot0((BYTE)(((mmc64_extexrom ^ 1) << 1) | mmc64_extgame), (BYTE)(((mmc64_extexrom ^ 1) << 1) | mmc64_extgame), CMODE_READ);
//	    }
	}

	void clockportEnableStore(int addr, byte value)
	{
	    if (((value & 1)!=0) != clockportEnabled) {
	        clockportEnabled = (value & 1) !=0;
//	#ifdef HAVE_TFE
//	        tfe_clockport_changed();
//	#endif
	    }
	}

	public void clockportEnableStore(boolean value) {
		if (value) {
			clockportEnabled = true;
		} else {
			clockportEnabled = false;
		}
	}

	protected void regStore(final int addr, final byte value, int active) {
		if (enabled && addr >= 0xdf10 && addr <= 0xdf13) {
			  switch(addr)
			  {
			    case 0xdf10:    /* MMC64 SPI transfer register */
			    	/*
		             * $DF10: MMC SPI transfer register
		             *
		             * byte written is sent to the card
		             */
		            if (active!=0) {
		            	card.dataWrite(value);
		                return;
		            }
		            break; 
			    case 0xdf11:    /* MMC64 control register */
			    	/*
		             * $DF11: MMC control register
		             *        ------------------------
		             *        bit 0:  0 = MMC BIOS enabled, 1 = MMC BIOS disabled                   (R/W)
		             *        bit 1:  0 = card selected, 1 = card not selected                      (R/W)
		             *        bit 2:  0 = 250khz transfer, 1 = 8mhz transfer                        (R/W)
		             *        bit 3:  0 = clock port @ $DE00, 1 = clock port @ $DF20                (R/W)
		             *        bit 4:  0 = normal Operation, 1 = flash mode                          (R/W)  (*)
		             *        bit 5:  0 = allow external rom when BIOS is disabled , 1 = disable    (R/W)
		             *        bit 6:  0 = SPI write trigger mode, 1 = SPI read trigger mode         (R/W)
		             *        bit 7:  0 = MMC64 is active, 1 = MMC64 is completely disabled         (R/W)  (**)
		             *
		             * (*) bit can only be programmed when flash jumper is set
		             * (**) bit can only be modified after unlocking
		             */
		            if (active!=0) {
		                biossel = (byte) ((value) & 1); /* bit 0 */
		                extrom = (byte) ((value >> 5) & 1);      /* bit 5 */

		                card.cardSelectedWrite((byte)(((value >> 1) ^ 1) & 1));   /* bit 1 */
		                card.spi_mmc_enable_8mhz_write((byte)(((value >> 2)) & 1)); /* bit 2 */
		                cport = (byte) (((value >> 3)) & 1); /* bit 3 */

		                if (flashJumper!=0) {    /* this bit can only be changed if the flashjumper is on */
		                    flashMode = (byte) (((value >> 4)) & 1); /* bit 4 */
		                }
		                card.triggerModeWrite((byte)(((value >> 6)) & 1));        /* bit 6 */

		                active=(((value >> 7)) & 1); /* bit 7 */

		                if (active!=0) {
		        			pla.setGameExrom(true, true);
		                } else {
		                    /* this controls the mapping of the MMC64 bios */
		                    if (biossel!=0) {
		            			pla.setGameExrom(true, true);
		                    } else {
		            			pla.setGameExrom(true, false);
		                    }
		                }
		                if (cport!=0) {
		                    hwClockport = 0xdf22;
		                } else {
		                    hwClockport = 0xde02;
		                }
		                return;
		            }
		            break;
			    case 0xdf12:  /* MMC64 status register, read only */
			      break;
			    case 0xdf13:  /* MMC64 identification register, also used for unlocking sequences */
			      unlocking[0] = unlocking[1];
			      unlocking[1] = value;
			      if (unlocking[0]==0x55 && unlocking[1]==(byte)0xaa)
			      {
			        System.out.println("bit 7 unlocked");
			        bit7Unlocked=true;    /* unlock bit 7 of $DF11 */
			      }
			      if (unlocking[0]==0x0a && unlocking[1]==0x1c)
			      {
			        active = 0;
					pla.setGameExrom(true, false);
			      }
			      break;
			    default:      /* Not for us */
			      return;
			  }
	    }

	}
	
	protected final Bank io2Bank = new Bank() {
		@Override
		public byte read(final int addr) {
			if (enabled && addr >= 0xdf10 && addr <= 0xdf13) {
				byte value;

				if (active!=0) {
			        /* MMC64 is completely disabled */
			        return 0;
			    }
				switch (addr) {
				case 0xdf10:
					/*
		             * $DF10: MMC SPI transfer register
		             *
		             * response from the card is read here
		             */
		            value = card.dataRead();
		            return value; 
				case 0xdf11: /* MMC64 control register */
					/*
		             * $DF11: MMC control register
		             *        ------------------------
		             *        bit 0:  0 = MMC BIOS enabled, 1 = MMC BIOS disabled                   (R/W)
		             *        bit 1:  0 = card selected, 1 = card not selected                      (R/W)
		             *        bit 2:  0 = 250khz transfer, 1 = 8mhz transfer                        (R/W)
		             *        bit 3:  0 = clock port @ $DE00, 1 = clock port @ $DF20                (R/W)
		             *        bit 4:  0 = normal Operation, 1 = flash mode                          (R/W)  (*)
		             *        bit 5:  0 = allow external rom when BIOS is disabled , 1 = disable    (R/W)
		             *        bit 6:  0 = SPI write trigger mode, 1 = SPI read trigger mode         (R/W)
		             *        bit 7:  0 = MMC64 is active, 1 = MMC64 is completely disabled         (R/W)  (**)
		             *
		             * (*) bit can only be programmed when flash jumper is set
		             * (**) bit can only be modified after unlocking
		             */
		            value = biossel;       /* bit 0 */
		            value |= (card.cardSelectedRead() << 1);  /* bit 1 */
		            value |= (card.enable8mhzRead() << 2);    /* bit 2 */
		            /* bit 3,4 always 0 */
		            value |= cport << 3;
		            value |= flashMode << 4;
		            value |= (extrom << 5); /* bit 5 */
		            value |= (card.triggerModeRead() << 6);   /* bit 6 */
		            value |= active << 7;    /* bit 7 always 0 */
		            return value;
				case 0xdf12: /* MMC64 status register */
				case 2:
		            /*
		             * $DF12: MMC status register
		             *        -----------------------
		             *        bit 0:  0 = SPI ready, 1 = SPI busy                       (R)
		             *        bit 1:  feedback of $DE00 bit 0 (GAME)                    (R)
		             *        bit 2:  feedback of $DE00 bit 1 (EXROM)                   (R)
		             *        bit 3:  0 = card inserted, 1 = no card inserted           (R)
		             *        bit 4:  0 = card write enabled, 1 = card write disabled   (R)
		             *        bit 5:  0 = flash jumper not set, 1 = flash jumper set    (R)
		             *        bit 6:  0
		             *        bit 7:  0
		             */
		            value = (byte) (flashJumper<<5);    /* bit 5 */
		            value |= (card.mmcBusy());     /* bit 0 */
		            value |= (extExrom?0:1) << 1;    /* bit 1 */
		            value |= (extGame?0:1) << 2;       /* bit 2 */
		            value |= (card.cardInserted() ^ 1) << 3;   /* bit 3 */
		            value |= (card.cardWriteEnabled()?0:1) << 4;      /* bit 4 */

		            /* bit 6,7 not readable */
		            return value;

		            /*
		             * $DF13 (R/W): MMC64 identification register
		             *              -----------------------------
		             * (R) #$64 when bit 1 of $DF11 is 0
		             *     #$01 when bit 1 of $DF11 is 1 and REV A hardware is used
		             *     #$02 when bit 1 of $DF11 is 1 and REV B hardware is used
		             */ 
				case 0xdf13: /* MMC64 identification register */
					if (0==cardsel) {
		                return 0x64;
		            } else {
		                if (revision!=0) {
		                    return 2;
		                } else {
		                    return 1;
		                }
		            }
				}
			}
			return pla.getDisconnectedBusBank().read(addr);

		}

		@Override
		public void write(final int addr, final byte value) {
			regStore(addr, value, active ^ 1); 
		}
	};

	protected final Bank io1Bank = new Bank() {
		@Override
		public byte read(final int addr) {
			return io2Bank.read(addr);
		}

		@Override
		public void write(int addr, byte value) {
			if (addr==0xde01) {
				clockportEnableStore(value != 0);
			}
			if (addr>=0xde10 && addr<=0xde13) {
				if (hwFlashJumper) {
					regStore(addr, value, 1);
				}
			}
		}
	};
	
	protected final Bank romlBank = new Bank() {
		@Override
		public byte read(int addr) {
			if (0 == active && 0 == biossel) {
				return bios[(addr & 0x1fff)];
			}
			return pla.getDisconnectedBusBank().read(addr);
		}

		@Override
		public void write(int addr, byte value) {
			if (addr==0xdf21) {
				clockportEnableStore(value != 0);
			}
			if (addr>=0xdf10 && addr<=0xdf13) {

				/* if (addr == 0x8000) LOG(("roml w %04x %02x active: %d == 0 bios: %d == 0 flashjumper: %d == 1 flashmode: %d == 1\n", addr, byte, mmc64_active, mmc64_biossel, mmc64_flashjumper, mmc64_flashmode)); */
				if (0 == active && 0 == biossel && flashJumper != 0
						&& flashMode != 0) {
//			        LOG(("MMC64 Flash w %04x %02x", addr, byte));
					if (bios[(addr & 0x1fff)] != value) {
						bios[(addr & 0x1fff)] = value;
						biosChanged = true;
					}
				}
				pla.cpuWrite(addr, value);
			}
		}
	};

	private int setImageFilename(final String name) {
		if (imageFilename != null && name != null
				&& name.equals(imageFilename))
			return 0;

		if (name != null && name.length() > 0) {
			if (!new File(name).exists())
				return -1;
		}

		if (enabled) {
			deactivate();
			imageFilename = name;
			activate();
		} else {
			imageFilename = name;
		}

		return 0;
	}

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	public static void main(String[] args) {
		// create BIOS data from UPD file
		try {
			File file = new File(
					"E:/usb/develop/workspace/jsidplay2/test/cart/mmc64/1.10/mmc64v1b.upd");
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			byte[] b = new byte[(int) file.length()];
			dis.readFully(b, 0, 0x419);
			dis.readFully(b, 0, 0x2000);
			dis.close();
			for (int i = 0; i < 0x2000; i += 8) {
				if ((i % 8) == 0) {
					System.out.print("\n\t\t");
				}
				for (int j = 0; j < 8 && i + j < 0x2000; j++) {
					System.out.printf(" (byte) 0x%02X,", b[i + j]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
