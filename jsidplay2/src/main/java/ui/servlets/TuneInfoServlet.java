package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.metamodel.SingularAttribute;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsidplay.sidtune.SidTune;
import libsidutils.ZipFileUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.STILEntry;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.musiccollection.SearchCriteria;
import ui.musiccollection.TuneInfo;

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
			response.setContentType(MIME_TYPE_JSON);
			response.getWriter()
					.println(new ObjectMapper().writer().writeValueAsString(getTuneInfos(getHVSCEntry(tuneFile))));
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		}
	}

	private HVSCEntry getHVSCEntry(File tuneFile) throws Exception {
		if (tuneFile == null) {
			return null;
		}
		SidTune tune = SidTune.load(tuneFile);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		if (root == null) {
			return null;
		}
		SidDatabase db = new SidDatabase(root);
		STIL stil = getSTIL(root);
		HVSCEntry hvscEntry = new HVSCEntry(() -> db.getTuneLength(tune), "", tuneFile, tune);
		String path = db.getPath(tune);
		STILEntry stilEntry = stil != null && path.length() > 0 ? stil.getSTILEntry(path) : null;
		if (stilEntry != null && stilEntry.globalComment != null) {
			hvscEntry.setStilGlbComment(stilEntry.globalComment.replaceAll("([ \t\r])+", " "));
		}
		return hvscEntry;
	}

	private Map<String, String> getTuneInfos(HVSCEntry hvscEntry) throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		if (hvscEntry == null) {
			return result;
		}
		for (SearchCriteria<?, ?> field : SearchCriteria.getSearchableAttributes()) {
			SingularAttribute<?, ?> singleAttribute = field.getAttribute();
			if (!singleAttribute.getDeclaringType().getJavaType().equals(HVSCEntry.class)) {
				continue;
			}
			try {
				String name = singleAttribute.getDeclaringType().getJavaType().getSimpleName() + "."
						+ singleAttribute.getName();
				Object value = ((Method) singleAttribute.getJavaMember()).invoke(hvscEntry);
				TuneInfo tuneInfo = new TuneInfo();
				tuneInfo.setName(name);
				tuneInfo.setValue(String.valueOf(value != null ? value : ""));
				result.put(name, String.valueOf(value != null ? value : ""));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			}
		}
		return result;
	}

	private STIL getSTIL(String hvscRoot) throws Exception {
		try (InputStream input = ZipFileUtils.newFileInputStream(ZipFileUtils.newFile(hvscRoot, STIL.STIL_FILE))) {
			return new STIL(input);
		}
	}

}
