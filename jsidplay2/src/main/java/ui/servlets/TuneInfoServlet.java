package ui.servlets;

import static sidplay.ini.IniDefaults.DEFAULT_APP_SERVER_DIR;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import libsidutils.stil.STIL.TuneEntry;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.StilEntry;

public class TuneInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String HVSC_ROOT = DEFAULT_APP_SERVER_DIR + "/C64Music";

	public static final String SERVLET_PATH = "/info";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());
		Map<String, String> infos;
		try {
			infos = getTuneInfos(filePath);

			ObjectMapper mapper = new ObjectMapper();
			response.setContentType(MIME_TYPE_JSON);
			response.getWriter().println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(infos));

			response.setStatus(HttpServletResponse.SC_OK);

		} catch (SidTuneError e) {
			e.printStackTrace();
		}
	}

	public Map<String, String> getTuneInfos(String resource) throws IOException, SidTuneError {
		Map<String, String> tuneInfos = new HashMap<String, String>();
		File tuneFile = getAbsoluteFile(resource);
		SidTune tune = SidTune.load(tuneFile);
		SidDatabase db = new SidDatabase(HVSC_ROOT);
		STIL stil = getSTIL(HVSC_ROOT);
		if (tune != null) {
			IntSupplier lengthFnct = new IntSupplier() {
				@Override
				public int getAsInt() {
					return db.getTuneLength(tune);
				}
			};
			HVSCEntry hvscEntry = new HVSCEntry(lengthFnct, "", tuneFile, tune);

			String path = db.getPath(tune);
			STILEntry stilEntry = path.length() > 0 ? stil.getSTILEntry(path) : null;
			if (stilEntry != null) {
				hvscEntry.setStilGlbComment(stilEntry.globalComment);
				StringBuffer stilText = formatStilText(stilEntry);
				tuneInfos.put(StilEntry.class.getSimpleName() + ".text", stilText.toString());
			}
			addTuneInfo(tuneInfos, "HVSCEntry.path", hvscEntry.getPath());
			addTuneInfo(tuneInfos, "HVSCEntry.name", hvscEntry.getName());
			addTuneInfo(tuneInfos, "HVSCEntry.title", hvscEntry.getTitle());
			addTuneInfo(tuneInfos, "HVSCEntry.author", hvscEntry.getAuthor());
			addTuneInfo(tuneInfos, "HVSCEntry.released", hvscEntry.getReleased());
			addTuneInfo(tuneInfos, "HVSCEntry.format", hvscEntry.getFormat());
			addTuneInfo(tuneInfos, "HVSCEntry.playerId", hvscEntry.getPlayerId());
			addTuneInfo(tuneInfos, "HVSCEntry.noOfSongs", hvscEntry.getNoOfSongs());
			addTuneInfo(tuneInfos, "HVSCEntry.startSong", hvscEntry.getStartSong());
			addTuneInfo(tuneInfos, "HVSCEntry.clockFreq", hvscEntry.getClockFreq());
			addTuneInfo(tuneInfos, "HVSCEntry.speed", hvscEntry.getSpeed());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel1", hvscEntry.getSidModel1());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel2", hvscEntry.getSidModel2());
			addTuneInfo(tuneInfos, "HVSCEntry.sidModel3", hvscEntry.getSidModel3());
			addTuneInfo(tuneInfos, "HVSCEntry.compatibility", hvscEntry.getCompatibility());
			addTuneInfo(tuneInfos, "HVSCEntry.tuneLength", hvscEntry.getTuneLength());
			addTuneInfo(tuneInfos, "HVSCEntry.audio", hvscEntry.getAudio());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase1", hvscEntry.getSidChipBase1());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase2", hvscEntry.getSidChipBase2());
			addTuneInfo(tuneInfos, "HVSCEntry.sidChipBase3", hvscEntry.getSidChipBase3());
			addTuneInfo(tuneInfos, "HVSCEntry.driverAddress", hvscEntry.getDriverAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.loadAddress", hvscEntry.getLoadAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.loadLength", hvscEntry.getLoadLength());
			addTuneInfo(tuneInfos, "HVSCEntry.initAddress", hvscEntry.getInitAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.playerAddress", hvscEntry.getPlayerAddress());
			addTuneInfo(tuneInfos, "HVSCEntry.fileDate", hvscEntry.getFileDate());
			addTuneInfo(tuneInfos, "HVSCEntry.fileSizeKb", hvscEntry.getFileSizeKb());
			addTuneInfo(tuneInfos, "HVSCEntry.tuneSizeB", hvscEntry.getTuneSizeB());
			addTuneInfo(tuneInfos, "HVSCEntry.relocStartPage", hvscEntry.getRelocStartPage());
			addTuneInfo(tuneInfos, "HVSCEntry.relocNoPages", hvscEntry.getRelocNoPages());
			addTuneInfo(tuneInfos, "HVSCEntry.stilGlbComment", hvscEntry.getStilGlbComment());
		}
		return tuneInfos;
	}

	private File getAbsoluteFile(String path) {
		return new File(DEFAULT_APP_SERVER_DIR, path);
	}

	private void addTuneInfo(Map<String, String> tuneInfos, String name, Object value) {
		tuneInfos.put(name, String.valueOf(value != null ? value : ""));
	}

	private STIL getSTIL(String hvscRoot) {
		try (FileInputStream input = new FileInputStream(new File(HVSC_ROOT, STIL.STIL_FILE))) {
			return new STIL(input);
		} catch (Exception e) {
			// ignore
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
