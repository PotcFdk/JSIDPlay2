package libsidutils.fingerprinting.rest;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import sidplay.fingerprinting.MusicInfoBean;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.fingerprinting.WavBean;

public interface FingerPrintingDataSource {

	IdBean insertTune(MusicInfoBean musicInfoBean);

	void insertHashes(HashBeans hashBeans);

	HashBeans findHashes(IntArrayBean intArray);

	MusicInfoBean findTune(SongNoBean songNoBean);

	MusicInfoWithConfidenceBean whatsSid(WavBean wavBean) throws IOException;

	boolean tuneExists(MusicInfoBean musicInfoBean);

}
