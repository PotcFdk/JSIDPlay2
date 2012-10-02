package applet.collection;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;

import org.swixml.SwingEngine;

import sidplay.ini.IniConfig;
import applet.JSIDPlay2;
import applet.PathUtils;
import applet.SidTuneConverter;
import applet.TuneTab;
import applet.collection.search.ISearchListener;
import applet.collection.search.SearchInIndexThread;
import applet.collection.search.SearchIndexCreator;
import applet.collection.search.SearchIndexerThread;
import applet.collection.search.SearchThread;
import applet.entities.service.VersionService;
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
import applet.sidtuneinfo.SidTuneInfoCache;
import applet.soundsettings.DownloadThread;
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

		/**
		 * HVSC download part number.
		 * 
		 * Note: HVSC download is split into several parts due to file size
		 * restrictions.
		 */
		private int part;

		/**
		 * @param part
		 *            HVSC download part number (1..2).
		 */
		public HVSCListener(final int part) {
			this.part = part;
		}

		@Override
		public void downloaded(final File downloadedFile) {
			if (part == 1) {
				// part 1 has been downloaded, start download of part 2
				DownloadThread downloadThread = new DownloadThread(config,
						new HVSCListener(2), JSIDPlay2.DEPLOYMENT_URL
								+ "hvsc/C64Music.002");
				downloadThread.start();
			} else {
				// part 1 and 2 has been downloaded, merge them
				autoConfiguration.setEnabled(true);
				File part1File = new File(
						System.getProperty("jsidplay2.tmpdir"), "C64Music.001");
				File part2File = new File(
						System.getProperty("jsidplay2.tmpdir"), "C64Music.002");
				File hvscFile = new File(
						System.getProperty("jsidplay2.tmpdir"), "C64Music.zip");
				BufferedInputStream is = null;
				BufferedOutputStream os = null;
				try {
					is = new BufferedInputStream(new SequenceInputStream(
							new FileInputStream(part1File),
							new FileInputStream(part2File)));
					os = new BufferedOutputStream(
							new FileOutputStream(hvscFile));
					int bytesRead;
					byte[] buffer = new byte[DownloadThread.MAX_BUFFER_SIZE];
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (os != null) {
						try {
							os.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				part1File.delete();
				part2File.delete();
				setRootFile(hvscFile);
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
			// ZIP has been downloaded
			autoConfiguration.setEnabled(true);
			setRootFile(new File(System.getProperty("jsidplay2.tmpdir"),
					"CGSC.zip"));
		}
	}

	@SuppressWarnings("serial")
	public static class HVSC extends Collection {
		public HVSC(final Player pl, final IniConfig cfg) {
			super(pl, cfg, "High Voltage SID Collection",
					"http://www.hvsc.de/", "PLEASE_SELECT_HVSC", "HVSC");
		}

		@Override
		public void createContents() {
			super.createContents();
			// Initially configure HVSC collection
			if (config.sidplay2().getHvsc() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.sidplay2().getHvsc());
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
				config.sidplay2().setHvsc(PathUtils.getPath(rootFile));
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

		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.HVSC
						&& !collectionDir.getText().equals(
								ifObj.getCollectionRoot())) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected void autoConfig() {
			if (autoConfiguration.isSelected()) {
				final File hvscFile = new File(
						System.getProperty("jsidplay2.tmpdir"), "C64Music.zip");
				if (hvscFile.exists()) {
					setRootFile(hvscFile);
				} else {
					// First time, the HVSC is downloaded, download part 1
					autoConfiguration.setEnabled(false);
					DownloadThread downloadThread = new DownloadThread(config,
							new HVSCListener(1), JSIDPlay2.DEPLOYMENT_URL
									+ "online/hvsc/C64Music.001");
					downloadThread.start();
				}
			}
		}
	}

	@SuppressWarnings("serial")
	public static class CGSC extends Collection {
		public CGSC(final Player pl, final IniConfig cfg) {
			super(pl, cfg, "Compute's Gazette Sid Collection",
					"http://www.btinternet.com/~pweighill/music/",
					"PLEASE_SELECT_CGSC", "CGSC");
		}

		@Override
		public void createContents() {
			super.createContents();
			// Initially configure CGSC collection
			if (config.sidplay2().getCgsc() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.sidplay2().getCgsc());
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
				config.sidplay2().setCgsc(PathUtils.getPath(rootFile));
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

		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.CGSC
						&& !collectionDir.getText().equals(
								ifObj.getCollectionRoot())) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected void autoConfig() {
			if (autoConfiguration.isSelected()) {
				final File cgscFile = new File(
						System.getProperty("jsidplay2.tmpdir"), "CGSC.zip");
				if (cgscFile.exists()) {
					setRootFile(cgscFile);
				} else {
					// First time, the CGSC is downloaded
					autoConfiguration.setEnabled(false);
					DownloadThread downloadThread = new DownloadThread(config,
							new CGSCListener(), JSIDPlay2.DEPLOYMENT_URL
									+ "online/cgsc/CGSC.zip");
					downloadThread.start();
				}
			}
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
	private SwingEngine swix;

	protected JCheckBox autoConfiguration;
	protected JTable tuneInfoTable;
	protected JPanel photograph;
	protected Picture picture;
	protected JTree fileBrowser;
	protected JComboBox searchCriteria, searchScope, searchResult;
	protected JButton startSearch, stopSearch, resetSearch, createSearchIndex,
			browse;
	protected JTextField searchFor, collectionDir;
	protected JLinkButton linkCollectionURL;

	protected Player player;
	protected IniConfig config;

	protected CollectionTreeModel collectionTreeModel;
	protected final String collectionTitle, collectionURL,
			msgSelectCollectionDir, dbName;
	// private Connection dbConnection;
	protected SearchThread searchThread;
	protected Object savedState;
	protected String recentlySearchedText;
	protected int recentlySearchedCriteria;
	protected boolean searchOptionsChanged;
	protected File lastDir;
	protected IFavorites favoritesToAddSearchResult;
	protected int currentProgress;

	private SidTuneInfoCache cache;

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

	public Action searchText = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!searchFor.getText().equals(recentlySearchedText)) {
				searchOptionsChanged = true;
				recentlySearchedText = searchFor.getText();
			}
			startSearch(false);
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

	private EntityManagerFactory emf;
	private EntityManager em;
	VersionService versionService;

	public Collection(Player pl, IniConfig cfg, final String title,
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

			cache = new SidTuneInfoCache(config);
			fillComboBoxes();
			setDefaultsAndActions();

			TuneInfoTableModel tuneInfoModel = (TuneInfoTableModel) tuneInfoTable
					.getModel();
			tuneInfoModel.setSidTuneInfoCache(cache);

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
								public void actionPerformed(ActionEvent arg0) {
									new applet.collection.stil.STIL(se);
								}
							});
						}

						final JMenu addToFavorites = new JMenu(getSwix()
								.getLocalizer().getString("ADD_TO_FAVORITES"));
						getUiEvents().fireEvent(IFavoriteTabNames.class,
								new IFavoriteTabNames() {
									public void setFavoriteTabNames(
											final String[] names,
											final String selected) {
										for (int i = 0; i < names.length; i++) {
											final int index = i;
											final String title = names[i];
											final JMenuItem tabItem = new JMenuItem(
													title);
											tabItem.addActionListener(new ActionListener() {
												public void actionPerformed(
														final ActionEvent e) {
													getUiEvents()
															.fireEvent(
																	IGetFavorites.class,
																	new IGetFavorites() {

																		public int getIndex() {
																			return index;
																		}

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

		int criteriaCount = 2 + SidTuneInfoCache.SIDTUNE_INFOS.length
				+ STIL.STIL_INFOS.length;
		for (int i = 0; i < criteriaCount; i++) {
			if (i < 2) {
				searchCriteria.addItem(i == 0 ? getSwix().getLocalizer()
						.getString("FILE_NAME") : getSwix().getLocalizer()
						.getString("FULL_PATH"));
			} else if (i - 2 < SidTuneInfoCache.SIDTUNE_INFOS.length) {
				searchCriteria.addItem(cache.getLocalizer().getString(
						SidTuneInfoCache.SIDTUNE_INFOS[i - 2]));
			} else {
				searchCriteria.addItem(getSwix().getLocalizer().getString(
						STIL.STIL_INFOS[i - 2
								- SidTuneInfoCache.SIDTUNE_INFOS.length]));
			}
		}
	}

	protected void setRootFile(final File rootFile) {
		if (rootFile.exists()
				&& !collectionDir.getText().equals(rootFile.getAbsolutePath())) {
			collectionDir.setText(rootFile.getAbsolutePath());
			fileBrowser.setModel(collectionTreeModel = new CollectionTreeModel(
					rootFile));
			if (((File) collectionTreeModel.getRoot()).exists()) {
				fileBrowser.setSelectionPath(new TreePath(collectionTreeModel
						.getRoot()));
			}

			final File dbFile = new File(rootFile.getParentFile(), dbName);
			String jdbcURL = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath() + ";shutdown=true";

			Map<String,String> properties = new HashMap<String,String>();
			properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
			properties.put("hibernate.connection.url", jdbcURL);
			properties.put("hibernate.connection.username", "");
			properties.put("hibernate.connection.password", "");
			properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			properties.put("hibernate.hbm2ddl.auto", "update");
			emf = Persistence.createEntityManagerFactory("hsqldb-ds",properties);
			em = (EntityManager) emf.createEntityManager();
			versionService = new VersionService(em);
			setRootDir(rootFile);
		}
		resetSearch();
	}

	protected abstract void setRootDir(final File collectionFile);

	protected abstract void autoConfig();

	/**
	 * On tree selection display tune info in the table
	 */
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
		Object authorInfo = tuneInfoModel.getValueAt(
				SidTuneInfoCache.INFO_AUTHOR, 1);
		if (authorInfo == null) {
			// author not available
			return;
		}
		String photoRes = sidAuthors.getProperty(authorInfo.toString());

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
				fileBrowser.setSelectionPath(treePath);
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						fileBrowser.scrollPathToVisible(fileBrowser
								.getSelectionPath());
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

							public String getTitle() {
								return getSwix().getLocalizer().getString(
										"NEW_TAB");
							}

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
			t.setField(searchCriteria.getSelectedIndex());
			t.setFieldValue(searchFor.getText());
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
		final String name = config.getHVSCName(file);
		if (null != name) {
			libsidutils.STIL stil = libsidutils.STIL.getInstance(config
					.sidplay2().getHvsc());
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
