package ui.servlets;
import static sidplay.ini.IniDefaults.DEFAULT_APP_SERVER_DIR;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_MPEG;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_SID;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_BIN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH = "/download";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());

		byte[] contents = getFile(filePath);
		response.setContentType(
				filePath.endsWith(".mp3") ? MIME_TYPE_MPEG : filePath.endsWith(".sid") ? MIME_TYPE_SID : MIME_TYPE_BIN);
		response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());

		try (ServletOutputStream servletOutputStream = response.getOutputStream()) {
			servletOutputStream.write(contents);
		} finally {
			response.getOutputStream().flush();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	public byte[] getFile(String path) throws IOException {
		return Files.readAllBytes(Paths.get(getAbsoluteFile(path).getPath()));
	}

	private File getAbsoluteFile(String path) {
		return new File(DEFAULT_APP_SERVER_DIR, path);
	}
}
