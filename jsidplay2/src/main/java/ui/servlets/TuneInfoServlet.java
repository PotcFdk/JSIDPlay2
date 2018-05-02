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
import libsidplay.sidtune.SidTuneError;
import libsidutils.ZipFileUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import libsidutils.stil.STIL.TuneEntry;
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
			response.getWriter().println(new ObjectMapper().writer().writeValueAsString(getTuneInfos(tuneFile)));
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (SidTuneError e) {
			throw new ServletException(e.getMessage());
		}
	}

	private Map<String, String> getTuneInfos(File tuneFile) throws IOException, SidTuneError {
		Map<String, String> result = new HashMap<String, String>();
		if (tuneFile == null) {
			return result;
		}
		SidTune tune = SidTune.load(tuneFile);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		if (root == null) {
			return result;
		}
		SidDatabase db = new SidDatabase(root);
		STIL stil = getSTIL(root);
		HVSCEntry hvscEntry = new HVSCEntry(() -> db.getTuneLength(tune), "", tuneFile, tune);
		String path = db.getPath(tune);
		STILEntry stilEntry = stil != null && path.length() > 0 ? stil.getSTILEntry(path) : null;
		if (stilEntry != null) {
			hvscEntry.setStilGlbComment(formatStilText(stilEntry).toString());
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

	private STIL getSTIL(String hvscRoot) {
		try (InputStream input = ZipFileUtils.newFileInputStream(ZipFileUtils.newFile(hvscRoot, STIL.STIL_FILE))) {
			return new STIL(input);
		} catch (NoSuchFieldException | IllegalAccessException | IOException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	private StringBuffer formatStilText(STILEntry stilEntry) {
		StringBuffer result = new StringBuffer();
		if (stilEntry != null) {
			// append STIL infos,replace multiple whitespaces
			String writeSTILEntry = writeSTILEntry(stilEntry);
			String replaceAll = writeSTILEntry.replaceAll("([ \t\r])+", " ");
			result.append(replaceAll);
		}
		return result;
	}

	private String writeSTILEntry(STILEntry stilEntry) {
		StringBuffer result = new StringBuffer();
		if (stilEntry.filename != null) {
			result.append("Filename: ");
			result.append(stilEntry.filename);
			result.append(" - ");
		}
		if (stilEntry.globalComment != null) {
			result.append("\n" + stilEntry.globalComment);
		}
		for (Info info : stilEntry.infos) {
			writeSTILEntry(result, info);
		}
		int subTuneNo = 1;
		for (TuneEntry entry : stilEntry.subtunes) {
			if (entry.globalComment != null) {
				result.append("\n" + entry.globalComment);
			}
			for (Info info : entry.infos) {
				result.append("\nSubTune #" + subTuneNo + ": ");
				writeSTILEntry(result, info);
			}
			subTuneNo++;
		}
		return result.append("                                        ").toString();
	}

	private void writeSTILEntry(StringBuffer buffer, Info info) {
		if (info.name != null) {
			buffer.append("\nName: ");
			buffer.append(info.name);
		}
		if (info.author != null) {
			buffer.append("\nAuthor: ");
			buffer.append(info.author);
		}
		if (info.title != null) {
			buffer.append("\nTitle: ");
			buffer.append(info.title);
		}
		if (info.artist != null) {
			buffer.append("\nArtist: ");
			buffer.append(info.artist);
		}
		if (info.comment != null) {
			buffer.append("\nComment: ");
			buffer.append(info.comment.replaceAll("\"", "'"));
		}
	}

}
