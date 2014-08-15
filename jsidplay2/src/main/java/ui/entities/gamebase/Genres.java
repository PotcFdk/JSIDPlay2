package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="GENRES")
@Access(AccessType.PROPERTY)
public class Genres {
	private int id;
	
	@Id
	@Column(name="GE_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private Games games;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="GE_ID")
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	private PGenres parentGenres;

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PG_ID")
	public PGenres getParentGenres() {
		return parentGenres;
	}

	public void setParentGenres(PGenres parentGenres) {
		this.parentGenres = parentGenres;
	}

	private String genre;

	@Column(name="GENRE")
	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

}
