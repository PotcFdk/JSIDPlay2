package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_JSON;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Properties;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.util.Pair;
import libsidplay.sidtune.SidTune;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.musiccollection.SearchCriteria;

@SuppressWarnings("serial")
public class TuneInfoServlet extends JSIDPlay2Servlet {

	public static final String SERVLET_PATH_TUNE_INFO = "/info";

	private ServletUtil util;

	public TuneInfoServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return "/info";
	}

	/**
	 * Get SID tune infos.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/info/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath
				.substring(decodedPath.indexOf(SERVLET_PATH_TUNE_INFO) + SERVLET_PATH_TUNE_INFO.length());

		try {
			response.setContentType(MIME_TYPE_JSON.getContentType());
			File tuneFile = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			HVSCEntry hvscEntry = createHVSCEntry(tuneFile);
			Map<String, String> tuneInfos = SearchCriteria
					.getAttributeValues(hvscEntry,
							field -> field.getAttribute().getDeclaringType().getJavaType().getSimpleName() + "."
									+ field.getAttribute().getName())
					.stream().collect(Collectors.toMap(Pair<String, String>::getKey, pair -> pair.getValue()));
			response.getWriter().println(new ObjectMapper().writer().writeValueAsString(tuneInfos));
		} catch (Exception e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private HVSCEntry createHVSCEntry(File tuneFile) throws Exception {
		if (tuneFile == null) {
			return null;
		}
		SidTune tune = SidTune.load(tuneFile);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		DoubleSupplier songLengthFnct = () -> 0;
		if (root != null) {
			SidDatabase db = new SidDatabase(root);
			songLengthFnct = () -> db.getTuneLength(tune);
		}
		return new HVSCEntry(songLengthFnct, "", tuneFile, tune);
	}

}
