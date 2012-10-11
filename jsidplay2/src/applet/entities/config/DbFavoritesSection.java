package applet.entities.config;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import sidplay.ini.intf.IFavoritesSection;

@Embeddable
public class DbFavoritesSection implements IFavoritesSection {

	@Column(length = 2048)
	private String favoritesTitles;

	@Override
	public String getFavoritesTitles() {
		return favoritesTitles;
	}

	@Override
	public void setFavoritesTitles(String favoritesTitles) {
		this.favoritesTitles = favoritesTitles;
	}

	@Column(length = 2048)
	private String favoritesFilenames;

	@Override
	public String getFavoritesFilenames() {
		return favoritesFilenames;
	}

	@Override
	public void setFavoritesFilenames(String favoritesFilenames) {
		this.favoritesFilenames = favoritesFilenames;
	}

	private String favoritesCurrent;

	@Override
	public String getFavoritesCurrent() {
		return favoritesCurrent;
	}

	@Override
	public void setFavoritesCurrent(String favoritesCurrent) {
		this.favoritesCurrent = favoritesCurrent;
	}

}
