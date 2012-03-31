package applet.siddump;

import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
public class SIDDumpTable extends JTable {

	public SIDDumpTable(TableModel model) {
		super(model);

		// disable enter key behavior
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setFont(new Font("Courier", getFont().getStyle(), 8));
		getTableHeader().setFont(
				new Font("Courier", getTableHeader().getFont().getStyle(),
						8));
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// set colored renderer
		setDefaultRenderer(Object.class, new SIDDumpTableCellRenderer());
	}

}
