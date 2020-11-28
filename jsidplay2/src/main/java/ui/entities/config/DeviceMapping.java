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

import libsidplay.common.ChipModel;
import libsidplay.config.IDeviceMapping;

@Entity
@Access(AccessType.PROPERTY)
public class DeviceMapping implements IDeviceMapping {

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

	private boolean used;

	@Override
	public boolean isUsed() {
		return used;
	}

	@Override
	public void setUsed(boolean used) {
		this.used = used;
	}

	private String serialNum;

	@Override
	public String getSerialNum() {
		return serialNum;
	}

	@Override
	public void setSerialNum(String serialNum) {
		this.serialNum = serialNum;
	}

	private ChipModel chipModel;

	@Override
	@Enumerated(EnumType.STRING)
	public ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public void setChipModel(ChipModel chipModel) {
		this.chipModel = chipModel;
	}

	public DeviceMapping() {
		super();
	}

	public DeviceMapping(String serialNum, ChipModel chipModel, boolean used) {
		this.serialNum = serialNum;
		this.chipModel = chipModel;
		this.used = used;
	}

}
