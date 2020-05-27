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

	protected IniFingerprintSection(final IniReader ini) {
		super(ini);
	}

	@Override
	public final int getVersion() {
		return iniReader.getPropertyInt("FingerPrint", "Version", IConfig.REQUIRED_CONFIG_VERSION);
	}

	@Override
	public void setVersion(int version) {
		iniReader.setProperty("FingerPrint", "Version", version);
	}

	@Override
	public int getNPeaks() {
		return iniReader.getPropertyInt("FingerPrint", "NPeaks", DEFAULT_NPEAKS);
	}

	@Override
	public void setNPeaks(int nPeaks) {
		iniReader.setProperty("FingerPrint", "NPeaks", nPeaks);
	}

	@Override
	public int getFftSize() {
		return iniReader.getPropertyInt("FingerPrint", "fftSize", DEFAULT_FFTSIZE);
	}

	@Override
	public void setFftSize(int fftSize) {
		iniReader.setProperty("FingerPrint", "fftSize", fftSize);
	}

	@Override
	public int getOverlap() {
		return iniReader.getPropertyInt("FingerPrint", "overlap", DEFAULT_OVERLAP);
	}

	@Override
	public void setOverlap(int overlap) {
		iniReader.setProperty("FingerPrint", "overlap", overlap);
	}

	@Override
	public int getC() {
		return iniReader.getPropertyInt("FingerPrint", "C", DEFAULT_C);
	}

	@Override
	public void setC(int c) {
		iniReader.setProperty("FingerPrint", "C", c);
	}

	@Override
	public int getPeakRange() {
		return iniReader.getPropertyInt("FingerPrint", "peakRange", DEFAULT_PEAK_RANGE);
	}

	@Override
	public void setPeakRange(int peakRange) {
		iniReader.setProperty("FingerPrint", "peakRange", peakRange);
	}

	@Override
	public float[] getRangeTime() {
		return iniReader.getPropertyFloats("FingerPrint", "range_time", DEFAULT_RANGE_TIME);
	}

	@Override
	public void setRangeTime(float[] rangeTime) {
		iniReader.setPropertyArray("FingerPrint", "range_time", rangeTime);
	}

	@Override
	public float[] getRangeFreq() {
		return iniReader.getPropertyFloats("FingerPrint", "range_freq", DEFAULT_RANGE_FREQ);
	}

	@Override
	public void setRangeFreq(float[] rangeFreq) {
		iniReader.setPropertyArray("FingerPrint", "range_freq", rangeFreq);
	}

	@Override
	public int[] getBand() {
		return iniReader.getPropertyInts("FingerPrint", "Band", DEFAULT_BAND);
	}

	@Override
	public void setBand(int[] band) {
		iniReader.setPropertyArray("FingerPrint", "Band", band);
	}

	@Override
	public int getMinFreq() {
		return iniReader.getPropertyInt("FingerPrint", "minFreq", DEFAULT_MIN_FREQ);
	}

	@Override
	public void setMinFreq(int minFreq) {
		iniReader.setProperty("FingerPrint", "minFreq", minFreq);
	}

	@Override
	public int getMaxFreq() {
		return iniReader.getPropertyInt("FingerPrint", "maxFreq", DEFAULT_MAX_FREQ);
	}

	@Override
	public void setMaxFreq(int maxFreq) {
		iniReader.setProperty("FingerPrint", "maxFreq", maxFreq);
	}

	@Override
	public int getMinPower() {
		return iniReader.getPropertyInt("FingerPrint", "minPower", DEFAULT_MIN_POWER);
	}

	@Override
	public void setMinPower(int minPower) {
		iniReader.setProperty("FingerPrint", "minPower", minPower);
	}

}
