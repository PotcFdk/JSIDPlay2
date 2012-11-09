package applet.favorites;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import libsidplay.Player;
import libsidutils.STIL.STILEntry;
import libsidutils.zip.ZipEntryFileProxy;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.PathUtils;
import applet.SidTuneConverter;
import applet.dnd.FileDrop;
import applet.entities.collection.HVSCEntry_;
import applet.events.IPlayTune;
import applet.events.UIEventFactory;
import applet.events.favorites.IFavoriteTabNames;
import applet.filefilter.TuneFileFilter;
import applet.stil.STIL;
import applet.ui.JNiceButton;

@SuppressWarnings("serial")
public class Favorites extends JPanel implements IFavorites {

	private final class AddColumnAction implements ActionListener {
		private final Object element;

		private AddColumnAction(Object element) {
			this.element = element;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				playListTable.getColumn(element);
				// if column already exist, do nothing
			} catch (IllegalArgumentException e1) {
				favoritesModel.addColumn(element);
			}

		}
	}

	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	private SwingEngine swix;

	protected JTable playListTable;
	protected JTextField filterField;
	protected JNiceButton unsort, moveUp, moveDown;

	protected FavoritesModel favoritesModel;
	protected transient RowSorter<TableModel> rowSorter;
	protected transient final FileFilter tuneFilter = new TuneFileFilter();

	protected final FavoritesView favoritesView;
	protected Player player;
	protected IConfig config;

	protected int headerColumnToRemove;
	protected File lastDir;
	protected Random randomPlayback = new Random();

	public Favorites(Player pl, IConfig cfg, EntityManager em,
			final FavoritesView favoritesView, IFavoritesSection favorite) {
		this.favoritesView = favoritesView;
		this.player = pl;
		this.config = cfg;

		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("nicebutton", JNiceButton.class);
			swix.getTaglib().registerTag("playlisttable", FavoritesTable.class);
			swix.insert(Favorites.class.getResource("Favorites.xml"), this);

			favoritesModel = (FavoritesModel) playListTable.getModel();
			favoritesModel.setConfig(cfg, favorite, swix.getLocalizer(), em);
			favoritesModel.setCollections(favoritesView.getHvsc(),
					favoritesView.getCgsc());
			((FavoritesCellRenderer) playListTable
					.getDefaultRenderer(Object.class)).setConfig(cfg);
			((FavoritesCellRenderer) playListTable
					.getDefaultRenderer(Object.class)).setPlayer(pl);
			favoritesModel.layoutChanged();

			fillComboBoxes();
			setDefaultsAndActions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SwingEngine getSwix() {
		return swix;
	}

	private void setDefaultsAndActions() {
		JTableHeader header = playListTable.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			private final JPopupMenu headerPopup;
			private JMenuItem removeColumn;
			{
				headerPopup = new JPopupMenu(getSwix().getLocalizer()
						.getString("CUSTOMIZE_COLUMN"));
				removeColumn = new JMenuItem(getSwix().getLocalizer()
						.getString("REMOVE_COLUMN"));
				removeColumn.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						removeColumn();
					}
				});
				headerPopup.add(removeColumn);

				addAddColumnMenuItem(HVSCEntry_.title);
				addAddColumnMenuItem(HVSCEntry_.author);
				addAddColumnMenuItem(HVSCEntry_.released);
				addAddColumnMenuItem(HVSCEntry_.format);
				addAddColumnMenuItem(HVSCEntry_.playerId);
				addAddColumnMenuItem(HVSCEntry_.noOfSongs);
				addAddColumnMenuItem(HVSCEntry_.startSong);
				addAddColumnMenuItem(HVSCEntry_.clockFreq);
				addAddColumnMenuItem(HVSCEntry_.speed);
				addAddColumnMenuItem(HVSCEntry_.sidModel1);
				addAddColumnMenuItem(HVSCEntry_.sidModel2);
				addAddColumnMenuItem(HVSCEntry_.compatibility);
				addAddColumnMenuItem(HVSCEntry_.tuneLength);
				addAddColumnMenuItem(HVSCEntry_.audio);
				addAddColumnMenuItem(HVSCEntry_.sidChipBase1);
				addAddColumnMenuItem(HVSCEntry_.sidChipBase2);
				addAddColumnMenuItem(HVSCEntry_.driverAddress);
				addAddColumnMenuItem(HVSCEntry_.loadAddress);
				addAddColumnMenuItem(HVSCEntry_.loadLength);
				addAddColumnMenuItem(HVSCEntry_.initAddress);
				addAddColumnMenuItem(HVSCEntry_.playerAddress);
				addAddColumnMenuItem(HVSCEntry_.fileDate);
				addAddColumnMenuItem(HVSCEntry_.fileSizeKb);
				addAddColumnMenuItem(HVSCEntry_.tuneSizeB);
				addAddColumnMenuItem(HVSCEntry_.relocStartPage);
				addAddColumnMenuItem(HVSCEntry_.relocNoPages);

			}

			private void addAddColumnMenuItem(SingularAttribute<?, ?> field) {
				JMenuItem menuItem = new JMenuItem(String.format(
						getSwix().getLocalizer().getString("ADD_COLUMN"),
						getSwix().getLocalizer().getString(
								field.getDeclaringType().getJavaType()
										.getSimpleName()
										+ "." + field.getName())));
				menuItem.addActionListener(new AddColumnAction(field));
				headerPopup.add(menuItem);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					JTableHeader popupHeader = (JTableHeader) e.getSource();
					headerColumnToRemove = popupHeader.columnAtPoint(new Point(
							e.getPoint()));
					int columnModelIndex = playListTable
							.convertColumnIndexToModel(headerColumnToRemove);
					if (columnModelIndex == 0) {
						removeColumn.setEnabled(false);
					} else {
						removeColumn.setEnabled(true);
					}
					headerPopup.show((Component) e.getSource(), e.getX(),
							e.getY());
				}
			}

		});
		rowSorter = new TableRowSorter<TableModel>(favoritesModel);
		rowSorter.addRowSorterListener(new RowSorterListener() {

			@Override
			public void sorterChanged(RowSorterEvent e) {
				setMoveEnabledState(getSelection());
			}

		});
		playListTable.setRowSorter(rowSorter);
		playListTable.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getModifiers() == 0
						&& KeyEvent.VK_BACK_SPACE == e.getKeyCode()
						|| KeyEvent.VK_DELETE == e.getKeyCode()) {
					removeSelectedRows();
				} else if (e.getModifiers() == 0
						&& KeyEvent.VK_ENTER == e.getKeyCode()) {
					playSelectedRow();
				}

			}

		});
		playListTable.addMouseListener(new MouseAdapter() {

			private JPopupMenu tablePopup;

			protected STILEntry getSTIL(final File file) {
				final String name = PathUtils.getHVSCName(config, file);
				if (null != name) {
					libsidutils.STIL stil = libsidutils.STIL.getInstance(config
							.getSidplay2().getHvsc());
					if (stil != null) {
						return stil.getSTIL(name);
					}
				}
				return null;
			}

			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				if (mouseEvent.getButton() == MouseEvent.BUTTON1
						&& mouseEvent.getClickCount() == 2) {
					playSelectedRow();
				} else if (mouseEvent.getButton() == MouseEvent.BUTTON3
						&& mouseEvent.getClickCount() == 1
						&& playListTable.getSelectionModel()
								.getMinSelectionIndex() != -1) {

					tablePopup = new JPopupMenu(getSwix().getLocalizer()
							.getString("TUNE_ACTIONS"));
					JMenuItem mi = new JMenuItem(getSwix().getLocalizer()
							.getString("SHOW_STIL"));
					mi.setEnabled(false);
					tablePopup.add(mi);
					int[] rows = playListTable.getSelectedRows();
					if (rows.length == 1) {
						int row = rowSorter.convertRowIndexToModel(rows[0]);
						File tuneFile = favoritesModel.getFile(row);
						final STILEntry se = getSTIL(tuneFile);
						if (se != null) {
							mi.setEnabled(true);
							mi.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent arg0) {
									new STIL(se);
								}
							});
						}
					}
					JMenuItem fileExportItem = new JMenuItem(getSwix()
							.getLocalizer().getString("EXPORT_TO_DIR"));
					fileExportItem.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							JFileChooser fc = new JFileChooser(lastDir);
							fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							final Frame containerFrame = JOptionPane
									.getFrameForComponent(Favorites.this);
							int result = fc.showOpenDialog(containerFrame);
							if (result == JFileChooser.APPROVE_OPTION
									&& fc.getSelectedFile() != null) {
								lastDir = fc.getSelectedFile();
								int[] rows = playListTable.getSelectedRows();
								for (int row1 : rows) {
									int row = rowSorter
											.convertRowIndexToModel(row1);
									File file = favoritesModel.getFile(row);
									try {
										if (file instanceof ZipEntryFileProxy) {
											// Extract ZIP file
											file = ZipEntryFileProxy
													.extractFromZip(
															(ZipEntryFileProxy) file,
															config.getSidplay2()
																	.getTmpDir());
										}
										PathUtils.copyFile(file,
												new File(fc.getSelectedFile(),
														file.getName()));
									} catch (IOException e1) {
										System.err.println(e1.getMessage());
									}
								}
							}
						}
					});
					tablePopup.add(fileExportItem);

					final JMenu moveToTab = new JMenu(getSwix().getLocalizer()
							.getString("MOVE_TO_TAB"));
					uiEvents.fireEvent(IFavoriteTabNames.class,
							new IFavoriteTabNames() {

								@Override
								public void setFavoriteTabNames(String[] names,
										String selected) {
									for (String name1 : names) {
										if (!name1.equals(selected)) {
											final String title = name1;
											JMenuItem tabItem = new JMenuItem(
													title);
											tabItem.addActionListener(new ActionListener() {

												@Override
												public void actionPerformed(
														ActionEvent e) {
													moveSelectedFavoritesToTab(
															title, false);
												}

											});
											moveToTab.add(tabItem);
										}
									}
								}
							});
					tablePopup.add(moveToTab);
					if (moveToTab.getMenuComponentCount() == 0) {
						moveToTab.setEnabled(false);
					}
					final JMenu copyToTab = new JMenu(getSwix().getLocalizer()
							.getString("COPY_TO_TAB"));
					uiEvents.fireEvent(IFavoriteTabNames.class,
							new IFavoriteTabNames() {

								@Override
								public void setFavoriteTabNames(String[] names,
										String selected) {
									for (String name1 : names) {
										if (!name1.equals(selected)) {
											final String title = name1;
											JMenuItem tabItem = new JMenuItem(
													title);
											tabItem.addActionListener(new ActionListener() {

												@Override
												public void actionPerformed(
														ActionEvent e) {
													moveSelectedFavoritesToTab(
															title, true);
												}

											});
											copyToTab.add(tabItem);
										}
									}
								}
							});
					tablePopup.add(copyToTab);
					if (copyToTab.getMenuComponentCount() == 0) {
						copyToTab.setEnabled(false);
					}

					JMenu convertItem = new JMenu(getSwix().getLocalizer()
							.getString("CONVERT_TO"));

					JMenuItem psid64 = new JMenuItem(getSwix().getLocalizer()
							.getString("PSID64"));
					psid64.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							JFileChooser fc = new JFileChooser(lastDir);
							fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							final Frame containerFrame = JOptionPane
									.getFrameForComponent(Favorites.this);
							int result = fc.showOpenDialog(containerFrame);
							if (result == JFileChooser.APPROVE_OPTION
									&& fc.getSelectedFile() != null) {
								lastDir = fc.getSelectedFile();
								convertSelectedTunes();
							}
						}

					});
					convertItem.add(psid64);

					tablePopup.add(convertItem);

					tablePopup.show((Component) mouseEvent.getSource(),
							mouseEvent.getX(), mouseEvent.getY());
				}
			}

		});

		playListTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						String[] selection = getSelection();
						setMoveEnabledState(selection);
					}

				});
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(java.io.File[] files, Object source,
					Point point) {
				addToFavorites(files);
			}
		});
		filterField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (filterField.getText().trim().length() == 0) {
					((TableRowSorter<TableModel>) rowSorter).setRowFilter(null);
				} else {
					if (validatePattern()) {
						RowFilter<TableModel, Integer> filter = RowFilter
								.regexFilter(filterField.getText());
						((TableRowSorter<TableModel>) rowSorter)
								.setRowFilter(filter);
					}
				}
			}

			private boolean validatePattern() {
				boolean ok = true;
				filterField.setToolTipText(null);
				filterField.setBackground(Color.white);
				try {
					Pattern.compile(filterField.getText());
				} catch (PatternSyntaxException e) {
					filterField.setToolTipText(e.getMessage());
					filterField.setBackground(Color.red);
					ok = false;
				}
				return ok;
			}

		});
	}

	public JTable getPlayListTable() {
		return playListTable;
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	@Override
	public void addToFavorites(File[] files) {
		for (int i = 0; files != null && i < files.length; i++) {
			final File file = files[i];
			if (file.isDirectory()) {
				addToFavorites(file.listFiles());
			} else {
				if (tuneFilter.accept(file)) {
					favoritesModel.add(createRelativePath(file));
				}
			}
		}
		favoritesModel.layoutChanged();
	}

	protected void playSelectedRow() {
		int selectedRow = playListTable.getSelectedRow();
		if (selectedRow == -1) {
			return;
		}
		final int selectedModelRow = rowSorter
				.convertRowIndexToModel(selectedRow);
		if (selectedModelRow != -1) {
			// play tune
			uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {
				@Override
				public boolean switchToVideoTab() {
					return false;
				}

				@Override
				public File getFile() {
					return favoritesModel.getFile(selectedModelRow);
				}

				@Override
				public Component getComponent() {
					return favoritesView;
				}
			});
			favoritesView.setCurrentlyPlayedFavorites(this);
		}
	}

	@Override
	public void removeSelectedRows() {
		int[] rows = playListTable.getSelectedRows();
		if (rows.length == 0) {
			return;
		}
		int response = JOptionPane.showConfirmDialog(this, String.format(swix
				.getLocalizer().getString("REMOVE_N_OF_MY_FAVORITES"),
				rows.length),
				swix.getLocalizer().getString("REMOVE_FAVORITES"),
				JOptionPane.YES_NO_OPTION);
		if (response == JOptionPane.YES_OPTION) {
			for (int i = 0; i < rows.length; i++) {
				int row = rowSorter.convertRowIndexToModel(rows[i]);
				favoritesModel.remove(row);
				// shift row numbers
				for (int j = i + 1; j < rows.length; j++) {
					rows[j] = rows[j] - 1;
				}
			}
		}
		favoritesModel.layoutChanged();
	}

	@Override
	public void loadFavorites(String filename) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename), "ISO-8859-1"))) {
			// new favorites file format
			String line;
			favoritesModel.clear();
			while ((line = r.readLine()) != null) {
				favoritesModel.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveFavorites(String filename) {
		try (PrintStream p = new PrintStream(filename, "ISO-8859-1")) {
			for (int i = 0; i < favoritesModel.size(); i++) {
				p.println(createRelativePath(favoritesModel.getFile(i)));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deselectFavorites() {
		playListTable.getSelectionModel().clearSelection();
		String[] selection = new String[0];
		setMoveEnabledState(selection);
	}

	protected void setMoveEnabledState(String[] selection) {
		List<? extends SortKey> keys = rowSorter.getSortKeys();
		int keySize = keys.size();
		if (keySize == 0 || keys.get(0).getSortOrder() == SortOrder.UNSORTED) {
			moveUp.setEnabled(selection.length == 1);
			moveDown.setEnabled(selection.length == 1);
			unsort.setEnabled(false);
		} else {
			moveUp.setEnabled(false);
			moveDown.setEnabled(false);
			unsort.setEnabled(true);
		}
	}

	@Override
	public void selectFavorites() {
		if (favoritesModel.size() == 0) {
			return;
		}
		playListTable.getSelectionModel().setSelectionInterval(0,
				favoritesModel.size() - 1);
	}

	public Action doUnsort = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			rowSorter.setSortKeys(new ArrayList<SortKey>());
		}
	};

	public Action doMoveUp = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int row = playListTable.getSelectedRow();
			int start = row;
			int to = row > 0 ? row - 1 : row;
			favoritesModel.move(start, to);
			playListTable.getSelectionModel().setSelectionInterval(to, to);
		}
	};

	public Action doMoveDown = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int row = playListTable.getSelectedRow();
			int start = row;
			int to = row < favoritesModel.size() - 1 ? row + 1 : row;
			favoritesModel.move(start, to);
			playListTable.getSelectionModel().setSelectionInterval(to, to);
		}
	};

	protected String createRelativePath(File fileToConvert) {
		boolean converted = false;
		String result = fileToConvert.getAbsolutePath();
		String hvscName = PathUtils.getHVSCName(config, fileToConvert);
		if (hvscName != null) {
			result = FavoritesModel.HVSC_PREFIX + hvscName;
			converted = true;
		}
		String cgscName = PathUtils.getCGSCName(config, fileToConvert);
		if (!converted && cgscName != null) {
			result = FavoritesModel.CGSC_PREFIX + cgscName;
			converted = true;
		}
		if (!converted && fileToConvert.isAbsolute()) {
			result = fileToConvert.getAbsolutePath();
		}
		return result;
	}

	protected void removeColumn() {
		int columnModelIndex = playListTable
				.convertColumnIndexToModel(headerColumnToRemove);
		if (columnModelIndex == 0) {
			// do not remove filenames
			return;
		}
		favoritesModel.removeColumn(columnModelIndex);
		favoritesModel.layoutChanged();
	}

	protected void moveSelectedFavoritesToTab(final String title,
			final boolean copy) {
		JTabbedPane pane = favoritesView.getTabbedPane();
		int index = pane.indexOfTab(title);
		// target panel
		IFavorites panel = (IFavorites) pane.getComponentAt(index);

		int[] rows = playListTable.getSelectedRows();
		for (int i = 0; i < rows.length; i++) {
			int row = rowSorter.convertRowIndexToModel(rows[i]);
			// add next row to target tab
			panel.addToFavorites(new File[] { favoritesModel.getFile(row) });
			if (!copy) {
				// remove row from source tab
				favoritesModel.remove(row);
				// shift row numbers
				for (int j = i + 1; j < rows.length; j++) {
					rows[j] = rows[j] - 1;
				}
			}
		}
	}

	protected void convertSelectedTunes() {
		final ArrayList<File> files = new ArrayList<File>();
		int[] rows = playListTable.getSelectedRows();
		for (int row1 : rows) {
			int row = rowSorter.convertRowIndexToModel(row1);
			files.add(favoritesModel.getFile(row));
		}

		SidTuneConverter c = new SidTuneConverter(config);
		c.convertFiles(files.toArray(new File[0]), lastDir);
	}

	@Override
	public File getNextFile(File file) {
		int playedRow = -1;
		int rowCount = playListTable.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			int row = rowSorter.convertRowIndexToModel(i);
			File currFile = favoritesModel.getFile(row);
			if (currFile.equals(file)) {
				playedRow = i;
				break;
			}
		}
		final int nextRow = playedRow + 1;
		if (nextRow == playListTable.getRowCount()) {
			return null;
		}
		int row = rowSorter.convertRowIndexToModel(nextRow);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				playListTable.scrollRectToVisible(playListTable.getCellRect(
						nextRow, 0, true));
			}

		});
		return favoritesModel.getFile(row);
	}

	@Override
	public File getNextRandomFile(File file) {
		int rowCount = playListTable.getRowCount();
		final int randomRow = Math.abs(randomPlayback
				.nextInt(Integer.MAX_VALUE)) % rowCount;
		int row = rowSorter.convertRowIndexToModel(randomRow);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				playListTable.scrollRectToVisible(playListTable.getCellRect(
						randomRow, 0, true));
			}

		});
		return favoritesModel.getFile(row);
	}

	@Override
	public String[] getSelection() {
		int[] rows = playListTable.getSelectedRows();
		if (rows.length == 0) {
			return new String[0];
		}
		ArrayList<String> filenames = new ArrayList<String>();
		for (int row1 : rows) {
			int row = rowSorter.convertRowIndexToModel(row1);
			filenames.add(favoritesModel.getFile(row).getAbsolutePath());
		}
		String[] retValue = filenames.toArray(new String[filenames.size()]);
		return retValue;
	}

	@Override
	public boolean isEmpty() {
		return favoritesModel.size() == 0;
	}

}
