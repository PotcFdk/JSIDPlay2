package ui.assembly64;

import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.of;
import static java.util.stream.IntStream.rangeClosed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.EnumToStringConverter;
import ui.common.UIPart;
import ui.directory.Directory;
import ui.entities.config.Assembly64Column;
import ui.entities.config.Assembly64ColumnType;
import ui.filefilter.DiskFileFilter;

public class Assembly64 extends C64VBox implements UIPart {
	public static final String ID = "ASSEMBLY64";

	private static final int MAX_ROWS = 500;

	private static final int DEFAULT_WIDTH = 150;

	@FXML
	private BorderPane parent;

	@FXML
	private HBox fieldContainer;

	@FXML
	private VBox nameVBox, groupVBox, yearVBox, handleVBox, eventVBox, ratingVBox, categoryVBox, updatedVBox;

	@FXML
	private TextField nameField, groupField, handleField, eventField, updatedField;

	@FXML
	private ComboBox<Integer> yearField, ratingField;

	@FXML
	private ComboBox<Category> categoryField;

	@FXML
	private ComboBox<Age> ageField;

	@FXML
	private CheckBox searchFromStartField, d64Field, t64Field, d81Field, d71Field, prgField, tapField, crtField,
			sidField, binField, g64Field;

	@FXML
	private TableView<SearchResult> assembly64Table;

	@FXML
	private TableView<ContentEntry> contentEntryTable;

	@FXML
	private TableColumn<SearchResult, String> nameColumn, groupColumn, yearColumn, handleColumn, eventColumn,
			ratingColumn, updatedColumn;

	@FXML
	private Menu addColumnMenu;

	@FXML
	private MenuItem removeColumn, attachDiskMenu, startMenu;

	@FXML
	private TableColumn<SearchResult, Category> categoryColumn;

	@FXML
	private TableColumn<ContentEntry, String> contentEntryColumn;

	@FXML
	private Button prevBtn, nextBtn;

	@FXML
	private Directory directory;

	@FXML
	private ObjectProperty<SearchResult> currentlyPlayedRowProperty;

	@FXML
	private ObjectProperty<ContentEntry> currentlyPlayedContentEntryProperty;

	@FXML
	private ContextMenu assembly64ContextMenu, contentEntryContextMenu;

	private ObservableList<Category> categoryItems;
	private ObservableList<SearchResult> searchResults;
	private ObservableList<ContentEntry> contentEntries;

	private SearchResult searchResult;

	private ContentEntry contentEntry;

	private File contentEntryFile;

	private int searchOffset, searchStop;

	private DiskFileFilter diskFileFilter;

	private ObjectMapper objectMapper;

	private Convenience convenience;

	private AtomicBoolean autostart;

	private PauseTransition pauseTransition, pauseTransitionContentEntry;
	private SequentialTransition sequentialTransition, sequentialTransitionContentEntry;

	private TablePosition<?, ?> selectedCell;

	public Assembly64() {
	}

