/*
 * driveimage.c
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
 */
package libsidplay.components.c1541;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;


import libsidplay.components.DirEntry;
import libsidplay.components.Directory;

/**
 * Support of disk images.
 * 
 * @author Ken Händel
 * 
 */
public abstract class DiskImage {
	/**
	 * Track containing the directory.
	 */
	protected static final int DIR_TRACK_1541 = 18;
	/**
	 * Minimum tracks of a disk.
	 */
	protected static final int MIN_TRACKS_1541 = 35;
	/**
	 * Maximum tracks of a D64 disk, that can be written.
	 */
	protected static final int EXT_TRACKS_1541 = 40;
	/**
	 * Maximum tracks of a disk, that can be accessed.
	 */
	protected static final int MAX_TRACKS_1541 = 42;

	/**
	 * Standard settings: Number of bytes per track in the speedzones 0-3.
	 */
	protected static final int[] RAW_TRACK_SIZE = { 6250, 6666, 7142, 7692 };
	/**
	 * Standard settings: Track (1-42) to speedzone (0-3) map.
	 */
	protected static final int SPEED_MAP_1541[] = { 3, 3, 3, 3, 3, 3, 3, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * Size of the GCR data of each track.
	 */
	protected int[] trackSize = new int[GCR.MAX_GCR_TRACKS];
	/**
	 * Recently attached file handle.
	 */
	protected RandomAccessFile fd;
	/**
	 * Recently attached file name.
	 */
	protected String fileName;
	/**
	 * Attach mode read-only.
	 */
	protected boolean readOnly;
	/**
	 * Track count of this image.
	 */
	protected int tracks;
	/**
	 * Group Code Recording support.
	 */
	protected GCR gcr;
	/**
	 * 40 tracks disk image extension policy.
	 */
	protected IExtendImageListener extendImageListener;

	protected DiskImage(final GCR gcr, final String fileName,
			final RandomAccessFile fd, final boolean readOnly) {
		this.gcr = gcr;
		this.fileName = fileName;
		this.fd = fd;
		this.readOnly = readOnly;
	}

	/**
	 * Attach an disk image file to drive.
	 * 
	 * @param gcr
	 *            group code recording support
	 * @param file
	 *            file of the image
	 * @return disk image
	 * @throws IOException
	 *             disk image file could not be attached
	 */
	public final static DiskImage attach(final GCR gcr, final File file)
			throws IOException {
		assert file != null;

		// Open image file
		RandomAccessFile fd;
		boolean readOnly = false;
		try {
			fd = new RandomAccessFile(file, "rw");
		} catch (IOException e) {
			/*
			 * If we cannot open the image read/write, try to open it read only.
			 */
			fd = new RandomAccessFile(file, "r");
			readOnly = true;
		}
		// Try to detect image type
		final byte header[] = new byte[Math.max(G64.IMAGE_HEADER.length(),
				NIB.IMAGE_HEADER.length())];
		fd.readFully(header, 0, header.length);
		fd.seek(0);
		final String headerString;
		try {
			headerString = new String(header, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			fd.close();
			throw new RuntimeException("No ISO-8859-1 encoding!");
		}
		// Create specific disk image
		final DiskImage image;
		if (headerString.startsWith(G64.IMAGE_HEADER)) {
			image = new G64(gcr, file.getName(), fd, readOnly);
		} else if (headerString.startsWith(NIB.IMAGE_HEADER)) {
			image = new NIB(gcr, file.getName(), fd, readOnly);
		} else {
			image = new D64(gcr, file.getName(), fd, readOnly);
		}

		try {
			image.attach();
		} catch (IOException e) {
			image.fd.close();
			throw e;
		}
		return image;
	}

	/**
	 * Attach disk image to drive.
	 * 
	 * @throws IOException
	 *             error reading disk image
	 */
	protected abstract void attach() throws IOException;

	/**
	 * Detach disk image from drive.
	 * 
	 * @throws IOException
	 *             disk image file write error
	 */
	public final void detach() throws IOException {
		fd.close();
	}

	/**
	 * Set policy how to deal with disks up to 40 tracks.
	 * 
	 * @param listener
	 *            listener to ask about the policy
	 */
	public final void setExtendImagePolicy(final IExtendImageListener listener) {
		this.extendImageListener = listener;
	}

	/**
	 * Write back unsaved disk image data.
	 * 
	 * @param track
	 *            dirty track
	 * @throws IOException
	 */
	public abstract void gcrDataWriteback(final int track) throws IOException;

	/**
	 * Is the disk image mounted read-only?
	 * 
	 * @return True if the mounted disk image is read-only; false otherwise.
	 */
	public final boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Absolute max nr of sectors per track for supported disk formats.
	 */
	private static final int MAX_SECTORS_PER_TRACK = 30;
	/**
	 * Nr of tracks * max sectors per track.
	 */
	private static final int MAX_OVERALL_SECTORS = MAX_SECTORS_PER_TRACK
			* MAX_TRACKS_1541;
	/**
	 * Number of directory entries per block.
	 */
	private static final int NR_ENTRIES_PER_BLOCK = 7;
	/**
	 * Size in bytes of a directory entry.
	 */
	private static final int DIR_ENTRY_SIZE = 32;

	public static final Directory getDirectory(final File file)
			throws IOException {
		final Directory dir = new Directory();
		final DiskImage img = DiskImage.attach(new GCR(), file);

		int[] arrCyclicAccessInfo = new int[MAX_OVERALL_SECTORS];

		// set Disk ID
		byte[] sectorBytes = new byte[GCR.SECTOR_SIZE];
		img.getDiskSector(DIR_TRACK_1541, 0, sectorBytes);
		// Get disk title/ID
		int afterBamInfos = (5 + MIN_TRACKS_1541 * 4);
		int freeBlocks = 0;
		for (int i = 1; i <= 35; i++) {
			if (i != 18) {
				freeBlocks += sectorBytes[5 + ((i - 1) * 4)] & 0xff;
			}
		}
		byte[] diskName = new byte[16];
		byte[] diskID = new byte[5];
		System.arraycopy(sectorBytes, afterBamInfos, diskName, 0,
				diskName.length);
		System.arraycopy(sectorBytes, afterBamInfos + 18, diskID, 0,
				diskID.length);
		dir.setSingleSided((sectorBytes[4] & 0x80) == 0);
		dir.setTitle(diskName);
		dir.setId(diskID);
		dir.setFreeBlocks(freeBlocks);

		// disk images
		byte[] currSector = new byte[GCR.SECTOR_SIZE];
		int nextTrack = 18, nextSector = 1, currNextSector = 0, currNextTrack = 0;
		int nextEntry = -1;
		while (true) {
			// not first block
			if (nextEntry < NR_ENTRIES_PER_BLOCK) {
				nextEntry++; // same block, next entry
			} else {
				// switch block or finish
				if (currNextTrack != 0) {
					nextTrack = currNextTrack;
					nextSector = currNextSector;
					nextEntry = 0;
				} else { // end of BAM chain
					break;
				}
			}

			// read selected sector
			if (!img.getDiskSector(nextTrack, nextSector, currSector)) {
				break;
			}

			// cyclic check and enter sector index info (but only if first dir
			// entry)
			if (nextEntry == 0 && currNextTrack != 0
					&& arrCyclicAccessInfo[nextSector] != 0) {
				break;
			} else {
				arrCyclicAccessInfo[nextSector]++;
				// mark sector as read
			}

			// store current next tracks/sector entry
			currNextTrack = currSector[1] & 0xff;
			currNextSector = currSector[2] & 0xff;

			final byte[] entryBytes = new byte[DIR_ENTRY_SIZE];
			// read next file entry
			System.arraycopy(currSector, 1 + nextEntry * DIR_ENTRY_SIZE,
					entryBytes, 0, DIR_ENTRY_SIZE);
			final byte fileType = entryBytes[2];
			if (fileType != DirEntry.FILETYPE_DEL) {
				final byte firstFileTrack = entryBytes[3];
				final byte firstFileSector = entryBytes[4];
				final byte[] fn = new byte[16];
				System.arraycopy(entryBytes, 5, fn, 0, 16);
				final byte lowByte = entryBytes[30];
				final byte highByte = entryBytes[31];
				final int nrSectors = (lowByte & 0xff)
						+ ((highByte & 0xff) << 8);
				dir.getDirEntries().add(new DirEntry(nrSectors, fn, fileType) {
					/**
					 * Save the program of this directory entry to the specified
					 * file.
					 * 
					 * @param autostartFile
					 *            file to save
					 * @throws IOException
					 *             File write error
					 */
					@Override
					public void save(File autostartFile) throws IOException {
						img.save(autostartFile, firstFileTrack, firstFileSector);
					}
				});
			}
		}
		img.detach();
		return dir;
	}

	private boolean getDiskSector(int track, int sector, byte[] currSector) {
		// check if the track is within range
		if (track < 1 || track > tracks) {
			return false;
		}
		gcr.setHalfTrack(track << 1, trackSize[track - 1], trackSize[track - 1]);

		int gcrDataPos = gcr.findSectorHeader(track, sector,
				trackSize[track - 1]);
		if (gcrDataPos == -1) {
			System.err.println(String.format(
					"Could not find header of T:%d S:%d.", track, sector));
		} else {
			gcrDataPos = gcr.findSectorData(gcrDataPos, trackSize[track - 1]);
			if (gcrDataPos == -1) {
				System.err.println(String
						.format("Could not find data sync of T:%d S:%d.",
								track, sector));
				return false;
			} else {
				gcr.convertGCRToSector(currSector, gcrDataPos,
						trackSize[track - 1]);
				if (currSector[0] != GCR.DATA_HEADER_START) {
					System.err.println(String.format(
							"Could not find data block id of T:%d S:%d.",
							track, sector));
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public final boolean save(final File file, final byte startTrack,
			final byte startSector) throws IOException {
		DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));
		byte[] currSector = new byte[GCR.SECTOR_SIZE];
		byte nextTrack, nextSector;
		int offset;

		int[] arrCyclicAccessInfo = new int[MAX_OVERALL_SECTORS];
		for (int i = 0; i < arrCyclicAccessInfo.length; i++) {
			arrCyclicAccessInfo[i] = 0;
		}
		nextTrack = startTrack;
		nextSector = startSector;
		while (true) {
			if (!getDiskSector(nextTrack, nextSector, currSector)) {
				dout.close();
				return false;
			}

			// check if cyclic sector chain
			offset = nextTrack * MAX_SECTORS_PER_TRACK + nextSector;
			if (arrCyclicAccessInfo[offset] != 0) {
				dout.close();
				return false;
			} else {
				arrCyclicAccessInfo[offset]++; // mark sector as read
			}
			// last sector ??
			if (currSector[1] == 0) {
				dout.write(currSector, 3, 254);
				dout.close();
				return true;
			}

			dout.write(currSector, 3, 254);
			nextTrack = currSector[1];
			nextSector = currSector[2];
		}
	}

}