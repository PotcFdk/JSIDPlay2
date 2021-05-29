package builder.exsid;

public enum HardwareModel {
	/** exSID USB */
	XS_MD_STD("exSID USB"),
	/** exSID+ USB */
	XS_MD_PLUS("exSID+ USB");

	private String model;

	private HardwareModel(String model) {
		this.model = model;
	}

	public String getModel() {
		return model;
	}
}
