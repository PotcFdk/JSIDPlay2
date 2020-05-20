package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;
import ui.entities.config.FilterSection;

@SuppressWarnings("serial")
public class FiltersServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public FiltersServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/filters";
	}

	/**
	 * Get SID filter definitions.
	 * 
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/filters
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<String> filters = getFilters();

		response.setContentType(MIME_TYPE_JSON.toString());
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(filters));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getFilters() {
		List<String> result = new ArrayList<String>();
		for (FilterSection iFilterSection : util.getConfiguration().getFilterSection()) {
			if (iFilterSection.isReSIDFilter6581()) {
				result.add("RESID_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDFilter8580()) {
				result.add("RESID_MOS8580_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter6581()) {
				result.add("RESIDFP_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter8580()) {
				result.add("RESIDFP_MOS8580_" + iFilterSection.getName());
			}
		}
		return result;
	}

}
