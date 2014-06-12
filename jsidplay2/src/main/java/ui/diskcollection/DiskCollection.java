package ui.diskcollection;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import libsidplay.Player;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.directory.Directory;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.DocsFileFilter;
import ui.filefilter.ScreenshotFileFilter;
import ui.filefilter.TapeFileFilter;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

public class DiskCollection extends Tab implements UIPart {

	public static final String MAGS_ID = "MAGS";
	public static final String DEMOS_ID = "DEMOS";
	public static final String HVMEC_ID = "HVMEC";
	private static final String HVMEC_DATA = "DATA";
	private static final String HVMEC_CONTROL = "CONTROL";

	@FXML
	private CheckBox autoConfiguration;
	@FXML
	private Directory directory;
	@FXML
	private ImageView screenshot;
	@FXML
	private TreeView<File> fileBrowser;
	@FXML
	private ContextMenu contextMenu;
	@FXML
	private MenuItem start, attachDisk;
	@FXML
	private TextField collectionDir;

	private UIUtil util;

	private ObjectProperty<DiskCollectionType> type;

	public DiskCollectionType getType() {
		return type.get();
	}

	public void setType(DiskCollectionType type) {
		switch (type) {
		case HVMEC:
			setId(HVMEC_ID);
			setText(util.getBundle().getString("HVMEC"));
			break;
		case DEMOS:
			setId(DEMOS_ID);
			setText(util.getBundle().getString("DEMOS"));
			break;
		case MAGS:
			setId(MAGS_ID);
			setText(util.getBundle().getString("MAGS"));
			break;

		default:
			break;
		}
		this.type.set(type);
	}

	private String downloadUrl;

	private final FileFilter screenshotsFileFilter = new ScreenshotFileFilter();
	protected final FileFilter diskFileFilter = new DiskFileFilter();
	private final FileFilter fileBrowserFileFilter = new FileFilter() {

		private final TapeFileFilter tapeFileFilter = new TapeFileFilter();
		private final DocsFileFilter docsFileFilter = new DocsFileFilter();

		@Override
		public boolean accept(File file) {
			if (type.get() == DiskCollectionType.HVMEC && file.isDirectory()
					&& file.getName().equals(HVMEC_CONTROL)) {
				return false;
			}
			return diskFileFilter.accept(file) || tapeFileFilter.accept(file)
					|| docsFileFilter.accept(file);
		}
	};

