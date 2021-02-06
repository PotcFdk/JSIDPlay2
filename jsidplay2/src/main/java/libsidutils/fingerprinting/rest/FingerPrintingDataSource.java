package libsidutils.fingerprinting.rest;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import sidplay.fingerprinting.MusicInfoBean;

public interface FingerPrintingDataSource {

	IdBean insertTune(MusicInfoBean musicInfoBean);

	void insertHashes(HashBeans hashBeans);

	HashBeans findHashes(IntArrayBean intArray);

	MusicInfoBean findTune(SongNoBean songNoBean);

	boolean tuneExists(MusicInfoBean musicInfoBean);

}
