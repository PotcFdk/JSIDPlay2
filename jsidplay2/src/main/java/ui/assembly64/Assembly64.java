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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.EnumToStringConverter;
import ui.common.UIPart;
import ui.filefilter.DiskFileFilter;

public class Assembly64 extends C64VBox implements UIPart {
	public static final String ID = "ASSEMBLY64";

	private static final int MAX_ROWS = 500;

	@FXML
	private BorderPane parent;

	@FXML
	private TextField nameField, groupField, handleField, eventField, updatedField, categoryField;

	@FXML
	private ComboBox<Integer> yearField, ratingField;

	@FXML
	private ComboBox<Age> ageField;

	@FXML
	private CheckBox searchFromStartField;

	@FXML
	private CheckBox d64Field, t64Field, d81Field, d71Field, prgField, tapField, crtField, sidField, binField, g64Field;

	@FXML
	private TableView<SearchResult> assembly64Table;

	@FXML
	private TableView<ContentEntry> contentEntryTable;

	@FXML
	private TableColumn<SearchResult, String> nameColumn, groupColumn, yearColumn, handleColumn, eventColumn,
			ratingColumn, updatedColumn;

	@FXML
	private TableColumn<SearchResult, Category> categoryColumn;

	@FXML
	private TableColumn<ContentEntry, String> contentEntryColumn;

	@FXML
	private Button prevBtn, nextBtn;

	@FXML
	private ObjectProperty<SearchResult> currentlyPlayedRowProperty;

	@FXML
	private ObjectProperty<ContentEntry> currentlyPlayedContentEntryProperty;

	@FXML
	private ContextMenu contentEntryContextMenu;

	@FXML
	private MenuItem attachDiskMenu;

	@FXML
	private TextArea errorMessageTextArea;

	private ObjectMapper objectMapper;

	private ObservableList<SearchResult> searchResults;
	private ObservableList<ContentEntry> contentEntries;

	private int searchOffset, searchStop;

	private Category[] categories;

	private SearchResult searchResult;

	private Convenience convenience;

	private AtomicBoolean autostart;

	private DiskFileFilter diskFileFilter;

	private PauseTransition pauseTransition, pauseTransitionContentEntry;
	private SequentialTransition sequentialTransition, sequentialTransitionContentEntry;

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

		categories = requestCategories();
		objectMapper = createObjectMapper();

