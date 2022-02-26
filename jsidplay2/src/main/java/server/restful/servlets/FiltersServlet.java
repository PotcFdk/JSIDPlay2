package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.config.FilterSection;

@SuppressWarnings("serial")
public class FiltersServlet extends JSIDPlay2Servlet {

	public static final String FILTERS_PATH = "/filters";

	public FiltersServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FILTERS_PATH;
	}

	/**
	 * Get SID filter definitions.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/filters
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			List<String> filters = getFilters();

			response.setContentType(MIME_TYPE_JSON.toString());
			response.getWriter().println(new ObjectMapper().writer().writeValueAsString(filters));
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintWriter(response.getWriter()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getFilters() {
		List<String> result = new ArrayList<>();
		for (FilterSection iFilterSection : configuration.getFilterSection()) {
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
