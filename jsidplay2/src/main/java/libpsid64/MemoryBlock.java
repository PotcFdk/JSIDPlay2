package libpsid64;

/**
 * Structure to describe a memory block in the C64's memory map.
 */
class MemoryBlock {
	/** start address */
	private int startAddress;
	/** size of the memory block in bytes */
	private int size;
	/** data to be stored */
	private byte[] data;
	private int dataOff;
	/** a short description */
	private String description;

	public int getStartAddress() {
		return startAddress;
	}
	public void setStartAddress(int startAddress) {
		this.startAddress = startAddress;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public int getDataOff() {
		return dataOff;
	}
	public void setDataOff(int dataOff) {
		this.dataOff = dataOff;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}