package applet.favorites;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import org.swixml.Localizer;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.collection.Collection;
import applet.collection.CollectionTreeModel;
import applet.entities.collection.HVSCEntry;
import applet.entities.collection.service.HVSCEntryService;

@SuppressWarnings("serial")
public class FavoritesModel extends DefaultTableModel {

	public static final String[] COLUMNS = new String[] { "PATH", "TITLE",
			"AUTHOR", "RELEASED", "FORMAT", "PLAYER_ID", "NO_OF_SONGS",
			"START_SONG", "CLOCK_FREQ", "SPEED", "SID_MODEL_1", "SID_MODEL_2",
			"COMPATIBILITY", "TUNE_LENGTH", "AUDIO", "SID_CHIP_BASE_1",
			"SID_CHIP_BASE_2", "DRIVER_ADDRESS", "LOAD_ADDRESS", "LOAD_LENGTH",
			"INIT_ADDRESS", "PLAYER_ADDRESS", "FILE_DATE", "FILE_SIZE_KB",
			"TUNE_SIZE_B", "RELOC_START_PAGE", "RELOC_NO_PAGES" };

	public static Class<?> COLUMN_TYPES[] = new Class[] { String.class,
			String.class, String.class, String.class, String.class,
			String.class, Integer.class, Integer.class, Enum.class, Enum.class,
			Enum.class, Enum.class, Enum.class, Long.class, String.class,
			Integer.class, Integer.class, Integer.class, Integer.class,
			Integer.class, Integer.class, Integer.class, Date.class,
			Integer.class, Integer.class, Short.class, Short.class };

	public static final String HVSC_PREFIX = "<HVSC>";

	public static final String CGSC_PREFIX = "<CGSC>";

	ArrayList<Integer> propertyIndices = new ArrayList<Integer>(1);

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
		propertyIndices.add(0);
		this.hvscEntryService = new HVSCEntryService(em);
	}

	public void setCollections(Collection hvsc, Collection cgsc) {
		this.hvsc = hvsc;
		this.cgsc = cgsc;
	}

	@Override
	public String getColumnName(int column) {
		int modelColumn = propertyIndices.get(column);
		return localizer.getString(COLUMNS[modelColumn]);
	}

	@Override
	public Class<?> getColumnClass(int column) {
		int modelColumn = propertyIndices.get(column);
		return COLUMN_TYPES[modelColumn];
	}

	@Override
	public Object getValueAt(int row, int column) {
		int modelColumn = propertyIndices.get(column);
		HVSCEntry entry = favorite.getFavorites().get(row);
		switch (modelColumn) {
		case 0:
			return entry.getPath();

		case 1:
			return entry.getTitle();

		case 2:
			return entry.getAuthor();

		case 3:
			return entry.getReleased();

		case 4:
			return entry.getFormat();

		case 5:
			return entry.getPlayerId();

		case 6:
			return entry.getNoOfSongs();

		case 7:
			return entry.getStartSong();

		case 8:
			return entry.getClockFreq();

		case 9:
			return entry.getSpeed();

		case 10:
			return entry.getSidModel1();

		case 11:
			return entry.getSidModel2();

		case 12:
			return entry.getCompatibility();

		case 13:
			return entry.getTuneLength();

		case 14:
			return entry.getAudio();

		case 15:
			return entry.getSidChipBase1();

		case 16:
			return entry.getSidChipBase2();

		case 17:
			return entry.getDriverAddress();

		case 18:
			return entry.getLoadAddress();

		case 19:
			return entry.getLoadLength();

		case 20:
			return entry.getInitAddress();

		case 21:
			return entry.getPlayerAddress();

		case 22:
			return entry.getFileDate();

		case 23:
			return entry.getFileSizeKb();

		case 24:
			return entry.getTuneSizeB();

		case 25:
			return entry.getRelocStartPage();

		case 26:
			return entry.getRelocNoPages();

		default:
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
		for (int i = 0; i < COLUMNS.length; i++) {
			if (columnName.equals(COLUMNS[i])) {
				modelIndex = i;
			}
		}
		if (!propertyIndices.contains(modelIndex)) {
			propertyIndices.add(modelIndex);
			super.addColumn(columnName);
		}
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
