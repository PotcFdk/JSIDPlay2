package libsidutils.fingerprinting.ini;

public interface IniFingerPrintDefaults {
	IniFingerprintConfig DEFAULTS = IniFingerprintConfig.getDefault();

	// Fingerprint Section

	IFingerprintSection FINGERPRINT_SECTION = DEFAULTS.getFingerPrintSection();
	int DEFAULT_NPEAKS = FINGERPRINT_SECTION.getNPeaks();
	int DEFAULT_FFTSIZE = FINGERPRINT_SECTION.getFftSize();
	int DEFAULT_OVERLAP = FINGERPRINT_SECTION.getOverlap();
	int DEFAULT_C = FINGERPRINT_SECTION.getC();
	int DEFAULT_PEAK_RANGE = FINGERPRINT_SECTION.getPeakRange();
	float[] DEFAULT_RANGE_TIME = FINGERPRINT_SECTION.getRangeTime();
	float[] DEFAULT_RANGE_FREQ = FINGERPRINT_SECTION.getRangeFreq();
	int[] DEFAULT_BAND = FINGERPRINT_SECTION.getBand();
	int DEFAULT_MIN_FREQ = FINGERPRINT_SECTION.getMinFreq();
	int DEFAULT_MAX_FREQ = FINGERPRINT_SECTION.getMaxFreq();
	int DEFAULT_MIN_POWER = FINGERPRINT_SECTION.getMinPower();
}
