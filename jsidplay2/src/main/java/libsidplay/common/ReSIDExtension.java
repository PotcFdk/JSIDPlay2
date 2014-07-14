package libsidplay.common;

public interface ReSIDExtension {

	void write(long time, int chipNum, int addr, byte data);

}