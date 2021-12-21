package ui.gamebase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import jsidplay2.Photos;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.download.DownloadThread;
import ui.common.download.IDownloadListener;
import ui.entities.gamebase.Games;

public class GameBasePage extends C64VBox implements UIPart {

	@FXML
	private TableView<Games> gamebaseTable;

	private Convenience convenience;
	private ImageView screenshot, photo;
	private String fileToRun;
	private final BiPredicate<File, File> FILE_TO_RUN_DETECTOR = (file,
			toAttach) -> fileToRun.length() == 0 && Convenience.LEXICALLY_FIRST_MEDIA.test(file, toAttach)
					|| fileToRun.equals(file.getName());
	private ObservableList<Games> allGames;
	private ObservableList<Games> filteredGames;

	public GameBasePage() {
	}

	public GameBasePage(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		convenience = new Convenience(util.getPlayer());
		allGames = FXCollections.<Games>observableArrayList();
		filteredGames = FXCollections.<Games>observableArrayList();
		SortedList<Games> sortedList = new SortedList<>(filteredGames);
		sortedList.comparatorProperty().bind(gamebaseTable.comparatorProperty());
		gamebaseTable.setItems(sortedList);
		gamebaseTable.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				autostart();
			}
		});
		gamebaseTable.setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				Games game = gamebaseTable.getSelectionModel().getSelectedItem();
				startGame(game);
			}
		});
		gamebaseTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				if (!newValue.getScreenshotFilename().isEmpty()) {
					try {
						URL url = new URL(util.getConfig().getOnlineSection().getGb64ScreenshotUrl()
								+ newValue.getScreenshotFilename().replace('\\', '/'));
						setScreenshot(url);
					} catch (MalformedURLException e) {
						System.err.println(e.getMessage());
					}
				}
				Image image = new Image(new ByteArrayInputStream(Photos.getPhoto("???", "???")));
				if (!newValue.getMusicians().getPhotoFilename().isEmpty()) {
					try {
						image = new Image(new URL(util.getConfig().getOnlineSection().getGb64PhotosUrl()
								+ newValue.getMusicians().getPhotoFilename().replace('\\', '/')).toExternalForm());
					} catch (MalformedURLException e) {
						System.err.println(e.getMessage());
					}
				}
				setPhoto(image);
			}
		});
	}

	@FXML
	private void autostart() {
		Games game = gamebaseTable.getSelectionModel().getSelectedItem();
		startGame(game);
	}

	private void setScreenshot(URL url) {
		if (screenshot == null) {
			screenshot = (ImageView) gamebaseTable.getScene().lookup("#gamebase_screenshot");
		}
		if (screenshot != null) {
			Platform.runLater(() -> screenshot.setImage(new Image(url.toExternalForm())));
		}
	}

	private void setPhoto(Image image) {
		if (photo == null) {
			photo = (ImageView) gamebaseTable.getScene().lookup("#gamebase_musician_photo");
		}
		if (photo != null) {
			Platform.runLater(() -> photo.setImage(image));
		}
	}

	protected void startGame(Games game) {
		if (game == null) {
			return;
		}
		if (game.getFilename().isEmpty()) {
			System.out.println("Game is not available on GameBase64: " + game.getName());
			return;
		}
		try {
			fileToRun = game.getFileToRun();

			new DownloadThread(util.getConfig(), new IDownloadListener() {

				@Override
				public void downloadStop(File downloadedFile) {
					try {
						if (downloadedFile != null
								&& convenience.autostart(downloadedFile, FILE_TO_RUN_DETECTOR, null)) {
							downloadedFile.deleteOnExit();
							Platform.runLater(() -> {
								util.setPlayingTab(GameBasePage.this);
							});
						}
					} catch (IOException | SidTuneError e) {
						// ignore
					}
				}

				@Override
				public void downloadStep(int step) {
					DoubleProperty progressProperty = util.progressProperty(gamebaseTable.getScene());
					progressProperty.setValue(step / 100.f);
				}
			}, new URL(util.getConfig().getOnlineSection().getGb64GamesUrl() + game.getFilename().replace('\\', '/')),
					false).start();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	void setGames(List<Games> games) {
		allGames.setAll(games);
		filteredGames.setAll(allGames);
	}

	void filter(String filterText) {
		filteredGames.clear();
		if (filterText.trim().length() == 0) {
			filteredGames.addAll(allGames);
		} else {
			allGames.stream()
					.filter(game -> game.getName().toLowerCase(Locale.US).contains(filterText.toLowerCase(Locale.US)))
					.forEach(filteredGames::add);
		}
	}

	public TableView<Games> getGamebaseTable() {
		return gamebaseTable;
	}

}
