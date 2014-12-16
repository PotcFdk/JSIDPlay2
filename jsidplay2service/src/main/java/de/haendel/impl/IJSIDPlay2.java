package de.haendel.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	List<File> getDirectory(String root);

	byte[] convert(Configuration cfg, String filename)
			throws InterruptedException, IOException, SidTuneError;
}
