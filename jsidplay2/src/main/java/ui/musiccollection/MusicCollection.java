package ui.musiccollection;

import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import jsidplay2.photos.SidAuthors;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.DesktopIntegration;
import libsidutils.PathUtils;
import libsidutils.psid64.Psid64;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import sidplay.Player;
import sidplay.audio.Audio;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.EnumToStringConverter;
import ui.common.TypeTextField;
import ui.common.UIPart;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.service.VersionService;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.TuneFileFilter;
import ui.musiccollection.search.SearchInIndexThread;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;
import ui.musiccollection.search.SearchThread;
import ui.stilview.STILView;

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
public class MusicCollection extends C64VBox implements UIPart {

	@FXML
	private CheckBox autoConfiguration;
	@FXML
	private TableView<TuneInfo> tuneInfoTable;
	@FXML
	private TableColumn<TuneInfo, String> nameColumn, valueColumn;
	@FXML
	private TitledPane photographPane;
	@FXML
	private ImageView photograph;
	@FXML
	private TreeView<File> fileBrowser;
	@FXML
	private ComboBox<SearchCriteria<?, ?>> searchCriteria;
	@FXML
	private ComboBox<SearchScope> searchScope;
	@FXML
	private ComboBox<SearchResult> searchResult;
	@FXML
	private Button startSearch, stopSearch, resetSearch, createSearchIndex;
	@FXML
	private TextField collectionDir;
	@FXML
	private TypeTextField stringTextField, integerTextField, longTextField, shortTextField, localDateTextField;
	@FXML
	private ComboBox<Enum<?>> combo;
	@FXML
	private ContextMenu contextMenu;
	@FXML
	private MenuItem showStil, convertToPSID64, soasc6581R2, soasc6581R3, soasc6581R4, soasc8580R5;
	@FXML
	private Menu addToFavoritesMenu;

	private final FileFilter tuneFilter = new TuneFileFilter();

	private ObjectProperty<MusicCollectionType> type;

	private ObservableList<TuneInfo> tuneInfos;
	private ObservableList<Enum<?>> comboItems;
	private ObjectProperty<List<TreeItem<File>>> currentlyPlayedTreeItemsProperty;

	private EntityManager em;
	private VersionService versionService;

	private SearchThread searchThread;
	private Object savedState, searchForValue, recentlySearchedForValue;
	private SearchCriteria<?, ?> recentlySearchedCriteria;
	private boolean searchOptionsChanged;
	private String hvscName;
	private int selectedSong;

	private FavoritesSection favoritesToAddSearchResult;

	private PropertyChangeListener tuneMatcherListener;

	private ChangeListener<? super TreeItem<File>> tuneInfoListener;

	private EventHandler<WindowEvent> contextMenuEvent;

	public MusicCollection() {
		super();
	}

	public MusicCollection(C64Window window, Player player) {
		super(window, player);
	}

	public MusicCollectionType getType() {
		return type.get();
	}

	public void setType(MusicCollectionType type) {
		this.type.set(type);
	}

