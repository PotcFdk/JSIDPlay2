package sidplay.ini;

import java.util.ArrayList;
import java.util.List;

import sidplay.ini.intf.IFavoritesSection;
import applet.entities.collection.HVSCEntry;

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

	@Override
	public List<HVSCEntry> getFavorites() {
		return new ArrayList<HVSCEntry>();
	}
}