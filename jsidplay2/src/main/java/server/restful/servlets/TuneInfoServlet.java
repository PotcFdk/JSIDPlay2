package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.util.Pair;
import libsidplay.sidtune.SidTune;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.musiccollection.SearchCriteria;

@SuppressWarnings("serial")
public class TuneInfoServlet extends JSIDPlay2Servlet {

	public static final String TUNE_INFO_PATH = "/info";

	public TuneInfoServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + TUNE_INFO_PATH;
	}

	/**
	 * Get SID tune infos.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/info/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			String filePath = request.getPathInfo();
			response.setContentType(MIME_TYPE_JSON.toString());
			File tuneFile = getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));

			TreeMap<String, String> tuneInfos = hvscEntry2SortedMap(createHVSCEntry(tuneFile));

			response.getWriter().println(new ObjectMapper().writer().writeValueAsString(tuneInfos));
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private HVSCEntry createHVSCEntry(File tuneFile) throws Exception {
		if (tuneFile == null) {
			return null;
		}
		SidTune tune = SidTune.load(tuneFile);
		File root = configuration.getSidplay2Section().getHvsc();
		DoubleSupplier songLengthFnct = () -> 0;
		if (root != null) {
			SidDatabase db = new SidDatabase(root);
			songLengthFnct = () -> db.getTuneLength(tune);
		}
		return new HVSCEntry(songLengthFnct, "", tuneFile, tune);
	}

	private TreeMap<String, String> hvscEntry2SortedMap(HVSCEntry hvscEntry) {
		List<Pair<String, String>> attributeValues = SearchCriteria.getAttributeValues(hvscEntry,
				field -> field.getAttribute().getDeclaringType().getJavaType().getSimpleName() + "."
						+ field.getAttribute().getName());
		// same order of keys as in the list
		return attributeValues.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue, (o1, o2) -> {
			throw new RuntimeException(String.format("Duplicate key for values %s and %s, I will not merge!", o1, o2));
		}, () -> new TreeMap<>((o1, o2) -> index(attributeValues, o1) - index(attributeValues, o2))));
	}

	private int index(List<Pair<String, String>> attributeValues, String o) {
		return IntStream.range(0, attributeValues.size())
				.filter(index -> Objects.equals(attributeValues.get(index).getKey(), o)).findFirst().getAsInt();
	}

}
