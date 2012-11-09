package applet.collection;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.metamodel.SingularAttribute;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;
import libsidutils.STIL.STILEntry;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.PathUtils;
import applet.SidTuneConverter;
import applet.TuneTab;
import applet.collection.search.ISearchListener;
import applet.collection.search.SearchInIndexThread;
import applet.collection.search.SearchIndexCreator;
import applet.collection.search.SearchIndexerThread;
import applet.collection.search.SearchThread;
import applet.config.ConfigView;
import applet.config.editors.CharTextField;
import applet.config.editors.FloatTextField;
import applet.config.editors.IntTextField;
import applet.config.editors.LongTextField;
import applet.config.editors.ShortTextField;
import applet.config.editors.YearTextField;
import applet.download.DownloadThread;
import applet.entities.PersistenceProperties;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.HVSCEntry_;
import applet.entities.collection.StilEntry;
import applet.entities.collection.StilEntry_;
import applet.entities.collection.service.VersionService;
import applet.events.ICollectionChanged;
import applet.events.ICollectionChanged.CollectionType;
import applet.events.IGotoURL;
import applet.events.IMadeProgress;
import applet.events.IPlayTune;
import applet.events.UIEvent;
import applet.events.favorites.IAddFavoritesTab;
import applet.events.favorites.IFavoriteTabNames;
import applet.events.favorites.IGetFavorites;
import applet.favorites.IFavorites;
import applet.filefilter.CollectionFileFilter;
import applet.gamebase.listeners.ProgressListener;
import applet.ui.JNiceButton;

/**
 * Common view base for HVSC and CGSC collections. Loosely based on Rhythmbox,
 * which is probably based on iTunes. Display is divided to 2 vertical panels of
 * identical widths
 * 
 * - 1st shows file meta info in table, and composer's photo, scaled to 100% of
 * width. These take the whole vertical space.
 * 
 * - 2nd column show search bar, which is used to match song and artist name
 * 
 * - 2nd column displays list of artists.
 * 
 * - 2nd column displays list of songs matching search criteria and selected
 * artist. - currently playing symbol - artist name - song name - total song
 * length (?)
 * 
 * @author Ken Händel
 * @author Antti Lankila
 */
