package applet.gamebase;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
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

import applet.entities.gamebase.Games;
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
					Vector<Games> vector = (Vector<Games>) dataModel
							.getDataVector().get(row);
					startGame(vector.get(0));
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
					Vector<Games> vector = (Vector<Games>) dataModel
							.getDataVector().get(row);
					startGame(vector.get(0));
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
							Vector<Games> vector = (Vector<Games>) dataModel
									.getDataVector().get(row);
							Games game = vector.get(0);
							downloadStart(
									"http://www.gb64.com/Screenshots/"
											+ game.getScreenshotFilename()
													.replace('\\', '/'),
									screenShotListener);
						}
					}
				});
	}

	abstract void downloadStart(String url, IDownloadListener listener);

	protected void startGame(Games game) {
		((GameListener) gameListener).setFileToRun(game.getFileToRun());
		downloadStart("http://gamebase64.hardabasht.com/games/"
				+ game.getFilename().replace('\\', '/'), gameListener);
	}

	public void setRows(List<Games> games) {
		rowSorter.setRowFilter(null);
		dataModel.setNumRows(0);
		for (Games game : games) {
			Vector<Games> data = new Vector<Games>();
			data.add(game);
			dataModel.addRow(data);
		}
		rowSorter.allRowsChanged();
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
