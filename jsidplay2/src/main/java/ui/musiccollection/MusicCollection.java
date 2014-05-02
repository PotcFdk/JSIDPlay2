package ui.musiccollection;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.metamodel.SingularAttribute;

import libpsid64.NotEnoughC64MemException;
import libpsid64.Psid64;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.State;
import sidplay.ini.IniReader;
import ui.common.C64Window;
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
import ui.entities.collection.StilEntry_;
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

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	private class SearchCriteria<DECLARING_CLASS, JAVA_TYPE> {
		public SearchCriteria(SingularAttribute<DECLARING_CLASS, JAVA_TYPE> att) {
			this.attribute = att;
		}

		private SingularAttribute<DECLARING_CLASS, JAVA_TYPE> attribute;

		public SingularAttribute<DECLARING_CLASS, JAVA_TYPE> getAttribute() {
			return attribute;
		}

		@Override
		public String toString() {
			return util.getBundle().getString(
					attribute.getDeclaringType().getJavaType().getSimpleName()
							+ "." + attribute.getName());
		}
	}

	@FXML
	private CheckBox autoConfiguration, enableSldb, singleSong;
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
	private TextField collectionDir, defaultTime;
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

	private ObservableList<TuneInfo> tuneInfos;
	private ObservableList<SearchScope> searchScopes;
	private ObservableList<SearchResult> searchResults;
	private ObservableList<SearchCriteria<?, ?>> searchCriterias;
	private ObservableList<Enum<?>> comboItems;

	private ObjectProperty<MusicCollectionType> type;

	public MusicCollectionType getType() {
		return type.get();
	}

	public void setType(MusicCollectionType type) {
		this.type.set(type);
	}

	private String collectionURL;

	private EntityManager em;
	private VersionService versionService;

	private TuneFileFilter fileFilter = new TuneFileFilter();
	private SearchThread searchThread;
	private Object savedState, searchForValue, recentlySearchedForValue;
	private SearchCriteria<?, ?> recentlySearchedCriteria;
	private boolean searchOptionsChanged;
	private String hvscName;
	private int currentSong;
	private DownloadThread downloadThread;

	private FavoritesSection favoritesToAddSearchResult;

	private ObservableList<TreeItem<File>> currentlyPlayedTreeItems = FXCollections
			.<TreeItem<File>> observableArrayList();

	public MusicCollection(C64Window window, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		util = new UIUtil(window, consolePlayer, player, config, this);
		setContent((Node) util.parse());
	}

	@FXML
	private void initialize() {
		SidPlay2Section sidplay2 = (SidPlay2Section) util.getConfig()
				.getSidplay2();

		int seconds = sidplay2.getPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60,
				seconds % 60));
		sidplay2.playLengthProperty().addListener(
				(observable, oldValue, newValue) -> defaultTime.setText(String
						.format("%02d:%02d", newValue.intValue() / 60,
								newValue.intValue() % 60)));

		enableSldb.setSelected(sidplay2.isEnableDatabase());
		sidplay2.enableDatabaseProperty().addListener(
				(observable, oldValue, newValue) -> enableSldb
						.setSelected(newValue));

		singleSong.setSelected(sidplay2.isSingle());
		sidplay2.singleProperty().addListener(
				(observable, oldValue, newValue) -> singleSong
						.setSelected(newValue));
		util.getConsolePlayer()
				.stateProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							if (newValue == State.RUNNING
									&& util.getPlayer().getTune() != null) {
								Platform.runLater(() ->
								// auto-expand current selected tune
								showNextHit(util.getPlayer().getTune()
										.getInfo().file));
							}
						});
		tuneInfos = FXCollections.<TuneInfo> observableArrayList();
		tuneInfoTable.setItems(tuneInfos);

		SearchScope forward = new SearchScope(util.getBundle().getString(
				"FORWARD"), () -> true);
		SearchScope backward = new SearchScope(util.getBundle().getString(
				"BACKWARD"), () -> false);
		searchScopes = FXCollections.<SearchScope> observableArrayList(forward,
				backward);
		searchScope.setItems(searchScopes);
		searchScope.getSelectionModel().select(forward);

		SearchResult addToFavorites = new SearchResult(util.getBundle()
				.getString("ADD_TO_A_NEW_PLAYLIST"),
				(file) -> createNewFavoritesTab(), (file) -> addFavorite(
						favoritesToAddSearchResult, file));
		SearchResult showNextHit = new SearchResult(util.getBundle().getString(
				"SHOW_NEXT_MATCH"), (file) -> {
		}, (file) -> {
			stopSearch(file);
			Platform.runLater(() -> showNextHit(file));
		});
		searchResults = FXCollections.<SearchResult> observableArrayList(
				addToFavorites, showNextHit);
		searchResult.setItems(searchResults);
		searchResult.getSelectionModel().select(showNextHit);

		searchCriterias = FXCollections
				.<SearchCriteria<?, ?>> observableArrayList();
		searchCriteria.setItems(searchCriterias);
		for (SingularAttribute<? extends Object, ?> singularAttribute : Arrays
				.asList(HVSCEntry_.path, HVSCEntry_.name, HVSCEntry_.title,
						HVSCEntry_.author, HVSCEntry_.released,
						HVSCEntry_.format, HVSCEntry_.playerId,
						HVSCEntry_.noOfSongs, HVSCEntry_.startSong,
						HVSCEntry_.clockFreq, HVSCEntry_.speed,
						HVSCEntry_.sidModel1, HVSCEntry_.sidModel2,
						HVSCEntry_.compatibility, HVSCEntry_.tuneLength,
						HVSCEntry_.audio, HVSCEntry_.sidChipBase1,
						HVSCEntry_.sidChipBase2, HVSCEntry_.driverAddress,
						HVSCEntry_.loadAddress, HVSCEntry_.loadLength,
						HVSCEntry_.initAddress, HVSCEntry_.playerAddress,
						HVSCEntry_.fileDate, HVSCEntry_.fileSizeKb,
						HVSCEntry_.tuneSizeB, HVSCEntry_.relocStartPage,
						HVSCEntry_.relocNoPages, StilEntry_.stilName,
						StilEntry_.stilAuthor, StilEntry_.stilTitle,
						StilEntry_.stilArtist, StilEntry_.stilComment)) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			SearchCriteria<?, ?> criteria = new SearchCriteria(
					singularAttribute);
			searchCriterias.add(criteria);
		}
		searchCriteria.getSelectionModel().select(0);

		comboItems = FXCollections.<Enum<?>> observableArrayList();
		combo.setItems(comboItems);

		fileBrowser.setCellFactory((value) -> {
			return new TreeCell<File>() {
				@Override
				protected void updateItem(File item, boolean empty) {
					super.updateItem(item, empty);
					if (!empty) {
						setText(item.getName());
						setGraphic(getTreeItem().getGraphic());
					} else {
						setText("");
						setGraphic(null);
					}
				}
			};
		});
		fileBrowser.setOnKeyPressed((event) -> {
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			if (event.getCode() == KeyCode.ENTER) {
				if (selectedItem != null
						&& !selectedItem.equals(fileBrowser.getRoot())
						&& selectedItem.getValue().isFile()) {
					playTune(selectedItem.getValue());
				}
			}

		});
		fileBrowser
				.getSelectionModel()
				.selectedItemProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							for (MenuItem item : Arrays.asList(soasc6581R2,
									soasc6581R4, soasc8580R5)) {
								item.setDisable(true);
							}
							if (newValue != null
									&& newValue.getValue().isFile()) {
								File tuneFile = newValue.getValue();
								try {
									SidTune sidTune = SidTune.load(tuneFile);
									showPhoto(sidTune);
									showTuneInfos(tuneFile, sidTune);
									getSOASCURL(sidTune);
								} catch (IOException | SidTuneError e) {
									e.printStackTrace();
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
						playTune(selectedItem.getValue());
					}
				});
		contextMenu.setOnShown((event) -> {
			final TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			showStil.setDisable(selectedItem == null
					|| !((MusicCollectionTreeItem) selectedItem).hasSTIL());
			convertToPSID64.setDisable(selectedItem == null);

			List<FavoritesSection> favorites = ((Configuration) util
					.getConfig()).getFavorites();
			addToFavoritesMenu.getItems().clear();
			for (final FavoritesSection section : favorites) {
				MenuItem item = new MenuItem(section.getName());
				item.setOnAction((event2) -> {
					addFavorites(
							Collections.singletonList(selectedItem.getValue()),
							section);
				});
				addToFavoritesMenu.getItems().add(item);
			}
			addToFavoritesMenu.setDisable(addToFavoritesMenu.getItems()
					.isEmpty());

		});

		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> {
			String initialRoot;
			switch (getType()) {
			case HVSC:
				initialRoot = util.getConfig().getSidplay2().getHvsc();
				break;

			case CGSC:
				initialRoot = util.getConfig().getSidplay2().getCgsc();
				break;

			default:
				throw new RuntimeException("Illegal music collection type: "
						+ type);
			}
			if (initialRoot != null) {
				setRoot(new File(initialRoot));
			}
		});
	}

	private final FileFilter tuneFilter = new TuneFileFilter();

	public void addFavorites(List<File> files, FavoritesSection section) {
		for (int i = 0; files != null && i < files.size(); i++) {
			final File file = files.get(i);
			if (file.isDirectory()) {
				addFavorites(Arrays.asList(file.listFiles()), section);
			} else {
				if (tuneFilter.accept(file)) {
					addFavorite(section, file);
				}
			}
		}
	}

	private void getSOASCURL(SidTune sidTune) {
		if (sidTune != null) {
			final SidTuneInfo tuneInfo = sidTune.getInfo();
			File rootFile = new File(util.getConfig().getSidplay2().getHvsc());
			String name = PathUtils.getCollectionName(new TFile(rootFile),
					tuneInfo.file);
			if (name != null && getType() == MusicCollectionType.HVSC) {
				hvscName = name.replace(".sid", "");
				currentSong = tuneInfo.currentSong;
				for (MenuItem item : Arrays.asList(soasc6581R2, soasc6581R4,
						soasc8580R5)) {
					item.setDisable(false);
				}
			}
		}
	}

	@FXML
	private void showSTIL() {
		MusicCollectionTreeItem selectedItem = (MusicCollectionTreeItem) fileBrowser
				.getSelectionModel().getSelectedItem();
		if (selectedItem != null && selectedItem.hasSTIL()) {
			STILView stilInfo = new STILView(util.getConsolePlayer(),
					util.getPlayer(), util.getConfig());
			stilInfo.setEntry(selectedItem.getStilEntry());
			stilInfo.open();
		}
	}

	@FXML
	private void convertToPSID64() {
		DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		final File directory = fileDialog.showDialog(fileBrowser.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(directory.getAbsolutePath());
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			Psid64 c = new Psid64();
			c.setTmpDir(util.getConfig().getSidplay2().getTmpDir());
			c.setVerbose(true);
			try {
				c.convertFiles(util.getConsolePlayer().getStil(),
						new File[] { selectedItem.getValue() }, directory);
			} catch (NotEnoughC64MemException | IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void startDownload6581R2() {
		final String url = util.getConfig().getOnline().getSoasc6581R2();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void startDownload6581R4() {
		final String url = util.getConfig().getOnline().getSoasc6581R4();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void startDownload8580R5() {
		final String url = util.getConfig().getOnline().getSoasc8580R5();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void doAutoConfiguration() {
		String url;
		switch (getType()) {
		case HVSC:
			url = util.getConfig().getOnline().getHvscUrl();
			break;

		case CGSC:
			url = util.getConfig().getOnline().getCgscUrl();
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
		YesNoDialog dialog = new YesNoDialog(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
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
				.getSidplay2())).getLastDirectoryFolder());
		File directory = fileDialog.showDialog(autoConfiguration.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(directory.getAbsolutePath());
			setRoot(directory);
		}
	}

	@FXML
	private void gotoURL() {
		// Open a browser URL

		// As an application we open the default browser
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(new URL(collectionURL).toURI());
				} catch (final IOException ioe) {
					ioe.printStackTrace();
				} catch (final URISyntaxException urie) {
					urie.printStackTrace();
				}
			}
		} else {
			System.err.println("Awt Desktop is not supported!");
		}
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

	@FXML
	private void doEnableSldb() {
		util.getConfig().getSidplay2()
				.setEnableDatabase(enableSldb.isSelected());
		util.getConsolePlayer().setSongLengthTimer(enableSldb.isSelected());
	}

	@FXML
	private void playSingleSong() {
		util.getConfig().getSidplay2().setSingle(singleSong.isSelected());
		util.getConsolePlayer().setSingle(singleSong.isSelected());
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			util.getConsolePlayer().setDefaultLength(secs);
			util.getConfig().getSidplay2().setPlayLength(secs);
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	public String getCollectionURL() {
		return collectionURL;
	}

	public void setCollectionURL(String collectionURL) {
		this.collectionURL = collectionURL;
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
							rootFile.getParentFile(), type.get().toString()),
							Database.HSQL)).createEntityManager();

			versionService = new VersionService(em);
			collectionDir.setText(rootFile.getAbsolutePath());

			if (rootFile.exists()) {
				SidPlay2Section sidPlay2Section = (SidPlay2Section) util
						.getConfig().getSidplay2();
				if (getType() == MusicCollectionType.HVSC) {
					util.getConfig().getSidplay2()
							.setHvsc(rootFile.getAbsolutePath());
					File theRootFile = sidPlay2Section.getHvscFile();
					setSongLengthDatabase(theRootFile);
					setSTIL(theRootFile);
					fileBrowser.setRoot(new MusicCollectionTreeItem(util
							.getConsolePlayer().getStil(), theRootFile));
				} else if (getType() == MusicCollectionType.CGSC) {
					util.getConfig().getSidplay2()
							.setCgsc(rootFile.getAbsolutePath());
					File theRootFile = sidPlay2Section.getCgscFile();
					fileBrowser.setRoot(new MusicCollectionTreeItem(util
							.getConsolePlayer().getStil(), theRootFile));
				}
				MusicCollectionCellFactory cellFactory = new MusicCollectionCellFactory();
				cellFactory
						.setCurrentlyPlayedTreeItems(currentlyPlayedTreeItems);
				fileBrowser.setCellFactory(cellFactory);
			}

		}
		doResetSearch();
	}

	public void doClose() {
		if (em != null) {
			em.getEntityManagerFactory().close();
		}
	}

	private void setSTIL(File hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				STIL.STIL_FILE))) {
			util.getConsolePlayer().setSTIL(new STIL(hvscRoot, input));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setSongLengthDatabase(File hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				SidDatabase.SONGLENGTHS_FILE))) {
			util.getConsolePlayer().setSidDatabase(new SidDatabase(input));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showPhoto(SidTune sidTune) {
		photograph
				.setImage(sidTune != null && sidTune.getImage() != null ? sidTune
						.getImage() : null);
	}

	private void showTuneInfos(File tuneFile, SidTune sidTune) {
		tuneInfos.clear();
		HVSCEntry entry = HVSCEntry.create(util.getConsolePlayer(),
				tuneFile.getAbsolutePath(), tuneFile, sidTune);

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
				Object value = ((Field) singleAttribute.getJavaMember())
						.get(entry);
				tuneInfo.setValue(String.valueOf(value != null ? value : ""));
			} catch (IllegalArgumentException | IllegalAccessException e) {
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
		if (!forceRecreate) {
			if (!versionService.isExpectedVersion()) {
				forceRecreate = true;
			}
		}

		if (forceRecreate) {
			final File root = fileBrowser.getRoot().getValue();
			searchThread = new SearchIndexerThread(root);
			searchThread.addSearchListener(new ISearchListener() {
				@Override
				public void searchStart() {
					disableSearch();
				}

				@Override
				public void searchHit(File match) {
					Platform.runLater(() -> {
						DoubleProperty progressProperty = util
								.progressProperty(fileBrowser);
						progressProperty.set((progressProperty.get() + 1) % 100);
					});
				}

				@Override
				public void searchStop(boolean canceled) {
					enableSearch();
				}

			});
			searchThread.addSearchListener(new SearchIndexCreator(fileBrowser
					.getRoot().getValue(), util.getConsolePlayer(), em));

			searchThread.start();
		} else {
			searchResult.getSelectionModel().getSelectedItem().getSearchStart()
					.accept(forceRecreate);
			setSearchValue();
			final SearchInIndexThread t = new SearchInIndexThread(em,
					searchScope.getSelectionModel().getSelectedItem()
							.getForward().getAsBoolean()) {
				@Override
				public List<File> getFiles(String filePath) {
					return PathUtils.getFiles(filePath, fileBrowser.getRoot()
							.getValue(), fileFilter);
				}
			};
			t.addSearchListener(new ISearchListener() {
				@Override
				public void searchStart() {
					disableSearch();
				}

				@Override
				public void searchHit(File match) {
					searchResult.getSelectionModel().getSelectedItem()
							.getSearchHit().accept(match);
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
		for (File file : PathUtils.getFiles(filePath, rootFile, fileFilter)) {
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

	private void addFavorite(FavoritesSection section, File file) {
		if (!file.isFile()) {
			// ignore directories
			return;
		}
		try {
			SidTune sidTune = SidTune.load(file);
			HVSCEntry entry = HVSCEntry.create(util.getConsolePlayer(),
					file.getAbsolutePath(), file, sidTune);
			section.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	private void downloadStart(String url) {
		System.out.println("Download URL: <" + url + ">");
		try {
			downloadThread = new DownloadThread(util.getConfig(),
					new ProgressListener(util, fileBrowser) {

						@Override
						public void downloaded(final File downloadedFile) {
							downloadThread = null;

							if (downloadedFile != null) {
								downloadedFile.deleteOnExit();
								Platform.runLater(() -> playTune(downloadedFile));
							}
						}
					}, new URL(url.trim()));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void playTune(final File file) {
		util.setPlayingTab(this);
		try {
			util.getConsolePlayer().playTune(SidTune.load(file), null);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

}