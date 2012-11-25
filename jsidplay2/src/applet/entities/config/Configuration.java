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
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IC1541Section;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;
import sidplay.ini.intf.IJoystickSection;
import sidplay.ini.intf.IPrinterSection;
import sidplay.ini.intf.ISidPlay2Section;
import applet.config.annotations.ConfigTypeName;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigSectionName;
import applet.config.annotations.ConfigTransient;

@Entity
@XmlRootElement(name = "configuration")
@ConfigTypeName(bundleKey = "CONFIGURATION")
public class Configuration implements IConfig {

	/** Bump this each time you want to invalidate the configuration */
	@ConfigTransient
	public static final int REQUIRED_CONFIG_VERSION = 19;

	@Transient
	@XmlTransient
	@ConfigTransient
	private final List<FilterSection> INITIAL_FILTERS;
	{
		INITIAL_FILTERS = new ArrayList<FilterSection>();
		FilterSection dbFilterSection;
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterLight8580");
		dbFilterSection.setFilter8580CurvePosition(13400);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterAverage8580");
		dbFilterSection.setFilter8580CurvePosition(12500);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterDark8580");
		dbFilterSection.setFilter8580CurvePosition(11700);
		INITIAL_FILTERS.add(dbFilterSection);

		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterLightest6581");
		dbFilterSection.setFilter6581CurvePosition(0.1f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterLighter6581");
		dbFilterSection.setFilter6581CurvePosition(0.3f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterLight6581");
		dbFilterSection.setFilter6581CurvePosition(0.4f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterAverage6581");
		dbFilterSection.setFilter6581CurvePosition(0.5f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterDark6581");
		dbFilterSection.setFilter6581CurvePosition(0.6f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterDarker6581");
		dbFilterSection.setFilter6581CurvePosition(0.7f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new FilterSection();
		dbFilterSection.setName("FilterDarkest6581");
		dbFilterSection.setFilter6581CurvePosition(0.9f);
		INITIAL_FILTERS.add(dbFilterSection);
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@ConfigTransient
	private Integer id;

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlTransient
	@ConfigTransient
	private String reconfigFilename;

	public String getReconfigFilename() {
		return reconfigFilename;
	}

	public void setReconfigFilename(String reconfigFilename) {
		this.reconfigFilename = reconfigFilename;
	}

	@Embedded
	@XmlElement(name = "sidplay2")
	private SidPlay2Section sidplay2 = new SidPlay2Section();

	public void setSidplay2(SidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
	}

	@Override
	@ConfigSectionName(bundleKey = "GENERAL")
	public ISidPlay2Section getSidplay2() {
		return sidplay2;
	}

	@Embedded
	@XmlElement(name = "online")
	private OnlineSection online = new OnlineSection();

	@XmlTransient
	@ConfigSectionName(bundleKey = "ONLINE")
	public OnlineSection getOnline() {
		return online;
	}

	public void setOnline(OnlineSection online) {
		this.online = online;
	}

	@Embedded
	@XmlElement(name = "c1541")
	private C1541Section c1541 = new C1541Section();

	public void setC1541(C1541Section c1541) {
		this.c1541 = c1541;
	}

	@Override
	@ConfigSectionName(bundleKey = "C1541")
	public IC1541Section getC1541() {
		return c1541;
	}

	@Embedded
	@XmlElement(name = "printer")
	private PrinterSection printer = new PrinterSection();

	public void setPrinter(PrinterSection printer) {
		this.printer = printer;
	}

	@Override
	@ConfigSectionName(bundleKey = "PRINTER")
	public IPrinterSection getPrinter() {
		return printer;
	}

	@Embedded
	@XmlElement(name = "joystick")
	private JoystickSection joystick = new JoystickSection();

	public void setJoystick(JoystickSection joystick) {
		this.joystick = joystick;
	}

	@Override
	@ConfigSectionName(bundleKey = "JOYSTICK")
	public IJoystickSection getJoystick() {
		return joystick;
	}

	@Embedded
	@XmlElement(name = "console")
	private ConsoleSection console = new ConsoleSection();

	public void setConsole(ConsoleSection console) {
		this.console = console;
	}

	@Override
	@ConfigSectionName(bundleKey = "CONSOLE")
	public IConsoleSection getConsole() {
		return console;
	}

	@Embedded
	@XmlElement(name = "audio")
	private AudioSection audio = new AudioSection();

	public void setAudio(AudioSection audio) {
		this.audio = audio;
	}

	@Override
	@ConfigSectionName(bundleKey = "AUDIO")
	public IAudioSection getAudio() {
		return audio;
	}

	@Embedded
	@XmlElement(name = "emulation")
	private EmulationSection emulation = new EmulationSection();

	public void setEmulation(EmulationSection emulation) {
		this.emulation = emulation;
	}

	@Override
	@ConfigSectionName(bundleKey = "EMULATION")
	public IEmulationSection getEmulation() {
		return emulation;
	}

	@ConfigDescription(bundleKey = "CURRENT_FAVORITE_DESC", toolTipBundleKey = "CURRENT_FAVORITE_TOOLTIP")
	private String currentFavorite;

	public String getCurrentFavorite() {
		return currentFavorite;
	}

	public void setCurrentFavorite(String currentFavorite) {
		this.currentFavorite = currentFavorite;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@XmlElement(name = "favorites")
	private List<FavoritesSection> favorites;

	public void setFavorites(List<FavoritesSection> favorites) {
		this.favorites = favorites;
	}

	@XmlTransient
	@ConfigSectionName(bundleKey = "FAVORITES")
	public List<FavoritesSection> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<FavoritesSection>();
		}
		return favorites;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@XmlElement(name = "filter")
	private List<FilterSection> filter = INITIAL_FILTERS;

	public void setFilter(List<FilterSection> filter) {
		this.filter = filter;
	}

	@Override
	@ConfigSectionName(bundleKey = "FILTERS")
	public List<? extends IFilterSection> getFilter() {
		if (filter == null) {
			filter = new ArrayList<FilterSection>();
		}
		return filter;
	}

}
