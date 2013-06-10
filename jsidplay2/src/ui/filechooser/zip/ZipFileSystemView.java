package ui.filechooser.zip;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;

public class ZipFileSystemView extends FileSystemView {

	@Override
	public File createNewFolder(File file) {
		return null;
	}

	@Override
	public File createFileObject(File dir, String filename) {
		if (dir instanceof ZipEntryFileProxy) {
			ZipEntryFileProxy zdir = (ZipEntryFileProxy) dir;
			return new ZipEntryFileProxy(zdir, filename, dir);
		} else if (dir.getName().toLowerCase().endsWith(".zip")) {
			File[] fs = getFiles(dir, true);
			for (int i = 0; i < fs.length; i++) {
				if (fs[i].getName().equals(filename)) {
					return fs[i];
				}
			}
		}
		return super.createFileObject(dir, filename);
	}

	@Override
	public File getChild(File dir, String filename) {
		if (dir instanceof ZipEntryFileProxy) {
			ZipEntryFileProxy zdir = (ZipEntryFileProxy) dir;
			return new ZipEntryFileProxy(zdir, dir.getPath() + filename, dir);
		}
		return super.getChild(dir, filename);
	}

	@Override
	public String getSystemDisplayName(File f) {
		if (f instanceof ZipEntryFileProxy) {
			return f.getName();
		}
		return super.getSystemDisplayName(f);
	}

	@Override
	public File getParentDirectory(File dir) {
		if (dir instanceof ZipEntryFileProxy) {
			return dir.getParentFile();
		}
		return super.getParentDirectory(dir);
	}

	@Override
	public File[] getFiles(File dir, boolean useFileHiding) {
		if (dir.getName().toLowerCase().endsWith(".zip")) {
			return new ZipFileProxy(dir).listFiles();
		}

		if (dir instanceof ZipEntryFileProxy) {
			return dir.listFiles();
		}

		return super.getFiles(dir, useFileHiding);
	}

	@Override
	public Boolean isTraversable(File f) {
		if (f instanceof ZipEntryFileProxy) {
			boolean b = ((ZipEntryFileProxy) f).isDirectory();
			return new Boolean(b);
		} else if (f.getName().toLowerCase().endsWith(".zip")) {
			// nested ZIPS are not allowed!
			return new Boolean(true);
		}

		return super.isTraversable(f);
	}
}
