package applet.filechooser;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import libsidplay.components.DirEntry;
import libsidplay.components.Directory;
import libsidutils.zip.ZipEntryFileProxy;

import org.swixml.SwingEngine;

import applet.JSIDPlay2;
import applet.entities.config.Configuration;

public class ImagePreview extends JPanel implements PropertyChangeListener {
	/**
	 * Property: A program has been double clicked.
	 */
	public static final String PROP_AUTOSTART_PRG = "autostart";
	/**
	 * Property: An image has been attached.
	 */
	public static final String PROP_ATTACH_IMAGE = "attach";
	/**
	 * Custom C64 font resource name.
	 * 
	 * http://style64.org/c64-truetype/petscii-rom-mapping
	 */
	private static final String FONT_NAME = "fonts/C64_Elite_Mono_v1.0-STYLE.ttf";
	/**
	 * Font size.
	 */
	private static final float FONT_SIZE = 10f;
	/**
	 * Upper case letters.
	 */
	private static final int TRUE_TYPE_FONT_BIG = 0xe000;
	/**
	 * Lower case letters.
	 */
	private static final int TRUE_TYPE_FONT_SMALL = 0xe100;
	/**
	 * Inverse Upper case letters.
	 */
	private static final int TRUE_TYPE_FONT_INVERSE_BIG = 0xe200;
	/**
	 * Inverse Lower case letters.
	 */
	private static final int TRUE_TYPE_FONT_INVERSE_SMALL = 0xe300;

	/**
	 * Current font set.
	 */
	protected int fontSet = TRUE_TYPE_FONT_BIG;
	/**
	 * Current font set.
	 */
	protected int fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;

	protected SwingEngine swix;
	protected JScrollPane scrollPane;
	protected JTable directory;

	private Configuration config;

	/**
	 * File to preview.
	 */
	protected File file = null;

	private Font cbmFont;
	{
		try {
			// Use custom CBM font
			InputStream fontStream = JSIDPlay2.class
					.getResourceAsStream(FONT_NAME);
			cbmFont = Font.createFont(Font.TRUETYPE_FONT, fontStream)
					.deriveFont(Font.PLAIN, FONT_SIZE);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Action doSwitchFont = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (fontSet == TRUE_TYPE_FONT_BIG) {
				fontSet = TRUE_TYPE_FONT_SMALL;
			} else {
				fontSet = TRUE_TYPE_FONT_BIG;
			}
			if (fontSetHeader == TRUE_TYPE_FONT_INVERSE_BIG) {
				fontSetHeader = TRUE_TYPE_FONT_INVERSE_SMALL;
			} else {
				fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;
			}
			// force update
			propertyChange(new PropertyChangeEvent(this,
					JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, null,
					getFile()));
		}
	};

	public ImagePreview() {
		try {
			swix = new SwingEngine(this);
			swix.insert(ImagePreview.class.getResource("ImagePreview.xml"),
					this);
			if (cbmFont != null) {
				directory.setFont(cbmFont);
				directory.getTableHeader().setFont(cbmFont);
			}
			directory.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getModifiers() == 0
							&& KeyEvent.VK_ENTER == e.getKeyCode()) {
						autoStartProgram();
					}
				}

			});
			directory.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent mouseEvent) {
					if (mouseEvent.getButton() == MouseEvent.BUTTON1
							&& mouseEvent.getClickCount() > 1) {
						autoStartProgram();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setConfig(final Configuration config) {
		this.config = config;
	}

	public void loadPreview() {
		if (file == null) {
			return;
		}

		try {
			if (file instanceof ZipEntryFileProxy) {
				// Load file entry from ZIP
				ZipEntryFileProxy zipEntry = (ZipEntryFileProxy) file;
				file = ZipEntryFileProxy.extractFromZip(zipEntry, config
						.getSidplay2().getTmpDir());
			}

			Vector<String> rowData;
			ImageTableModel model = (ImageTableModel) directory.getModel();
			model.setRowCount(0);
			try {
				Directory dir = DirectoryUtil.getDirectory(file, config);
				if (dir != null) {
					// Print directory title/id
					model.setColumnName(0, print(dir.toString(), fontSetHeader));
					List<DirEntry> dirEntries = dir.getDirEntries();
					// Print directory entries
					for (DirEntry dirEntry : dirEntries) {
						rowData = new Vector<String>();
						rowData.add(print(dirEntry.toString(), fontSet));
						model.addRow(rowData);
					}
					// Print directory result
					if (dir.getStatusLine() != null) {
						rowData = new Vector<String>();
						rowData.add(print(dir.getStatusLine(), fontSet));
						model.addRow(rowData);
					}
				} else {
					throw new IOException();
				}
			} catch (IOException ioE) {
				model.setColumnName(0, "");
				rowData = new Vector<String>();
				rowData.add(print("SORRY, NO PREVIEW AVAILABLE!",
						TRUE_TYPE_FONT_BIG));
				model.addRow(rowData);
			}

			rowData = new Vector<String>();
			rowData.add(print("READY.", TRUE_TYPE_FONT_BIG));
			model.addRow(rowData);

			model.fireTableStructureChanged();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String print(final char c, int fontSet) {
		if ((c & 0x60) == 0) {
			return String.valueOf((char) (c | 0x40 | (fontSet ^ 0x0200)));
		} else {
			return String.valueOf((char) (c | fontSet));
		}
	}

	private String print(final String s, int fontSet) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			buf.append(print(s.charAt(i), fontSet));
		}
		return buf.toString();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent e) {
		boolean update = false;
		final String prop = e.getPropertyName();

		// If the directory changed, don't show an image.
		if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
			file = null;
			update = true;

			// If a file became selected, find out which one.
		} else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
			file = (File) e.getNewValue();
			update = true;
		}

		// Update the preview accordingly.
		if (update) {
			if (isShowing()) {
				loadPreview();
				repaint();
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						getScrollPane().getVerticalScrollBar().setValue(0);
					}
				});
			}
		}
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public File getFile() {
		return file;
	}

	protected void autoStartProgram() {
		int row = directory.getSelectedRow();
		if (row != -1) {
			try {
				Directory dir = DirectoryUtil.getDirectory(file, config);
				if (dir != null) {
					List<DirEntry> dirEntries = dir.getDirEntries();
					if (row < dirEntries.size()) {
						try {
							DirEntry dirEntry = dirEntries.get(row);
							File autostartFile = new File(config.getSidplay2()
									.getTmpDir(), dirEntry.getValidFilename()
									+ ".prg");
							autostartFile.deleteOnExit();
							dirEntry.save(autostartFile);
							firePropertyChange(PROP_AUTOSTART_PRG, null,
									autostartFile);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
