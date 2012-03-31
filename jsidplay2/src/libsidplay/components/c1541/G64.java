/**
 * G64 disk image.
 * 
 * @author Ken
 */
package libsidplay.components.c1541;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * @author Ken Händel
 * 
 */
public class G64 extends DiskImage {

	/**
	 * Expected G64 image header.
	 */
	static final String IMAGE_HEADER = "GCR-1541";
	/**
	 * GCR image file offset, where the track data offsets start.
	 */
	private static final int BEGIN_TRACK_DATA_OFFSETS = 12;

	/**
	 * Speed zone (0..3) of the disk image (for every track and every byte).
	 */
	private int[] speedZoneMap = new int[GCR.MAX_GCR_TRACKS
			* GCR.NUM_MAX_BYTES_TRACK];

	public G64(final GCR gcr, final String fileName, final RandomAccessFile fd,
			final boolean readOnly) {
		super(gcr, fileName, fd, readOnly);
	}

	/**
	 * Read image in G64 format and fill this GCR data and speed zone data.
	 * 
	 * @throws IOException
	 *             disk image file could not be attached
	 */
	@Override
	protected void attach() throws IOException {
		// Check image header
		final byte[] header = new byte[IMAGE_HEADER.length()];
		fd.readFully(header);
		if (!new String(header, "ISO-8859-1").equals(IMAGE_HEADER)) {
			throw new IOException(
					String.format("GCR image is not GCR-1541 format."));
		}
		// Check version number
		final int version = fd.read();
		if (version != 0) {
			throw new IOException(String.format(
					"Unknown GCR image version %d.", version));
		}
		// Check track number
		tracks = fd.read() >> 1;
		if (tracks < MIN_TRACKS_1541 || tracks > MAX_TRACKS_1541) {
			throw new IOException(String.format(
					"Invalid number of tracks (%d).", tracks));
		}
		// Check GCR raw track size
		final int rawTrackSize = fd.read() + (fd.read() << 8);
		if (rawTrackSize != GCR.NUM_MAX_BYTES_TRACK) {
			throw new IOException(String.format(
					"Unexpected GCR raw track size: %s (expected: %s)",
					rawTrackSize, GCR.NUM_MAX_BYTES_TRACK));
		}

		// Read offsets to track data
		int[] trackOffsets = new int[MAX_TRACKS_1541 << 1];
		readIntLittleEndian(fd, trackOffsets, tracks << 1);
		// Read offsets to speed data
		int[] speedZoneOffsets = new int[MAX_TRACKS_1541 << 1];
		readIntLittleEndian(fd, speedZoneOffsets, tracks << 1);

		int trackDataPos = 0;
		int zoneDataPos = 0;
		for (int track = 1; track <= MAX_TRACKS_1541; track++) {
			// Initialize GCR data and speed zone data
			gcr.setTrackData(trackDataPos, GCR.NUM_MAX_BYTES_TRACK, (byte) 0xff);
			Arrays.fill(speedZoneMap, zoneDataPos, zoneDataPos
					+ (GCR.NUM_MAX_BYTES_TRACK >> 2), 0x00);

			trackSize[track - 1] = DiskImage.RAW_TRACK_SIZE[SPEED_MAP_1541[track - 1]];

			if (track <= tracks && trackOffsets[(track - 1) << 1] != 0) {
				long gcrTrackDataStart = trackOffsets[(track - 1) << 1];

				// Determine track size
				fd.seek(gcrTrackDataStart);
				int trackLen = fd.read() + (fd.read() << 8);
				if (trackLen > GCR.NUM_MAX_BYTES_TRACK) {
					throw new IOException(
							String.format(
									"Track field length %d is not supported.",
									trackLen));
				}
				trackSize[track - 1] = trackLen;

				// Read GCR data
				fd.seek(gcrTrackDataStart + 2);
				final byte[] trackBytes = new byte[trackLen];
				fd.readFully(trackBytes);
				gcr.setTrackData(trackBytes, trackDataPos, trackLen);

				if (speedZoneOffsets[(track - 1) << 1] <= 3) {
					// Speed zone 0-3 of the whole track
					Arrays.fill(speedZoneMap, zoneDataPos, zoneDataPos
							+ GCR.NUM_MAX_BYTES_TRACK,
							speedZoneOffsets[(track - 1) << 1]);
				} else {
					// Speed zone is offset to variable speed data
					long speedZoneOffsetStart = speedZoneOffsets[(track - 1) << 1];

					final byte[] speedData = new byte[GCR.NUM_MAX_BYTES_TRACK >> 2];
					fd.seek(speedZoneOffsetStart);
					fd.readFully(speedData, 0, (trackLen + 3) >> 2);
					for (int i = 0; i < speedData.length; i++) {
						speedZoneMap[zoneDataPos + (i << 2) + 3] = speedData[i] & 3;
						speedZoneMap[zoneDataPos + (i << 2) + 2] = ((speedData[i] & 0xff) >> 2) & 3;
						speedZoneMap[zoneDataPos + (i << 2) + 1] = ((speedData[i] & 0xff) >> 4) & 3;
						speedZoneMap[zoneDataPos + (i << 2)] = ((speedData[i] & 0xff) >> 6) & 3;
					}
				}
			}
			trackDataPos += GCR.NUM_MAX_BYTES_TRACK;
			zoneDataPos += GCR.NUM_MAX_BYTES_TRACK;
		}
	}

