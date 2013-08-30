package ui.musiccollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ui.JSIDPlay2Main;
import ui.filefilter.TuneFileFilter;

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
	private boolean hasLoadedChildren;
	private boolean isLeaf;
	private boolean hasSTIL;

	private MusicCollection musicCollection;
	private libsidutils.STIL stil;

	public MusicCollectionTreeItem(MusicCollection musicCollection,
			libsidutils.STIL stil, File file) {
		super(file);
		this.musicCollection = musicCollection;
		this.stil = stil;
		this.isLeaf = file.isFile();
		this.hasSTIL = isLeaf && stil != null
				&& stil.getSTILEntry(file) != null;
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

	public boolean hasSTIL() {
		return hasSTIL;
	}

	private void loadChildren() {
		hasLoadedChildren = true;
		Collection<MusicCollectionTreeItem> children = new ArrayList<MusicCollectionTreeItem>();
		File[] listFiles = getValue().listFiles(fFileFilter);
		if (listFiles != null) {
			Arrays.sort(listFiles, FILE_COMPARATOR_IGNORE_CASE);
			for (File file : listFiles) {
				MusicCollectionTreeItem childItem = new MusicCollectionTreeItem(
						musicCollection, stil, file);
				children.add(childItem);
				if (childItem.hasSTIL()) {
					childItem.setGraphic(new ImageView(stilIcon));
				} else {
					childItem.setGraphic(new ImageView(noStilIcon));
				}
			}
		}
		super.getChildren().setAll(children);
	}

	public MusicCollection getMusicCollection() {
		return musicCollection;
	}
}
