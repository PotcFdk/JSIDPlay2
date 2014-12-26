package de.haendel.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebService;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@WebService(serviceName = "JSIDPlay2WS", portName = "JSIDPlay2", name = "JSIDPlay2", endpointInterface = "de.haendel.service.IJSIDPlay2WS", targetNamespace = "http://www.haendel.de/jbossas/jsidplay2service")
public class JSIDPlay2ServiceWS implements IJSIDPlay2WS {

	private static final String TMP_PREFIX = "jsidplay2";
	private static final String TMP_SUFFIX = ".jboss.mp3";

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	@Override
	@WebMethod
	public byte[] convert(Configuration config, String filename)
			throws InterruptedException, IOException, SidTuneError {
		File file = null;
		OutputStream out = null;
		try {
			file = File.createTempFile(TMP_PREFIX, TMP_SUFFIX);
			file.deleteOnExit();
			out = new FileOutputStream(file);
			jsidplay2Service.convert(config, filename, out);
			return Files.readAllBytes(Paths.get(file.getPath()));
		} finally {
			if (out != null) {
				out.close();
			}
			if (file != null) {
				file.delete();
			}
		}
	}

}
