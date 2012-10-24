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

@Entity
public class DbFavoritesSection implements IFavoritesSection {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	@XmlElement(name = "favorites")
	@ElementCollection
	@CollectionTable(name = "favorites")
	private List<String> favorites;

	public void setFavorites(List<String> favorites) {
		this.favorites = favorites;
	}

	@Override
	public List<String> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<String>();
		}
		return favorites;
	}

	@Column
	private String filename;

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@ManyToOne
	@XmlIDREF
	public DbConfig dbConfig;

	@XmlTransient
	public DbConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DbConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

}
