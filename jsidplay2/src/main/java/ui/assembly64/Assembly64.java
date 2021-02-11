package ui.assembly64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.of;
import static java.util.stream.IntStream.rangeClosed;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static ui.assembly64.SearchResult.DATE_PATTERN;
import static ui.assembly64.SearchResult.MATCH_ALL;
import static ui.assembly64.SearchResult.NO;
import static ui.assembly64.SearchResult.YES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
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
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.TypeTextField;
import ui.common.UIPart;
import ui.common.converter.EnumToStringConverter;
import ui.common.converter.IntegerToStringConverter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.directory.Directory;
import ui.entities.config.Assembly64Column;
import ui.entities.config.Assembly64ColumnType;

public class Assembly64 extends C64VBox implements UIPart {

	public static final String ID = "ASSEMBLY64";

	private static final int MAX_ROWS = 500;

	private static final int DEFAULT_WIDTH = 150;

	@FXML
	private BorderPane borderPane;

	@FXML
	private HBox hBox;

	@FXML
	private VBox nameVBox, groupVBox, yearVBox, handleVBox, eventVBox, ratingVBox, categoryVBox, updatedVBox,
			releasedVBox;

	@FXML
	private TextField nameTextField, groupTextField, handleTextField, eventTextField, updatedTextField;

	@FXML
	private TypeTextField releasedTextField;

	@FXML
	private ComboBox<Category> categoryComboBox;

	@FXML
	private ComboBox<Integer> yearComboBox, ratingComboBox;

	@FXML
	private ComboBox<Age> ageComboBox;

	@FXML
	private CheckBox searchFromStartCheckBox, d64CheckBox, t64CheckBox, d81CheckBox, d71CheckBox, prgCheckBox,
			tapCheckBox, crtCheckBox, sidCheckBox, binCheckBox, g64CheckBox;

	@FXML
	private TableView<SearchResult> assembly64Table;

	@FXML
	private TableView<ContentEntry> contentEntryTable;

	@FXML
	private TableColumn<ContentEntry, String> contentEntryFilenameColumn;

	@FXML
	private ContextMenu assembly64ContextMenu, contentEntryContextMenu;

	@FXML
	private Menu addColumnMenu;

	@FXML
	private MenuItem removeColumnMenuItem, insertDiskMenuItem, insertTapeMenuItem, autostartMenuItem;

	@FXML
	private Button prevButton, nextButton;

	@FXML
	private Directory directory;

	@FXML
	private ObjectProperty<SearchResult> currentlyPlayedSearchResultRowProperty;

	@FXML
	private ObjectProperty<ContentEntry> currentlyPlayedContentEntryRowProperty;

	private ObservableList<Category> categoryItems;
	private ObservableList<SearchResult> searchResultItems;
	private ObservableList<ContentEntry> contentEntryItems;

	private SearchResult searchResult;

	private ContentEntry contentEntry;

	private File contentEntryFile;

	private int searchOffset, searchStop;

	private DiskFileFilter diskFileFilter;

	private TapeFileFilter tapeFileFilter;

	private ObjectMapper objectMapper;

	private Convenience convenience;

	private AtomicBoolean autostart;

	private PauseTransition pauseTransitionSearchResult, pauseTransitionContentEntries;
	private SequentialTransition sequentialTransitionSearchResult, sequentialTransitionContentEntries;

	private TablePosition<?, ?> searchResultTableSelectedCell;

