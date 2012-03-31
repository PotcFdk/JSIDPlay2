package applet.collection.search;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import libsidutils.STIL;
import applet.collection.CollectionTreeModel;
import applet.sidtuneinfo.SidTuneInfoCache;

public class SearchInIndexThread extends SearchThread {

	private final Connection fConnection;
	private final CollectionTreeModel fModel;
	private ResultSet rs;

	private int field;
	private String fieldValue;
	private boolean caseSensitive;

	public SearchInIndexThread(CollectionTreeModel model, Connection conn, boolean forward) {
		super(forward);
		fConnection = conn;
		fModel = model;
	}

	private void select() throws SQLException {
		if (rs != null) {
			rs.close();
		}

		String fv = fieldValue;
		if (!caseSensitive) {
			fv = fv.toLowerCase();
		}
		String[] pieces = fv.split("\\s+");

		StringBuilder query = new StringBuilder("SELECT \"FULL_PATH\" FROM ");
		String fieldName = "";
		int firstSTILIdx = 2 + SidTuneInfoCache.SIDTUNE_INFOS.length;
		if (field >= firstSTILIdx) {
			// STIL infos
			fieldName = STIL.STIL_INFOS[field - firstSTILIdx];
			query.append("stil");
		} else if (field >= 2) {
			// tune infos
			fieldName = SidTuneInfoCache.SIDTUNE_INFOS[field - 2];
			query.append("collection");
		} else if (field == 0) {
			// Full filename
			fieldName = "FILE_NAME";
			query.append("collection");
		} else if (field == 1) {
			// Full path
			fieldName = "FULL_PATH";
			query.append("collection");
		}
		query.append(" WHERE 1=1");

		/* split by spaces, find entries where all match */
		for (@SuppressWarnings("unused") String piece : pieces) {
			if (!caseSensitive) {
				query.append(" AND lower(\"" + fieldName + "\")");
			} else {
				query.append(" AND \"" + fieldName + "\"");
			}
			query.append(" LIKE ?");
		}
		query.append(" ORDER BY \"FULL_PATH\" ASC");
		PreparedStatement stmt = fConnection.prepareStatement(query.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		for (int i = 0; i < pieces.length; i++) {
			stmt.setString(i + 1, "%" + pieces[i] + "%");
		}

		rs = stmt.executeQuery();
	}

	@Override
	public void run() {
		for (ISearchListener listener : fListeners) {
			listener.searchStart();
		}
		try {
			if (rs == null) {
				select();
			}
			// search the collection
			while (!fAborted && (fForward ? next() : prev())) {
				String filePath = rs.getString(1);
				for (ISearchListener listener : fListeners) {
					ArrayList<File> file = fModel.getFile(filePath);
					if (file.size() > 0) {
						listener.searchHit(file.get(file.size() - 1));
					}
				}
			}
			if (!fAborted) {
				rs.close();
				rs = null;
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, e);
		}
		for (ISearchListener listener : fListeners) {
			listener.searchStop(fAborted);
		}
	}

	private boolean prev() throws SQLException {
		if (rs.isFirst()) {
			return false;// rs.last();
		} else {
			return rs.previous();
		}
	}

	private boolean next() throws SQLException {
		if (rs.isLast()) {
			return false;// rs.first();
		} else {
			return rs.next();
		}
	}

	@Override
	public Object getSearchState() {
		return rs;
	}

	@Override
	public void restoreSearchState(Object state) {
		if (state instanceof ResultSet) {
			rs = (ResultSet) state;
		}
	}

	public int getField() {
		return field;
	}

	public void setField(int index) {
		this.field = index;
	}

	public String getFieldValue() {
		return fieldValue;
	}

	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
}
