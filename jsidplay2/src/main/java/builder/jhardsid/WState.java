package builder.jhardsid;

enum WState {
	OK(1), BUSY(2), ERROR(3), END(4);

	private int wState;

	private WState(int wState) {
		this.wState = wState;
	}

	public int getWState() {
		return wState;
	}
}