package applet.demos;

import java.awt.Component;
import java.io.File;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class DiskTreeRenderer extends DefaultTreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean _hasFocus) {
		File file = null;
		if (value instanceof File) {
			file = (File) value;
		}
		return super.getTreeCellRendererComponent(tree,
				file != null ? file.getName() : value, sel, expanded, leaf,
				row, _hasFocus);
	}
}
