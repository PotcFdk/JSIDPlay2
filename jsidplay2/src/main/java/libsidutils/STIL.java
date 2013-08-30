package libsidutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class STIL {
	public static final String STIL_FILE = "DOCUMENTS/STIL.txt";

	public static class Info {
		public String name;
		public String author;
		public String title;
		public String artist;
		public String comment;

		@Override
		public String toString() {
			return "info";
		}
	}

	public static class TuneEntry {
		public String globalComment;
		public int tuneNo = -1;
		public ArrayList<Info> infos = new ArrayList<Info>();

		@Override
		public String toString() {
			return "" + tuneNo;
		}
	}

	public static class STILEntry {
		public String comment;
		public String filename;
		public String globalComment;
		public ArrayList<TuneEntry> subtunes = new ArrayList<TuneEntry>();
		public ArrayList<Info> infos = new ArrayList<Info>();

		public STILEntry(String name) {
			filename = name;
		}

		@Override
		public String toString() {
			return "" + filename.substring(filename.lastIndexOf('/') + 1);
		}
	}

	private final HashMap<String, STILEntry> fastMap = new HashMap<String, STILEntry>();
	private File hvscRoot;

	public STIL(File hvscRoot, InputStream input) throws Exception {
		this.hvscRoot = hvscRoot;
		fastMap.clear();

		Pattern p = Pattern
				.compile("(NAME|AUTHOR|TITLE|ARTIST|COMMENT): *(.*)");

		STILEntry entry = null;
		TuneEntry tuneEntry = null;
		Info lastInfo = null;
		String lastProp = null;
		StringBuilder cmts = new StringBuilder();

		try (@SuppressWarnings("resource")
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				input))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					cmts.append(line.trim() + "\n");
					continue;
				}

				/* New entry? */
				if (line.startsWith("/")) {
					entry = new STILEntry(line);
					fastMap.put(line, entry);

					entry.comment = cmts.toString();
					cmts.delete(0, cmts.length());

					lastInfo = new Info();
					entry.infos.add(lastInfo);

					tuneEntry = null;
					lastProp = null;
					continue;
				}

				if (line.startsWith("(#")) {
					if (entry == null) {
						throw new RuntimeException(
								"Invalid format in STIL file: '(#' before '/'.");
					}

					// subtune
					int end = line.indexOf(")");
					int tuneNo = Integer.parseInt(line.substring(2, end));

					// subtune number
					tuneEntry = new TuneEntry();
					tuneEntry.tuneNo = tuneNo;
					entry.subtunes.add(tuneEntry);

					lastInfo = new Info();
					tuneEntry.infos.add(lastInfo);

					lastProp = null;
					continue;
				}

				line = line.trim();
				if ("".equals(line)) {
					continue;
				}

				if (entry == null) {
					throw new RuntimeException("No entry to put data in: "
							+ line);
				}

				if (lastInfo == null) {
					throw new RuntimeException("No context to put data in: "
							+ line);
				}

				Matcher m = p.matcher(line);
				if (m.matches()) {
					lastProp = m.group(1);

					/* If a field repeats, that starts a new tuneinfo structure. */
					Field f = lastInfo.getClass().getField(
							lastProp.toLowerCase());
					if (f.get(lastInfo) != null) {
						lastInfo = new Info();
						if (tuneEntry != null) {
							tuneEntry.infos.add(lastInfo);
						} else {
							entry.infos.add(lastInfo);
						}
					}
					f.set(lastInfo, m.group(2));
				} else if (lastProp != null) {
					/* Concat more shit after the previous line */
					Field f = lastInfo.getClass().getField(
							lastProp.toLowerCase());
					f.set(lastInfo, f.get(lastInfo) + "\n" + line);
				}
			}
		}
	}

	public STILEntry getSTILEntry(File file) {
		return fastMap.get(PathUtils.getCollectionName(hvscRoot, file));
	}

}
