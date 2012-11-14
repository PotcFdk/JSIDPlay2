package applet.favorites;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import org.swixml.Localizer;

import applet.collection.Collection;
import applet.collection.CollectionTreeModel;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.HVSCEntry_;
import applet.entities.collection.service.HVSCEntryService;
import applet.entities.config.Configuration;
import applet.entities.config.FavoriteColumn;
import applet.entities.config.FavoritesSection;
import applet.entities.config.service.FavoritesService;

@SuppressWarnings("serial")
public class FavoritesModel extends DefaultTableModel {

	public static final String HVSC_PREFIX = "<HVSC>";

	public static final String CGSC_PREFIX = "<CGSC>";

	protected Configuration config;
	protected Collection hvsc, cgsc;

	private FavoritesSection favorite;

	private Localizer localizer;

	private ArrayList<SingularAttribute<?, ?>> attributes = new ArrayList<SingularAttribute<?, ?>>();

	private HVSCEntryService hvscEntryService;

	private FavoritesService favoritesService;

	public FavoritesModel() {
		super(new Object[] { "Filename" }, 0);
	}

	public void setConfig(Configuration cfg, FavoritesSection favorite,
			Localizer localizer, EntityManager em) {
		this.config = cfg;
		this.favorite = favorite;
		this.localizer = localizer;
		this.hvscEntryService = new HVSCEntryService(em);
		this.favoritesService = new FavoritesService(em);

		this.attributes.add(HVSCEntry_.path);
		FavoritesSection favoritesSection = (favorite);
		for (FavoriteColumn column : favoritesSection.getColumns()) {
			String columnProperty = column.getColumnProperty();
			try {
				Field field = HVSCEntry_.class.getDeclaredField(columnProperty);
				SingularAttribute<?, ?> attribute = (SingularAttribute<?, ?>) field
						.get(null);
				if (!attributes.contains(attribute)) {
					attributes.add(attribute);
					super.addColumn(attribute);
				}
			} catch (Exception e) {
				// ignore missing or miss-configured columns
			}
		}

	}

	public void setCollections(Collection hvsc, Collection cgsc) {
		this.hvsc = hvsc;
		this.cgsc = cgsc;
	}

	@Override
	public String getColumnName(int column) {
		SingularAttribute<?, ?> field = attributes.get(column);
		return localizer.getString(field.getDeclaringType().getJavaType()
				.getSimpleName()
				+ "." + field.getName());
	}

	@Override
	public Class<?> getColumnClass(int column) {
		return attributes.get(column).getJavaType();
	}

	@Override
	public Object getValueAt(int row, int column) {
		SingularAttribute<?, ?> field = attributes.get(column);
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
	public int getRowCount() {
		return size();
	}

	@Override
	public int getColumnCount() {
		return attributes.size();
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

	@Override
	public void addColumn(Object columnName) {
		SingularAttribute<?, ?> field = (SingularAttribute<?, ?>) columnName;
		if (!attributes.contains(field)) {
			attributes.add(field);
			favoritesService.addColumn(favorite, field);
			super.addColumn(columnName);
		}
	}

	public void removeColumn(int column) {
		SingularAttribute<?, ?> field = attributes.get(column);
		attributes.remove(field);
		favoritesService.removeColumn(favorite, field);
		setColumnCount(getColumnCount() - 1);
	}

	public void moveColumn(int fromIndex, int toIndex) {
		if (fromIndex < 1 || toIndex < 1 || fromIndex == toIndex) {
			return;
		}
		favoritesService.moveColumn(favorite, fromIndex - 1, toIndex - 1);
	}

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

	public void move(int from, int to) {
		Collections.swap(favorite.getFavorites(), from, to);
	}

	public void layoutChanged() {
		fireTableStructureChanged();
	}

}
