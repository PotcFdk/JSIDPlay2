package sidplay.fingerprinting.ini;

public interface IFingerprintConfig {

	/** Bump this each time you want to invalidate the configuration */
	int REQUIRED_CONFIG_VERSION = 1;

	IFingerprintSection getFingerPrintSection();

}
