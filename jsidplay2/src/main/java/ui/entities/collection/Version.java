package ui.entities.collection;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
public class Version {

	private int id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private int version;

	public final int getVersion() {
		return version;
	}

	public final void setVersion(int version) {
		this.version = version;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
