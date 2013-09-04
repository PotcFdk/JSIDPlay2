package ui.entities.config;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import sidplay.ini.intf.IFilterSection;

@Entity
@XmlAccessorType(XmlAccessType.FIELD)
public class FilterSection implements IFilterSection {

	@Id
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	private float filter8580CurvePosition;

	@Override
	public float getFilter8580CurvePosition() {
		return filter8580CurvePosition;
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		this.filter8580CurvePosition = filter8580CurvePosition;
	}

	private float filter6581CurvePosition;

	@Override
	public float getFilter6581CurvePosition() {
		return filter6581CurvePosition;
	}

	@Override
	public void setFilter6581CurvePosition(float filter6581CurvePosition) {
		this.filter6581CurvePosition = filter6581CurvePosition;
	}

}
