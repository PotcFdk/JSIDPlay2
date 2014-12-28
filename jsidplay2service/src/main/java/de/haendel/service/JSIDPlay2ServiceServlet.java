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

	private static final String MUSIC_FILTER = ".*\\.(mp3|sid)$";

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	// http://localhost:8080/jsidplay2service/JSIDPlay2SRV
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		String dirBE = req.getParameter("dir");
		String downloadBE = req.getParameter("download");
		String convertBE = req.getParameter("convert");
		if (req.getParameterMap().isEmpty() || dirBE != null) {
			String dir = dirBE != null ? new String(
					dirBE.getBytes("iso-8859-1"), "UTF-8") : "/";

			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			PrintWriter writer = response.getWriter();
			writer.println("<html><head><title>JSIDPlay2</title></head><body>");
			writer.println("<h1>Directory: " + dir + "</h1>");

			getDirectory(jsidplay2Service.getDirectory(dir, MUSIC_FILTER),
					writer);
			writer.println("</body></html>");
			writer.close();
		} else if (convertBE != null) {
			String convert = new String(convertBE.getBytes("iso-8859-1"),
					"UTF-8");

			response.setContentType("audio/mpeg");
			response.addHeader("Content-Disposition", "inline; filename="
					+ PathUtils.getBaseNameNoExt(new File(convert).getName())
					+ ".mp3");
			try (OutputStream stream = response.getOutputStream()) {
				Configuration cfg = new Configuration();
				cfg.getSidplay2().setDefaultPlayLength(0);
				cfg.getEmulation().setEmulation(Emulation.RESIDFP);
				jsidplay2Service.convert(cfg, convert, stream);
			} catch (SidTuneError | InterruptedException e) {
				log(e.getMessage(), e);
			}
		} else {
			// downloadBE != null
			String download = new String(downloadBE.getBytes("iso-8859-1"),
					"UTF-8");

			File file = jsidplay2Service.getFile(download);
			response.setContentLength((int) file.length());
			String name = file.getName();
			if (name.endsWith(".mp3") || name.endsWith(".sid")) {
				response.setContentType(name.endsWith(".mp3") ? "audio/mpeg"
						: "audio/prs.sid");
			}
			response.addHeader("Content-Disposition", "attachment; filename="
					+ name);
			getDownload(response.getOutputStream(), file);
		}
	}

	private void getDownload(ServletOutputStream out, File file)
			throws IOException {
		try (ServletOutputStream stream = out;
				BufferedInputStream buf = new BufferedInputStream(
						new FileInputStream(file))) {
			int readBytes = 0;
			while ((readBytes = buf.read()) != -1) {
				stream.write(readBytes);
			}
		}
	}

	private void getDirectory(List<String> directory, PrintWriter writer)
			throws IOException {
		for (Iterator<String> iterator = directory.iterator(); iterator
				.hasNext();) {
			String file = (String) iterator.next();
			String encode = URLEncoder.encode(file, "UTF-8");
			if (file.endsWith(".sid")) {
				writer.println("<A href='JSIDPlay2SRV?download=" + encode
						+ "'>" + file + "</A>");
				if (file.endsWith(".sid")) {
					writer.println("<A href='JSIDPlay2SRV?convert=" + encode
							+ "'>***as MP3***</A>");
				}
				writer.println("<BR>");
			} else if (file.endsWith(".mp3")) {
				writer.println("<A href='JSIDPlay2SRV?download=" + encode
						+ "'>" + file + "</A>");
				writer.println("<BR>");
			} else {
				writer.println("<A href='JSIDPlay2SRV?dir=" + encode + "'>"
						+ file + "</A><BR>");
			}
		}
	}

}
