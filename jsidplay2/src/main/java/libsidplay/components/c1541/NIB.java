package libsidplay.components.c1541;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class NIB extends G64 {

	private static class TrackCycle {
		public TrackCycle(final int start, final int stop) {
			cycleStart = start;
			cycleStop = stop;
		}

		/** start position of cycle */
		int cycleStart;
		/** stop position of cycle */
		int cycleStop;
	}

	/**
	 * Expected G64 image header.
	 */
	static final String IMAGE_HEADER = "MNIB-1541-RAW";

	/**
	 * NIB file track data length.
	 */
	private static final int MNIB_TRACK_LENGTH = 0x2000;

	private static final int MATCH_LENGTH = 7;
	private static final int MIN_TRACK_LENGTH = 0x1780;

	/**
	 * NIB file header.
	 */
	private byte[] nibHeader = new byte[0x100];
	/**
	 * GCR data of one track.
	 */
	private byte[] trackData = new byte[GCR.NUM_MAX_BYTES_TRACK];
	/**
	 * MNIB file data of one track.
	 */
	private byte[] mnibTrackData = new byte[MNIB_TRACK_LENGTH];

	/**
	 * Speed zone for each track (1-42).
	 */
	private static int speed_map_1541[] = {
			// 1-10
			3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
			// 11-20
			3, 3, 3, 3, 3, 3, 3, 2, 2, 2,
			// 21-30
			2, 2, 2, 2, 1, 1, 1, 1, 1, 1,
			// 31-35
			0, 0, 0, 0, 0,
			// 36 - 42 (non-standard)
			0, 0, 0, 0, 0, 0, 0 };

	/**
	 * Constructor.
	 * 
	 * @param gcr
	 *            GCR support
	 * @param fileName
	 *            disk image file name
	 * @param fd
	 *            file handle of the disk image
	 * @param readOnly
	 *            mount read-only?
	 */
	public NIB(final GCR gcr, final String fileName, final RandomAccessFile fd,
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
		fd.readFully(nibHeader);
		if (!new String(nibHeader, "ISO-8859-1").startsWith(IMAGE_HEADER)) {
			throw new IOException(
					String.format("GCR image is not MNIB-1541-RAW format."));
		}
		// Check track number
		tracks = MAX_TRACKS_1541;
		// Read offsets to track data
		int[] trackOffsets = new int[MAX_TRACKS_1541 << 1];
		// Read offsets to speed data
		int[] speedZoneOffsets = new int[MAX_TRACKS_1541 << 1];

		/* Create index and speed tables */
		for (int track = 0; track < MAX_TRACKS_1541; track++) {
			/* calculate track positions */
			trackOffsets[track * 2] = 12 + MAX_TRACKS_1541 * 16 + track * 7930;
			trackOffsets[track * 2 + 1] = 0; /* no halftracks */
			/* set speed zone data */
			speedZoneOffsets[track * 2] = (nibHeader[17 + track * 2] & 0x03);
			speedZoneOffsets[track * 2 + 1] = 0;
		}

		int trackDataPos = 0;
		// number of first nibble-track in nib image
		int headerOffset = 0x10;
		for (int track = 0; track < MAX_TRACKS_1541; track++) {
			Arrays.fill(trackData, (byte) 0xff);

			// Skip halftracks if present in image
			if (nibHeader[headerOffset] < (track + 1) << 1) {
				fd.seek(fd.getFilePointer() + MNIB_TRACK_LENGTH);
				headerOffset += 2;
			}
			headerOffset += 2;

			// read in one track
			try {
				fd.readFully(mnibTrackData);
			} catch (IOException ioE) {
				// track doesn't exist: write blank track
				Arrays.fill(trackData, (byte) 0x55);
				trackData[0] = (byte) 0xff;

				int trackLen = Rotation.ROT_SPEED_BPC[speed_map_1541[track]] / 40;
				trackSize[track] = trackLen;
				gcr.setTrackData(trackData, trackDataPos, trackLen);
				trackDataPos += trackLen;
				continue;
			}

			// System.out.printf("Track: %2d ", track + 1);
			/*
			 * source_track = check_vmax(mnib_track);
			 */
			int trackLen = extractGCRtrack(trackData, mnibTrackData);

			if (trackLen == 0) {
				trackLen = Rotation.ROT_SPEED_BPC[speed_map_1541[track]] / 40;
				Arrays.fill(trackData, (byte) 0x55);
				trackData[0] = (byte) 0xff;
			} else if (trackLen > GCR.NUM_MAX_BYTES_TRACK) {
				System.err.printf("  Warning: track too long, cropping to %d!",
						GCR.NUM_MAX_BYTES_TRACK);
				trackLen = GCR.NUM_MAX_BYTES_TRACK;
			}
			trackSize[track] = trackLen;
			// System.out.printf("- track length:  %d\n", trackLen);

			gcr.setTrackData(trackData, trackDataPos, GCR.NUM_MAX_BYTES_TRACK);
			trackDataPos += GCR.NUM_MAX_BYTES_TRACK;
		}
	}

	/**
	 * try to extract one complete cycle of GCR data from an 8kB buffer. Align
	 * track to sector gap if possible, else align to track 0, else copy cyclic
	 * loop from begin of source. If buffer is pure nonsense, return tracklen =
	 * 0; [Input] destination buffer, source buffer [Return] length of copied
	 * track fragment
	 */
	private int extractGCRtrack(byte[] dest, byte[] src) {
		/* start position of cycle */
		/* stop position of cycle */
		TrackCycle cycle = new TrackCycle(0, 0);
		/* position of sector 0 */
		int sector0Pos;
		/* position of sector gap */
		int sectorGapPos;

		findTrackCycle(src, cycle);
		int trackLen = cycle.cycleStop - cycle.cycleStart;
		if (trackLen == 0) {
			return 0;
		}
		/* working buffer */
		byte[] gcrData = new byte[MNIB_TRACK_LENGTH << 1];
		/* copy twice the data to work buffer */
		System.arraycopy(src, cycle.cycleStart, gcrData, 0, trackLen);
		System.arraycopy(src, cycle.cycleStart, gcrData, trackLen, trackLen);

		if (-1 != (sectorGapPos = findSectorGap(gcrData, trackLen))) {
			System.arraycopy(gcrData, sectorGapPos, dest, 0, trackLen);
		} else if (-1 != (sector0Pos = findSector0(gcrData, trackLen))) {
			System.arraycopy(gcrData, sector0Pos, dest, 0, trackLen);
		} else {
			System.arraycopy(gcrData, 0, dest, 0, trackLen);
		}
		return trackLen;
	}

	private int findTrackCycle(byte[] gcrData, TrackCycle cycle) {
		/* start of nibbled track data */
		int nibTrack = cycle.cycleStart;
		/* maximum position allowed for cycle */
		int stopPos = nibTrack + MNIB_TRACK_LENGTH - MATCH_LENGTH;

		for (int startPos = nibTrack; true; ) {
			/* cycle search variable */
			int syncPos;
			if ((syncPos = startPos + MIN_TRACK_LENGTH) >= stopPos) {
				cycle.cycleStop = cycle.cycleStart;
				return (0); /* no cycle found */
			}

			/* try to find next sync */
			while ((syncPos = findSync(gcrData, syncPos, stopPos)) != -1) {
				/* found a sync, now let's see if data matches */
				int p1 = startPos;
				/* start of cycle repetition */
				int cycle_pos = syncPos;
				for (int p2 = cycle_pos; p2 < stopPos;) {
					/* try to match all remaining syncs, too */
					if (!equals(gcrData, p1, p2, MATCH_LENGTH)) {
						cycle_pos = -1;
						break;
					}
					if ((p1 = findSync(gcrData, p1, stopPos)) == -1)
						break;
					if ((p2 = findSync(gcrData, p2, stopPos)) == -1)
						break;
				}

				if (cycle_pos != -1) {
					cycle.cycleStart = startPos;
					cycle.cycleStop = cycle_pos;
					return (cycle_pos - startPos);
				}
			}
			startPos = findSync(gcrData, startPos, stopPos);
			if (startPos == -1) {
				startPos = stopPos;
			}
		}
	}

	private boolean equals(byte[] gcrData, int from, int to, int length) {
		for (int i = 0; i < length; i++) {
			if (gcrData[from + i] != gcrData[to + i]) {
				return false;
			}
		}
		return true;
	}

	private int findSector0(byte[] gcrData, int tracklen) {
		int pos = 0;
		int bufferEnd = 2 * tracklen - 10;

		/* try to find sector 0 */
		while (pos < bufferEnd) {
			if ((pos = findSync(gcrData, pos, bufferEnd)) == -1)
				return -1;
			if ((gcrData[pos + 0] == 0x52)
					&& ((gcrData[pos + 1] & 0xc0) == 0x40)
					&& ((gcrData[pos + 2] & 0x0f) == 0x05)
					&& ((gcrData[pos + 3] & 0xfc) == 0x28)) {
				break;
			}
		}

		/* find last GCR byte before sync */
		do {
			pos--;
			if (pos == 0)
				pos += tracklen;
		} while (gcrData[pos] == (byte) 0xff);

		/* move to first sync GCR byte */
		pos++;
		while (pos >= tracklen)
			pos -= tracklen;

		return pos;
	}

	private int findSync(byte[] gcrData, int pos, int gcrEnd) {
		while (true) {
			if (pos + 1 >= gcrEnd) {
				pos = gcrEnd;
				return -1; /* not found */
			}
			if (((gcrData[pos + 0] & 0x03) == 0x03)
					&& (gcrData[pos + 1] == (byte) 0xff))
				break;
			pos++;
		}

		pos++;
		while ((pos < gcrEnd) && (gcrData[pos] == (byte) 0xff))
			pos++;
		return (pos < gcrEnd) ? pos : -1;
	}

	private int findSectorGap(byte[] gcrData, int tracklen) {
		int syncMax = 0;
		int pos = 0;
		int bufferEnd = (tracklen << 1) - 10;

		if ((pos = findSync(gcrData, pos, bufferEnd)) == -1) {
			return -1;
		}
		int syncLast = pos;
		int maxgap = 0;
		/* try to find biggest (sector) gap */
		while (pos < bufferEnd) {
			if ((pos = findSync(gcrData, pos, bufferEnd)) == -1)
				break;
			int gap = pos - syncLast;
			if (gap > maxgap) {
				maxgap = gap;
				syncMax = pos;
			}
			syncLast = pos;
		}

		if (maxgap == 0)
			return -1; /* no gap found */

		/* find last GCR byte before sync */
		pos = syncMax;
		do {
			pos--;
			if (pos == 0)
				pos += tracklen;
		} while (gcrData[pos] == (byte) 0xff);

		/* move to first sync GCR byte */
		pos++;
		while (pos >= tracklen)
			pos -= tracklen;

		return pos;
	}

	@Override
	public void gcrDataWriteback(final int track) throws IOException {
		System.err.println("Writing to nib files is not supported!");
	}

}
