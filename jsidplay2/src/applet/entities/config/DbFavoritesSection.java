package applet.entities.config;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import sidplay.ini.intf.IFavoritesSection;

@Entity
public class DbFavoritesSection implements IFavoritesSection {

	@Id
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Column
	private String favoritesFilename;

	@Override
	public String getFilename() {
		return favoritesFilename;
	}

	@Override
	public void setFilename(String favoritesFilename) {
		this.favoritesFilename = favoritesFilename;
	}

	@ManyToOne
	public DbConfig dbConfig;

	public DbConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DbConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

}
