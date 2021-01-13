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

import javax.persistence.metamodel.SingularAttribute;

import de.schlichtherle.truezip.file.TFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.psid64.Psid64;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.filefilter.FavoritesExtension;
import ui.common.filefilter.TuneFileFilter;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.entities.config.FavoriteColumn;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.stilview.STILView;

public class FavoritesTab extends C64VBox implements UIPart {

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
	private ContextMenu contextMenu;

	private ObservableList<HVSCEntry> filteredFavorites;

	private FileFilter tuneFilter = new TuneFileFilter();
	private FavoritesSection favoritesSection;

	private ObjectProperty<HVSCEntry> currentlyPlayedHVSCEntryProperty;
	private Favorites favorites;
	private int selectedColumn;

	public FavoritesTab() {
		super();
	}

	public FavoritesTab(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	@SuppressWarnings("rawtypes")
	protected void initialize() {
		filteredFavorites = FXCollections.<HVSCEntry>observableArrayList();
		SortedList<HVSCEntry> sortedList = new SortedList<>(filteredFavorites);
		sortedList.comparatorProperty().bind(favoritesTable.comparatorProperty());
		favoritesTable.setItems(sortedList);
		favoritesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		favoritesTable.getColumns().addListener((Change<? extends TableColumn<HVSCEntry, ?>> change) -> {
			while (change.next()) {
				if (change.wasReplaced()) {
					moveColumn();
				}
			}
		});
		favoritesTable.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue.intValue() != -1) {
				favoritesSection.setSelectedRowFrom(newValue.intValue());
				favoritesSection.setSelectedRowTo(newValue.intValue());

			}
			moveUp.setDisable(newValue == null || newValue.intValue() == 0 || favoritesTable.getSortOrder().size() > 0);
			moveDown.setDisable(newValue == null || newValue.intValue() == favoritesSection.getFavorites().size() - 1
					|| favoritesTable.getSortOrder().size() > 0);
		});
		favoritesTable.setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				final HVSCEntry hvscEntry = favoritesTable.getSelectionModel().getSelectedItem();
				if (getHVSCFile(hvscEntry) != null) {
					playTune(hvscEntry);
					favoritesTable.scrollTo(hvscEntry);
				}
			}
		});
		favoritesTable.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				final HVSCEntry hvscEntry = favoritesTable.getSelectionModel().getSelectedItem();
				if (getHVSCFile(hvscEntry) != null) {
					playTune(hvscEntry);
				}
			} else if (event.getCode() == KeyCode.DELETE) {
				removeSelectedFavorites();
			}
		});
		filterField.setOnKeyReleased(event -> filter(filterField.getText()));

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())
					|| !SingularAttribute.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field.get(null);
				addAddColumnHeaderMenuItem(addColumnMenu, singleAttribute);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		contextMenu.setOnShown(event -> {
			HVSCEntry hvscEntry = favoritesTable.getSelectionModel().getSelectedItem();
			showStil.setDisable(hvscEntry == null || util.getPlayer().getStilEntry(hvscEntry.getPath()) == null);
			List<Tab> tabs = favorites.getFavoriteTabs();
			moveToTab.getItems().clear();
			copyToTab.getItems().clear();
			for (final Tab tab : tabs) {
				if (tab.equals(favorites.getSelectedTab())) {
					continue;
				}
				final String name = tab.getText();
				MenuItem moveToTabItem = new MenuItem(name);
				moveToTabItem.setOnAction(event2 -> {
					ObservableList<HVSCEntry> selectedItems = favoritesTable.getSelectionModel().getSelectedItems();
					copyToTab(selectedItems, (FavoritesTab) tab.getContent());
					removeFavorites(selectedItems);
				});
				moveToTab.getItems().add(moveToTabItem);
				MenuItem copyToTabItem = new MenuItem(name);
				copyToTabItem.setOnAction(event2 -> {
					ObservableList<HVSCEntry> selectedItems = favoritesTable.getSelectionModel().getSelectedItems();
					copyToTab(selectedItems, (FavoritesTab) tab.getContent());
				});
				copyToTab.getItems().add(copyToTabItem);
			}
			moveToTab.setDisable(moveToTab.getItems().isEmpty());
			copyToTab.setDisable(copyToTab.getItems().isEmpty());
			// never remove the first column
			removeColumn.setDisable(true);
			for (TablePosition tablePosition : favoritesTable.getSelectionModel().getSelectedCells()) {
				selectedColumn = tablePosition.getColumn();
				removeColumn.setDisable(favoritesTable.getSelectionModel().getSelectedCells().size() != 1
						|| tablePosition.getColumn() <= 0);
				break;
			}
			if (selectedColumn > 0 && selectedColumn < favoritesTable.getColumns().size()) {
				TableColumn tableColumn = favoritesTable.getColumns().get(selectedColumn);
				removeColumn.setText(String.format(util.getBundle().getString("REMOVE_COLUMN"), tableColumn.getText()));
			}
		});

		currentlyPlayedHVSCEntryProperty = new SimpleObjectProperty<>();
		for (TableColumn column : favoritesTable.getColumns()) {
			FavoritesCellFactory cellFactory = (FavoritesCellFactory) column.getCellFactory();
			cellFactory.setPlayer(util.getPlayer());
			cellFactory.setCurrentlyPlayedHVSCEntryProperty(currentlyPlayedHVSCEntryProperty);
		}
	}

	private File getHVSCFile(HVSCEntry hvscEntry) {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		return hvscEntry != null
				? PathUtils.getFile(hvscEntry.getPath(), sidPlay2Section.getHvsc(), sidPlay2Section.getCgsc())
				: null;
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

	@FXML
	private void removeColumn() {
		if (selectedColumn < 0) {
			return;
		}
		TableColumn<HVSCEntry, ?> tableColumn = favoritesTable.getColumns().get(selectedColumn);
		FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn.getUserData();
		favoritesTable.getColumns().remove(tableColumn);
		favoritesSection.getColumns().remove(favoriteColumn);
	}

	@FXML
	private void exportToDir() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(sidplay2Section.getLastDirectoryFolder());
		File directory = fileDialog.showDialog(favoritesTable.getScene().getWindow());
		if (directory != null) {
			sidplay2Section.setLastDirectory(directory.getAbsolutePath());
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel().getSelectedItems()) {
				File file = getHVSCFile(hvscEntry);
				copyToUniqueName(file, directory, file.getName(), 1);
			}
		}
	}

	private void copyToUniqueName(File file, File directory, String name, int number) {
		String newName = name;
		if (number > 1) {
			newName = PathUtils.getFilenameWithoutSuffix(name) + "_" + number + PathUtils.getFilenameSuffix(name);
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
		HVSCEntry hvscEntry = favoritesTable.getSelectionModel().getSelectedItem();
		if (hvscEntry == null) {
			return;
		}
		STILView stilInfo = new STILView(util.getPlayer());
		stilInfo.setEntry(util.getPlayer().getStilEntry(hvscEntry.getPath()));
		stilInfo.open();
	}

	@FXML
	private void convertToPsid64() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(sidPlay2Section.getLastDirectoryFolder());
		File directory = fileDialog.showDialog(favoritesTable.getScene().getWindow());
		if (directory != null) {
			sidPlay2Section.setLastDirectory(directory.getAbsolutePath());
			final ArrayList<File> files = new ArrayList<>();
			for (HVSCEntry hvscEntry : favoritesTable.getSelectionModel().getSelectedItems()) {
				files.add(getHVSCFile(hvscEntry));
			}
			Psid64 c = new Psid64();
			c.setTmpDir(sidPlay2Section.getTmpDir());
			c.setVerbose(false);
			try {
				c.convertFiles(util.getPlayer(), files.toArray(new File[0]), directory, sidPlay2Section.getHvsc());
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}

	}

	FavoritesSection getFavoritesSection() {
		return favoritesSection;
	}

	void addFavorites(List<File> files) {
		String result = "";
		for (File file : files) {
			final File[] listFiles = file.listFiles();
			if (file.isDirectory() && listFiles != null) {
				addFavorites(Arrays.asList(listFiles));
			} else if (tuneFilter.accept(file)) {
				result = String.join("", result, addFavorite(file));
			}
		}
		if (!result.isEmpty()) {
			openErrorDialog(result);
		}
	}

	void restoreColumns(final FavoritesSection favoritesSection) {
		this.favoritesSection = favoritesSection;
		filteredFavorites.addAll(favoritesSection.getFavorites());

		// Restore persisted columns
		for (FavoriteColumn favoriteColumn : favoritesSection.getColumns()) {
			try {
				SingularAttribute<?, ?> attribute = getAttribute(favoriteColumn.getColumnProperty());
				addColumn(attribute, favoriteColumn);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		Iterator<TableColumn<HVSCEntry, ?>> columnsIt = favoritesTable.getColumns().iterator();
		TableColumn<HVSCEntry, ?> pathColumn = columnsIt.next();
		pathColumn.widthProperty().addListener((observable, oldValue, newValue) -> {
			favoritesSection.setWidth(newValue.doubleValue());
		});
		Double width = favoritesSection.getWidth();
		if (width != 0) {
			pathColumn.setPrefWidth(width.doubleValue());
		}
		for (FavoriteColumn favoriteColumn : favoritesSection.getColumns()) {
			TableColumn<HVSCEntry, ?> column = columnsIt.next();
			width = favoriteColumn.getWidth();
			if (width != null) {
				column.setPrefWidth(width.doubleValue());
			}
		}
		favoritesSection.getObservableFavorites()
				.addListener((ListChangeListener.Change<? extends HVSCEntry> change) -> {
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
		util.getConfig().getFavorites().remove(favoritesSection);
	}

	void filter(String filterText) {
		filteredFavorites.clear();
		if (filterText.trim().length() == 0) {
			filteredFavorites.addAll(favoritesSection.getFavorites());
		} else {
			outer: for (HVSCEntry hvscEntry : favoritesSection.getFavorites()) {
				for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable.getColumns()) {
					FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn.getUserData();
					String name = favoriteColumn != null ? favoriteColumn.getColumnProperty()
							: HVSCEntry_.path.getName();
					try {
						Object value = ((Method) getAttribute(name).getJavaMember()).invoke(hvscEntry);
						String text = value != null ? value.toString() : "";
						if (text.contains(filterText)) {
							filteredFavorites.add(hvscEntry);
							continue outer;
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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
		String result = "";
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		favoritesFile = addFileExtension(favoritesFile);
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(new FileInputStream(favoritesFile), "ISO-8859-1"))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (line.startsWith("<HVSC>") || line.startsWith("<CGSC>")) {
					// backward compatibility
					line = line.substring(7);
				}
				File file = PathUtils.getFile(line, sidPlay2Section.getHvsc(), sidPlay2Section.getCgsc());
				if (file != null) {
					result = String.join("", result, addFavorite(file));
				}
			}
		}
		if (!result.isEmpty()) {
			openErrorDialog(result);
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
		if (favoritesSection.getFavorites().isEmpty()) {
			return;
		}
		HVSCEntry hvscEntry = favoritesSection.getFavorites()
				.get(Math.abs(new Random().nextInt(Integer.MAX_VALUE)) % favoritesSection.getFavorites().size());
		playTune(hvscEntry);
	}

	private File addFileExtension(File favoritesFile) {
		String extension = FavoritesExtension.EXTENSION;
		if (extension.startsWith("*")) {
			extension = extension.substring(1);
		}
		if (!favoritesFile.getName().endsWith(extension)) {
			favoritesFile = new File(favoritesFile.getParentFile(), favoritesFile.getName() + extension);
		}
		return favoritesFile;
	}

	private String addFavorite(File file) {
		String result = "";
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		SidTune tune;
		try {
			tune = SidTune.load(file);
			String collectionName = PathUtils.getCollectionName(sidPlay2Section.getHvsc(), file);
			HVSCEntry entry = new HVSCEntry(() -> util.getPlayer().getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.),
					collectionName, file, tune);
			favoritesSection.getFavorites().add(entry);
		} catch (IOException | SidTuneError e) {
			result = String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()) + "\n";
		}
		return result;
	}

	void removeFavorites(ObservableList<HVSCEntry> selectedItems) {
		favoritesSection.getFavorites().removeAll(selectedItems);
		filter(filterField.getText());
	}

	private SingularAttribute<?, ?> getAttribute(String columnProperty) throws IllegalAccessException {
		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())) {
				continue;
			}
			if (!SingularAttribute.class.isAssignableFrom(field.getType())) {
				continue;
			}
			if (columnProperty.equals(field.getName())) {
				return (SingularAttribute<?, ?>) field.get(null);
			}
		}
		return null;
	}

	private void addAddColumnHeaderMenuItem(Menu addColumnMenu, final SingularAttribute<?, ?> attribute) {
		MenuItem menuItem = new MenuItem();
		menuItem.setText(attribute.getName());
		menuItem.setOnAction(event -> {
			FavoriteColumn favoriteColumn = new FavoriteColumn();
			favoriteColumn.setColumnProperty(attribute.getName());
			favoritesSection.getColumns().add(favoriteColumn);
			addColumn(attribute, favoriteColumn);
		});
		addColumnMenu.getItems().add(menuItem);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void addColumn(SingularAttribute<?, ?> attribute, final FavoriteColumn favoriteColumn) {
		String text = util.getBundle()
				.getString(attribute.getDeclaringType().getJavaType().getSimpleName() + "." + attribute.getName());
		TableColumn tableColumn = new TableColumn();
		tableColumn.setUserData(favoriteColumn);
		tableColumn.setText(text);
		tableColumn.setCellValueFactory(new PropertyValueFactory(attribute.getName()));
		FavoritesCellFactory cellFactory = new FavoritesCellFactory();
		cellFactory.setPlayer(util.getPlayer());
		cellFactory.setCurrentlyPlayedHVSCEntryProperty(currentlyPlayedHVSCEntryProperty);
		tableColumn.setCellFactory(cellFactory);
		tableColumn.widthProperty().addListener((observable, oldValue, newValue) -> {
			favoriteColumn.setWidth(newValue.doubleValue());
		});
		favoritesTable.getColumns().add(tableColumn);
	}

	void moveColumn() {
		Collection<FavoriteColumn> newOrderList = new ArrayList<>();
		for (TableColumn<HVSCEntry, ?> tableColumn : favoritesTable.getColumns()) {
			FavoriteColumn favoriteColumn = (FavoriteColumn) tableColumn.getUserData();
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
		String result = "";
		for (HVSCEntry hvscEntry : toCopy) {
			result = String.join("", result, tab.addFavorite(getHVSCFile(hvscEntry)));
		}
		if (!result.isEmpty()) {
			openErrorDialog(result);
		}
	}

	void deselectCurrentlyPlayedHVSCEntry() {
		currentlyPlayedHVSCEntryProperty.set(null);
	}

	void playTune(final HVSCEntry hvscEntry) {
		favorites.setCurrentlyPlayedFavorites(this);
		util.setPlayingTab(this, currentlyPlayedHVSCEntryProperty);
		try {
			util.getPlayer().getC64().ejectCartridge();
			util.getPlayer().play(SidTune.load(getHVSCFile(hvscEntry)));
			currentlyPlayedHVSCEntryProperty.set(hvscEntry);
			favoritesTable.scrollTo(hvscEntry);
			int selectedIndex = filteredFavorites.indexOf(hvscEntry);
			favoritesSection.setSelectedRowFrom(selectedIndex);
			favoritesSection.setSelectedRowTo(selectedIndex);
		} catch (IOException | SidTuneError e) {
			openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
		}
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, "");
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		TextArea textArea = new TextArea(msg);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		alert.getDialogPane().setContent(textArea);
		alert.showAndWait();
	}

	public void setFavorites(Favorites favorites) {
		this.favorites = favorites;
	}

}
