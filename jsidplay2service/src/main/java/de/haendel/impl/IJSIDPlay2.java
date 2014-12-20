package de.haendel.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	List<File> getDirectory(String root, String filter);

	byte[] convert(Configuration cfg, String filename, String hvsc)
			throws InterruptedException, IOException, SidTuneError;
}
