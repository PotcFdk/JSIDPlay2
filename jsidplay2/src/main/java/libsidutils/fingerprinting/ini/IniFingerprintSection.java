package libsidutils.fingerprinting.ini;

import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_BAND;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_C;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_FFTSIZE;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_MAX_FREQ;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_MIN_FREQ;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_MIN_POWER;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_NPEAKS;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_OVERLAP;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_PEAK_RANGE;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_RANGE_FREQ;
import static libsidutils.fingerprinting.ini.IniFingerPrintDefaults.DEFAULT_RANGE_TIME;

import libsidplay.config.IConfig;
import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

public class IniFingerprintSection extends IniSection implements IFingerprintSection {

	private static final String SECTION_ID = "FingerPrint";

	protected IniFingerprintSection(final IniReader ini) {
		super(ini);
	}

	@Override
	public final int getVersion() {
		return iniReader.getPropertyInt(SECTION_ID, "Version", IConfig.REQUIRED_CONFIG_VERSION);
	}

	@Override
	public void setVersion(int version) {
		iniReader.setProperty(SECTION_ID, "Version", version);
	}

	@Override
	public int getNPeaks() {
		return iniReader.getPropertyInt(SECTION_ID, "NPeaks", DEFAULT_NPEAKS);
	}

	@Override
	public void setNPeaks(int nPeaks) {
		iniReader.setProperty(SECTION_ID, "NPeaks", nPeaks);
	}

	@Override
	public int getFftSize() {
		return iniReader.getPropertyInt(SECTION_ID, "fftSize", DEFAULT_FFTSIZE);
	}

	@Override
	public void setFftSize(int fftSize) {
		iniReader.setProperty(SECTION_ID, "fftSize", fftSize);
	}

	@Override
	public int getOverlap() {
		return iniReader.getPropertyInt(SECTION_ID, "overlap", DEFAULT_OVERLAP);
	}

	@Override
	public void setOverlap(int overlap) {
		iniReader.setProperty(SECTION_ID, "overlap", overlap);
	}

	@Override
	public int getC() {
		return iniReader.getPropertyInt(SECTION_ID, "C", DEFAULT_C);
	}

	@Override
	public void setC(int c) {
		iniReader.setProperty(SECTION_ID, "C", c);
	}

	@Override
	public int getPeakRange() {
		return iniReader.getPropertyInt(SECTION_ID, "peakRange", DEFAULT_PEAK_RANGE);
	}

	@Override
	public void setPeakRange(int peakRange) {
		iniReader.setProperty(SECTION_ID, "peakRange", peakRange);
	}

	@Override
	public float[] getRangeTime() {
		return iniReader.getPropertyFloats(SECTION_ID, "range_time", DEFAULT_RANGE_TIME);
	}

	@Override
	public void setRangeTime(float[] rangeTime) {
		iniReader.setPropertyArray(SECTION_ID, "range_time", rangeTime);
	}

	@Override
	public float[] getRangeFreq() {
		return iniReader.getPropertyFloats(SECTION_ID, "range_freq", DEFAULT_RANGE_FREQ);
	}

	@Override
	public void setRangeFreq(float[] rangeFreq) {
		iniReader.setPropertyArray(SECTION_ID, "range_freq", rangeFreq);
	}

	@Override
	public int[] getBand() {
		return iniReader.getPropertyInts(SECTION_ID, "Band", DEFAULT_BAND);
	}

	@Override
	public void setBand(int[] band) {
		iniReader.setPropertyArray(SECTION_ID, "Band", band);
	}

	@Override
	public int getMinFreq() {
		return iniReader.getPropertyInt(SECTION_ID, "minFreq", DEFAULT_MIN_FREQ);
	}

	@Override
	public void setMinFreq(int minFreq) {
		iniReader.setProperty(SECTION_ID, "minFreq", minFreq);
	}

	@Override
	public int getMaxFreq() {
		return iniReader.getPropertyInt(SECTION_ID, "maxFreq", DEFAULT_MAX_FREQ);
	}

	@Override
	public void setMaxFreq(int maxFreq) {
		iniReader.setProperty(SECTION_ID, "maxFreq", maxFreq);
	}

	@Override
	public int getMinPower() {
		return iniReader.getPropertyInt(SECTION_ID, "minPower", DEFAULT_MIN_POWER);
	}

	@Override
	public void setMinPower(int minPower) {
		iniReader.setProperty(SECTION_ID, "minPower", minPower);
	}

}
