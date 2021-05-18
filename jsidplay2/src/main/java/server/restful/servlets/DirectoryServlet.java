package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.schlichtherle.truezip.file.TFile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import server.restful.common.CollectionFileComparator;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DirectoryServlet extends JSIDPlay2Servlet {

	public DirectoryServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/directory";
	}

	/**
	 * Get directory contents containing music collections.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cjpg%7Cprg%7Cd64)$
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		String filePath = request.getPathInfo();
		String filter = request.getParameter("filter");

		List<String> files = getDirectory(filePath, filter, request.isUserInRole(ROLE_ADMIN));

		response.setContentType(MIME_TYPE_JSON.toString());
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(files));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getDirectory(String path, String filter, boolean adminRole) {
		if (path == null || path.equals("/")) {
			return getRoot(adminRole);
		} else if (path.startsWith(C64_MUSIC)) {
			File root = configuration.getSidplay2Section().getHvsc();
			return getCollectionFiles(root, path, filter, C64_MUSIC, adminRole);
		} else if (path.startsWith(CGSC)) {
			File root = configuration.getSidplay2Section().getCgsc();
			return getCollectionFiles(root, path, filter, CGSC, adminRole);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				File root = new TFile(directoryValue);
				return getCollectionFiles(root, path, filter, directoryLogicalName, adminRole);
			}
		}
		return getRoot(adminRole);
	}

	private List<String> getCollectionFiles(File rootFile, String path, String filter, String virtualCollectionRoot,
			boolean adminRole) {
		ArrayList<String> result = new ArrayList<>();
		if (rootFile != null) {
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			File file = ZipFileUtils.newFile(rootFile, path.substring(virtualCollectionRoot.length()));
			File[] listFiles = file.listFiles(pathname -> {
				if (pathname.isDirectory() && pathname.getName().endsWith(".tmp")) {
					return false;
				}
				return pathname.isDirectory() || filter == null
						|| pathname.getName().toLowerCase(Locale.US).matches(filter);
			});
			if (listFiles != null) {
				List<File> asList = Arrays.asList(listFiles);
				Collections.sort(asList, new CollectionFileComparator());
				addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, file) + "/../", null);
				for (File f : asList) {
					addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, f), f);
				}
			}
		}
		if (result.isEmpty()) {
			return getRoot(adminRole);
		}
		return result;
	}

	private void addPath(ArrayList<String> result, String path, File f) {
		result.add(path + (f != null && f.isDirectory() ? "/" : ""));
	}

	private List<String> getRoot(boolean adminRole) {
		List<String> result = new ArrayList<>(Arrays.asList(C64_MUSIC + "/", CGSC + "/"));

		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if (!needToBeAdmin || adminRole) {
				result.add(directoryLogicalName + "/");
			}
		}
		return result;
	}
}
