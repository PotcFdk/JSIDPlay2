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

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Table(name = "YEARS")
@Access(AccessType.PROPERTY)
public class Years {
	private int id;

	@Id
	@Column(name = "YE_ID")
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private Games games;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "YE_ID")
	public final Games getGames() {
		return games;
	}

	public final void setGames(Games games) {
		this.games = games;
	}

	private int year;

	@Column(name = "YEAR")
	public final int getYear() {
		return year;
	}

	public final void setYear(int year) {
		this.year = year;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
