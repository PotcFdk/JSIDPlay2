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

	public KeyTableEntity(KeyTableEntity keyTableEntity) {
		setKeyCodeName(keyTableEntity.getKeyCodeName());
		setEntry(keyTableEntity.getEntry());
	}

	public KeyTableEntity(String keyCode, KeyTableEntry entry) {
		setKeyCodeName(keyCode);
		setEntry(entry);
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

	private ShadowField<ObjectProperty<KeyTableEntry>, KeyTableEntry> entry = new ShadowField<>(
			SimpleObjectProperty::new, null);

	@Enumerated(EnumType.STRING)
	public KeyTableEntry getEntry() {
		return entry.get();
	}

	public void setEntry(KeyTableEntry entry) {
		this.entry.set(entry);
	}

	public ObjectProperty<KeyTableEntry> entryProperty() {
		return entry.property();
	}

	private ShadowField<StringProperty, String> keyCodeName = new ShadowField<>(SimpleStringProperty::new, null);

	public String getKeyCodeName() {
		return keyCodeName.get();
	}

	public void setKeyCodeName(String keyCodeName) {
		this.keyCodeName.set(keyCodeName);
	}

	public StringProperty keyCodeNameProperty() {
		return keyCodeName.property();
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
