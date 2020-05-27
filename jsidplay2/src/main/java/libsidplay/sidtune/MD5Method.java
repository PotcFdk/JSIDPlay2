package libsidplay.sidtune;

/**
 * MD5 checksum calculation method - used for SLDB (SongLengthDataBase)
 *
 * @author ken
 *
 */
public enum MD5Method {
	/**
	 * Calculate MD5 of the SID tune according to the header information (used until
	 * HVSC#67)
	 */
	MD5_PSID_HEADER,
	/**
	 * Calculate MD5 of the whole tune contents (since HVSC#68)
	 */
	MD5_CONTENTS
}
