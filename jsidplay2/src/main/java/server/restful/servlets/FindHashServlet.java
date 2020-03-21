package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.whatsSidService;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class FindHashServlet extends JSIDPlay2Servlet {

	@SuppressWarnings("unused")
	private ServletUtil util;

	public FindHashServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/hash";
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		IntArrayBean intArrayBean = getInput(request, IntArrayBean.class);

		HashBeans result = whatsSidService.findHash(intArrayBean);

		setOutput(request, response, result, HashBeans.class);
	}
}
