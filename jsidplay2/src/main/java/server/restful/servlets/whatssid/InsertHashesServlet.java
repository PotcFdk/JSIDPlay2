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
import libsidutils.fingerprinting.rest.beans.HashBeans;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class InsertHashesServlet extends JSIDPlay2Servlet {

	public static final String INSERT_HASHES_PATH = "/insert-hashes";

	@SuppressWarnings("unused")
	private ServletUtil util;

	public InsertHashesServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + INSERT_HASHES_PATH;
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HashBeans hashes = getInput(request, HashBeans.class);

		EntityManager entityManager = getEntityManager();
		final WhatsSidService whatsSidService = new WhatsSidService(entityManager);
		whatsSidService.insertHashes(hashes);
		closeEntityManager();
	}
}
