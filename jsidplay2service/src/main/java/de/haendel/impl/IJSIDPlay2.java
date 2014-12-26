package de.haendel.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	List<File> getDirectory(String root, String filter);

	public void convert(Configuration config, String resource, OutputStream out)
			throws InterruptedException, IOException, SidTuneError;
}
