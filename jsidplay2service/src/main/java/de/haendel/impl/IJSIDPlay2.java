package de.haendel.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import libsidplay.sidtune.SidTuneError;
import sidplay.audio.AudioDriver;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	/**
	 * Get directory contents
	 * 
	 * @param dir
	 *            directory relative to ROOT directory
	 * @param filter
	 *            regexp filter for relevant directory entries
	 * @return directory list (directories ends with a slash)
	 */
	List<String> getDirectory(String dir, String filter);

	/**
	 * get file contents
	 * 
	 * @param path
	 *            path relative to ROOT directory
	 * @return file contents
	 * @throws IOException
	 *             file read error
	 */
	byte[] getFile(String path) throws IOException;

	/**
	 * Convert SID to MP3
	 * 
	 * @param config
	 *            configuration with quality settings
	 * @param resource
	 *            SID resource relative to ROOT_DIR
	 * @param driver
	 * 			  audio driver
	 * @throws InterruptedException
	 *             player aborted
	 * @throws IOException
	 *             file read error
	 * @throws SidTuneError
	 *             invalid tune
	 */
	void convert(Configuration config, String resource, AudioDriver driver)
			throws InterruptedException, IOException, SidTuneError;

	/**
	 * Get composer photo
	 * 
	 * @param resource
	 *            SID resource relative to ROOT_DIR
	 * @return composer photo contents
	 * @throws IOException
	 *             file read error
	 * @throws SidTuneError
	 *             invalid tune
	 */
	byte[] getPhoto(String resource) throws IOException, SidTuneError;

	/**
	 * Get SID tune infos
	 * 
	 * @param resource
	 *            SID resource relative to ROOT_DIR
	 * @return map containing various SID tune infos
	 * @throws IOException
	 *             file read error
	 * @throws SidTuneError
	 *             invalid tune
	 */
	Map<String, String> getTuneInfos(String resource) throws IOException,
			SidTuneError;

	/**
	 * Get all filter names.
	 * 
	 * @return list of filter names
	 */
	List<String> getFilters();
}
