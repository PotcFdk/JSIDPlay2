package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.fingerprinting.WavBean;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	public static final String IDENTIFY_PATH = "/whatssid";

	@SuppressWarnings("unused")
	private ServletUtil util;

	public WhatsSidServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + IDENTIFY_PATH;
	}

	/**
	 * WhatsSID? (SID tune recognition).
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/whatssid
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		WavBean wavBean = getInput(request, WavBean.class);

		EntityManager entityManager = getEntityManager();
		final IniFingerprintConfig config = new IniFingerprintConfig();
		final FingerPrinting fingerPrinting = new FingerPrinting(config, new WhatsSidService(entityManager));
		MusicInfoWithConfidenceBean musicInfoWithConfidence = fingerPrinting.match(wavBean);
		closeEntityManager();

		setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
	}
}
