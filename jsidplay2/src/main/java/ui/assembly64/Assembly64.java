package ui.assembly64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.EnumToStringConverter;
import ui.common.NumberToStringConverter;
import ui.common.UIPart;

public class Assembly64 extends C64VBox implements UIPart {
	private static final String HTTP_HACKERSWITHSTYLE_DDNS_NET_8080 = "http://hackerswithstyle.ddns.net:8080";

	public static final String ID = "ASSEMBLY64";

	private static final int MAX_ROWS = 500;

	@FXML
	Button prev, next;

	@FXML
	private TableView<SearchResult> assembly64Table;

	@FXML
	private TableView<ContentEntry> contentEntryTable;

	@FXML
	private Menu programMenu;

	@FXML
	private TableColumn<SearchResult, String> nameColumn, groupColumn, yearColumn, handleColumn, eventColumn,
			ratingColumn, updatedColumn, categoryColumn;

	@FXML
	private TableColumn<ContentEntry, String> contentEntryColumn;

	@FXML
	private TextField nameField, groupField, handleField, eventField, categoryField;

	@FXML
	private ComboBox<Integer> yearField, ratingField;

	@FXML
	private ComboBox<Age> ageField;

	@FXML
	private CheckBox searchFromStartField;

	@FXML
	private CheckBox d64Field, t64Field, d81Field, d71Field, prgField, tapField, crtField, sidField, binField, g64Field;

	@FXML
	private ContextMenu contextMenu, contentEntryContextMenu;

	@FXML
	private MenuItem getProgramEntriesMenu, attachDiskMenu;

	private ObservableList<SearchResult> searchResults;

	private ObservableList<ContentEntry> contentEntries;

	private int searchOffset, searchStop;

