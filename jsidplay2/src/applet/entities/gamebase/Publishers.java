package applet.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="PUBLISHERS")
public class Publishers {
	@Id
	@Column(name="PU_ID")
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="PU_ID")
	private Games games;
	
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	@Column(name="PUBLISHER")
	private String publisher;

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

}
