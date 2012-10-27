package applet.entities.config;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import sidplay.ini.intf.IFilterSection;
import applet.config.annotations.ConfigClass;
import applet.config.annotations.ConfigTransient;

@Entity
@ConfigClass(getBundleKey = "FILTER")
public class DbFilterSection implements IFilterSection {

	@Id
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@ManyToOne
	@XmlIDREF
	@ConfigTransient
	public DbConfig dbConfig;

	@XmlTransient
	public DbConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DbConfig dbConfig) {
		this.dbConfig = dbConfig;
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