	/**
	 * Write GCR data back to image.
	 * 
	 * @param track
	 *            dirty track
	 * @throws IOException
	 *             error writing data to image
	 */
	@Override
	public void gcrDataWriteback(final int track) throws IOException {
		// Read offsets to track data
		int[] trackOffsets = new int[MAX_TRACKS_1541 << 1];
		fd.seek(BEGIN_TRACK_DATA_OFFSETS);
		readIntLittleEndian(fd, trackOffsets, tracks << 1);
		// Read offsets to speed data
		int[] speedZoneOffsets = new int[MAX_TRACKS_1541 << 1];
		fd.seek(BEGIN_TRACK_DATA_OFFSETS + tracks << 3);
		readIntLittleEndian(fd, speedZoneOffsets, tracks << 1);

		int gcrTrackDataStart = trackOffsets[(track - 1) << 1];
		if (gcrTrackDataStart == 0) {
			// Extended track will be written
			if (track > tracks) {
				// Allowed to extend the disk image to 40 tracks?
				if (extendImageListener != null
						&& !extendImageListener.isAllowed()) {
					// Forbidden by policy
					return;
				}
			}
			// Extend disk image
			fd.seek(fd.length());
			gcrTrackDataStart = trackOffsets[(track - 1) << 1] = 0;
		}
		// Clear gap between the end of the actual track and the start of the
		// next track.
		int gap = GCR.NUM_MAX_BYTES_TRACK - trackSize[track - 1];
		if (gap > 0) {
			gcr.setTrackData(((track - 1) * GCR.NUM_MAX_BYTES_TRACK),
					trackSize[track - 1] + gap, (byte) 0);
		}

		// Write track length
		fd.seek(gcrTrackDataStart);
		fd.write(trackSize[track - 1]);
		fd.write(trackSize[track - 1] >> 8);

		// Write track
		fd.seek(gcrTrackDataStart + 2);
		fd.write(gcr.getTrackData(((track - 1) * GCR.NUM_MAX_BYTES_TRACK),
				GCR.NUM_MAX_BYTES_TRACK));

		if (speedZoneMap != null) {
			// Detect different speed zones within a track
			for (int i = 0; i < GCR.NUM_MAX_BYTES_TRACK; i++) {
				if ((speedZoneMap[(track - 1) * GCR.NUM_MAX_BYTES_TRACK] != speedZoneMap[(track - 1)
						* GCR.NUM_MAX_BYTES_TRACK + i])) {
					System.err.printf("Saving different speed zones"
							+ " is not supported yet (track=%d).", track);
					return;
				}
			}
			// Detect speed zones to add
			if (speedZoneOffsets[(track - 1) << 1] > 3) {
				System.err.printf("Adding new speed zones"
						+ " is not supported yet (track=%d).", track);
				return;
			}
			// Write speed zones
			gcrTrackDataStart = BEGIN_TRACK_DATA_OFFSETS + (tracks << 3) + ((track - 1) << 3);
			fd.seek(gcrTrackDataStart);
			fd.writeInt(flip(speedZoneOffsets[(track - 1) << 1]));

			// TODO We do not support writing different speeds yet.
		}

	}

	/**
	 * Reverses endianness of 32-bit integer.
	 * 
	 * @param value
	 *            value to flip
	 * @return value with endianness flipped
	 */
	private int flip(final int value) {
		return (value >> 24) & 0xff | (value >> 8) & 0xff00 | (value << 8)
				& 0xff0000 | (value << 24);
	}

	/**
	 * Reads integer little-endian integers from a file (at the current
	 * position).
	 * 
	 * @param fd
	 *            file handle
	 * @param buf
	 *            target buffer containing the integer values of the file
	 * @param num
	 *            number of integers to read
	 * @throws IOException
	 *             read error occurred
	 */
	private void readIntLittleEndian(final RandomAccessFile fd,
			final int[] buf, final int num) throws IOException {
		for (int i = 0; i < num; i++) {
			buf[i] = flip(fd.readInt());
		}
	}

}
