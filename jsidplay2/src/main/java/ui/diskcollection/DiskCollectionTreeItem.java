package ui.diskcollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class DiskCollectionTreeItem extends TreeItem<File> {

	private static final Comparator<File> FILE_COMPARATOR_IGNORE_CASE = new Comparator<File>() {
		@Override
		public int compare(File a, File b) {
			Integer aw = a.isFile() ? 1 : 0;
			Integer bw = b.isFile() ? 1 : 0;
			if (aw.equals(bw)) {
				return a.getName().toLowerCase()
						.compareTo(b.getName().toLowerCase());
			}
			return aw.compareTo(bw);
		}
	};

	private File rootFile;
	private FileFilter fileFilter;
	private boolean hasLoadedChildren;
	private boolean isLeaf;

	public DiskCollectionTreeItem(File file, File rootFile,
			FileFilter fileFilter) {
		super(file);
		this.rootFile = rootFile;
		this.isLeaf = file.isFile();
		this.fileFilter = fileFilter;
	}

	@Override
	public boolean isLeaf() {
		return isLeaf;
	}

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (hasLoadedChildren == false) {
			loadChildren();
		}
		return super.getChildren();
	}

	private void loadChildren() {
		hasLoadedChildren = true;
		Collection<DiskCollectionTreeItem> children = new ArrayList<DiskCollectionTreeItem>();
		File[] listFiles = getValue().listFiles(fileFilter);
		if (listFiles != null) {
			Arrays.sort(listFiles, FILE_COMPARATOR_IGNORE_CASE);
			for (File file : listFiles) {
				children.add(new DiskCollectionTreeItem(file, rootFile,
						fileFilter));
			}
		}
		super.getChildren().setAll(children);
	}
}
