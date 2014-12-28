package de.haendel.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import libsidplay.common.Emulation;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import ui.entities.config.Configuration;
import de.haendel.impl.IJSIDPlay2;

@WebServlet("/JSIDPlay2SRV/*")
public class JSIDPlay2ServiceServlet extends HttpServlet {

	private static final long serialVersionUID = -2861221133139848654L;

	private static final String MUSIC_FILTER = ".*\\.(mp3|sid)$";

	@Inject
	private IJSIDPlay2 jsidplay2Service;

	// http://localhost:8080/jsidplay2service/JSIDPlay2SRV
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = req.getSession(true);
		Configuration config = getConfiguration(session);

		String pathInfo = req.getRequestURI();
		String[] pathParts = pathInfo.split("/");
		String mode = null;
		StringBuilder path= new StringBuilder();
		if (pathParts.length>3) {
			mode = pathParts[3];
			if (pathParts.length>4) {
				for (int i = 4; i < pathParts.length; i++) {
					path.append(pathParts[i]).append("/");
				}
			}
		}
		
		if (mode == null || "dir".equals(mode)) {
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");

			PrintWriter writer = response.getWriter();
			writer.println("<html><head><title>JSIDPlay2</title></head><body>");
			writer.println("<h1>Directory: " + path + "</h1>");

			getDirectory(jsidplay2Service.getDirectory(path.toString(), MUSIC_FILTER),
					writer);
			writer.println("</body></html>");
			writer.close();
		} else if ("convert".equals(mode)) {
			response.setContentType("audio/mpeg");
			response.addHeader("Content-Disposition", "inline; filename="
					+ PathUtils.getBaseNameNoExt(new File(path.toString()).getName())
					+ ".mp3");
			try (OutputStream stream = response.getOutputStream()) {
				config.getSidplay2().setDefaultPlayLength(0);
				config.getEmulation().setEmulation(Emulation.RESIDFP);
				jsidplay2Service.convert(config, path.toString(), stream);
			} catch (SidTuneError | InterruptedException e) {
				log(e.getMessage(), e);
			}
		} else {
			File file = jsidplay2Service.getFile(path.toString());
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

	private Configuration getConfiguration(HttpSession session) {
		if (session.isNew()) {
			session.setAttribute("Configuration", new Configuration());
		}
		return (Configuration) session.getAttribute("Configuration");
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
			if (file.endsWith(".sid")) {
				writer.println("<A href='/jsidplay2service/JSIDPlay2SRV/download" + file
						+ "'>" + file + "</A>");
				if (file.endsWith(".sid")) {
					writer.println("<A href='/jsidplay2service/JSIDPlay2SRV/convert" + file
							+ "'>***as MP3***</A>");
				}
				writer.println("<BR>");
			} else if (file.endsWith(".mp3")) {
				writer.println("<A href='/jsidplay2service/JSIDPlay2SRV/download" + file
						+ "'>" + file + "</A>");
				writer.println("<BR>");
			} else {
				writer.println("<A href='/jsidplay2service/JSIDPlay2SRV/dir" + file + "'>"
						+ file + "</A><BR>");
			}
		}
	}

}
