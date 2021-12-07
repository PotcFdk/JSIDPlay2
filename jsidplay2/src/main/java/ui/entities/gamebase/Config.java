package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
public class Config {
	private int majorVersion;

	@Column(name = "MAJORVERSION")
	public final int getMajorVersion() {
		return majorVersion;
	}

	public final void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	private int minorVersion;

	@Column(name = "MINORVERSION")
	public final int getMinorVersion() {
		return minorVersion;
	}

	public final void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	private int officialUpdate;

	@Id
	@Column(name = "OFFICIALUPDATE")
	public final int getOfficialUpdate() {
		return officialUpdate;
	}

	public final void setOfficialUpdate(int officialUpdate) {
		this.officialUpdate = officialUpdate;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
