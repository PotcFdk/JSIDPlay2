/*
 * via2d.c - VIA2 emulation in the 1541, 1541II, 1571 and 2031 disk drive.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *  André Fachat <fachat@physik.tu-chemnitz.de>
 *  Daniel Sladic <sladic@eecg.toronto.edu>
 *  Ettore Perazzoli <ettore@comm2000.it>
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
package libsidplay.components.c1541;

import java.io.File;
import java.io.IOException;

import libsidplay.common.Event;
import libsidplay.components.mos6510.MOS6510;

/**
 * Handling of the Disk Controller.
 * 
 * @author Ken Händel
 * 
 */
public abstract class VIA6522DC extends VIACore {
	/**
	 * Number of cycles before an attached disk becomes visible to the R/W head.
	 * This is mostly to make routines that auto-detect disk changes happy.
	 * 
	 */
	private static final int DRIVE_ATTACH_DELAY = (3 * 600000);
	/**
	 * Number of cycles the write protection is activated on detach.
	 */
	private static final int DRIVE_DETACH_DELAY = (3 * 200000);
	/**
	 * Number of cycles the after a disk can be inserted after a disk has been
	 * detached.
	 */
	private static final int DRIVE_ATTACH_DETACH_DELAY = (3 * 400000);

	/**
	 * Current mode read or write.
	 * 
	 * @author Ken Händel
	 * 
	 */
	enum Mode {
		READ, WRITE
	}

	/**
	 * Tick when the disk image was attached.
	 */
	private long attachClk;
	/**
	 * Tick when the disk image was detached.
	 */
	private long detachClk;
	/**
	 * Tick when the disk image was attached, but an old image was just
	 * detached.
	 */
	private long attachDetachClk;
	/**
	 * Byte to read from r/w head.
	 */
	private byte gcrRead;
	/**
	 * GCR value being written to the disk.
	 */
	private byte gcrWrite;
	/**
	 * Are we in read or write mode?
	 */
	private Mode readWriteMode;
	/**
	 * Activates the byte ready line.
	 */
	private int byteReadyActive;
	/**
	 * Does the current GCR data needs to be written to disk?
	 */
	private boolean gcrDataDirty;
	/**
	 * Current half track on which the R/W head is positioned.
	 */
	private int currentHalfTrack;
	/**
	 * Disk rotation.
	 */
	private final Rotation rot;
	/**
	 * Attached disk image.
	 */
	private DiskImage image;
	/**
	 * Floppy device number (8-11).
	 */
	private final int id;

	/**
	 * Creates a new instance of VIA6522DC.
	 * 
	 * @param deviceID
	 *            the C1541 device ID
	 * @param cpu
	 *            drive CPU
	 */
	public VIA6522DC(final int deviceID, final MOS6510 cpu) {
		super("1541Drive" + deviceID + "VIA6522DC");
		this.id = deviceID;
		// Create disk rotation support.
		rot = new Rotation() {
			@Override
			protected long cpuClk() {
				return cpu.getEventScheduler().getTime(Event.Phase.PHI2);
			}

			@Override
			protected final Mode getReadWriteMode() {
				return getMode();
			}

			@Override
			protected void readData(final byte readData) {
				setLastGCRRead(readData);
				if (isByteReadyActive()) {
					cpu.setFlagV(true);
					signal(VIACore.VIA_SIG_CA1, VIACore.VIA_SIG_RISE);
				}
			}

			@Override
			protected byte writeData() {
				byte writeData = getLastGCRWrite();
				if (isByteReadyActive()) {
					cpu.setFlagV(true);
					signal(VIACore.VIA_SIG_CA1, VIACore.VIA_SIG_RISE);
				}
				return writeData;
			}

			@Override
			protected int getCurrentTrackSize() {
				if (getImage() != null) {
					// Use metrics of the attached disk image
					return getImage().trackSize[(getHalfTrack() >> 1) - 1];
				} else {
					// If no disk is attached, use metrics of a standard disk
					return DiskImage.RAW_TRACK_SIZE[DiskImage.SPEED_MAP_1541[(getHalfTrack() >> 1) - 1]];
				}
			}

			@Override
			protected final void setDirty() {
				setGCRDataDirty();
			}

			@Override
			protected final boolean isDiskChangeInProgress() {
				return getAttachClk() != 0;
			}

		};
	}

	@Override
	public final void reset() {
		attachClk = 0;
		detachClk = 0;
		attachDetachClk = 0;
		gcrRead = 0;
		gcrWrite = 0x55;
		byteReadyActive = 0;
		readWriteMode = Mode.READ;
		gcrDataDirty = false;
		currentHalfTrack = 2;
		setHalfTrack(DiskImage.DIR_TRACK_1541 << 1);
		super.reset();
		rot.reset();
	}