	public Assembly64(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		autostart = new AtomicBoolean();
		convenience = new Convenience(util.getPlayer());
		diskFileFilter = new DiskFileFilter();

		categoryItems = FXCollections.observableArrayList(requestCategories());
		objectMapper = createObjectMapper();

		searchResults = FXCollections.<SearchResult>observableArrayList();
		SortedList<SearchResult> sortedSearchResultList = new SortedList<>(searchResults);
		sortedSearchResultList.comparatorProperty().bind(assembly64Table.comparatorProperty());
		assembly64Table.setItems(sortedSearchResultList);
		assembly64Table.getSelectionModel().selectedItemProperty().addListener((s, o, n) -> getContentEntries(false));
		assembly64Table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		assembly64Table.setOnMousePressed(e -> getContentEntries(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		assembly64Table.setOnKeyPressed(event -> getContentEntries(event.getCode() == KeyCode.ENTER));
		assembly64Table.getColumns().addListener((Change<? extends TableColumn<SearchResult, ?>> change) -> {
			while (change.next()) {
				if (change.wasReplaced()) {
					moveColumn();
				}
			}
		});
		assembly64ContextMenu.setOnShown(event -> showAssembly64ContextMenu());
		restoreColumns();

		contentEntries = FXCollections.<ContentEntry>observableArrayList();
		SortedList<ContentEntry> sortedContentEntryList = new SortedList<>(contentEntries);
		sortedContentEntryList.comparatorProperty().bind(contentEntryTable.comparatorProperty());
		contentEntryTable.setItems(sortedContentEntryList);
		contentEntryTable.getSelectionModel().selectedItemProperty()
				.addListener((s, o, n) -> getContentEntryFile(false));
		contentEntryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		contentEntryTable.setOnMousePressed(e -> getContentEntryFile(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		contentEntryTable.setOnKeyReleased(event -> getContentEntryFile(event.getCode() == KeyCode.ENTER));
		contentEntryTable.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));
		contentEntryTable.prefHeightProperty().bind(parent.heightProperty().multiply(0.4));
		contentEntryContextMenu.setOnShown(event -> showContentEntryContextMenu());
		contentEntryColumn.prefWidthProperty().bind(contentEntryTable.widthProperty());

		directory.getAutoStartFileProperty().addListener((s, o, n) -> autostart(n));
		directory.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));
		directory.prefHeightProperty().bind(parent.heightProperty().multiply(0.4));

		yearField.setItems(FXCollections.<Integer>observableArrayList(
				concat(of(0), rangeClosed(1980, Year.now().getValue())).boxed().collect(Collectors.toList())));
		yearField.getSelectionModel().select(0);
		yearField.setConverter(new ZeroContainingRatingConverter(util.getBundle()));

		ratingField.setItems(FXCollections
				.<Integer>observableArrayList(concat(of(0), rangeClosed(1, 10)).boxed().collect(Collectors.toList())));
		ratingField.getSelectionModel().select(0);
		ratingField.setConverter(new ZeroContainingRatingConverter(util.getBundle()));

		ageField.setConverter(new EnumToStringConverter<Age>(util.getBundle()));
		ageField.setItems(FXCollections.<Age>observableArrayList(Age.values()));
		ageField.getSelectionModel().select(Age.ALL);

		categoryField.setConverter(new CategoryToStringConverter<Category>(util.getBundle()));
		categoryField.setItems(categoryItems);
		categoryField.getSelectionModel().select(Category.ALL);

		pauseTransition = new PauseTransition(Duration.millis(1000));
		sequentialTransition = new SequentialTransition(pauseTransition);
		sequentialTransition.setCycleCount(1);
		pauseTransition.setOnFinished(evt -> requestSearchResults());

		pauseTransitionContentEntry = new PauseTransition(Duration.millis(500));
		sequentialTransitionContentEntry = new SequentialTransition(pauseTransitionContentEntry);
		sequentialTransitionContentEntry.setCycleCount(1);
		pauseTransitionContentEntry.setOnFinished(evt -> requestContentEntries());
	}

	@Override
	public void doClose() {
		sequentialTransition.stop();
		sequentialTransitionContentEntry.stop();
	}

	@FXML
	private void removeColumn() {
		if (selectedCell == null) {
			return;
		}
		removeColumn(selectedCell.getTableColumn());
	}

	@FXML
	private void search() {
		searchOffset = 0;
		searchAgain();
	}

	@FXML
	private void prevPage() {
		searchOffset -= MAX_ROWS;
		searchAgain();
	}

	@FXML
	private void nextPage() {
		searchOffset = searchStop;
		searchAgain();
	}

	@FXML
	private void autostart() {
		autostart(null);
	}

	@FXML
	private void attachDisk() {
		if (contentEntryFile != null) {
			try {
				util.getPlayer().insertDisk(contentEntryFile);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
			}
		}
	}

	private void restoreColumns() {
		fieldContainer.getChildren().clear();
		for (Assembly64Column column : util.getConfig().getAssembly64Section().getColumns()) {
			setColumnWidth(column, addColumn(column).getPrefWidth());
		}
	}

