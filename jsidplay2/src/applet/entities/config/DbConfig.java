package applet.entities.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import sidplay.ini.IniConfig;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IC1541Section;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFavoritesSection;
import sidplay.ini.intf.IFilterSection;
import sidplay.ini.intf.IJoystickSection;
import sidplay.ini.intf.IPrinterSection;
import sidplay.ini.intf.ISidPlay2Section;

@Entity
public class DbConfig implements IConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Embedded
	private DbSidPlay2Section sidplay2;

	public void setSidplay2(DbSidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
	}

	@Override
	public ISidPlay2Section getSidplay2() {
		return sidplay2;
	}

	@Embedded
	private DbC1541Section c1541;

	public void setC1541(DbC1541Section c1541) {
		this.c1541 = c1541;
	}

	@Override
	public IC1541Section getC1541() {
		return c1541;
	}

	@Embedded
	private DbPrinterSection printer;

	public void setPrinter(DbPrinterSection printer) {
		this.printer = printer;
	}

	@Override
	public IPrinterSection getPrinter() {
		return printer;
	}

	@Embedded
	private DbJoystickSection joystick;

	public void setJoystick(DbJoystickSection joystick) {
		this.joystick = joystick;
	}

	@Override
	public IJoystickSection getJoystick() {
		return joystick;
	}

	@Embedded
	private DbConsoleSection console;

	public void setConsole(DbConsoleSection console) {
		this.console = console;
	}

	@Override
	public IConsoleSection getConsole() {
		return console;
	}

	@Embedded
	private DbAudioSection audio;

	public void setAudio(DbAudioSection audio) {
		this.audio = audio;
	}

	@Override
	public IAudioSection getAudio() {
		return audio;
	}

	@Embedded
	private DbEmulationSection emulation;

	public void setEmulation(DbEmulationSection emulation) {
		this.emulation = emulation;
	}

	@Override
	public IEmulationSection getEmulation() {
		return emulation;
	}

	private String currentFavorite;

	@Override
	public String getCurrentFavorite() {
		return currentFavorite;
	}

	@Override
	public void setCurrentFavorite(String currentFavorite) {
		this.currentFavorite = currentFavorite;
	}

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.PERSIST)
	private List<DbFavoritesSection> favorites;

	public void setFavorites(List<DbFavoritesSection> favorites) {
		this.favorites = favorites;
	}

	@Override
	public List<? extends IFavoritesSection> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<DbFavoritesSection>();
		}
		return favorites;
	}

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.PERSIST)
	private List<DbFilterSection> filter;

	public void setFilter(List<DbFilterSection> filter) {
		this.filter = filter;
	}

	@Override
	public List<? extends IFilterSection> getFilter() {
		if (filter == null) {
			filter = new ArrayList<DbFilterSection>();
		}
		return filter;
	}

	public void copyFrom(IniConfig iniConfig) {
		setSidplay2(new DbSidPlay2Section());
		getSidplay2().setEnableDatabase(
				iniConfig.getSidplay2().isEnableDatabase());
		getSidplay2().setPlayLength(iniConfig.getSidplay2().getPlayLength());
		getSidplay2()
				.setRecordLength(iniConfig.getSidplay2().getRecordLength());
		getSidplay2().setHVMEC(iniConfig.getSidplay2().getHVMEC());
		getSidplay2().setDemos(iniConfig.getSidplay2().getDemos());
		getSidplay2().setMags(iniConfig.getSidplay2().getMags());
		getSidplay2().setCgsc(iniConfig.getSidplay2().getCgsc());
		getSidplay2().setHvsc(iniConfig.getSidplay2().getHvsc());
		getSidplay2().setSingle(iniConfig.getSidplay2().isSingle());
		getSidplay2().setSoasc6581R2(iniConfig.getSidplay2().getSoasc6581R2());
		getSidplay2().setSoasc6581R4(iniConfig.getSidplay2().getSoasc6581R4());
		getSidplay2().setSoasc8580R5(iniConfig.getSidplay2().getSoasc8580R5());
		getSidplay2().setEnableProxy(iniConfig.getSidplay2().isEnableProxy());
		getSidplay2().setProxyHostname(
				iniConfig.getSidplay2().getProxyHostname());
		getSidplay2().setProxyPort(iniConfig.getSidplay2().getProxyPort());
		getSidplay2().setLastDirectory(
				iniConfig.getSidplay2().getLastDirectory());
		getSidplay2().setTmpDir(iniConfig.getSidplay2().getTmpDir());
		getSidplay2().setFrameX(iniConfig.getSidplay2().getFrameX());
		getSidplay2().setFrameY(iniConfig.getSidplay2().getFrameY());
		getSidplay2().setFrameWidth(iniConfig.getSidplay2().getFrameWidth());
		getSidplay2().setFrameHeight(iniConfig.getSidplay2().getFrameHeight());

		setC1541(new DbC1541Section());
		getC1541().setDriveOn(iniConfig.getC1541().isDriveOn());
		getC1541().setDriveSoundOn(iniConfig.getC1541().isDriveSoundOn());
		getC1541().setParallelCable(iniConfig.getC1541().isParallelCable());
		getC1541().setRamExpansion0(
				iniConfig.getC1541().isRamExpansionEnabled0());
		getC1541().setRamExpansion1(
				iniConfig.getC1541().isRamExpansionEnabled1());
		getC1541().setRamExpansion2(
				iniConfig.getC1541().isRamExpansionEnabled2());
		getC1541().setRamExpansion3(
				iniConfig.getC1541().isRamExpansionEnabled3());
		getC1541().setRamExpansion4(
				iniConfig.getC1541().isRamExpansionEnabled4());
		getC1541().setExtendImagePolicy(
				iniConfig.getC1541().getExtendImagePolicy());
		getC1541().setFloppyType(iniConfig.getC1541().getFloppyType());

		setPrinter(new DbPrinterSection());
		getPrinter().setPrinterOn(iniConfig.getPrinter().isPrinterOn());

		setJoystick(new DbJoystickSection());
		getJoystick().setDeviceName1(iniConfig.getJoystick().getDeviceName1());
		getJoystick().setDeviceName2(iniConfig.getJoystick().getDeviceName2());
		getJoystick().setComponentNameUp1(
				iniConfig.getJoystick().getComponentNameUp1());
		getJoystick().setComponentNameUp2(
				iniConfig.getJoystick().getComponentNameUp2());
		getJoystick().setComponentNameDown1(
				iniConfig.getJoystick().getComponentNameDown1());
		getJoystick().setComponentNameDown2(
				iniConfig.getJoystick().getComponentNameDown2());
		getJoystick().setComponentNameLeft1(
				iniConfig.getJoystick().getComponentNameLeft1());
		getJoystick().setComponentNameLeft2(
				iniConfig.getJoystick().getComponentNameLeft2());
		getJoystick().setComponentNameRight1(
				iniConfig.getJoystick().getComponentNameRight1());
		getJoystick().setComponentNameRight2(
				iniConfig.getJoystick().getComponentNameRight2());
		getJoystick().setComponentNameBtn1(
				iniConfig.getJoystick().getComponentNameBtn1());
		getJoystick().setComponentNameBtn2(
				iniConfig.getJoystick().getComponentNameBtn2());
		getJoystick().setComponentValueUp1(
				iniConfig.getJoystick().getComponentValueUp1());
		getJoystick().setComponentValueUp2(
				iniConfig.getJoystick().getComponentValueUp2());
		getJoystick().setComponentValueDown1(
				iniConfig.getJoystick().getComponentValueDown1());
		getJoystick().setComponentValueDown2(
				iniConfig.getJoystick().getComponentValueDown2());
		getJoystick().setComponentValueLeft1(
				iniConfig.getJoystick().getComponentValueLeft1());
		getJoystick().setComponentValueLeft2(
				iniConfig.getJoystick().getComponentValueLeft2());
		getJoystick().setComponentValueRight1(
				iniConfig.getJoystick().getComponentValueRight1());
		getJoystick().setComponentValueRight2(
				iniConfig.getJoystick().getComponentValueRight2());
		getJoystick().setComponentValueBtn1(
				iniConfig.getJoystick().getComponentValueBtn1());
		getJoystick().setComponentValueBtn2(
				iniConfig.getJoystick().getComponentValueBtn2());

		// XXX console section
		setConsole(new DbConsoleSection());

		setAudio(new DbAudioSection());
		getAudio().setFrequency(iniConfig.getAudio().getFrequency());
		getAudio().setSampling(iniConfig.getAudio().getSampling());
		getAudio().setPlayOriginal(iniConfig.getAudio().isPlayOriginal());
		getAudio().setMp3File(iniConfig.getAudio().getMp3File());
		getAudio().setLeftVolume(iniConfig.getAudio().getLeftVolume());
		getAudio().setRightVolume(iniConfig.getAudio().getRightVolume());

		setEmulation(new DbEmulationSection());
		getEmulation().setDefaultClockSpeed(
				iniConfig.getEmulation().getDefaultClockSpeed());
		getEmulation().setUserClockSpeed(
				iniConfig.getEmulation().getUserClockSpeed());
		getEmulation().setDefaultSidModel(
				iniConfig.getEmulation().getDefaultSidModel());
		getEmulation().setUserSidModel(
				iniConfig.getEmulation().getUserSidModel());
		getEmulation()
				.setHardsid6581(iniConfig.getEmulation().getHardsid6581());
		getEmulation()
				.setHardsid8580(iniConfig.getEmulation().getHardsid8580());
		getEmulation().setFilter(iniConfig.getEmulation().isFilter());
		getEmulation().setFilter6581(iniConfig.getEmulation().getFilter6581());
		getEmulation().setFilter8580(iniConfig.getEmulation().getFilter8580());
		getEmulation().setDigiBoosted8580(
				iniConfig.getEmulation().isDigiBoosted8580());
		getEmulation()
				.setDualSidBase(iniConfig.getEmulation().getDualSidBase());
		getEmulation().setForceStereoTune(
				iniConfig.getEmulation().isForceStereoTune());
		getEmulation().setStereoSidModel(
				iniConfig.getEmulation().getStereoSidModel());

		ArrayList<DbFavoritesSection> newFavoritesList = new ArrayList<DbFavoritesSection>();
		for (IFavoritesSection f : iniConfig.getFavorites()) {
			DbFavoritesSection newFavorite = new DbFavoritesSection();
			newFavorite.setDbConfig(this);
			newFavorite.setName(f.getName());
			newFavorite.setFilename(f.getFilename());
			newFavoritesList.add(newFavorite);
		}
		setCurrentFavorite(iniConfig.getCurrentFavorite());
		setFavorites(newFavoritesList);

		ArrayList<DbFilterSection> newFilterList = new ArrayList<DbFilterSection>();
		for (IFilterSection f : iniConfig.getFilter()) {
			DbFilterSection newFilter = new DbFilterSection();
			newFilter.setDbConfig(this);
			newFilter
					.setFilter6581CurvePosition(f.getFilter6581CurvePosition());
			newFilter
					.setFilter8580CurvePosition(f.getFilter8580CurvePosition());
			newFilter.setName(f.getName());
			newFilterList.add(newFilter);
		}
		setFilter(newFilterList);
	}

}
