package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Table(name = "MUSICIANS", indexes = { @Index(name = "MUSICIANS_PRG_INDEX", columnList = "GRP", unique = false),
		@Index(name = "MUSICIANS_MUSICIAN_INDEX", columnList = "MUSICIAN", unique = false),
		@Index(name = "MUSICIANS_NICK_INDEX", columnList = "NICK", unique = false) })
@Access(AccessType.PROPERTY)
public class Musicians {
	private int id;

	@Id
	@Column(name = "MU_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private Games games;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MU_ID")
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	private String photoFilename;

	@Column(name = "PHOTO")
	public String getPhotoFilename() {
		return photoFilename;
	}

	public void setPhotoFilename(String photo) {
		this.photoFilename = photo;
	}

	private String musician;

	@Column(name = "MUSICIAN")
	public String getMusician() {
		return musician;
	}

	public void setMusician(String musician) {
		this.musician = musician;
	}

	private String group;

	@Column(name = "GRP")
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	private String nickname;

	@Column(name = "NICK")
	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
