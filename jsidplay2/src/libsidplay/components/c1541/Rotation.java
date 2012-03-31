/**
 * rotation.c
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
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
 * @author Ken Händel
 * 
 **/
package libsidplay.components.c1541;

import java.util.Random;

import libsidplay.components.c1541.VIA6522DC.Mode;

/**
 * 
 * Disk rotation.
 * 
 * @author Ken Händel
 * 
 */
public abstract class Rotation {
	/**
	 * Random to emulate magnetic flux changes.
	 */
	private static final Random RANDOM = new Random();
	/**
	 * Speed (in microbits per drive clock) of the disk in the 4 speed zones.
	 * 
	 * The drive contains a 16.00 MHz crystal and these values are established
	 * by dividing by 16, 15, 14 and 13 and then by 4.
	 */
	protected static final int[] ROT_SPEED_BPC = { 250000, 266667, 285714, 307692 };
	
	/**
	 * Count number of microbits (0..1000000) per second.
	 */
	private long accum;
	/**
	 * Time, when the disk motor has been turned on.
	 */
	private long rotationLastClk;
	/**
	 * Current bit count (0..7) of a byte, that have been read or written.
	 */
	private int bitCounter;
	/**
	 * Current disk speed zone.
	 */
	private int speedZone;
	/**
	 * Last 10 bits that have passed under the read/write head.
	 */
	private int lastReadData;
	/**
	 * Shift register for writing data.
	 */
	private byte lastWriteData;
	/**
	 * Number of consequtive 0s in the stream.
	 */
	private int zeroCount;
	/**
	 * The GCR image.
	 */
	private final GCR gcr;

	/**
	 * Disk rotation support.
	 */
	public Rotation() {
		// Create Group Code Recording support
		gcr = new GCR();
	}

	public final void reset() {
		speedZone = 0;
		accum = 0;
		bitCounter = 0;
		rotationBegins();
		gcr.reset();
	}

	protected final void setSpeedZone(final int zone) {
		speedZone = zone;
	}

	protected final void rotationBegins() {
		rotationLastClk = cpuClk();
	}
	
