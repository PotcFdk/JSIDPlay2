package applet.collection;

import java.io.File;
import java.lang.reflect.Field;

import javax.persistence.metamodel.SingularAttribute;
import javax.swing.table.AbstractTableModel;

import org.swixml.Localizer;

import applet.entities.collection.HVSCEntry;
import applet.entities.collection.HVSCEntry_;
import applet.entities.config.Configuration;

public final class TuneInfoTableModel extends AbstractTableModel {

	private HVSCEntry entry;

	public void setFile(final File tuneFile) {
		this.entry = HVSCEntry.create(config, tuneFile.getAbsolutePath(),
				tuneFile);
		this.author = entry.getAuthor();
	}

	private Configuration config;

	public void setConfig(Configuration config) {
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
		if (this.entry == null) {
			return "";
		}
		SingularAttribute<?, ?> field;
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
		case 25:
			field = HVSCEntry_.relocNoPages;
			break;
		default:
			return "";
		}

		if (columnIndex == 0) {
			// property name
			return localizer.getString(field.getDeclaringType().getJavaType()
					.getSimpleName()
					+ "." + field.getName());
		}
		// property value
		if (field == null) {
			return "";
		}
		try {
			return ((Field) field.getJavaMember()).get(entry);
		} catch (Exception e) {
			return entry.getPath();
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