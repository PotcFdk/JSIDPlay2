package applet.favorites;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import org.swixml.Localizer;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.collection.Collection;
import applet.collection.CollectionTreeModel;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.HVSCEntry_;
import applet.entities.collection.service.HVSCEntryService;

@SuppressWarnings("serial")
public class FavoritesModel extends DefaultTableModel {

	public static final String HVSC_PREFIX = "<HVSC>";

	public static final String CGSC_PREFIX = "<CGSC>";

	ArrayList<SingularAttribute<?, ?>> propertyIndices = new ArrayList<SingularAttribute<?, ?>>(
			1);

	protected IConfig config;
	protected Collection hvsc, cgsc;

	private IFavoritesSection favorite;

	private Localizer localizer;

	private HVSCEntryService hvscEntryService;

	public FavoritesModel() {
		super(new Object[] { "Filename" }, 0);
	}

	public void setConfig(IConfig cfg, IFavoritesSection favorite,
			Localizer localizer, EntityManager em) {
		this.config = cfg;
		this.favorite = favorite;
		this.localizer = localizer;
		this.propertyIndices.add(HVSCEntry_.path);
		this.hvscEntryService = new HVSCEntryService(em);
	}

	public void setCollections(Collection hvsc, Collection cgsc) {
		this.hvsc = hvsc;
		this.cgsc = cgsc;
	}

	@Override
	public String getColumnName(int column) {
		SingularAttribute<?, ?> field = propertyIndices.get(column);
		return localizer.getString(field.getDeclaringType().getJavaType()
				.getSimpleName()
				+ "." + field.getName());
	}

	@Override
	public Class<?> getColumnClass(int column) {
		return propertyIndices.get(column).getJavaType();
	}

	@Override
	public Object getValueAt(int row, int column) {
		SingularAttribute<?, ?> field = propertyIndices.get(column);
		HVSCEntry entry = favorite.getFavorites().get(row);
		try {
			return ((Field) field.getJavaMember()).get(entry);
		} catch (Exception e) {
			return entry.getPath();
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public void addColumn(Object columnName) {
		SingularAttribute<?, ?> field = (SingularAttribute<?, ?>) columnName;
		if (!propertyIndices.contains(field)) {
			propertyIndices.add(field);
			super.addColumn(columnName);
		}
	}

	public void removeColumn(int column) {
		SingularAttribute<?, ?> field = propertyIndices.get(column);
		propertyIndices.remove(field);
		setColumnCount(getColumnCount() - 1);
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
		HVSCEntry hvscEntry = favorite.getFavorites().get(row);
		String filename = hvscEntry.getPath();
		return getFile(filename);
	}

	private File getFile(String filename) {
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
			try {
				File tuneFile = getFile(path);
				HVSCEntry entry = hvscEntryService.add(config, path, tuneFile);
				favorite.getFavorites().add(entry);
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		HVSCEntry tmp = favorite.getFavorites().get(start);
		favorite.getFavorites().set(start, favorite.getFavorites().get(to));
		favorite.getFavorites().set(to, tmp);
	}

	public void layoutChanged() {
		fireTableStructureChanged();
	}

}
