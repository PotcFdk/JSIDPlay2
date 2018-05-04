package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.util.Pair;
import libsidplay.sidtune.SidTune;
import libsidutils.siddatabase.SidDatabase;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.musiccollection.SearchCriteria;

public class TuneInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_TUNE_INFO = "/info";

	private ServletUtil util;

	public TuneInfoServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Get SID tune infos.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/info/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = java.net.URLDecoder.decode(request.getRequestURI(), "UTF-8")
				.substring(request.getRequestURI().indexOf(SERVLET_PATH_TUNE_INFO) + SERVLET_PATH_TUNE_INFO.length());

		File tuneFile = util.getAbsoluteFile(filePath, request.getUserPrincipal());
		try {
			HVSCEntry hvscEntry = createHVSCEntry(tuneFile);
			Map<String, String> tuneInfos = SearchCriteria
					.getAttributeValues(hvscEntry,
							field -> field.getAttribute().getDeclaringType().getJavaType().getSimpleName() + "."
									+ field.getAttribute().getName())
					.stream().collect(Collectors.toMap(Pair<String, String>::getKey, pair -> pair.getValue()));
			response.setContentType(MIME_TYPE_JSON);
			response.getWriter().println(new ObjectMapper().writer().writeValueAsString(tuneInfos));
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			throw new ServletException(e.getMessage());
		}
	}

	private HVSCEntry createHVSCEntry(File tuneFile) throws Exception {
		if (tuneFile == null) {
			return null;
		}
		SidTune tune = SidTune.load(tuneFile);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		IntSupplier songLengthFnct = () -> 0;
		if (root != null) {
			SidDatabase db = new SidDatabase(root);
			songLengthFnct = () -> db.getTuneLength(tune);
		}
		return new HVSCEntry(songLengthFnct, "", tuneFile, tune);
	}

}
