package ui.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;

import com.fasterxml.jackson.databind.ObjectMapper;

import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;

public class FavoritesServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_FAVORITES = "/favorites";

	private ServletUtil util;

	public FavoritesServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Get contents of the first SID favorites tab.
	 * 
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/favorites
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<String> filters = getFavorites();

		response.setContentType(MimeTypes.Type.APPLICATION_JSON_UTF_8.asString());
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
