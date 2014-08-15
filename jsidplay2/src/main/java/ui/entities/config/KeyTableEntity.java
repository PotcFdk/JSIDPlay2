package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

import libsidplay.components.keyboard.KeyTableEntry;

@Entity
@Access(AccessType.PROPERTY)
public class KeyTableEntity {

	public KeyTableEntity() {
	}

	public KeyTableEntity(String keyCode, KeyTableEntry entry) {
		this(keyCode, entry, false);
	}

	public KeyTableEntity(String keyCode, KeyTableEntry entry, boolean b) {
		this.keyCodeName = keyCode;
		this.entry = entry;
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String keyCodeName;

	private KeyTableEntry entry;

	@Enumerated(EnumType.STRING)
	public KeyTableEntry getEntry() {
		return entry;
	}

	public void setEntry(KeyTableEntry entry) {
		this.entry = entry;
	}

	public String getKeyCodeName() {
		return keyCodeName;
	}

	public void setKeyCodeName(String keyCodeName) {
		this.keyCodeName = keyCodeName;
	}

}
