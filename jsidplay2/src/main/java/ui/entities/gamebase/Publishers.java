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
@Table(name = "PUBLISHERS", indexes = {
		@Index(name = "PUBLISHERS_PUBLISHER_INDEX", columnList = "PUBLISHER", unique = false) })
@Access(AccessType.PROPERTY)
public class Publishers {
	private int id;

	@Id
	@Column(name = "PU_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private Games games;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PU_ID")
	public Games getGames() {
		return games;
	}

	public void setGames(Games games) {
		this.games = games;
	}

	private String publisher;

	@Column(name = "PUBLISHER")
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