	private void showAssembly64ContextMenu() {
		selectedCell = assembly64Table.getSelectionModel().getSelectedCells().stream().findFirst().orElse(null);
		removeColumn.setDisable(selectedCell == null || selectedCell.getColumn() <= 0);
		removeColumn.setText(String.format(util.getBundle().getString("REMOVE_COLUMN"),
				selectedCell != null ? selectedCell.getTableColumn().getText() : "?"));

		List<Assembly64ColumnType> columnTypes = new ArrayList<>(Arrays.asList(Assembly64ColumnType.values()));
		columnTypes.removeIf(columnType -> assembly64Table.getColumns().stream()
				.map(tableColumn -> ((Assembly64Column) tableColumn.getUserData()).getColumnType())
				.collect(Collectors.toList()).contains(columnType));

		addColumnMenu.getItems().clear();
		for (Assembly64ColumnType columnType : columnTypes) {
			addAddColumnHeaderMenuItem(columnType);
		}
	}

	private void showContentEntryContextMenu() {
		ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
		attachDiskMenu.setDisable(contentEntry == null || !diskFileFilter.accept(new File(contentEntry.getName())));
		startMenu.setDisable(contentEntry == null);
	}

	private void addAddColumnHeaderMenuItem(Assembly64ColumnType columnType) {
		MenuItem menuItem = new MenuItem();
		menuItem.setText(util.getBundle().getString(columnType.getColumnProperty().toUpperCase(Locale.US)));
		menuItem.setOnAction(event -> {
			Assembly64Column column = new Assembly64Column();
			column.setColumnType(columnType);
			addColumn(column);
			util.getConfig().getAssembly64Section().getColumns().add(column);
		});
		addColumnMenu.getItems().add(menuItem);
	}

	private TableColumn<SearchResult, String> addColumn(Assembly64Column column) {
		Assembly64ColumnType columnType = column.getColumnType();

		TableColumn<SearchResult, String> tableColumn = new TableColumn<>();
		tableColumn.setUserData(column);
		tableColumn.setText(util.getBundle().getString(columnType.getColumnProperty().toUpperCase(Locale.US)));
		tableColumn.setCellValueFactory(new PropertyValueFactory<SearchResult, String>(columnType.getColumnProperty()));
		tableColumn.setPrefWidth(column.getWidth() != null ? column.getWidth().doubleValue() : DEFAULT_WIDTH);
		tableColumn.widthProperty().addListener((observable, oldValue, newValue) -> {
			column.setWidth(newValue.doubleValue());
			setColumnWidth(column, newValue);
		});
		assembly64Table.getColumns().add(tableColumn);
		switch (columnType) {
		case NAME:
			fieldContainer.getChildren().add(nameVBox);
			break;
		case GROUP:
			fieldContainer.getChildren().add(groupVBox);
			break;
		case YEAR:
			fieldContainer.getChildren().add(yearVBox);
			tableColumn.setCellFactory(new ZeroIgnoringCellFactory());
			break;
		case HANDLE:
			fieldContainer.getChildren().add(handleVBox);
			break;
		case EVENT:
			fieldContainer.getChildren().add(eventVBox);
			break;
		case RATING:
			fieldContainer.getChildren().add(ratingVBox);
			tableColumn.setCellFactory(new ZeroIgnoringCellFactory());
			break;
		case CATEGORY:
			fieldContainer.getChildren().add(categoryVBox);
			break;
		case UPDATED:
			fieldContainer.getChildren().add(updatedVBox);
			break;
		default:
			break;
		}
		return tableColumn;
	}

	private void setColumnWidth(Assembly64Column column, Number width) {
		switch (column.getColumnType()) {
		case NAME:
			nameField.prefWidthProperty().set(width.doubleValue());
			break;
		case GROUP:
			groupField.prefWidthProperty().set(width.doubleValue());
			break;
		case YEAR:
			yearField.prefWidthProperty().set(width.doubleValue());
			break;
		case HANDLE:
			handleField.prefWidthProperty().set(width.doubleValue());
			break;
		case EVENT:
			eventField.prefWidthProperty().set(width.doubleValue());
			break;
		case RATING:
			ratingField.prefWidthProperty().set(width.doubleValue());
			break;
		case CATEGORY:
			categoryField.prefWidthProperty().set(width.doubleValue());
			break;
		case UPDATED:
			updatedField.prefWidthProperty().set(width.doubleValue());
			break;
		}
	}

