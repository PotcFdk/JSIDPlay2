package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_BLEED;
import static sidplay.ini.IniDefaults.DEFAULT_BLUR;
import static sidplay.ini.IniDefaults.DEFAULT_BRIGHTNESS;
import static sidplay.ini.IniDefaults.DEFAULT_CONTRAST;
import static sidplay.ini.IniDefaults.DEFAULT_ENABLE_DATABASE;
import static sidplay.ini.IniDefaults.DEFAULT_FADE_IN_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_FADE_OUT_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_GAMMA;
import static sidplay.ini.IniDefaults.DEFAULT_HVSC_DIR;
import static sidplay.ini.IniDefaults.DEFAULT_LAST_DIR;
import static sidplay.ini.IniDefaults.DEFAULT_LOOP;
import static sidplay.ini.IniDefaults.DEFAULT_OFFSET;
import static sidplay.ini.IniDefaults.DEFAULT_PAL_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_PHASE_SHIFT;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_LENGTH;
import static sidplay.ini.IniDefaults.DEFAULT_SATURATION;
import static sidplay.ini.IniDefaults.DEFAULT_SINGLE_TRACK;
import static sidplay.ini.IniDefaults.DEFAULT_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_TINT;
import static sidplay.ini.IniDefaults.DEFAULT_TMP_DIR;
import static sidplay.ini.IniDefaults.DEFAULT_TURBO_TAPE;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import sidplay.ini.converter.BeanToStringConverter;
import sidplay.ini.converter.FileToStringConverter;
import sidplay.ini.converter.ParameterTimeConverter;

/**
 * SIDPlay2 section of the INI file.
 *
 * @author Ken Händel
 *
 */
@Parameters(resourceBundle = "sidplay.ini.IniSidplay2Section")
public class IniSidplay2Section extends IniSection implements ISidPlay2Section {

	protected IniSidplay2Section(final IniReader ini) {
		super(ini);
	}

	@Override
	public final int getVersion() {
		return iniReader.getPropertyInt("SIDPlay2", "Version", IConfig.REQUIRED_CONFIG_VERSION);
	}

	@Override
	public void setVersion(int version) {
		iniReader.setProperty("SIDPlay2", "Version", version);
	}

	@Override
	public final boolean isEnableDatabase() {
		return iniReader.getPropertyBool("SIDPlay2", "EnableDatabase", DEFAULT_ENABLE_DATABASE);
	}

	@Override
	@Parameter(names = { "--enableSidDatabase", "-n" }, descriptionKey = "ENABLE_SID_DATABASE", arity = 1, order = 0)
	public final void setEnableDatabase(final boolean enable) {
		iniReader.setProperty("SIDPlay2", "EnableDatabase", enable);
	}

	@Override
	public final double getStartTime() {
		return iniReader.getPropertyTime("SIDPlay2", "Start Time", DEFAULT_START_TIME);
	}

