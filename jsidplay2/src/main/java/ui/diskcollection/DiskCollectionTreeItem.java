package ui.diskcollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class DiskCollectionTreeItem extends TreeItem<File> {

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
			Arrays.sort(
					listFiles,
					(a, b) -> {
						Integer aw = a.isFile() ? 1 : 0;
						Integer bw = b.isFile() ? 1 : 0;
						if (aw.equals(bw)) {
							return a.getName()
									.toLowerCase(Locale.ENGLISH)
									.compareTo(
											b.getName().toLowerCase(
													Locale.ENGLISH));
						}
						return aw.compareTo(bw);
					});
			for (File file : listFiles) {
				children.add(new DiskCollectionTreeItem(file, rootFile,
						fileFilter));
			}
		}
		super.getChildren().setAll(children);
	}
}
