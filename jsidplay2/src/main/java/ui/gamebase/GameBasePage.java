package ui.gamebase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.BiPredicate;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import libsidplay.sidtune.SidTuneError;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.entities.gamebase.Games;

public class GameBasePage extends Tab implements UIPart {

	@FXML
	private TableView<Games> gamebaseTable;

	private Convenience convenience;
	private ImageView screenshot;
	private String fileToRun;
	private final BiPredicate<File, File> FILE_TO_RUN_DETECTOR = (file,
			toAttach) -> (fileToRun.length() == 0 && Convenience.LEXICALLY_FIRST_MEDIA.test(file, toAttach))
					|| fileToRun.equals(file.getName());
	private ObservableList<Games> allGames;
	private ObservableList<Games> filteredGames;

	private UIUtil util;

	public GameBasePage(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
	}

	@FXML
	private void initialize() {
		convenience = new Convenience(util.getPlayer());
		allGames = FXCollections.<Games>observableArrayList();
		filteredGames = FXCollections.<Games>observableArrayList();
		gamebaseTable.setItems(filteredGames);
		gamebaseTable.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				Games game = gamebaseTable.getSelectionModel().getSelectedItem();
				startGame(game);
			}
		});
		gamebaseTable.setOnMousePressed((event) -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				Games game = gamebaseTable.getSelectionModel().getSelectedItem();
				startGame(game);
			}
		});
		gamebaseTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				if (newValue.getScreenshotFilename().isEmpty()) {
					System.out.println("Screenshot is not available on GameBase64: " + newValue.getName());
				} else {
					try {
						URL url = new URL(util.getConfig().getOnlineSection().getGb64ScreenshotUrl()
								+ newValue.getScreenshotFilename().replace('\\', '/'));
						if (screenshot == null) {
							screenshot = (ImageView) gamebaseTable.getScene().lookup("#screenshot");
						}
						if (screenshot != null) {
							Platform.runLater(() -> screenshot.setImage(new Image(url.toString())));
						}
					} catch (MalformedURLException e) {
						System.err.println(e.getMessage());
					}
				}
			}
		});
	}

	protected void startGame(Games game) {
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
					} catch (IOException | SidTuneError | URISyntaxException e) {
						// ignore
					}
				}

				@Override
				public void downloadStep(int step) {
					DoubleProperty progressProperty = util.progressProperty(gamebaseTable);
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
			for (Games game : allGames) {
				if (game.getName().contains(filterText)) {
					filteredGames.add(game);
				}
			}
		}
	}

	public TableView<Games> getGamebaseTable() {
		return gamebaseTable;
	}

}
