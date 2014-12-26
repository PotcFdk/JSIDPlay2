package de.haendel.service;

import java.io.IOException;

import javax.jws.WebService;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

@WebService
public interface IJSIDPlay2WS {

	byte[] convert(Configuration cfg, String filename)
			throws InterruptedException, IOException, SidTuneError;

}
