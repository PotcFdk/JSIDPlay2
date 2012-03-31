package libsidutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;

public class STIL {
	public static final String STIL_INFOS[] = new String[] {
		"STIL_GLB_COMMENT", "STIL_NAME", "STIL_AUTHOR", "STIL_TITLE",
		"STIL_ARTIST", "STIL_COMMENT" };

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

	private static STIL theStil;
	private static String theHVSCRoot;
	
	public static STIL getInstance(String hvscRoot) {
		if (theStil == null && hvscRoot != null && hvscRoot.length() != 0
				&& hvscRoot != theHVSCRoot) {
			File stilFile = getSTILFile(hvscRoot);
			if (stilFile != null && stilFile.exists()) {
				theStil = new STIL(stilFile);
				theHVSCRoot = hvscRoot;
			}
		}
		return theStil;
	}

	private STIL(final File file) {
		fastMap.clear();
		
		Pattern p = Pattern.compile("(NAME|AUTHOR|TITLE|ARTIST|COMMENT): *(.*)");

		BufferedReader r = null;
		try {
			final InputStream is;
			if (file instanceof ZipEntryFileProxy) {
				is = ((ZipEntryFileProxy) file).getInputStream();
			} else {
				is = new FileInputStream(file);
			}
			r = new BufferedReader(new InputStreamReader(is));

			STILEntry entry = null;
			TuneEntry tuneEntry = null;
			Info lastInfo = null;
			String lastProp = null;
			StringBuilder cmts = new StringBuilder();

			String line;
			while ((line = r.readLine()) != null) {
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
						throw new RuntimeException("Invalid format in STIL file: '(#' before '/'.");
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
					throw new RuntimeException("No entry to put data in: " + line);
				}
				
				if (lastInfo == null) {
					throw new RuntimeException("No context to put data in: " + line);
				}
				
				Matcher m = p.matcher(line);
				if (m.matches()) {
					lastProp = m.group(1);

					/* If a field repeats, that starts a new tuneinfo structure. */
					Field f = lastInfo.getClass().getField(lastProp.toLowerCase());
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
					Field f = lastInfo.getClass().getField(lastProp.toLowerCase());
					f.set(lastInfo, f.get(lastInfo) + "\n" + line);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static File getSTILFile(String hvscRoot) {
		if (hvscRoot.toLowerCase().endsWith(".zip")) {
			ZipFileProxy root = new ZipFileProxy(new File(hvscRoot));
			File[] docs = root.getFileChildren("C64Music/DOCUMENTS/");
			for (int i = 0; i < docs.length; i++) {
				if (docs[i].getName().equals("STIL.txt")) {
					return docs[i];
				}
			}
			return null;
		} else {
			return new File(hvscRoot + File.separator + "DOCUMENTS"
					+ File.separator + "STIL.txt");
		}
	}

	public STILEntry getSTIL(String name) {
		return fastMap.get(name);
	}
}
