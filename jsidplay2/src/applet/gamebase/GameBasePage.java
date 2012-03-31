package applet.gamebase;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.swixml.SwingEngine;

import applet.gamebase.listeners.GameListener;
import applet.soundsettings.IDownloadListener;

@SuppressWarnings("unchecked")
public abstract class GameBasePage extends JPanel {

	protected GameBase gameBase;
	protected IDownloadListener screenShotListener, gameListener;

	private SwingEngine swix;

	protected JTable gamebasetable;
	protected TableRowSorter<TableModel> rowSorter;
	protected DefaultTableModel dataModel;

	public GameBasePage(GameBase gb, IDownloadListener scrListener,
			IDownloadListener gmListener) {
		this.gameBase = gb;
		this.screenShotListener = scrListener;
		this.gameListener = gmListener;
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("gamebasetable", GameBaseTable.class);
			swix.insert(GameBase.class.getResource("GameBasePage.xml"), this);

			dataModel = (DefaultTableModel) gamebasetable.getModel();
			rowSorter = (TableRowSorter<TableModel>) gamebasetable
					.getRowSorter();

			setDefaultsAndActions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		gamebasetable.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(final KeyEvent e) {
				/* handle return presses */
				if (e.getKeyChar() != '\n') {
					return;
				}

				/* find current selection */
				int row1 = gamebasetable.getSelectedRow();
				if (row1 != -1) {
					int row = rowSorter.convertRowIndexToModel(row1);
					startGame((Vector<String>) dataModel.getDataVector().get(
							row));
				}
			}
		});
		gamebasetable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				int row1 = gamebasetable.getSelectedRow();
				if (row1 != -1 && mouseEvent.getButton() == MouseEvent.BUTTON1
						&& mouseEvent.getClickCount() > 1) {
					int row = rowSorter.convertRowIndexToModel(row1);
					startGame((Vector<String>) dataModel.getDataVector().get(
							row));
				}
			}
		});
		gamebasetable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						int row1 = gamebasetable.getSelectedRow();
						if (row1 != -1 && !e.getValueIsAdjusting()) {
							int row = rowSorter.convertRowIndexToModel(row1);
							Vector<String> vector = (Vector<String>) dataModel
									.getDataVector().get(row);
							String filename = String.valueOf(vector.get(3));

							downloadStart("http://www.gb64.com/Screenshots/"
									+ filename.replace('\\', '/'),
									screenShotListener);
						}
					}
				});
	}

	abstract void downloadStart(String url, IDownloadListener listener);

	protected void startGame(Vector<String> vector) {
		String filename = String.valueOf(vector.get(1));
		((GameListener) gameListener).setFileToRun(String
				.valueOf(vector.get(2)));
		downloadStart(
				"http://gamebase64.hardabasht.com/games/"
						+ filename.replace('\\', '/'), gameListener);
	}

	public void setRows(ResultSet result) {
		rowSorter.setRowFilter(null);
		dataModel.setNumRows(0);
		try {
			while (result.next()) {
				Vector<String> data = new Vector<String>();
				data.add(result.getString("NAME"));
				data.add(result.getString("FILENAME"));
				data.add(result.getString("FILETORUN"));
				data.add(result.getString("SCRNSHOTFILENAME"));
				data.add(result.getString("COMMENT"));
				data.add(String.valueOf(result.getInt("GE_ID")));
				data.add(String.valueOf(result.getInt("YE_ID")));
				data.add(String.valueOf(result.getInt("PU_ID")));
				data.add(String.valueOf(result.getInt("MU_ID")));
				data.add(String.valueOf(result.getInt("PR_ID")));
				data.add(result.getString("SIDFILENAME"));
				dataModel.addRow(data);
			}
			rowSorter.allRowsChanged();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

	}

	public void filter(String filterText) {
		if (filterText.trim().length() == 0) {
			rowSorter.setRowFilter(null);
		} else {
			RowFilter<TableModel, Integer> filter = RowFilter
					.regexFilter(filterText);
			rowSorter.setRowFilter(filter);
		}
	}

}
