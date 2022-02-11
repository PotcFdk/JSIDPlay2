package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.MAX_WHATSIDS_IN_PARALLEL;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.LRUCache;
import server.restful.filters.LimitRequestFilter;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<Integer, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<Integer, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	public static final String WHATSSID_PATH = "/whatssid";

	public WhatsSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + WHATSSID_PATH;
	}

	@Override
	public Filter createServletFilter() {
		return new LimitRequestFilter(MAX_WHATSIDS_IN_PARALLEL);
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
			int hashCode = wavBean.hashCode();

			MusicInfoWithConfidenceBean musicInfoWithConfidence = null;
			if (!MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.containsKey(hashCode)) {
				WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
				FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
				musicInfoWithConfidence = fingerPrinting.match(wavBean);
				MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(hashCode, musicInfoWithConfidence);
				info(String.valueOf(musicInfoWithConfidence));
			} else {
				musicInfoWithConfidence = MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.get(hashCode);
				info(String.valueOf(musicInfoWithConfidence) + " (cached)");
			}
			setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
		} catch (Throwable t) {
			error(t);
		} finally {
			closeEntityManager();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
