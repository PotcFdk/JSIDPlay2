package applet.joysticksettings;

import javax.swing.table.DefaultTableModel;

import org.swixml.Localizer;

import net.java.games.input.Controller;

@SuppressWarnings("serial")
public class JoystickTestModel extends DefaultTableModel {

	private static final int COLUMN_COUNT = 2;
	private static final int COL_NAME = 0;
	private static final int COL_VALUE = 1;

	private Localizer localizer;
	private Controller controller;

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case COL_NAME:
			return localizer != null ? localizer.getString("COMPONENT") : "";
		case COL_VALUE:
			return localizer != null ? localizer.getString("VALUE") : "";
		default:
			return "N/A";
		}
	}

	@Override
	public int getRowCount() {
		if (controller != null)
			return controller.getComponents().length;
		return 0;
	}

	private long lastPollingTime;

	@Override
	public Object getValueAt(int row, int column) {
		/* throttle polling to max. once every 5 ms */
		long currentTime = System.currentTimeMillis();
		if (currentTime > lastPollingTime + 5) {
			controller.poll();
			lastPollingTime = currentTime;
		}

		switch (column) {
		case COL_NAME:
			return controller.getComponents()[row].getName();
		case COL_VALUE:
			return controller.getComponents()[row].getPollData();
		default:
			return "N/A";
		}
	}

	public void setInput(Controller controller) {
		this.controller = controller;
	}

	public void setLocalizer(Localizer l) {
		localizer = l;
		fireTableStructureChanged();
	}
}
