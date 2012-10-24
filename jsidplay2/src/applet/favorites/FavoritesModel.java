package applet.favorites;

import java.io.File;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.collection.Collection;
import applet.collection.CollectionTreeModel;
import applet.sidtuneinfo.SidTuneInfoCache;

@SuppressWarnings("serial")
public class FavoritesModel extends DefaultTableModel {

	public static final String HVSC_PREFIX = "<HVSC>";

	public static final String CGSC_PREFIX = "<CGSC>";

	private SidTuneInfoCache infoCache;

	ArrayList<Integer> propertyIndices = new ArrayList<Integer>(1);

	protected IConfig config;
	protected Collection hvsc, cgsc;

	private IFavoritesSection favorite;

	public FavoritesModel() {
		super(new Object[] { "Filename" }, 0);
	}

	public void setConfig(IConfig cfg, IFavoritesSection favorite) {
		this.config = cfg;
		this.favorite = favorite;
		infoCache = new SidTuneInfoCache(config);
		propertyIndices.add(0);
	}

	public void setCollections(Collection hvsc, Collection cgsc) {
		this.hvsc = hvsc;
		this.cgsc = cgsc;
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
			return favorite.getFavorites().get(row);
		}
		int modelColumn = propertyIndices.get(column);
		if (infoCache.getInfo(getFile(row)) != null) {
			return infoCache.getInfo(getFile(row))[modelColumn - 1];
		} else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
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
	public int getRowCount() {
		return size();
	}

	@Override
	public int getColumnCount() {
		return propertyIndices.size();
	}

	public File getFile(int row) {
		String filename = favorite.getFavorites().get(row);
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

	//
	// von aussen sichtbar!
	//

	public void add(String path) {
		if (!favorite.getFavorites().contains(path)) {
			favorite.getFavorites().add(path);
		}
	}

	public void remove(int row) {
		favorite.getFavorites().remove(row);
	}

	public void clear() {
		favorite.getFavorites().clear();
	}

	public int size() {
		if (favorite == null) {
			return 0;
		}
		return favorite.getFavorites().size();
	}

	public void move(int start, int to) {
		String tmp = favorite.getFavorites().get(start);
		favorite.getFavorites().set(start, favorite.getFavorites().get(to));
		favorite.getFavorites().set(to, tmp);
	}

	public void layoutChanged() {
		fireTableStructureChanged();
	}

}
