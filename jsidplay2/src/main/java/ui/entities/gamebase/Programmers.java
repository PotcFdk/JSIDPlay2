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
@Table(name = "PROGRAMMERS", indexes = {
		@Index(name = "PROGRAMMERS_PROGRAMMER_INDEX", columnList = "PROGRAMMER", unique = false) })
@Access(AccessType.PROPERTY)
public class Programmers {
	private int id;

	@Id
	@Column(name = "PR_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String programmer;

	@Column(name = "PROGRAMMER")
	public String getProgrammer() {
		return programmer;
	}

	public void setProgrammer(String programmer) {
		this.programmer = programmer;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
