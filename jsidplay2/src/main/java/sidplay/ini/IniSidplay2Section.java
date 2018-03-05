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
import static sidplay.ini.IniDefaults.DEFAULT_PHASE_SHIFT;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_LENGTH;
import static sidplay.ini.IniDefaults.DEFAULT_SATURATION;
import static sidplay.ini.IniDefaults.DEFAULT_SINGLE_TRACK;
import static sidplay.ini.IniDefaults.DEFAULT_TINT;
import static sidplay.ini.IniDefaults.DEFAULT_TURBO_TAPE;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import sidplay.consoleplayer.ParameterTimeConverter;

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
	@Parameter(names = { "--enableSidDatabase", "-n" }, descriptionKey = "ENABLE_SID_DATABASE", arity = 1)
	public final void setEnableDatabase(final boolean enable) {
		iniReader.setProperty("SIDPlay2", "EnableDatabase", enable);
	}

	@Override
	public final int getDefaultPlayLength() {
		return iniReader.getPropertyTime("SIDPlay2", "Default Play Length", DEFAULT_PLAY_LENGTH);
	}

	@Override
	@Parameter(names = { "--defaultLength", "-g" }, descriptionKey = "DEFAULT_LENGTH", converter = ParameterTimeConverter.class)
	public final void setDefaultPlayLength(final int playLength) {
		iniReader.setProperty("SIDPlay2", "Default Play Length",
				String.format("%02d:%02d", (playLength / 60), (playLength % 60)));
	}

	@Override
	public int getFadeInTime() {
		return iniReader.getPropertyTime("SIDPlay2", "Fade In Time", DEFAULT_FADE_IN_TIME);
	}

	@Override
	public void setFadeInTime(int fadeInTime) {
		iniReader.setProperty("SIDPlay2", "Fade In Time",
				String.format("%02d:%02d", (fadeInTime / 60), (fadeInTime % 60)));
	}
	
	@Override
	public int getFadeOutTime() {
		return iniReader.getPropertyTime("SIDPlay2", "Fade Out Time", DEFAULT_FADE_OUT_TIME);
	}

	@Override
	public void setFadeOutTime(int fadeOutTime) {
		iniReader.setProperty("SIDPlay2", "Fade Out Time",
				String.format("%02d:%02d", (fadeOutTime / 60), (fadeOutTime % 60)));
	}
	
	@Override
	public boolean isLoop() {
		return iniReader.getPropertyBool("SIDPlay2", "Loop", DEFAULT_LOOP);
	}

	@Override
	@Parameter(names = { "--loop", "-l" }, descriptionKey = "LOOP", arity=1)
	public void setLoop(boolean loop) {
		iniReader.setProperty("SIDPlay2", "Loop", loop);
	}

	@Override
	public final boolean isSingle() {
		return iniReader.getPropertyBool("SIDPlay2", "SingleTrack", DEFAULT_SINGLE_TRACK);
	}

	@Override
	@Parameter(names = { "--single", "-s" }, descriptionKey = "SINGLE", arity=1)
	public final void setSingle(final boolean singleSong) {
		iniReader.setProperty("SIDPlay2", "SingleTrack", singleSong);
	}

	@Override
	public final String getHvsc() {
		return iniReader.getPropertyString("SIDPlay2", "HVSC Dir", DEFAULT_HVSC_DIR);
	}

	@Override
	public final void setHvsc(final String hvsc) {
		iniReader.setProperty("SIDPlay2", "HVSC Dir", hvsc);
	}

	@Override
	public final String getLastDirectory() {
		return iniReader.getPropertyString("SIDPlay2", "Last Directory", DEFAULT_LAST_DIR);
	}

	@Override
	public final void setLastDirectory(final String lastDir) {
		iniReader.setProperty("SIDPlay2", "Last Directory", lastDir);
	}

	@Override
	public final String getTmpDir() {
		return iniReader.getPropertyString("SIDPlay2", "Temp Dir",
				System.getProperty("user.home") + System.getProperty("file.separator") + ".jsidplay2");
	}

	@Override
	public final void setTmpDir(final String path) {
		iniReader.setProperty("SIDPlay2", "Temp Dir", path);
	}

	public float getBrightness() {
		return iniReader.getPropertyFloat("SIDPlay2", "Brightness", DEFAULT_BRIGHTNESS);
	}

	public void setBrightness(float brightness) {
		iniReader.setProperty("SIDPlay2", "Brightness", brightness);
	}

	public float getContrast() {
		return iniReader.getPropertyFloat("SIDPlay2", "Contrast", DEFAULT_CONTRAST);
	}

	public void setContrast(float contrast) {
		iniReader.setProperty("SIDPlay2", "Contrast", contrast);
	}

	public float getGamma() {
		return iniReader.getPropertyFloat("SIDPlay2", "Gamma", DEFAULT_GAMMA);
	}

	public void setGamma(float gamma) {
		iniReader.setProperty("SIDPlay2", "Gamma", gamma);
	}

	public float getSaturation() {
		return iniReader.getPropertyFloat("SIDPlay2", "Saturation", DEFAULT_SATURATION);
	}

	public void setSaturation(float saturation) {
		iniReader.setProperty("SIDPlay2", "Saturation", saturation);
	}

	public float getPhaseShift() {
		return iniReader.getPropertyFloat("SIDPlay2", "Phase Shift", DEFAULT_PHASE_SHIFT);
	}

	public void setPhaseShift(float phaseShift) {
		iniReader.setProperty("SIDPlay2", "Phase Shift", phaseShift);
	}

	public float getOffset() {
		return iniReader.getPropertyFloat("SIDPlay2", "Offset", DEFAULT_OFFSET);
	}

	public void setOffset(float offset) {
		iniReader.setProperty("SIDPlay2", "Offset", offset);
	}

	public float getTint() {
		return iniReader.getPropertyFloat("SIDPlay2", "Tint", DEFAULT_TINT);
	}

	public void setTint(float tint) {
		iniReader.setProperty("SIDPlay2", "Tint", tint);
	}

	public float getBlur() {
		return iniReader.getPropertyFloat("SIDPlay2", "Blur", DEFAULT_BLUR);
	}

	public void setBlur(float blur) {
		iniReader.setProperty("SIDPlay2", "Blur", blur);
	}

	public float getBleed() {
		return iniReader.getPropertyFloat("SIDPlay2", "Bleed", DEFAULT_BLEED);
	}

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

}