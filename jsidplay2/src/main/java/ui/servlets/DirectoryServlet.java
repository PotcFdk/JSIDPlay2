package ui.servlets;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import static ui.servlets.JSIDPlay2Server.ROOT_DIR;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DirectoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH = "/directory";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());
		String filter = request.getParameter("filter");
		List<String> files = getDirectory(filePath, filter);

		ObjectMapper mapper = new ObjectMapper();
		response.setContentType(MIME_TYPE_JSON);
		response.getWriter().println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(files));

		response.setStatus(HttpServletResponse.SC_OK);
	}

	private static class FileTypeComparator implements Comparator<File> {

		@Override
		public int compare(File file1, File file2) {

			if (file1.isDirectory() && file2.isFile())
				return -1;
			if (file1.isDirectory() && file2.isDirectory()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			if (file1.isFile() && file2.isFile()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			return 1;
		}
	}

	private static final Comparator<File> DIRECTORY_COMPARATOR = new FileTypeComparator();

	public List<String> getDirectory(String path, String filter) {
		File file = getAbsoluteFile(path);
		File[] listFiles = file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory() && pathname.getName().endsWith(".tmp"))
					return false;
				return pathname.isDirectory() || filter == null || pathname.getName().matches(filter);
			}
		});
		ArrayList<String> result = new ArrayList<String>();
		if (listFiles != null) {
			List<File> asList = Arrays.asList(listFiles);
			Collections.sort(asList, DIRECTORY_COMPARATOR);
			addPath(result, new File(file, "../"));
			for (File f : asList) {
				addPath(result, f);
			}
		}
		return result;
	}

	private File getAbsoluteFile(String path) {
		return new File(ROOT_DIR, path);
	}

	private void addPath(ArrayList<String> result, File f) {
		try {
			String canonicalPath = f.getCanonicalPath();
			if (canonicalPath.startsWith(ROOT_DIR)) {
				String path = canonicalPath.substring(ROOT_DIR.length());
				if (!path.isEmpty()) {
					result.add(path + (f.isDirectory() ? "/" : ""));
				} else {
					result.add("/");
				}
			}
		} catch (IOException e) {
			// ignore invalid path
		}
	}

}