		searchResults = FXCollections.<SearchResult>observableArrayList();
		SortedList<SearchResult> sortedSearchResultList = new SortedList<>(searchResults);
		sortedSearchResultList.comparatorProperty().bind(assembly64Table.comparatorProperty());
		assembly64Table.setItems(sortedSearchResultList);
		assembly64Table.getSelectionModel().selectedItemProperty().addListener((s, o, n) -> requestFileList(false));
		assembly64Table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		assembly64Table.setOnMousePressed(e -> requestFileList(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		assembly64Table.setOnKeyPressed(event -> requestFileList(event.getCode() == KeyCode.ENTER));

		contentEntries = FXCollections.<ContentEntry>observableArrayList();
		SortedList<ContentEntry> sortedContentEntryList = new SortedList<>(contentEntries);
		sortedContentEntryList.comparatorProperty().bind(contentEntryTable.comparatorProperty());
		contentEntryTable.setItems(sortedContentEntryList);
		contentEntryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		contentEntryTable.setOnMousePressed(e -> startContentEntry(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		contentEntryTable.setOnKeyPressed(event -> startContentEntry(event.getCode() == KeyCode.ENTER));

		contentEntryContextMenu.setOnShown(event -> {
			ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
			attachDiskMenu.setDisable(contentEntry == null || !diskFileFilter.accept(new File(contentEntry.getName())));
		});

		yearField.setItems(FXCollections.<Integer>observableArrayList(
				concat(of(0), rangeClosed(1980, Year.now().getValue())).boxed().collect(Collectors.toList())));
		yearField.getSelectionModel().select(0);
		yearField.setConverter(new ZeroContainingRatingConverter());

		ratingField.setItems(FXCollections
				.<Integer>observableArrayList(concat(of(0), rangeClosed(1, 10)).boxed().collect(Collectors.toList())));
		ratingField.getSelectionModel().select(0);
		ratingField.setConverter(new ZeroContainingRatingConverter());

		ageField.setConverter(new EnumToStringConverter<Age>(util.getBundle()));
		ageField.setItems(FXCollections.<Age>observableArrayList(Age.values()));
		ageField.getSelectionModel().select(Age.ALL);

		nameField.setOnKeyReleased(event -> newSearch());
		groupField.setOnKeyReleased(event -> newSearch());
		yearField.setOnKeyReleased(event -> newSearch());
		handleField.setOnKeyReleased(event -> newSearch());
		eventField.setOnKeyReleased(event -> newSearch());
		categoryField.setOnKeyReleased(event -> newSearch());

		Platform.runLater(() -> {
			yearField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			ratingField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			ageField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			nameField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			groupField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			handleField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			eventField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			updatedField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			categoryField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));

			contentEntryColumn.prefWidthProperty().bind(contentEntryTable.widthProperty());
			nameColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			groupColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			yearColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			handleColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			eventColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			ratingColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			updatedColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			categoryColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
			contentEntryTable.prefHeightProperty().bind(parent.heightProperty().multiply(0.2));
		});

		pauseTransition = new PauseTransition(Duration.millis(1000));
		sequentialTransition = new SequentialTransition(pauseTransition);
		sequentialTransition.setCycleCount(1);
		pauseTransition.setOnFinished(evt -> requestRelease());

		pauseTransitionContentEntry = new PauseTransition(Duration.millis(500));
		sequentialTransitionContentEntry = new SequentialTransition(pauseTransitionContentEntry);
		sequentialTransitionContentEntry.setCycleCount(1);
		pauseTransitionContentEntry.setOnFinished(evt -> requestFileList());
	}

	@Override
	public void doClose() {
		sequentialTransition.stop();
		sequentialTransitionContentEntry.stop();
	}

	@FXML
	private void searchYear() {
		newSearch();
	}

	@FXML
	private void searchRating() {
		newSearch();
	}

	@FXML
	private void searchFromStart() {
		newSearch();
	}

	@FXML
	private void searchAge() {
		newSearch();
	}

	@FXML
	private void searchName() {
		newSearch();
	}

	@FXML
	private void searchGroup() {
		newSearch();
	}

	@FXML
	private void searchHandle() {
		newSearch();
	}

	@FXML
	private void searchEvent() {
		newSearch();
	}

	@FXML
	private void searchCategory() {
		newSearch();
	}

	@FXML
	private void searchD64() {
		newSearch();
	}

	@FXML
	private void searchT64() {
		newSearch();
	}

	@FXML
	private void searchD81() {
		newSearch();
	}

	@FXML
	private void searchD71() {
		newSearch();
	}

	@FXML
	private void searchPrg() {
		newSearch();
	}

	@FXML
	private void searchTap() {
		newSearch();
	}

	@FXML
	private void searchCrt() {
		newSearch();
	}

	@FXML
	private void searchSid() {
		newSearch();
	}

	@FXML
	private void searchBin() {
		newSearch();
	}

	@FXML
	private void searchG64() {
		newSearch();
	}

	private void newSearch() {
		searchOffset = 0;
		search();
	}

	private void search() {
		searchResults.clear();
		contentEntries.clear();
		errorMessageTextArea.clear();
		sequentialTransition.playFromStart();
	}

	@FXML
	private void prevPage() {
		searchOffset -= MAX_ROWS;
		search();
	}

	@FXML
	private void nextPage() {
		searchOffset = searchStop;
		search();
	}

	@FXML
	private void autostart() {
		Optional<ContentEntry> optionalContentEntry = contentEntryTable.getSelectionModel().getSelectedItems().stream()
				.findFirst();
		if (optionalContentEntry.isPresent()) {
			ContentEntry contentEntry = optionalContentEntry.get();
			try {
				File file = requestContentEntry(contentEntry);
				if (convenience.autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null)) {
					util.setPlayingTab(this);
					currentlyPlayedRowProperty.set(searchResult);
					currentlyPlayedContentEntryProperty.set(contentEntry);
				}
			} catch (IOException | SidTuneError | URISyntaxException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
			}
		}
	}

	@FXML
	private void attachDisk() {
		Optional<ContentEntry> optionalContentEntry = contentEntryTable.getSelectionModel().getSelectedItems().stream()
				.findFirst();
		if (optionalContentEntry.isPresent()) {
			ContentEntry contentEntry = optionalContentEntry.get();
			try {
				File file = requestContentEntry(contentEntry);
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
			}
		}
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		JodaModule module = new JodaModule();
		module.addDeserializer(Category.class, new CategoryDeserializer(categories));
		objectMapper.registerModule(module);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return objectMapper;
	}

