package sidplay.ini;

import sidplay.ini.intf.IFilterSection;

public class IniFilterSection implements IFilterSection {
	private final IniReader ini;

	public IniFilterSection(IniReader ini, String heading) {
		this.ini = ini;
		this.name = heading;
	}

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public float getFilter8580CurvePosition() {
		return ini.getPropertyFloat(name, "Filter8580CurvePosition", 0);
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		ini.setProperty(name, "Filter8580CurvePosition",
				filter8580CurvePosition);
	}

	@Override
	public float getFilter6581CurvePosition() {
		return ini.getPropertyFloat(name, "Filter6581CurvePosition", 0);
	}

	@Override
	public void setFilter6581CurvePosition(float filter6581CurvePosition) {
		ini.setProperty(name, "Filter6581CurvePosition",
				filter6581CurvePosition);
	}
}