package libsidplay.components.mos6526;


public interface CIAEnvironment {
	void interruptIRQ(boolean state);

	void interruptNMI(boolean state);

	void lightpen(boolean b);
}
