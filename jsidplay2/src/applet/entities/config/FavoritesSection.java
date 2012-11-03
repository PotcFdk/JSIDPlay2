package applet.entities.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import sidplay.ini.intf.IFavoritesSection;
import applet.config.annotations.ConfigClass;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigMethod;
import applet.config.annotations.ConfigTransient;

@Entity
@ConfigClass(bundleKey = "FAVORITE")
public class FavoritesSection implements IFavoritesSection {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@ConfigTransient
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ConfigDescription(descriptionKey = "FAVORITES_NAME_DESC", toolTipKey = "FAVORITES_NAME_TOOLTIP")
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "favorite")
	@ElementCollection
	@CollectionTable(name = "favorites")
	private List<String> favorites;

	public void setFavorites(List<String> favorites) {
		this.favorites = favorites;
	}

	@Override
	@XmlTransient
	@ConfigMethod(nameKey = "FILENAME")
	public List<String> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<String>();
		}
		return favorites;
	}

	@Column
	@ConfigTransient
	private String filename;

	/**
	 * Favorites are persisted, please use getFavorites() instead.
	 */
	@Deprecated
	@Override
	public String getFilename() {
		return filename;
	}

	@ManyToOne
	@XmlIDREF
	@ConfigTransient
	public Configuration configuration;

	@XmlTransient
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

}
