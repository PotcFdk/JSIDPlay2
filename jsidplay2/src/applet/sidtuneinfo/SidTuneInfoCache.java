package applet.sidtuneinfo;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.SidDatabase;

import org.swixml.Localizer;
import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;

public class SidTuneInfoCache {
	public static final String[] SIDTUNE_INFOS = new String[] { "TITLE",
			"AUTHOR", "RELEASED", "FORMAT", "PLAYER_ID", "NO_OF_SONGS",
			"START_SONG", "CLOCK_FREQ", "SPEED", "SID_MODEL_1", "SID_MODEL_2",
			"COMPATIBILITY", "TUNE_LENGTH", "AUDIO", "SID_CHIP_BASE_1",
			"SID_CHIP_BASE_2", "DRIVER_ADDRESS", "LOAD_ADDRESS",
			"INIT_ADDRESS", "PLAYER_ADDRESS", "FILE_DATE", "FILE_SIZE_KB",
			"TUNE_SIZE_B", "RELOC_START_PAGE", "RELOC_NO_PAGES" };

	public static Class<?> SIDTUNE_TYPES[] = new Class[] { String.class,
			String.class, String.class, String.class, String.class,
			Integer.class, Integer.class, String.class, String.class,
			String.class, String.class, String.class, String.class,
			String.class, String.class, String.class, String.class,
			String.class, String.class, String.class, String.class,
			Integer.class, Integer.class, Integer.class, Integer.class };

	public static int INFO_TITLE = 0;

	public static int INFO_AUTHOR = 1;

	public static int INFO_RELEASED = 2;

	public static int INFO_FORMAT = 3;

	public static int INFO_PLAYER_ID = 4;

	public static int INFO_NO_OF_SONGS = 5;

	public static int INFO_START_SONG = 6;

	public static int INFO_CLOCK_FREQ = 7;

	public static int INFO_SPEED = 8;

	public static int INFO_SID = 9;

	public static int INFO_COMPATIBILITY = 10;

	public static int INFO_TUNE_LENGTH = 11;

	public static int INFO_AUDIO = 12;

	public static int INFO_SID_CHIP_BASE_1 = 13;

	public static int INFO_SID_CHIP_BASE_2 = 14;

	public static int INFO_DRIVER_ADDRESS = 15;

	public static int INFO_LOAD_ADDRESS = 16;

	public static int INFO_INIT_ADDRESS = 17;

	public static int INFO_PLAYER_ADDRESS = 18;

	public static int INFO_FILE_DATE = 19;

	public static int INFO_FILE_SIZE = 20;

	public static int INFO_TUNE_SIZE = 21;

	public static int INFO_RELOC_START_PAGE = 22;

	public static int INFO_RELOC_NO_PAGES = 23;

	protected IConfig config;
	private Localizer localizer;

