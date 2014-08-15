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
@Table(name="PGENRES")
@Access(AccessType.PROPERTY)
public class PGenres {
	private int id;
	
	@Id
	@Column(name="PG_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private Games games;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PG_ID")
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	private String parentGenre;

	@Column(name="PARENTGENRE")
	public String getParentGenre() {
		return parentGenre;
	}

	public void setParentGenre(String parentGenre) {
		this.parentGenre = parentGenre;
	}

}
