package ui.musiccollection;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.metamodel.SingularAttribute;

import libpsid64.Psid64;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import libsidutils.WebUtils;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.TypeTextField;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.common.dialog.YesNoDialog;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.collection.service.VersionService;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.TuneFileFilter;
import ui.musiccollection.search.ISearchListener;
import ui.musiccollection.search.SearchInIndexThread;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;
import ui.musiccollection.search.SearchThread;
import ui.stilview.STILView;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

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
public class MusicCollection extends Tab implements UIPart {

	public static final String HVSC_ID = "HVSC";
	public static final String CGSC_ID = "CGSC";

	private static final String HVSC_URL = "http://www.hvsc.de/";
	private static final String CGSC_URL = "http://www.c64music.co.uk/";

	@FXML
	private CheckBox autoConfiguration;
	@FXML
	private TableView<TuneInfo> tuneInfoTable;
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
	private TypeTextField stringTextField, integerTextField, longTextField,
			shortTextField;
	@FXML
	private ComboBox<Enum<?>> combo;
	@FXML
	private ContextMenu contextMenu;
	@FXML
	private MenuItem showStil, convertToPSID64, soasc6581R2, soasc6581R4,
			soasc8580R5;
	@FXML
	private Menu addToFavoritesMenu;

	private UIUtil util;

	private ObjectProperty<MusicCollectionType> type;

	private ObservableList<TuneInfo> tuneInfos;
	private ObservableList<Enum<?>> comboItems;
	private ObservableList<TreeItem<File>> currentlyPlayedTreeItems = FXCollections
			.<TreeItem<File>> observableArrayList();

	private String collectionURL;

	private EntityManager em;
	private VersionService versionService;

	private final FileFilter tuneFilter = new TuneFileFilter();
	private SearchThread searchThread;
	private Object savedState, searchForValue, recentlySearchedForValue;
	private SearchCriteria<?, ?> recentlySearchedCriteria;
	private boolean searchOptionsChanged;
	private String hvscName;
	private int currentSong;

	private FavoritesSection favoritesToAddSearchResult;

	private ChangeListener<? super State> tuneMatcherListener = (observable,
			oldValue, newValue) -> {
		if (newValue == State.START
				&& util.getPlayer().getTune() != SidTune.RESET) {
			Platform.runLater(() -> {
				if (fileBrowser.getRoot() != null) {
					// auto-expand current selected tune
					SidTune tune = util.getPlayer().getTune();
					String collectionName = util.getPlayer()
							.getSidDatabaseInfo(db -> db.getPath(tune), "");
					showNextHit(new TFile(fileBrowser.getRoot().getValue(),
							collectionName));
				}
			});
		}
	};

