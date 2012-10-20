package applet.demos;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.PathUtils;
import applet.TuneTab;
import applet.download.DownloadThread;
import applet.download.IDownloadListener;
import applet.events.ICollectionChanged;
import applet.events.ICollectionChanged.CollectionType;
import applet.events.IInsertMedia;
import applet.events.Reset;
import applet.events.UIEvent;
import applet.filechooser.ImagePreview;
import applet.filefilter.DemosFileFilter;
import applet.filefilter.DiskFileFilter;
import applet.gamebase.listeners.GameListener;
import applet.gamebase.listeners.ProgressListener;

public abstract class DiskCollection extends TuneTab implements
		PropertyChangeListener {
	public static final class HVMEC extends DiskCollection {

		public HVMEC(final Player pl, final IConfig cfg) {
			super(pl, cfg, JSIDPlay2.DEPLOYMENT_URL + "online/hvmec/HVMEC",
					"HVMEC");
			// Initially configure Demos
			if (config.getSidplay2().getHVMEC() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.getSidplay2().getHVMEC());
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.HVMEC;
							}
						});
			}
		}

		@Override
		protected void setRootDir(final File rootFile) {
			if (rootFile.exists()) {
				config.getSidplay2().setHVMEC(PathUtils.getPath(rootFile));
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return rootFile;
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.HVMEC;
							}
						});
			}
		}

		@Override
		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.HVMEC) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected String getIconPath(File file) {
			return file.getParentFile().getPath()
					.replace("/DATA/", "/CONTROL/");
		}

	}

	public static final class Demos extends DiskCollection {

		public Demos(final Player pl, final IConfig cfg) {
			super(pl, cfg, JSIDPlay2.DEPLOYMENT_URL + "online/demos/Demos",
					"Demos");
			// Initially configure Demos
			if (config.getSidplay2().getDemos() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.getSidplay2().getDemos());
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.DEMOS;
							}
						});
			}
		}

		@Override
		protected void setRootDir(final File rootFile) {
			if (rootFile.exists()) {
				config.getSidplay2().setDemos(PathUtils.getPath(rootFile));
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return rootFile;
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.DEMOS;
							}
						});
			}
		}

		@Override
		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.DEMOS) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected String getIconPath(File file) {
			return file.getParentFile().getPath();
		}

	}

	public static final class Mags extends DiskCollection {

		public Mags(final Player pl, final IConfig cfg) {
			super(pl, cfg, JSIDPlay2.DEPLOYMENT_URL
					+ "online/mags/C64Magazines", "C64Magazines");
			// Initially configure Mags
			if (config.getSidplay2().getMags() != null) {
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return new File(config.getSidplay2().getMags());
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.MAGS;
							}
						});
			}
		}

		@Override
		protected void setRootDir(final File rootFile) {
			if (rootFile.exists()) {
				config.getSidplay2().setMags(PathUtils.getPath(rootFile));
				getUiEvents().fireEvent(ICollectionChanged.class,
						new ICollectionChanged() {

							@Override
							public File getCollectionRoot() {
								return rootFile;
							}

							@Override
							public CollectionType getColectionType() {
								return CollectionType.MAGS;
							}
						});
			}
		}

		@Override
		public void notify(UIEvent event) {
			if (event.isOfType(ICollectionChanged.class)) {
				ICollectionChanged ifObj = (ICollectionChanged) event
						.getUIEventImpl();
				if (ifObj.getColectionType() == CollectionType.MAGS) {
					setRootFile(ifObj.getCollectionRoot());
				}
			}
		}

		@Override
		protected String getIconPath(File file) {
			return file.getParentFile().getPath();
		}

	}

	protected String downloadUrl;
	protected String zipName;

	protected SwingEngine swix;

	protected IConfig config;
	protected Player player;

	protected JSplitPane verticalSplit, horizontalSplit;
	protected ImagePreview imgPreview;
	protected JLabel photograph;

	protected JTree fileBrowser;
	protected JTextField collectionDir;
	protected JCheckBox autoConfiguration;

	protected IDownloadListener gameListener;

	public class DemosListener extends ProgressListener {

		@Override
		public void downloaded(File downloadedFile) {
			autoConfiguration.setEnabled(true);
			if (downloadedFile != null) {
				setRootFile(downloadedFile);
			}
		}
	}

	public Action doAutoConfiguration = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (autoConfiguration.isSelected()) {
				autoConfiguration.setEnabled(false);
				try {
					DownloadThread downloadThread = new DownloadThread(config,
							new DemosListener(), new URL(downloadUrl + ".zip"));
					downloadThread.start();
				} catch (MalformedURLException e2) {
					e2.printStackTrace();
				}
			}
		}
	};

	public Action doBrowse = new AbstractAction() {

		private File fLastDir2;

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fc = new JFileChooser(fLastDir2);
			fc.setFileFilter(new DemosFileFilter());
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(DiskCollection.this);
			final int result = fc.showOpenDialog(containerFrame);
			if (result == JFileChooser.APPROVE_OPTION
					&& fc.getSelectedFile() != null) {
				fLastDir2 = fc.getSelectedFile().getParentFile();
				final File demosFile = fc.getSelectedFile();
				setRootFile(demosFile);
			}
		}
	};

	public DiskCollection(Player pl, IConfig cfg, String downloadUrl,
			String zipName) {
		this.player = pl;
		this.config = cfg;
		this.downloadUrl = downloadUrl;
		this.zipName = zipName;
		gameListener = new GameListener(this, pl, cfg);
		createContents();
	}

	private void createContents() {
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("directory", ImagePreview.class);
			swix.insert(DiskCollection.class.getResource("DiskCollection.xml"),
					this);

			setDefaultsAndActions();

			imgPreview.setConfig(config);
			imgPreview.addPropertyChangeListener(this);

			fileBrowser.getSelectionModel().setSelectionMode(
					TreeSelectionModel.SINGLE_TREE_SELECTION);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		fileBrowser.getSelectionModel().addTreeSelectionListener(
				new TreeSelectionListener() {

					@Override
					public void valueChanged(TreeSelectionEvent e) {
						if (fileBrowser.getSelectionCount() != 1) {
							return;
						}
						final File file = (File) fileBrowser.getSelectionPath()
								.getLastPathComponent();
						if (file instanceof ZipFileProxy || file.isDirectory()) {
							return;
						}
						showPhoto(file);
						// Show directory
						imgPreview.propertyChange(new PropertyChangeEvent(this,
								JFileChooser.SELECTED_FILE_CHANGED_PROPERTY,
								null, file));
					}
				});
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
					attachAndRunDemo((File) fileBrowser.getSelectionPath()
							.getLastPathComponent(), null);
				}
			}

		});
		fileBrowser.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent mouseEvent) {
				final TreePath selectionPath = fileBrowser.getSelectionPath();
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
					attachAndRunDemo((File) fileBrowser.getSelectionPath()
							.getLastPathComponent(), null);
				}
			}
		});
	}

	protected void setRootFile(final File rootFile) {
		if (rootFile.exists()) {
			collectionDir.setText(rootFile.getAbsolutePath());
			fileBrowser.setModel(new DiskCollectionTreeModel(rootFile));
			fileBrowser.setCellRenderer(new DiskTreeRenderer());
			fileBrowser.setSelectionPath(new TreePath(fileBrowser.getModel()
					.getRoot()));
			setRootDir(rootFile);
		}
	}

	protected abstract void setRootDir(File rootFile);

	private DiskFileFilter diskfileFilter = new DiskFileFilter();

	protected void attachAndRunDemo(File selectedFile, final File autoStartFile) {
		if (selectedFile.getName().toLowerCase().endsWith(".pdf")) {
			openPDF(selectedFile);
		} else {
			if (diskfileFilter.accept(selectedFile)) {
				insertDisk(selectedFile, autoStartFile);
			} else {
				insertTape(selectedFile, autoStartFile);
			}
			if (autoStartFile == null) {
				resetAndLoadDemo(selectedFile);
			}
		}
	}

	private void openPDF(final File selectedFile) {
		try {
			File pdfFile = selectedFile;
			if (pdfFile instanceof ZipEntryFileProxy) {
				// Extract ZIP file
				pdfFile = ZipEntryFileProxy
						.extractFromZip((ZipEntryFileProxy) selectedFile);
			}
			if (pdfFile.getName().endsWith(".gz")) {
				// Extract GZ file
				pdfFile = ZipEntryFileProxy.extractFromGZ(pdfFile);
			}
			if (pdfFile.exists()) {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(pdfFile);
				} else {
					System.out.println("Awt Desktop is not supported!");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void insertDisk(final File selectedFile, final File autoStartFile) {
		getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

			@Override
			public MediaType getMediaType() {
				return MediaType.DISK;
			}

			@Override
			public File getSelectedMedia() {
				return selectedFile;
			}

			@Override
			public File getAutostartFile() {
				return autoStartFile;
			}

			@Override
			public Component getComponent() {
				return DiskCollection.this;
			}
		});
	}

	private void insertTape(final File selectedFile, final File autoStartFile) {
		getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

			@Override
			public MediaType getMediaType() {
				return MediaType.TAPE;
			}

			@Override
			public File getSelectedMedia() {
				return selectedFile;
			}

			@Override
			public File getAutostartFile() {
				return autoStartFile;
			}

			@Override
			public Component getComponent() {
				return DiskCollection.this;
			}
		});
	}

	private void resetAndLoadDemo(final File selectedFile) {
		final String command;
		if (diskfileFilter.accept(selectedFile)) {
			// load from disk
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			// load from tape
			command = "LOAD\rRUN\r";
		}
		// reset required after inserting the cartridge
		getUiEvents().fireEvent(Reset.class, new Reset() {

			@Override
			public boolean switchToVideoTab() {
				return true;
			}

			@Override
			public String getCommand() {
				return command;
			}

			@Override
			public Component getComponent() {
				return DiskCollection.this;
			}
		});
	}

	protected void showPhoto(final File file) {
		final ImageIcon picture = getPicture(file);
		if (picture != null) {
			photograph.setIcon(picture);
			horizontalSplit.setDividerLocation(picture.getIconWidth() + 20);
			verticalSplit.setDividerLocation(verticalSplit
					.getMaximumDividerLocation());
		}
	}

	protected ImageIcon getPicture(final File file) {
		final File[] scrnshts = getScreenshots((File) fileBrowser.getModel()
				.getRoot(), file);
		if (scrnshts.length > 0) {
			// XXX For the moment just show the first!
			File theScreenShot = scrnshts[0];
			InputStream is = null;
			try {
				if (theScreenShot instanceof ZipEntryFileProxy) {
					// Extract ZIP file
					theScreenShot = ZipEntryFileProxy
							.extractFromZip((ZipEntryFileProxy) theScreenShot);
				}
				if (theScreenShot.getName().endsWith(".gz")) {
					// Extract GZ file
					theScreenShot = ZipEntryFileProxy
							.extractFromGZ(theScreenShot);
				}
				is = new FileInputStream(theScreenShot);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				byte[] bytes = new byte[(int) theScreenShot.length()];
				int readBytes = 0, complete = 0;
				do {
					readBytes = is.read(bytes, complete, bytes.length
							- complete);
					if (readBytes > 0) {
						os.write(bytes, complete, readBytes);
						complete += readBytes;
					}
				} while (readBytes > 0);
				return new ImageIcon(os.toByteArray());
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
			}
		}
		return null;
	}

	protected File[] getScreenshots(File root, File file) {
		final ArrayList<File> scrnShts = new ArrayList<File>();
		final String path = getIconPath(file);
		final File[] photoFiles;
		if (root.getName().toLowerCase().endsWith(".zip")) {
			ZipFileProxy rootProxy = (ZipFileProxy) root;
			photoFiles = rootProxy.getFileChildren(path);
		} else {
			File photoDir = new File(path);
			photoFiles = photoDir.listFiles();
		}
		for (File photoFile : photoFiles) {
			if (photoFile.getName().endsWith(".png")
					|| photoFile.getName().endsWith(".gif")) {
				scrnShts.add(photoFile);
			}
		}
		return scrnShts.toArray(new File[scrnShts.size()]);
	}

	protected abstract String getIconPath(File file);

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (ImagePreview.PROP_ATTACH_IMAGE.equals(event.getPropertyName())) {
			attachAndRunDemo((File) fileBrowser.getSelectionPath()
					.getLastPathComponent(), null);
		} else if (ImagePreview.PROP_AUTOSTART_PRG.equals(event
				.getPropertyName())) {
			attachAndRunDemo((File) fileBrowser.getSelectionPath()
					.getLastPathComponent(), (File) event.getNewValue());
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
	}

}
