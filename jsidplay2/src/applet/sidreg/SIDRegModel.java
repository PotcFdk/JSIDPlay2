package applet.sidreg;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import org.swixml.Localizer;

import libsidplay.common.IReSIDExtension;

@SuppressWarnings("serial")
public class SIDRegModel extends DefaultTableModel implements IReSIDExtension {
	public static final int ABS_CYCLES = 0;
	public static final int COL_CYCLES = 1;
	public static final int COL_DESCRIPTION = 2;
	public static final int COL_CHIP_NUM = 3;
	public static final int COL_VALUE = 4;

	private static final int MAX_ROW_COUNT = 10000;
	private static final int UPDATE_ON_ROW = 2000;

	private Localizer localizer;

	/**
	 * Recorded row number
	 */
	private int fFetchedRow;

	private final String description[] = new String[] {
			"VOICE_1_FREQ_L",
			"VOICE_1_FREQ_H",
			"VOICE_1_PULSE_L",
			"VOICE_1_PULSE_H",
			"VOICE_1_CTRL",
			"VOICE_1_AD",
			"VOICE_1_SR",
			"VOICE_2_FREQ_L",
			"VOICE_2_FREQ_H",
			"VOICE_2_PULSE_L",
			"VOICE_2_PULSE_H",
			"VOICE_2_CTRL",
			"VOICE_2_AD",
			"VOICE_2_SR",
			"VOICE_3_FREQ_L",
			"VOICE_3_FREQ_H",
			"VOICE_3_PULSE_L",
			"VOICE_3_PULSE_H",
			"VOICE_3_CTRL",
			"VOICE_3_AD",
			"VOICE_3_SR",
			"FCUT_L",
			"FCUT_H",
			"FRES",
			"FVOL",
			"PADDLE1",
			"PADDLE2",
			"OSC3",
			"ENV3",
		};
	private long fTime;

	public SIDRegModel() {
	}

	@Override
	public int getColumnCount() {
		// time/description[num]/value
		return 5;
	}

	@Override
	public int getRowCount() {
		return dataVector.size();
	}

	@Override
	public Class<String> getColumnClass(final int columnIndex) {
		return String.class;
	}

	@Override
	public String getColumnName(final int column) {
		switch (column) {
		case ABS_CYCLES:

			return "Absolute Time (Cycles)";

		case COL_CYCLES:

			return "Relative Time (Cycles)";

		case COL_CHIP_NUM:

			return "Chip No.";

		case COL_DESCRIPTION:

			return "Description";

		case COL_VALUE:

			return "Value";

		default:
			return "";
		}
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
		return false;
	}

	public void write(final long time, final int chipNum, final int addr,
			final byte data) {
		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;
		final Vector<String> row = new Vector<String>();
		row.setSize(getColumnCount());
		row.add(ABS_CYCLES, String.valueOf(time));
		row.add(COL_CYCLES, String.valueOf(relTime));
		row.add(COL_DESCRIPTION,
				localizer != null ? localizer.getString(description[addr])
						: "?");
		row.add(COL_CHIP_NUM, String.valueOf(chipNum));
		row.add(COL_VALUE, String.format("$%02X", data & 0xff));

		putInRow(row);

		fTime = time;
	}

	/**
	 * Put the row into the table
	 * 
	 * @param output
	 *            the row to add
	 */
	@SuppressWarnings("unchecked")
	private void putInRow(final Vector<String> output) {
		if (fFetchedRow == MAX_ROW_COUNT) {
			return;
		}
		dataVector.insertElementAt(output, fFetchedRow++);
		if (fFetchedRow % UPDATE_ON_ROW == 0) {
			fireTableDataChanged();
		}
	}

	public void init() {
		clearTable();
		fTime = 0;
		fFetchedRow = 0;
	}

	/**
	 * Clear recorded rows
	 */
	private void clearTable() {
		dataVector.removeAllElements();
		fireTableDataChanged();
	}

	/**
	 * Stop recording
	 */
	public void stop() {
		// set total recorded frames update table
		fireTableDataChanged();
	}

	public void setLocalizer(Localizer l) {
		localizer = l;
	}

}
