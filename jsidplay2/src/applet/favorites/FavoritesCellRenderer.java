package applet.favorites;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import libsidplay.Player;
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;
import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.PathUtils;

@SuppressWarnings("serial")
public class FavoritesCellRenderer extends DefaultTableCellRenderer {

	private Icon fStilIcon;

	private Icon fStilNoIcon;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (column == 0) {
			if (value != null) {
				int row1 = table.getRowSorter().convertRowIndexToModel(row);
				File file = model.getFile(row1);
				if (!file.exists()) {
					setBackground(Color.RED);
					setToolTipText("File not found!");
				} else if (isCurrentlyPlayed(file)) {
					setBackground(Color.LIGHT_GRAY);
					setToolTipText(null);
				} else {
					setBackground(Color.WHITE);
					setToolTipText(null);
				}
				if (getSTIL(file) != null) {
					// set STIL icon
					if (fStilIcon == null) {
						fStilIcon = new ImageIcon(
								JSIDPlay2.class.getResource("icons/stil.png"));
					}
					setIcon(fStilIcon);
				} else {
					// set NO STIL icon
					if (fStilNoIcon == null) {
						fStilNoIcon = new ImageIcon(
								JSIDPlay2.class
										.getResource("icons/stil_no.png"));
					}
					setIcon(fStilNoIcon);
				}
			}
		} else {
			setIcon(null);
		}
		return super.getTableCellRendererComponent(table, value, isSelected,
				hasFocus, row, column);
	}

	private STILEntry getSTIL(final File file) {
		final String name = PathUtils.getHVSCName(config, file);
		if (null != name) {
			STIL stil = STIL.getInstance(config.getSidplay2().getHvsc());
			if (stil != null) {
				return stil.getSTIL(name);
			}
		}
		return null;
	}

	private Player player;
	private IConfig config;
	private FavoritesModel model;

	public FavoritesCellRenderer(FavoritesModel dm) {
		model = dm;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public void setConfig(IConfig config) {
		this.config = config;
	}

	/**
	 * Check if the current tree element is being played
	 * 
	 * @param file
	 * 
	 * @return is it played now?
	 */
	private boolean isCurrentlyPlayed(File file) {
		if (player.getTune() == null || file == null) {
			return false;
		}
		return player.getTune().getInfo().file.equals(file);
	}
}
