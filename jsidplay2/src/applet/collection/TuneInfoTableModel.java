package applet.collection;

import java.io.File;

import javax.swing.table.AbstractTableModel;

import libsidutils.zip.ZipFileProxy;

import org.swixml.Localizer;

import sidplay.ini.intf.IConfig;
import applet.entities.collection.HVSCEntry;

public final class TuneInfoTableModel extends AbstractTableModel {

	public static final String[] COLUMNS = new String[] { "PATH", "TITLE",
			"AUTHOR", "RELEASED", "FORMAT", "PLAYER_ID", "NO_OF_SONGS",
			"START_SONG", "CLOCK_FREQ", "SPEED", "SID_MODEL_1", "SID_MODEL_2",
			"COMPATIBILITY", "TUNE_LENGTH", "AUDIO", "SID_CHIP_BASE_1",
			"SID_CHIP_BASE_2", "DRIVER_ADDRESS", "LOAD_ADDRESS", "LOAD_LENGTH",
			"INIT_ADDRESS", "PLAYER_ADDRESS", "FILE_DATE", "FILE_SIZE_KB",
			"TUNE_SIZE_B", "RELOC_START_PAGE", "RELOC_NO_PAGES" };

	private File tuneFile;

	public void setFile(final File file) {
		this.tuneFile = file;
	}

	private IConfig config;

	public void setConfig(IConfig config) {
		this.config = config;
	}

	private Localizer localizer;

	private String author;

	public void setLocalizer(Localizer localizer) {
		this.localizer = localizer;
	}

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		if (tuneFile == null || tuneFile.isDirectory()
				|| tuneFile instanceof ZipFileProxy) {
			return "";
		}
		if (columnIndex == 0) {
			// property name
			return localizer.getString(COLUMNS[rowIndex + 1]);
		}

		// property value
		HVSCEntry entry = HVSCEntry.create(config, tuneFile.getAbsolutePath(),
				tuneFile);
		switch (rowIndex) {
		case 0:
			return entry.getTitle();

		case 1:
			author = entry.getAuthor();
			return author;

		case 2:
			return entry.getReleased();

		case 3:
			return entry.getFormat();

		case 4:
			return entry.getPlayerId();

		case 5:
			return entry.getNoOfSongs();

		case 6:
			return entry.getStartSong();

		case 7:
			return entry.getClockFreq();

		case 8:
			return entry.getSpeed();

		case 9:
			return entry.getSidModel1();

		case 10:
			return entry.getSidModel2();

		case 11:
			return entry.getCompatibility();

		case 12:
			return entry.getTuneLength();

		case 13:
			return entry.getAudio();

		case 14:
			return entry.getSidChipBase1();

		case 15:
			return entry.getSidChipBase2();

		case 16:
			return entry.getDriverAddress();

		case 17:
			return entry.getLoadAddress();

		case 18:
			return entry.getLoadLength();

		case 19:
			return entry.getInitAddress();

		case 20:
			return entry.getPlayerAddress();

		case 21:
			return entry.getFileDate();

		case 22:
			return entry.getFileSizeKb();

		case 23:
			return entry.getTuneSizeB();

		case 24:
			return entry.getRelocStartPage();

		case 25:
			return entry.getRelocNoPages();

		default:
			return "";
		}
	}

	@Override
	public int getRowCount() {
		return 26;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	public String getAuthor() {
		return author;
	}
}