package applet.favorites;

import java.io.File;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import sidplay.ini.IniConfig;
import applet.collection.Collection;
import applet.collection.CollectionTreeModel;
import applet.sidtuneinfo.SidTuneInfoCache;

@SuppressWarnings("serial")
public class FavoritesModel extends DefaultTableModel {

	public static final String HVSC_PREFIX = "<HVSC>";

	public static final String CGSC_PREFIX = "<CGSC>";

	private SidTuneInfoCache infoCache;

	ArrayList<Integer> propertyIndices = new ArrayList<Integer>(1);

	protected IniConfig config;
	protected Collection hvsc, cgsc;

	public FavoritesModel() {
		super(new Object[] { "Filename" }, 0);
	}

	public void setConfig(IniConfig cfg) {
		this.config = cfg;
		infoCache = new SidTuneInfoCache(config);
		propertyIndices.add(0);
	}

	public void setCollections(Collection hvsc, Collection cgsc) {
		this.hvsc = hvsc;
		this.cgsc = cgsc;
	}

	public Collection getHvsc() {
		return hvsc;
	}

	public Collection getCgsc() {
		return cgsc;
	}

	@Override
	public String getColumnName(int column) {
		int modelColumn = propertyIndices.get(column);
		if (modelColumn == 0) {
			return "Path name";
		} else {
			return infoCache.getLocalizer().getString(
					SidTuneInfoCache.SIDTUNE_INFOS[modelColumn - 1]);
		}
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (column == 0) {
			return super.getColumnClass(column);
		}
		int modelColumn = propertyIndices.get(column);
		return SidTuneInfoCache.SIDTUNE_TYPES[modelColumn - 1];
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (column == 0) {
			return super.getValueAt(row, column);
		}
		int modelColumn = propertyIndices.get(column);
		if (infoCache.getInfo(getFile(getValueAt(row, 0))) != null) {
			return infoCache.getInfo(getFile(getValueAt(row, 0)))[modelColumn - 1];
		} else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public boolean contains(Object obj, int column) {
		int modelColumn = propertyIndices.get(column);
		for (int i = 0; i < getRowCount(); i++) {
			Object value = getValueAt(i, modelColumn);
			String pathName = getFile(value).getAbsolutePath();
			if (pathName.equals(obj.toString())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void addColumn(Object columnName) {
		int modelIndex = 1;
		for (int i = 0; i < SidTuneInfoCache.SIDTUNE_INFOS.length; i++) {
			if (columnName.equals(SidTuneInfoCache.SIDTUNE_INFOS[i])) {
				modelIndex = i + 1;
			}
		}
		propertyIndices.add(modelIndex);
		super.addColumn(columnName);
	}

	public void removeColumn(int column) {
		int modelIndex = propertyIndices.get(column);
		// System.err.println("Remove: " + modelIndex);
		for (int i = 0; i < propertyIndices.size(); i++) {
			if (propertyIndices.get(i).equals(modelIndex)) {
				propertyIndices.remove(propertyIndices.get(i));
				setColumnCount(getColumnCount() - 1);
				break;
			}
		}
		// for (int i = 0; i < propertyIndices.size(); i++) {
		// System.err.println("ar: " + propertyIndices.get(i));
		// }
	}

	@Override
	public int getColumnCount() {
		return propertyIndices.size();
	}

	public File getFile(Object value) {
		String filename = value.toString();
		if (filename.startsWith(HVSC_PREFIX)) {
			TreeModel model = hvsc.getFileBrowser().getModel();
			if (model instanceof CollectionTreeModel) {
				ArrayList<File> f = ((CollectionTreeModel) model)
						.getFile(filename.substring(HVSC_PREFIX.length()));
				if (f.size() > 0) {
					return f.get(f.size() - 1);
				}
			}
		} else if (filename.startsWith(CGSC_PREFIX)) {
			TreeModel model = cgsc.getFileBrowser().getModel();
			if (model instanceof CollectionTreeModel) {
				ArrayList<File> f = ((CollectionTreeModel) model)
						.getFile(filename.substring(CGSC_PREFIX.length()));
				if (f.size() > 0) {
					return f.get(f.size() - 1);
				}
			}
		}
		return new File(filename);
	}

}
