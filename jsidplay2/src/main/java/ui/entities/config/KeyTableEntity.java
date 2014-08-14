package ui.entities.config;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

import libsidplay.components.keyboard.KeyTableEntry;

@Entity
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

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String keyCodeName;

	@Enumerated(EnumType.STRING)
	private KeyTableEntry entry;

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
