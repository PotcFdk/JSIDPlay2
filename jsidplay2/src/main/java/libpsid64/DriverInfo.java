package libpsid64;

class DriverInfo {
	private byte[] memory;
	private int relocatedDriverPos;
	private int size;

	public byte[] getMemory() {
		return memory;
	}
	public void setMemory(byte[] memory) {
		this.memory = memory;
	}
	public int getRelocatedDriverPos() {
		return relocatedDriverPos;
	}
	public void setRelocatedDriverPos(int relocatedDriverPos) {
		this.relocatedDriverPos = relocatedDriverPos;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
}