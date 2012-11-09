package applet.collection;

import java.io.File;

import javax.persistence.metamodel.SingularAttribute;
import javax.swing.table.AbstractTableModel;

import libsidutils.zip.ZipFileProxy;

import org.swixml.Localizer;

import sidplay.ini.intf.IConfig;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.HVSCEntry_;

public final class TuneInfoTableModel extends AbstractTableModel {

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
			SingularAttribute<?, ?> field;
			// property name
			switch (rowIndex) {
			case 0:
				field = HVSCEntry_.title;
				break;
			case 1:
				field = HVSCEntry_.author;
				break;
			case 2:
				field = HVSCEntry_.released;
				break;
			case 3:
				field = HVSCEntry_.format;
				break;
			case 4:
				field = HVSCEntry_.playerId;
				break;
			case 5:
				field = HVSCEntry_.noOfSongs;
				break;
			case 6:
				field = HVSCEntry_.startSong;
				break;
			case 7:
				field = HVSCEntry_.clockFreq;
				break;
			case 8:
				field = HVSCEntry_.speed;
				break;
			case 9:
				field = HVSCEntry_.sidModel1;
				break;
			case 10:
				field = HVSCEntry_.sidModel2;
				break;
			case 11:
				field = HVSCEntry_.compatibility;
				break;
			case 12:
				field = HVSCEntry_.tuneLength;
				break;
			case 13:
				field = HVSCEntry_.audio;
				break;
			case 14:
				field = HVSCEntry_.sidChipBase1;
				break;
			case 15:
				field = HVSCEntry_.sidChipBase2;
				break;
			case 16:
				field = HVSCEntry_.driverAddress;
				break;
			case 17:
				field = HVSCEntry_.loadAddress;
				break;
			case 18:
				field = HVSCEntry_.loadLength;
				break;
			case 19:
				field = HVSCEntry_.initAddress;
				break;
			case 20:
				field = HVSCEntry_.playerAddress;
				break;
			case 21:
				field = HVSCEntry_.fileDate;
				break;
			case 22:
				field = HVSCEntry_.fileSizeKb;
				break;
			case 23:
				field = HVSCEntry_.tuneSizeB;
				break;
			case 24:
				field = HVSCEntry_.relocStartPage;
				break;
			case 25:
				field = HVSCEntry_.relocNoPages;
				break;
			default:
				return "";
			}
			return localizer.getString(field.getDeclaringType().getJavaType()
					.getSimpleName()
					+ "." + field.getName());
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