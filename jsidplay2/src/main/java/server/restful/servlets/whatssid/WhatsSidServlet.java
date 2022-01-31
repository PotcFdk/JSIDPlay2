package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.LRUCache;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<WAVBean, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<WAVBean, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	public static final String WHATSSID_PATH = "/whatssid";

	public WhatsSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + WHATSSID_PATH;
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

			MusicInfoWithConfidenceBean musicInfoWithConfidence = MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.get(wavBean);
			if (musicInfoWithConfidence == null) {
				WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
				FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
				musicInfoWithConfidence = fingerPrinting.match(wavBean);
				MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(wavBean, musicInfoWithConfidence);
			}
			info(String.valueOf(musicInfoWithConfidence));
			setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
		} catch (Throwable t) {
			error(t);
		} finally {
			closeEntityManager();
		}
	}

}
