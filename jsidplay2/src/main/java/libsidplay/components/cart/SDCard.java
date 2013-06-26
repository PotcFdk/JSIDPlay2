/*
 * spi-sdcard.c - SD Card over SPI Emulation
 *
 * Written by
 *  Groepaz/Hitmen <groepaz@gmx.net>
 * large parts derived from mmc64.c written by
 *  Markus Stehr <bastetfurry@ircnet.de>
 *  Marco van den Heuvel <blackystardust68@yahoo.com>
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
 */
package libsidplay.components.cart;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class SDCard {
	protected static final byte MMC_CARD_IDLE = 0;
	protected static final byte MMC_CARD_RESET = 1;
	protected static final byte MMC_CARD_INIT = 2;
	protected static final byte MMC_CARD_READ = 3;
	protected static final byte MMC_CARD_DUMMY_READ = 4;
	protected static final byte MMC_CARD_WRITE = 5;
	protected static final byte MMC_CARD_DUMMY_WRITE = 6;
	protected static final byte MMC_CARD_RETURN_WRITE = 7;

	protected static final byte MMC_CARD_INSERTED = 0;
	protected static final byte MMC_CARD_NOTINSERTED = 1;

	protected static final byte MMC_SPIMODE_READ = 1;

	protected static final int CARD_TYPE_MMC = 1;
	protected static final int CARD_TYPE_SDHC = 3;

	protected int cardType = CARD_TYPE_MMC;
	protected boolean cardRw;

	/**
	 * Image file.
	 */
	protected RandomAccessFile imageFile;

	/**
	 * Pointer inside image.
	 */
	protected long imagePointer;

	/**
	 * write sequence counter.
	 */
	protected int writeSequence;

	protected byte cardInserted;
	protected byte cardState;
	protected byte cardResetCount;

	protected int blockSize;

	/* Gets set when dummy byte is read */
	protected int readFirstbyte;

	/* MMC SPI data write port buffering */

	/**
	 * Command buffer.
	 */
	protected byte[] cmdBuffer= new byte[9];
	protected int cmdBufferPointer;
	
	protected void clearCmdBuffer() {
		Arrays.fill(cmdBuffer, (byte) 0);
		cmdBufferPointer = 0;
	}

	/* MMC SPI data read port buffering */
	protected int readBufferReadptr, readBufferWriteptr;
	byte[] readBuffer = new byte[0x1000]; /* FIXME */

	protected void readBufferSet(byte[] data, int size) {
		int dataPos = 0;
		while (size != 0) {
			byte value = data[dataPos++];
			readBuffer[readBufferWriteptr] = value;
			readBufferWriteptr++;
			readBufferWriteptr &= 0xfff;
			/* FIXME */size--;
		}
	}

	protected byte readBufferGetbyte() {
		byte value = 0; /* FIXME */
		if (readBufferReadptr != readBufferWriteptr) {
			value = readBuffer[readBufferReadptr];
			readBufferReadptr++;
			readBufferReadptr &= 0xfff;
			/* FIXME */
		}
		return value;
	}
	
	/* Resets the card */
	protected void resetCard() {
		triggerModeWrite((byte) 0);
		cardSelectedWrite((byte) 0);
		/* mmcreplay_cport = 0; */
		/* mmcreplay_speed = 0; */

		cardResetCount = 0;
		imagePointer = 0;
		blockSize = 512;
		clearCmdBuffer();
	}

	/* TODO */
	/* 0 = card inserted, 1 = no card inserted (R) */
	public byte cardInserted()
	{
	    return cardInserted;
	}

	byte setCardInserted(byte value) {
		byte oldvalue = cardInserted();
		cardInserted = value;
		return oldvalue;
	}

	public int setCardType(int value) {
		int oldvalue = cardType;
		cardType = value;
		return oldvalue;
	}

	/* TODO */
	/* 0 = SPI ready, 1 = SPI busy */
	public byte mmcBusy() {
		return 0;
	}

	/* TODO */
	/* 0 = card write enabled, 1 = card write disabled (R) */
	public boolean cardWriteEnabled() {
		return cardRw;
	}

	/* TODO */
	protected byte cardSelected = 0;

	public byte cardSelectedRead() {
		return cardSelected;
	}

	/* TODO */
	public void cardSelectedWrite(byte value) {
		cardSelected = value;
		/*
		 * LOG(("MMC spi_mmc_card_selected_write %02x",spi_mmc_card_selected)
		 * );
		 */
	}

	/* TODO */
	public byte enable8mhzRead() {
		return 0;
	}

	/* TODO */
	public void spi_mmc_enable_8mhz_write(byte value) {
	}

	/* 0 = SPI write trigger mode, 1 = SPI read trigger mode */
	protected byte triggerMode = 0;

	/* TODO */
	public byte triggerModeRead() {
		return triggerMode;
	}

	/* TODO */
	public void triggerModeWrite(byte value) {
		triggerMode = value;
	}

	/* TODO */
	public byte dataRead()
	{
	    switch (cardState) {
	        case MMC_CARD_RETURN_WRITE:
	            cardState = MMC_CARD_IDLE;
	            return (byte) 0xff;
	        case MMC_CARD_RESET:
	            switch (cardResetCount) {
	                case 0:
	                    cardResetCount++;
	                    return 0x00;
	                case 1:
	                    cardResetCount++;
	                    return 0x01;
	                case 2:
	                    cardResetCount++;
	                    return 0x01;
	                case 3:
	                    cardResetCount++;
	                    return 0x00;
	                case 4:
	                    cardResetCount++;
	                    return 0x01;
	                case 5:
	                    cardResetCount = 0;
	                    return 0x01;
	            }
	            break;
	        case MMC_CARD_INIT:
	            return 0x00;
	        case MMC_CARD_READ:
	        case MMC_CARD_DUMMY_READ:
	            if (triggerModeRead() == MMC_SPIMODE_READ) {
	                /* read trigger mode */
	                if (readFirstbyte != blockSize + 5) {
	                    readFirstbyte++;
	                }

	                if (readFirstbyte == blockSize + 3) {
	                    return 0x00;
	                }

	                if (readFirstbyte == blockSize + 4) {
	                    return 0x01;
	                }

	                if (readFirstbyte == blockSize + 5) {
	                    return 0x00;
	                }
	            } else {
	                /* write trigger mode */
	                if (readFirstbyte != blockSize + 2) {
	                    readFirstbyte++;
	                }

	                if (readFirstbyte == blockSize + 1) {
	                    return 0x00;
	                }

	                if (readFirstbyte == blockSize + 2) {
	                    return 0x01;
	                }
	            }

	            if (readFirstbyte == 0) {
	                return (byte) 0xFF;
	            }

	            if (readFirstbyte == 1) {
	                return (byte) 0xFE;
	            }

	            if (readFirstbyte == 2
	                && triggerModeRead() == MMC_SPIMODE_READ) {
	                return (byte) 0xFE;
	            }

	            if (0==cardInserted()
	                && cardState != MMC_CARD_DUMMY_READ) {
	                byte val = readBufferGetbyte();
	                return val;
	            } else {
	                return 0x00;
	            }
	    }
	    return 0;
	}
	
	/*
	CMD0        None(0)             R1  No  GO_IDLE_STATE               Software reset.
	CMD1        None(0)             R1  No  SEND_OP_COND                Initiate initialization process.
	ACMD41(*1)  *2                  R1  No  APP_SEND_OP_COND            For only SDC. Initiate initialization process.
	CMD8        *3                  R7  No  SEND_IF_COND                For only SDC V2. Check voltage range.
	CMD9        None(0)             R1  Yes SEND_CSD                    Read CSD register.
	CMD10       None(0)             R1  Yes SEND_CID                    Read CID register.
	CMD12       None(0)             R1b No  STOP_TRANSMISSION           Stop to read data.
	CMD16       Block length[31:0]  R1  No  SET_BLOCKLEN                Change R/W block size.
	CMD17       Address[31:0]       R1  Yes READ_SINGLE_BLOCK           Read a block.
	CMD18       Address[31:0]       R1  Yes READ_MULTIPLE_BLOCK         Read multiple blocks.
	CMD23       No. blocks[15:0]    R1  No  SET_BLOCK_COUNT             For only MMC. Define number of blocks to transfer  with next multi-block read/write command.
	ACMD23(*1)  No. blocks[22:0]    R1  No  SET_WR_BLOCK_ERASE_COUNT    For only SDC. Define number of blocks to pre-erase with next multi-block write command.
	CMD24       Address[31:0]       R1  Yes WRITE_BLOCK                 Write a block.
	CMD25       Address[31:0]       R1  Yes WRITE_MULTIPLE_BLOCK        Write multiple blocks.
	CMD55(*1)   None(0)             R1  No  APP_CMD                     Application specific command.
	CMD58       None(0)             R3  No  READ_OCR                    Read OCR.

	*1:ACMD<n> means a command sequense of CMD55-CMD<n>.
	*2: Rsv(0)[31], HCS[30], Rsv(0)[29:0]
	*3: Rsv(0)[31:12], Supply Voltage(1)[11:8], Check Pattern(0xAA)[7:0]
	*/

	protected long getAddr() {
		long addr;
		if (cardType == CARD_TYPE_SDHC) {
			/* SDHC (max 2^41) */
			addr = (cmdBuffer[5] * 0x100L) + (cmdBuffer[4] * 0x10000L)
					+ (cmdBuffer[3] * 0x1000000L)
					+ (cmdBuffer[2] * 0x100000000L);
			addr <<= 1;
		} else {
			/* MMC/SD (max 2^32) */
			addr = cmdBuffer[5] + (cmdBuffer[4] * 0x100)
					+ (cmdBuffer[3] * 0x10000) + (cmdBuffer[2] * 0x1000000);
		}
		return addr;
	}

	/* Executes a command */
	protected void executeCmd()
	{
	    long currentAddressPointer;

	    switch (cmdBuffer[1]) {
	        case (byte) 0xff:
	            cardState = MMC_CARD_IDLE;
	            break;
	        case 0x40:             /* CMD00 Reset */
	            resetCard();
	            cardState = MMC_CARD_RESET;
	            break;
	        case 0x41:             /* CMD01 Init */
	            cardState = MMC_CARD_INIT;
	            break;
	        case 0x48:             /* CMD8 ? */
	            if (cardType == CARD_TYPE_MMC) {
	                /* MMC */
	                byte[] cmdresp = new byte[]
	                    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(cmdresp, 0x200);
	            } else {
	                /* SD v2 */
	                byte[] cmdresp = new byte[]
	                    { 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 1;
	                readBufferSet(cmdresp, 0x200);
	            }
	            break;
	        case 0x49:             /* CMD9 send CSD */
	            if (0==cardInserted()) {
	                byte[] csdresp = new byte[]
	                    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(csdresp, 0x200);
	            } else {
	                cardState = MMC_CARD_DUMMY_READ;
	                readFirstbyte = 0;
	            }
	            break;
	        case 0x4a:             /* CMD9 send CID */
	            if (0==cardInserted()) {
	                byte[] cidresp = new byte[]
	                    { 0, 0, 0, 0,
	                    1+'v'-'a', 1+'i'-'a', 1+'c'-'a', 1+'e'-'a', '2', '3', /* "viceemu" */
	                    0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;

	                readBufferReadptr = 0;
	                readBufferWriteptr = 0;
	                readBufferSet(cidresp, 0x10);
	            } else {
	                cardState = MMC_CARD_DUMMY_READ;
	                readFirstbyte = 0;
	            }
	            break;
	        case 0x4c:             /* CMD12 Stop */
	            cardState = MMC_CARD_IDLE;
	            break;
	        case 0x50:             /* CMD16 Set Block Size */
	            cardState = MMC_CARD_IDLE;
	            blockSize =
	                cmdBuffer[5] +
	                (cmdBuffer[4] * 0x100) +
	                (cmdBuffer[3] * 0x10000) +
	                (cmdBuffer[2] * 0x1000000);
	            break;
	        case 0x51:
	            if (0==cardInserted()) {
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;

	                currentAddressPointer = getAddr();
	                try {
	                	imageFile.seek(currentAddressPointer);
                        byte[] readbuf = new byte[0x1000];    /* FIXME */
                        imageFile.seek(currentAddressPointer);
                        if (currentAddressPointer < imageFile.length()) {
                            if (imageFile.read(readbuf, 1, blockSize) > 0)
                            {
                                readBufferReadptr = 0;
                                readBufferWriteptr = 0;
                                readBufferSet(readbuf, blockSize);
                            } else {
                                /* FIXME: handle error */
                            }
                        }
	                } catch (IOException e) {
                        cardState = MMC_CARD_DUMMY_READ;
	                }
	            } else {
	                cardState = MMC_CARD_DUMMY_READ;
	                readFirstbyte = 0;
	            }
	            break;
	        case 0x58:
	/*log_debug("CMD Block Write received");*/
	            if (0==cardInserted() && blockSize > 0) {
	                currentAddressPointer = getAddr();
                    writeSequence = 0;
                    cardState = MMC_CARD_WRITE;
	            } else {
	                writeSequence = 0;
	                cardState = MMC_CARD_DUMMY_WRITE;
	            }
	            break;
	        case 0x69:             /* ACMD41 ? */
	            {
	                byte[] cmdresp = new byte[]
	                    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(cmdresp, 0x200);
	            }
	            break;
	        case 0x77:             /* CMD77 ? */
	            if (cardType!=CARD_TYPE_MMC) {
	                /* SD v2 only */
	                byte[] cmdresp = new byte[]
	                    { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(cmdresp, 0x200);
	            }
	            break;
	        case 0x7a:             /* CMD58 ? */
	            if (cardType == CARD_TYPE_SDHC) {
	                /* SDHC */
	                byte[] cmdresp = new byte[]
	                    { 0, (byte) 0xc0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(cmdresp, 0x200);
	            } else {
	                /* SD */
	                byte[] cmdresp = new byte[]
	                    { 0, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	                cardState = MMC_CARD_READ;
	                readFirstbyte = 0;
	                readBufferSet(cmdresp, 0x200);
	            }
	            break;
	    }
	}

	protected void writeToCmdBuffer(byte mmcreplayCmdChar)
	{
	    /* Check for 0xff sync byte */
	    if (cmdBufferPointer == 0) {
	        if (mmcreplayCmdChar < (byte) 0xff) {
	/*LOG(("SPI: NOT write to buffer 1: %04x %02x",mmc_cmd_buffer_pointer,mmcreplay_cmd_char)); */
	            /* yuck! hack! hack1 */
	            if (mmcreplayCmdChar == 0x51) {
	                cmdBuffer[0] = (byte) 0xff;
	                cmdBufferPointer++;
	            } else {
	                return;
	            }
	        }
	    }

	    /* Check for one 0xff sync byte too much */
	    if (cmdBufferPointer == 1) {
	        if (mmcreplayCmdChar == (byte) 0xff) {
	            cmdBufferPointer = 0;
	            return;
	        }
	    }

	/*LOG(("SPI: write to buffer: %04x %02x",mmc_cmd_buffer_pointer,mmcreplay_cmd_char));*/
	    /* Write byte to buffer */
	    cmdBuffer[cmdBufferPointer] = mmcreplayCmdChar;
	    cmdBufferPointer++;
	    /* If the buffer is full, execute the buffer and clear it */
	    if ((cmdBufferPointer > 8) ||
	/*    if ((cmdBufferPointer > 8) || */
	        (cmdBufferPointer > 7 && cmdBuffer[1] == 0x40) ||  /* cmd0 */
	        (cmdBufferPointer > 8 && cmdBuffer[1] == 0x48) ||  /* cmd8 */
	        (cmdBufferPointer > 8 && cmdBuffer[1] == 0x49) ||  /* cmd9 */
	        (cmdBufferPointer > 8 && cmdBuffer[1] == 0x4a) ||  /* cmd10 */
	        (cmdBufferPointer > 8 && cmdBuffer[1] == 0x50)   /* cmd16 */
	      ||  (cmdBufferPointer > 8 && cmdBuffer[1] == 0x51)    /* cmd17 */
	        )
	    {
	        executeCmd();
	        clearCmdBuffer();
	    }
	}
	
    protected void writeToMMC(byte value)
    {
        switch (writeSequence) {
            case 0:
                if (value == (byte) 0xfe) {
                    writeSequence++;
                    imagePointer = 0;
                }
                break;
            case 1:
                if (cardState == MMC_CARD_WRITE) {
                	try {
						imageFile.write(value);
					} catch (IOException e) {
						e.printStackTrace();
                        System.out.println("could not write to mmc image file");
                        /* FIXME: handle error */
					}
                }
                imagePointer++;
                if (imagePointer == blockSize) {
                    writeSequence++;
                }
                break;
            case 2:
                writeSequence++;
                break;
            case 3:
                cardState = MMC_CARD_RETURN_WRITE;
                break;
        }
    }

    /* TODO */
    public void dataWrite(byte value)
    {
        if (cardState == MMC_CARD_WRITE
            || cardState == MMC_CARD_DUMMY_WRITE) {
    /*LOG(("spi data write mmc: %02x",value));*/
            writeToMMC(value);
        } else {
    /*LOG(("spi data write cmd: %02x",value));*/
            writeToCmdBuffer(value);
        }
    }
    
    public int mmc_open_card_image(String name, boolean rw) throws IOException
    {
        String imageFilename = name;

        setCardInserted(MMC_CARD_NOTINSERTED);

        if (imageFilename == null) {
            System.out.println("sd card image name not set");
            return 1;
        }

        if (imageFile != null) {
            closeCardImage();
        }

        if (rw) {
            imageFile = new RandomAccessFile(imageFilename, "rw");
        }

        if (imageFile == null) {
            imageFile = new RandomAccessFile(imageFilename, "r");

            if (imageFile == null) {
            	// XXX Nonsens in Java, change me!
                System.out.printf("could not open sd card image: %s\n", imageFilename);
                return 1;
            } else {
                /* FIXME */
                setCardInserted(MMC_CARD_INSERTED);
                System.out.printf("opened sd card image (ro): %s\n", imageFilename);
                /* imageFile_readonly = 1; */
                /* mmcreplay_hw_writeprotect = 1; */
                /* mmcreplay_writeprotect = MMC_WRITEPROT; */
            }
        } else {
            /* imageFile_readonly = 0; */
            setCardInserted(MMC_CARD_INSERTED);
            System.out.printf("opened sd card image (rw): %s\n", imageFilename);
        }
        cardRw = rw;
        return 0;
    }

    public void closeCardImage()
    {
        /* unmount mmc cart image */
        if (imageFile != null) {
        	try {
				imageFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            imageFile = null;
            setCardInserted(MMC_CARD_NOTINSERTED);
        }
    }
}