	private Category[] requestCategories() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/categories").build();

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			String result = response.readEntity(String.class);
			return (Category[]) new ObjectMapper().readValue(result, Category[].class);
		} catch (IOException e) {
			e.printStackTrace();
			return new Category[0];
		}
	}

	private void requestRelease() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/find2").path(
				"/{name}/{group}/{year}/{handle}/{event}/{rating}/{category}/{fromstart}/{d64}/{t64}/{d71}/{d81}/{prg}/{tap}/{crt}/{sid}/{bin}/{g64}/{or}/{days}")
				.queryParam("offset", searchOffset).build(get(nameField), get(groupField), get(yearField),
						get(handleField), get(eventField), get(ratingField), getCategory(categoryField),
						getSearchFromStart(searchFromStartField), get(d64Field), get(t64Field), get(d71Field),
						get(d81Field), get(prgField), get(tapField), get(crtField), get(sidField), get(binField),
						get(g64Field), "n", getAge(ageField));

		if (uri.getPath().contains("***/***/***/***/***/***/***")) {
			return;
		}
		String result = null;
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			result = response.readEntity(String.class);

			Object start = response.getHeaderString("start");
			searchOffset = start != null ? Integer.parseInt(start.toString()) : 0;
			prevBtn.setDisable(start == null || searchOffset == 0);

			Object stop = response.getHeaderString("stop");
			searchStop = stop != null ? Integer.parseInt(stop.toString()) : searchOffset + MAX_ROWS;
			nextBtn.setDisable(stop == null);

			searchResults.setAll(objectMapper.readValue(result, SearchResult[].class));
		} catch (JsonParseException | JsonMappingException e) {
			try {
				ErrorMessage errorMessage = objectMapper.readValue(result, ErrorMessage.class);
				errorMessageTextArea.setText(String.join("\n",
						errorMessage.getStatus() + ": " + errorMessage.getError(), errorMessage.getMessage()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void requestFileList(SearchResult searchResult) {
		this.searchResult = searchResult;
		sequentialTransitionContentEntry.playFromStart();
	}

	private void requestFileList(boolean doAutostart) {
		final SearchResult searchResult = assembly64Table.getSelectionModel().getSelectedItem();
		if (searchResult != null) {
			requestFileList(searchResult);
		}
		autostart.set(doAutostart);
	}

	private void requestFileList() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/u64/entry").path("/{id}/{category}")
				.build(searchResult.getId(), searchResult.getCategory().getId());
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			ContentEntrySearchResult contentEntry = (ContentEntrySearchResult) objectMapper
					.readValue(response.readEntity(String.class), ContentEntrySearchResult.class);
			contentEntries.setAll(contentEntry.getContentEntry());
			if (autostart.getAndSet(false)) {
				contentEntryTable.getSelectionModel().select(contentEntries.stream().findFirst().orElse(null));
				autostart();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File requestContentEntry(ContentEntry contentEntry) throws FileNotFoundException, IOException {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/u64/binary").path("/{id}/{category}/{contentEntryId}")
				.build(searchResult.getId(), searchResult.getCategory().getId(), contentEntry.getId());

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			File tempFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), contentEntry.getName());
			tempFile.deleteOnExit();
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(response.readEntity(byte[].class));
			}
			return tempFile;
		}
	}

	private void startContentEntry(boolean doAutostart) {
		final ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
		if (contentEntry != null && doAutostart) {
			autostart();
		}
	}

	private String get(ComboBox<Integer> field) {
		Integer value = field.getSelectionModel().getSelectedItem();
		if (value == 0) {
			return "***";
		}
		return String.valueOf(value);
	}

	private int getAge(ComboBox<Age> field) {
		Age value = field.getSelectionModel().getSelectedItem();
		return value != null ? value.getDays() : -1;
	}

	private String get(TextField field) {
		String value = field.getText().trim();
		if (value.isEmpty()) {
			return "***";
		}
		return value;
	}

	private String getCategory(TextField field) {
		String value = field.getText().trim();
		if (value.isEmpty()) {
			return "***";
		}
		return Arrays.asList(categories).stream().filter(category -> category.getDescription().contains(value))
				.map(category -> String.valueOf(category.getId())).findFirst().orElse("***");
	}

	private String get(CheckBox field) {
		return field.isSelected() ? "Y" : "N";
	}

	private String getSearchFromStart(CheckBox field) {
		return field.isSelected() ? "n" : "y";
	}

}
