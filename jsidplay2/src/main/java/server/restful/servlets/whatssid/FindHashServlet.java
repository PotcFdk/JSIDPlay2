package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class FindHashServlet extends JSIDPlay2Servlet {

	public static final String FIND_HASH_PATH = "/hash";

	public FindHashServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FIND_HASH_PATH;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			IntArrayBean intArrayBean = getInput(request, IntArrayBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager(request.getServletContext()));
			HashBeans result = whatsSidService.findHashes(intArrayBean);

			setOutput(request, response, result, HashBeans.class);
		} catch (Throwable t) {
			error(t);
		} finally {
			closeEntityManager(request.getServletContext());
		}
	}
}
