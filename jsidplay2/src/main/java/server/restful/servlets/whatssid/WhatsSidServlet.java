package server.restful.servlets.whatssid;

import static java.lang.Thread.getAllStackTraces;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.servlets.ConvertServlet.RTMP_THREAD;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	public static final String IDENTIFY_PATH = "/whatssid";

	public WhatsSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
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
		super.doPost(request);
		try {
			WAVBean wavBean = getInput(request, WAVBean.class);

			MusicInfoWithConfidenceBean musicInfoWithConfidence = null;
			if (!getAllStackTraces().keySet().stream().map(Thread::getName).filter(RTMP_THREAD::equals).findFirst()
					.isPresent()) {
				final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
				musicInfoWithConfidence = new FingerPrinting(new IniFingerprintConfig(), whatsSidService)
						.match(wavBean);
				info(String.valueOf(musicInfoWithConfidence));
			}
			setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
		} catch (Throwable t) {
			error(t);
		} finally {
			closeEntityManager();
		}
	}

}
