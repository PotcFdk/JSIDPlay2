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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
import applet.config.annotations.ConfigClass;
import applet.config.annotations.ConfigMethod;
import applet.config.annotations.ConfigTransient;

@Entity
@XmlRootElement(name = "config")
@ConfigClass(getBundleKey = "CONFIG")
public class DbConfig implements IConfig {

	@Transient
	@XmlTransient
	@ConfigTransient
	private final List<DbFilterSection> INITIAL_FILTERS;
	{
		INITIAL_FILTERS = new ArrayList<DbFilterSection>();
		DbFilterSection dbFilterSection;
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterLight8580");
		dbFilterSection.setFilter8580CurvePosition(13400);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterAverage8580");
		dbFilterSection.setFilter8580CurvePosition(12500);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterDark8580");
		dbFilterSection.setFilter8580CurvePosition(11700);
		INITIAL_FILTERS.add(dbFilterSection);

		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterLightest6581");
		dbFilterSection.setFilter6581CurvePosition(0.1f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterLighter6581");
		dbFilterSection.setFilter6581CurvePosition(0.3f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterLight6581");
		dbFilterSection.setFilter6581CurvePosition(0.4f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterAverage6581");
		dbFilterSection.setFilter6581CurvePosition(0.5f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterDark6581");
		dbFilterSection.setFilter6581CurvePosition(0.6f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterDarker6581");
		dbFilterSection.setFilter6581CurvePosition(0.7f);
		INITIAL_FILTERS.add(dbFilterSection);
		dbFilterSection = new DbFilterSection();
		dbFilterSection.setDbConfig(this);
		dbFilterSection.setName("FilterDarkest6581");
		dbFilterSection.setFilter6581CurvePosition(0.9f);
		INITIAL_FILTERS.add(dbFilterSection);
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlJavaTypeAdapter(IntegerAdapter.class)
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
	private DbSidPlay2Section sidplay2 = new DbSidPlay2Section();

	public void setSidplay2(DbSidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
	}

	@Override
	@ConfigMethod(getBundleKey = "JSIDPLAY2")
	public ISidPlay2Section getSidplay2() {
		return sidplay2;
	}

	@Embedded
	@XmlElement(name = "online")
	private DbOnlineSection online = new DbOnlineSection();

	@Override
	@XmlTransient
	@ConfigMethod(getBundleKey = "ONLINE")
	public DbOnlineSection getOnline() {
		return online;
	}

	public void setOnline(DbOnlineSection online) {
		this.online = online;
	}

	@Embedded
	@XmlElement(name = "c1541")
	private DbC1541Section c1541 = new DbC1541Section();

	public void setC1541(DbC1541Section c1541) {
		this.c1541 = c1541;
	}

	@Override
	@ConfigMethod(getBundleKey = "C1541")
	public IC1541Section getC1541() {
		return c1541;
	}

	@Embedded
	@XmlElement(name = "printer")
	private DbPrinterSection printer = new DbPrinterSection();

	public void setPrinter(DbPrinterSection printer) {
		this.printer = printer;
	}

	@Override
	@ConfigMethod(getBundleKey = "PRINTER")
	public IPrinterSection getPrinter() {
		return printer;
	}

	@Embedded
	@XmlElement(name = "joystick")
	private DbJoystickSection joystick = new DbJoystickSection();

	public void setJoystick(DbJoystickSection joystick) {
		this.joystick = joystick;
	}

	@Override
	@ConfigMethod(getBundleKey = "JOYSTICK")
	public IJoystickSection getJoystick() {
		return joystick;
	}

	@Embedded
	@XmlElement(name = "console")
	private DbConsoleSection console = new DbConsoleSection();

	public void setConsole(DbConsoleSection console) {
		this.console = console;
	}

	@Override
	@ConfigMethod(getBundleKey = "CONSOLE")
	public IConsoleSection getConsole() {
		return console;
	}

	@Embedded
	@XmlElement(name = "audio")
	private DbAudioSection audio = new DbAudioSection();

	public void setAudio(DbAudioSection audio) {
		this.audio = audio;
	}

	@Override
	@ConfigMethod(getBundleKey = "AUDIO")
	public IAudioSection getAudio() {
		return audio;
	}

	@Embedded
	@XmlElement(name = "emulation")
	private DbEmulationSection emulation = new DbEmulationSection();

	public void setEmulation(DbEmulationSection emulation) {
		this.emulation = emulation;
	}

	@Override
	@ConfigMethod(getBundleKey = "EMULATION")
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

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.ALL)
	@XmlElement(name = "favorites")
	private List<DbFavoritesSection> favorites;

	public void setFavorites(List<DbFavoritesSection> favorites) {
		this.favorites = favorites;
	}

	@Override
	@ConfigMethod(getBundleKey = "FAVORITES")
	public List<? extends IFavoritesSection> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<DbFavoritesSection>();
		}
		return favorites;
	}

	public List<DbFavoritesSection> getFavoritesInternal() {
		if (favorites == null) {
			favorites = new ArrayList<DbFavoritesSection>();
		}
		return favorites;
	}

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.ALL)
	@XmlElement(name = "filter")
	private List<DbFilterSection> filter = INITIAL_FILTERS;

	public void setFilter(List<DbFilterSection> filter) {
		this.filter = filter;
	}

	@Override
	@ConfigMethod(getBundleKey = "FILTERS")
	public List<? extends IFilterSection> getFilter() {
		if (filter == null) {
			filter = new ArrayList<DbFilterSection>();
		}
		return filter;
	}

}
