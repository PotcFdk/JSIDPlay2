package libsidplay.components.printer;

public interface UserportPrinterEnvironment {
	void printerUserportWriteData(byte data);
	void printerUserportWriteStrobe(boolean strobe);
}
