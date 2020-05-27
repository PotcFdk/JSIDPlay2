package libsidplay.components.c1530;

public interface DatasetteEnvironment {
	boolean getTapeSense();

	void setMotor(boolean state);

	void toggleWriteBit(boolean state);
}
