package ui.gamebase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.common.download.DownloadThread;
import ui.common.download.ProgressListener;
import ui.common.fileextension.MDBFileExtensions;
import ui.common.util.DesktopUtil;
import ui.entities.DatabaseType;
import ui.entities.PersistenceProperties;
import ui.entities.config.OnlineSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.gamebase.Games;
import ui.entities.gamebase.service.GamesService;

public class GameBase extends C64VBox implements UIPart {

	public static final String ID = "GAMEBASE";

	private static final String EXT_MDB = ".mdb";

	protected final class GameBaseListener extends ProgressListener {
		protected GameBaseListener(UIUtil util, Scene scene) {
			super(util, scene);
		}

		@Override
		public void downloaded(File downloadedFile) {
			final SidPlay2Section sidplay2 = util.getConfig().getSidplay2Section();
			try {
				if (downloadedFile == null) {
					return;
				}
				final File tmpDir = sidplay2.getTmpDir();
				TFile zip = new TFile(downloadedFile);
				TFile.cp_rp(zip, tmpDir, TArchiveDetector.ALL);
				setRoot(new File(tmpDir, zip.listFiles((dir, name) -> name.endsWith(EXT_MDB))[0].getName()));
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> {
					enableGameBase.setDisable(false);
				});
			}
		}
	}

	private static final String GB64_URL = "http://www.gb64.com";

	@FXML
	protected CheckBox enableGameBase;
	@FXML
	protected TextField dbFileField, filterField;
	@FXML
	protected TitledPane contents;
	@FXML
	protected TabPane letter;
	@FXML
	protected TextField infos, programmer, category, musician;
	@FXML
	protected TextArea comment;
	@FXML
	protected Button linkMusic;
	@FXML
	protected TextField gameBaseFile;

	private EntityManager em;
	private GamesService gamesService;

	public GameBase() {
		super();
	}

	public GameBase(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		final SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();

		filterField.setOnKeyReleased(event -> {
			// reset filter
			letter.getTabs().stream().forEach(tab -> ((GameBasePage) tab.getContent()).filter(""));

			Tab selectedTab = letter.getSelectionModel().getSelectedItem();
			String trim = filterField.getText().trim();
			OptionalInt optionalFilteredFirstChar = trim.chars().map(Character::toUpperCase).findFirst();
			OptionalInt optionalTabFirstChar = selectedTab.getText().chars().findFirst();
			if (optionalFilteredFirstChar.isPresent()
					&& optionalFilteredFirstChar.getAsInt() != optionalTabFirstChar.getAsInt()) {
				selectedTab = letter.getTabs().stream().filter(
						tab -> tab.getText().chars().findFirst().getAsInt() == optionalFilteredFirstChar.getAsInt())
						.findFirst().orElse(letter.getTabs().stream().findFirst().get());
				letter.getSelectionModel().select(selectedTab);
			}

			((GameBasePage) selectedTab.getContent()).filter(trim);
		});

		contents.setPrefHeight(Double.MAX_VALUE);

		letter.getTabs().stream()
				.forEach(tab -> ((GameBasePage) tab.getContent()).getGamebaseTable().getSelectionModel()
						.selectedItemProperty().addListener((observable, oldValue, newValue) -> selectGame(newValue)));

		Platform.runLater(() -> setRoot(sidPlay2Section.getGameBase64()));
	}

	@Override
	public void doClose() {
		disconnect();
	}

	@FXML
	private void doEnableGameBase() {
		final OnlineSection onlineSection = util.getConfig().getOnlineSection();

		if (enableGameBase.isSelected()) {
			enableGameBase.setDisable(true);
			try {
				final URL url = new URL(onlineSection.getGamebaseUrl());
				DownloadThread downloadThread = new DownloadThread(util.getConfig(),
						new GameBaseListener(util, letter.getScene()), url, true);
				downloadThread.start();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void downloadMusic() {
		final SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		final OnlineSection onlineSection = util.getConfig().getOnlineSection();

		try {
			URL url = new URL(onlineSection.getGb64MusicUrl() + linkMusic.getText().replace('\\', '/'));
			try (InputStream is = url.openStream()) {
				util.getPlayer().play(SidTune.load(linkMusic.getText(), is));
				util.setPlayingTab(this);
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			File file = PathUtils.getFile(linkMusic.getText().replace('\\', '/'), sidPlay2Section.getHvsc(),
					sidPlay2Section.getCgsc());
			try {
				util.getPlayer().play(SidTune.load(file));
				util.setPlayingTab(this);
			} catch (IOException | SidTuneError e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(MDBFileExtensions.DESCRIPTION, MDBFileExtensions.EXTENSIONS));
		setRoot(fileDialog.showOpenDialog(letter.getScene().getWindow()));
	}

	@FXML
	private void gotoURL() {
		DesktopUtil.browse(GB64_URL);
	}

	private void selectGame(Games newValue) {
		if (newValue != null) {
			comment.setText(newValue.getComment());
			category.setText(getCategory(newValue));
			infos.setText(getInfos(newValue));
			musician.setText(newValue.getMusicians().getMusician());
			programmer.setText(newValue.getProgrammers().getProgrammer());
			String sidFilename = newValue.getSidFilename();
			linkMusic.setDisable(sidFilename == null || sidFilename.isEmpty());
			linkMusic.setText(linkMusic.isDisable() ? "" : sidFilename);
		}
	}

	private String getInfos(Games newValue) {
		return String.format(util.getBundle().getString("PUBLISHER"), newValue.getYears().getYear(),
				newValue.getPublishers().getPublisher());
	}

	private String getCategory(Games newValue) {
		String genre = newValue.getGenres().getGenre();
		String parentGenre = newValue.getGenres().getParentGenres().getParentGenre();
		if (parentGenre != null && parentGenre.length() != 0) {
			return parentGenre + " - " + genre;
		}
		return genre;
	}

	private void setRoot(File file) {
		if (file != null && file.exists()) {
			Task<Void> task = new Task<Void>() {

				@Override
				public Void call() throws Exception {
					try {
						fetchGames(file);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}

	private void fetchGames(File file) {
		final SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();

		sidPlay2Section.setGameBase64(file);

		Platform.runLater(() -> {
			enableGameBase.setDisable(true);
			letter.getTabs().stream().forEach(tab -> tab.setDisable(true));
			util.progressProperty(letter.getScene()).set(ProgressIndicator.INDETERMINATE_PROGRESS);
		});

		connect(file);

		Map<Tab, List<Games>> gamesForTabs = letter.getTabs().stream()
				.collect(Collectors.toMap(Function.identity(), tab -> gamesService.select(tab.getText().charAt(0))));

		Platform.runLater(() -> {
			letter.getTabs().stream().forEach(tab -> ((GameBasePage) tab.getContent()).setGames(gamesForTabs.get(tab)));

			enableGameBase.setDisable(false);
			letter.getTabs().stream().forEach(tab -> tab.setDisable(false));

			letter.getSelectionModel().selectFirst();
			filterField.setText("");
			gameBaseFile.setText(file.getAbsolutePath());
			util.progressProperty(letter.getScene()).set(0);
		});
	}

	private void connect(File dbFile) {
		if (em != null) {
			em.close();
		}
		em = Persistence
				.createEntityManagerFactory(PersistenceProperties.GAMEBASE_DS,
						new PersistenceProperties(DatabaseType.MSACCESS, "", "", dbFile.getAbsolutePath()))
				.createEntityManager();
		gamesService = new GamesService(em);
	}

	private void disconnect() {
		if (em != null && em.isOpen()) {
			em.getEntityManagerFactory().close();
		}
	}

}
