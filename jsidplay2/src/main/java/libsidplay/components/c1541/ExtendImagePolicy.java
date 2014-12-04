package libsidplay.components.c1541;

/**
 * Strategies to extend disk image to 40 tracks.
 * 
 * @author Ken Händel
 * 
 */
public enum ExtendImagePolicy {
	/**
	 * Never extend disk image.
	 */
	EXTEND_NEVER,
	/**
	 * Ask the user, if he wants to extend disk image.
	 */
	EXTEND_ASK,
	/**
	 * Always extend disk image automatically.
	 */
	EXTEND_ACCESS
}