package applet.disassembler;

import java.awt.Point;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class DisassemblerTableModel extends AbstractTableModel {
	public static final int COL_ADDR = 0;

	public static final int COL_BYTES = 1;

	public static final int COL_INSTR = 2;

	public static final int COL_OPS = 3;

	public static final int COL_CYCLES = 4;

	/**
	 * key is opcode, value is command description
	 */
	static protected Map<Integer, CPUCommand> fCommands = CPUParser.getCpuCommands();
	
	/**
	 * viewer
	 */
	private final JTable memoryTable;
	private int newFirstRow = 0;
	private final byte[] ram;
	
	public DisassemblerTableModel(JTable memoryView, byte[] ram) {
		this.memoryTable = memoryView;
		this.ram = ram;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		
		/* get the top row's point & the row idx underneath */
		Point p = memoryTable.getVisibleRect().getLocation();
		int firstRow = memoryTable.rowAtPoint(p);

		if (newFirstRow != firstRow) {
			/* XXX we should do this in some better way. fire a table event changed,
			 * then listen to repaint msg? */
			fireTableDataChanged();
			memoryTable.repaint();
		}
		newFirstRow = firstRow;
		
		/* hack hack: walk insn table from where we started out */
		int skipRows = rowIndex - firstRow;
		rowIndex = firstRow;
		for (int i = 0; i < skipRows; i ++) {
			rowIndex += fCommands.get(ram[rowIndex] & 0xff).getByteCount();
			rowIndex &= 0xffff;
		}
		
		CPUCommand cmd = fCommands.get(ram[rowIndex] & 0xff);
		assert(cmd != null);
		
		if (columnIndex == COL_ADDR) {
			return String.format("%04X", rowIndex & 0xffff);
		} else if (columnIndex == COL_BYTES) {
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < cmd.getByteCount(); i ++) {
				hex.append(String.format("%02X ", ram[rowIndex + i & 0xffff]));
			}
			return hex.toString();			
		} else if (columnIndex == COL_INSTR || columnIndex == COL_OPS) {
			String disassembly = CPUParser.getInstance().getDisassembly(ram, rowIndex);
			String[] parts = disassembly.split(":", 2);
			if (columnIndex == 2)
				return parts[0];
			else if (parts.length == 2)
				return parts[1];
			else
				return "";
		} else {
			return cmd.getCycles();
		}
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case COL_ADDR:
			return "Addr";
		case COL_BYTES:
			return "Raw";
		case COL_INSTR:
			return "Op";
		case COL_OPS:
			return "Args";
		default:
			return "Len";
		}
	}

	public int getRowCount() {
		return 65536;
	}

	public int getColumnCount() {
		return 5;
	}
}