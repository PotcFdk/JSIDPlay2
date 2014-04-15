package ui.gamebase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.entities.config.Configuration;
import ui.entities.gamebase.Games;
import ui.gamebase.listeners.GameListener;

public class GameBasePage extends Tab implements UIPart {

	private static final String GB64_SCREENSHOT_DOWNLOAD_URL = "http://www.gb64.com/Screenshots/";
	private static final String GB64_GAMES_DOWNLOAD_URL = "http://gamebase64.hardabasht.com/games/";

	@FXML
	private TableView<Games> gamebaseTable;

	private UIUtil util;

	private ObservableList<Games> allGames;

	private ObservableList<Games> filteredGames;

	private IDownloadListener screenShotListener;
	private GameListener gameListener;

	public GameBasePage(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		util = new UIUtil(consolePlayer, player, config, this);
		setContent((Node) util.parse());
	}

	@FXML
	private void initialize() {
		allGames = FXCollections.<Games> observableArrayList();
		filteredGames = FXCollections.<Games> observableArrayList();
		gamebaseTable.setItems(filteredGames);
		gamebaseTable.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				Games game = gamebaseTable.getSelectionModel()
						.getSelectedItem();
				startGame(game);
			}
		});
		gamebaseTable.setOnMousePressed((event) -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				Games game = gamebaseTable.getSelectionModel()
						.getSelectedItem();
				startGame(game);
			}
		});
		gamebaseTable
				.getSelectionModel()
				.selectedItemProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							if (newValue != null) {
								if (newValue.getScreenshotFilename().isEmpty()) {
									System.out
											.println("Screenshot is not available on GameBase64: "
													+ newValue.getName());
								} else {
									downloadStart(GB64_SCREENSHOT_DOWNLOAD_URL
											+ newValue.getScreenshotFilename()
													.replace('\\', '/'),
											screenShotListener);
								}
							}
						});
	}

	protected void startGame(Games game) {
		if (game.getFilename().isEmpty()) {
			System.out.println("Game is not available on GameBase64: "
					+ game.getName());
			return;
		}
		gameListener.setFileToRun(game.getFileToRun());
		util.setPlayedGraphics(gamebaseTable);
		downloadStart(
				GB64_GAMES_DOWNLOAD_URL + game.getFilename().replace('\\', '/'),
				gameListener);
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

	public void setScreenShotListener(IDownloadListener screenShotListener) {
		this.screenShotListener = screenShotListener;
	}

	public void setGameListener(GameListener gameListener) {
		this.gameListener = gameListener;
	}

	protected void downloadStart(String url, IDownloadListener listener) {
		try {
			DownloadThread downloadThread = new DownloadThread(
					util.getConfig(), listener, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
