package applet.gamebase;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.TuneTab;
import applet.collection.Picture;
import applet.download.DownloadThread;
import applet.download.IDownloadListener;
import applet.entities.PersistenceProperties;
import applet.entities.gamebase.Games;
import applet.entities.gamebase.service.ConfigService;
import applet.entities.gamebase.service.GamesService;
import applet.events.UIEvent;
import applet.gamebase.listeners.GameBaseListener;
import applet.gamebase.listeners.GameListener;
import applet.gamebase.listeners.MusicListener;
import applet.gamebase.listeners.ScreenShotListener;

public class GameBase extends TuneTab {

	public static final String EXPECTED_VERSION = "1";

	protected final class GameSelectionListener implements
			ListSelectionListener {
		private final GameBasePage page;

		protected GameSelectionListener(GameBasePage page) {
			this.page = page;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void valueChanged(ListSelectionEvent e) {
			int row1 = page.gamebasetable.getSelectedRow();
			if (row1 != -1 && !e.getValueIsAdjusting()) {
				int row = page.rowSorter.convertRowIndexToModel(row1);
				Vector<Games> vector = (Vector<Games>) page.dataModel
						.getDataVector().get(row);
				Games game = vector.get(0);

				comment.setText(game.getComment());
				String genre = game.getGenres().getGenre();
				String pGenre = game.getGenres().getParentGenres()
						.getParentGenre();
				if (pGenre != null && pGenre.length() != 0) {
					category.setText(pGenre + "-" + genre);
				} else {
					category.setText(genre);
				}
				infos.setText(String.format(
						getSwix().getLocalizer().getString("PUBLISHER"), game
								.getYears().getYear(), game.getPublishers()
								.getPublisher()));
				musician.setText(game.getMusicians().getMusician());
				programmer.setText(game.getProgrammers().getProgrammer());
				String sidFilename = game.getSidFilename();
				linkMusic.setText(sidFilename != null ? sidFilename : "");
				linkMusic.setEnabled(sidFilename != null
						&& sidFilename.length() > 0);
			}
		}
	}

	/**
	 * Display Index to choose a letter.
	 */
	public static final String ALL_LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private EntityManager em;
	private ConfigService configService;
	private GamesService gamesService;

	/**
	 * INI settings.
	 */
	public IConfig config;

	/**
	 * C64 environment.
	 */
	public Player player;

	/**
	 * A GameBase page for each letter.
	 */
	public List<GameBasePage> pages = new ArrayList<GameBasePage>();

	private SwingEngine swix;
	public JCheckBox enableGameBase;
	protected JTextField dbFileField, filterField;
	protected JTabbedPane letter;
	public Picture picture;
	public JPanel screenshot;
	protected JTextField infos, programmer, category, musician;
	protected JTextArea comment;
	protected JButton linkMusic;

	/**
	 * Last downloaded screenshot file.
	 */
	public List<File> lastScreenshot = new ArrayList<File>();

	public Action gotoURL = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			String sidFilename = String.valueOf(linkMusic.getText());
			downloadStart(
					"http://www.gb64.com/C64Music/"
							+ sidFilename.replace('\\', '/'),
					new MusicListener(GameBase.this));
		}
	};

	public Action doEnableGameBase = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (enableGameBase.isSelected()) {
				enableGameBase.setEnabled(false);
				final String outputDir = System.getProperty("jsidplay2.tmpdir");
				File dbFile = new File(outputDir, "GBC_v09.properties");
				if (dbFile.exists()) {
					// There is already a database file downloaded earlier.
					// Therefore we try to connect

					connect(new File(outputDir, "GBC_v09").getAbsolutePath());

					// Check version of GB64
					if (configService.checkVersion()) {
						// Version check is positive
						enableGameBase.setEnabled(true);
						setLettersEnabled(true);
						GameBasePage page = pages.get(0);
						page.setRows(gamesService.select(ALL_LETTERS.charAt(0)));
						letter.setSelectedIndex(0);
					} else {
						System.err
								.println("Version is different or database is broken,"
										+ " re-download");
						disconnect();
						downloadStart(JSIDPlay2.DEPLOYMENT_URL
								+ "online/gamebase/gb64.jar",
								new GameBaseListener(GameBase.this));
					}

				} else {
					// First time, the database is downloaded
					downloadStart(JSIDPlay2.DEPLOYMENT_URL
							+ "online/gamebase/gb64.jar", new GameBaseListener(
							GameBase.this));
				}
			}
		}
	};

	public GameBase(Player player, IConfig config) {
		this.config = config;
		this.player = player;
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("picture", Picture.class);
			swix.insert(GameBase.class.getResource("GameBase.xml"), this);

			setDefaultsAndActions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		for (int i = 0; i < ALL_LETTERS.length(); i++) {
			final GameBasePage page = new GameBasePage(this,
					new ScreenShotListener(GameBase.this), new GameListener(
							this, player, config)) {
				@Override
				void downloadStart(String url, IDownloadListener listener) {
					GameBase.this.downloadStart(url, listener);
				}
			};
			page.gamebasetable.getSelectionModel().addListSelectionListener(
					new GameSelectionListener(page));
			pages.add(page);
			letter.addTab(String.valueOf(ALL_LETTERS.charAt(i)), page);
		}
		letter.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				int i = letter.getSelectedIndex();
				if (i != -1) {
					filterField.setText("");
					GameBasePage page = pages.get(i);
					page.setRows(gamesService.select(ALL_LETTERS.charAt(i)));
				}
			}
		});
		setLettersEnabled(false);
		filterField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				int i = letter.getSelectedIndex();
				if (i != -1) {
					GameBasePage page = pages.get(i);
					if (filterField.getText().trim().length() == 0) {
						page.filter(filterField.getText());
					} else {
						if (validatePattern()) {
							page.filter(filterField.getText());
						}
					}
				}
			}

			private boolean validatePattern() {
				boolean ok = true;
				filterField.setToolTipText(null);
				filterField.setBackground(Color.white);
				try {
					Pattern.compile(filterField.getText());
				} catch (PatternSyntaxException e) {
					filterField.setToolTipText(e.getMessage());
					filterField.setBackground(Color.red);
					ok = false;
				}
				return ok;
			}

		});
	}

	public void downloadStart(String url, IDownloadListener listener) {
		try {
			DownloadThread downloadThread = new DownloadThread(config,
					listener, new URL(url));
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void setLettersEnabled(boolean b) {
		for (int i = 0; i < ALL_LETTERS.length(); i++) {
			letter.setEnabledAt(i, b);
		}
	}

	public void connect(String dbFile) {
		if (!new File(dbFile + ".properties").exists()) {
			System.err.println("Database does not exist: " + dbFile);
			return;
		}
		disconnect();

		em = Persistence.createEntityManagerFactory(
				PersistenceProperties.GAMEBASE_DS,
				new PersistenceProperties(new File(dbFile)))
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
		enableGameBase.setEnabled(true);
		setLettersEnabled(true);
	}

	public SwingEngine getSwix() {
		return swix;
	}

	@Override
	public void notify(UIEvent evt) {
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
	}

	public void clearPicture() {
		picture.setComposerImage(null);
		screenshot.repaint();
	}

	public GamesService getGamesService() {
		return gamesService;
	}

	public JTabbedPane getLetter() {
		return letter;
	}

}
