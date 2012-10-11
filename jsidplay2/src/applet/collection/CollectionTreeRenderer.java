package applet.collection;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import libsidplay.Player;
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;
import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.PathUtils;

@SuppressWarnings("serial")
final class CollectionTreeRenderer extends DefaultTreeCellRenderer {

	protected Player player;
	protected IConfig config;

	private final JPanel view;

	private File file;

	private ImageIcon fStilIcon;

	private ImageIcon fStilNoIcon;

	/**
	 * @param view
	 * @param sidplay
	 */
	public CollectionTreeRenderer(final JPanel view, Player pl, IConfig cfg) {
		this.view = view;
		this.config = cfg;
		this.player = pl;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean _hasFocus) {
		if (value instanceof File) {
			file = (File) value;
			if (leaf && file != null) {
				if (getSTIL(file) != null) {
					// set STIL icon
					if (fStilIcon == null) {
						fStilIcon = new ImageIcon(
								JSIDPlay2.class.getResource("icons/stil.png"));
					}
					setLeafIcon(fStilIcon);
				} else {
					// set NO STIL icon
					if (fStilNoIcon == null) {
						fStilNoIcon = new ImageIcon(
								JSIDPlay2.class
										.getResource("icons/stil_no.png"));
					}
					setLeafIcon(fStilNoIcon);
				}
			}
			return super.getTreeCellRendererComponent(tree,
					file != null ? file.getName() : value, sel, expanded, leaf,
					row, _hasFocus);
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded,
				leaf, row, _hasFocus);
	}

	private STILEntry getSTIL(final File file) {
		if (file == null) {
			return null;
		}
		final String name = PathUtils.getHVSCName(config, file);
		if (null != name) {
			STIL stil = STIL.getInstance(config.getSidplay2().getHvsc());
			if (stil != null) {
				return stil.getSTIL(name);
			}
		}
		return null;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (isCurrentlyPlayed(file)) {
			// highlight currently played tune
			g.setColor(Color.LIGHT_GRAY);
			int x = (getIcon() == null) ? 0 : getIcon().getIconWidth()
					+ getIconTextGap() - 1;
			g.fillRect(x, 0, this.view.getWidth(), this.view.getHeight());
		}
		super.paintComponent(g);
	}

	/**
	 * Check if the current tree element is being played
	 * 
	 * @param filename
	 *            file name
	 * 
	 * @return is it played now?
	 */
	private boolean isCurrentlyPlayed(File f) {
		if (player.getTune() == null || file == null) {
			return false;
		}
		return player.getTune().getInfo().file.equals(f);
	}
}