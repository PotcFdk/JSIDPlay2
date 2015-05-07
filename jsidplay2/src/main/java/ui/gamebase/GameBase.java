package ui.gamebase;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.config.SidPlay2Section;
import ui.entities.gamebase.service.GamesService;
import ui.filefilter.MDBFileExtensions;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

public class GameBase extends Tab implements UIPart {

	public static final String ID = "GAMEBASE";

	private static final String EXT_MDB = ".mdb";

	protected final class GameBaseListener extends ProgressListener {
		protected GameBaseListener(UIUtil util, Node node) {
			super(util, node);
		}

		@Override
		public void downloaded(File downloadedFile) {
			try {
				if (downloadedFile == null) {
					return;
				}
				final SidPlay2Section sidplay2 = (SidPlay2Section) util
						.getConfig().getSidplay2Section();
				final String tmpDir = sidplay2.getTmpDir();
				TFile zip = new TFile(downloadedFile);
				TFile.cp_rp(zip, new File(tmpDir), TArchiveDetector.ALL);
				Platform.runLater(() -> {
					enableGameBase.setDisable(true);
					setLettersDisable(true);
				});
				File dbFile = new File(tmpDir,
						zip.listFiles((dir, name) -> name.endsWith(EXT_MDB))[0]
								.getName());
				sidplay2.setGameBase64(dbFile.getAbsolutePath());
				connect(dbFile);
				Platform.runLater(() -> {
					gameBaseFile.setText(dbFile.getAbsolutePath());
					enableGameBaseUI();
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

	private static final String GB64_MUSIC_DOWNLOAD_URL = "http://www.gb64.com/C64Music/";

	@FXML
	protected CheckBox enableGameBase;
	@FXML
	protected TextField dbFileField, filterField;
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

	private UIUtil util;

	public GameBase(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		filterField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				GameBasePage page = (GameBasePage) letter.getSelectionModel()
						.getSelectedItem();
				if (filterField.getText().trim().length() == 0) {
					page.filter("");
				} else {
					page.filter(filterField.getText());
				}
			}

		});

		for (Tab tab : letter.getTabs()) {
			GameBasePage page = (GameBasePage) tab;
			page.getGamebaseTable()
					.getSelectionModel()
					.selectedItemProperty()
					.addListener(
							(observable, oldValue, newValue) -> {
								if (newValue != null) {
									comment.setText(newValue.getComment());
									String genre = newValue.getGenres()
											.getGenre();
									String pGenre = newValue.getGenres()
											.getParentGenres().getParentGenre();
									if (pGenre != null && pGenre.length() != 0) {
										category.setText(pGenre + "-" + genre);
									} else {
										category.setText(genre);
									}
									infos.setText(String.format(
											util.getBundle().getString(
													"PUBLISHER"), newValue
													.getYears().getYear(),
											newValue.getPublishers()
													.getPublisher()));
									musician.setText(newValue.getMusicians()
											.getMusician());
									programmer.setText(newValue
											.getProgrammers().getProgrammer());
									String sidFilename = newValue
											.getSidFilename();
									linkMusic
											.setText(sidFilename != null ? sidFilename
													: "");
									linkMusic.setDisable(sidFilename == null
											|| sidFilename.length() == 0);
								}
							});
		}
		letter.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> {
					selectTab((GameBasePage) newValue);
				});
		Platform.runLater(() -> {
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2Section();
			String initialRoot = sidPlay2Section.getGameBase64();
			if (initialRoot != null && new File(initialRoot).exists()) {
				gameBaseFile.setText(initialRoot);
				setRoot(new File(initialRoot));
			}
		});
	}

	@FXML
	private void doEnableGameBase() {
		if (enableGameBase.isSelected()) {
			File dbFile = new File(util.getConfig().getSidplay2Section().getTmpDir(),
					"GameBase64.mdb");
			if (dbFile.exists()) {
				// There is already a database file downloaded earlier.
				// Therefore we try to connect

				SidPlay2Section sidPlay2Section = (SidPlay2Section) util
						.getConfig().getSidplay2Section();
				sidPlay2Section.setGameBase64(dbFile.getAbsolutePath());
				gameBaseFile.setText(dbFile.getAbsolutePath());
				setRoot(dbFile);
			} else {
				enableGameBase.setDisable(true);
				try {
					final URL url = new URL(util.getConfig().getOnlineSection()
							.getGamebaseUrl());
					DownloadThread downloadThread = new DownloadThread(
							util.getConfig(),
							new GameBaseListener(util, letter), url);
					downloadThread.start();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@FXML
	private void downloadMusic() {
		try {
			URL url = new URL(GB64_MUSIC_DOWNLOAD_URL
					+ linkMusic.getText().replace('\\', '/'));
			try (InputStream is = url.openStream()) {
				util.getPlayer().play(SidTune.load(linkMusic.getText(), is));
				util.setPlayingTab(this);
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(e.getMessage());
		}
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(MDBFileExtensions.DESCRIPTION,
						MDBFileExtensions.EXTENSIONS));
		File file = fileDialog.showOpenDialog(letter.getScene().getWindow());
		if (file != null) {
			gameBaseFile.setText(file.getAbsolutePath());
			SidPlay2Section sidPlay2Section = (SidPlay2Section) util
					.getConfig().getSidplay2Section();
			sidPlay2Section.setGameBase64(file.getAbsolutePath());
			File theRootFile = sidPlay2Section.getGameBase64File();
			gameBaseFile.setText(file.getAbsolutePath());
			setRoot(theRootFile);
		}
	}

	@FXML
	private void gotoURL() {
		// Open a browser URL

		// As an application we open the default browser
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(new URL(GB64_URL).toURI());
				} catch (final IOException ioe) {
					ioe.printStackTrace();
				} catch (final URISyntaxException urie) {
					urie.printStackTrace();
				}
			}
		} else {
			System.err.println("Awt Desktop is not supported!");
		}
	}

	private void setRoot(File file) {
		connect(file);
		enableGameBaseUI();
	}

	private void enableGameBaseUI() {
		enableGameBase.setDisable(false);
		setLettersDisable(false);
		letter.getSelectionModel().selectFirst();
		selectTab((GameBasePage) letter.getSelectionModel().getSelectedItem());
	}

	protected void setLettersDisable(boolean b) {
		for (Tab tab : letter.getTabs()) {
			tab.setDisable(b);
		}
	}

	protected void connect(File dbFile) {
		if (em != null) {
			em.getEntityManagerFactory().close();
		}
		em = Persistence.createEntityManagerFactory(
				PersistenceProperties.GAMEBASE_DS,
				new PersistenceProperties(dbFile, Database.MSACCESS))
				.createEntityManager();
		gamesService = new GamesService(em);
	}

	public void doClose() {
		if (em != null) {
			em.getEntityManagerFactory().close();
		}
	}

	protected void selectTab(GameBasePage page) {
		if (gamesService != null) {
			page.setGames(gamesService.select(page.getText().charAt(0)));
		}
		filterField.setText("");
	}

}
