package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class FavoritesServlet extends JSIDPlay2Servlet {

	public FavoritesServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/favorites";
	}

	/**
	 * Get contents of the first SID favorites tab.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/favorites
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		List<String> filters = getFirstFavorites();

		response.setContentType(MIME_TYPE_JSON.toString());
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(filters));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getFirstFavorites() {
		List<String> filters = configuration.getFavorites().stream().findFirst()
				.map(favoritesSection -> favoritesSection.getFavorites()).orElseGet(Collections::emptyList).stream()
				.map(favorite -> getFavoriteFilename(favorite)).filter(Objects::nonNull).collect(Collectors.toList());
		return filters;
	}

	private String getFavoriteFilename(HVSCEntry entry) {
		if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvsc(), null).size() > 0) {
			return C64_MUSIC + entry.getPath();
		} else if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgsc(), null).size() > 0) {
			return CGSC + entry.getPath();
		}
		return null;
	}

}
