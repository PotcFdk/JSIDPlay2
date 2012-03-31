package applet.gamebase;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

import sidplay.ini.IniConfig;
import applet.TuneTab;
import applet.collection.Picture;
import applet.events.IMadeProgress;
import applet.events.UIEvent;
import applet.gamebase.listeners.GameBaseListener;
import applet.gamebase.listeners.GameListener;
import applet.gamebase.listeners.MusicListener;
import applet.gamebase.listeners.ScreenShotListener;
import applet.soundsettings.DownloadThread;
import applet.soundsettings.IDownloadListener;

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
				Vector<String> vector = (Vector<String>) page.dataModel
						.getDataVector().get(row);
				try {
					String cmt = String.valueOf(vector.get(4));
					comment.setText(cmt);

					int catIdx = Integer.valueOf(vector.get(5));
					category.setText(getCategory(catIdx));

					int yearIdx = Integer.valueOf(vector.get(6));
					int publisherIdx = Integer.valueOf(vector.get(7));
					infos.setText(String.format(getSwix().getLocalizer()
							.getString("PUBLISHER"), getYear(yearIdx),
							getPublisher(publisherIdx)));

					int musIdx = Integer.valueOf(vector.get(8));
					musician.setText(getMusician(musIdx));

					int progIdx = Integer.valueOf(vector.get(9));
					programmer.setText(getProgrammer(progIdx));

					String musId = String.valueOf(vector.get(10));
					try {
						linkMusic.setText(musId != null ? musId : "");
						linkMusic.setEnabled(musId != null
								&& musId.length() > 0);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} catch (NumberFormatException e1) {
					e1.printStackTrace();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}

			}
		}
	}

	/**
	 * Display Index to choose a letter.
	 */
	public static final String ALL_LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * HSQLDB driver class name.
	 */
	private String DRIVER_CLASS = "org.hsqldb.jdbcDriver";

	/**
	 * GameBase64 database connection handle.
	 */
	private Connection connection;

	/**
	 * Query statement.
	 */
	protected PreparedStatement stmt, stmtNum, pubStmt, yearStmt, genreStmt,
			pgenreStmt, musStmt, progStmt;

	/**
	 * INI settings.
	 */
	public IniConfig config;

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
	protected JTextField dbFile, filterField;
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
				if (new File(outputDir, "gb64.idx.data").exists()) {
					// There is already a database file downloaded earlier.
					// Therefore we try to connect
					PreparedStatement verStmt = null;
					try {
						connect(new File(outputDir, "gb64.idx")
								.getAbsolutePath());
						// Check version of GB64
						verStmt = getConnection().prepareStatement(
								"SELECT number FROM version");
						ResultSet rs = verStmt.executeQuery();
						if (!rs.next()
								|| !EXPECTED_VERSION.equals(rs.getString(1))) {
							throw new SQLException();
						}
						// Version check is positive
						enableGameBase.setEnabled(true);
						setLettersEnabled(true);
						GameBasePage page = pages.get(0);
						page.setRows(select(String.valueOf(ALL_LETTERS
								.charAt(0))));
					} catch (Exception e1) {
						System.err
								.println("Version is different or database is broken,"
										+ " re-download");
						// Version is different or database is broken,
						// re-download
						try {
							if (getConnection() != null
									&& !getConnection().isClosed()) {
								getConnection().close();
							}
						} catch (SQLException e2) {
							e2.printStackTrace();
						}
						downloadStart(
								"http://jsidplay2.sourceforge.net/online/gamebase/gb64.jar",
								new GameBaseListener(GameBase.this));
					} finally {
						if (verStmt != null) {
							try {
								verStmt.close();
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					}
				} else {
					// First time, the database is downloaded
					downloadStart(
							"http://jsidplay2.sourceforge.net/online/gamebase/gb64.jar",
							new GameBaseListener(GameBase.this));
				}
			}
		}
	};

	public GameBase(Player player, IniConfig config) {
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
			try {
				final GameBasePage page = new GameBasePage(this,
						new ScreenShotListener(GameBase.this),
						new GameListener(this, player, config)) {
					@Override
					void downloadStart(String url, IDownloadListener listener) {
						GameBase.this.downloadStart(url, listener);
					}
				};
				page.gamebasetable.getSelectionModel()
						.addListSelectionListener(
								new GameSelectionListener(page));
				pages.add(page);
				letter.addTab(String.valueOf(ALL_LETTERS.charAt(i)), page);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		letter.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				int i = letter.getSelectedIndex();
				if (i != -1) {
					filterField.setText("");
					GameBasePage page = pages.get(i);
					page.setRows(select(String.valueOf(ALL_LETTERS.substring(i,
							i + 1))));
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
		DownloadThread downloadThread = new DownloadThread(config, listener,
				url);
		downloadThread.start();
		getUiEvents().fireEvent(IMadeProgress.class,
				new IMadeProgress() {

					@Override
					public int getPercentage() {
						return 0;
					}
				});
	}

	public void setLettersEnabled(boolean b) {
		for (int i = 0; i < ALL_LETTERS.length(); i++) {
			letter.setEnabledAt(i, b);
		}
	}

	public void connect(String dbFile) throws SQLException {
		try {
			final String dbFileNoExt;
			if (dbFile.lastIndexOf('.') != -1) {
				dbFileNoExt = dbFile.substring(0, dbFile.lastIndexOf('.'));
			} else {
				dbFileNoExt = dbFile;
			}
			if (!new File(dbFileNoExt + ".idx.data").exists()) {
				System.err.println("Database does not exist: " + dbFileNoExt
						+ ".idx");
				return;
			}
			if (connection != null) {
				connection.close();
			}
			Class.forName(DRIVER_CLASS).newInstance();
			connection = DriverManager.getConnection("jdbc:hsqldb:file:"
					+ dbFileNoExt + ".idx;shutdown=true");
			stmt = connection.prepareStatement(
					"SELECT * FROM GAMES WHERE NAME LIKE ? ORDER BY NAME ASC",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			stmtNum = connection
					.prepareStatement(
							"SELECT * FROM GAMES WHERE NAME BETWEEN ? AND ? ORDER BY NAME ASC",
							ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);
			pubStmt = connection.prepareStatement(
					"SELECT PUBLISHER FROM PUBLISHERS WHERE PU_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			yearStmt = connection.prepareStatement(
					"SELECT YEAR FROM YEARS WHERE YE_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			genreStmt = connection.prepareStatement(
					"SELECT PG_ID, GENRE FROM GENRES WHERE GE_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			pgenreStmt = connection.prepareStatement(
					"SELECT PARENTGENRE FROM PGENRES WHERE PG_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			musStmt = connection.prepareStatement(
					"SELECT MUSICIAN, GRP FROM MUSICIANS WHERE MU_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			progStmt = connection.prepareStatement(
					"SELECT PROGRAMMER FROM PROGRAMMERS WHERE PR_ID=?",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
		} catch (InstantiationException e) {
			throw new SQLException(e);
		} catch (IllegalAccessException e) {
			throw new SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new SQLException(e);
		} catch (SQLException e) {
			throw new SQLException(e);
		}
	}

	public ResultSet select(String character) {
		try {
			if (character.charAt(0) == ALL_LETTERS.charAt(0)) {
				stmtNum.setString(1, "0%");
				stmtNum.setString(2, "9%");
				return stmtNum.executeQuery();
			} else {
				stmt.setString(1, character + "%");
				return stmt.executeQuery();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SwingEngine getSwix() {
		return swix;
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public void notify(UIEvent evt) {
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
	}

	public String getPublisher(int id) throws SQLException {
		String result;
		pubStmt.setInt(1, id);
		ResultSet rs = pubStmt.executeQuery();
		if (rs.next()) {
			result = rs.getString("PUBLISHER");
		} else {
			result = "";
		}
		rs.close();
		return result;
	}

	public String getYear(int id) throws SQLException {
		String result;
		yearStmt.setInt(1, id);
		ResultSet rs = yearStmt.executeQuery();
		if (rs.next()) {
			result = String.valueOf(rs.getInt("YEAR"));
		} else {
			result = "????";
		}
		rs.close();
		return result;
	}

	public String getCategory(int id) throws SQLException {
		String result;
		genreStmt.setInt(1, id);
		ResultSet rs = genreStmt.executeQuery();
		if (rs.next()) {
			result = rs.getString("GENRE");
			int pgId = rs.getInt("PG_ID");
			rs.close();
			pgenreStmt.setInt(1, pgId);
			rs = pgenreStmt.executeQuery();
			if (rs.next()) {
				String pGenre = rs.getString("PARENTGENRE");
				result = pGenre + (pGenre.length() > 0 ? " - " : "") + result;
				rs.close();
			} else {
				result = "????" + result;
			}
		} else {
			result = "????";
		}
		rs.close();
		return result;
	}

	public String getMusician(int id) throws SQLException {
		String result;
		musStmt.setInt(1, id);
		ResultSet rs = musStmt.executeQuery();
		if (rs.next()) {
			result = rs.getString("MUSICIAN");
			String group = rs.getString("GRP");
			if (group.length() > 0) {
				result += " (" + group + ")";
			}
			rs.close();
		} else {
			result = "????";
		}
		return result;
	}

	public String getProgrammer(int id) throws SQLException {
		String result;
		progStmt.setInt(1, id);
		ResultSet rs = progStmt.executeQuery();
		if (rs.next()) {
			result = rs.getString("PROGRAMMER");
			rs.close();
		} else {
			result = "????";
		}
		return result;
	}

	public void clearPicture() {
		picture.setComposerImage(null);
		screenshot.repaint();
	}

	public IniConfig getConfig() {
		return config;
	}

}
