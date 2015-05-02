package ui.favorites;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;

import javax.persistence.metamodel.SingularAttribute;

import libpsid64.Psid64;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.config.Configuration;
import ui.entities.config.FavoriteColumn;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileFilter;
import ui.stilview.STILView;

import com.sun.javafx.scene.control.skin.TableColumnHeader;

import de.schlichtherle.truezip.file.TFile;

public class FavoritesTab extends Tab implements UIPart {

	@FXML
	private TextField filterField;
	@FXML
	private TableView<HVSCEntry> favoritesTable;
	@FXML
	private Menu addColumnMenu, moveToTab, copyToTab;
	@FXML
	private MenuItem showStil, removeColumn;
	@FXML
	private Button moveUp, moveDown;
	@FXML
	private ContextMenu contextMenuHeader, contextMenu;

	private UIUtil util;

	private ObservableList<HVSCEntry> filteredFavorites;

	private FileFilter tuneFilter = new TuneFileFilter();
	private FavoritesSection favoritesSection;

	private ObjectProperty<HVSCEntry> currentlyPlayedHVSCEntryProperty;
	private Favorites favorites;

	public FavoritesTab(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
	}

	@SuppressWarnings("rawtypes")
	@FXML
	private void initialize() {
		filteredFavorites = FXCollections.<HVSCEntry> observableArrayList();
		favoritesTable.setItems(filteredFavorites);
		favoritesTable.getSelectionModel().setSelectionMode(
				SelectionMode.MULTIPLE);
		favoritesTable.getColumns().addListener(
				(Change<? extends TableColumn<HVSCEntry, ?>> change) -> {
					while (change.next()) {
						if (change.wasReplaced()) {
							moveColumn();
						}
					}
				});
		favoritesTable
				.getSelectionModel()
				.selectedIndexProperty()
				.addListener((observable, oldValue, newValue) -> {
					if (newValue != null && newValue.intValue() != -1) {
						// Save last selected row
						favoritesSection.setSelectedRowFrom(newValue.intValue());
						favoritesSection.setSelectedRowTo(newValue.intValue());

					}
					moveUp.setDisable(newValue == null
							|| newValue.intValue() == 0
							|| favoritesTable.getSortOrder().size() > 0);
					moveDown.setDisable(newValue == null
							|| newValue.intValue() == favoritesSection
									.getFavorites().size() - 1
							|| favoritesTable.getSortOrder().size() > 0);
				});
		favoritesTable
				.setOnMousePressed((event) -> {
					final HVSCEntry hvscEntry = favoritesTable
							.getSelectionModel().getSelectedItem();
					SidPlay2Section sidPlay2Section = (SidPlay2Section) util
							.getConfig().getSidplay2();
					if (hvscEntry != null
							&& PathUtils.getFile(hvscEntry.getPath(),
									sidPlay2Section.getHvscFile(),
									sidPlay2Section.getCgscFile()) != null
							&& event.isPrimaryButtonDown()
							&& event.getClickCount() > 1) {
						playTune(hvscEntry);
						favoritesTable.scrollTo(hvscEntry);
					}
				});
		favoritesTable.setOnKeyPressed((event) -> {
			final HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
					.getSelectedItem();
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2();
			if (event.getCode() == KeyCode.ENTER
					&& hvscEntry != null
					&& PathUtils.getFile(hvscEntry.getPath(),
							sidPlay2Section.getHvscFile(),
							sidPlay2Section.getCgscFile()) != null) {
				playTune(hvscEntry);
			}
			if (event.getCode() == KeyCode.DELETE) {
				removeSelectedFavorites();
			}
		});
		filterField.setOnKeyReleased((event) -> {
			filter(filterField.getText());
		});

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())
					|| !(SingularAttribute.class.isAssignableFrom(field
							.getType()))) {
				continue;
			}
			try {
				SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
						.get(null);
				addAddColumnHeaderMenuItem(addColumnMenu, singleAttribute);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		contextMenuHeader.setOnShown((event) -> {
			TableColumnBase tableColumn = getContextMenuColumn();
			// never remove the first column
				removeColumn.setDisable(favoritesTable.getColumns().indexOf(
						tableColumn) == 0);
			});

		contextMenu
				.setOnShown((event) -> {
					HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
							.getSelectedItem();
					showStil.setDisable(hvscEntry == null
							|| util.getPlayer().getStilEntry(
									hvscEntry.getPath()) == null);
					List<Tab> tabs = favorites.getFavoriteTabs();
					moveToTab.getItems().clear();
					copyToTab.getItems().clear();
					for (final Tab tab : tabs) {
						if (tab.equals(FavoritesTab.this)) {
							continue;
						}
						final String name = tab.getText();
						MenuItem moveToTabItem = new MenuItem(name);
						moveToTabItem
								.setOnAction((event2) -> {
									ObservableList<HVSCEntry> selectedItems = favoritesTable
											.getSelectionModel()
											.getSelectedItems();
									copyToTab(selectedItems, (FavoritesTab) tab);
									removeFavorites(selectedItems);
								});
						moveToTab.getItems().add(moveToTabItem);
						MenuItem copyToTabItem = new MenuItem(name);
						copyToTabItem
								.setOnAction((event2) -> {
									ObservableList<HVSCEntry> selectedItems = favoritesTable
											.getSelectionModel()
											.getSelectedItems();
									copyToTab(selectedItems, (FavoritesTab) tab);
								});
						copyToTab.getItems().add(copyToTabItem);
					}
					moveToTab.setDisable(moveToTab.getItems().size() == 0);
					copyToTab.setDisable(copyToTab.getItems().size() == 0);
				});

		currentlyPlayedHVSCEntryProperty = new SimpleObjectProperty<HVSCEntry>();
		for (TableColumn column : favoritesTable.getColumns()) {
			FavoritesCellFactory cellFactory = (FavoritesCellFactory) column
					.getCellFactory();
			cellFactory.setPlayer(util.getPlayer());
			cellFactory
					.setCurrentlyPlayedHVSCEntryProperty(currentlyPlayedHVSCEntryProperty);
		}
	}

	@FXML
	private void doMoveUp() {
		int from = favoritesTable.getSelectionModel().getSelectedIndex();
		if (from == -1) {
			return;
		}
		moveRow(from, from - 1);
	}

	@FXML
	private void doMoveDown() {
		int from = favoritesTable.getSelectionModel().getSelectedIndex();
		if (from == -1) {
			return;
		}
		moveRow(from, from + 1);
	}

	@SuppressWarnings({ "rawtypes" })
	@FXML
	private void removeColumn() {
		TableColumnBase tableColumn = getContextMenuColumn();
		FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
				.getUserData();
		favoritesTable.getColumns().remove(tableColumn);
		favoritesSection.getColumns().remove(favoriteColumn);
	}

	@FXML
	private void exportToDir() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		File directory = fileDialog.showDialog(favoritesTable.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(directory.getAbsolutePath());
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2();
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel()
					.getSelectedItems()) {
				File file = PathUtils.getFile(hvscEntry.getPath(),
						sidPlay2Section.getHvscFile(),
						sidPlay2Section.getCgscFile());
				String name = file.getName();
				copyToUniqueName(file, directory, name, 1);
			}
		}
	}

	private void copyToUniqueName(File file, File directory, String name,
			int number) {
		String newName = name;
		if (number > 1) {
			newName = PathUtils.getBaseNameNoExt(name) + "_" + number
					+ PathUtils.getExtension(name);
		}
		File newFile = new File(directory, newName);
		if (newFile.exists()) {
			copyToUniqueName(file, directory, name, ++number);
		} else {
			try {
				TFile.cp(file, newFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void showStil() {
		HVSCEntry hvscEntry = favoritesTable.getSelectionModel()
				.getSelectedItem();
		if (hvscEntry == null) {
			return;
		}
		STILView stilInfo = new STILView(util.getPlayer());
		stilInfo.setEntry(util.getPlayer().getStilEntry(hvscEntry.getPath()));
		stilInfo.open();
	}

	@FXML
	private void convertToPsid64() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		File directory = fileDialog.showDialog(favoritesTable.getScene()
				.getWindow());
		if (directory != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(directory.getAbsolutePath());
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2();
			final ArrayList<File> files = new ArrayList<File>();
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel()
					.getSelectedItems()) {
				File file = PathUtils.getFile(hvscEntry.getPath(),
						sidPlay2Section.getHvscFile(),
						sidPlay2Section.getCgscFile());
				files.add(file);
			}
			Psid64 c = new Psid64();
			c.setTmpDir(util.getConfig().getSidplay2().getTmpDir());
			c.setVerbose(false);
			try {
				c.convertFiles(util.getPlayer(), files.toArray(new File[0]),
						directory, sidPlay2Section.getHvscFile());
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}

	}

	FavoritesSection getFavoritesSection() {
		return favoritesSection;
	}

	void addFavorites(List<File> files) {
		for (int i = 0; files != null && i < files.size(); i++) {
			final File file = files.get(i);
			if (file.isDirectory()) {
				addFavorites(Arrays.asList(file.listFiles()));
			} else {
				if (tuneFilter.accept(file)) {
					addFavorite(file);
				}
			}
		}
	}

	void restoreColumns(final FavoritesSection favoritesSection) {
		this.favoritesSection = favoritesSection;
		setText(favoritesSection.getName());
		filteredFavorites.addAll(favoritesSection.getFavorites());

		// Restore persisted columns
		for (FavoriteColumn favoriteColumn : favoritesSection.getColumns()) {
			try {
				SingularAttribute<?, ?> attribute = getAttribute(favoriteColumn
						.getColumnProperty());
				addColumn(attribute, favoriteColumn);
			} catch (IllegalArgumentException | NoSuchFieldException
					| IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		Iterator<TableColumn<HVSCEntry, ?>> columnsIt = favoritesTable
				.getColumns().iterator();
		TableColumn<HVSCEntry, ?> pathColumn = columnsIt.next();
		pathColumn.widthProperty().addListener(
				(observable, oldValue, newValue) -> {
					favoritesSection.setWidth(newValue.doubleValue());
				});
		Double width = favoritesSection.getWidth();
		if (width != null) {
			pathColumn.setPrefWidth(width.doubleValue());
		}
		for (FavoriteColumn favoriteColumn : favoritesSection.getColumns()) {
			TableColumn<HVSCEntry, ?> column = columnsIt.next();
			width = favoriteColumn.getWidth();
			if (width != null) {
				column.setPrefWidth(width.doubleValue());
			}
		}
		favoritesSection.getObservableFavorites().addListener(
				(ListChangeListener.Change<? extends HVSCEntry> change) -> {
					while (change.next()) {
						if (change.wasPermutated() || change.wasUpdated()) {
							continue;
						}
						if (change.wasAdded()) {
							filteredFavorites.addAll(change.getAddedSubList());
						} else if (change.wasRemoved()) {
							filteredFavorites.removeAll(change.getRemoved());
						}
					}
				});
		// Initially select last selected row
		Integer from = favoritesSection.getSelectedRowFrom();
		if (from != null && from != -1) {
			favoritesTable.getSelectionModel().select(from);
			HVSCEntry hvscEntry = favoritesSection.getFavorites().get(from);
			favoritesTable.scrollTo(hvscEntry);
		}
	}

	void removeSelectedFavorites() {
		removeFavorites(favoritesTable.getSelectionModel().getSelectedItems());
	}

	void removeAllFavorites() {
		favoritesSection.getFavorites().clear();
		((Configuration) util.getConfig()).getFavorites().remove(
				favoritesSection);
	}

	void filter(String filterText) {
		filteredFavorites.clear();
		if (filterText.trim().length() == 0) {
			filteredFavorites.addAll(favoritesSection.getFavorites());
		} else {
			outer: for (HVSCEntry hvscEntry : favoritesSection.getFavorites()) {
				for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable
						.getColumns()) {
					FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
							.getUserData();
					String name = favoriteColumn != null ? favoriteColumn
							.getColumnProperty() : HVSCEntry_.path.getName();
					try {
						Object value = ((Method) getAttribute(name)
								.getJavaMember()).invoke(hvscEntry);
						String text = value != null ? value.toString() : "";
						if (text.contains(filterText)) {
							filteredFavorites.add(hvscEntry);
							continue outer;
						}
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchFieldException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	void selectAllFavorites() {
		favoritesTable.getSelectionModel().selectAll();
	}

	void clearSelection() {
		favoritesTable.getSelectionModel().clearSelection();
	}

	void loadFavorites(File favoritesFile) throws IOException {
		favoritesFile = addFileExtension(favoritesFile);
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(favoritesFile), "ISO-8859-1"))) {
			String line;
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2();
			while ((line = r.readLine()) != null) {
				if (line.startsWith("<HVSC>/") || line.startsWith("<CGSC>/")) {
					// backward compatibility
					line = line.substring(7);
				}
				File file = PathUtils.getFile(line,
						sidPlay2Section.getHvscFile(),
						sidPlay2Section.getCgscFile());
				if (file != null) {
					addFavorite(file);
				}
			}
		}
	}

	void saveFavorites(File favoritesFile) throws IOException {
		favoritesFile = addFileExtension(favoritesFile);
		try (PrintStream p = new PrintStream(favoritesFile)) {
			for (HVSCEntry hvscEntry : favoritesTable.getItems()) {
				p.println(new TFile(hvscEntry.getPath()).getPath());
			}
		}
	}

	void playNext() {
		boolean recentlyPlayedFound = false;
		for (HVSCEntry hvscEntry : favoritesSection.getFavorites()) {
			if (recentlyPlayedFound) {
				playTune(hvscEntry);
				break;
			}
			if (hvscEntry == currentlyPlayedHVSCEntryProperty.get()) {
				recentlyPlayedFound = true;
			}
		}
	}

	void playNextRandom() {
		if (favoritesSection.getFavorites().size() == 0) {
			return;
		}
		HVSCEntry hvscEntry = favoritesSection.getFavorites().get(
				Math.abs(new Random().nextInt(Integer.MAX_VALUE))
						% favoritesSection.getFavorites().size());
		playTune(hvscEntry);
	}

	private File addFileExtension(File favoritesFile) {
		String extension = FavoritesExtension.EXTENSION;
		if (extension.startsWith("*")) {
			extension = extension.substring(1);
		}
		if (!favoritesFile.getName().endsWith(extension)) {
			favoritesFile = new File(favoritesFile.getParentFile(),
					favoritesFile.getName() + extension);
		}
		return favoritesFile;
	}

	private void addFavorite(File file) {
		SidTune tune;
		try {
			tune = SidTune.load(file);
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getPlayer().getConfig().getSidplay2();
			String collectionName = PathUtils.getCollectionName(
					sidPlay2Section.getHvscFile(), file.getPath());
			HVSCEntry entry = new HVSCEntry(() -> util.getPlayer()
					.getSidDatabaseInfo(db -> db.getFullSongLength(tune)),
					collectionName, file, tune);
			favoritesSection.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	void removeFavorites(ObservableList<HVSCEntry> selectedItems) {
		favoritesSection.getFavorites().removeAll(selectedItems);
		filter(filterField.getText());
	}

	private SingularAttribute<?, ?> getAttribute(String columnProperty)
			throws NoSuchFieldException, IllegalAccessException {
		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())) {
				continue;
			}
			if (!(SingularAttribute.class.isAssignableFrom(field.getType()))) {
				continue;
			}
			if (columnProperty.equals(field.getName())) {
				return (SingularAttribute<?, ?>) field.get(null);
			}
		}
		return null;
	}

	TableColumnBase<?, ?> getContextMenuColumn() {
		TableColumnHeader columnHeader = (TableColumnHeader) contextMenuHeader
				.getOwnerNode();
		return columnHeader.getTableColumn();
	}

	private void addAddColumnHeaderMenuItem(Menu addColumnMenu,
			final SingularAttribute<?, ?> attribute) {
		MenuItem menuItem = new MenuItem();
		menuItem.setText(attribute.getName());
		menuItem.setOnAction((event) -> {
			FavoriteColumn favoriteColumn = new FavoriteColumn();
			favoriteColumn.setColumnProperty(attribute.getName());
			favoritesSection.getColumns().add(favoriteColumn);
			addColumn(attribute, favoriteColumn);
		});
		addColumnMenu.getItems().add(menuItem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void addColumn(SingularAttribute<?, ?> attribute,
			final FavoriteColumn favoriteColumn) {
		String text = util.getBundle().getString(
				attribute.getDeclaringType().getJavaType().getSimpleName()
						+ "." + attribute.getName());
		TableColumn tableColumn = new TableColumn();
		tableColumn.setUserData(favoriteColumn);
		tableColumn.setText(text);
		tableColumn.setCellValueFactory(new PropertyValueFactory(attribute
				.getName()));
		FavoritesCellFactory cellFactory = new FavoritesCellFactory();
		cellFactory.setPlayer(util.getPlayer());
		cellFactory
				.setCurrentlyPlayedHVSCEntryProperty(currentlyPlayedHVSCEntryProperty);
		tableColumn.setCellFactory(cellFactory);
		tableColumn.setContextMenu(contextMenuHeader);
		tableColumn.widthProperty().addListener(
				(observable, oldValue, newValue) -> {
					favoriteColumn.setWidth(newValue.doubleValue());
				});
		favoritesTable.getColumns().add(tableColumn);
	}

	void moveColumn() {
		Collection<FavoriteColumn> newOrderList = new ArrayList<FavoriteColumn>();
		for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable
				.getColumns()) {
			FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn
					.getUserData();
			if (favoriteColumn != null) {
				newOrderList.add(favoriteColumn);
			}
		}
		favoritesSection.getColumns().clear();
		favoritesSection.getColumns().addAll(newOrderList);
	}

	void moveRow(int from, int to) {
		Collections.swap(favoritesSection.getFavorites(), from, to);
		filter(filterField.getText());
		favoritesTable.getSelectionModel().select(to);
	}

	void copyToTab(final List<HVSCEntry> toCopy, final FavoritesTab tab) {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig()
				.getSidplay2();
		for (HVSCEntry hvscEntry : toCopy) {
			tab.addFavorite(PathUtils.getFile(hvscEntry.getPath(),
					sidPlay2Section.getHvscFile(),
					sidPlay2Section.getCgscFile()));
		}
	}

	void playTune(final HVSCEntry hvscEntry) {
		favorites.setCurrentlyPlayedFavorites(this);
		SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig()
				.getSidplay2();
		util.setPlayingTab(this);
		try {
			File file = PathUtils.getFile(hvscEntry.getPath(),
					sidPlay2Section.getHvscFile(),
					sidPlay2Section.getCgscFile());
			util.getPlayer().play(SidTune.load(file));
			currentlyPlayedHVSCEntryProperty.set(hvscEntry);
			favoritesTable.scrollTo(hvscEntry);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	public void setFavorites(Favorites favorites) {
		this.favorites = favorites;
	}

}
