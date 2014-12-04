package libsidplay.components.c1541;

/**
 * Call-back how to deal with 40 tracks images.
 * 
 * @author Ken Händel
 * 
 */
public interface IExtendImageListener {

	/**
	 * Call-back to deal with 40 tracks images.
	 * 
	 * @return extend current image?
	 */
	boolean isAllowed();

}