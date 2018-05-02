package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import ui.entities.config.Configuration;
import ui.entities.config.FilterSection;

public class FiltersServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_FILTERS = "/filters";

	private ServletUtil util;

	public FiltersServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Get SID filter definitions.
	 * 
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/filters
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType(MIME_TYPE_JSON);
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(getFilters()));
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
