/**
 * gcr.c - GCR handling.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *  Daniel Sladic <sladic@eecg.toronto.edu>
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

import static libsidplay.components.c1541.DOSErrorCodes.*;

import java.util.Arrays;

/**
 * 
 * Group Coded Recording.
 * 
 * @author Ken Händel
 * 
 */
public final class GCR {
	/**
	 * Number of tracks we emulate.
	 */
	public static final int MAX_GCR_TRACKS = 70;
	/**
	 * Number of bytes in one raw track.
	 */
	public static final int NUM_MAX_BYTES_TRACK = 7928;
	/**
	 * Byte size of a whole sector.
	 */
	public static final int SECTOR_SIZE = 260;
	/**
	 * GCR byte size of a whole sector's data.
	 */
	private static final int GCR_SECTOR_SIZE_DATA_ONLY = 325;
	/**
	 * Begin of sector block header.
	 */
	public static final byte BLOCK_HEADER_START = 0x08;
	/**
	 * Begin of sector data header.
	 */
	public static final byte DATA_HEADER_START = 0x07;
	/**
	 * Convert a nybble to GCR code.
	 * 
	 * <PRE>
	 * Nybble Code
	 * 0000	  01010
	 * 0001	  01011
	 * 0010	  10010
	 * 0011	  10011
	 * 0100	  01110
	 * 0101	  01111
	 * 0110	  10110
	 * 0111	  10111
	 * 1000	  01001
	 * 1001	  11001
	 * 1010	  11010
	 * 1011	  11011
	 * 1100	  01101
	 * 1101	  11101
	 * 1110	  11110
	 * 1111	  10101
	 * </PRE>
	 */
	private static final byte[] TO_GCR = { 0x0a, 0x0b, 0x12, 0x13, 0x0e, 0x0f,
			0x16, 0x17, 0x09, 0x19, 0x1a, 0x1b, 0x0d, 0x1d, 0x1e, 0x15 };
	/**
	 * GCR to byte table.
	 */
	private static final byte[] FROM_GCR = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0,
			1, 0, 12, 4, 5, 0, 0, 2, 3, 0, 15, 6, 7, 0, 9, 10, 11, 0, 13, 14, 0 };
	
	/**
	 * Complete disk image as GCR data.
	 */
	private byte[] data = new byte[MAX_GCR_TRACKS * NUM_MAX_BYTES_TRACK];
	/**
	 * Offset into GCR data, the start of the current GCR track data.
	 */
	private int dataPos;
	/**
	 * Offset of the R/W head on the current track (bytes).
	 */
	private int gcrHeadOffset;

	/**
	 * Is a disk attached to the disk drive?
	 */
	private boolean isDiskAttached = false;
	
	/**
	 * Get GCR data of a whole track.
	 * 
	 * @param trackPos
	 *            track offset
	 * @param trackSize
	 *            track size
	 * @return GCR data
	 */
	protected byte[] getTrackData(final int trackPos, final int trackSize) {
		final byte[] trackBytes = new byte[trackSize];
		System.arraycopy(data, trackPos, trackBytes, 0, trackSize);
		return trackBytes;
	}

	/**
	 * Set GCR data of a whole track.
	 * 
	 * @param trackPos
	 *            track offset
	 * @param trackSize
	 *            track size
	 * @param byt
	 *            GCR value to be used for the whole track
	 */
	protected void setTrackData(final int trackPos, final int trackSize,
			final byte byt) {
		Arrays.fill(data, trackPos, trackPos + trackSize, byt);
	}

	/**
	 * Set GCR data of a whole track.
	 * 
	 * @param trackBytes
	 *            GCR data of a whole track
	 * @param trackPos
	 *            track offset
	 * @param trackSize
	 *            track size
	 */
	protected void setTrackData(final byte[] trackBytes, final int trackPos,
			final int trackSize) {
		System.arraycopy(trackBytes, 0, data, trackPos, trackSize);
	}

	/**
	 * Attach disk.
	 */
	protected void attach() {
		isDiskAttached = true;
	}

	/**
	 * Detach disk, reset GCR data.
	 */
	protected void detach() {
		Arrays.fill(data, (byte) 0);
		isDiskAttached = false;
	}

	/**
	 * Reset GCR data offset.
	 */
	protected void reset() {
		dataPos = 0;
		gcrHeadOffset = 0;
	}
	
	/**
	 * Convert the contents of a disk sector to GCR coded bytes. Input is a
	 * pre-formatted sector filled with 0x55 bytes (gap data).
	 * 
	 * @param sectorBytes
	 *            sector data to convert
	 * @param off
	 *            position of result
	 * @param track
	 *            track, where sector is located in
	 * @param sector
	 *            sector to convert
	 * @param diskID1
	 *            first byte of the disk ID
	 * @param diskID2
	 *            second byte of the disk ID
	 * @param errorCode
	 *            error code
	 */
	protected void convertSectorToGCR(final byte[] sectorBytes, int off,
			final int track, final int sector, final byte diskID1,
			final byte diskID2, final DOSErrorCodes errorCode) {
		byte[] buf = new byte[4];

		/* Sync (5 times 0xff) */
		Arrays.fill(data, off, off + 5, (byte) 0xff);
		off += 5;

		byte headerId1 = (byte) ((errorCode == CBMDOS_IPE_DISK_ID_MISMATCH) ? diskID1 ^ 0xff
				: diskID1);

		/* GCR block header */

		// Part1:
		// 8 - $08 begin of block header
		// CKS - checksum
		// S - sector number
		// T - track number
		buf[0] = (byte) ((errorCode == CBMDOS_IPE_READ_ERROR_BNF) ? 0xff
				: BLOCK_HEADER_START);
		buf[1] = (byte) (sector ^ track ^ diskID2 ^ headerId1);
		buf[2] = (byte) sector;
		buf[3] = (byte) track;

		if (errorCode == CBMDOS_IPE_READ_ERROR_BCHK) {
			buf[1] ^= 0xff;
		}
		convert4BytesToGCR(buf, 0, data, off);
		off += 5;

		// Part2:
		// ID2 - 2nd ASCII-code of disk ID
		// ID1 - 1st ASCII-code of disk ID
		// 0F - unused
		// 0F - unused
		buf[0] = diskID2;
		buf[1] = headerId1;
		buf[2] = 0x0f;
		buf[3] = 0x0f;
		convert4BytesToGCR(buf, 0, data, off);
		off += 5;

		// gap (10 GCR bytes)
		off += 10;

		/* Sync (5 times 0xff) */
		Arrays.fill(data, off, off + 5, (byte) 0xff);
		off += 5;

		/* GCR data */

		// 7 - $07 begin of data header
		sectorBytes[0] = (byte) ((errorCode == CBMDOS_IPE_READ_ERROR_DATA) ? 0xff : DATA_HEADER_START);
		// CKS - checksum
		int chksum = sectorBytes[1];
		for (int i = 2; i < 257; i++) {
			chksum ^= sectorBytes[i];
		}
		sectorBytes[257] = (byte) ((errorCode == CBMDOS_IPE_READ_ERROR_CHK) ? chksum ^ 0xff : chksum);
		sectorBytes[258] = sectorBytes[259] = 0; // filler???
		// header + 256 bytes data + checksum
		int sectorBytesPos = 0;
		for (int i = 0; i < GCR_SECTOR_SIZE_DATA_ONLY / 5; i++) {
			convert4BytesToGCR(sectorBytes, sectorBytesPos, data, off);
			sectorBytesPos += 4;
			off += 5;
		}
	}

	/**
	 * Convert 4 bytes into the GCR coded form (5 bytes).
	 * 
	 * @param source
	 *            byte array to convert containing 4 bytes.
	 * @param srcPos
	 *            position of source
	 * @param dest
	 *            resulting byte array
	 * @param dstPos
	 *            position of dest
	 */
	private void convert4BytesToGCR(final byte[] source, int srcPos,
			final byte[] dest, int dstPos) {
		int idx = 0;

		for (int i = 2; i < 10; i += 2, srcPos++, dstPos++) {
			/* make room for the upper nybble */
			idx <<= 5;
			idx |= TO_GCR[(source[srcPos] & 0xff) >> 4];

			/* make room for the lower nybble */
			idx <<= 5;
			idx |= TO_GCR[(source[srcPos]) & 0x0f];

			dest[dstPos] = (byte) (idx >> i);
		}

		dest[dstPos] = (byte) idx;
	}

	/**
	 * Convert a GCR coded sector into bytes.
	 * 
	 * @param dest
	 *            resulting GCR byte array
	 * @param dstPos
	 *            position of the GCR data containing the sector
	 *            the sector
	 * @param trackSize
	 *            the track size
	 */
	protected void convertGCRToSector(final byte[] dest, int dstPos,
			final int trackSize) {
		byte[] header = new byte[5];
		int bufferPos = 0;
		int trackEnd = dataPos + trackSize;
		for (int i = 0; i < (GCR_SECTOR_SIZE_DATA_ONLY / 5); i++) {
			for (int j = 0; j < header.length; j++) {
				header[j] = data[dstPos++];
				if (dstPos >= trackEnd) {
					dstPos = dataPos;
				}
			}
			convertGCRTo4Bytes(header, dest, bufferPos);
			bufferPos += 4;
		}
	}

	/**
	 * Search for the sector header of the given track and sector in the GCR
	 * data.
	 * 
	 * @param track
	 *            track where the sector is contained in
	 * @param sector
	 *            sector to search for
	 * @param trackSize
	 *            the track size
	 * @return offset in the GCR data pointing to the sector or -1 (not found)
	 */
	protected int findSectorHeader(final int track, final int sector,
			final int trackSize) {
		int offset = dataPos;
		byte[] headerAsGCR = new byte[5], header = new byte[4];
		boolean wrapOver = false;
		int syncCount = 0;
		int trackEnd = dataPos + trackSize;
		while ((offset < trackEnd) && !wrapOver) {
			while (data[offset] != (byte) 0xff) {
				offset++;
				if (offset >= trackEnd) {
					return -1;
				}
			}
			while (data[offset] == (byte) 0xff) {
				offset++;
				if (offset == trackEnd) {
					offset = dataPos;
					wrapOver = true;
				}
				/* Check for killer tracks. */
				if ((++syncCount) >= trackSize) {
					return -1;
				}
			}

			for (int i = 0; i < 5; i++) {
				headerAsGCR[i] = data[offset++];
				if (offset >= trackEnd) {
					offset = dataPos;
					wrapOver = true;
				}
			}

			convertGCRTo4Bytes(headerAsGCR, header, 0);

			if (header[0] == BLOCK_HEADER_START) {
				/* FIXME: Add some sanity checks here. */
				if (header[2] == sector && header[3] == track) {
					return offset;
				}
			}
		}
		return -1;
	}

	/**
	 * Find sector GCR data starting at the sector header position.
	 * 
	 * @param sectorHeaderPos
	 *            search start position
	 * @param trackSize
	 *            the track size
	 * @return offset in the GCR data pointing to the sector data or -1 (not
	 *         found)
	 */
	protected int findSectorData(final int sectorHeaderPos, final int trackSize) {
		int trackEnd = dataPos + trackSize;
		int header = 0;
		int sectorDataPos = sectorHeaderPos;
		while (data[sectorDataPos] != (byte) 0xff) {
			sectorDataPos++;
			if (sectorDataPos >= trackEnd) {
				sectorDataPos = dataPos;
			}
			header++;
			if (header >= 500) {
				return -1;
			}
		}

		while (data[sectorDataPos] == (byte) 0xff) {
			sectorDataPos++;
			if (sectorDataPos == trackEnd) {
				sectorDataPos = dataPos;
			}
		}
		return sectorDataPos;
	}

	/**
	 * Convert 5-byte GCR code to 4-byte.
	 * 
	 * @param source
	 *            source buffer containing the 5-byte GCR code.
	 * @param dest
	 *            target buffer to hold the result
	 * @param destPos
	 *            off set into the target buffer
	 */
	private void convertGCRTo4Bytes(final byte[] source, final byte[] dest,
			final int destPos) {
		/* at least 24 bits for shifting into bits 16...20 */
		int idx = source[0] & 0xff;
		idx <<= 13;

		int sourcePos = 0;
		int off = destPos;
		for (int i = 5; i < 13; i += 2, off++) {
			sourcePos++;
			idx |= (source[sourcePos] & 0xff) << i;

			dest[off] = (byte) ((FROM_GCR[(idx >> 16) & 0x1f] & 0xff) << 4);
			idx <<= 5;

			dest[off] |= FROM_GCR[(idx >> 16) & 0x1f];
			idx <<= 5;
		}
	}

	protected int readNextBit(final int currentTrackSize) {
		if (!isDiskAttached) {
			/* if no image is attached, read 0 */
			return 0;
		}
		int byteOffset = gcrHeadOffset >> 3;
		int bitNumber = (~gcrHeadOffset) & 7;
		gcrHeadOffset = (gcrHeadOffset + 1) % (currentTrackSize << 3);
		byte gcrData = data[dataPos + byteOffset];
		return (gcrData >> bitNumber) & 1;
	}

	protected void writeNextBit(final boolean value, final int currentTrackSize) {
		if (!isDiskAttached) {
			/* if no image is attached, writes do nothing */
			return;
		}
		int byteOffset = gcrHeadOffset >> 3;
		int bitNumber = (~gcrHeadOffset) & 7;
		gcrHeadOffset = (gcrHeadOffset + 1) % (currentTrackSize << 3);

		byte gcrData = data[dataPos + byteOffset];
		if (value) {
			data[dataPos + byteOffset] = (byte) (gcrData | 1 << bitNumber);
		} else {
			data[dataPos + byteOffset] = (byte) (gcrData & ~(1 << bitNumber));
		}
	}

	/**
	 * Set the current GCR data track position.
	 * 
	 * @param num
	 *            half-track to set
	 *
	 * @param  oldTrackSize
	 * @param  currentTrackSize
	 */
	protected final void setHalfTrack(final int num, final int oldTrackSize,
			final int currentTrackSize) {
		dataPos = ((num >> 1) - 1) * NUM_MAX_BYTES_TRACK;
		gcrHeadOffset = gcrHeadOffset * oldTrackSize / currentTrackSize;
	}

}