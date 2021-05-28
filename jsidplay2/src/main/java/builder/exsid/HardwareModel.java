package builder.exsid;

public enum HardwareModel {
	/** exSID USB */
	XS_MD_STD("ExSID"),
	/** exSID+ USB */
	XS_MD_PLUS("ExSID+");

	private String model;

	private HardwareModel(String model) {
		this.model = model;
	}

	public String getModel() {
		return model;
	}
}
