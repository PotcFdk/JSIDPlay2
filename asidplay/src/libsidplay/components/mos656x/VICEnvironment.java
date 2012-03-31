package libsidplay.components.mos656x;

public interface VICEnvironment {
	void interruptIRQ(boolean state);

	void signalAEC(boolean state);
}
