/**
 * D64 disk images.
 * 
 * @author Ken
 */
package libsidplay.components.c1541;

import static libsidplay.components.c1541.DOSErrorCodes.CBMDOS_IPE_READ_ERROR_SYNC;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Ken Händel
 * 
 */
public final class D64 extends DiskImage {

	/**
	 * D64 image byte size (35 tracks).
	 */
	private static final int D64_FILE_SIZE_35 = 174848;
	/**
	 * D64 sector byte size.
	 */
	private static final int D64_SECTOR_SIZE = 256;
	/**
	 * GCR byte size of a whole sector.
	 */
	private static final int GCR_SECTOR_SIZE_WITH_HEADER = 354;
	/**
	 * Number of gap bytes between sectors of the speedzones 0-3.
	 */
	private static final int[] GAPS_BETWEEN_SECTORS = { 9, 12, 17, 8 };
	/**
	 * Number of sectors per track of the speedzones 0-3.
	 */
	private static final int[] SECTOR_MAP_D64 = { 17, 18, 19, 21 };
	/**
	 * Disk ID.
	 */
	private byte diskID1, diskID2;
	/**
	 * Error in each sector appended to the D64 disk image.
	 */
	private byte[] errorInfo;

	public D64(final GCR gcr, final String fileName, final RandomAccessFile fd,
			final boolean readOnly) {
		super(gcr, fileName, fd, readOnly);
	}

	/**
	 * Detect 35..42 track d64 image, determine image parameters. Walk from 35
	 * to 42, calculate expected image file size for each track, and compare
	 * this with the size of the given image.
	 * 
	 * @throws IOException
	 *             disk image file read error
	 */
	@Override
	protected void attach() throws IOException {
		long imageFilesize = fd.length();
		// Auto-detection start parameters (track 35)
		tracks = MIN_TRACKS_1541;
		int imageBlocks = D64_FILE_SIZE_35 >> 8;

		boolean hasErrorinfo = false;
		// detect image by increasing parameters (track 35-42);
		while (true) {
			// check if image file size matches
			// the currently tested image size
			if (imageFilesize == imageBlocks << 8) {
				// image found without error information
				hasErrorinfo = false;
				break;

			} else if (imageFilesize == (imageBlocks << 8) + imageBlocks) {
				// image found plus error information
				hasErrorinfo = true;
				break;
			}

			imageBlocks += SECTOR_MAP_D64[SPEED_MAP_1541[tracks]];
			// try next track
			tracks++;

			// we tried them all up to 42, none worked, image must be corrupt
			if (tracks >= MAX_TRACKS_1541) {
				throw new IOException("Image must be corrupt!");
			}
		}

		// Read error information contained in the first block
		errorInfo = new byte[imageBlocks];
		if (hasErrorinfo) {
			fd.seek((imageBlocks << 8));
			fd.readFully(errorInfo, 0, imageBlocks);
		}

//		System.out.printf("D64 disk image recognised: %s, %d tracks%s.\n",
//				filename, tracks, readOnly ? " (read only)" : "");

		// set Disk ID
		byte[] sectorBytes = new byte[D64_SECTOR_SIZE];
		readSector(sectorBytes, 0, DIR_TRACK_1541, 0);
		diskID1 = sectorBytes[0xa2];
		diskID2 = sectorBytes[0xa3];

		// Initialize track size and speed zone for all tracks.
		// D64 does not contain speed zone info, therefore assume common
		// defaults
		for (int track = 1; track <= MAX_TRACKS_1541; track++) {
			trackSize[track - 1] = RAW_TRACK_SIZE[SPEED_MAP_1541[track - 1]];
		}

		final byte[] gcrSectorBytes = new byte[GCR.SECTOR_SIZE];
		for (int track = 1; track <= tracks; track++) {
			// Point to the GCR data where the track is located
			int gcrDataPos = (track - 1) * GCR.NUM_MAX_BYTES_TRACK;

			// Clear the whole track to avoid read errors
			gcr.setTrackData(gcrDataPos, GCR.NUM_MAX_BYTES_TRACK, (byte) 0x55);

			int sectorPerTrack = SECTOR_MAP_D64[SPEED_MAP_1541[track - 1]];
			for (int sector = 0; sector < sectorPerTrack; sector++) {
				final DOSErrorCodes errorCode = readSector(gcrSectorBytes, 1,
						track, sector);
				if (errorCode == CBMDOS_IPE_READ_ERROR_SYNC) {
					// If the data block is not found, the whole track gets
					// zeroed
					gcrDataPos = (track - 1) * GCR.NUM_MAX_BYTES_TRACK;
					gcr.setTrackData(gcrDataPos, GCR.NUM_MAX_BYTES_TRACK,
							(byte) 0x00);
					break;
				}
				gcr.convertSectorToGCR(gcrSectorBytes, gcrDataPos, track,
						sector, diskID1, diskID2, errorCode);

				// Point to the GCR data where the next sector is located
				gcrDataPos += GAPS_BETWEEN_SECTORS[SPEED_MAP_1541[track - 1]]
						+ GCR_SECTOR_SIZE_WITH_HEADER;
			}
		}
	}

