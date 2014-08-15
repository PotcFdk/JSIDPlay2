package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Access(AccessType.PROPERTY)
public class Config {
	private int majorVersion;

	@Column(name="MAJORVERSION")
	public int getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	private int minorVersion;

	@Column(name="MINORVERSION")
	public int getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	private int officialUpdate;

	@Id
	@Column(name="OFFICIALUPDATE")
	public int getOfficialUpdate() {
		return officialUpdate;
	}

	public void setOfficialUpdate(int officialUpdate) {
		this.officialUpdate = officialUpdate;
	}

}
