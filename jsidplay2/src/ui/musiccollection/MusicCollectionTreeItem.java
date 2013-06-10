package ui.musiccollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import ui.JSIDPlay2Main;
import ui.filefilter.TuneFileFilter;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import libsidutils.STIL;

public class MusicCollectionTreeItem extends TreeItem<File> {
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

	private static final Image stilIcon = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	private static final Image noStilIcon = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private final FileFilter fFileFilter = new TuneFileFilter();
	private File rootFile;
	private boolean hasLoadedChildren;
	private boolean isLeaf;
	private boolean hasSTIL;

	public MusicCollectionTreeItem(File file, File rootFile) {
		super(file);
		init(file, rootFile);
	}

	public MusicCollectionTreeItem(File file, File rootFile, Node icon) {
		super(file, icon);
		init(file, rootFile);
	}

	@Override
	public boolean isLeaf() {
		if (getValue().getName().toLowerCase().endsWith("zip")) {
			return false;
		}
		return isLeaf;
	}

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (hasLoadedChildren == false) {
			loadChildren();
		}
		return super.getChildren();
	}

	public boolean hasSTIL() {
		return hasSTIL;
	}

	private void init(File file, File rootFile) {
		this.rootFile = rootFile;
		this.isLeaf = file.isFile();
		this.hasSTIL = isLeaf && STIL.getSTIL(rootFile, file) != null;
	}

	private void loadChildren() {
		hasLoadedChildren = true;
		Collection<MusicCollectionTreeItem> children = new ArrayList<MusicCollectionTreeItem>();
		File[] listFiles = getValue().listFiles(fFileFilter);
		if (listFiles != null) {
			Arrays.sort(listFiles, FILE_COMPARATOR_IGNORE_CASE);
			for (File file : listFiles) {
				boolean childIsLeaf = file.isFile();
				boolean childHasSTIL = childIsLeaf
						&& STIL.getSTIL(rootFile, file) != null;
				children.add(new MusicCollectionTreeItem(file, rootFile,
						childHasSTIL ? new ImageView(stilIcon) : new ImageView(
								noStilIcon)));
			}
		}
		super.getChildren().setAll(children);
	}
}
