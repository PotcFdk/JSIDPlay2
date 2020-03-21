package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.whatsSidService;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WavBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class IdentifyServlet extends JSIDPlay2Servlet {

	public static final String IDENTIFY_PATH = "/identify";

	@SuppressWarnings("unused")
	private ServletUtil util;

	public IdentifyServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + IDENTIFY_PATH;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		WavBean wavBean = getInput(request, WavBean.class);

		MusicInfoWithConfidenceBean musicInfoWithConfidence = whatsSidService.identify(wavBean);

		setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
	}
}