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
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import sidplay.consoleplayer.State;
import sidplay.ini.IniReader;
import ui.common.C64Tab;
import ui.common.TypeTextField;
import ui.common.dialog.YesNoDialog;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
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
public class MusicCollection extends C64Tab implements ISearchListener {

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
			return getBundle().getString(
					attribute.getDeclaringType().getJavaType().getSimpleName()
							+ "." + attribute.getName());
		}
	}

	@FXML
	protected CheckBox autoConfiguration, enableSldb, singleSong;
	@FXML
	private TableView<TuneInfo> tuneInfoTable;
	@FXML
	private ImageView photograph;
	@FXML
	protected TreeView<File> fileBrowser;
	@FXML
	private ComboBox<SearchCriteria<?, ?>> searchCriteria;
	@FXML
	private ComboBox<String> searchScope, searchResult;
	@FXML
	private Button startSearch, stopSearch, resetSearch, createSearchIndex;
	@FXML
	protected TextField collectionDir, defaultTime;
	@FXML
	private TypeTextField stringTextField, integerTextField, longTextField,
			shortTextField;
	@FXML
	private ComboBox<Enum<?>> combo;
	@FXML
	private ContextMenu contextMenu;
	@FXML
	protected MenuItem showStil, convertToPSID64, soasc6581R2, soasc6581R4,
			soasc8580R5;
	@FXML
	protected Menu addToFavorites;

	private ObservableList<TuneInfo> tuneInfos = FXCollections
			.<TuneInfo> observableArrayList();
	private ObservableList<String> searchScopes = FXCollections
			.<String> observableArrayList();
	private ObservableList<String> searchResults = FXCollections
			.<String> observableArrayList();
	private ObservableList<SearchCriteria<?, ?>> searchCriterias = FXCollections
			.<SearchCriteria<?, ?>> observableArrayList();
	private ObservableList<Enum<?>> comboItems = FXCollections
			.<Enum<?>> observableArrayList();

	private MusicCollectionType type;
	private String collectionURL, dbName;

	private EntityManager em;
	private VersionService versionService;

	protected TuneFileFilter fileFilter = new TuneFileFilter();
	private SearchThread searchThread;
	private Object savedState;
	private Object searchForValue, recentlySearchedForValue;
	private SearchCriteria<?, ?> recentlySearchedCriteria;
	private boolean searchOptionsChanged;
	protected String hvscName;
	protected int currentSong;
	protected DownloadThread downloadThread;

	protected FavoritesSection favoritesToAddSearchResult;

	private DoubleProperty progress = new SimpleDoubleProperty();
	protected List<TreeItem<File>> currentlyPlayedTreeItems;

	public DoubleProperty getProgressValue() {
		return progress;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		SidPlay2Section sidplay2 = (SidPlay2Section) getConfig().getSidplay2();

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
		getConsolePlayer().stateProperty().addListener(
				(observable, oldValue, newValue) -> {
					if (newValue == State.RUNNING
							&& getPlayer().getTune() != null) {
						Platform.runLater(() ->
						// auto-expand current selected tune
						showNextHit(getPlayer().getTune().getInfo().file));
					}
				});
		tuneInfoTable.setItems(tuneInfos);

		searchScope.setItems(searchScopes);
		searchScopes.addAll(getBundle().getString("FORWARD"), getBundle()
				.getString("BACKWARD"));
		searchScope.getSelectionModel().select(0);

		searchResult.setItems(searchResults);
		searchResults.addAll(getBundle().getString("SHOW_NEXT_MATCH"),
				getBundle().getString("ADD_TO_A_NEW_PLAYLIST"));
		searchResult.getSelectionModel().select(0);

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

		combo.setItems(comboItems);

		fileBrowser.setCellFactory((value) -> {
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

			List<FavoritesSection> favorites = ((Configuration) getConfig())
					.getFavorites();
			addToFavorites.getItems().clear();
			for (final FavoritesSection section : favorites) {
				MenuItem item = new MenuItem(section.getName());
				item.setOnAction((event2) -> {
					addFavorites(
							Collections.singletonList(selectedItem.getValue()),
							section);
				});
				addToFavorites.getItems().add(item);
			}
			addToFavorites.setDisable(addToFavorites.getItems().isEmpty());

		});

		String initialRoot;
		switch (type) {
		case HVSC:
			initialRoot = getConfig().getSidplay2().getHvsc();
			break;

		case CGSC:
			initialRoot = getConfig().getSidplay2().getCgsc();
			break;

		default:
			throw new RuntimeException("Illegal music collection type: " + type);
		}
		if (initialRoot != null) {
			setRoot(new File(initialRoot));
		}
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
			File rootFile = new File(getConfig().getSidplay2().getHvsc());
			String name = PathUtils.getCollectionName(new TFile(rootFile),
					tuneInfo.file);
			if (name != null) {
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
		TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
				.getSelectedItem();
		STILView stilInfo = new STILView();
		stilInfo.setPlayer(getPlayer());
		stilInfo.setConfig(getConfig());
		STIL stil = getConsolePlayer().getStil();
		if (stil != null) {
			File hvscFile = selectedItem.getValue();
			stilInfo.setEntry(stil.getSTILEntry(hvscFile));
		}
		try {
			stilInfo.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void convertToPSID64() {
		DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		final File directory = fileDialog.showDialog(fileBrowser.getScene()
				.getWindow());
		if (directory != null) {
			getConfig().getSidplay2().setLastDirectory(
					directory.getAbsolutePath());
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel()
					.getSelectedItem();
			Psid64 c = new Psid64();
			c.setTmpDir(getConfig().getSidplay2().getTmpDir());
			c.setVerbose(true);
			try {
				c.convertFiles(getConsolePlayer().getStil(),
						new File[] { selectedItem.getValue() }, directory);
			} catch (NotEnoughC64MemException | IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void startDownload6581R2() {
		final String url = getConfig().getOnline().getSoasc6581R2();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void startDownload6581R4() {
		final String url = getConfig().getOnline().getSoasc6581R4();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void startDownload8580R5() {
		final String url = getConfig().getOnline().getSoasc8580R5();
		downloadStart(MessageFormat.format(url, hvscName, currentSong));
	}

	@FXML
	private void doAutoConfiguration() {
		String url;
		switch (type) {
		case HVSC:
			url = getConfig().getOnline().getHvscUrl();
			break;

		case CGSC:
			url = getConfig().getOnline().getCgscUrl();
			break;

		default:
			throw new RuntimeException("Illegal music collection type: " + type);
		}
		if (autoConfiguration.isSelected()) {
			autoConfiguration.setDisable(true);
			try {
				DownloadThread downloadThread = new DownloadThread(getConfig(),
						new ProgressListener(progress) {

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
		YesNoDialog dialog = new YesNoDialog();
		dialog.setTitle(getBundle().getString("CREATE_SEARCH_DATABASE"));
		dialog.setText(String.format(
				getBundle().getString("RECREATE_DATABASE"), dbName));
		dialog.getConfirmed().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				startSearch(true);
			}
		});
		try {
			dialog.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void doBrowse() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		File directory = fileDialog.showDialog(autoConfiguration.getScene()
				.getWindow());
		if (directory != null) {
			getConfig().getSidplay2().setLastDirectory(
					directory.getAbsolutePath());
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
		getConfig().getSidplay2().setEnableDatabase(enableSldb.isSelected());
		getConsolePlayer().setSLDb(enableSldb.isSelected());
	}

	@FXML
	private void playSingleSong() {
		getConfig().getSidplay2().setSingle(singleSong.isSelected());
		getConsolePlayer().getTrack().setSingle(singleSong.isSelected());
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			getConsolePlayer().getTimer().setDefaultLength(secs);
			getConfig().getSidplay2().setPlayLength(secs);
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_FORMAT"));
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

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public MusicCollectionType getType() {
		return type;
	}

	public void setType(MusicCollectionType type) {
		this.type = type;
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

	protected void setRoot(final File rootFile) {
		if (rootFile.exists()) {
			if (em != null) {
				em.getEntityManagerFactory().close();
			}
			em = Persistence
					.createEntityManagerFactory(
							PersistenceProperties.COLLECTION_DS,
							new PersistenceProperties(new File(rootFile
									.getParentFile(), dbName))).createEntityManager();

			versionService = new VersionService(em);
			collectionDir.setText(rootFile.getAbsolutePath());

			if (rootFile.exists()) {
				SidPlay2Section sidPlay2Section = (SidPlay2Section) getConfig()
						.getSidplay2();
				if (type == MusicCollectionType.HVSC) {
					getConfig().getSidplay2().setHvsc(
							rootFile.getAbsolutePath());
					File theRootFile = sidPlay2Section.getHvscFile();
					setSongLengthDatabase(theRootFile);
					setSTIL(theRootFile);
					fileBrowser.setRoot(new MusicCollectionTreeItem(this,
							getConsolePlayer().getStil(), theRootFile));
				} else if (type == MusicCollectionType.CGSC) {
					getConfig().getSidplay2().setCgsc(
							rootFile.getAbsolutePath());
					File theRootFile = sidPlay2Section.getCgscFile();
					fileBrowser.setRoot(new MusicCollectionTreeItem(this,
							getConsolePlayer().getStil(), theRootFile));
				}
				fileBrowser.setCellFactory(new MusicCollectionCellFactory());
			}

		}
		doResetSearch();
	}

	public void doCloseWindow() {
		if (em != null) {
			em.getEntityManagerFactory().close();
		}
	}

	private void setSTIL(File hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				STIL.STIL_FILE))) {
			getConsolePlayer().setSTIL(new STIL(hvscRoot, input));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setSongLengthDatabase(File hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				SidDatabase.SONGLENGTHS_FILE))) {
			getConsolePlayer().setSidDatabase(new SidDatabase(input));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void showPhoto(SidTune sidTune) {
		photograph
				.setImage(sidTune != null && sidTune.getImage() != null ? sidTune
						.getImage() : null);
	}

	protected void showTuneInfos(File tuneFile, SidTune sidTune) {
		tuneInfos.clear();
		HVSCEntry entry = HVSCEntry.create(getConsolePlayer(),
				tuneFile.getAbsolutePath(), tuneFile, sidTune);

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())) {
				continue;
			}
			if (!(SingularAttribute.class.isAssignableFrom(field.getType()))) {
				continue;
			}
			TuneInfo tuneInfo = new TuneInfo();
			String name = getBundle().getString(
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

	@Override
	public void searchStart() {
		Platform.runLater(() -> {
			startSearch.setDisable(true);
			stopSearch.setDisable(false);
			resetSearch.setDisable(true);
			createSearchIndex.setDisable(true);
		});
	}

	@Override
	public void searchStop(final boolean canceled) {
		// remember search state
		savedState = searchThread.getSearchState();
		progress.set(0);

		Platform.runLater(() -> {
			startSearch.setDisable(false);
			stopSearch.setDisable(true);
			resetSearch.setDisable(false);
			createSearchIndex.setDisable(false);
		});
	}

	@Override
	public void searchHit(final File current) {
		if (!current.isFile()) {
			// ignore directories
			return;
		}
		if (searchThread instanceof SearchInIndexThread
				&& searchResult.getSelectionModel().isSelected(0)) {
			searchThread.setAborted(true);
			searchStop(true);
		}
		Platform.runLater(() -> {
			if (searchThread instanceof SearchIndexerThread) {
				// search index is created
				progress.set((progress.get() + 1) % 100);
			} else {
				switch (searchResult.getSelectionModel().getSelectedIndex()) {
				case 1:
					addFavorite(favoritesToAddSearchResult, current);
					break;

				default:
					showNextHit(current);
					break;
				}
			}
		});
	}

	protected void addFavorite(FavoritesSection section, File file) {
		try {
			SidTune sidTune = SidTune.load(file);
			HVSCEntry entry = HVSCEntry.create(getConsolePlayer(),
					file.getAbsolutePath(), file, sidTune);
			section.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	protected void showNextHit(final File matchFile) {
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
			currentlyPlayedTreeItems = pathSegs;
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

	protected void startSearch(boolean forceRecreate) {
		if (searchThread != null && searchThread.isAlive()) {
			return;
		}

		if (!new File(collectionDir.getText()).exists()) {
			return;
		}

		progress.set(0);

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
			searchThread.addSearchListener(this);
			searchThread.addSearchListener(new SearchIndexCreator(fileBrowser
					.getRoot().getValue(), getConsolePlayer(), em));

			searchThread.start();
		} else {
			switch (searchResult.getSelectionModel().getSelectedIndex()) {
			case 1:
				// Add result to favorites?
				// Create new favorites tab
				List<FavoritesSection> favorites = ((Configuration) getConfig())
						.getFavorites();
				FavoritesSection newFavorites = new FavoritesSection();
				newFavorites.setName(getBundle().getString("NEW_TAB"));
				favoritesToAddSearchResult = newFavorites;
				favorites.add(newFavorites);
				break;

			default:
				break;
			}

			setSearchValue();
			final SearchInIndexThread t = new SearchInIndexThread(em,
					searchScope.getSelectionModel().getSelectedIndex() != 1) {
				@Override
				public List<File> getFiles(String filePath) {
					return PathUtils.getFiles(filePath, fileBrowser.getRoot()
							.getValue(), fileFilter);
				}
			};
			t.addSearchListener(this);
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

	private void downloadStart(String url) {
		System.out.println("Download URL: <" + url + ">");
		try {
			downloadThread = new DownloadThread(getConfig(),
					new ProgressListener(progress) {

						@Override
						public void downloaded(final File downloadedFile) {
							downloadThread = null;

							if (downloadedFile != null) {
								Platform.runLater(() -> playTune(downloadedFile));
							}
						}
					}, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	protected void playTune(final File file) {
		setPlayedGraphics(fileBrowser);
		try {
			getConsolePlayer().playTune(SidTune.load(file), null);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	public List<TreeItem<File>> getCurrentlyPlayedTreeItems() {
		return currentlyPlayedTreeItems;
	}
}