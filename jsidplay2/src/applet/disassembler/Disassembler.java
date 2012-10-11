package applet.disassembler;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import sidplay.ini.intf.IConfig;

public class Disassembler extends XDialog {

	protected JTextField address, startAddress, endAddress;
	protected JButton driverAddress, loadAddress, initAddress, playerAddress,
			save;
	protected JTable memoryTable;
	protected DisassemblerTableModel memoryTableModel;
	protected JScrollPane memorytablescrollPane;

	protected Player player;
	protected IConfig config;

	public Disassembler(final Player pl, final IConfig cfg) {
		this.player = pl;
		this.config = cfg;
		try {
			new SwingEngine(this).insert(
					Disassembler.class.getResource("Disassembler.xml"), this);

			fillComboBoxes();
			setDefaults();

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setTune(final SidTune tune) {
		memoryTableModel = new DisassemblerTableModel(memoryTable, player
				.getC64().getRAM());
		memoryTable.setModel(memoryTableModel);
		memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		memoryTable.getColumnModel().getColumn(DisassemblerTableModel.COL_ADDR)
				.setPreferredWidth(50);
		memoryTable.getColumnModel()
				.getColumn(DisassemblerTableModel.COL_BYTES)
				.setPreferredWidth(100);
		memoryTable.getColumnModel()
				.getColumn(DisassemblerTableModel.COL_INSTR)
				.setPreferredWidth(100);
		memoryTable.getColumnModel().getColumn(DisassemblerTableModel.COL_OPS)
				.setPreferredWidth(100);
		memoryTable.getColumnModel()
				.getColumn(DisassemblerTableModel.COL_CYCLES)
				.setPreferredWidth(50);

		if (tune == null) {
			return;
		}
		final SidTuneInfo tuneInfo = tune.getInfo();
		// Display PSID Driver location
		String line;
		if (tuneInfo.determinedDriverAddr == 0) {
			line = "N/A";
		} else {
			line = String.format("$%04x - $%04x",
					tuneInfo.determinedDriverAddr,
					tuneInfo.determinedDriverAddr
							+ tuneInfo.determinedDriverLength - 1);
		}
		driverAddress.setText(line);

		// Display PSID Load address
		line = String.format("$%04x - $%04x", tuneInfo.loadAddr,
				tuneInfo.loadAddr + tuneInfo.c64dataLen - 1);
		loadAddress.setText(line);

		// Display PSID Init address
		if (tuneInfo.playAddr == 0xffff) {
			line = String.format("SYS $%04x", tuneInfo.initAddr);
		} else {
			line = String.format("$%04x", tuneInfo.initAddr);
		}
		initAddress.setText(line);

		// Display PSID Play address
		if (tuneInfo.playAddr == 0xffff) {
			line = "N/A";
		} else {
			line = String.format("$%04x", tuneInfo.playAddr);
		}
		playerAddress.setText(line);
	}

	public Action gotoAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			gotoMem(address.getText());
		}
	};

	public Action gotoDriverAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JButton btn = (JButton) e.getSource();
			final String value = btn.getText();
			/* parse $1234 -> 1234 */
			final int hexValueBegin = value.indexOf('$');
			if (hexValueBegin > -1) {
				gotoMem("0x"
						+ value.substring(hexValueBegin + 1,
								hexValueBegin + 1 + 4));
			}
		}
	};

	public Action gotoLoadAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JButton btn = (JButton) e.getSource();
			final String value = btn.getText();
			/* parse $1234 -> 1234 */
			final int hexValueBegin = value.indexOf('$');
			if (hexValueBegin > -1) {
				gotoMem("0x"
						+ value.substring(hexValueBegin + 1,
								hexValueBegin + 1 + 4));
			}
		}
	};

	public Action gotoInitAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JButton btn = (JButton) e.getSource();
			final String value = btn.getText();
			/* parse $1234 -> 1234 */
			final int hexValueBegin = value.indexOf('$');
			if (hexValueBegin > -1) {
				gotoMem("0x"
						+ value.substring(hexValueBegin + 1,
								hexValueBegin + 1 + 4));
			}
		}
	};

	public Action gotoPlayerAddress = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JButton btn = (JButton) e.getSource();
			final String value = btn.getText();
			/* parse $1234 -> 1234 */
			final int hexValueBegin = value.indexOf('$');
			if (hexValueBegin > -1) {
				gotoMem("0x"
						+ value.substring(hexValueBegin + 1,
								hexValueBegin + 1 + 4));
			}
		}
	};

	public Action saveMemory = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser(config.getSidplay2()
					.getLastDirectory());
			int rc = chooser.showOpenDialog(getContentPane());
			if (rc == JFileChooser.APPROVE_OPTION) {
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(chooser.getSelectedFile());
					int start = Integer.decode(startAddress.getText());
					int end = Integer.decode(endAddress.getText()) + 1;
					start &= 0xffff;
					end &= 0xffff;
					fos.write(start & 0xff);
					fos.write(start >> 8);
					byte[] ram = player.getC64().getRAM();
					while (start != end) {
						fos.write(ram[start]);
						start = start + 1 & 0xffff;
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(getContentPane(),
							ex.getMessage());
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(getContentPane(),
									ex.getMessage());
						}
					}
				}
			}
		}
	};

	private void setDefaults() {
		// nothing to do
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	protected void gotoMem(final String destination) {
		try {
			int dest = Integer.decode(destination) & 0xffff;
			address.setText(String.format("0x%04x", dest));
			memorytablescrollPane.getViewport().setViewPosition(
					memoryTable.getCellRect(
							memoryTable.convertRowIndexToView(dest), 0, true)
							.getLocation());
		} catch (final NumberFormatException e) {
		}
	}

}
