package libpsid64;

/**
 * Structure to describe a memory block in the C64's memory map.
 */
class MemoryBlock {
	/** start address */
	protected int startAddress;
	/** size of the memory block in bytes */
	protected int size;
	/** data to be stored */
	protected byte[] data;
	protected int dataOff;
	/** a short description */
	protected String description;
}