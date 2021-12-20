package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class ViewEntity {

	public ViewEntity() {
	}

	public ViewEntity(String fxId) {
		setFxId(fxId);
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	@JsonIgnore
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private ShadowField<StringProperty, String> fxId = new ShadowField<>(SimpleStringProperty::new, null);

	public String getFxId() {
		return fxId.get();
	}

	public void setFxId(String fxId) {
		this.fxId.set(fxId);
	}

	public final StringProperty fxIdProperty() {
		return fxId.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
