package applet.sidreg;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class SIDRegTable extends JTable {
	public SIDRegTable(final DefaultTableModel model) {
		super(model);
		// disable enter key behavior
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
		setRowSelectionAllowed(false);
		setCellSelectionEnabled(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		getColumnModel().getColumn(SIDRegModel.COL_CYCLES).setPreferredWidth(100);
		getColumnModel().getColumn(SIDRegModel.COL_DESCRIPTION).setPreferredWidth(300);
		getColumnModel().getColumn(SIDRegModel.COL_CHIP_NUM).setPreferredWidth(50);
	}
}