	private void removeColumn(TableColumn<?, ?> tableColumn) {
		boolean repeatSearch = false;
		Assembly64Column column = (Assembly64Column) tableColumn.getUserData();
		switch (column.getColumnType()) {
		case NAME:
			fieldContainer.getChildren().remove(nameVBox);
			repeatSearch = !nameField.getText().isEmpty();
			nameField.setText("");
			break;
		case GROUP:
			fieldContainer.getChildren().remove(groupVBox);
			repeatSearch = !groupField.getText().isEmpty();
			groupField.setText("");
			break;
		case YEAR:
			fieldContainer.getChildren().remove(yearVBox);
			repeatSearch = yearField.getSelectionModel().getSelectedIndex() > 0;
			yearField.getSelectionModel().select(0);
			break;
		case HANDLE:
			fieldContainer.getChildren().remove(handleVBox);
			repeatSearch = !handleField.getText().isEmpty();
			handleField.setText("");
			break;
		case EVENT:
			fieldContainer.getChildren().remove(eventVBox);
			repeatSearch = !eventField.getText().isEmpty();
			eventField.setText("");
			break;
		case RATING:
			fieldContainer.getChildren().remove(ratingVBox);
			repeatSearch = ratingField.getSelectionModel().getSelectedIndex() > 0;
			ratingField.getSelectionModel().select(0);
			break;
		case CATEGORY:
			fieldContainer.getChildren().remove(categoryVBox);
			repeatSearch = categoryField.getSelectionModel().getSelectedIndex() > 0;
			categoryField.getSelectionModel().select(Category.ALL);
			break;
		case UPDATED:
			fieldContainer.getChildren().remove(updatedVBox);
			break;

		default:
			break;
		}
		assembly64Table.getColumns().remove(tableColumn);
		util.getConfig().getAssembly64Section().getColumns().remove(column);
		if (repeatSearch) {
			search();
		}
	}

	private void moveColumn() {
		fieldContainer.getChildren().clear();
		util.getConfig().getAssembly64Section().getColumns().clear();
		assembly64Table.getColumns().stream().map(tableColumn -> (Assembly64Column) tableColumn.getUserData())
				.filter(column -> {
					switch (column.getColumnType()) {
					case NAME:
						fieldContainer.getChildren().add(nameVBox);
						return true;
					case GROUP:
						fieldContainer.getChildren().add(groupVBox);
						return true;
					case YEAR:
						fieldContainer.getChildren().add(yearVBox);
						return true;
					case HANDLE:
						fieldContainer.getChildren().add(handleVBox);
						return true;
					case EVENT:
						fieldContainer.getChildren().add(eventVBox);
						return true;
					case RATING:
						fieldContainer.getChildren().add(ratingVBox);
						return true;
					case CATEGORY:
						fieldContainer.getChildren().add(categoryVBox);
						return true;
					case UPDATED:
						fieldContainer.getChildren().add(updatedVBox);
						return true;
					default:
						return true;
					}
				}).forEach(column -> util.getConfig().getAssembly64Section().getColumns().add(column));
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		JodaModule module = new JodaModule();
		module.addDeserializer(Category.class, new CategoryDeserializer(categoryItems));
		objectMapper.registerModule(module);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return objectMapper;
	}

