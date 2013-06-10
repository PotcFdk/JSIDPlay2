package ui.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="PGENRES")
public class PGenres {
	@Id
	@Column(name="PG_ID")
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PG_ID")
	private Games games;
	
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	@Column(name="PARENTGENRE")
	private String parentGenre;

	public String getParentGenre() {
		return parentGenre;
	}

	public void setParentGenre(String parentGenre) {
		this.parentGenre = parentGenre;
	}

}
