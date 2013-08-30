package ui.gamebase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import ui.common.C64Tab;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.entities.gamebase.Games;
import ui.gamebase.listeners.GameListener;

public class GameBasePage extends C64Tab {

	private static final String GB64_SCREENSHOT_DOWNLOAD_URL = "http://www.gb64.com/Screenshots/";
	private static final String GB64_GAMES_DOWNLOAD_URL = "http://gamebase64.hardabasht.com/games/";

	@FXML
	protected TableView<Games> gamebaseTable;

	private ObservableList<Games> allGames = FXCollections
			.<Games> observableArrayList();

	private ObservableList<Games> filteredGames = FXCollections
			.<Games> observableArrayList();

	protected IDownloadListener screenShotListener;
	private GameListener gameListener;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getConfig() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		gamebaseTable.setItems(filteredGames);
		gamebaseTable.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					Games game = gamebaseTable.getSelectionModel()
							.getSelectedItem();
					startGame(game);
				}
			}
		});
		gamebaseTable.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
					Games game = gamebaseTable.getSelectionModel()
							.getSelectedItem();
					startGame(game);
				}
			}
		});
		gamebaseTable.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Games>() {

					@Override
					public void changed(
							ObservableValue<? extends Games> observable,
							Games oldValue, Games newValue) {
						if (newValue != null) {
							if (newValue.getScreenshotFilename().isEmpty()) {
								System.out
										.println("Screenshot is not available on GameBase64: "
												+ newValue.getName());
								return;
							}
							downloadStart(
									GB64_SCREENSHOT_DOWNLOAD_URL
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
		setPlayedGraphics(gamebaseTable);
		downloadStart(
				GB64_GAMES_DOWNLOAD_URL + game.getFilename().replace('\\', '/'),
				gameListener);
	}

	void setGames(List<Games> games) {
		allGames.addAll(games);
		filteredGames.addAll(allGames);
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
			DownloadThread downloadThread = new DownloadThread(getConfig(),
					listener, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
