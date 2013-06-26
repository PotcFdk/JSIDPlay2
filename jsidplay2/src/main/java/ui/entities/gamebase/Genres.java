package ui.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="GENRES")
public class Genres {
	@Id
	@Column(name="GE_ID")
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="GE_ID")
	private Games games;
	
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PG_ID")
	private PGenres parentGenres;

	public PGenres getParentGenres() {
		return parentGenres;
	}

	public void setParentGenres(PGenres parentGenres) {
		this.parentGenres = parentGenres;
	}

	@Column(name="GENRE")
	private String genre;

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

}
