package de.haendel.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidplay.common.Emulation;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@WebServlet("/JSIDPlay2SRV")
public class JSIDPlay2ServiceServlet extends HttpServlet {

	private static final long serialVersionUID = -2861221133139848654L;

	private static final String ROOT = "/media/readyshare/Musik";

	private static final String ROOTSID = "/home/ken/Downloads/C64Music";

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	// http://localhost:8080/jsidplay2service/JSIDPlay2SRV
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		String dirBE = req.getParameter("dir");
		String downloadBE = req.getParameter("download");
		String convertBE = req.getParameter("convert");
		if (req.getParameterMap().isEmpty()) {
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			PrintWriter writer = response.getWriter();
			writer.println("<html><head><title>JSIDPlay2</title></head><body>");
			writer.println("<h1>Musik:</h1>");
			getDirectory(ROOT, jsidplay2Service.getDirectory(ROOT, null), writer);
			writer.println("<h1>SIDs:</h1>");
			getDirectory(ROOTSID, jsidplay2Service.getDirectory(ROOTSID, null),
					writer);
			writer.println("</body></html>");
			writer.close();
		} else if (dirBE != null) {
			String dir = new String(dirBE.getBytes("iso-8859-1"), "UTF-8");

			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			PrintWriter writer = response.getWriter();
			writer.println("<html><head><title>JSIDPlay2</title></head><body>");
			writer.println("<h1>Directory: " + dir + "</h1>");

			getDirectory(dir, jsidplay2Service.getDirectory(dir, null), writer);
			writer.println("</body></html>");
			writer.close();
		} else if (convertBE != null) {
			String convert = new String(convertBE.getBytes("iso-8859-1"),
					"UTF-8");
			
			try (OutputStream stream = response.getOutputStream()) {
				Configuration cfg = new Configuration();
				cfg.getEmulation().setEmulation(Emulation.RESIDFP);
				byte[] contents = jsidplay2Service.convert(cfg, convert, ROOTSID);
				response.setContentType("audio/mpeg");
				
				response.addHeader(
						"Content-Disposition",
						"attachment; filename="
								+ PathUtils.getBaseNameNoExt(new File(convert)
								.getName()) + ".mp3");
				response.setContentLength(contents.length);
				stream.write(contents, 0, contents.length);
			} catch (SidTuneError | InterruptedException e) {
				log(e.getMessage(), e);
			}
		} else {
			// downloadBE != null
			String download = new String(downloadBE.getBytes("iso-8859-1"),
					"UTF-8");

			getDownload(response, new File(download));
		}
	}

	private void getDownload(HttpServletResponse response, File mp3)
			throws IOException {
		if (mp3.getName().endsWith(".mp3") || mp3.getName().endsWith(".sid")) {
			response.setContentType(mp3.getName().endsWith(".mp3") ? "audio/mpeg"
					: "audio/prs.sid");
		}
		response.addHeader("Content-Disposition",
				"attachment; filename=" + mp3.getName());
		try (ServletOutputStream stream = response.getOutputStream();
				BufferedInputStream buf = new BufferedInputStream(
						new FileInputStream(mp3))) {
			response.setContentLength((int) mp3.length());
			int readBytes = 0;
			while ((readBytes = buf.read()) != -1) {
				stream.write(readBytes);
			}
		}
	}

	private void getDirectory(String root, List<File> directory,
			PrintWriter writer) throws IOException {
		writer.println("<A href='JSIDPlay2SRV?dir="
				+ new File(root, "/.").getCanonicalPath() + "'>.</A><BR>");
		writer.println("<A href='JSIDPlay2SRV?dir="
				+ new File(root, "/..").getCanonicalPath() + "'>..</A><BR>");
		for (Iterator<File> iterator = directory.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			String encode = URLEncoder.encode(file.getPath(), "UTF-8");
			if (file.isDirectory()) {
				writer.println("<A href='JSIDPlay2SRV?dir=" + encode + "'>"
						+ file.getName() + "</A><BR>");
			} else {
				writer.println("<A href='JSIDPlay2SRV?download=" + encode + "'>"
						+ file.getName() + "</A>");
				if (file.getName().endsWith(".sid")) {
					writer.println("<A href='JSIDPlay2SRV?convert=" + encode
							+ "'> Convert to MP3!</A>");
				}
				writer.println("<BR>");
			}
		}
	}

}
