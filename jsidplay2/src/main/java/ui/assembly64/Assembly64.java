package ui.assembly64;

import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.of;
import static java.util.stream.IntStream.rangeClosed;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

import org.apache.commons.codec.binary.Hex;

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
import ui.common.TypeTextField;
import ui.common.UIPart;
import ui.directory.Directory;
import ui.entities.config.Assembly64Column;
import ui.entities.config.Assembly64ColumnType;
import ui.filefilter.DiskFileFilter;

public class Assembly64 extends C64VBox implements UIPart {

	public static final String ID = "ASSEMBLY64";

	private static final MessageDigest MD5_DIGEST;

	static {
		try {
			MD5_DIGEST = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final Comparator<String> NUMERIC_COMPARATOR = (o1, o2) -> extractInt(o1) - extractInt(o2);

	private static final Comparator<String> DATE_COMPARATOR = (o1, o2) -> extractDate(o1).compareTo(extractDate(o2));

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
	private MenuItem removeColumnMenuItem, attachDiskMenuItem, autostartMenuItem;

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

	private ObjectMapper objectMapper;

	private Convenience convenience;

	private AtomicBoolean autostart;

	private PauseTransition pauseTransitionSearchResult, pauseTransitionContentEntries;
	private SequentialTransition sequentialTransitionSearchResult, sequentialTransitionContentEntries;

	private TablePosition<?, ?> searchResultTableSelectedCell;

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
		yearComboBox.setConverter(new ZeroContainingRatingConverter(util.getBundle()));

		ratingComboBox.setItems(FXCollections
				.<Integer>observableArrayList(concat(of(0), rangeClosed(1, 10)).boxed().collect(Collectors.toList())));
		ratingComboBox.getSelectionModel().select(0);
		ratingComboBox.setConverter(new ZeroContainingRatingConverter(util.getBundle()));

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
		attachDiskMenuItem.setDisable(contentEntry == null || !diskFileFilter.accept(new File(contentEntry.getName())));
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
			hBox.getChildren().add(nameVBox);
			break;
		case GROUP:
			hBox.getChildren().add(groupVBox);
			break;
		case YEAR:
			hBox.getChildren().add(yearVBox);
			tableColumn.setCellFactory(new ZeroIgnoringCellFactory());
			tableColumn.setComparator(NUMERIC_COMPARATOR);
			break;
		case HANDLE:
			hBox.getChildren().add(handleVBox);
			break;
		case EVENT:
			hBox.getChildren().add(eventVBox);
			break;
		case RATING:
			hBox.getChildren().add(ratingVBox);
			tableColumn.setCellFactory(new ZeroIgnoringCellFactory());
			tableColumn.setComparator(NUMERIC_COMPARATOR);
			break;
		case CATEGORY:
			hBox.getChildren().add(categoryVBox);
			break;
		case UPDATED:
			hBox.getChildren().add(updatedVBox);
			tableColumn.setComparator(DATE_COMPARATOR);
			break;
		case RELEASED:
			hBox.getChildren().add(releasedVBox);
			tableColumn.setComparator(DATE_COMPARATOR);
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
						hBox.getChildren().add(nameVBox);
						return true;
					case GROUP:
						hBox.getChildren().add(groupVBox);
						return true;
					case YEAR:
						hBox.getChildren().add(yearVBox);
						return true;
					case HANDLE:
						hBox.getChildren().add(handleVBox);
						return true;
					case EVENT:
						hBox.getChildren().add(eventVBox);
						return true;
					case RATING:
						hBox.getChildren().add(ratingVBox);
						return true;
					case CATEGORY:
						hBox.getChildren().add(categoryVBox);
						return true;
					case UPDATED:
						hBox.getChildren().add(updatedVBox);
						return true;
					case RELEASED:
						hBox.getChildren().add(releasedVBox);
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
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/categories").build();

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
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/find").path(
				"/{name}/{group}/{year}/{handle}/{event}/{rating}/{category}/{fromstart}/{d64}/{t64}/{d71}/{d81}/{prg}/{tap}/{crt}/{sid}/{bin}/{g64}/{or}/{days}/{releasedFrom}/{releasedTo}")
				.queryParam("offset", searchOffset).build(get(nameTextField), get(groupTextField),
						get(yearComboBox, value -> value, value -> value == 0, "***"), get(handleTextField),
						get(eventTextField), get(ratingComboBox, value -> value, value -> value == 0, "***"),
						get(categoryComboBox, value -> value.getId(), value -> value == Category.ALL, "***"),
						get(searchFromStartCheckBox), get(d64CheckBox), get(t64CheckBox), get(d71CheckBox),
						get(d81CheckBox), get(prgCheckBox), get(tapCheckBox), get(crtCheckBox), get(sidCheckBox),
						get(binCheckBox), get(g64CheckBox), "n",
						get(ageComboBox, value -> value.getDays(), value -> value == Age.ALL, -1), getDate(true),
						getDate(false));
		if (uri.getPath().contains("***/***/***/***/***/***/***/***/***")) {
			return;
		}
		String result = "";
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			result = response.readEntity(String.class);

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
	}