	private static final LinkedHashMap<File, Object[]> pathNamesToTune = new LinkedHashMap<File, Object[]>() {
		private static final int MAX_ENTRIES = 128;

		@Override
		protected boolean removeEldestEntry(final Entry<File, Object[]> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

	public SidTuneInfoCache(IConfig cfg) {
		this.config = cfg;
		SwingEngine swix = new SwingEngine(this);
		swix.setResourceBundle("applet.sidtuneinfo.SidTuneInfoCacheTexts");
		localizer = swix.getLocalizer();
	}

	public Localizer getLocalizer() {
		return localizer;
	}
	
	public Object[] getInfo(final File tuneFile) {
		Object[] infos = pathNamesToTune.get(tuneFile);
		if (infos != null) {
			return infos;
		}
		
		final SidTune tune;
		try {
			tune = SidTune.load(tuneFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (tune == null) {
			return null;
		}

		tune.selectSong(1);
		SidTuneInfo info = tune.getInfo();
		
		infos = new Object[SIDTUNE_INFOS.length];
		infos[0] = info.infoString[0];
		infos[1] = info.infoString[1];
		infos[2] = info.infoString[2];
		infos[3] = tune.getClass().getSimpleName();
		infos[4] = getPlayer(tune);
		infos[5] = info.songs;
		infos[6] = info.startSong;
		infos[7] = info.clockSpeed;
		/* FIXME: song speed is not a global property like the other things here are.
		 * Maybe it should be removed. */
		infos[8] = tune.getSongSpeed(1);
		infos[9] = info.sid1Model;
		infos[10] = info.sid2Model;
		infos[11] = info.compatibility;
		final SidDatabase sldb = SidDatabase.getInstance(config.getSidplay2()
				.getHvsc());
		int fullLength;
		if (sldb != null) {
			fullLength = sldb.getFullSongLength(tune);
		} else {
			fullLength = 0;
		}
		infos[12] = getFullSongLength(fullLength);
		infos[13] = getStereo(info.sidChipBase2 != 0);
		infos[14] = String.format("$%04x", info.sidChipBase1);
		infos[15] = String.format("$%04x", info.sidChipBase2);
		infos[16] = getDriverAddress(info);
		infos[17] = getLoadAddress(info);
		infos[18] = getInitAddress(info);
		infos[19] = getPlayerAddress(info);
		infos[20] = getDate(tuneFile.lastModified());
		infos[21] = getKilos(tuneFile.length());
		infos[22] = tuneFile.length();
		infos[23] = String.format("$%02x", info.relocStartPage);
		infos[24] = String.format("$%02x", info.relocPages);

		pathNamesToTune.put(tuneFile, infos);
		return infos;
	}

	private static String getDriverAddress(final SidTuneInfo tuneInfo) {
		final StringBuffer line = new StringBuffer();
		if (tuneInfo.determinedDriverAddr == 0) {
			line.append("not present");
		} else {
			line.append(String.format("$%04x", tuneInfo.determinedDriverAddr));
			line.append(String.format("-$%04x", tuneInfo.determinedDriverAddr
					+ tuneInfo.determinedDriverLength - 1));
		}
		return line.toString();
	}

	private static String getLoadAddress(final SidTuneInfo tuneInfo) {
		final StringBuffer line = new StringBuffer();
		line.append(String.format("$%04x", tuneInfo.loadAddr));
		line.append(String.format("-$%04x", tuneInfo.loadAddr
				+ tuneInfo.c64dataLen - 1));
		return line.toString();
	}

	private static String getInitAddress(final SidTuneInfo tuneInfo) {
		final StringBuffer line = new StringBuffer();
		if (tuneInfo.playAddr == 0xffff) {
			line.append(String.format("SYS $%04x", tuneInfo.initAddr));
		} else {
			line.append(String.format("$%04x", tuneInfo.initAddr));
		}
		return line.toString();
	}

	private static String getPlayerAddress(final SidTuneInfo tuneInfo) {
		final StringBuffer line = new StringBuffer();
		if (tuneInfo.playAddr != 0xffff) {
			line.append(String.format("$%04x", tuneInfo.playAddr));
		}
		return line.toString();
	}

	private static String getStereo(final boolean stereo) {
		return stereo ? "Stereo" : "Mono";
	}

	private static String getPlayer(SidTune tune) {
		StringBuilder ids = new StringBuilder();
		for (String s : tune.identify()) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(s);
		}
		return ids.toString();
	}

	private static String getFullSongLength(final int songLength) {
		if (songLength < 1) {
			return "Unknown";
		} else {
			return String.format("%02d:%02d", (songLength / 60 % 100),
					(songLength % 60));
		}
	}

	private static Date getDate(final long lastModified) {
		return new Date(lastModified) {
			@Override
			public String toString() {
				return DateFormat.getDateInstance(DateFormat.SHORT)
				.format(this);
			}
		};
	}

	private static Integer getKilos(final long length) {
		return Integer.valueOf((int) (length >> 10));
	}
}
