package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class FindTuneServlet extends JSIDPlay2Servlet {

	public static final String FIND_TUNE_PATH = "/tune";

	public FindTuneServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FIND_TUNE_PATH;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			SongNoBean songNoBean = getInput(request, SongNoBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			MusicInfoBean musicInfoBean = whatsSidService.findTune(songNoBean);

			setOutput(request, response, musicInfoBean, MusicInfoBean.class);
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintWriter(response.getWriter()));
		} finally {
			closeEntityManager();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
