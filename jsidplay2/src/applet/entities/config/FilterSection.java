package applet.entities.config;

import javax.persistence.Entity;
import javax.persistence.Id;

import sidplay.ini.intf.IFilterSection;
import applet.config.annotations.ConfigTypeName;
import applet.config.annotations.ConfigDescription;

@Entity
@ConfigTypeName(bundleKey = "FILTER")
public class FilterSection implements IFilterSection {

	@Id
	@ConfigDescription(bundleKey = "FILTER_NAME_DESC", toolTipBundleKey = "FILTER_NAME_TOOLTIP")
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@ConfigDescription(bundleKey = "FILTER_FILTER8580_CURVE_POSITION_DESC", toolTipBundleKey = "FILTER_FILTER8580_CURVE_POSITION_TOOLTIP")
	private float filter8580CurvePosition;

	@Override
	public float getFilter8580CurvePosition() {
		return filter8580CurvePosition;
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		this.filter8580CurvePosition = filter8580CurvePosition;
	}

	@ConfigDescription(bundleKey = "FILTER_FILTER6581_CURVE_POSITION_DESC", toolTipBundleKey = "FILTER_FILTER6581_CURVE_POSITION_TOOLTIP")
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
