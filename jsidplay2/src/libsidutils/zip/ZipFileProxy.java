package libsidutils.zip;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileProxy extends File {
	protected Map<String, HashMap<String, String>> hash;
	private ZipFile zipfile;

	public ZipFileProxy(File file) {
		super(file.getAbsolutePath());
		try {
			this.hash = new HashMap<String, HashMap<String, String>>();
			zipfile = new ZipFile(file, ZipFile.OPEN_READ);
			hash.put("", new HashMap<String, String>());
			@SuppressWarnings("rawtypes")
			Enumeration en = zipfile.entries();
			parse(en);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public File[] listFiles() {
		return listFiles((FileFilter) null);
	}

	@Override
	public File[] listFiles(FileFilter filter) {
		return getFileChildren("");
	}

	/* create a hashtable of the entries and their paths */
	@SuppressWarnings("rawtypes")
	private void parse(Enumeration en) {
		String fullName="";
		while (en.hasMoreElements()) {
			try {
				ZipEntry ze = (ZipEntry) en.nextElement();
				fullName = ze.getName();

				// Determine parent path for hashtable access
				String parent = "";
				File f = new File(fullName);
				if (f.getParent() != null) {
					parent = f.getParent().replace('\\', '/') + "/";
				}
				// Add empty children of directory ZIP entry to hashtable
				if (ze.isDirectory() && hash.get(fullName) == null) {
					HashMap<String, String> children = new HashMap<String, String>();
					hash.put(fullName, children);
				}
				// Add child to the parent children's list in the hashtable
				final Map<String, String> parentChildren = (Map<String, String>) hash
						.get(parent);
				if (parentChildren == null) {
					HashMap<String, String> children = new HashMap<String, String>();
					children.put(fullName, "");
					hash.put(parent, children);
				} else {
					parentChildren.put(fullName, "");
				}
			} catch (IllegalArgumentException e) {
				System.err.println(fullName);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get files of a parent path (separated by a slash).
	 * 
	 * Note: an empty path returns the root files.
	 * 
	 * @param parentPath
	 *            parent path (must end with a slash)
	 * @return file children of the specified parent directory path
	 */
	public final File[] getFileChildren(final String parentPath) {
		final Map<String, String> children = (Map<String, String>) hash
				.get(parentPath);
		if (children == null) {
			return new File[0];
		}
		final File[] files = new File[children.size()];
		final Iterator<String> it = children.keySet().iterator();
		int count = 0;
		while (it.hasNext()) {
			String name = (String) it.next();
			files[count] = new ZipEntryFileProxy(this, zipfile, name, this);
			count++;
		}
		return files;
	}
}
