package libsidutils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import libsidutils.stringsearch.BNDMWildcards;

/**
 * Scan tune files to find out the player ID. A configuration file is used with
 * patterns of well-known players.
 * 
 * @author Ken Händel
 * 
 */
public final class SidIdV2 {

	/**
	 * This is a single pattern to match a player.
	 * 
	 * @author Ken Händel
	 * 
	 */
	private static class Pattern {
		/**
		 * Pattern to search for.
		 */
		private final byte[] pattern;
		/**
		 * Pre-processed search pattern.
		 */
		private final Object preProcessedPattern;
		/**
		 * Is this a splitted sub pattern to match?
		 */
		private final boolean isSubPattern;

		/**
		 * Create a new pattern.
		 * 
		 * @param p
		 *            pattern of the player
		 * @param pre
		 *            pre-processed pattern
		 * @param subPattern
		 *            Is this a splitted sub pattern to match?
		 */
		public Pattern(final byte[] p, final Object pre,
				final boolean subPattern) {
			this.pattern = p;
			this.preProcessedPattern = pre;
			this.isSubPattern = subPattern;
		}

		/**
		 * Get search pattern.
		 * 
		 * @return pattern of the player
		 */
		public byte[] getPattern() {
			return pattern;
		}

		/**
		 * Get pre-processed search pattern.
		 * 
		 * @return pre-processed pattern
		 */
		public Object getPreProcessedPattern() {
			return preProcessedPattern;
		}

		/**
		 * Is this a splitted sub pattern to match?
		 * 
		 * @return true -for sub-patterns, else false
		 */
		public boolean isSubPattern() {
			return isSubPattern;
		}
	}

	/**
	 * Player section of the SID-ID configuration file.
	 * 
	 * @author Ken Händel
	 * 
	 */
	private static class PlayerSection {

		/**
		 * Player name.
		 */
		private final String playerName;
		/**
		 * Patterns to match.<BR>
		 * OR(AND(bytes, ...), ...)
		 */
		private final ArrayList<ArrayList<Pattern>> orList;

		/**
		 * Create a new section. This creates the pattern lists, also.
		 * 
		 * @param name
		 *            player name
		 */
		public PlayerSection(final String name) {
			this.playerName = name;
			this.orList = new ArrayList<ArrayList<Pattern>>();
			this.orList.add(new ArrayList<Pattern>());
		}

		/**
		 * Get the players name.
		 * 
		 * @return player name
		 */
		public final String getPlayerName() {
			return playerName;
		}

		/**
		 * Get pattern list: OR(AND(bytes, ...), ...).
		 * 
		 * @return pattern list
		 */
		public final ArrayList<ArrayList<Pattern>> getOrList() {
			return orList;
		}

	}

	/**
	 * Package of the internal configuration file.
	 */
	private static final String SID_ID_PKG = "libsidutils/";

	/**
	 * Name of the configuration file.
	 */
	private static final String FNAME = "sidid.cfg";

	/**
	 * Integer base for hexadecimals.
	 */
	private static final int HEX_RADIX = 16;

	/**
	 * Maximum pattern length.
	 */
	private static final int MAX_KEY_LENGTH = 32;

	/**
	 * Search algorithm to be used.
	 */
	private final BNDMWildcards search;

	/**
	 * Configuration sections.
	 */
	private ArrayList<PlayerSection> sections;

	/**
	 * Scan multiple times.
	 */
	private boolean multiScan;

	/**
	 * Scan file for multiple player IDs.
	 * 
	 * @param m
	 *            on/off multi scan
	 */
	public void setMultiScan(final boolean m) {
		this.multiScan = m;
	}

	/**
	 * Constructor. Enable use of BNDM algorithm.
	 */
	public SidIdV2() {
		search = new BNDMWildcards();
	}

	/**
	 * Search player ID of a tune file.
	 * 
	 * @param filename
	 *            file name
	 * @return list of players (depending of multiScan one or more entries)
	 * @throws IOException
	 *             read error
	 */
	public ArrayList<String> identify(final String filename) throws IOException {
		return identify(load(filename));
	}