	private ChangeListener<? super TreeItem<File>> tuneInfoListener = (
			observable, oldValue, newValue) -> {
		for (MenuItem item : Arrays.asList(soasc6581R2, soasc6581R4,
				soasc8580R5)) {
			item.setDisable(true);
		}
		if (newValue != null && newValue.getValue().isFile()) {
			File tuneFile = newValue.getValue();
			try {
				SidTune sidTune = SidTune.load(tuneFile);
				showPhoto(sidTune);
				showTuneInfos(tuneFile, sidTune);
				getSOASCURL(sidTune, tuneFile);
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	};

	private EventHandler<WindowEvent> contextMenuEvent = event -> {
		final TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
				.getSelectedItem();
		showStil.setDisable(selectedItem == null
				|| !((MusicCollectionTreeItem) selectedItem).hasSTIL());
		convertToPSID64.setDisable(selectedItem == null);

		SidPlay2Section sidplay2Section = (SidPlay2Section) util.getPlayer()
				.getConfig().getSidplay2Section();
		List<FavoritesSection> favorites = ((Configuration) util.getConfig())
				.getFavorites();
		addToFavoritesMenu.getItems().clear();
		for (final FavoritesSection section : favorites) {
			MenuItem item = new MenuItem(section.getName());
			item.setOnAction(event2 -> {
				addFavorites(sidplay2Section, section,
						Collections.singletonList(selectedItem.getValue()));
			});
			addToFavoritesMenu.getItems().add(item);
		}
		addToFavoritesMenu.setDisable(addToFavoritesMenu.getItems().isEmpty());
	};

	public MusicCollection(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	public MusicCollectionType getType() {
		return type.get();
	}

	public void setType(MusicCollectionType type) {
		switch (type) {
		case HVSC:
			setId(HVSC_ID);
			collectionURL = HVSC_URL;
			break;

		case CGSC:
			setId(CGSC_ID);
			collectionURL = CGSC_URL;
			break;

		default:
			break;
		}
		setText(util.getBundle().getString(getId()));
		this.type.set(type);
	}

	@FXML
	private void initialize() {
		util.getPlayer().stateProperty().addListener(tuneMatcherListener);
		tuneInfos = FXCollections.<TuneInfo> observableArrayList();
		tuneInfoTable.setItems(tuneInfos);

		searchScope
				.setConverter(new EnumToString<SearchScope>(util.getBundle()));
		searchScope.setItems(FXCollections
				.<SearchScope> observableArrayList(SearchScope.values()));
		searchScope.getSelectionModel().select(SearchScope.FORWARD);

		searchResult.setConverter(new EnumToString<SearchResult>(util
				.getBundle()));
		searchResult.setItems(FXCollections
				.<SearchResult> observableArrayList(SearchResult.values()));
		searchResult.getSelectionModel().select(SearchResult.SHOW_NEXT_MATCH);

		searchCriteria
				.setConverter(new SearchCriteriaToString(util.getBundle()));
		searchCriteria.setItems(FXCollections
				.<SearchCriteria<?, ?>> observableArrayList(SearchCriteria
						.getSearchableAttributes()));
		searchCriteria.getSelectionModel().select(0);

		comboItems = FXCollections.<Enum<?>> observableArrayList();
		combo.setItems(comboItems);

		contextMenu.setOnShown(contextMenuEvent);

		fileBrowser.setCellFactory(treeView -> new FileTreeCell());
		fileBrowser.getSelectionModel().selectedItemProperty()
				.addListener(tuneInfoListener);
		fileBrowser.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				playSelected();
			}

		});
		fileBrowser.setOnMousePressed((event) -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				playSelected();
			}
		});

		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				String initialRoot;
				switch (getType()) {
				case HVSC:
					initialRoot = util.getConfig().getSidplay2Section()
							.getHvsc();
					break;

				case CGSC:
					initialRoot = util.getConfig().getSidplay2Section()
							.getCgsc();
					break;

				default:
					throw new RuntimeException(
							"Illegal music collection type: " + type);
				}
				if (initialRoot != null) {
					setRoot(new File(initialRoot));
				}
			});
		});
	}

	public void doClose() {
		util.getPlayer().stateProperty().removeListener(tuneMatcherListener);
		if (em != null) {
			em.getEntityManagerFactory().close();
		}
	}

	@FXML
	private void showSTIL() {
		MusicCollectionTreeItem selectedItem = (MusicCollectionTreeItem) fileBrowser
				.getSelectionModel().getSelectedItem();
		if (selectedItem != null && selectedItem.hasSTIL()) {
			STILView stilInfo = new STILView(util.getPlayer());
			stilInfo.setEntry(selectedItem.getStilEntry());
			stilInfo.open();
		}
	}

	@FXML
	private void convertToPSID64() {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) (util.getConfig()
				.getSidplay2Section());
		TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
				.getSelectedItem();
		DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog
				.setInitialDirectory(sidPlay2Section.getLastDirectoryFolder());
		final File directory = fileDialog.showDialog(fileBrowser.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2Section()
					.setLastDirectory(directory.getAbsolutePath());
			Psid64 c = new Psid64();
			c.setTmpDir(util.getConfig().getSidplay2Section().getTmpDir());
			c.setVerbose(true);
			try {
				c.convertFiles(util.getPlayer(),
						new File[] { selectedItem.getValue() }, directory,
						sidPlay2Section.getHvscFile());
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void startDownload6581R2() {
		final String url = util.getConfig().getOnlineSection().getSoasc6581R2();
		downloadStart(MessageFormat.format(url, hvscName, currentSong).trim());
	}

	@FXML
	private void startDownload6581R4() {
		final String url = util.getConfig().getOnlineSection().getSoasc6581R4();
		downloadStart(MessageFormat.format(url, hvscName, currentSong).trim());
	}

	@FXML
	private void startDownload8580R5() {
		final String url = util.getConfig().getOnlineSection().getSoasc8580R5();
		downloadStart(MessageFormat.format(url, hvscName, currentSong).trim());
	}

	@FXML
	private void doAutoConfiguration() {
		String url;
		switch (getType()) {
		case HVSC:
			url = util.getConfig().getOnlineSection().getHvscUrl();
			break;

		case CGSC:
			url = util.getConfig().getOnlineSection().getCgscUrl();
			break;

		default:
			throw new RuntimeException("Illegal music collection type: " + type);
		}
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
										setRoot(downloadedFile);
									}
								});
							}
						}, new URL(url));
				downloadThread.start();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void searchCategory() {
		if (searchCriteria.getSelectionModel().getSelectedItem() != recentlySearchedCriteria) {
			searchOptionsChanged = true;
			recentlySearchedCriteria = searchCriteria.getSelectionModel()
					.getSelectedItem();
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
		YesNoDialog dialog = new YesNoDialog(util.getPlayer());
		dialog.getStage().setTitle(
				util.getBundle().getString("CREATE_SEARCH_DATABASE"));
		dialog.setText(String.format(
				util.getBundle().getString("RECREATE_DATABASE"), type.get()
						.toString()));
		dialog.getConfirmed().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				startSearch(true);
			}
		});
		dialog.open();
	}

	@FXML
	private void doBrowse() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2Section())).getLastDirectoryFolder());
		File directory = fileDialog.showDialog(autoConfiguration.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2Section()
					.setLastDirectory(directory.getAbsolutePath());
			setRoot(directory);
		}
	}

	@FXML
	private void gotoURL() {
		WebUtils.browse(collectionURL);
	}

	@FXML
	private void doSetValue() {
		setSearchValue();
		if (searchForValue != null
				&& !searchForValue.equals(recentlySearchedForValue)) {
			searchOptionsChanged = true;
			recentlySearchedForValue = searchForValue;
		}
		startSearch(false);
	}

	private void setSearchEditorVisible() {
		for (Node node : Arrays.asList(stringTextField, integerTextField,
				longTextField, shortTextField, combo)) {
			node.setVisible(false);
		}
		Class<?> type = getSelectedField().getJavaType();
		if (type == Long.class) {
			longTextField.setVisible(true);
		} else if (type == Integer.class || type == Date.class) {
			integerTextField.setVisible(true);
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
		Class<?> type = getSelectedField().getJavaType();
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
		} else if (type == Date.class) {
			Calendar cal = Calendar.getInstance();
			cal.set((Integer) integerTextField.getValue(), 1, 1);
			searchForValue = cal.getTime();
		}
	}

	private SingularAttribute<?, ?> getSelectedField() {
		return ((SearchCriteria<?, ?>) searchCriteria.getSelectionModel()
				.getSelectedItem()).getAttribute();
	}

	private void setRoot(final File rootFile) {
		if (rootFile.exists()) {
			if (em != null) {
				em.getEntityManagerFactory().close();
			}
			em = Persistence.createEntityManagerFactory(
					PersistenceProperties.COLLECTION_DS,
					new PersistenceProperties(new File(
							rootFile.getParentFile(), type.get().toString())
							.getAbsolutePath(), Database.HSQL_FILE))
					.createEntityManager();

			versionService = new VersionService(em);
			collectionDir.setText(rootFile.getAbsolutePath());

			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2Section();
			if (getType() == MusicCollectionType.HVSC) {
				util.getConfig().getSidplay2Section()
						.setHvsc(rootFile.getAbsolutePath());
				File theRootFile = sidPlay2Section.getHvscFile();
				setSongLengthDatabase(sidPlay2Section.getHvsc());
				setSTIL(sidPlay2Section.getHvsc());
				fileBrowser.setRoot(new MusicCollectionTreeItem(util
						.getPlayer(), theRootFile));
			} else if (getType() == MusicCollectionType.CGSC) {
				util.getConfig().getSidplay2Section()
						.setCgsc(rootFile.getAbsolutePath());
				File theRootFile = sidPlay2Section.getCgscFile();
				fileBrowser.setRoot(new MusicCollectionTreeItem(util
						.getPlayer(), theRootFile));
			}
			MusicCollectionCellFactory cellFactory = new MusicCollectionCellFactory();
			cellFactory.setCurrentlyPlayedTreeItems(currentlyPlayedTreeItems);
			fileBrowser.setCellFactory(cellFactory);
		}
		doResetSearch();
	}

	private void setSTIL(String hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				STIL.STIL_FILE))) {
			util.getPlayer().setSTIL(new STIL(input));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setSongLengthDatabase(String hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				SidDatabase.SONGLENGTHS_FILE))) {
			util.getPlayer().setSidDatabase(new SidDatabase(input));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showPhoto(SidTune sidTune) {
		Collection<String> infoString = sidTune.getInfo().getInfoString();
		if (infoString.size() > 1) {
			Iterator<String> it = infoString.iterator();
			/* String name = */it.next();
			String author = it.next();
			photograph.setImage(SidAuthors.getImage(author));
		}
	}

	private void showTuneInfos(File tuneFile, SidTune tune) {
		tuneInfos.clear();
		String collectionName = PathUtils.getCollectionName(fileBrowser
				.getRoot().getValue(), tuneFile.getPath());
		HVSCEntry entry = new HVSCEntry(() -> util.getPlayer()
				.getSidDatabaseInfo(db -> db.getFullSongLength(tune), 0),
				collectionName, tuneFile, tune);

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())) {
				continue;
			}
			if (!(SingularAttribute.class.isAssignableFrom(field.getType()))) {
				continue;
			}
			TuneInfo tuneInfo = new TuneInfo();
			String name = util.getBundle().getString(
					HVSCEntry.class.getSimpleName() + "." + field.getName());
			tuneInfo.setName(name);
			try {
				SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
						.get(entry);
				Object value = ((Method) singleAttribute.getJavaMember())
						.invoke(entry);
				tuneInfo.setValue(String.valueOf(value != null ? value : ""));
			} catch (IllegalArgumentException | IllegalAccessException
					| InvocationTargetException e) {
			}
			tuneInfos.add(tuneInfo);
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

		Consumer<Boolean> searchStart;
		Consumer<File> searchHit;
		if (forceRecreate) {
			searchStart = reCreate -> {
				disableSearch();
				Platform.runLater(() -> util.progressProperty(fileBrowser).set(
						ProgressBar.INDETERMINATE_PROGRESS));
			};
			searchHit = file -> {
			};
			searchStart.accept(forceRecreate);

			final File root = fileBrowser.getRoot().getValue();
			searchThread = new SearchIndexerThread(root);
			searchThread.addSearchListener(new ISearchListener() {
				@Override
				public void searchStart() {
				}

				@Override
				public void searchHit(File match) {
					searchHit.accept(match);
				}

				@Override
				public void searchStop(boolean canceled) {
					enableSearch();
					Platform.runLater(() -> util.progressProperty(fileBrowser)
							.set(0));
				}

			});
			searchThread.addSearchListener(new SearchIndexCreator(fileBrowser
					.getRoot().getValue(), util.getPlayer(), em));

			searchThread.start();
		} else {
			SidPlay2Section sidplay2Section = (SidPlay2Section) util
					.getPlayer().getConfig().getSidplay2Section();

			SearchResult result = searchResult.getSelectionModel()
					.getSelectedItem();
			switch (result) {
			case ADD_TO_A_NEW_PLAYLIST:
				searchStart = file -> createNewFavoritesTab();
				searchHit = file -> addFavorite(sidplay2Section,
						favoritesToAddSearchResult, file);
				break;

			case SHOW_NEXT_MATCH:
			default:
				searchStart = file -> {
				};
				searchHit = file -> {
					stopSearch(file);
					Platform.runLater(() -> showNextHit(file));
				};
				break;
			}
			searchStart.accept(forceRecreate);
			setSearchValue();
			final SearchInIndexThread t = new SearchInIndexThread(
					em,
					searchScope.getSelectionModel().getSelectedItem() == SearchScope.FORWARD) {
				@Override
				public List<File> getFiles(String filePath) {
					return PathUtils.getFiles(filePath, fileBrowser.getRoot()
							.getValue(), tuneFilter);
				}
			};
			t.addSearchListener(new ISearchListener() {
				@Override
				public void searchStart() {
					disableSearch();
				}

				@Override
				public void searchHit(File match) {
					searchHit.accept(match);
				}

				@Override
				public void searchStop(boolean canceled) {
					enableSearch();
				}

			});
			t.setField(getSelectedField());
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

	private void stopSearch(File current) {
		if (!current.isFile()) {
			// ignore directories
			return;
		}
		searchThread.setAborted(true);
		enableSearch();

	}

	private void disableSearch() {
		Platform.runLater(() -> {
			startSearch.setDisable(true);
			stopSearch.setDisable(false);
			resetSearch.setDisable(true);
			createSearchIndex.setDisable(true);
		});
	}

	private void enableSearch() {
		// remember search state
		savedState = searchThread.getSearchState();
		Platform.runLater(() -> {
			startSearch.setDisable(false);
			stopSearch.setDisable(true);
			resetSearch.setDisable(false);
			createSearchIndex.setDisable(false);
		});
	}

	private void showNextHit(final File matchFile) {
		if (!matchFile.isFile()) {
			// ignore directories
			return;
		}
		TreeItem<File> rootItem = fileBrowser.getRoot();
		if (rootItem == null
				|| matchFile.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".mp3")) {
			return;
		}
		List<TreeItem<File>> pathSegs = new ArrayList<TreeItem<File>>();
		pathSegs.add(rootItem);

		File rootFile = rootItem.getValue();
		String filePath = matchFile.getPath();
		TreeItem<File> curItem = rootItem;
		for (File file : PathUtils.getFiles(filePath, rootFile, tuneFilter)) {
			for (TreeItem<File> childItem : curItem.getChildren()) {
				if (file.equals(childItem.getValue())) {
					pathSegs.add(childItem);
					curItem = childItem;
					childItem.setExpanded(true);
				}
			}
		}
		if (pathSegs.size() > 0) {
			currentlyPlayedTreeItems.setAll(pathSegs);
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			TreeItem<File> treeItem = pathSegs.get(pathSegs.size() - 1);
			if (selectedItem == null
					|| !treeItem.getValue().equals(selectedItem.getValue())) {
				fileBrowser.getSelectionModel().select(treeItem);
				fileBrowser.scrollTo(fileBrowser.getRow(treeItem));
			}
		}
	}

	private void createNewFavoritesTab() {
		FavoritesSection newFavorites = new FavoritesSection();
		newFavorites.setName(util.getBundle().getString("NEW_TAB"));
		favoritesToAddSearchResult = newFavorites;
		((Configuration) util.getConfig()).getFavorites().add(newFavorites);
	}

	private void downloadStart(String url) {
		System.out.println("Download URL: <" + url + ">");
		try {
			new DownloadThread(util.getConfig(), new ProgressListener(util,
					fileBrowser) {

				@Override
				public void downloaded(final File downloadedFile) {
					if (downloadedFile != null) {
						downloadedFile.deleteOnExit();
						Platform.runLater(() -> playTune(downloadedFile));
					}
				}
			}, new URL(url)).start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void getSOASCURL(SidTune sidTune, File tuneFile) {
		final SidTuneInfo tuneInfo = sidTune.getInfo();
		File rootFile = new File(util.getConfig().getSidplay2Section()
				.getHvsc());
		String collectionName = PathUtils.getCollectionName(
				new TFile(rootFile), tuneFile.getPath());
		if (collectionName != null && getType() == MusicCollectionType.HVSC) {
			hvscName = collectionName.replace(".sid", "");
			currentSong = tuneInfo.getCurrentSong();
			for (MenuItem item : Arrays.asList(soasc6581R2, soasc6581R4,
					soasc8580R5)) {
				item.setDisable(false);
			}
		}
	}

	private void addFavorites(SidPlay2Section sidplay2Section,
			FavoritesSection section, List<File> files) {
		for (File file : files) {
			if (file.isDirectory()) {
				addFavorites(sidplay2Section, section,
						Arrays.asList(file.listFiles()));
			} else if (file.isFile() && tuneFilter.accept(file)) {
				addFavorite(sidplay2Section, section, file);
			}
		}
	}

	private void addFavorite(SidPlay2Section sidPlay2Section,
			FavoritesSection section, File file) {
		try {
			SidTune tune = SidTune.load(file);
			String collectionName = PathUtils.getCollectionName(
					sidPlay2Section.getHvscFile(), file.getPath());
			HVSCEntry entry = new HVSCEntry(() -> util.getPlayer()
					.getSidDatabaseInfo(db -> db.getFullSongLength(tune), 0),
					collectionName, file, tune);
			section.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	private void playSelected() {
		final TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
				.getSelectedItem();
		if (selectedItem != null && !selectedItem.equals(fileBrowser.getRoot())
				&& selectedItem.getValue().isFile()) {
			playTune(selectedItem.getValue());
		}
	}

	private void playTune(final File file) {
		util.setPlayingTab(this);
		try {
			util.getPlayer().play(SidTune.load(file));
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

}