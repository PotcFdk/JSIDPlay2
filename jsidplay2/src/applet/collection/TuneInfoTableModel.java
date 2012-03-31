package applet.collection;

import java.io.File;

import javax.swing.table.AbstractTableModel;

import libsidutils.zip.ZipFileProxy;

import applet.sidtuneinfo.SidTuneInfoCache;

public final class TuneInfoTableModel extends AbstractTableModel {

	private SidTuneInfoCache infoCache;

	private File tuneFile;

	/**
	 */
	public TuneInfoTableModel() {
	}

	public final void setSidTuneInfoCache(SidTuneInfoCache cache) {
		infoCache = cache;
	}
	
	public void setFile(final File file) {
		this.tuneFile = file;
	}

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		return false;
	}

	public Object getValueAt(final int rowIndex, final int columnIndex) {
		if (tuneFile == null || tuneFile.isDirectory()
				|| tuneFile instanceof ZipFileProxy) {
			return "";
		}

		if (columnIndex == 0) {
			return infoCache.getLocalizer() != null ? infoCache.getLocalizer()
					.getString(SidTuneInfoCache.SIDTUNE_INFOS[rowIndex])
					: "?";
		}
		
		if (infoCache.getInfo(tuneFile) != null) {
			return infoCache.getInfo(tuneFile)[rowIndex];
		} else {
			return "";
		}
	}

	public int getRowCount() {
		return SidTuneInfoCache.SIDTUNE_INFOS.length;
	}

	public int getColumnCount() {
		return 2;
	}
}