	private void getContentEntries(boolean doAutostart) {
		searchResult = assembly64Table.getSelectionModel().getSelectedItem();
		if (searchResult == null) {
			return;
		}
		autostart.set(doAutostart);
		sequentialTransitionContentEntries.playFromStart();
	}

	private void requestContentEntries() {
		if (searchResult == null) {
			return;
		}
		directory.clear();
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/entries").path("/{id}/{categoryId}")
				.build(searchResult.getId(), searchResult.getCategory().getId());
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			ContentEntrySearchResult contentEntry = (ContentEntrySearchResult) objectMapper
					.readValue(response.readEntity(String.class), ContentEntrySearchResult.class);
			contentEntryItems.setAll(contentEntry.getContentEntry());
			contentEntryTable.getSelectionModel().select(contentEntryItems.stream().findFirst().orElse(null));
			if (autostart.getAndSet(false)) {
				autostart(null);
			}
		} catch (ProcessingException | IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
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
			System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
		}
		directory.loadPreview(contentEntryFile);
		if (doAutostart) {
			autostart(null);
		}
	}

	private File requestContentEntry(ContentEntry contentEntry) throws FileNotFoundException, IOException {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search/binary").path("/{id}/{categoryId}/{contentEntryId}")
				.build(searchResult.getId(), searchResult.getCategory().getId(), contentEntry.getId());

		// name without embedded sub-folder (sid/name.sid -> name.sid):
		String name = new File(contentEntry.getName()).getName();
		File tempFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), name);
		tempFile.deleteOnExit();

		if (tempFile.exists()) {
			try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(tempFile));
					DigestInputStream dis = new DigestInputStream(is, MD5_DIGEST)) {
				// read the file and update the hash calculation
				while (dis.read() != -1)
					;
				// get the hash value as byte array
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			char[] hex = Hex.encodeHex(MD5_DIGEST.digest());
			String digest = new String(hex);
			System.err.println("JSIDPlay2: " + digest + " <-> Assembly64: " + contentEntry.getChecksum());
			if (digest.equals(contentEntry.getChecksum())) {
				return tempFile;
			}
		}

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
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

	private String getDate(boolean fromTo) {
		LocalDate value = null;
		if (releasedTextField.getValue() instanceof LocalDate) {
			value = (LocalDate) releasedTextField.getValue();
		} else if (releasedTextField.getValue() instanceof YearMonth) {
			YearMonth yearMonth = (YearMonth) releasedTextField.getValue();
			value = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(),
					fromTo ? 1 : yearMonth.lengthOfMonth());
		} else if (releasedTextField.getValue() instanceof Year) {
			Year year = (Year) releasedTextField.getValue();
			value = LocalDate.of(year.getValue(), fromTo ? 1 : 12, fromTo ? 1 : 31);
		}
		if (value == null) {
			return "***";
		}
		return value.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	}

	private String get(CheckBox field) {
		return field.isSelected() ? "Y" : "N";
	}

	private void searchAgain() {
		searchResultItems.clear();
		contentEntryItems.clear();
		sequentialTransitionSearchResult.playFromStart();
	}

	private void autostart(File autostartFile) {
		if (contentEntry != null && contentEntryFile != null) {
			try {
				if (convenience.autostart(contentEntryFile, Convenience.LEXICALLY_FIRST_MEDIA, autostartFile)) {
					util.setPlayingTab(this);
					currentlyPlayedSearchResultRowProperty.set(searchResult);
					currentlyPlayedContentEntryRowProperty.set(contentEntry);
				}
			} catch (IOException | SidTuneError | URISyntaxException e) {
				System.err.println(String.format("Cannot AUTOSTART file '%s'.", contentEntry.getName()));
			}
		}
	}

	private static final int extractInt(String string) {
		try {
			String numericCharacters = string.replaceAll("\\D", "");
			return numericCharacters.isEmpty() ? 0 : Integer.parseInt(numericCharacters);
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	private static final LocalDate extractDate(String string) {
		try {
			return LocalDate.parse(string, DATE_FORMATTER);
		} catch (DateTimeParseException e) {
			return LocalDate.now();
		}
	}
}