	/**
	 * Read sector of disk image.
	 * 
	 * @param sectorBytes
	 *            sector bytes read from track
	 * @param sectorBytesPos
	 *            offset with the start of the sector bytes
	 * @param track
	 *            track
	 * @param sector
	 *            sector
	 * @return DOS error code of the sector to read
	 * @throws IOException
	 *             error reading disk image
	 */
	private DOSErrorCodes readSector(final byte[] sectorBytes,
			final int sectorBytesPos, final int track, final int sector)
			throws IOException {
		int sectorCount = getSectorCount(track, sector);
		fd.seek(sectorCount << 8);
		fd.readFully(sectorBytes, sectorBytesPos, D64_SECTOR_SIZE);
		return DOSErrorCodes.valueOf(errorInfo[sectorCount]);
	}

	/**
	 * Count sectors up to the given location.
	 * 
	 * @param track
	 *            track
	 * @param sector
	 *            sector
	 * @return sector number
	 */
	private int getSectorCount(final int track, final int sector) {
		int sectorCount = sector;
		for (int i = 1; i < track; i++) {
			sectorCount += SECTOR_MAP_D64[SPEED_MAP_1541[i - 1]];
		}
		return sectorCount;
	}

	@Override
	public void gcrDataWriteback(final int track) throws IOException {
		if (track > EXT_TRACKS_1541) {
			// tracks 41-42 can't be written
			return;
		}
		int sectorsPerTrack = SECTOR_MAP_D64[SPEED_MAP_1541[track - 1]];
		if (track > tracks) {
			// Allowed to extend the disk image to 40 tracks?
			// No one to ask for means yes.
			if (extendImageListener == null || extendImageListener.isAllowed()) {
				driveExtendDiskImage();
			} else {
				// Forbidden by policy
				return;
			}
		}

		// Write back each sector of the track
		for (int sector = 0; sector < sectorsPerTrack; sector++) {
			int gcrDataPos = gcr.findSectorHeader(track, sector,
					trackSize[track - 1]);
			if (gcrDataPos == -1) {
				System.err.println(String.format(
						"Could not find header of T:%d S:%d.", track, sector));
			} else {
				gcrDataPos = gcr.findSectorData(gcrDataPos,
						trackSize[track - 1]);
				if (gcrDataPos == -1) {
					System.err.println(String.format(
							"Could not find data sync of T:%d S:%d.", track,
							sector));
				} else {
					gcrDataWritebackSector(gcrDataPos, track, sector);
				}
			}
		}
	}

	/**
	 * Extend disk image to 42 tracks.
	 * 
	 * @throws IOException
	 *             disk image write error
	 */
	private void driveExtendDiskImage() throws IOException {
		byte[] buffer = new byte[D64_SECTOR_SIZE];
		tracks = EXT_TRACKS_1541;
		for (int tr = MIN_TRACKS_1541 + 1; tr <= EXT_TRACKS_1541; tr++) {
			int sectorSize = SECTOR_MAP_D64[SPEED_MAP_1541[tr - 1]];
			for (int sec = 0; sec < sectorSize; sec++) {
				writeSector(buffer, 0, tr, sec);
			}
		}
	}

	/**
	 * Write back dirty sector into the disk image.
	 * 
	 * @param offset
	 *            offset of the GCR data
	 * @param track
	 *            track
	 * @param sector
	 *            sector
	 * @throws IOException
	 *             disk image write error
	 */
	private void gcrDataWritebackSector(final int offset, final int track,
			final int sector) throws IOException {
		byte[] sectorBytes = new byte[GCR.SECTOR_SIZE];
		gcr.convertGCRToSector(sectorBytes, offset, trackSize[track - 1]);
		if (sectorBytes[0] != GCR.DATA_HEADER_START) {
			System.err.println(String
					.format("Could not find data block id of T:%d S:%d.",
							track, sector));
		} else {
			writeSector(sectorBytes, 1, track, sector);
		}
	}

	/**
	 * Write a sector.
	 * 
	 * @param sectorBytes
	 *            sector bytes to write
	 * @param sectorBytesPos
	 *            offset of the sector bytes
	 * @param track
	 *            track
	 * @param sector
	 *            sector
	 * @throws IOException
	 *             disk image write error
	 */
	private void writeSector(final byte[] sectorBytes,
			final int sectorBytesPos, final int track, final int sector)
			throws IOException {
		int sectorCount = getSectorCount(track, sector);
		fd.seek(sectorCount << 8);
		fd.write(sectorBytes, sectorBytesPos, D64_SECTOR_SIZE);
	}

}
