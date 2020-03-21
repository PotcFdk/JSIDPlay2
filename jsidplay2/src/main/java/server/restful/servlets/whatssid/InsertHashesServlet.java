package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.whatsSidService;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

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

		whatsSidService.insertHashes(hashes);
	}
}