	private List<Category> requestCategories() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/categories").build();

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			String result = response.readEntity(String.class);
			List<Category> asList = Arrays.asList((Category[]) new ObjectMapper().readValue(result, Category[].class));
			asList.sort(Comparator.comparing(Category::getDescription));
			asList.set(0, Category.ALL);
			return FXCollections.<Category>observableArrayList(asList);
		} catch (ProcessingException | IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private void requestSearchResults() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/find2").path(
				"/{name}/{group}/{year}/{handle}/{event}/{rating}/{category}/{fromstart}/{d64}/{t64}/{d71}/{d81}/{prg}/{tap}/{crt}/{sid}/{bin}/{g64}/{or}/{days}")
				.queryParam("offset", searchOffset).build(get(nameField), get(groupField),
						get(yearField, value -> value, value -> value == 0, "***"), get(handleField), get(eventField),
						get(ratingField, value -> value, value -> value == 0, "***"),
						get(categoryField, value -> value.getId(), value -> value == Category.ALL, "***"),
						get(searchFromStartField), get(d64Field), get(t64Field), get(d71Field), get(d81Field),
						get(prgField), get(tapField), get(crtField), get(sidField), get(binField), get(g64Field), "n",
						get(ageField, value -> value.getDays(), value -> value == Age.ALL, -1));
		if (uri.getPath().contains("***/***/***/***/***/***/***")) {
			return;
		}
		String result = "";
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			result = response.readEntity(String.class);

			Object start = response.getHeaderString("start");
			searchOffset = start != null ? Integer.parseInt(String.valueOf(start)) : 0;
			prevBtn.setDisable(start == null || searchOffset == 0);

			Object stop = response.getHeaderString("stop");
			searchStop = stop != null ? Integer.parseInt(String.valueOf(stop)) : searchOffset + MAX_ROWS;
			nextBtn.setDisable(stop == null);

			searchResults.setAll(objectMapper.readValue(result, SearchResult[].class));
		} catch (ProcessingException | IOException e) {
			try {
				ErrorMessage errorMessage = objectMapper.readValue(result, ErrorMessage.class);
				System.err.println(String.join("\n", errorMessage.getStatus() + ": " + errorMessage.getError(),
						errorMessage.getMessage()));
			} catch (IOException e1) {
				System.err.println("Unexpected result: " + e.getMessage());
			}
		}
	}

	private void getContentEntries(boolean doAutostart) {
		this.searchResult = assembly64Table.getSelectionModel().getSelectedItem();
		sequentialTransitionContentEntry.playFromStart();
		autostart.set(doAutostart);
	}

	private void requestContentEntries() {
		if (searchResult == null) {
			return;
		}
		directory.clear();
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/contententries").path("/{id}/{category}")
				.build(searchResult.getId(), searchResult.getCategory().getId());

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			ContentEntrySearchResult contentEntry = (ContentEntrySearchResult) objectMapper
					.readValue(response.readEntity(String.class), ContentEntrySearchResult.class);
			contentEntries.setAll(contentEntry.getContentEntry());
			contentEntryTable.getSelectionModel().select(contentEntries.stream().findFirst().orElse(null));
			if (autostart.getAndSet(false)) {
				autostart();
			}
		} catch (ProcessingException | IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
		}
	}

	private void getContentEntryFile(boolean doAutostart) {
		this.contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
		if (contentEntry == null) {
			return;
		}
		try {
			contentEntryFile = requestContentEntry(contentEntry);
		} catch (ProcessingException | IOException e) {
			System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
		}
		directory.loadPreview(contentEntryFile);
		if (doAutostart) {
			autostart();
		}
	}

	private File requestContentEntry(ContentEntry contentEntry) throws FileNotFoundException, IOException {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/binary")
				.path("/{id}/{categoryId}/{contentEntryId}")
				.build(searchResult.getId(), searchResult.getCategory().getId(), contentEntry.getId());

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			// name without embedded sub-folder (sid/name.sid -> name.sid):
			String name = new File(contentEntry.getName()).getName();
			File tempFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), name);
			tempFile.deleteOnExit();
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(response.readEntity(byte[].class));
			}
			return tempFile;
		}
	}

	private <T, U> U get(ComboBox<T> comboBox, Function<T, U> toResult, Predicate<T> checkEmpty, U emptyValue) {
		T value = comboBox.getValue();
		if (checkEmpty.test(value)) {
			return emptyValue;
		}
		return toResult.apply(value);
	}

	private String get(TextField field) {
		String value = field.getText().trim();
		if (value.isEmpty()) {
			return "***";
		}
		return value;
	}

	private String get(CheckBox field) {
		return field.isSelected() ? "Y" : "N";
	}

	private void searchAgain() {
		searchResults.clear();
		contentEntries.clear();
		sequentialTransition.playFromStart();
	}

	private void autostart(File autostartFile) {
		if (contentEntry != null && contentEntryFile != null) {
			try {
				if (convenience.autostart(contentEntryFile, Convenience.LEXICALLY_FIRST_MEDIA, autostartFile)) {
					util.setPlayingTab(this);
					currentlyPlayedRowProperty.set(searchResult);
					currentlyPlayedContentEntryProperty.set(contentEntry);
				}
			} catch (IOException | SidTuneError | URISyntaxException e) {
				System.err.println(String.format("Cannot AUTOSTART file '%s'.", contentEntry.getName()));
			}
		}
	}

}
