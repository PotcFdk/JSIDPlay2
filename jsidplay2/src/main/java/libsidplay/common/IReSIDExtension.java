package libsidplay.common;

public interface IReSIDExtension {

	void write(long time, int chipNum, int addr, byte data);

}