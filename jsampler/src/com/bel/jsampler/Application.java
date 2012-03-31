package com.bel.jsampler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.sun.tools.attach.VirtualMachine;

public class Application {
	public static void main(String[] args) throws Exception {
		File output = extractJsampFile();
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		MainGUI gui = new MainGUI(output.getAbsolutePath());
		gui.setTitle("JSamp Swing Frontend");
		gui.setSize(700, 500);
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setVisible(true);
	}

	/**
	 * Extract the sampling profiler from our package for use by the attached-to process.
	 * 
	 * @return Temporary file for this session.
	 * @throws Exception
	 */
	private static File extractJsampFile() throws Exception {
		InputStream res = Application.class.getClassLoader().getResourceAsStream("jsamp.jar");
		File output = File.createTempFile("jsamp", ".jar");
		FileOutputStream tmp = new FileOutputStream(output);
		byte[] data = new byte[10000];
		while (true) {
			int bytes = res.read(data);
			if (bytes <= 0) {
				break;
			}
			tmp.write(data, 0, bytes);
		}
		res.close();
		tmp.close();
		return output;
	}
}

class FloatRenderer extends DefaultTableCellRenderer {
	NumberFormat formatter;
	
	FloatRenderer() {
		formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(3);
		formatter.setMinimumFractionDigits(3);
	}
	
	@Override
	protected void setValue(Object value) {
		if (value == null) {
			setText("");
		} else {
			setText(formatter.format(value));
		}
	}
	
}

class SamplingResult extends AbstractTableModel {
	private final List<String> data = new ArrayList<String>();
	private final Map<String, Integer> self = new HashMap<String, Integer>();
	private final Map<String, Integer> cumu = new HashMap<String, Integer>();
	private int selfTotal;
	private int cumuTotal;
	
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
		case 0:
			return String.class;
		case 1:
			return Float.class;
		case 2:
			return Float.class;
		default:
			return Object.class;
		}
	}
	
	public Object getValueAt(int row, int column) {
		switch (column) {
		case 0:
			return data.get(row);
		case 1:
			if (self.containsKey(data.get(row))) {
				return self.get(data.get(row)) * 100f / selfTotal;
			} else {
				return null;
			}
		case 2:
			if (cumu.containsKey(data.get(row))) {
				return cumu.get(data.get(row)) * 100f / cumuTotal;
			} else {
				return null;
			}
		default:
			return null;
		}
	}
	
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Method";
		case 1:
			return "Self %";
		case 2:
			return "Cumulative %";
		default:
			return null;
		}
	}
	
	protected void clear() {
		self.clear();
		data.clear();
		selfTotal = 0;
	}
	
	protected void addSelf(String method) {
		if (self.containsKey(method)) {
			self.put(method, self.get(method) + 1);
		} else {
			self.put(method, 1);
		}
		selfTotal ++;
	}
	
	protected void addCumu(String method) {
		if (cumu.containsKey(method)) {
			cumu.put(method, cumu.get(method) + 1);
		} else {
			cumu.put(method, 1);
		}
		cumuTotal ++;
	}

	protected void finish() {
		Set<String> keys = new HashSet<String>();
		keys.addAll(self.keySet());
		keys.addAll(cumu.keySet());
		data.addAll(keys);
	}	
}

class MainGUI extends JFrame {
	final JLabel processLabel = new JLabel("PID:");
	final JTextField process = new JTextField();
	final JButton attach = new JButton("Attach");
	final SamplingResult samplingResult = new SamplingResult();
	final JTable table = new JTable(samplingResult);
	final String jsampJarPath;
	int port = 10000;
	
	private void fillTable(String inputFile) throws Exception {
		File input = new File(inputFile);
		BufferedReader br = new BufferedReader(new FileReader(input));
		
		String line;
		samplingResult.clear();
		while ((line = br.readLine()) != null) {
			// remove the (FooBar.java:1234) specifier
			line = line.replaceAll("\\(.*\\)$", "");
			samplingResult.addSelf(line);

			/* add self & its callers to cumulative time */
			samplingResult.addCumu(line);
			while ((line = br.readLine()) != null) {
				if (line.length() == 0) {
					break;
				}
				line = line.replaceAll("\\(.*\\)$", "");
				samplingResult.addCumu(line);
			}
		}
		samplingResult.finish();
		samplingResult.fireTableDataChanged();		

		br.close();
		input.delete();
	}
	
	MainGUI(String path) {
		jsampJarPath = path;

		table.setAutoCreateRowSorter(true);
		table.setDefaultRenderer(Float.class, new FloatRenderer());
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		add(processLabel, c);
		c.gridx = 1;
		c.weightx = 0.2;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(process, c);
		c.fill = 0;
		c.weightx = 1;
		c.gridx = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(attach, c);

		attach.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent click) {
				try {
					int interval = 1;
					File output = File.createTempFile("run", ".txt");
					output.delete();

					VirtualMachine vm = VirtualMachine.attach(process.getText());
					vm.loadAgent(jsampJarPath, interval + ";" + port + ";" + output.getAbsolutePath());
					// FIXME wait a sec for the other jvm to attach...
					JOptionPane.showMessageDialog(MainGUI.this, "Sampling to " + output.getAbsolutePath() + ", click to end");
					Socket s = new Socket(InetAddress.getLocalHost(), port);
					s.close();
					// FIXME wait a sec for the other jvm to finish dumping the results...
					Thread.sleep(5000);
					fillTable(output.getAbsolutePath());
				}
				catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(MainGUI.this, e);
				}
				port ++;
			}
		});
		
		/* add some dummy element to eat rest of the space */
		c.weighty = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy ++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = GridBagConstraints.REMAINDER;
		add(new JScrollPane(table), c);
	}
}