	@Override
	@Parameter(names = { "--startTime",
			"-t" }, descriptionKey = "START_TIME", converter = ParameterTimeConverter.class, order = 1)
	public final void setStartTime(final double startTime) {
		String time = new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (startTime * 1000)));
		iniReader.setProperty("SIDPlay2", "Start Time", time);
	}

	@Override
	public final double getDefaultPlayLength() {
		return iniReader.getPropertyTime("SIDPlay2", "Default Play Length", DEFAULT_PLAY_LENGTH);
	}

	@Override
	@Parameter(names = { "--defaultLength",
			"-g" }, descriptionKey = "DEFAULT_LENGTH", converter = ParameterTimeConverter.class, order = 2)
	public final void setDefaultPlayLength(final double playLength) {
		String time = new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (playLength * 1000)));
		iniReader.setProperty("SIDPlay2", "Default Play Length", time);
	}

	@Override
	public double getFadeInTime() {
		return iniReader.getPropertyTime("SIDPlay2", "Fade In Time", DEFAULT_FADE_IN_TIME);
	}

	@Override
	@Parameter(names = { "--fadeIn" }, descriptionKey = "FADE_IN", converter = ParameterTimeConverter.class, order = 3)
	public void setFadeInTime(double fadeInTime) {
		String time = new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (fadeInTime * 1000)));
		iniReader.setProperty("SIDPlay2", "Fade In Time", time);
	}

	@Override
	public double getFadeOutTime() {
		return iniReader.getPropertyTime("SIDPlay2", "Fade Out Time", DEFAULT_FADE_OUT_TIME);
	}

	@Override
	@Parameter(names = {
			"--fadeOut" }, descriptionKey = "FADE_OUT", converter = ParameterTimeConverter.class, order = 4)
	public void setFadeOutTime(double fadeOutTime) {
		String time = new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (fadeOutTime * 1000)));
		iniReader.setProperty("SIDPlay2", "Fade Out Time", time);
	}

	@Override
	public boolean isLoop() {
		return iniReader.getPropertyBool("SIDPlay2", "Loop", DEFAULT_LOOP);
	}

	@Override
	@Parameter(names = { "--loop", "-l" }, descriptionKey = "LOOP", arity = 1, order = 5)
	public void setLoop(boolean loop) {
		iniReader.setProperty("SIDPlay2", "Loop", loop);
	}

	@Override
	public final boolean isSingle() {
		return iniReader.getPropertyBool("SIDPlay2", "SingleTrack", DEFAULT_SINGLE_TRACK);
	}

	@Override
	@Parameter(names = { "--single", "-s" }, descriptionKey = "SINGLE", arity = 1, order = 6)
	public final void setSingle(final boolean singleSong) {
		iniReader.setProperty("SIDPlay2", "SingleTrack", singleSong);
	}

	@Override
	public final File getHvsc() {
		return iniReader.getPropertyFile("SIDPlay2", "HVSC Dir", DEFAULT_HVSC_DIR);
	}

	@Override
	@Parameter(names = { "--hvsc" }, descriptionKey = "HVSC_DIR", converter = FileToStringConverter.class, order = 7)
	public final void setHvsc(final File hvsc) {
		iniReader.setProperty("SIDPlay2", "HVSC Dir", hvsc);
	}

	@Override
	public final File getLastDirectory() {
		return iniReader.getPropertyFile("SIDPlay2", "Last Directory", DEFAULT_LAST_DIR);
	}

	@Override
	public final void setLastDirectory(final File lastDir) {
		iniReader.setProperty("SIDPlay2", "Last Directory", lastDir);
	}

	@Override
	public final File getTmpDir() {
		return iniReader.getPropertyFile("SIDPlay2", "Temp Dir", DEFAULT_TMP_DIR);
	}

	@Override
	public final void setTmpDir(final File tmpDir) {
		iniReader.setProperty("SIDPlay2", "Temp Dir", tmpDir);
	}

	@Override
	public boolean isPalEmulation() {
		return iniReader.getPropertyBool("SIDPlay2", "PAL Emulation", DEFAULT_PAL_EMULATION);
	}

	@Override
	@Parameter(names = { "--palEmulation" }, descriptionKey = "PAL_EMULATION", arity = 1, order = 8)
	public void setPalEmulation(boolean palEmulation) {
		iniReader.setProperty("SIDPlay2", "PAL Emulation", palEmulation);
	}

	@Override
	public float getBrightness() {
		return iniReader.getPropertyFloat("SIDPlay2", "Brightness", DEFAULT_BRIGHTNESS);
	}

	@Override
	public void setBrightness(float brightness) {
		iniReader.setProperty("SIDPlay2", "Brightness", brightness);
	}

	@Override
	public float getContrast() {
		return iniReader.getPropertyFloat("SIDPlay2", "Contrast", DEFAULT_CONTRAST);
	}

	@Override
	public void setContrast(float contrast) {
		iniReader.setProperty("SIDPlay2", "Contrast", contrast);
	}

	@Override
	public float getGamma() {
		return iniReader.getPropertyFloat("SIDPlay2", "Gamma", DEFAULT_GAMMA);
	}

	@Override
	public void setGamma(float gamma) {
		iniReader.setProperty("SIDPlay2", "Gamma", gamma);
	}

	@Override
	public float getSaturation() {
		return iniReader.getPropertyFloat("SIDPlay2", "Saturation", DEFAULT_SATURATION);
	}

	@Override
	public void setSaturation(float saturation) {
		iniReader.setProperty("SIDPlay2", "Saturation", saturation);
	}

	@Override
	public float getPhaseShift() {
		return iniReader.getPropertyFloat("SIDPlay2", "Phase Shift", DEFAULT_PHASE_SHIFT);
	}

	@Override
	public void setPhaseShift(float phaseShift) {
		iniReader.setProperty("SIDPlay2", "Phase Shift", phaseShift);
	}

	@Override
	public float getOffset() {
		return iniReader.getPropertyFloat("SIDPlay2", "Offset", DEFAULT_OFFSET);
	}

	@Override
	public void setOffset(float offset) {
		iniReader.setProperty("SIDPlay2", "Offset", offset);
	}

	@Override
	public float getTint() {
		return iniReader.getPropertyFloat("SIDPlay2", "Tint", DEFAULT_TINT);
	}

	@Override
	public void setTint(float tint) {
		iniReader.setProperty("SIDPlay2", "Tint", tint);
	}

	@Override
	public float getBlur() {
		return iniReader.getPropertyFloat("SIDPlay2", "Blur", DEFAULT_BLUR);
	}

	@Override
	public void setBlur(float blur) {
		iniReader.setProperty("SIDPlay2", "Blur", blur);
	}

	@Override
	public float getBleed() {
		return iniReader.getPropertyFloat("SIDPlay2", "Bleed", DEFAULT_BLEED);
	}

	@Override
	public void setBleed(float bleed) {
		iniReader.setProperty("SIDPlay2", "Bleed", bleed);
	}

	@Override
	public boolean isTurboTape() {
		return iniReader.getPropertyBool("SIDPlay2", "TurboTape", DEFAULT_TURBO_TAPE);
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		iniReader.setProperty("SIDPlay2", "TurboTape", turboTape);
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}

}