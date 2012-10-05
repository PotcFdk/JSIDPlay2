package applet.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="MUSICIANS")
public class Musicians {
	@Id
	@Column(name="MU_ID")
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToOne
	@JoinColumn(name="MU_ID")
	private Games games;
	
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	// private Blob? photo;
	
	@Column(name="MUSICIAN")
	private String musician;

	public String getMusician() {
		return musician;
	}

	public void setMusician(String musician) {
		this.musician = musician;
	}

	@Column(name="GRP")
	private String group;

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	@Column(name="NICK")
	private String nickname;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
