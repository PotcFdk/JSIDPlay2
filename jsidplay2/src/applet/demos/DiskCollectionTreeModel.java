/**
 * 
 */
package applet.demos;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import libsidutils.zip.ZipFileProxy;
import applet.filefilter.DiskFileFilter;
import applet.filefilter.DocsFileFilter;
import applet.filefilter.TapeFileFilter;

public final class DiskCollectionTreeModel implements TreeModel {
	/**
	 * file filter for tunes
	 */
	protected final FileFilter diskFileFilter = new DiskFileFilter();
	protected final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	protected final DocsFileFilter docsFileFilter = new DocsFileFilter();

	/**
	 * Save child files of a parent file.
	 */
	private HashMap<File, File[]> childs = new HashMap<File, File[]>();

	/**
	 * root element
	 */
	private final File fRoot;

	public DiskCollectionTreeModel() {
		this(new File(""));
	}

	/**
	 * @param view
	 */
	public DiskCollectionTreeModel(File rootFile) {
		if (rootFile.getName().toLowerCase().endsWith(".zip")) {
			fRoot = new ZipFileProxy(rootFile);
		} else {
			fRoot = rootFile;
		}
	}

	public void valueForPathChanged(TreePath treepath, Object obj) {

	}

	public void removeTreeModelListener(TreeModelListener treemodellistener) {

	}

	public boolean isLeaf(Object obj) {
		if (!(obj instanceof File)) {
			return false;
		}
		if (obj instanceof ZipFileProxy) {
			return false;
		}
		return ((File) obj).isFile();
	}

	public Object getRoot() {
		return fRoot;
	}

	public int getIndexOfChild(Object parent, Object child) {
		File[] listFiles = getListOfFiles((File) parent);
		for (int i = 0; i < listFiles.length; i++) {
			if (listFiles[i].equals(child)) {
				return i;
			}
		}
		return -1;
	}

	public int getChildCount(Object obj) {
		File[] listFiles = getListOfFiles((File) obj);
		if (listFiles == null) {
			return 0;
		}
		return listFiles.length;
	}

	public Object getChild(Object obj, int i) {
		return getListOfFiles((File) obj)[i];
	}

	/**
	 * Get child files of a parent file. For performance reasons these children
	 * are hashed.
	 * 
	 * @param obj
	 *            parent file
	 * @return child files
	 */
	private File[] getListOfFiles(File obj) {
		if (childs.get(obj) != null) {
			return childs.get(obj);
		}
		File[] listFiles = ((File) obj).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				// XXX filter HVMEC HTML stuff (should not be HVMEC specific!)
				if (file.isDirectory()
						&& file.getName().equals("HVMEC/CONTROL/")) {
					return false;
				}
				return diskFileFilter.accept(file)
						|| tapeFileFilter.accept(file)
						|| docsFileFilter.accept(file);
			}
		});
		if (listFiles != null) {
			Arrays.sort(listFiles, new Comparator<File>() {
				public int compare(File a, File b) {
					Integer aw = a.isFile() ? 1 : 0;
					Integer bw = b.isFile() ? 1 : 0;
					if (aw.equals(bw)) {
						return a.getName().toLowerCase()
								.compareTo(b.getName().toLowerCase());
					}
					return aw.compareTo(bw);
				}
			});
		}
		childs.put(obj, listFiles);
		return listFiles;
	}

	public void addTreeModelListener(TreeModelListener treemodellistener) {

	}

	/**
	 * Path name to tree path converter.
	 * 
	 * @param filePath
	 *            path to convert
	 * @return tree path to the file beginning with root
	 */
	public ArrayList<File> getFile(String filePath) {
		final StringTokenizer selectFile = new StringTokenizer(filePath, "/");
		final ArrayList<File> pathSegs = new ArrayList<File>();
		File lastFile = (File) getRoot();
		pathSegs.add(lastFile);
		File curFile = lastFile;
		if (lastFile instanceof ZipFileProxy) {
			curFile = (File) getChild(lastFile, 0);
			pathSegs.add(curFile);
		}
		while (selectFile.hasMoreTokens()) {
			final String nextSegment = selectFile.nextToken();
			boolean found = false;
			for (int i = 0; i < getChildCount(curFile); i++) {
				File child = (File) getChild(curFile, i);
				if (child.getName().startsWith(nextSegment)) {
					curFile = child;
					found = true;
					break;
				}
			}
			if (!found) {
				return new ArrayList<File>();
			}
			pathSegs.add(curFile);
			lastFile = curFile;
		}
		return pathSegs;
	}
}