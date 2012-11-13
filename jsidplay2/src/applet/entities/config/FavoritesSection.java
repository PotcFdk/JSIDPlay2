package applet.entities.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import sidplay.ini.intf.IFavoritesSection;
import applet.config.annotations.ConfigClass;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigMethod;
import applet.config.annotations.ConfigTransient;
import applet.entities.collection.HVSCEntry;

@Entity
@ConfigClass(bundleKey = "FAVORITE")
public class FavoritesSection implements IFavoritesSection {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlJavaTypeAdapter(IntegerAdapter.class)
	@ConfigTransient
	private Integer id;

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	@OneToMany
	private List<FavoriteColumn> columns;

	@ConfigMethod(nameKey = "FAVORITES_COLUMNS")
	public List<FavoriteColumn> getColumns() {
		if (columns == null) {
			columns = new ArrayList<FavoriteColumn>();
		}
		return columns;
	}

	public void setColumns(List<FavoriteColumn> columns) {
		this.columns = columns;
	}

	@XmlElement(name = "favorite")
	@OneToMany
	private List<HVSCEntry> favorites;

	public void setFavorites(List<HVSCEntry> favorites) {
		this.favorites = favorites;
	}

	@Override
	@XmlTransient
	@ConfigMethod(nameKey = "HVSC_ENTRIES")
	public List<HVSCEntry> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<HVSCEntry>();
		}
		return favorites;
	}

	@ManyToOne(cascade = CascadeType.ALL)
	@XmlIDREF
	@ConfigTransient
	private Configuration configuration;

	@XmlTransient
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

}
