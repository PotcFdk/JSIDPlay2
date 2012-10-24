package sidplay.ini;

import java.util.ArrayList;
import java.util.List;

import sidplay.ini.intf.IFavoritesSection;

/**
 * Favorites section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniFavoritesSection extends IniSection implements
		IFavoritesSection {

	protected IniFavoritesSection(IniReader iniReader) {
		super(iniReader);
	}

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	private String filename;

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void setFilename(String favoritesFilename) {
		this.filename = favoritesFilename;
	}

	@Override
	public List<String> getFavorites() {
		return new ArrayList<String>();
	}
}