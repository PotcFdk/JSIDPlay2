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
@Table(name="PROGRAMMERS")
@Access(AccessType.PROPERTY)
public class Programmers {
	private int id;
	
	@Id
	@Column(name="PR_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private Games games;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PR_ID")
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	private String programmer;

	@Column(name="PROGRAMMER")
	public String getProgrammer() {
		return programmer;
	}

	public void setProgrammer(String programmer) {
		this.programmer = programmer;
	}
}
