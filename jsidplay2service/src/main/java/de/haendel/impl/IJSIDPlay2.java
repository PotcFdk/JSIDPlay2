package de.haendel.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public interface IJSIDPlay2 {

	List<File> getDirectory(String root, String filter);

	public byte[] convert(Configuration config, String resource, String hvsc) throws InterruptedException, IOException,
			SidTuneError;
	public void convert2(Configuration config, String resource, String hvsc,
			OutputStream out) throws InterruptedException, IOException,
			SidTuneError;
}
