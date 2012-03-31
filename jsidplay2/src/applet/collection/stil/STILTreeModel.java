package applet.collection.stil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;


public class STILTreeModel extends DefaultTreeModel {

	public STILTreeModel() {
		super(null);
	}
	
	public Object getChild(final Object parent, final int i) {
		Object obj = ((DefaultMutableTreeNode)parent).getUserObject();
		if (obj instanceof STILEntry) {
			final STILEntry stilEntry = (STILEntry) obj;
			return new DefaultMutableTreeNode(stilEntry.subtunes.get(i));
		}
		if (obj instanceof TuneEntry) {
			final TuneEntry tuneEntry = (TuneEntry) obj;
			return new DefaultMutableTreeNode(tuneEntry.infos.get(i));
		}
		return null;
	}

	public int getChildCount(final Object parent) {
		Object obj = ((DefaultMutableTreeNode)parent).getUserObject();
		if (obj == null) {
			return 0;
		}
		if (obj instanceof STILEntry) {
			final STILEntry stilEntry = (STILEntry) obj;
			return stilEntry.subtunes.size();
		}
		if (obj instanceof TuneEntry) {
			final TuneEntry tuneEntry = (TuneEntry) obj;
			return tuneEntry.infos.size();
		}
		if (obj instanceof Info) {
			final Info info = (Info) obj;
			int count = 0;
			if (info.name != null) {
				count ++;
			}
			if (info.author != null) {
				count ++;
			}
			if (info.title != null) {
				count ++;
			}
			if (info.artist != null) {
				count ++;
			}
			if (info.comment != null) {
				count ++;
			}
			return count;
		}
		return 0;
	}

	public int getIndexOfChild(final Object parent, final Object child) {
		Object obj = ((DefaultMutableTreeNode)parent).getUserObject();
		if (obj instanceof STILEntry) {
			final STILEntry stilEntry = (STILEntry) obj;
			for (int i = 0; i < stilEntry.subtunes.size(); i++) {
				if (stilEntry.subtunes.get(i) == child) {
					return i;
				}
			}
		}
		if (obj instanceof TuneEntry) {
			final TuneEntry tuneEntry = (TuneEntry) obj;
			for (int i = 0; i < tuneEntry.infos.size(); i++) {
				if (tuneEntry.infos.get(i) == child) {
					return i;
				}
			}
		}
		return -1;
	}

	public boolean isLeaf(final Object parent) {
		Object obj = ((DefaultMutableTreeNode)parent).getUserObject();
		return obj instanceof Info;
	}
}