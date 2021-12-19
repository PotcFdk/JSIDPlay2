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
@Table(name = "GENRES", indexes = { @Index(name = "GENRES_GENRE_INDEX", columnList = "GENRE", unique = false),
		@Index(name = "GENRES_PG_ID", columnList = "PG_ID", unique = false) })
@Access(AccessType.PROPERTY)
public class Genres {
	private int id;

	@Id
	@Column(name = "GE_ID")
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private PGenres parentGenres;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PG_ID")
	public final PGenres getParentGenres() {
		return parentGenres;
	}

	public void setParentGenres(PGenres parentGenres) {
		this.parentGenres = parentGenres;
	}

	private String genre;

	@Column(name = "GENRE")
	public final String getGenre() {
		return genre;
	}

	public final void setGenre(String genre) {
		this.genre = genre;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
