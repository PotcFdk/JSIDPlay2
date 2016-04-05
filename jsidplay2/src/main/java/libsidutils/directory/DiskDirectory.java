package libsidutils.directory;

import java.io.File;
import java.io.IOException;

import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.GCR;

public class DiskDirectory {

	/**
	 * Number of directory entries per block.
	 */
	private static final int NR_ENTRIES_PER_BLOCK = 7;
	/**
	 * Size in bytes of a directory entry.
	 */
	private static final int DIR_ENTRY_SIZE = 32;

	public static final Directory getDirectory(final File file) throws IOException {
		final Directory dir = new Directory();
		final DiskImage img = DiskImage.attach(new GCR(), file);

		int[] arrCyclicAccessInfo = new int[DiskImage.MAX_OVERALL_SECTORS];

		// set Disk ID
		byte[] sectorBytes = new byte[GCR.SECTOR_SIZE];
		img.getDiskSector(DiskImage.DIR_TRACK_1541, 0, sectorBytes);
		// Get disk title/ID
		int afterBamInfos = (5 + DiskImage.MIN_TRACKS_1541 * 4);
		int freeBlocks = 0;
		for (int i = 1; i <= 35; i++) {
			if (i != 18) {
				freeBlocks += sectorBytes[5 + ((i - 1) * 4)] & 0xff;
			}
		}
		byte[] diskName = new byte[16];
		byte[] diskID = new byte[5];
		System.arraycopy(sectorBytes, afterBamInfos, diskName, 0, diskName.length);
		System.arraycopy(sectorBytes, afterBamInfos + 18, diskID, 0, diskID.length);
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
			if (nextEntry == 0 && currNextTrack != 0 && arrCyclicAccessInfo[nextSector] != 0) {
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
			System.arraycopy(currSector, 1 + nextEntry * DIR_ENTRY_SIZE, entryBytes, 0, DIR_ENTRY_SIZE);
			final byte fileType = entryBytes[2];
			if (fileType != DirEntry.FILETYPE_DEL) {
				final byte firstFileTrack = entryBytes[3];
				final byte firstFileSector = entryBytes[4];
				final byte[] fn = new byte[16];
				System.arraycopy(entryBytes, 5, fn, 0, 16);
				final byte lowByte = entryBytes[30];
				final byte highByte = entryBytes[31];
				final int nrSectors = (lowByte & 0xff) + ((highByte & 0xff) << 8);
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
}
