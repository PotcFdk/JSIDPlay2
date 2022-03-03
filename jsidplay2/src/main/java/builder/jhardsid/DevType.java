package builder.jhardsid;

enum DevType {
	UNKNOWN(0), HS4U(1), HSUP(2), HSUNO(3), END(4);
	private int devType;

	private DevType(int devType) {
		this.devType = devType;
	}

	public int getDevType() {
		return devType;
	}
}