public abstract class Collection extends TuneTab implements
		TreeSelectionListener, ISearchListener {

	/**
	 * HVSC listener gets informed, if the download has been completed.
	 * 
	 * @author Ken
	 * 
	 */
	public class HVSCListener extends ProgressListener {

		@Override
		public void downloaded(final File downloadedFile) {
			autoConfiguration.setEnabled(true);
			if (downloadedFile != null) {
				setRootFile(downloadedFile);
			}
		}
	}

	/**
	 * CGSC listener gets informed, if the download has been completed.
	 * 
	 * @author Ken
	 * 
	 */
	public class CGSCListener extends ProgressListener {

		@Override
		public void downloaded(final File downloadedFile) {
			autoConfiguration.setEnabled(true);
			if (downloadedFile != null) {
				setRootFile(downloadedFile);
			}
		}
	}

	@SuppressWarnings("serial")
	public static class HVSC extends Collection {
		public HVSC(final Player pl, final IConfig cfg) {
			super(pl, cfg, "High Voltage SID Collection",
					"http://www.hvsc.de/", "PLEASE_SELECT_HVSC", "HVSC");
		}

		@Override
		public void createContents() {
			super.createContents();
			// Initially configure HVSC collection
			if (config.getSidplay2().getHvsc() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.getSidplay2().getHvsc());
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.HVSC;
							}
						});
			}
		}

		@Override
		protected void setRootDir(final File rootFile) {
			if (rootFile.exists()) {
				config.getSidplay2().setHvsc(rootFile.getAbsolutePath());
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return rootFile;
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.HVSC;
							}
						});
			}
		}

		@Override
		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.HVSC) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected void autoConfig() {
			if (autoConfiguration.isSelected()) {
				autoConfiguration.setEnabled(false);
				try {
					DownloadThread downloadThread = new DownloadThread(config,
							new HVSCListener(), new URL(config.getOnline()
									.getHvscUrl()));
					downloadThread.start();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("serial")
	public static class CGSC extends Collection {
		public CGSC(final Player pl, final IConfig cfg) {
			super(pl, cfg, "Compute's Gazette Sid Collection",
					"http://www.btinternet.com/~pweighill/music/",
					"PLEASE_SELECT_CGSC", "CGSC");
		}

		@Override
		public void createContents() {
			super.createContents();
			// Initially configure CGSC collection
			if (config.getSidplay2().getCgsc() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.getSidplay2().getCgsc());
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.CGSC;
							}
						});
			}
		}

		@Override
		protected void setRootDir(final File rootFile) {
			if (rootFile.exists()) {
				// save settings
				config.getSidplay2().setCgsc(rootFile.getAbsolutePath());
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return rootFile;
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.CGSC;
							}
						});
			}
		}

		@Override
		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.CGSC) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected void autoConfig() {
			if (autoConfiguration.isSelected()) {
				// First time, the CGSC is downloaded
				autoConfiguration.setEnabled(false);
				try {
					DownloadThread downloadThread = new DownloadThread(config,
							new CGSCListener(), new URL(config.getOnline()
									.getCgscUrl()));
					downloadThread.start();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class SearchCriteria<DECLARING_CLASS, JAVA_TYPE> {
		public SearchCriteria(SingularAttribute<DECLARING_CLASS, JAVA_TYPE> att) {
			this.attribute = att;
		}

		private SingularAttribute<DECLARING_CLASS, JAVA_TYPE> attribute;

		public SingularAttribute<DECLARING_CLASS, JAVA_TYPE> getAttribute() {
			return attribute;
		}

		@Override
		public String toString() {
			return getSwix().getLocalizer().getString(
					attribute.getDeclaringType().getJavaType().getSimpleName()
							+ "." + attribute.getName());
		}
	}

	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties sidAuthors = new Properties();

	static {
		InputStream is = Collection.class
				.getResourceAsStream("pictures.properties");
		try {
			sidAuthors.load(is);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	}

	/**
	 * XML based Swing support
	 */
	private SwingEngine swix, swixSearchEditor;

	protected JCheckBox autoConfiguration;
	protected JTable tuneInfoTable;
	protected JPanel photograph, searchParent;
	protected Picture picture;
	protected JTree fileBrowser;
	protected JComboBox<SearchCriteria<?, ?>> searchCriteria;
	protected JComboBox<String> searchScope, searchResult;
	protected JButton startSearch, stopSearch, resetSearch, createSearchIndex,
			browse;
	protected JTextField collectionDir;
	protected JLinkButton linkCollectionURL;

	protected JTextField textField;
	protected JCheckBox checkbox;
	protected JComboBox<Enum<?>> combo;
	protected YearTextField spinner;

	protected Player player;
	protected IConfig config;

	private EntityManager em;
	private VersionService versionService;

	protected CollectionTreeModel collectionTreeModel;
	protected final String collectionTitle, collectionURL,
			msgSelectCollectionDir, dbName;

	protected SearchThread searchThread;
	protected Object savedState;
	protected Object searchForValue, recentlySearchedForValue;
	protected int recentlySearchedCriteria;
	protected boolean searchOptionsChanged;
	protected File lastDir;
	protected IFavorites favoritesToAddSearchResult;
	protected int currentProgress;

	public Action doAutoConfiguration = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			autoConfig();
		}

	};

	public Action searchCategory = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (searchCriteria.getSelectedIndex() != recentlySearchedCriteria) {
				searchOptionsChanged = true;
				recentlySearchedCriteria = searchCriteria.getSelectedIndex();
			}
		}
	};

	public Action doStartSearch = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			startSearch(false);
		}
	};

	public Action doStopSearch = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (searchThread != null && searchThread.isAlive()) {
				searchThread.setAborted(true);
			}
		}
	};

	public Action doResetSearch = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			resetSearch();
		}
	};

	public Action doCreateSearchIndex = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(Collection.this);
			final int response = JOptionPane.showConfirmDialog(containerFrame,
					String.format(
							getSwix().getLocalizer().getString(
									"RECREATE_DATABASE"), dbName),
					getSwix().getLocalizer()
							.getString("CREATE_SEARCH_DATABASE"),
					JOptionPane.YES_NO_OPTION);
			if (response == JOptionPane.YES_OPTION) {
				startSearch(true);
			}
		}
	};

	public Action doBrowse = new AbstractAction() {

		private File fLastDir2;

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fc = new JFileChooser(fLastDir2);
			fc.setFileFilter(new CollectionFileFilter());
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(Collection.this);
			final int result = fc.showOpenDialog(containerFrame);
			if (result == JFileChooser.APPROVE_OPTION
					&& fc.getSelectedFile() != null) {
				fLastDir2 = fc.getSelectedFile().getParentFile();
				final File collectionFile = fc.getSelectedFile();
				setRootFile(collectionFile);
			}
		}
	};

	public Action gotoURL = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getUiEvents().fireEvent(IGotoURL.class, new IGotoURL() {
				@Override
				public URL getCollectionURL() {
					return linkCollectionURL.getLinkURL();
				}
			});
		}
	};

	public Action doSetYear = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setSearchValue();
			if (!searchForValue.equals(recentlySearchedForValue)) {
				searchOptionsChanged = true;
				recentlySearchedForValue = searchForValue;
			}
			startSearch(false);
		}
	};

	public Action doSetValue = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setSearchValue();
			if (!searchForValue.equals(recentlySearchedForValue)) {
				searchOptionsChanged = true;
				recentlySearchedForValue = searchForValue;
			}
			startSearch(false);
		}
	};

	public Action doSetBoolean = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setSearchValue();
			if (!searchForValue.equals(recentlySearchedForValue)) {
				searchOptionsChanged = true;
				recentlySearchedForValue = searchForValue;
			}
			startSearch(false);
		}
	};

	public Action doSetEnum = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setSearchValue();
			if (!searchForValue.equals(recentlySearchedForValue)) {
				searchOptionsChanged = true;
				recentlySearchedForValue = searchForValue;
			}
			startSearch(false);
		}
	};

	public Collection(Player pl, IConfig cfg, final String title,
			final String sourceURL, final String message, final String dbName) {
		this.player = pl;
		this.config = cfg;
		this.collectionTitle = title;
		this.collectionURL = sourceURL;
		this.msgSelectCollectionDir = message;
		this.dbName = dbName;
		createContents();
	}

	public JTree getFileBrowser() {
		return fileBrowser;
	}

	protected void createContents() {
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("tuneinfotable", TuneInfoTable.class);
			swix.getTaglib().registerTag("picture", Picture.class);
			swix.getTaglib().registerTag("nicebutton", JNiceButton.class);
			swix.getTaglib().registerTag("link", JLinkButton.class);
			swix.insert(Collection.class.getResource("Collection.xml"), this);

			swixSearchEditor = new SwingEngine(this);
			swixSearchEditor.getTaglib().registerTag("shorttextfield",
					ShortTextField.class);
			swixSearchEditor.getTaglib().registerTag("inttextfield",
					IntTextField.class);
			swixSearchEditor.getTaglib().registerTag("longtextfield",
					LongTextField.class);
			swixSearchEditor.getTaglib().registerTag("floattextfield",
					FloatTextField.class);
			swixSearchEditor.getTaglib().registerTag("chartextfield",
					CharTextField.class);
			swixSearchEditor.getTaglib().registerTag("yeartextfield",
					YearTextField.class);

			fillComboBoxes();
			setDefaultsAndActions();

			TuneInfoTableModel tuneInfoModel = (TuneInfoTableModel) tuneInfoTable
					.getModel();
			tuneInfoModel.setConfig(config);
			tuneInfoModel.setLocalizer(swix.getLocalizer());

			fileBrowser.setModel(new DefaultTreeModel(
					new DefaultMutableTreeNode(swix.getLocalizer().getString(
							this.msgSelectCollectionDir)), false));
			fileBrowser.setCellRenderer(new CollectionTreeRenderer(this,
					player, config));
			fileBrowser.getSelectionModel().setSelectionMode(
					TreeSelectionModel.SINGLE_TREE_SELECTION);

			linkCollectionURL.setText(String.format(swix.getLocalizer()
					.getString("DOWNLOAD"), collectionTitle));
			linkCollectionURL.setLinkURL(new URL(collectionURL));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		{
			searchCriteria.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent event) {
					int selectedIndex = searchCriteria.getSelectedIndex();
					if (selectedIndex != -1) {
						searchParent.removeAll();
						Class<?> fieldType = getSelectedField().getJavaType();
						String uiTypeName = getUITypeName(fieldType);
						try {
							createEditor(searchParent, uiTypeName);
							if (Enum.class.isAssignableFrom(fieldType)) {
								initEnumComboBox(fieldType);
							}
							setSearchValue();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			searchCriteria.setSelectedIndex(0);

			// Listen for when the selection changes.
			fileBrowser.addTreeSelectionListener(this);
			fileBrowser.addKeyListener(new KeyAdapter() {

				@Override
				public void keyTyped(final KeyEvent e) {
					/* handle return presses */
					if (e.getKeyChar() != '\n') {
						return;
					}

					/* find current selection */
					final TreePath path = fileBrowser.getSelectionPath();
					if (path == null) {
						return;
					}

					final File file = (File) path.getLastPathComponent();
					if (file.isFile()) {
						// play tune
						getUiEvents().fireEvent(IPlayTune.class,
								new IPlayTune() {
									@Override
									public boolean switchToVideoTab() {
										return false;
									}

									@Override
									public File getFile() {
										return file;
									}

									@Override
									public Component getComponent() {
										return Collection.this;
									}
								});
					}
				}

			});
			fileBrowser.addMouseListener(new MouseAdapter() {

				private JPopupMenu fContextPopup;

				@Override
				public void mouseClicked(final MouseEvent mouseEvent) {
					final TreePath selectionPath = fileBrowser
							.getSelectionPath();
					if (selectionPath == null
							|| !(selectionPath.getLastPathComponent() instanceof File)) {
						return;
					}
					final File tuneFile = (File) selectionPath
							.getLastPathComponent();
					if (!tuneFile.isFile()) {
						return;
					}

					if (mouseEvent.getButton() == MouseEvent.BUTTON1
							&& mouseEvent.getClickCount() > 1) {
						// play tune
						getUiEvents().fireEvent(IPlayTune.class,
								new IPlayTune() {
									@Override
									public boolean switchToVideoTab() {
										return false;
									}

									@Override
									public File getFile() {
										return tuneFile;
									}

									@Override
									public Component getComponent() {
										return Collection.this;
									}
								});
						return;
					}

					if (mouseEvent.getButton() == MouseEvent.BUTTON3
							&& mouseEvent.getClickCount() == 1) {
						fContextPopup = new JPopupMenu(getSwix().getLocalizer()
								.getString("TUNE_ACTIONS"));
						JMenuItem mi = new JMenuItem(getSwix().getLocalizer()
								.getString("SHOW_STIL"));
						fContextPopup.add(mi);
						mi.setEnabled(false);
						final STILEntry se = getSTIL(tuneFile);
						if (se != null) {
							mi.setEnabled(true);
							mi.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent arg0) {
									new applet.stil.STIL(se);
								}
							});
						}

						final JMenu addToFavorites = new JMenu(getSwix()
								.getLocalizer().getString("ADD_TO_FAVORITES"));
						getUiEvents().fireEvent(IFavoriteTabNames.class,
								new IFavoriteTabNames() {
									@Override
									public void setFavoriteTabNames(
											final String[] names,
											final String selected) {
										for (int i = 0; i < names.length; i++) {
											final int index = i;
											final String title = names[i];
											final JMenuItem tabItem = new JMenuItem(
													title);
											tabItem.addActionListener(new ActionListener() {
												@Override
												public void actionPerformed(
														final ActionEvent e) {
													getUiEvents()
															.fireEvent(
																	IGetFavorites.class,
																	new IGetFavorites() {

																		@Override
																		public int getIndex() {
																			return index;
																		}

																		@Override
																		public void setFavorites(
																				final IFavorites favorites) {
																			favorites
																					.addToFavorites(new File[] { tuneFile });
																		}
																	});
												}
											});
											addToFavorites.add(tabItem);
										}
									}
								});
						fContextPopup.add(addToFavorites);

						final JMenu convertItem = new JMenu(getSwix()
								.getLocalizer().getString("CONVERT_TO"));
						final JMenuItem psid64 = new JMenuItem(getSwix()
								.getLocalizer().getString("PSID64"));
						psid64.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(final ActionEvent e) {
								final JFileChooser fc = new JFileChooser(
										lastDir);
								fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
								final Frame containerFrame = JOptionPane
										.getFrameForComponent(Collection.this);
								final int result = fc
										.showOpenDialog(containerFrame);
								if (result == JFileChooser.APPROVE_OPTION
										&& fc.getSelectedFile() != null) {
									lastDir = fc.getSelectedFile();
									convertFile(tuneFile);
								}
							}
						});
						convertItem.add(psid64);
						fContextPopup.add(convertItem);

						fContextPopup.show((Component) mouseEvent.getSource(),
								mouseEvent.getX(), mouseEvent.getY());
					}
				}
			});
		}
	}

	private void fillComboBoxes() {
		searchScope.addItem(getSwix().getLocalizer().getString("FORWARD"));
		searchScope.addItem(getSwix().getLocalizer().getString("BACKWARD"));

		searchResult.addItem(getSwix().getLocalizer().getString(
				"SHOW_NEXT_MATCH"));
		searchResult.addItem(getSwix().getLocalizer().getString(
				"ADD_TO_A_NEW_PLAYLIST"));

		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.path));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.name));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.title));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.author));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.released));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.format));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.playerId));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.noOfSongs));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.startSong));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Clock>(
				HVSCEntry_.clockFreq));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Speed>(
				HVSCEntry_.speed));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Model>(
				HVSCEntry_.sidModel1));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Model>(
				HVSCEntry_.sidModel2));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Compatibility>(
				HVSCEntry_.compatibility));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Long>(
				HVSCEntry_.tuneLength));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.audio));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.sidChipBase1));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.sidChipBase2));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.driverAddress));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.loadAddress));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.loadLength));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.initAddress));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Integer>(
				HVSCEntry_.playerAddress));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Date>(
				HVSCEntry_.fileDate));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Long>(
				HVSCEntry_.fileSizeKb));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Long>(
				HVSCEntry_.tuneSizeB));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Short>(
				HVSCEntry_.relocStartPage));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, Short>(
				HVSCEntry_.relocNoPages));
		searchCriteria.addItem(new SearchCriteria<HVSCEntry, String>(
				HVSCEntry_.stilGlbComment));
		searchCriteria.addItem(new SearchCriteria<StilEntry, String>(
				StilEntry_.stilName));
		searchCriteria.addItem(new SearchCriteria<StilEntry, String>(
				StilEntry_.stilAuthor));
		searchCriteria.addItem(new SearchCriteria<StilEntry, String>(
				StilEntry_.stilTitle));
		searchCriteria.addItem(new SearchCriteria<StilEntry, String>(
				StilEntry_.stilArtist));
		searchCriteria.addItem(new SearchCriteria<StilEntry, String>(
				StilEntry_.stilComment));
	}

	private String getUITypeName(Class<?> fieldType) {
		if (fieldType == String.class) {
			return String.class.getSimpleName();
		} else if (fieldType == Short.class || fieldType == short.class) {
			return Short.class.getSimpleName();
		} else if (fieldType == Integer.class || fieldType == int.class) {
			return Integer.class.getSimpleName();
		} else if (fieldType == Long.class || fieldType == long.class) {
			return Long.class.getSimpleName();
		} else if (fieldType == Boolean.class || fieldType == boolean.class) {
			return Boolean.class.getSimpleName();
		} else if (Enum.class.isAssignableFrom(fieldType)) {
			return Enum.class.getSimpleName();
		} else if (fieldType == Float.class || fieldType == float.class) {
			return Float.class.getSimpleName();
		} else if (fieldType == Character.class || fieldType == char.class) {
			return Character.class.getSimpleName();
		} else if (fieldType == File.class) {
			return File.class.getSimpleName();
		} else if (fieldType == Date.class) {
			return "Year";
		} else {
			throw new RuntimeException("unsupported type: "
					+ fieldType.getSimpleName());
		}
	}

	private void createEditor(JComponent parent, String uiTypeName)
			throws Exception {
		JComponent editor = (JComponent) swixSearchEditor
				.render(ConfigView.class.getResource("editors/" + uiTypeName
						+ ".xml"));
		parent.add(editor, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JComponent initEnumComboBox(Class<?> cls) {
		ActionListener[] actionListeners = combo.getActionListeners();
		for (ActionListener actionListener : actionListeners) {
			combo.removeActionListener(actionListener);
		}
		Class<? extends Enum> en = (Class<? extends Enum>) cls;
		for (Enum val : en.getEnumConstants()) {
			combo.addItem(val);
		}
		for (ActionListener actionListener : actionListeners) {
			combo.addActionListener(actionListener);
		}
		return combo;
	}

	private void setSearchValue() {
		Class<?> type = getSelectedField().getJavaType();
		if (type == Character.class) {
			String text = textField.getText();
			searchForValue = text.length() > 0 ? text.charAt(0) : 0;
		} else if (type == Float.class) {
			String text = textField.getText();
			try {
				searchForValue = Float.parseFloat(text);
			} catch (NumberFormatException e1) {
				searchForValue = Float.valueOf(0);
			}
		} else if (type == Integer.class) {
			String text = textField.getText();
			try {
				searchForValue = Integer.parseInt(text);
			} catch (NumberFormatException e1) {
				searchForValue = Integer.valueOf(0);
			}
		} else if (type == Long.class) {
			String text = textField.getText();
			try {
				searchForValue = Long.parseLong(text);
			} catch (NumberFormatException e1) {
				searchForValue = Long.valueOf(0);
			}
		} else if (type == Short.class) {
			String text = textField.getText();
			try {
				searchForValue = Short.parseShort(text);
			} catch (NumberFormatException e1) {
				searchForValue = Short.valueOf((short) 0);
			}
		} else if (Enum.class.isAssignableFrom(type)) {
			searchForValue = combo.getSelectedItem();
		} else if (type == Boolean.class) {
			searchForValue = checkbox.isSelected();
		} else if (type == Date.class) {
			Date date = (Date) spinner.getValue();
			searchForValue = date;
		} else if (type == String.class) {
			String text = textField.getText();
			searchForValue = text;
		}
	}

	private SingularAttribute<?, ?> getSelectedField() {
		return ((SearchCriteria<?, ?>) searchCriteria.getSelectedItem())
				.getAttribute();
	}

	protected void setRootFile(final File rootFile) {
		if (rootFile.exists()) {
			em = Persistence.createEntityManagerFactory(
					PersistenceProperties.COLLECTION_DS,
					new PersistenceProperties(new File(
							rootFile.getParentFile(), dbName)))
					.createEntityManager();

			versionService = new VersionService(em);

			collectionDir.setText(rootFile.getAbsolutePath());
			fileBrowser.setModel(collectionTreeModel = new CollectionTreeModel(
					rootFile));
			if (((File) collectionTreeModel.getRoot()).exists()) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						fileBrowser.setSelectionPath(new TreePath(
								collectionTreeModel.getRoot()));
						showPhoto();
						updateUI();
					}

				});
			}

			setRootDir(rootFile);
		}
		resetSearch();
	}

	protected abstract void setRootDir(final File collectionFile);

	protected abstract void autoConfig();

	/**
	 * On tree selection display tune info in the table
	 */
	@Override
	public void valueChanged(final TreeSelectionEvent treeselectionevent) {
		final TreePath treePath = treeselectionevent.getNewLeadSelectionPath();
		if (treePath == null) {
			return;
		}
		final Object pathComponent = treePath.getLastPathComponent();
		if (pathComponent instanceof File) {
			final File file = (File) pathComponent;
			if (file.isFile()) {
				TuneInfoTableModel tuneInfoModel = (TuneInfoTableModel) tuneInfoTable
						.getModel();
				tuneInfoModel.setFile(file);
				showPhoto();
				tuneInfoTable.updateUI();
			}
		}
	}

	protected void showPhoto() {
		TuneInfoTableModel tuneInfoModel = (TuneInfoTableModel) tuneInfoTable
				.getModel();
		String authorInfo = tuneInfoModel.getAuthor();
		if (authorInfo == null) {
			// author not available
			return;
		}
		String photoRes = sidAuthors.getProperty(authorInfo);

		picture.setComposerImage(null);

		if (photoRes != null) {
			photoRes = "Photos/" + photoRes;
			final URL resource = JSIDPlay2.class.getResource(photoRes);
			picture.setComposerImage(new ImageIcon(resource).getImage());
		}
		photograph.repaint();
	}

	@Override
	public void searchStart() {
		startSearch.setEnabled(false);
		stopSearch.setEnabled(true);
		resetSearch.setEnabled(false);
		createSearchIndex.setEnabled(false);
	}

	@Override
	public void searchStop(final boolean canceled) {
		startSearch.setEnabled(true);
		stopSearch.setEnabled(false);
		resetSearch.setEnabled(true);
		createSearchIndex.setEnabled(true);

		// remember search state
		savedState = searchThread.getSearchState();
		getUiEvents().fireEvent(IMadeProgress.class, new IMadeProgress() {

			@Override
			public int getPercentage() {
				return 100;
			}
		});
	}

	@Override
	public void searchHit(final File current) {
		if (searchThread instanceof SearchIndexerThread) {
			// if search index is created, do not show the next result
			getUiEvents().fireEvent(IMadeProgress.class, new IMadeProgress() {

				@Override
				public int getPercentage() {
					return ++currentProgress % 100;
				}
			});
			return;
		}
		switch (searchResult.getSelectedIndex()) {
		case 0:
			searchThread.setAborted(true);
			searchStop(true);

			showNextHit(current);
			break;

		case 1:
			favoritesToAddSearchResult.addToFavorites(new File[] { current });
			break;

		default:
			break;
		}

	}

	private void showNextHit(final File matchFile) {
		try {
			if (fileBrowser.getModel() == collectionTreeModel) {
				final String path = matchFile.getPath().replace('\\', '/');
				ArrayList<File> pathSegs = collectionTreeModel.getFile(path
						.substring(path.indexOf('/') + 1));
				final TreePath treePath = new TreePath(
						pathSegs.toArray(new File[pathSegs.size()]));
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						fileBrowser.setSelectionPath(treePath);
						fileBrowser.scrollPathToVisible(fileBrowser
								.getSelectionPath());
						showPhoto();
						updateUI();
					}

				});
			}
		} catch (final Exception e) {
			// ignore, if the next file could not be determined
		}
	}

	protected void startSearch(boolean forceRecreate) {
		if (searchThread != null && searchThread.isAlive()) {
			return;
		}

		if (!new File(collectionDir.getText()).exists()) {
			return;
		}

		currentProgress = 0;

		/*
		 * validate database: version is inserted only after successful create
		 * completes.
		 */
		if (!forceRecreate) {
			if (!versionService.isExpectedVersion()) {
				forceRecreate = true;
			}
		}

		if (forceRecreate) {
			final File root = (File) collectionTreeModel.getRoot();
			searchThread = new SearchIndexerThread(root);
			searchThread.addSearchListener(this);
			searchThread.addSearchListener(new SearchIndexCreator(
					(File) collectionTreeModel.getRoot(), config, em));

			searchThread.start();
		} else {
			switch (searchResult.getSelectedIndex()) {
			case 1:
				// Add result to favorites?
				// Create new favorites tab
				getUiEvents().fireEvent(IAddFavoritesTab.class,
						new IAddFavoritesTab() {

							@Override
							public String getTitle() {
								return getSwix().getLocalizer().getString(
										"NEW_TAB");
							}

							@Override
							public void setFavorites(final IFavorites favorites) {
								favoritesToAddSearchResult = favorites;
							}
						});
				break;

			default:
				break;
			}

			final SearchInIndexThread t = new SearchInIndexThread(
					collectionTreeModel, em,
					searchScope.getSelectedIndex() != 1);
			t.addSearchListener(this);
			t.setField(getSelectedField());
			t.setFieldValue(searchForValue);
			t.setCaseSensitive(false);
			if (searchOptionsChanged) {
				resetSearch();
				searchOptionsChanged = false;
			}
			searchThread = t;
			searchThread.setSearchState(savedState);
			searchThread.start();
		}

	}

	protected void resetSearch() {
		savedState = null;
	}

	protected void convertFile(final File tuneFile) {
		SidTuneConverter c = new SidTuneConverter(config);
		c.convertFiles(new File[] { tuneFile }, lastDir);
	}

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
	public void setTune(final Player m_engine, final SidTune m_tune) {
		if (m_tune == null) {
			return;
		}
		// auto-expand current selected tune
		showNextHit(m_tune.getInfo().file);
	}

	public SwingEngine getSwix() {
		return swix;
	}
}
