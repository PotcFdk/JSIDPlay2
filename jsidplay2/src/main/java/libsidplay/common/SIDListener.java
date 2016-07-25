package libsidplay.common;

public interface SIDListener {

	void write(long time, int addr, byte data);

}