package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class TuneExistsServlet extends JSIDPlay2Servlet {

	public static final String TUNE_EXISTS_PATH = "/tune-exists";

	public TuneExistsServlet(ServletUtil servletUtil) {
		super(servletUtil);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + TUNE_EXISTS_PATH;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			Boolean exists = whatsSidService.tuneExists(musicInfoBean);

			setOutput(request, response, exists, Boolean.class);
		} finally {
			closeEntityManager();
		}
	}
}