	@FXML
	@Override
	protected void initialize() {
		tuneMatcherListener = event -> {
			Platform.runLater(() -> showCurrentTune());
		};
		tuneInfoListener = (observable, oldValue, newValue) -> {
			if (newValue != null && newValue.getValue().isFile()) {
				File tuneFile = newValue.getValue();
				try {
					SidTune sidTune = SidTune.load(tuneFile);
					if (getType() == MusicCollectionType.HVSC) {
						enableSOASC(sidTune.getInfo(), tuneFile);
						showPhoto(sidTune.getInfo());
					}
					showTuneInfos(tuneFile, sidTune);
				} catch (IOException | SidTuneError e) {
					openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()),
							getType());
				}
			}
		};
		contextMenuEvent = event -> {
			final TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
			showStil.setDisable(selectedItem == null || !((MusicCollectionTreeItem) selectedItem).hasSTIL());
			convertToPSID64.setDisable(selectedItem == null);

			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
			List<FavoritesSection> favorites = util.getConfig().getFavorites();
			addToFavoritesMenu.getItems().clear();
			for (final FavoritesSection section : favorites) {
				MenuItem item = new MenuItem(section.getName());
				item.setOnAction(event2 -> {
					addFavorites(sidplay2Section, section, Collections.singletonList(selectedItem.getValue()));
				});
				addToFavoritesMenu.getItems().add(item);
			}
			addToFavoritesMenu.setDisable(addToFavoritesMenu.getItems().isEmpty());
		};
		util.getPlayer().stateProperty().addListener(tuneMatcherListener);
		tuneInfos = FXCollections.<TuneInfo>observableArrayList();
		SortedList<TuneInfo> sortedList = new SortedList<>(tuneInfos);
		sortedList.comparatorProperty().bind(tuneInfoTable.comparatorProperty());
		tuneInfoTable.setItems(sortedList);
		tuneInfoTable.setPrefHeight(Double.MAX_VALUE);
		photographPane.setPrefHeight(Double.MAX_VALUE);
		nameColumn.prefWidthProperty().bind(tuneInfoTable.widthProperty().multiply(0.4));
		valueColumn.prefWidthProperty().bind(tuneInfoTable.widthProperty().multiply(0.6));

		searchScope.setConverter(new EnumToStringConverter<SearchScope>(util.getBundle()));
		searchScope.setItems(FXCollections.<SearchScope>observableArrayList(SearchScope.values()));
		searchScope.getSelectionModel().select(SearchScope.FORWARD);

		searchResult.setConverter(new EnumToStringConverter<SearchResult>(util.getBundle()));
		searchResult.setItems(FXCollections.<SearchResult>observableArrayList(SearchResult.values()));
		searchResult.getSelectionModel().select(SearchResult.SHOW_NEXT_MATCH);

		searchCriteria.setConverter(new SearchCriteriaToString(util.getBundle()));
		searchCriteria.setItems(
				FXCollections.<SearchCriteria<?, ?>>observableArrayList(SearchCriteria.getSearchableAttributes()));
		searchCriteria.getSelectionModel().select(0);

		comboItems = FXCollections.<Enum<?>>observableArrayList();
		combo.setItems(comboItems);

		currentlyPlayedTreeItemsProperty = new SimpleObjectProperty<>(Collections.emptyList());

		contextMenu.setOnShown(contextMenuEvent);

		fileBrowser.setCellFactory(treeView -> new FileTreeCell());
		fileBrowser.getSelectionModel().selectedItemProperty().addListener(tuneInfoListener);
		fileBrowser.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				playSelected();
			}

		});
		fileBrowser.setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				playSelected();
			}
		});

		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				File initialRoot;
				switch (getType()) {
				case HVSC:
					initialRoot = util.getConfig().getSidplay2Section().getHvscFile();
					break;

				case CGSC:
					initialRoot = util.getConfig().getSidplay2Section().getCgscFile();
					break;

				default:
					throw new RuntimeException("Illegal music collection type: " + type);
				}
				if (initialRoot != null) {
					setRoot(initialRoot);
					showCurrentTune();
				}
			});
		});
	}

	private void showCurrentTune() {
		if (util.getPlayer().getTune() != SidTune.RESET && fileBrowser.getRoot() != null) {
			// auto-expand current selected tune
			SidTune tune = util.getPlayer().getTune();
			String collectionName = util.getPlayer().getSidDatabaseInfo(db -> db.getPath(tune), "");
			showNextHit(new TFile(fileBrowser.getRoot().getValue(), collectionName));
		}
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(tuneMatcherListener);
		closeDatabase();
	}

	private void closeDatabase() {
		if (fileBrowser.getRoot() != null && fileBrowser.getRoot().getValue() instanceof TFile) {
			TFile tf = (TFile) fileBrowser.getRoot().getValue();
			try {
				TVFS.umount(tf);
			} catch (FsSyncException e) {
				e.printStackTrace();
			}
		}
		if (em != null && em.isOpen()) {
			em.close();
		}
	}

	@FXML
	private void showSTIL() {
		MusicCollectionTreeItem selectedItem = (MusicCollectionTreeItem) fileBrowser.getSelectionModel()
				.getSelectedItem();
		if (selectedItem != null && selectedItem.hasSTIL()) {
			STILView stilInfo = new STILView(util.getPlayer());
			stilInfo.setEntry(selectedItem.getStilEntry());
			stilInfo.open();
		}
	}

	@FXML
	private void convertToPSID64() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
		DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(sidPlay2Section.getLastDirectoryFolder());
		final File directory = fileDialog.showDialog(fileBrowser.getScene().getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(directory.getAbsolutePath());
			Psid64 c = new Psid64();
			c.setTmpDir(util.getConfig().getSidplay2Section().getTmpDir());
			c.setVerbose(true);
			try {
				c.convertFiles(util.getPlayer(), new File[] { selectedItem.getValue() }, directory,
						sidPlay2Section.getHvscFile());
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()), getType());
			}
		}
	}

	@FXML
	private void startDownload6581R2() {
		final String url = util.getConfig().getOnlineSection().getSoasc6581R2();
		downloadStart(url, 49);
	}

	@FXML
	private void startDownload6581R3() {
		final String url = util.getConfig().getOnlineSection().getSoasc6581R3();
		downloadStart(url, 49);
	}

	@FXML
	private void startDownload6581R4() {
		final String url = util.getConfig().getOnlineSection().getSoasc6581R4();
		downloadStart(url, 49);
	}

	@FXML
	private void startDownload8580R5() {
		final String url = util.getConfig().getOnlineSection().getSoasc8580R5();
		downloadStart(url, 49);
	}

	@FXML
	private void doAutoConfiguration() {
		String url, urlSearchIndex, urlSearchIndexProperties;
		switch (getType()) {
		case HVSC:
			url = util.getConfig().getOnlineSection().getHvscUrl();
			urlSearchIndex = util.getConfig().getOnlineSection().getHvscSearchIndexUrl();
			urlSearchIndexProperties = util.getConfig().getOnlineSection().getHvscSearchIndexPropertiesUrl();
			break;

		case CGSC:
			url = util.getConfig().getOnlineSection().getCgscUrl();
			urlSearchIndex = util.getConfig().getOnlineSection().getCgscSearchIndexUrl();
			urlSearchIndexProperties = util.getConfig().getOnlineSection().getCgscSearchIndexPropertiesUrl();
			break;

		default:
			throw new RuntimeException("Illegal music collection type: " + type);
		}
		if (autoConfiguration.isSelected()) {
			autoConfiguration.setDisable(true);
			closeDatabase();
			try {
				new DownloadThread(util.getConfig(), new ProgressListener(util, fileBrowser.getScene()) {

					@Override
					public void downloaded(final File downloadedFile) {
						try {
							new DownloadThread(util.getConfig(), new ProgressListener(util, fileBrowser.getScene()) {

								@Override
								public void downloaded(final File downloadedFile) {
									try {
										new DownloadThread(util.getConfig(),
												new ProgressListener(util, fileBrowser.getScene()) {
													@Override
													public void downloaded(final File downloadedFile) {
														Platform.runLater(() -> {
															autoConfiguration.setDisable(false);
															if (downloadedFile != null) {
																setRoot(downloadedFile);
															}
														});
													}
												}, new URL(url), true).start();
									} catch (MalformedURLException e) {
										e.printStackTrace();
									}
								}
							}, new URL(urlSearchIndexProperties), false).start();
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
				}, new URL(urlSearchIndex), false).start();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void searchCategory() {
		if (searchCriteria.getSelectionModel().getSelectedItem() != recentlySearchedCriteria) {
			searchOptionsChanged = true;
			recentlySearchedCriteria = searchCriteria.getSelectionModel().getSelectedItem();
		}
		setSearchEditorVisible();
	}

	@FXML
	private void doStartSearch() {
		startSearch(false);
	}

	@FXML
	private void doStopSearch() {
		if (searchThread != null && searchThread.isAlive()) {
			searchThread.setAborted(true);
		}
	}

	@FXML
	private void doResetSearch() {
		savedState = null;
	}

	@FXML
	private void doCreateSearchIndex() {
		String msg = String.format(util.getBundle().getString("RECREATE_DATABASE"), type.get().toString());

		Alert alert = new Alert(AlertType.CONFIRMATION, msg);
		alert.setTitle(util.getBundle().getString("CREATE_SEARCH_DATABASE"));
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			startSearch(true);
		}
	}

	@FXML
	private void doBrowse() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		File directory = fileDialog.showDialog(autoConfiguration.getScene().getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(directory.getAbsolutePath());
			setRoot(directory);
		}
	}

	@FXML
	private void gotoURL() {
		DesktopIntegration.browse(type.get().getUrl());
	}

	@FXML
	private void doSetValue() {
		setSearchValue();
		if (!Objects.equals(searchForValue, recentlySearchedForValue)) {
			searchOptionsChanged = true;
			recentlySearchedForValue = searchForValue;
		}
		startSearch(false);
	}

	private void setSearchEditorVisible() {
		for (Node node : Arrays.asList(stringTextField, integerTextField, localDateTextField, longTextField,
				shortTextField, combo)) {
			node.setVisible(false);
		}
		SearchCriteria<?, ?> selectedItem = searchCriteria.getSelectionModel().getSelectedItem();
		Class<?> type = selectedItem.getAttribute().getJavaType();
		if (type == Long.class) {
			longTextField.setVisible(true);
		} else if (type == Integer.class) {
			integerTextField.setVisible(true);
		} else if (type == LocalDateTime.class) {
			localDateTextField.setVisible(true);
		} else if (type == Short.class) {
			shortTextField.setVisible(true);
		} else if (type == String.class) {
			stringTextField.setVisible(true);
		} else if (Enum.class.isAssignableFrom(type)) {
			comboItems.clear();
			@SuppressWarnings("unchecked")
			Class<? extends Enum<?>> en = (Class<? extends Enum<?>>) type;
			for (Enum<?> val : en.getEnumConstants()) {
				comboItems.add(val);
			}
			combo.setVisible(true);
			combo.getSelectionModel().select(0);
		}
	}

	private void setSearchValue() {
		SearchCriteria<?, ?> selectedItem = searchCriteria.getSelectionModel().getSelectedItem();
		Class<?> type = selectedItem.getAttribute().getJavaType();
		if (type == Integer.class) {
			searchForValue = integerTextField.getValue();
		} else if (type == Long.class) {
			searchForValue = longTextField.getValue();
		} else if (type == Short.class) {
			searchForValue = shortTextField.getValue();
		} else if (type == String.class) {
			searchForValue = stringTextField.getValue();
		} else if (Enum.class.isAssignableFrom(type)) {
			searchForValue = combo.getSelectionModel().getSelectedItem();
		} else if (type == LocalDateTime.class) {
			searchForValue = localDateTextField.getValue();
		}
	}

	private void setRoot(final File rootFile) {
		try {
			SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
			if (getType() == MusicCollectionType.HVSC) {
				util.getPlayer().setSidDatabase(new SidDatabase(rootFile.getAbsolutePath()));
				setSTIL(rootFile.getAbsolutePath());
				sidPlay2Section.setHvsc(rootFile.getAbsolutePath());
				setViewRoot(sidPlay2Section.getHvscFile());
			} else if (getType() == MusicCollectionType.CGSC) {
				sidPlay2Section.setCgsc(rootFile.getAbsolutePath());
				setViewRoot(sidPlay2Section.getCgscFile());
			}

			closeDatabase();
			File dbFilename = new File(rootFile.getParentFile(), type.get().toString());
			PersistenceProperties pp = new PersistenceProperties(dbFilename.getAbsolutePath(), "", "",
					Database.HSQL_FILE);
			EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(type.get().getDataSource(), pp);
			em = emFactory.createEntityManager();
			versionService = new VersionService(em);
		} catch (FileNotFoundException e) {
			openErrorDialog(String.format(util.getBundle().getString("ERR_FILE_NOT_FOUND"), e.getMessage()), getType());
		} catch (IOException | NoSuchFieldException | IllegalAccessException | PersistenceException
				| IllegalStateException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			openErrorDialog(sw.toString(), getType());
		}
	}

	private void openErrorDialog(String msg, MusicCollectionType type) {
		msg = String.format(util.getBundle().getString("ERR_CANNOT_CONFIGURE"), type) + msg;

		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}

	private void setViewRoot(final File theRootFile) {
		MusicCollectionCellFactory cellFactory = new MusicCollectionCellFactory();
		cellFactory.setCurrentlyPlayedTreeItems(currentlyPlayedTreeItemsProperty);
		fileBrowser.setRoot(new MusicCollectionTreeItem(util.getPlayer(), theRootFile));
		fileBrowser.setCellFactory(cellFactory);
		collectionDir.setText(theRootFile.getAbsolutePath());
		doResetSearch();
	}

	private void setSTIL(String hvscRoot) throws IOException, NoSuchFieldException, IllegalAccessException {
		try (InputStream input = new TFileInputStream(new TFile(hvscRoot, STIL.STIL_FILE))) {
			util.getPlayer().setSTIL(new STIL(input));
		}
	}

	private void startSearch(boolean forceRecreate) {
		if (searchThread != null && searchThread.isAlive()) {
			return;
		}

		if (!new File(collectionDir.getText()).exists()) {
			return;
		}

		/*
		 * validate database: version is inserted only after successful create
		 * completes.
		 */
		if (!forceRecreate && !versionService.isExpectedVersion()) {
			forceRecreate = true;
		}

		Consumer<Void> searchStart;
		Consumer<File> searchHit;
		Consumer<Boolean> searchStop;
		if (forceRecreate) {
			if (!em.isOpen()) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_DATABASE")), getType());
				return;
			}
			SearchIndexCreator searchIndexCreator = new SearchIndexCreator(fileBrowser.getRoot().getValue(),
					util.getPlayer(), em);
			searchStart = x -> {
				Platform.runLater(() -> {
					disableSearch();
					util.progressProperty(fileBrowser.getScene()).set(ProgressIndicator.INDETERMINATE_PROGRESS);
				});
				searchIndexCreator.getSearchStart().accept(x);
			};
			searchHit = searchIndexCreator.getSearchHit();
			searchStop = cancelled -> {
				Platform.runLater(() -> {
					enableSearch();
					util.progressProperty(fileBrowser.getScene()).set(0);
				});
				searchIndexCreator.getSearchStop().accept(cancelled);
			};

			searchThread = new SearchIndexerThread(fileBrowser.getRoot().getValue(), searchStart, searchHit,
					searchStop);
			searchThread.start();
		} else {
			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

			switch (searchResult.getSelectionModel().getSelectedItem()) {
			case ADD_TO_A_NEW_PLAYLIST:
				searchStart = file -> {
					Platform.runLater(() -> {
						disableSearch();
						createNewFavoritesTab();
					});
				};
				searchHit = file -> {
					while (favoritesToAddSearchResult == null) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					addFavorite(sidplay2Section, favoritesToAddSearchResult, file);
				};
				searchStop = cancelled -> Platform.runLater(() -> enableSearch());
				break;

			case SHOW_NEXT_MATCH:
			default:
				searchStart = x -> Platform.runLater(() -> disableSearch());
				searchHit = file -> {
					// ignore directories
					if (file.isFile()) {
						searchThread.setAborted(true);
						Platform.runLater(() -> {
							enableSearch();
							showNextHit(file);
						});
					}
				};
				searchStop = cancelled -> Platform.runLater(() -> enableSearch());
				break;
			}
			setSearchValue();
			final SearchInIndexThread t = new SearchInIndexThread(em,
					searchScope.getSelectionModel().getSelectedItem() == SearchScope.FORWARD, searchStart, searchHit,
					searchStop) {
				@Override
				public List<File> getFiles(String filePath) {
					return PathUtils.getFiles(filePath, fileBrowser.getRoot().getValue(), tuneFilter);
				}
			};
			t.setField(searchCriteria.getSelectionModel().getSelectedItem().getAttribute());
			t.setFieldValue(searchForValue);
			t.setCaseSensitive(false);
			if (searchOptionsChanged) {
				doResetSearch();
				searchOptionsChanged = false;
			}
			searchThread = t;
			searchThread.setSearchState(savedState);
			searchThread.start();
		}

	}

	private void disableSearch() {
		startSearch.setDisable(true);
		stopSearch.setDisable(false);
		resetSearch.setDisable(true);
		createSearchIndex.setDisable(true);
	}

	private void enableSearch() {
		// remember search state
		savedState = searchThread.getSearchState();
		startSearch.setDisable(false);
		stopSearch.setDisable(true);
		resetSearch.setDisable(false);
		createSearchIndex.setDisable(false);
	}

	private void showNextHit(final File matchFile) {
		if (!matchFile.isFile()) {
			// ignore directories
			return;
		}
		TreeItem<File> rootItem = fileBrowser.getRoot();
		if (rootItem == null || matchFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			return;
		}
		List<TreeItem<File>> pathSegs = new ArrayList<>();
		pathSegs.add(rootItem);

		File rootFile = rootItem.getValue();
		String filePath = matchFile.getPath();
		TreeItem<File> curItem = rootItem;
		for (File file : PathUtils.getFiles(filePath, rootFile, tuneFilter)) {
			for (TreeItem<File> childItem : curItem.getChildren()) {
				if (file.equals(childItem.getValue())) {
					pathSegs.add(curItem = childItem);
					childItem.setExpanded(true);
					break;
				}
			}
		}
		if (pathSegs.size() > 0) {
			currentlyPlayedTreeItemsProperty.set(pathSegs);
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
			TreeItem<File> treeItem = pathSegs.get(pathSegs.size() - 1);
			if (selectedItem == null || !treeItem.getValue().equals(selectedItem.getValue())) {
				fileBrowser.getSelectionModel().select(treeItem);
				fileBrowser.scrollTo(fileBrowser.getRow(treeItem));
			}
		}
	}

	private void createNewFavoritesTab() {
		FavoritesSection newFavorites = new FavoritesSection();
		newFavorites.setName(util.getBundle().getString("NEW_TAB"));
		favoritesToAddSearchResult = newFavorites;
		util.getConfig().getFavorites().add(newFavorites);
	}

	private void downloadStart(final String url, final int hvscVersion) {
		String realUrl = MessageFormat.format(url, hvscVersion, hvscName, selectedSong).trim();
		System.out.println("Download URL: <" + realUrl + ">");
		try {
			new DownloadThread(util.getConfig(), new ProgressListener(util, fileBrowser.getScene()) {

				@Override
				public void downloaded(final File downloadedFile) {
					if (downloadedFile != null) {
						if (downloadedFile.length() < 1000 && hvscVersion < 100) {
							// skip errors, try to guess HVSC version
							downloadedFile.delete();
							downloadStart(url, hvscVersion + 1);
						} else {
							downloadedFile.deleteOnExit();
							Platform.runLater(() -> {
								Configuration config = util.getConfig();
								SidPlay2Section sidplay2Section = config.getSidplay2Section();
								AudioSection audioSection = config.getAudioSection();
								audioSection.setMp3File(downloadedFile.getAbsolutePath());
								audioSection.setPlayOriginal(true);
								audioSection.setAudio(Audio.COMPARE_MP3);
								playTune(PathUtils.getFile(hvscName + ".sid", sidplay2Section.getHvscFile(), null));
							});
						}
					}
				}
			}, new URL(realUrl), false).start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void enableSOASC(SidTuneInfo tuneInfo, File tuneFile) {
		soasc6581R2.setDisable(true);
		soasc6581R3.setDisable(true);
		soasc6581R4.setDisable(true);
		soasc8580R5.setDisable(true);
		File hvscFile = util.getConfig().getSidplay2Section().getHvscFile();
		hvscName = PathUtils.getCollectionName(hvscFile, tuneFile);
		if (hvscName != null) {
			hvscName = hvscName.replace(".sid", "");
			selectedSong = tuneInfo.getSelectedSong();
			soasc6581R2.setDisable(false);
			soasc6581R3.setDisable(false);
			soasc6581R4.setDisable(false);
			soasc8580R5.setDisable(false);
		}
	}

	private void showPhoto(SidTuneInfo info) {
		if (info.getInfoString().size() > 1) {
			Iterator<String> it = info.getInfoString().iterator();
			/* String name = */it.next();
			String author = it.next();
			byte[] imageData = SidAuthors.getImageData(author);
			photograph.setImage(imageData != null ? new Image(new ByteArrayInputStream(imageData)) : null);
		}
	}

	private void showTuneInfos(File tuneFile, SidTune tune) {
		String collectionName = PathUtils.getCollectionName(fileBrowser.getRoot().getValue(), tuneFile);
		HVSCEntry entry = new HVSCEntry(() -> util.getPlayer().getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.),
				collectionName, tuneFile, tune);
		tuneInfos.setAll(SearchCriteria
				.getAttributeValues(entry, field -> searchCriteria.getConverter().toString(field)).stream()
				.map(info -> new TuneInfo(info.getKey(), info.getValue())).collect(Collectors.toList()));
	}

	private void addFavorites(SidPlay2Section sidplay2Section, FavoritesSection section, List<File> files) {
		for (File file : files) {
			final File[] listFiles = file.listFiles();
			if (file.isDirectory() && listFiles != null) {
				addFavorites(sidplay2Section, section, Arrays.asList(listFiles));
			} else if (file.isFile() && tuneFilter.accept(file)) {
				addFavorite(sidplay2Section, section, file);
			}
		}
	}

	private void addFavorite(SidPlay2Section sidPlay2Section, FavoritesSection section, File file) {
		try {
			SidTune tune = SidTune.load(file);

			String collectionName;
			if (getType() == MusicCollectionType.HVSC) {
				collectionName = PathUtils.getCollectionName(sidPlay2Section.getHvscFile(), file);
			} else {
				collectionName = PathUtils.getCollectionName(sidPlay2Section.getCgscFile(), file);
			}
			HVSCEntry entry = new HVSCEntry(() -> util.getPlayer().getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.),
					collectionName, file, tune);
			section.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()), getType());
		}
	}

	private void playSelected() {
		final TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
		if (selectedItem != null && !selectedItem.equals(fileBrowser.getRoot()) && selectedItem.getValue().isFile()) {
			playTune(selectedItem.getValue());
		}
	}

	private void playTune(final File file) {
		util.setPlayingTab(this, currentlyPlayedTreeItemsProperty);
		try {
			util.getPlayer().play(SidTune.load(file));
		} catch (IOException | SidTuneError e) {
			openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()), getType());
		}
	}

}