package libsidutils.zip;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import libsidutils.PathUtils;

public class ZipEntryFileProxy extends File {
	private static final long serialVersionUID = -2926789689053926603L;
	ZipFileProxy zip;
	ZipFile zipfile;
	String name, path;
	File parent;
	ZipEntry entry;

	public ZipEntryFileProxy(ZipEntryFileProxy zipEntry, String path,
			File parent) {
		this(zipEntry.zip, zipEntry.zipfile, path, parent);
	}

	public ZipEntryFileProxy(ZipFileProxy zip, ZipFile zipfile, String path,
			File parent) {
		super("");
		this.zip = zip;
		this.zipfile = zipfile;
		this.path = path;
		this.parent = parent;
		this.entry = zipfile.getEntry(path);

		// determine if the entry is a directory
		String tmp = path;

		if (entry.isDirectory()) {
			tmp = path.substring(0, path.length() - 1);
		}

		// then calculate the name
		int brk = tmp.lastIndexOf("/");
		name = path;
		if (brk != -1) {
			name = tmp.substring(brk + 1);
		}
	}

	@Override
	public long length() {
		return entry.getSize();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ 1234321;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public boolean isDirectory() {
		return entry.isDirectory();
	}

	@Override
	public boolean isFile() {
		return !entry.isDirectory();
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public String getAbsolutePath() {
		return path;
	}

	@Override
	public File getAbsoluteFile() {
		return this;
	}

	@Override
	public File getCanonicalFile() {
		return this;
	}

	@Override
	public File getParentFile() {
		return parent;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ZipEntryFileProxy) {
			ZipEntryFileProxy zo = (ZipEntryFileProxy) obj;
			if (zo.getAbsolutePath().equals(getAbsolutePath())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public File[] listFiles() {
		return listFiles((FileFilter) null);
	}

	@Override
	public File[] listFiles(FileFilter filter) {
		if (zip.hash.get(path) == null) {
			return new File[0];
		}
		Map<String, String> children = zip.hash.get(path);
		ArrayList<File> files = new ArrayList<File>();
		Iterator<String> it = children.keySet().iterator();
		while (it.hasNext()) {
			final String name = it.next();
			if (filter == null || filter.accept(new File(name) {
				@Override
				public boolean isDirectory() {
					return name.endsWith("/");
				}

				@Override
				public String getName() {
					return name;
				}
			})) {
				files.add(new ZipEntryFileProxy(zip, zipfile, name, this));
			}
		}
		return files.toArray(new File[files.size()]);
	}

	/**
	 * Extract a file from ZIP
	 * 
	 * @param zipEntry
	 *            ZIP entry to extract
	 * @param targetDir
	 * @return temp file with the contents of ZIP file entry
	 * @throws IOException
	 *             I/O error
	 */
	public static final File extractFromZip(final ZipEntryFileProxy zipEntry,
			String targetDir) throws IOException {
		File tmpFile = new File(targetDir, zipEntry.getName());
		tmpFile.deleteOnExit();
		try (InputStream is = zipEntry.getInputStream();
				OutputStream os = new FileOutputStream(tmpFile)) {
			byte[] b = new byte[1024];
			while (is.available() > 0) {
				int len = is.read(b);
				if (len > 0) {
					os.write(b, 0, len);
				}
			}
		}
		return tmpFile;
	}

	/**
	 * Extract a file from GZ
	 * 
	 * @param file
	 *            file to extract
	 * @param targetDir
	 * @return temp file with the contents of GZ file entry
	 * @throws IOException
	 *             I/O error
	 */
	public static final File extractFromGZ(final File file, String targetDir)
			throws IOException {
		File tmpFile = new File(targetDir, PathUtils.getBaseNameNoExt(file));
		tmpFile.deleteOnExit();
		try (InputStream is = new GZIPInputStream(new FileInputStream(file));
				OutputStream os = new FileOutputStream(tmpFile)) {
			byte[] b = new byte[1024];
			while (is.available() > 0) {
				int len = is.read(b);
				if (len > 0) {
					os.write(b, 0, len);
				}
			}
		}
		return tmpFile;
	}

	public InputStream getInputStream() throws IOException {
		return zipfile.getInputStream(entry);
	}

	public ZipFileProxy getZip() {
		return zip;
	}

	@Override
	public String toString() {
		return getName();
	}
}
