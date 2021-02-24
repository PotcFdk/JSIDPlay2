package libsidutils.fingerprinting.rest;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;

/**
 * This data source is the interface to the fingerprinting database.
 * 
 * @author ken
 *
 */
public interface FingerPrintingDataSource {

	/**
	 * Insert a new tune into the database. First step of inserting a tune with
	 * hashes.
	 * 
	 * @param musicInfoBean tune information
	 * @return tune id
	 */
	IdBean insertTune(MusicInfoBean musicInfoBean);

	/**
	 * Insert the hashes of a new tune into the database. Last step of inserting a
	 * tune with hashes.
	 * 
	 * @param hashBeans generated hashes of the fingerprinted tune
	 */
	void insertHashes(HashBeans hashBeans);

	/**
	 * Check if a tune is already in the database. Call this prior to inserting a
	 * new tune.
	 * 
	 * @param musicInfoBean tune information
	 * @return tune exists
	 */
	boolean tuneExists(MusicInfoBean musicInfoBean);

	/**
	 * Find hashes of an already existing tune. First step of matching a tune.
	 * 
	 * @param intArray hashes of an already existing tune to match.
	 * @return matching hashes
	 */
	HashBeans findHashes(IntArrayBean intArray);

	/**
	 * Find a matched tune. Last step of matching a tune.
	 * 
	 * @param songNoBean matched song number
	 * @return music info of a matched tune
	 */
	MusicInfoBean findTune(SongNoBean songNoBean);

}
