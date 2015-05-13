package libsidplay.common;

public interface SIDListener {

	void write(long time, int chipNum, int addr, byte data);

}