	private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
			false);

	private String id, category;

	private Convenience convenience;

	public Assembly64() {
	}

	public Assembly64(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		convenience = new Convenience(util.getPlayer());

		searchResults = FXCollections.<SearchResult>observableArrayList();
		SortedList<SearchResult> sortedList = new SortedList<>(searchResults);
		sortedList.comparatorProperty().bind(assembly64Table.comparatorProperty());
		assembly64Table.setItems(searchResults);
		assembly64Table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		contentEntries = FXCollections.<ContentEntry>observableArrayList();
		SortedList<ContentEntry> sortedProgramEntryList = new SortedList<>(contentEntries);
		sortedProgramEntryList.comparatorProperty().bind(contentEntryTable.comparatorProperty());
		contentEntryTable.setItems(contentEntries);
		contentEntryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		contentEntryTable.setOnMousePressed(event -> {
			final ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
			if (contentEntry != null && event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				start();
			}
		});
		contentEntryContextMenu.setOnShown(event -> {
			ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItem();
			boolean disable = contentEntry == null || !contentEntry.getName().toLowerCase(Locale.US).endsWith(".d64");
			attachDiskMenu.setDisable(disable);
		});
		contentEntryColumn.prefWidthProperty().bind(contentEntryTable.widthProperty());

		nameColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		groupColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		yearColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		handleColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		eventColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		ratingColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		updatedColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
		categoryColumn.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));

		nameField.setOnKeyReleased(event -> newSearch());

		groupField.setOnKeyReleased(event -> newSearch());

		yearField.setConverter(new NumberToStringConverter<Integer>(0));
		yearField.setItems(FXCollections.<Integer>observableArrayList(
				IntStream.concat(IntStream.of(0), IntStream.rangeClosed(1980, Year.now().getValue())).boxed()
						.collect(Collectors.toList())));
		yearField.setOnKeyReleased(event -> newSearch());

		yearField.getSelectionModel().select(0);
		yearField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));

		handleField.setOnKeyReleased(event -> newSearch());

		eventField.setOnKeyReleased(event -> newSearch());

		ratingField.setConverter(new NumberToStringConverter<Integer>(0));
		ratingField.setItems(FXCollections.<Integer>observableArrayList(
				IntStream.concat(IntStream.of(0), IntStream.rangeClosed(1, 9)).boxed().collect(Collectors.toList())));
		ratingField.getSelectionModel().select(0);
		ratingField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));

		categoryField.setOnKeyReleased(event -> newSearch());

		ageField.setConverter(new EnumToStringConverter<Age>(util.getBundle()));
		ageField.setItems(FXCollections.<Age>observableArrayList(Age.values()));
		ageField.getSelectionModel().select(null);
		ageField.prefWidthProperty().bind(assembly64Table.widthProperty().multiply(0.125));
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
	private void getProgramEntries() {
		SearchResult searchResult = assembly64Table.getSelectionModel().getSelectedItems().stream().findFirst()
				.orElse(null);
		if (searchResult != null) {
			listFiles(searchResult.getId(), searchResult.getCategory());
		}
	}

	@FXML
	private void start() {
		ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItems().stream().findFirst()
				.orElse(null);
		if (contentEntry != null) {
			try {
				byte[] byteArray = download(id, category, contentEntry.getId());
				if (byteArray != null) {
					String filename = contentEntry.getName();
					File tempFile = File.createTempFile(PathUtils.getFilenameWithoutSuffix(filename),
							PathUtils.getFilenameSuffix(filename),
							new File(util.getConfig().getSidplay2Section().getTmpDir()));
					tempFile.deleteOnExit();
					try (FileOutputStream fos = new FileOutputStream(tempFile)) {
						fos.write(byteArray);
					}
					if (convenience.autostart(tempFile, Convenience.LEXICALLY_FIRST_MEDIA, null)) {
//					util.setPlayingTab(this);
					}
				}
			} catch (IOException | SidTuneError | URISyntaxException e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
			}
		}
	}

	@FXML
	private void attachDisk() {
		ContentEntry contentEntry = contentEntryTable.getSelectionModel().getSelectedItems().stream().findFirst()
				.orElse(null);
		if (contentEntry != null) {
			try {
				byte[] byteArray = download(id, category, contentEntry.getId());
				if (byteArray != null) {
					String filename = contentEntry.getName();
					File tempFile = File.createTempFile(PathUtils.getFilenameWithoutSuffix(filename),
							PathUtils.getFilenameSuffix(filename),
							new File(util.getConfig().getSidplay2Section().getTmpDir()));
					tempFile.deleteOnExit();
					try (FileOutputStream fos = new FileOutputStream(tempFile)) {
						fos.write(byteArray);
					}
					util.getPlayer().insertDisk(tempFile);
				}
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", contentEntry.getName()));
			}
		}
	}

	private void newSearch() {
		searchOffset = 0;
		search();
	}

	private void search() {
		if (nameField.getText().length() < 3) {
			return;
		}
		searchResults.clear();
		Platform.runLater(() -> {
			URI uri = UriBuilder.fromPath(HTTP_HACKERSWITHSTYLE_DDNS_NET_8080 + "/leet/search2/find2").path(
					"/{name}/{group}/{year}/{handle}/{event}/{rating}/{category}/{fromstart}/{d64}/{t64}/{d71}/{d81}/{prg}/{tap}/{crt}/{sid}/{bin}/{g64}/{or}/{days}")
					.queryParam("offset", searchOffset).build(getName(nameField), get(groupField), get(yearField),
							get(handleField), get(eventField), get(ratingField), get(categoryField),
							get(searchFromStartField), get(d64Field), get(t64Field), get(d71Field), get(d81Field),
							get(prgField), get(tapField), get(crtField), get(sidField), get(binField), get(g64Field),
							"n", -1);

			Response response = null;
			try {
				Client client = ClientBuilder.newClient();
				WebTarget target = client.target(uri);
				response = target.request().get();

				System.out.println("start=" + response.getHeaders().get("start"));
				System.out.println("stop=" + response.getHeaders().get("stop"));

				Object start = response.getHeaders().getFirst("start");
				searchOffset = start != null ? Integer.parseInt(start.toString()) : 0;
				prev.setDisable(start == null || searchOffset == 0);

				Object stop = response.getHeaders().getFirst("stop");
				searchStop = stop != null ? Integer.parseInt(stop.toString()) : searchOffset + MAX_ROWS;
				next.setDisable(stop == null);

				String result = response.readEntity(String.class);
				searchResults.setAll(objectMapper.readValue(result, SearchResult[].class));
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (response != null) {
					response.close();
				}
			}
		});
	}

	private void listFiles(String id, String category) {
		Platform.runLater(() -> {
			URI uri = UriBuilder.fromPath(HTTP_HACKERSWITHSTYLE_DDNS_NET_8080 + "/leet/u64/entry")
					.path("/{id}/{category}").build(id, category);

			Response response = null;
			try {
				Client client = ClientBuilder.newClient();
				WebTarget target = client.target(uri);
				System.out.println(uri);
				response = target.request().get();
				String result = response.readEntity(String.class);
				System.out.println(result);
				ProgramSearchResult contentEntry = (ProgramSearchResult) objectMapper.readValue(result,
						ProgramSearchResult.class);
				contentEntries.setAll(contentEntry.getContentEntry());
				this.id = id;
				this.category = category;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (response != null) {
					response.close();
				}
			}
		});
	}

	private byte[] download(String id, String category, String contentEntryId) {
		URI uri = UriBuilder.fromPath(HTTP_HACKERSWITHSTYLE_DDNS_NET_8080 + "/leet/u64/binary")
				.path("/{id}/{category}/{contentEntryId}").build(id, category, contentEntryId);

		Response response = null;
		try {
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(uri);
			System.out.println(uri);
			response = target.request().get();
			return response.readEntity(byte[].class);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	private String get(ComboBox<Integer> field) {
		Integer value = field.getSelectionModel().getSelectedItem();
		return value.intValue() != 0 ? value.toString() : "***";
	}

	private String get(TextField field) {
		String value = field.getText();
		return !value.isEmpty() ? value : "***";
	}

	private String getName(TextField field) {
		String value = field.getText();
		return value.length() > 2 ? value : " ";
	}

	private Object get(CheckBox field) {
		return field.isSelected() ? "Y" : "N";
	}
}
