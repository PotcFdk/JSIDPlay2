package de.haendel.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	List<String> getDirectory(String dir, String filter);

	File getFile(String path);

	void convert(Configuration config, String resource, OutputStream out)
			throws InterruptedException, IOException, SidTuneError;

	byte[] loadPhoto(String resource) throws IOException, SidTuneError;
}