	/**
	 * Get attached disk image (null if nothing attached).
	 * 
	 * @return attached disk image
	 */
	protected DiskImage getImage() {
		return image;
	}

	/**
	 * Insert Disk.<BR>
	 * A previously inserted disk will be ejected first.
	 * 
	 * @param file
	 *            disk image file
	 * @return attached disk image
	 * @throws IOException
	 *             cannot read disk image file
	 */
	public DiskImage insertDisk(final File file) throws IOException {
		ejectDisk();
		attachClk = cpuClk();
		if (detachClk > 0) {
			attachDetachClk = cpuClk();
		}
		image = DiskImage.attach(rot.getGCR(), file);
		rot.getGCR().attach();
		System.out.printf("Unit %d: " + image.getClass().getSimpleName()
				+ " disk image attached: %s.\n", id, file.getAbsolutePath());
		diskAttachedDetached(file.getName(), true);
		return image;
	}

	/**
	 * Detach Disk.<BR>
	 * A previously inserted disk will be ejected.
	 * 
	 * @throws IOException
	 *             cannot write disk file
	 * 
	 */
	public void ejectDisk() throws IOException {
		if (image != null) {
			gcrDataWriteback();
			detachClk = cpuClk();
			rot.getGCR().detach();
			image.detach();
			System.out.printf("Unit %d: " + image.getClass().getSimpleName()
					+ " disk image detached: %s.\n", id, image.fileName);
			diskAttachedDetached(image.fileName, false);
			image = null;
		}
	}

	/**
	 * Ticks when the disk image was attached.
	 * 
	 * @return when the disk was attached
	 */
	public long getAttachClk() {
		return attachClk;
	}

	/**
	 * Reposition the read/write head to the parameterized half-track accounting
	 * for potential change in speed zone (= track length).
	 * 
	 * @param num
	 *            half-track to set
	 */
	protected void setHalfTrack(final int num) {
		final int oldTrackSize = rot.getCurrentTrackSize();
		currentHalfTrack = num;
		rot.getGCR().setHalfTrack(num, oldTrackSize, rot.getCurrentTrackSize());
	}

	/**
	 * Get current half track on which the R/W head is positioned.
	 * 
	 * @return current half-track
	 */
	public int getHalfTrack() {
		return currentHalfTrack;
	}

	protected void moveHead(final boolean forward) {
		gcrDataWriteback();
		if (forward && currentHalfTrack < DiskImage.MAX_TRACKS_1541 << 1) {
			// upper limit is track 42
			setHalfTrack(currentHalfTrack + 1);
		}
		if (!forward && currentHalfTrack > 2) {
			// lower limit is track 1
			setHalfTrack(currentHalfTrack - 1);
		}
	}

	/**
	 * Write back dirty GCR data to the attached disk image.
	 */
	protected void gcrDataWriteback() {
		boolean isDirty = gcrDataDirty;
		gcrDataDirty = false;
		if (image == null || !isDirty) {
			// No disk attached or not dirty, yet?
			return;
		}
		if (image.readOnly) {
			System.err.println("Attempt to write to read-only disk image.");
		} else {
			try {
				image.gcrDataWriteback(currentHalfTrack >> 1);
			} catch (final IOException e) {
				System.err.println(String.format(
						"Error writing T:%d to disk image.",
						currentHalfTrack >> 1));
			}
		}
	}

	/**
	 * Is LED on?
	 * 
	 * @return LED on
	 */
	public final boolean isLEDOn() {
		return (oldpb & 0x8) != 0;
	}

	/**
	 * Is disk motor on?
	 * 
	 * @return motor on
	 */
	public final boolean isMotorOn() {
		return (byteReadyActive & 4) != 0;
	}

	@Override
	protected final void setCa2(final int state) {
		rotateDisk();
		byteReadyActive = byteReadyActive & ~0x02 | state << 1;
	}

	@Override
	protected final void setCb2(final int state) {
		rotateDisk();
		readWriteMode = state != 0 ? Mode.READ : Mode.WRITE;
	}

	protected Mode getMode() {
		return readWriteMode;
	}

	@Override
	protected final void storePra(final int addr, final byte byt) {
		rotateDisk();
		gcrWrite = byt;
	}

	/**
	 * Get recently written GCR byte.
	 * 
	 * @return GCR byte
	 */
	protected final byte getLastGCRWrite() {
		return gcrWrite;
	}

