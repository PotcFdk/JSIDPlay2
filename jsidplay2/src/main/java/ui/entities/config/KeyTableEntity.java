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

import com.fasterxml.jackson.annotation.JsonIgnore;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.components.keyboard.KeyTableEntry;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class KeyTableEntity {

	public KeyTableEntity() {
	}

	/** Copy constructor */
	public KeyTableEntity(KeyTableEntity keyTableEntity) {
		this(keyTableEntity.getKeyCodeName(), keyTableEntity.getEntry());
	}

	private KeyTableEntity(String keyCodeName, KeyTableEntry keyTableEntry) {
		setKeyCodeName(keyCodeName);
		setEntry(keyTableEntry);
	}

	public final static KeyTableEntity of(String keyCode, KeyTableEntry entry) {
		return new KeyTableEntity(keyCode, entry);
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	@JsonIgnore
	public final Integer getId() {
		return id;
	}

	public final void setId(Integer id) {
		this.id = id;
	}

	private ShadowField<ObjectProperty<KeyTableEntry>, KeyTableEntry> entry = new ShadowField<>(
			SimpleObjectProperty::new, null);

	@Enumerated(EnumType.STRING)
	public final KeyTableEntry getEntry() {
		return entry.get();
	}

	public final void setEntry(KeyTableEntry entry) {
		this.entry.set(entry);
	}

	public final ObjectProperty<KeyTableEntry> entryProperty() {
		return entry.property();
	}

	private ShadowField<StringProperty, String> keyCodeName = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getKeyCodeName() {
		return keyCodeName.get();
	}

	public final void setKeyCodeName(String keyCodeName) {
		this.keyCodeName.set(keyCodeName);
	}

	public final StringProperty keyCodeNameProperty() {
		return keyCodeName.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