	public DiskCollection(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	@FXML
	private void initialize() {
		directory.getAutoStartFileProperty().addListener(
				(observable, oldValue, newValue) -> {
					attachAndRunDemo(fileBrowser.getSelectionModel()
							.getSelectedItem().getValue(), newValue);
				});
		fileBrowser
				.getSelectionModel()
				.selectedItemProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							if (newValue != null
									&& newValue.getValue().isFile()) {
								File file = newValue.getValue();
								showScreenshot(file);
								try {
									directory.loadPreview(extract(file));
								} catch (Exception e) {
									System.err.println(String.format(
											"Cannot insert media file '%s'.",
											file.getAbsolutePath()));
								}
							}
						});
		fileBrowser.setOnKeyPressed((event) -> {
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
				if (selectedItem.getValue().isFile()) {
					File file = selectedItem.getValue();
					if (file.isFile()) {
						attachAndRunDemo(selectedItem.getValue(), null);
					}
				}
			}
		});
		fileBrowser
				.setOnMousePressed((event) -> {
					final TreeItem<File> selectedItem = fileBrowser
							.getSelectionModel().getSelectedItem();
					if (selectedItem != null
							&& selectedItem.getValue().isFile()
							&& event.isPrimaryButtonDown()
							&& event.getClickCount() > 1) {
						attachAndRunDemo(fileBrowser.getSelectionModel()
								.getSelectedItem().getValue(), null);
					}
				});
		contextMenu.setOnShown((event) -> {
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			boolean disable = selectedItem == null
					|| !selectedItem.getValue().isFile();
			start.setDisable(disable);
			attachDisk.setDisable(disable);
		});
		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				String initialRoot;
				switch (type.get()) {
				case HVMEC:
					this.downloadUrl = util.getConfig().getOnline()
							.getHvmecUrl();
					initialRoot = util.getConfig().getSidplay2().getHVMEC();
					break;

				case DEMOS:
					this.downloadUrl = util.getConfig().getOnline()
							.getDemosUrl();
					initialRoot = util.getConfig().getSidplay2().getDemos();
					break;

				case MAGS:
					this.downloadUrl = util.getConfig().getOnline()
							.getMagazinesUrl();
					initialRoot = util.getConfig().getSidplay2().getMags();
					break;

				default:
					throw new RuntimeException(
							"Illegal disk collection type : " + type);
				}
				if (initialRoot != null) {
					setRootFile(new File(initialRoot));
				}
			});
		});
	}

	@FXML
	private void doAutoConfiguration() {
		if (autoConfiguration.isSelected()) {
			autoConfiguration.setDisable(true);
			try {
				DownloadThread downloadThread = new DownloadThread(
						util.getConfig(), new ProgressListener(util,
								fileBrowser) {

							@Override
							public void downloaded(final File downloadedFile) {
								Platform.runLater(() -> {
									autoConfiguration.setDisable(false);
									if (downloadedFile != null) {
										setRootFile(downloadedFile);
									}
								});
							}
						}, new URL(downloadUrl));
				downloadThread.start();
			} catch (MalformedURLException e2) {
				e2.printStackTrace();
			}
		}
	}

	@FXML
	private void attachDisk() {
		File file = fileBrowser.getSelectionModel().getSelectedItem()
				.getValue();
		try {
			File extractedFile = extract(file);
			util.getPlayer()
					.getConfig()
					.getSidplay2()
					.setLastDirectory(
							extractedFile.getParentFile().getAbsolutePath());
			util.getPlayer().insertDisk(extractedFile, null);
		} catch (IOException | SidTuneError e) {
			System.err.println(String.format("Cannot insert media file '%s'.",
					file.getAbsolutePath()));
		}
	}

	@FXML
	private void start() {
		attachAndRunDemo(fileBrowser.getSelectionModel().getSelectedItem()
				.getValue(), null);
	}

	@FXML
	private void doBrowse() {
		DirectoryChooser fileDialog = new DirectoryChooser();
		SidPlay2Section sidplay2 = (SidPlay2Section) util.getConfig()
				.getSidplay2();
		fileDialog.setInitialDirectory(sidplay2.getLastDirectoryFolder());
		File directory = fileDialog.showDialog(autoConfiguration.getScene()
				.getWindow());
		if (directory != null) {
			sidplay2.setLastDirectory(directory.getAbsolutePath());
			setRootFile(directory);
		}
	}

	protected void setRootFile(final File rootFile) {
		if (rootFile.exists()) {
			collectionDir.setText(rootFile.getAbsolutePath());

			final File theRootFile = new TFile(rootFile);
			fileBrowser.setRoot(new DiskCollectionTreeItem(theRootFile,
					theRootFile, fileBrowserFileFilter));

			if (type.get() == DiskCollectionType.HVMEC) {
				util.getConfig().getSidplay2()
						.setHVMEC(rootFile.getAbsolutePath());
			} else if (type.get() == DiskCollectionType.DEMOS) {
				util.getConfig().getSidplay2()
						.setDemos(rootFile.getAbsolutePath());
			} else if (type.get() == DiskCollectionType.MAGS) {
				util.getConfig().getSidplay2()
						.setMags(rootFile.getAbsolutePath());
			}
		}
	}

	protected void attachAndRunDemo(File file, final File autoStartFile) {
		if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Awt Desktop is not supported!");
			}
		} else {
			try {
				File extractedFile = extract(file);
				util.getPlayer()
						.getConfig()
						.getSidplay2()
						.setLastDirectory(
								extractedFile.getParentFile().getAbsolutePath());
				if (diskFileFilter.accept(file)) {
					util.getPlayer().insertDisk(extractedFile, autoStartFile);
				} else {
					util.getPlayer().insertTape(extractedFile, autoStartFile);
				}
				if (autoStartFile == null) {
					resetAndLoadDemo(extractedFile);
				}
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	private void resetAndLoadDemo(final File file) {
		final String command;
		if (diskFileFilter.accept(file)) {
			// load from disk
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			// load from tape
			command = "LOAD\rRUN\r";
		}
		util.setPlayingTab(this);
		util.getPlayer().setCommand(command);
		util.getPlayer().play(null);
	}

	protected void showScreenshot(final File file) {
		Image image = createImage(file);
		if (image != null) {
			screenshot.setImage(image);
		}
	}

	private Image createImage(final File file) {
		try {
			File screenshot = findScreenshot(file);
			if (screenshot != null && screenshot.exists()) {
				try (TFileInputStream is = new TFileInputStream(screenshot)) {
					return new Image(is);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private File findScreenshot(File file) {
		TreeItem<File> rootItem = fileBrowser.getRoot();
		if (rootItem == null) {
			return null;
		}
		String parentPath = type.get() == DiskCollectionType.HVMEC ? file
				.getParentFile().getPath().replace(HVMEC_DATA, HVMEC_CONTROL)
				: file.getParentFile().getPath();
		List<File> parentFiles = PathUtils.getFiles(parentPath,
				rootItem.getValue(), null);
		if (parentFiles.size() > 0) {
			File parentFile = parentFiles.get(parentFiles.size() - 1);
			for (File photoFile : parentFile.listFiles()) {
				if (!photoFile.isDirectory()
						&& screenshotsFileFilter.accept(photoFile)) {
					return photoFile;
				}
			}
		}
		return null;
	}

	protected File extract(File file) throws IOException {
		if (file.getName().endsWith(".gz")) {
			File dst = new File(util.getConfig().getSidplay2().getTmpDir(),
					PathUtils.getBaseNameNoExt(file.getName()));
			dst.deleteOnExit();
			try (InputStream is = new GZIPInputStream(
					new TFileInputStream(file))) {
				TFile.cp(is, dst);
			}
			return dst;
		} else {
			File dst = new File(util.getConfig().getSidplay2().getTmpDir(),
					file.getName());
			dst.deleteOnExit();
			new TFile(file).cp(dst);
			return dst;
		}
	}

}
