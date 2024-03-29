package ui.gamebase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
import ui.common.filefilter.MDBFileExtensions;
import ui.common.util.DesktopUtil;
import ui.entities.DatabaseType;
import ui.entities.PersistenceProperties;
import ui.entities.config.SidPlay2Section;
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
			try {
				if (downloadedFile == null) {
					return;
				}
				final SidPlay2Section sidplay2 = util.getConfig().getSidplay2Section();
				final File tmpDir = sidplay2.getTmpDir();
				TFile zip = new TFile(downloadedFile);
				TFile.cp_rp(zip, tmpDir, TArchiveDetector.ALL);
				Platform.runLater(() -> {
					enableGameBase.setDisable(true);
					setLettersDisable(true);
				});
				File dbFile = new File(tmpDir, zip.listFiles((dir, name) -> name.endsWith(EXT_MDB))[0].getName());
				sidplay2.setGameBase64(dbFile);
				setRoot(dbFile);
				Platform.runLater(() -> {
					gameBaseFile.setText(dbFile.getAbsolutePath());
				});
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
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
		filterField.setOnKeyReleased(event -> {
			GameBasePage page = (GameBasePage) letter.getSelectionModel().getSelectedItem().getContent();
			if (filterField.getText().trim().length() == 0) {
				page.filter("");
			} else {
				page.filter(filterField.getText());
			}
		});

		contents.setPrefHeight(Double.MAX_VALUE);
		for (Tab tab : letter.getTabs()) {
			GameBasePage page = (GameBasePage) tab.getContent();
			if (page.getGamebaseTable() != null) {
				page.getGamebaseTable().getSelectionModel().selectedItemProperty()
						.addListener((observable, oldValue, newValue) -> {
							if (newValue != null) {
								comment.setText(newValue.getComment());
								String genre = newValue.getGenres().getGenre();
								String pGenre = newValue.getGenres().getParentGenres().getParentGenre();
								if (pGenre != null && pGenre.length() != 0) {
									category.setText(pGenre + "-" + genre);
								} else {
									category.setText(genre);
								}
								infos.setText(String.format(util.getBundle().getString("PUBLISHER"),
										newValue.getYears().getYear(), newValue.getPublishers().getPublisher()));
								musician.setText(newValue.getMusicians().getMusician());
								programmer.setText(newValue.getProgrammers().getProgrammer());
								String sidFilename = newValue.getSidFilename();
								linkMusic.setText(sidFilename != null ? sidFilename : "");
								linkMusic.setVisible(sidFilename != null && sidFilename.length() > 0);
							}
						});
			}
		}
		letter.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			selectTab(newValue);
		});
		Platform.runLater(() -> {
			SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
			File initialRoot = sidPlay2Section.getGameBase64();
			if (initialRoot != null && initialRoot.exists()) {
				gameBaseFile.setText(initialRoot.getAbsolutePath());
				setRoot(initialRoot);
			}
		});
	}

	@FXML
	private void doEnableGameBase() {
		if (enableGameBase.isSelected()) {
			enableGameBase.setDisable(true);
			try {
				final URL url = new URL(util.getConfig().getOnlineSection().getGamebaseUrl());
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
		try {
			URL url = new URL(
					util.getConfig().getOnlineSection().getGb64MusicUrl() + linkMusic.getText().replace('\\', '/'));
			try (InputStream is = url.openStream()) {
				util.getPlayer().play(SidTune.load(linkMusic.getText(), is));
				util.setPlayingTab(this);
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
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
		File file = fileDialog.showOpenDialog(letter.getScene().getWindow());
		if (file != null) {
			gameBaseFile.setText(file.getAbsolutePath());
			SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
			sidPlay2Section.setGameBase64(file);
			File theRootFile = sidPlay2Section.getGameBase64();
			gameBaseFile.setText(file.getAbsolutePath());
			setRoot(theRootFile);
		}
	}

	@FXML
	private void gotoURL() {
		DesktopUtil.browse(GB64_URL);
	}

	private void setRoot(File file) {
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				connect(file);
				enableGameBaseUI();
				return null;
			}
		};
		new Thread(task).start();
	}

	private void enableGameBaseUI() {
		enableGameBase.setDisable(false);
		setLettersDisable(false);
		letter.getSelectionModel().selectFirst();
		selectTab(letter.getSelectionModel().getSelectedItem());
	}

	protected void setLettersDisable(boolean b) {
		for (Tab tab : letter.getTabs()) {
			tab.setDisable(b);
		}
	}

	protected void connect(File dbFile) {
		if (em != null) {
			em.close();
		}
		em = Persistence
				.createEntityManagerFactory(PersistenceProperties.GAMEBASE_DS,
						new PersistenceProperties(DatabaseType.MSACCESS, "", "", dbFile.getAbsolutePath()))
				.createEntityManager();
		gamesService = new GamesService(em);
	}

	@Override
	public void doClose() {
		if (em != null && em.isOpen()) {
			em.close();
		}
	}

	protected void selectTab(Tab newValue) {
		if (gamesService != null) {
			((GameBasePage) newValue.getContent()).setGames(gamesService.select(newValue.getText().charAt(0)));
		}
		filterField.setText("");
	}

}
