package ui.diskcollection;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;
import ui.common.C64Tab;
import ui.directory.Directory;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.config.SidPlay2Section;
import ui.events.IInsertMedia;
import ui.events.IPlayTune;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.DocsFileFilter;
import ui.filefilter.ScreenshotFileFilter;
import ui.filefilter.TapeFileFilter;
import ui.filefilter.ZipFileExtensions;

public class DiskCollection extends C64Tab {

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

	private DiscCollectionType type;
	private String downloadUrl;

	private final FileFilter screenshotsFileFilter = new ScreenshotFileFilter();
	private final FileFilter diskFileFilter = new DiskFileFilter();
	private final FileFilter fileBrowserFileFilter = new FileFilter() {

		private final TapeFileFilter tapeFileFilter = new TapeFileFilter();
		private final DocsFileFilter docsFileFilter = new DocsFileFilter();

		@Override
		public boolean accept(File file) {
			if (type == DiscCollectionType.HVMEC && file.isDirectory()
					&& file.getName().equals("HVMEC/CONTROL/")) {
				return false;
			}
			return diskFileFilter.accept(file) || tapeFileFilter.accept(file)
					|| docsFileFilter.accept(file);
		}
	};

	private DoubleProperty progress = new SimpleDoubleProperty();

	public DoubleProperty getProgressValue() {
		return progress;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		this.downloadUrl = type == DiscCollectionType.HVMEC ? getConfig()
				.getOnline().getHvmecUrl()
				: type == DiscCollectionType.DEMOS ? getConfig().getOnline()
						.getDemosUrl()
						: type == DiscCollectionType.MAGS ? getConfig()
								.getOnline().getMagazinesUrl() : null;

		final String initialRoot = type == DiscCollectionType.HVMEC ? getConfig()
				.getSidplay2().getHVMEC()
				: type == DiscCollectionType.DEMOS ? getConfig().getSidplay2()
						.getDemos()
						: type == DiscCollectionType.MAGS ? getConfig()
								.getSidplay2().getMags() : null;

		if (initialRoot != null) {
			setRootFile(new File(initialRoot));
		}

		// XXX JavaFX: better initialization support using constructor
		// arguments?
		directory.setConfig(getConfig());
		directory.initialize(location, resources);

		directory.getAutoStartFileProperty().addListener(
				new ChangeListener<File>() {
					@Override
					public void changed(
							ObservableValue<? extends File> observable,
							File oldValue, File newValue) {
						attachAndRunDemo(fileBrowser.getSelectionModel()
								.getSelectedItem().getValue(), newValue);
					}
				});
		fileBrowser
				.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {
					@Override
					public TreeCell<File> call(TreeView<File> arg0) {
						return new TreeCell<File>() {
							@Override
							protected void updateItem(File item, boolean empty) {
								super.updateItem(item, empty);
								if (!empty) {
									setText(item.getName());
									setGraphic(getTreeItem().getGraphic());
								}
							}
						};
					}
				});
		fileBrowser.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<TreeItem<File>>() {
					@Override
					public void changed(
							ObservableValue<? extends TreeItem<File>> observable,
							TreeItem<File> oldValue, TreeItem<File> newValue) {
						if (newValue != null
								&& !newValue.equals(fileBrowser.getRoot())
								&& newValue.getValue().isFile()) {
							File file = newValue.getValue();
							showScreenshot(file);
							directory.loadPreview(file);
						}
					}

				});
		fileBrowser.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
						.getSelectedItem();
				if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
					if (!selectedItem.equals(fileBrowser.getRoot())
							&& selectedItem.getValue().isFile()) {
						File file = selectedItem.getValue();
						if (file.isFile()) {
							attachAndRunDemo(selectedItem.getValue(), null);
						}
					}
				}
			}
		});
		fileBrowser.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				final TreeItem<File> selectedItem = fileBrowser
						.getSelectionModel().getSelectedItem();
				if (selectedItem != null && selectedItem.getValue().isFile()
						&& event.isPrimaryButtonDown()
						&& event.getClickCount() > 1) {
					attachAndRunDemo(fileBrowser.getSelectionModel()
							.getSelectedItem().getValue(), null);
				}
			}
		});
		contextMenu.setOnShown(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
						.getSelectedItem();
				boolean disable = selectedItem == null
						|| !selectedItem.getValue().isFile()
						|| selectedItem.equals(fileBrowser.getRoot());
				start.setDisable(disable);
				attachDisk.setDisable(disable);
			}
		});
	}

	public DiscCollectionType getType() {
		return type;
	}

	public void setType(DiscCollectionType type) {
		this.type = type;
	}

	@FXML
	private void doAutoConfiguration() {
		if (autoConfiguration.isSelected()) {
			autoConfiguration.setDisable(true);
			try {
				DownloadThread downloadThread = new DownloadThread(getConfig(),
						new ProgressListener(progress) {

							@Override
							public void downloaded(final File downloadedFile) {
								Platform.runLater(new Runnable() {

									@Override
									public void run() {
										autoConfiguration.setDisable(false);
										if (downloadedFile != null) {
											setRootFile(downloadedFile);
										}
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
		insertMedia(IInsertMedia.MediaType.DISK, fileBrowser
				.getSelectionModel().getSelectedItem().getValue(), null);
	}

	@FXML
	private void start() {
		attachAndRunDemo(fileBrowser.getSelectionModel().getSelectedItem()
				.getValue(), null);
	}

	@FXML
	private void doBrowseZip() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(ZipFileExtensions.DESCRIPTION,
						ZipFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(autoConfiguration
				.getScene().getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			setRootFile(file);
		}
	}

	@FXML
	private void doBrowse() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		File directory = fileDialog.showDialog(autoConfiguration.getScene()
				.getWindow());
		if (directory != null) {
			getConfig().getSidplay2().setLastDirectory(
					directory.getAbsolutePath());
			setRootFile(directory);
		}
	}

	private void setRootFile(final File rootFile) {
		if (rootFile.exists()) {
			collectionDir.setText(rootFile.getAbsolutePath());

			final File theRootFile = rootFile.getName().toLowerCase()
					.endsWith(".zip") ? new ZipFileProxy(rootFile) : rootFile;
			fileBrowser.setRoot(new DiskCollectionTreeItem(theRootFile,
					theRootFile, fileBrowserFileFilter));

			if (type == DiscCollectionType.HVMEC) {
				getConfig().getSidplay2().setHVMEC(rootFile.getAbsolutePath());
			} else if (type == DiscCollectionType.DEMOS) {
				getConfig().getSidplay2().setDemos(rootFile.getAbsolutePath());
			} else if (type == DiscCollectionType.MAGS) {
				getConfig().getSidplay2().setMags(rootFile.getAbsolutePath());
			}
		}
	}

	private void attachAndRunDemo(File selectedFile, final File autoStartFile) {
		if (selectedFile.getName().toLowerCase().endsWith(".pdf")) {
			try {
				selectedFile = extractFile(selectedFile);
				if (selectedFile.exists()) {
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(selectedFile);
					} else {
						System.out.println("Awt Desktop is not supported!");
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			if (diskFileFilter.accept(selectedFile)) {
				insertMedia(IInsertMedia.MediaType.DISK, selectedFile,
						autoStartFile);
			} else {
				insertMedia(IInsertMedia.MediaType.TAPE, selectedFile,
						autoStartFile);
			}
			if (autoStartFile == null) {
				resetAndLoadDemo(selectedFile);
			}
		}
	}

	private void insertMedia(final IInsertMedia.MediaType mediaType,
			final File selectedFile, final File autoStartFile) {
		getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

			@Override
			public MediaType getMediaType() {
				return mediaType;
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
			public Object getComponent() {
				return DiskCollection.this;
			}
		});
	}

	private void resetAndLoadDemo(final File selectedFile) {
		final String command;
		if (diskFileFilter.accept(selectedFile)) {
			// load from disk
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			// load from tape
			command = "LOAD\rRUN\r";
		}
		// reset required after inserting the cartridge
		getPlayer().setCommand(command);
		getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

			@Override
			public boolean switchToVideoTab() {
				return true;
			}

			@Override
			public Object getComponent() {
				return DiskCollection.this;
			}

			@Override
			public SidTune getSidTune() {
				return null;
			}
		});
	}

	private void showScreenshot(final File file) {
		Image image = createImage(file);
		if (image != null) {
			screenshot.setImage(image);
		}
	}

	private Image createImage(final File file) {
		try {
			File screenshot = findScreenshot(file);
			if (screenshot != null) {
				screenshot = extractFile(screenshot);
				if (screenshot.exists()) {
					return new Image(screenshot.toURI().toURL()
							.toExternalForm());
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
		String parentPath = type == DiscCollectionType.HVMEC ? file
				.getParentFile().getPath().replace("/DATA/", "/CONTROL/")
				: file.getParentFile().getPath();
		List<File> parentFiles = PathUtils.getFiles(parentPath,
				rootItem.getValue(), null);
		if (parentFiles.size() > 0) {
			File parentFile = parentFiles.get(parentFiles.size() - 1);
			for (File photoFile : parentFile.listFiles()) {
				if (screenshotsFileFilter.accept(photoFile)) {
					return photoFile;
				}
			}
		}
		return null;
	}

	private File extractFile(File file) throws IOException {
		if (file instanceof ZipEntryFileProxy) {
			// Extract ZIP file
			file = ZipEntryFileProxy.extractFromZip((ZipEntryFileProxy) file,
					getConfig().getSidplay2().getTmpDir());
		}
		if (file.getName().endsWith(".gz")) {
			// Extract GZ file
			file = ZipEntryFileProxy.extractFromGZ(file, getConfig()
					.getSidplay2().getTmpDir());
		}
		return file;
	}

}