	@Override
	protected final void storePrb(final byte byt) {
		rotateDisk();
		if (((oldpb ^ byt) & 0x3) != 0 && (byt & 0x4) != 0) {
			/* Stepper motor */
			if ((oldpb & 0x3) == (byt + 1 & 0x3)) {
				moveHead(false);
			} else if ((oldpb & 0x3) == (byt - 1 & 0x3)) {
				moveHead(true);
			}
		}

		if (((oldpb ^ byt) & 0x60) != 0) {
			rot.setSpeedZone((byt & 0xff) >> 5 & 0x3);
		}
		if (((oldpb ^ byt) & 0x04) != 0) {
			byteReadyActive = byteReadyActive & ~0x04 | byt & 0x04;
			/* drive motor is turned on */
			if (isMotorOn()) {
				rot.rotationBegins();
			}
		}
	}

	@Override
	protected final void storeAcr(final byte value) {
	}

	@Override
	protected final void storeSr(final byte value) {
	}

	@Override
	protected final void storeT2l(final byte value) {
	}

	@Override
	protected final byte readPra() {
		byteRead();

		/*
		 * the bus is shared between the read/write modes. The schematic says
		 * that during write mode, the read circuitry is disconnected and
		 * therefore it's likely that you will read 0xff if you change the DDR.
		 */
		byte encoderDecoderValue = readWriteMode == Mode.READ ? gcrRead
				: (byte) 0xff;

		return (byte) (encoderDecoderValue & ~via[VIACore.VIA_DDRA] | via[VIACore.VIA_PRA]
				& via[VIACore.VIA_DDRA]);
	}

	/**
	 * Set recently read GCR byte.
	 * 
	 * @param lastReadData
	 *            GCR byte
	 */
	protected final void setLastGCRRead(final byte lastReadData) {
		gcrRead = lastReadData;
	}

	@Override
	protected final byte readPrb() {
		rotateDisk();
		return (byte) ((rot.syncFound() | writeProtectSense())
				& ~via[VIACore.VIA_DDRB] | via[VIACore.VIA_PRB]
				& via[VIACore.VIA_DDRB]);
	}

	/**
	 * Is byte ready to be processed?
	 * 
	 * @return byte ready active
	 */
	protected final boolean isByteReadyActive() {
		return (byteReadyActive & 2) != 0;
	}

	/**
	 * Rotate disk, if the motor is on.
	 */
	protected final void rotateDisk() {
		if (isMotorOn()) {
			rot.rotateDisk();
		}
	}

	protected void setGCRDataDirty() {
		this.gcrDataDirty = true;
	}

	/**
	 * Read GCR data by rotating the disk, if disk attachment/detachment is in
	 * progress just delay and read zero.
	 */
	private void byteRead() {
		if (attachClk != 0) {
			if (cpuClk() - attachClk < DRIVE_ATTACH_DELAY) {
				gcrRead = 0;
			} else {
				attachClk = 0;
			}
		} else if (attachDetachClk != 0) {
			if (cpuClk() - attachDetachClk < DRIVE_ATTACH_DETACH_DELAY) {
				gcrRead = 0;
			} else {
				attachDetachClk = 0;
			}
		} else {
			rotateDisk();
		}
	}

	/**
	 * Implements write protect sense, in respect to the disk currently being
	 * attached/detached.
	 * 
	 * @return 0x0 (write protected), 0x10 (read/write)
	 */
	private byte writeProtectSense() {
		/*
		 * Clear the write protection bit for the time the disk is pulled out on
		 * detach.
		 */
		if (detachClk != 0) {
			if (cpuClk() - detachClk < DRIVE_DETACH_DELAY) {
				return 0x0;
			}
			detachClk = 0;
		}
		/*
		 * Set the write protection bit for the minimum time until a new disk
		 * can be inserted.
		 */
		if (attachDetachClk != 0) {
			if (cpuClk() - attachDetachClk < DRIVE_ATTACH_DETACH_DELAY) {
				return 0x10;
			}
			attachDetachClk = 0;
		}
		/*
		 * Clear the write protection bit for the time the disk is put in on
		 * attach.
		 */
		if (attachClk != 0) {
			if (cpuClk() - attachClk < DRIVE_ATTACH_DELAY) {
				return 0x0;
			}
			attachClk = 0;
		}

		if (image == null || !image.isReadOnly()) {
			// No disk in drive or write protection is off.
			return 0x10;
		} else {
			// Write protection is on
			return 0x0;
		}
	}

	public abstract void diskAttachedDetached(String imageName, boolean attached);

}
