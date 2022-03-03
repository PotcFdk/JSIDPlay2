package builder.jhardsid;

enum SysMode {
	UNDEF(0), SIDPLAY(1), VST(2), END(3);

	int sysMode;

	private SysMode(int sysMode) {
		this.sysMode = sysMode;
	}

	public int getSysMode() {
		return sysMode;
	}

}