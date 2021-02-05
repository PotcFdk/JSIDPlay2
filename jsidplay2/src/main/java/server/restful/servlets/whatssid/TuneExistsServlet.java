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
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import sidplay.fingerprinting.MusicInfoBean;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class TuneExistsServlet extends JSIDPlay2Servlet {

	public static final String TUNE_EXISTS_PATH = "/tune-exists";

	@SuppressWarnings("unused")
	private ServletUtil util;

	public TuneExistsServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + TUNE_EXISTS_PATH;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

		EntityManager entityManager = getEntityManager();
		final WhatsSidService whatsSidService = new WhatsSidService(entityManager);
		Boolean exists = whatsSidService.tuneExists(musicInfoBean);
		closeEntityManager();

		setOutput(request, response, exists, Boolean.class);
	}
}
