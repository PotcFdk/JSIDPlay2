package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Table(name = "PGENRES", indexes = {
		@Index(name = "PGENRES_PARENTGENRE_INDEX", columnList = "PARENTGENRE", unique = false),
		@Index(name = "PGENRES_PG_ID", columnList = "PG_ID", unique = false) })
@Access(AccessType.PROPERTY)
public class PGenres {
	private int id;

	@Id
	@Column(name = "PG_ID")
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private String parentGenre;

	@Column(name = "PARENTGENRE")
	public final String getParentGenre() {
		return parentGenre;
	}

	public final void setParentGenre(String parentGenre) {
		this.parentGenre = parentGenre;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
