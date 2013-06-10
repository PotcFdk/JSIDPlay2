package ui.stil;

import javafx.scene.control.TreeItem;
import libsidutils.STIL.Info;

public class InfoTreeItem extends TreeItem<Object>{

	public InfoTreeItem(Info info) {
		super(info);
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	
}