	/**
	 * Search player ID of a program.
	 * 
	 * @param prg
	 *            program to identify
	 * @return list of players (depending of multiScan one or more entries)
	 */
	public ArrayList<String> identify(final byte[] prg) {
		final ArrayList<String> result = new ArrayList<String>();
		for (final PlayerSection section : sections) {
			if (matchOneOf(section.getOrList(), prg)) {
				result.add(section.getPlayerName());
				if (!multiScan) {
					return result;
				}
			}
		}
		return result;
	}

	/**
	 * Match one of the patterns in the list.
	 * 
	 * @param orList
	 *            the list of patterns to match
	 * @param prg
	 *            the byte array containing the text
	 * @return true - one of the patterns have matched, false otherwise
	 */
	private boolean matchOneOf(final ArrayList<ArrayList<Pattern>> orList,
			final byte[] prg) {
		for (final ArrayList<Pattern> andList : orList) {
			if (matchAllOf(andList, prg)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Match all patterns in the list.
	 * 
	 * @param andList
	 *            the list of patterns to match
	 * @param prg
	 *            the byte array containing the text
	 * @return true - all patterns have matched, false otherwise
	 */
	private boolean matchAllOf(final ArrayList<Pattern> andList,
			final byte[] prg) {
		int prgOffset = 0;
		for (final Pattern pattern : andList) {
			// Determine the range to search for
			int prgEnd = prg.length - prgOffset;
			if (pattern.isSubPattern()) {
				// A sub pattern is expected to be exactly behind the last match
				// This is forced here, by reducing the search area!
				prgEnd = prgOffset + pattern.getPattern().length;
				if (prgEnd > prg.length) {
					return false;
				}
			}
			// search for the pattern
			final int index = search.searchBytes(prg, prgOffset, prgEnd,
					pattern.getPattern(), pattern.getPreProcessedPattern());
			if (index == -1) {
				// not found
				return false;
			}
			// XXX Next search is done behind the last occurrence.
			// So, The patterns have to be sorted according to their expected
			// memory location. This is true at least for splitted sub-patterns
			// of the maximum pattern length, but maybe not for all SID-ID
			// entries.
			prgOffset = index + pattern.getPattern().length;
		}
		return true;
	}

	/**
	 * Load byte array containing the text to be searched in.
	 * 
	 * @param name
	 *            the filename to load
	 * @return the byte array with the file contents
	 * @throws IOException
	 *             read error
	 */
	private byte[] load(final String name) throws IOException {
		try (final DataInputStream in = new DataInputStream(
				new FileInputStream(name))) {
			final int length = (int) new File(name).length();
			final byte[] buffer = new byte[length];
			in.readFully(buffer);
			return buffer;
		}
	}

	/**
	 * Read configuration file and configure the SID-ID class.
	 */
	public void readconfig() {
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(readConfiguration())));
		sections = new ArrayList<PlayerSection>();
		PlayerSection section = null;
		final ArrayList<Byte> byteList = new ArrayList<Byte>();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				final StringTokenizer stok = new StringTokenizer(line, " ");
				while (stok.hasMoreTokens()) {
					final String origToken = stok.nextToken();
					final String token = origToken.toLowerCase();
					if (token.length() > 0) {
						if ("??".equals(token)) {
							// Add wild-card
							byteList.add((byte) '?');
						} else if ("end".equals(token)) {
							assert section != null;

							// logical AND of last read bytes
							andBytes(section, byteList);

							// Append a new orList entry for further contents
							final ArrayList<Pattern> orList = new ArrayList<Pattern>();
							section.getOrList().add(orList);
						} else if ("and".equals(token)) {
							assert section != null;
							// logical AND of last read bytes
							andBytes(section, byteList);
						} else if (token.length() == 2) {
							// Add hex value
							byteList.add(Integer.valueOf(token, HEX_RADIX)
									.byteValue());
						} else {
							// Create new player section
							if (section != null) {
								// last section is always empty. (refer to
								// "end")
								section.getOrList().remove(
										section.getOrList().size() - 1);
							}
							section = new PlayerSection(origToken);
							sections.add(section);
						}
					}
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		if (section != null) {
			// last section is always empty. (refer to
			// "end")
			section.getOrList().remove(section.getOrList().size() - 1);
		}
	}

	/**
	 * Add all bytes of the byte list to the last element of the orList.
	 * 
	 * @param section
	 *            the current section
	 * @param byteList
	 *            the bytes to add
	 */
	private void andBytes(final PlayerSection section,
			final ArrayList<Byte> byteList) {
		// get last orList entry
		final ArrayList<Pattern> lastOr = section.getOrList().get(
				section.getOrList().size() - 1);
		// If patterns exceed the maximum pattern length
		// create logical ANDed sup-patterns
		int byteListOffset = 0;
		int byteCount = byteList.size();
		boolean isSubPattern = false;
		while (byteCount > MAX_KEY_LENGTH) {
			// add up to the maximum pattern length
			andBytesMaxPtnLength(lastOr, byteList, byteListOffset,
					MAX_KEY_LENGTH, isSubPattern);
			byteCount -= MAX_KEY_LENGTH;
			byteListOffset += MAX_KEY_LENGTH;
			isSubPattern = true;
		}
		// add the remaining bytes to the last entry (AND)
		andBytesMaxPtnLength(lastOr, byteList, byteListOffset, byteCount,
				isSubPattern);

		// empty read bytes
		byteList.clear();
	}

	/**
	 * Add a pattern to the pattern search list.
	 * 
	 * @param ptnList
	 *            the pattern list to add a new pattern for
	 * @param byteList
	 *            the list of bytes to add
	 * @param byteListOffset
	 *            Current offset of the bytes to add
	 * @param byteCount
	 *            length of bytes to add (will become the search pattern length,
	 *            and must not exceed the maximum pattern length)
	 * @param isSubPattern
	 *            is this a splitted sub-pattern?
	 */
	private void andBytesMaxPtnLength(final ArrayList<Pattern> ptnList,
			final ArrayList<Byte> byteList, final int byteListOffset,
			final int byteCount, final boolean isSubPattern) {
		assert byteCount < MAX_KEY_LENGTH;
		final byte[] and = new byte[byteCount];
		for (int i = 0; i < and.length; i++) {
			and[i] = byteList.get(i + byteListOffset);
		}
		ptnList.add(new Pattern(and, search.processBytes(and, (byte) '?'),
				isSubPattern));
	}

	/**
	 * Load configuration file.
	 * 
	 * @return the configuration file entries
	 */
	private byte[] readConfiguration() {
		final File iniFilename = getLocation();
		if (iniFilename != null) {
			try {
				return load(iniFilename.getAbsolutePath());
			} catch (final IOException e) {
				System.err.println("Read error: " + iniFilename);
				return readInternal();
			}
		} else {
			return readInternal();
		}
	}

	/**
	 * Read from internal SID-ID configuration file.
	 * 
	 * @return the contents
	 */
	private byte[] readInternal() {
		try (final InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(SID_ID_PKG + FNAME)) {
			if (inputStream == null) {
				throw new RuntimeException("Internal SIDID not found: "
						+ SID_ID_PKG + FNAME);
			}
			final int length = inputStream.available();
			final byte[] data = new byte[length];
			int count, pos = 0;
			while (pos < length
					&& (count = inputStream.read(data, pos, length - pos)) >= 0) {
				pos += count;
			}
			if (pos != length) {
				System.err.println("Internal SIDID was not loaded completely!");
			}
			return data;
		} catch (final IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Internal SIDID not found: "
					+ SID_ID_PKG + FNAME);
		}
	}

	/**
	 * Search for the configuration file at various locations.
	 * <OL>
	 * <LI>current working dir
	 * <LI>user home dir
	 * </OL>
	 * 
	 * @return the configuration file or null (not found)
	 */
	private File getLocation() {
		try {
			final String[] paths = new String[] { "user.dir", "user.home" };
			for (final String path : paths) {
				File sidIdFile;
				sidIdFile = locate(path);
				if (sidIdFile.exists()) {
					return sidIdFile;
				}
			}
		} catch (AccessControlException e) {
			// access denied in the ui version
		}
		return null;
	}

	/**
	 * Locate configuration file at the given path.
	 * 
	 * @param location
	 *            the path to search in
	 * @return the file (caller should check exists)
	 */
	private File locate(final String location) {
		final String path = System.getProperty(location);
		if (path != null) {
			return new File(path, FNAME);
		} else {
			return new File("", FNAME);
		}
	}

}
