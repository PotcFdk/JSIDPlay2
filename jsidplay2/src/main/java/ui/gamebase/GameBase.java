package ui.gamebase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import libsidutils.PathUtils;
import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;
import ui.common.C64Tab;
import ui.download.DownloadThread;
import ui.download.IDownloadListener;
import ui.download.ProgressListener;
import ui.entities.PersistenceUtil;
import ui.entities.gamebase.Games;
import ui.entities.gamebase.service.ConfigService;
import ui.entities.gamebase.service.GamesService;
import ui.gamebase.listeners.GameListener;
import ui.gamebase.listeners.MusicListener;

public class GameBase extends C64Tab {

	private final class GameBaseListener extends ProgressListener {
		private GameBaseListener(DoubleProperty progress) {
			super(progress);
		}

		@Override
		public void downloaded(File downloadedFile) {
			if (downloadedFile == null) {
				enableGameBase.setDisable(false);
				return;
			}
			try {

				ZipFileProxy zip = new ZipFileProxy(downloadedFile);
				for (File zipEntry : zip.listFiles()) {
					if (zipEntry.isFile()) {
						ZipEntryFileProxy.extractFromZip(
								(ZipEntryFileProxy) zipEntry, getConfig()
										.getSidplay2().getTmpDir());
					}
				}
				connect(new File(downloadedFile.getParent(),
						PathUtils.getBaseNameNoExt(downloadedFile)));
				setLettersDisable(false);
				letter.getSelectionModel().selectFirst();
				selectTab((GameBasePage) letter.getSelectionModel()
						.getSelectedItem());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				enableGameBase.setDisable(false);
			}
		}
	}

	private static final String GB64_MUSIC_DOWNLOAD_URL = "http://www.gb64.com/C64Music/";

	@FXML
	private CheckBox enableGameBase;
	@FXML
	private TextField dbFileField, filterField;
	@FXML
	private TabPane letter;
	@FXML
	private ImageView screenshot;
	@FXML
	private TextField infos, programmer, category, musician;
	@FXML
	private TextArea comment;
	@FXML
	private Button linkMusic;

	private EntityManager em;
	private ConfigService configService;
	private GamesService gamesService;

	public List<File> lastScreenshot = new ArrayList<File>();

	private DoubleProperty progress = new SimpleDoubleProperty();

	public DoubleProperty getProgressValue() {
		return progress;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}

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

			// XXX JavaFX: better initialization support using constructor
			// arguments?
			page.setConfig(getConfig());
			page.setPlayer(getPlayer());
			page.initialize(location, resources);

			page.setScreenShotListener(new ProgressListener(progress) {

				@Override
				public void downloaded(File downloadedFile) {
					if (downloadedFile == null) {
						return;
					}
					downloadedFile.deleteOnExit();
					try {
						synchronized (lastScreenshot) {
							for (File file : lastScreenshot) {
								file.delete();
							}
							lastScreenshot.clear();
							lastScreenshot.add(downloadedFile);
						}
						final URL resource = downloadedFile.toURI().toURL();
						Platform.runLater(new Runnable() {

							@Override
							public void run() {
								showScreenshot(resource);
							}
						});
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			});
			page.setGameListener(new GameListener(this, progress, getPlayer(),
					getConfig()));
			page.getGamebaseTable().getSelectionModel().selectedItemProperty()
					.addListener(new ChangeListener<Games>() {

						@Override
						public void changed(
								ObservableValue<? extends Games> observable,
								Games oldValue, Games newValue) {
							if (newValue != null) {
								comment.setText(newValue.getComment());
								String genre = newValue.getGenres().getGenre();
								String pGenre = newValue.getGenres()
										.getParentGenres().getParentGenre();
								if (pGenre != null && pGenre.length() != 0) {
									category.setText(pGenre + "-" + genre);
								} else {
									category.setText(genre);
								}
								infos.setText(String.format(getBundle()
										.getString("PUBLISHER"), newValue
										.getYears().getYear(), newValue
										.getPublishers().getPublisher()));
								musician.setText(newValue.getMusicians()
										.getMusician());
								programmer.setText(newValue.getProgrammers()
										.getProgrammer());
								String sidFilename = newValue.getSidFilename();
								linkMusic
										.setText(sidFilename != null ? sidFilename
												: "");
								linkMusic.setDisable(sidFilename == null
										|| sidFilename.length() == 0);
							}
						}
					});
		}
		letter.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Tab>() {

					@Override
					public void changed(
							ObservableValue<? extends Tab> observable,
							Tab oldValue, Tab newValue) {
						selectTab((GameBasePage) newValue);
					}
				});
	}

	@FXML
	private void doEnableGameBase() {
		if (enableGameBase.isSelected()) {
			enableGameBase.setDisable(true);
			File dbFile = new File(getConfig().getSidplay2().getTmpDir(),
					"gb64.properties");
			if (dbFile.exists()) {
				// There is already a database file downloaded earlier.
				// Therefore we try to connect

				connect(new File(getConfig().getSidplay2().getTmpDir(), "gb64"));

				// Check version of GB64
				if (configService.checkVersion()) {
					// Version check is positive
					enableGameBase.setDisable(false);
					setLettersDisable(false);
					letter.getSelectionModel().selectFirst();
					selectTab((GameBasePage) letter.getSelectionModel()
							.getSelectedItem());
					return;
				} else {
					System.err
							.println("Version is different or database is broken,"
									+ " re-download");
					disconnect();
				}

			}
			downloadStart(getConfig().getOnline().getGamebaseUrl(),
					new GameBaseListener(progress));
		}
	}

	@FXML
	private void downloadMusic() {
		downloadStart(
				GB64_MUSIC_DOWNLOAD_URL
						+ linkMusic.getText().replace('\\', '/'),
				new MusicListener(this, progress));
	}

	private void downloadStart(String url, IDownloadListener listener) {
		try {
			DownloadThread downloadThread = new DownloadThread(getConfig(),
					listener, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void setLettersDisable(boolean b) {
		for (Tab tab : letter.getTabs()) {
			tab.setDisable(b);
		}
	}

	private void connect(File dbFile) {
		disconnect();
		em = Persistence.createEntityManagerFactory(
				PersistenceUtil.GAMEBASE_DS, new PersistenceUtil(dbFile))
				.createEntityManager();
		gamesService = new GamesService(em);
		configService = new ConfigService(em);
	}

	private void disconnect() {
		if (em != null && em.isOpen()) {
			em.close();
			EntityManagerFactory emf = em.getEntityManagerFactory();
			if (emf != null && emf.isOpen()) {
				emf.close();
			}
		}
		enableGameBase.setDisable(false);
		setLettersDisable(true);
	}

	private void showScreenshot(final URL resource) {
		Image image = new Image(resource.toString());
		if (image != null) {
			screenshot.setImage(image);
		}
	}

	private void selectTab(GameBasePage page) {
		page.setGames(gamesService.select(page.getText().charAt(0)));
		filterField.setText("");
	}

}
