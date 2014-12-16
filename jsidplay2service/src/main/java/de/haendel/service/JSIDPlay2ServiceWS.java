package de.haendel.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebService;

import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@WebService(serviceName = "JSIDPlay2", portName = "JSIDPlay2", name = "JSIDPlay2", endpointInterface = "de.haendel.service.IJSIDPlay2WS", targetNamespace = "http://www.haendel.de/jbossas/jsidplay2service")
public class JSIDPlay2ServiceWS implements IJSIDPlay2WS {

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	@Override
	@WebMethod
	public byte[] convert(Configuration cfg, String filename)
			throws InterruptedException, IOException, SidTuneError {
		return jsidplay2Service.convert(cfg, filename);
	}

}
