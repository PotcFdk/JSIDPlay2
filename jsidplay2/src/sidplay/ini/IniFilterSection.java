package sidplay.ini;


public class IniFilterSection {
	private final IniReader ini;
	private final String heading;
	
	public IniFilterSection(IniReader ini, String heading) {
		this.ini = ini;
		this.heading = heading;
	}

	public float getFilter8580CurvePosition() {
		return ini.getPropertyFloat(heading, "Filter8580CurvePosition", 0);
	}

	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		ini.setProperty(heading, "Filter8580CurvePosition", filter8580CurvePosition);
	}

	public float getFilter6581CurvePosition() {
		return ini.getPropertyFloat(heading, "Filter6581CurvePosition", 0);
	}

	public void setFilter6581CurvePosition(double filter6581CurvePosition) {
		ini.setProperty(heading, "Filter6581CurvePosition", filter6581CurvePosition);
	}
}