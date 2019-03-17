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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import ui.directory.Directory;
import ui.filefilter.DiskFileFilter;

public class Assembly64 extends C64VBox implements UIPart {
	public static final String ID = "ASSEMBLY64";

	private float FIELD_WIDTH = 1 / 8f;

	private static final int MAX_ROWS = 500;

	@FXML
	private BorderPane parent;

	@FXML
	private TextField nameField, groupField, handleField, eventField, updatedField;

	@FXML
	private ComboBox<Integer> yearField, ratingField;

	@FXML
	private ComboBox<Category> categoryField;

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
	private Directory directory;

	@FXML
	private ObjectProperty<SearchResult> currentlyPlayedRowProperty;

	@FXML
	private ObjectProperty<ContentEntry> currentlyPlayedContentEntryProperty;

	@FXML
	private ContextMenu contentEntryContextMenu;

	@FXML
	private MenuItem attachDiskMenu, startMenu;

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

		contentEntries = FXCollections.<ContentEntry>observableArrayList();
		SortedList<ContentEntry> sortedContentEntryList = new SortedList<>(contentEntries);
		sortedContentEntryList.comparatorProperty().bind(contentEntryTable.comparatorProperty());
		contentEntryTable.setItems(sortedContentEntryList);
		contentEntryTable.getSelectionModel().selectedItemProperty()
				.addListener((s, o, n) -> getContentEntryFile(false));
		contentEntryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		contentEntryTable.setOnMousePressed(e -> getContentEntryFile(e.isPrimaryButtonDown() && e.getClickCount() > 1));
		contentEntryTable.setOnKeyReleased(event -> getContentEntryFile(event.getCode() == KeyCode.ENTER));

		contentEntryContextMenu.setOnShown(event -> {
			ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
			attachDiskMenu.setDisable(contentEntry == null || !diskFileFilter.accept(new File(contentEntry.getName())));
			startMenu.setDisable(contentEntry == null);
		});

		contentEntryColumn.prefWidthProperty().bind(contentEntryTable.widthProperty());
		directory.getAutoStartFileProperty().addListener((s, o, n) -> autostart(n));

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

		Platform.runLater(() -> {
			for (Control field : new Control[] { yearField, ratingField, ageField, nameField, groupField, handleField,
					eventField, updatedField, categoryField }) {
				field.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(FIELD_WIDTH));
			}
			for (TableColumn<?, ?> column : new TableColumn[] { nameColumn, groupColumn, yearColumn, handleColumn,
					eventColumn, ratingColumn, updatedColumn, categoryColumn }) {
				column.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(FIELD_WIDTH));
			}
			contentEntryTable.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));
			directory.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.5));

			contentEntryTable.prefHeightProperty().bind(parent.heightProperty().multiply(0.4));
			directory.prefHeightProperty().bind(parent.heightProperty().multiply(0.4));
		});

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
		} catch (IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private void requestSearchResults() {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/search2/find2").path(
				"/{name}/{group}/{year}/{handle}/{event}/{rating}/{category}/{fromstart}/{d64}/{t64}/{d71}/{d81}/{prg}/{tap}/{crt}/{sid}/{bin}/{g64}/{or}/{days}")
				.queryParam("offset", searchOffset).build(get(nameField), get(groupField), get(yearField),
						get(handleField), get(eventField), get(ratingField), getCategory(categoryField),
						get(searchFromStartField), get(d64Field), get(t64Field), get(d71Field), get(d81Field),
						get(prgField), get(tapField), get(crtField), get(sidField), get(binField), get(g64Field), "n",
						getAge(ageField));

		if (uri.getPath().contains("***/***/***/***/***/***/***")) {
			return;
		}
		String result = null;
		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			result = response.readEntity(String.class);

			Object start = response.getHeaderString("start");
			searchOffset = start != null ? Integer.parseInt(String.valueOf(start)) : 0;
			prevBtn.setDisable(start == null || searchOffset == 0);

			Object stop = response.getHeaderString("stop");
			searchStop = stop != null ? Integer.parseInt(String.valueOf(stop)) : searchOffset + MAX_ROWS;
			nextBtn.setDisable(stop == null);

			searchResults.setAll(objectMapper.readValue(result, SearchResult[].class));
		} catch (IOException e) {
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
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/u64/entry").path("/{id}/{category}")
				.build(searchResult.getId(), searchResult.getCategory().getId());

		try (Response response = ClientBuilder.newClient().target(uri).request().get()) {
			ContentEntrySearchResult contentEntry = (ContentEntrySearchResult) objectMapper
					.readValue(response.readEntity(String.class), ContentEntrySearchResult.class);
			contentEntries.setAll(contentEntry.getContentEntry());
			contentEntryTable.getSelectionModel().select(contentEntries.stream().findFirst().orElse(null));
			if (autostart.getAndSet(false)) {
				autostart();
			}
		} catch (IOException e) {
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
		} catch (IOException e) {
			System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
		}
		directory.loadPreview(contentEntryFile);
		if (doAutostart) {
			autostart();
		}
	}

	private File requestContentEntry(ContentEntry contentEntry) throws FileNotFoundException, IOException {
		String assembly64Url = util.getConfig().getOnlineSection().getAssembly64Url();
		URI uri = UriBuilder.fromPath(assembly64Url + "/leet/u64/binary").path("/{id}/{category}/{contentEntryId}")
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

	private String getCategory(ComboBox<Category> field) {
		Category category = field.getValue();
		if (category.equals(Category.ALL)) {
			return "***";
		}
		return String.valueOf(category.getId());
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
