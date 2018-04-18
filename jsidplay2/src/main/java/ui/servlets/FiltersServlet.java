package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsidplay.config.IFilterSection;
import sidplay.ini.IniDefaults;

public class FiltersServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH = "/filters";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<String> filters = getFilters();

		response.setContentType(MIME_TYPE_JSON);
		response.getWriter().println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(filters));

		response.setStatus(HttpServletResponse.SC_OK);
	}

	public List<String> getFilters() {
		List<String> result = new ArrayList<String>();
		List<? extends IFilterSection> filterSection = IniDefaults.DEFAULTS.getFilterSection();
		for (Iterator<? extends IFilterSection> iterator = filterSection.iterator(); iterator.hasNext();) {
			final IFilterSection iFilterSection = (IFilterSection) iterator.next();
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