	public Assembly64() {
		super();
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
		tapeFileFilter = new TapeFileFilter();

		categoryItems = FXCollections.observableArrayList(requestCategories());
		objectMapper = createObjectMapper();

		searchResultItems = FXCollections.<SearchResult>observableArrayList();
		SortedList<SearchResult> sortedSearchResultList = new SortedList<>(searchResultItems);
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

		contentEntryItems = FXCollections.<ContentEntry>observableArrayList();
		SortedList<ContentEntry> sortedContentEntryList = new SortedList<>(contentEntryItems);
		sortedContentEntryList.comparatorProperty().bind(contentEntryTable.comparatorProperty());
		contentEntryTable.setItems(sortedContentEntryList);
		contentEntryTable.getSelectionModel().selectedItemProperty().addListener((s, o, n) -> getContentEntry(false));
		contentEntryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		contentEntryTable.setOnMousePressed(e -> getContentEntry(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		contentEntryTable.setOnKeyReleased(event -> getContentEntry(event.getCode() == KeyCode.ENTER));
		contentEntryTable.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));
		contentEntryTable.prefHeightProperty().bind(borderPane.heightProperty().multiply(0.4));
		contentEntryContextMenu.setOnShown(event -> showContentEntryContextMenu());
		contentEntryFilenameColumn.prefWidthProperty().bind(contentEntryTable.widthProperty());

		directory.getAutoStartFileProperty().addListener((s, o, n) -> autostart(n));
		directory.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));
		directory.prefHeightProperty().bind(borderPane.heightProperty().multiply(0.4));

		yearComboBox.setItems(FXCollections.<Integer>observableArrayList(
				concat(of(0), rangeClosed(1980, Year.now().getValue())).boxed().collect(Collectors.toList())));
		yearComboBox.getSelectionModel().select(0);
		yearComboBox.setConverter(new IntegerToStringConverter(util.getBundle(), "ALL_CONTENT"));

		ratingComboBox.setItems(FXCollections
				.<Integer>observableArrayList(concat(of(0), rangeClosed(1, 10)).boxed().collect(Collectors.toList())));
		ratingComboBox.getSelectionModel().select(0);
		ratingComboBox.setConverter(new IntegerToStringConverter(util.getBundle(), "ALL_CONTENT"));

		ageComboBox.setConverter(new EnumToStringConverter<Age>(util.getBundle()));
		ageComboBox.setItems(FXCollections.<Age>observableArrayList(Age.values()));
		ageComboBox.getSelectionModel().select(Age.ALL);

		categoryComboBox.setConverter(new CategoryToStringConverter<Category>(util.getBundle()));
		categoryComboBox.setItems(categoryItems);
		categoryComboBox.getSelectionModel().select(Category.ALL);

		pauseTransitionSearchResult = new PauseTransition(Duration.millis(1000));
		sequentialTransitionSearchResult = new SequentialTransition(pauseTransitionSearchResult);
		sequentialTransitionSearchResult.setCycleCount(1);
		pauseTransitionSearchResult.setOnFinished(evt -> requestSearchResults());

		pauseTransitionContentEntries = new PauseTransition(Duration.millis(500));
		sequentialTransitionContentEntries = new SequentialTransition(pauseTransitionContentEntries);
		sequentialTransitionContentEntries.setCycleCount(1);
		pauseTransitionContentEntries.setOnFinished(evt -> requestContentEntries());
	}

	@Override
	public void doClose() {
		sequentialTransitionSearchResult.stop();
		sequentialTransitionContentEntries.stop();
	}

	@FXML
	private void removeColumn() {
		if (searchResultTableSelectedCell == null) {
			return;
		}
		TableColumn<?, ?> tableColumn = searchResultTableSelectedCell.getTableColumn();
		assembly64Table.getColumns().remove(tableColumn);
		removeColumn((Assembly64Column) tableColumn.getUserData());
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
	private void insertDisk() {
		if (contentEntryFile != null) {
			try {
				util.getPlayer().insertDisk(contentEntryFile);
			} catch (IOException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getId()));
			}
		}
	}

	@FXML
	private void insertTape() {
		if (contentEntryFile != null) {
			try {
				util.getPlayer().insertTape(contentEntryFile);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getId()));
			}
		}
	}

	private void restoreColumns() {
		hBox.getChildren().clear();
		for (Assembly64Column column : util.getConfig().getAssembly64Section().getColumns()) {
			setColumnWidth(column, addColumn(column).getPrefWidth());
		}
	}

	private void showAssembly64ContextMenu() {
		searchResultTableSelectedCell = assembly64Table.getSelectionModel().getSelectedCells().stream().findFirst()
				.orElse(null);
		removeColumnMenuItem
				.setDisable(searchResultTableSelectedCell == null || searchResultTableSelectedCell.getColumn() <= 0);
		removeColumnMenuItem.setText(String.format(util.getBundle().getString("REMOVE_COLUMN"),
				!removeColumnMenuItem.isDisable() ? searchResultTableSelectedCell.getTableColumn().getText() : "?"));

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
		insertDiskMenuItem.setDisable(contentEntry == null || !diskFileFilter.accept(new File(contentEntry.getId())));
		insertTapeMenuItem.setDisable(contentEntry == null || !tapeFileFilter.accept(new File(contentEntry.getId())));
		autostartMenuItem.setDisable(contentEntry == null);
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

	private TableColumn<SearchResult, ?> addColumn(Assembly64Column column) {
		Assembly64ColumnType columnType = column.getColumnType();

		TableColumn<SearchResult, ?> tableColumn;
		switch (columnType) {
		case YEAR:
		case RATING:
			TableColumn<SearchResult, Integer> tableColumnInteger = new TableColumn<>();
			tableColumn = tableColumnInteger;
			tableColumnInteger.setCellValueFactory(
					new PropertyValueFactory<SearchResult, Integer>(columnType.getColumnProperty()));
			tableColumnInteger.setCellFactory(new ZeroIgnoringCellFactory());
			break;
		case UPDATED:
		case RELEASED:
			TableColumn<SearchResult, LocalDate> tableColumnLocalDate = new TableColumn<>();
			tableColumn = tableColumnLocalDate;
			tableColumnLocalDate.setCellValueFactory(
					new PropertyValueFactory<SearchResult, LocalDate>(columnType.getColumnProperty()));
			break;
		default:
			TableColumn<SearchResult, String> tableColumnString = new TableColumn<>();
			tableColumn = tableColumnString;
			tableColumnString.setCellValueFactory(
					new PropertyValueFactory<SearchResult, String>(columnType.getColumnProperty()));
			break;
		}
		tableColumn.setUserData(column);
		tableColumn.setText(util.getBundle().getString(columnType.getColumnProperty().toUpperCase(Locale.US)));
		tableColumn.setPrefWidth(column.getWidth() != null ? column.getWidth().doubleValue() : DEFAULT_WIDTH);
		tableColumn.widthProperty().addListener((observable, oldValue, newValue) -> {
			column.setWidth(newValue.doubleValue());
			setColumnWidth(column, newValue);
		});
		assembly64Table.getColumns().add(tableColumn);
		switch (columnType) {
		case NAME:
			hBox.getChildren().add(nameVBox);
			break;
		case GROUP:
			hBox.getChildren().add(groupVBox);
			break;
		case YEAR:
			hBox.getChildren().add(yearVBox);
			break;
		case HANDLE:
			hBox.getChildren().add(handleVBox);
			break;
		case EVENT:
			hBox.getChildren().add(eventVBox);
			break;
		case RATING:
			hBox.getChildren().add(ratingVBox);
			break;
		case CATEGORY:
			hBox.getChildren().add(categoryVBox);
			break;
		case UPDATED:
			hBox.getChildren().add(updatedVBox);
			break;
		case RELEASED:
			hBox.getChildren().add(releasedVBox);
			break;
		default:
			break;
		}
		return tableColumn;
	}

	private void setColumnWidth(Assembly64Column column, Number width) {
		switch (column.getColumnType()) {
		case NAME:
			nameTextField.prefWidthProperty().set(width.doubleValue());
			break;
		case GROUP:
			groupTextField.prefWidthProperty().set(width.doubleValue());
			break;
		case YEAR:
			yearComboBox.prefWidthProperty().set(width.doubleValue());
			break;
		case HANDLE:
			handleTextField.prefWidthProperty().set(width.doubleValue());
			break;
		case EVENT:
			eventTextField.prefWidthProperty().set(width.doubleValue());
			break;
		case RATING:
			ratingComboBox.prefWidthProperty().set(width.doubleValue());
			break;
		case CATEGORY:
			categoryComboBox.prefWidthProperty().set(width.doubleValue());
			break;
		case UPDATED:
			updatedTextField.prefWidthProperty().set(width.doubleValue());
			break;
		case RELEASED:
			releasedTextField.prefWidthProperty().set(width.doubleValue());
			break;
		}
	}

	private void removeColumn(Assembly64Column column) {
		boolean repeatSearch = false;
		switch (column.getColumnType()) {
		case NAME:
			hBox.getChildren().remove(nameVBox);
			repeatSearch = !nameTextField.getText().isEmpty();
			nameTextField.setText("");
			break;
		case GROUP:
			hBox.getChildren().remove(groupVBox);
			repeatSearch = !groupTextField.getText().isEmpty();
			groupTextField.setText("");
			break;
		case YEAR:
			hBox.getChildren().remove(yearVBox);
			repeatSearch = yearComboBox.getSelectionModel().getSelectedIndex() > 0;
			yearComboBox.getSelectionModel().select(0);
			break;
		case HANDLE:
			hBox.getChildren().remove(handleVBox);
			repeatSearch = !handleTextField.getText().isEmpty();
			handleTextField.setText("");
			break;
		case EVENT:
			hBox.getChildren().remove(eventVBox);
			repeatSearch = !eventTextField.getText().isEmpty();
			eventTextField.setText("");
			break;
		case RATING:
			hBox.getChildren().remove(ratingVBox);
			repeatSearch = ratingComboBox.getSelectionModel().getSelectedIndex() > 0;
			ratingComboBox.getSelectionModel().select(0);
			break;
		case CATEGORY:
			hBox.getChildren().remove(categoryVBox);
			repeatSearch = categoryComboBox.getSelectionModel().getSelectedIndex() > 0;
			categoryComboBox.getSelectionModel().select(Category.ALL);
			break;
		case UPDATED:
			hBox.getChildren().remove(updatedVBox);
			break;
		case RELEASED:
			hBox.getChildren().remove(releasedVBox);
			repeatSearch = !releasedTextField.getText().isEmpty();
			break;

		default:
			break;
		}
		util.getConfig().getAssembly64Section().getColumns().remove(column);
		if (repeatSearch) {
			search();
		}
	}

	private void moveColumn() {
		hBox.getChildren().clear();
		util.getConfig().getAssembly64Section().getColumns().clear();
		assembly64Table.getColumns().stream().map(tableColumn -> (Assembly64Column) tableColumn.getUserData())
				.filter(column -> {
					switch (column.getColumnType()) {
					case NAME:
						return hBox.getChildren().add(nameVBox);
					case GROUP:
						return hBox.getChildren().add(groupVBox);
					case YEAR:
						return hBox.getChildren().add(yearVBox);
					case HANDLE:
						return hBox.getChildren().add(handleVBox);
					case EVENT:
						return hBox.getChildren().add(eventVBox);
					case RATING:
						return hBox.getChildren().add(ratingVBox);
					case CATEGORY:
						return hBox.getChildren().add(categoryVBox);
					case UPDATED:
						return hBox.getChildren().add(updatedVBox);
					case RELEASED:
						return hBox.getChildren().add(releasedVBox);
					default:
						return false;
					}
				}).forEach(column -> util.getConfig().getAssembly64Section().getColumns().add(column));
	}

	private ObjectMapper createObjectMapper() {
		JavaTimeModule module = new JavaTimeModule();
		module.addDeserializer(Category.class, new CategoryDeserializer(categoryItems));
		return new ObjectMapper().registerModule(module).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	private List<Category> requestCategories() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/v2/categories").build();

		try (Response response = requestUri(uri)) {
			String responseString = readString(response);
			List<Category> result = new ObjectMapper().readValue(responseString, new TypeReference<List<Category>>() {
			});
			result.sort(Comparator.comparing(Category::getDescription));
			result.add(0, Category.ALL);
			return FXCollections.<Category>observableArrayList(result);
		} catch (ProcessingException | IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private void searchAgain() {
		searchResultItems.clear();
		contentEntryItems.clear();
		directory.clear();
		Platform.runLater(() -> {
			util.progressProperty(assembly64Table.getScene()).set(INDETERMINATE_PROGRESS);
		});
		sequentialTransitionSearchResult.playFromStart();
	}

	private void requestSearchResults() {
		try {
			String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();

			final String name = get(nameTextField);
			final String group = get(groupTextField);
			final Object year = get(yearComboBox, value -> value, value -> value == 0, MATCH_ALL);
			final String handle = get(handleTextField);
			final String event = get(eventTextField);
			final Object rating = get(ratingComboBox, value -> value, value -> value == 0, MATCH_ALL);
			final Object category = get(categoryComboBox, value -> value.getId(), value -> value == Category.ALL,
					MATCH_ALL);
			final String searchFromStart = get(searchFromStartCheckBox);
			final String d64 = get(d64CheckBox);
			final String t64 = get(t64CheckBox);
			final String d71 = get(d71CheckBox);
			final String d81 = get(d81CheckBox);
			final String prg = get(prgCheckBox);
			final String tap = get(tapCheckBox);
			final String crt = get(crtCheckBox);
			final String sid = get(sidCheckBox);
			final String bin = get(binCheckBox);
			final String g64 = get(g64CheckBox);
			final String or = getOr();
			final Integer days = get(ageComboBox, value -> value.getDays(), value -> value == Age.ALL, -1);
			final String dateFrom = get(releasedTextField, true);
			final String dateTo = get(releasedTextField, false);
			UriBuilder uriBuilder = UriBuilder.fromPath(assembly64Url + "/leet/search/v2");
			int matchCount = 0;
			if (name != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("name", name);
			}
			if (group != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("group", group);
			}
			if (year != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("year", year);
			}
			if (handle != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("handle", handle);
			}
			if (event != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("event", event);
			}
			if (rating != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("rating", rating);
			}
			if (category != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("category", category);
			}
			if (searchFromStart != null) {
				uriBuilder = uriBuilder.queryParam("searchFromStart", searchFromStart);
			}
			if (d64 != null) {
				uriBuilder = uriBuilder.queryParam("d64", d64);
			}
			if (t64 != null) {
				uriBuilder = uriBuilder.queryParam("t64", t64);
			}
			if (d71 != null) {
				uriBuilder = uriBuilder.queryParam("d71", d71);
			}
			if (d81 != null) {
				uriBuilder = uriBuilder.queryParam("d81", d81);
			}
			if (prg != null) {
				uriBuilder = uriBuilder.queryParam("prg", prg);
			}
			if (tap != null) {
				uriBuilder = uriBuilder.queryParam("tap", tap);
			}
			if (crt != null) {
				uriBuilder = uriBuilder.queryParam("crt", crt);
			}
			if (sid != null) {
				uriBuilder = uriBuilder.queryParam("sid", sid);
			}
			if (bin != null) {
				uriBuilder = uriBuilder.queryParam("bin", bin);
			}
			if (g64 != null) {
				uriBuilder = uriBuilder.queryParam("g64", g64);
			}
			if (or != null) {
				uriBuilder = uriBuilder.queryParam("or", or);
			}
			if (days != null) {
				uriBuilder = uriBuilder.queryParam("days", days);
			}
			if (dateFrom != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("dateFrom", dateFrom);
			}
			if (dateTo != null) {
				matchCount++;
				uriBuilder = uriBuilder.queryParam("dateTo", dateTo);
			}
			uriBuilder = uriBuilder.queryParam("offset", searchOffset);

			URI uri = uriBuilder.build();

			if (matchCount == 0) {
				// avoid to request everything, it would take too much time!
				return;
			}
			String result = "";
			try (Response response = requestUri(uri)) {
				result = readString(response);

				Object start = response.getHeaderString("start");
				searchOffset = start != null ? Integer.parseInt(String.valueOf(start)) : 0;
				prevButton.setDisable(start == null || searchOffset == 0);

				Object stop = response.getHeaderString("stop");
				searchStop = stop != null ? Integer.parseInt(String.valueOf(stop)) : searchOffset + MAX_ROWS;
				nextButton.setDisable(stop == null);

				searchResultItems.setAll(objectMapper.readValue(result, SearchResult[].class));
			} catch (ProcessingException | IOException e) {
				try {
					ErrorMessage errorMessage = objectMapper.readValue(result, ErrorMessage.class);
					System.err.println(String.join("\n", errorMessage.getStatus() + ": " + errorMessage.getError(),
							errorMessage.getMessage()));
				} catch (IOException e1) {
					System.err.println("Unexpected result: " + e.getMessage());
				}
			}
		} finally {
			Platform.runLater(() -> util.progressProperty(assembly64Table.getScene()).set(0));
		}
	}

	private void getContentEntries(boolean doAutostart) {
		searchResult = assembly64Table.getSelectionModel().getSelectedItem();
		if (searchResult == null) {
			return;
		}
		autostart.set(doAutostart);
		Platform.runLater(() -> util.progressProperty(assembly64Table.getScene()).set(INDETERMINATE_PROGRESS));
		sequentialTransitionContentEntries.playFromStart();
	}

	private void requestContentEntries() {
		try {
			if (searchResult == null) {
				return;
			}
			directory.clear();
			String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
			final String itemId = Base64.getEncoder().encodeToString(searchResult.getId().getBytes());
			URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/v2/contententries")
					.path("/{itemId}/{categoryId}").build(itemId, searchResult.getCategory().getId());
			try (Response response = requestUri(uri)) {
				ContentEntrySearchResult contentEntry = objectMapper.readValue(readString(response),
						ContentEntrySearchResult.class);
				contentEntryItems.setAll(contentEntry.getContentEntry());
				contentEntryTable.getSelectionModel().select(contentEntryItems.stream().findFirst().orElse(null));
				if (autostart.getAndSet(false)) {
					autostart(null);
				}
			} catch (ProcessingException | IOException e) {
				System.err.println("Unexpected result: " + e.getMessage());
			}
		} finally {
			Platform.runLater(() -> util.progressProperty(assembly64Table.getScene()).set(0));
		}
	}

	private void getContentEntry(boolean doAutostart) {
		this.contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
		if (contentEntry == null) {
			return;
		}
		try {
			contentEntryFile = requestContentEntry(contentEntry);
		} catch (ProcessingException | IOException e) {
			System.err.println(String.format("Cannot DOWNLOAD file '%s'.", contentEntry.getId()));
		}
		directory.loadPreview(contentEntryFile);
		if (doAutostart) {
			autostart(null);
		}
	}

	private File requestContentEntry(ContentEntry contentEntry) throws FileNotFoundException, IOException {
		// name without embedded sub-folder (sid/name.sid -> name.sid):
		String name = new File(contentEntry.getId()).getName();
		File contentEntryFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), name);
		File contentEntryChecksumFile = new File(util.getConfig().getSidplay2Section().getTmpDir(),
				PathUtils.getFilenameWithoutSuffix(name) + ".md5");
		try {
			// file already downloaded and checksum ok?
			if (contentEntryFile.exists() && contentEntryChecksumFile.exists()) {
				String checksum = DigestUtils.md5Hex(getContents(contentEntryFile));
				String checksumToverify = new String(getContents(contentEntryChecksumFile), US_ASCII);
				if (checksum.equals(checksumToverify)) {
					return contentEntryFile;
				}
			}
		} finally {
			contentEntryFile.deleteOnExit();
			contentEntryChecksumFile.deleteOnExit();
		}
		// request file, create checksum
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		final String itemId = Base64.getEncoder().encodeToString(searchResult.getId().getBytes());
		final String fileId = Base64.getEncoder().encodeToString(contentEntry.getId().getBytes());
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/v2/binary").path("/{itemId}/{categoryId}/{fileId}")
				.build(itemId, searchResult.getCategory().getId(), fileId);
		try (Response response = requestUri(uri);
				OutputStream outputStream = new FileOutputStream(contentEntryFile);
				PrintStream checksumPrintStream = new PrintStream(contentEntryChecksumFile)) {
			outputStream.write(response.readEntity(byte[].class));
			checksumPrintStream.print(response.getHeaderString("checksum"));
			return contentEntryFile;
		}
	}

	private byte[] getContents(File contentEntryChecksumFile) {
		try (InputStream is = new FileInputStream(contentEntryChecksumFile)) {
			return IOUtils.toByteArray(is);
		} catch (IOException e) {
			return new byte[0];
		}
	}

	private String get(TypeTextField field, boolean dateFromTo) {
		Object value = field.getValue();
		if (value instanceof YearMonth) {
			YearMonth yearMonth = (YearMonth) value;
			value = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(),
					dateFromTo ? 1 : yearMonth.lengthOfMonth());
		} else if (value instanceof Year) {
			Year year = (Year) value;
			value = LocalDate.of(year.getValue(), dateFromTo ? 1 : 12, dateFromTo ? 1 : 31);
		}
		if (value == null) {
			return MATCH_ALL;
		}
		return ((LocalDate) value).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
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
			return MATCH_ALL;
		}
		return value;
	}

	private String get(CheckBox field) {
		return field.isSelected() ? YES : NO;
	}

	private String getOr() {
		return NO;
	}

	private void autostart(File autostartFile) {
		if (contentEntry != null && contentEntryFile != null) {
			try {
				if (convenience.autostart(contentEntryFile, Convenience.LEXICALLY_FIRST_MEDIA, autostartFile)) {
					util.setPlayingTab(this, currentlyPlayedSearchResultRowProperty,
							currentlyPlayedContentEntryRowProperty);
					currentlyPlayedSearchResultRowProperty.set(searchResult);
					currentlyPlayedContentEntryRowProperty.set(contentEntry);
				}
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot AUTOSTART file '%s'.", contentEntry.getId()));
			}
		}
	}

	private Response requestUri(URI uri) {
		return ClientBuilder.newClient().target(uri).request().get();
	}

	private String readString(Response response) {
		return response.readEntity(String.class);
	}

}
