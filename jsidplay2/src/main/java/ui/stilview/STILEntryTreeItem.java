package ui.stilview;

import java.util.ArrayList;
import java.util.Collection;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;

public class STILEntryTreeItem extends TreeItem<Object> {

	private boolean hasLoadedChildren;

	public STILEntryTreeItem(STILEntry entry) {
		super(entry);
	}

	@Override
	public boolean isLeaf() {
		STILEntry stilEntry = (STILEntry) getValue();
		return stilEntry.subtunes.size() == 0;
	}

	@Override
	public ObservableList<TreeItem<Object>> getChildren() {
		if (hasLoadedChildren == false) {
			hasLoadedChildren = true;
			Collection<TuneEntryTreeItem> children = new ArrayList<TuneEntryTreeItem>();
			STILEntry stilEntry = (STILEntry) getValue();
			for (TuneEntry tuneEntry : stilEntry.subtunes) {
				children.add(new TuneEntryTreeItem(tuneEntry));
			}
			super.getChildren().setAll(children);
		}
		return super.getChildren();
	}
}
