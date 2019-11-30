package server.restful.servlets;

import static server.restful.common.MimeType.MIME_TYPE_JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
public class FavoritesServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public FavoritesServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return "/favorites";
	}

	/**
	 * Get contents of the first SID favorites tab.
	 * 
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/favorites
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<String> filters = getFavorites();

		response.setContentType(MIME_TYPE_JSON.getContentType());
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(filters));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getFavorites() {
		List<String> result = new ArrayList<String>();
		for (FavoritesSection favoritesSection : util.getConfiguration().getFavorites()) {
			for (HVSCEntry favorite : favoritesSection.getFavorites()) {
				String filename = util.getFavoriteFilename(favorite);
				if (filename != null) {
					result.add(filename);
				}
			}
			break;
		}
		return result;
	}

}
