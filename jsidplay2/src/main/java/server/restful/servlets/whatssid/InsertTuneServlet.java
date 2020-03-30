package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.getEntityManager;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class InsertTuneServlet extends JSIDPlay2Servlet {

	public static final String INSERT_TUNE_PATH = "/insert-tune";

	@SuppressWarnings("unused")
	private ServletUtil util;

	public InsertTuneServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + INSERT_TUNE_PATH;
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

		EntityManager entityManager = getEntityManager();
		final WhatsSidService whatsSidService = new WhatsSidService(entityManager);
		IdBean idBean = whatsSidService.insertTune(musicInfoBean);
		entityManager.clear();

		setOutput(request, response, idBean, IdBean.class);
	}
}