	/**
	 * Rotate the disk according to system clock. While rotating, calculate read/write
	 * data on ths platter and note presence and absence of SYNC.
	 * The caller must check the precondition, if the drive motor is on.
	 */
	protected final void rotateDisk() {
		/*
		 * Calculate the number of bits that have passed under the R/W head
		 * since the last time.
		 */
		long clk = cpuClk();		
		accum += ROT_SPEED_BPC[speedZone] * (clk - rotationLastClk);
		rotationLastClk = clk;
		int bitsMoved = (int) (accum / 1000000);
		accum = accum % 1000000;

		/* if next bit is under the r/w head, read it and study it */
		if (getReadWriteMode() == Mode.READ) {
			while (bitsMoved -- != 0) {
				/* GCR=0 support.
				 * 
				 * In the absence of 1-bits (magnetic flux changes), the drive
				 * will use a timer counter to count how many 0s it has read. Every
				 * 4 read bits, it will detect a 1-bit, because it doesn't
				 * distinguish between reset occuring from magnetic flux or regular
				 * wraparound.
				 * 
				 * Random magnetic flux events can also occur after GCR data has been
				 * quiet for a long time, for at least 4 bits. So the first value
				 * read will always be 1. Afterwards, the 0-bit sequence lengths
				 * vary randomly, but can never exceed 3.
				 * 
				 * Each time a random event happens, it tends to advance the bit counter
				 * by half a clock, because the random event can occur at any time
				 * and thus the expectation value is that it occurs at 50 % point
				 * within the bitcells.
				 * 
				 * Additionally, the underlying disk rotation has no way to keep in sync
				 * with the electronics, so the bitstream after a GCR=0 may or may not
				 * be shifted with respect to the bit counter by the time drive
				 * encounters it. This situation will persist until the next sync
				 * sequence. There is no specific emulation for variable disk rotation,
				 * this case is thought to be covered by the random event handling.
				 * 
				 * Here's some genuine 1541 patterns for reference:
				 * 
				 * 53 12 46 22 24 AA AA AA AA AA AA AA A8 AA AA AA
				 * 53 11 11 11 14 AA AA AA AA AA AA AA A8 AA AA AA
				 * 53 12 46 22 24 AA AA AA AA AA AA AA A8 AA AA AA
				 * 53 12 22 24 45 2A AA AA AA AA AA AA AA 2A AA AA
				 * 53 11 52 22 24 AA AA AA AA AA AA AA A8 AA AA AA
				 */

				int bit = gcr.readNextBit(getCurrentTrackSize());
				lastReadData = ((lastReadData << 1) & 0x3fe);

				if (bit != 0) {
					zeroCount = 0;
					lastReadData |= 1;
				}

				/* Simulate random magnetic flux events in our lame-ass emulation. */
	            if (++ zeroCount > 8 && (lastReadData & 0x3f) == 0x8 && RANDOM.nextInt() > (1 << 30)) {
					lastReadData |= 1;
	                /*
	                 * Simulate loss of sync against the underlying platter.
	                 * Whenever 1-bits occur, there's a chance that they occured
	                 * due to a random magnetic flux event, and can thus occur
	                 * at any phase of the bit-cell clock.
	                 * 
	                 * It follows, therefore, that such events have a chance to
	                 * advance the bit_counter by about 0,5 clocks each time they
	                 * occur. Hence > 0 here, which filters out 50 % of events.
	                 */
	                if (bitCounter < 7 && RANDOM.nextInt() > 0) {
	                    bitCounter ++;
	                    lastReadData = (lastReadData << 1) & 0x3fe;
	                }
				} else if ((lastReadData & 0xf) == 0) {
					/* Simulate clock reset */
					lastReadData |= 1;
				}
				lastWriteData <<= 1;
				
				/* is sync? reset bit counter, don't move data, etc. */
				if (lastReadData == 0x3ff) {
					bitCounter = 0;
				} else {
					if (++ bitCounter == 8) {
						bitCounter = 0;
						// tlr claims that the write register is loaded at every
						// byte boundary, and since the bus is shared, it's reasonable
						// to guess that it would be loaded with whatever was last read.
						lastWriteData = (byte) lastReadData;
						readData((byte) lastReadData);
					}
				}
			}
		} else {
			/* When writing, the first byte after transition is going to echo the
			 * bits from the last read value.
			 */
			while (bitsMoved -- != 0) {
				lastReadData = (lastReadData << 1) & 0x3fe;
				if ((lastReadData & 0xf) == 0) {
					/* 0 -> 1 */
					lastReadData |= 1;
				}
				
				setDirty();
				gcr.writeNextBit((lastWriteData & 0x80) != 0, getCurrentTrackSize());
				lastWriteData <<= 1;				

				if (++ bitCounter == 8) {
					bitCounter = 0;
					lastWriteData = writeData();
				}
			}
		}
	}

	/**
	 * SYNC is detected whenever the last 10 bits are 1, and we aren't writing, or
	 * disk wasn't just being changed.
	 * 
	 * @return 0 when found, 0x80 when not.
	 */
	protected final byte syncFound() {
		if (getReadWriteMode() == Mode.WRITE || isDiskChangeInProgress()) {
			return (byte) 0x80;
		}

		return lastReadData == 0x3ff ? (byte) 0 : (byte) 0x80;
	}

	/**
	 * Group Code Recording support.
	 * 
	 * @return the GCR support
	 */
	protected final GCR getGCR() {
		return gcr;
	}
	
	protected abstract long cpuClk();
	protected abstract VIA6522DC.Mode getReadWriteMode();
	protected abstract void readData(byte readData);
	protected abstract byte writeData();
	protected abstract int getCurrentTrackSize();
	protected abstract void setDirty();
	protected abstract boolean isDiskChangeInProgress();

}
