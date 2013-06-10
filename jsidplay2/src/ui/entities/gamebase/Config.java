package ui.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Config {
	@Column(name="MAJORVERSION")
	private int majorVersion;

	public int getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	@Column(name="MINORVERSION")
	private int minorVersion;

	public int getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	@Id
	@Column(name="OFFICIALUPDATE")
	private int officialUpdate;

	public int getOfficialUpdate() {
		return officialUpdate;
	}

	public void setOfficialUpdate(int officialUpdate) {
		this.officialUpdate = officialUpdate;
	}

}
