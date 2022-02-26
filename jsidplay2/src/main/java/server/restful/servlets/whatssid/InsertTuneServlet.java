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
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class InsertTuneServlet extends JSIDPlay2Servlet {

	public static final String INSERT_TUNE_PATH = "/insert-tune";

	public InsertTuneServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + INSERT_TUNE_PATH;
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPut(request);
		try {
			MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			IdBean idBean = whatsSidService.insertTune(musicInfoBean);

			setOutput(request, response, idBean, IdBean.class);
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
