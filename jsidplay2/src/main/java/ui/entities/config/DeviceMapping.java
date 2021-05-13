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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.common.ChipModel;
import libsidplay.config.IDeviceMapping;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class DeviceMapping implements IDeviceMapping {

	public DeviceMapping() {
		super();
	}

	public DeviceMapping(DeviceMapping deviceMapping) {
		this(deviceMapping.getSerialNum(), deviceMapping.getChipModel(), deviceMapping.isUsed());
	}

	public DeviceMapping(IDeviceMapping deviceMapping) {
		this(deviceMapping.getSerialNum(), deviceMapping.getChipModel(), true);
	}

	public DeviceMapping(String serialNum, ChipModel chipModel, boolean used) {
		setSerialNum(serialNum);
		setChipModel(chipModel);
		setUsed(used);
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

	private ShadowField<BooleanProperty, Boolean> used = new ShadowField<>(SimpleBooleanProperty::new, false);

	@Override
	public boolean isUsed() {
		return used.get();
	}

	@Override
	public void setUsed(boolean used) {
		this.used.set(used);
	}

	public BooleanProperty usedProperty() {
		return used.property();
	}

	private ShadowField<StringProperty, String> serialNum = new ShadowField<>(SimpleStringProperty::new, null);

	@Override
	public String getSerialNum() {
		return serialNum.get();
	}

	@Override
	public void setSerialNum(String serialNum) {
		this.serialNum.set(serialNum);
	}

	public StringProperty serialNumberProperty() {
		return serialNum.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> chipModel = new ShadowField<>(SimpleObjectProperty::new,
			null);

	@Override
	@Enumerated(EnumType.STRING)
	public ChipModel getChipModel() {
		return chipModel.get();
	}

	@Override
	public void setChipModel(ChipModel chipModel) {
		this.chipModel.set(chipModel);
	}

	public ObjectProperty<ChipModel> chipModelProperty() {
		return chipModel.property();
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
