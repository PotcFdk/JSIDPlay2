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

@Entity
@XmlRootElement(name = "config")
public class DbConfig implements IConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlJavaTypeAdapter(IntegerAdapter.class)
	private Integer id;

	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	@XmlElement(name = "sidplay2")
	private DbSidPlay2Section sidplay2;

	public void setSidplay2(DbSidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
	}

	@Override
	public ISidPlay2Section getSidplay2() {
		return sidplay2;
	}

	@Embedded
	@XmlElement(name = "c1541")
	private DbC1541Section c1541;

	public void setC1541(DbC1541Section c1541) {
		this.c1541 = c1541;
	}

	@Override
	public IC1541Section getC1541() {
		return c1541;
	}

	@Embedded
	@XmlElement(name = "printer")
	private DbPrinterSection printer;

	public void setPrinter(DbPrinterSection printer) {
		this.printer = printer;
	}

	@Override
	public IPrinterSection getPrinter() {
		return printer;
	}

	@Embedded
	@XmlElement(name = "joystick")
	private DbJoystickSection joystick;

	public void setJoystick(DbJoystickSection joystick) {
		this.joystick = joystick;
	}

	@Override
	public IJoystickSection getJoystick() {
		return joystick;
	}

	@Embedded
	@XmlElement(name = "console")
	private DbConsoleSection console;

	public void setConsole(DbConsoleSection console) {
		this.console = console;
	}

	@Override
	public IConsoleSection getConsole() {
		return console;
	}

	@Embedded
	@XmlElement(name = "audio")
	private DbAudioSection audio;

	public void setAudio(DbAudioSection audio) {
		this.audio = audio;
	}

	@Override
	public IAudioSection getAudio() {
		return audio;
	}

	@Embedded
	@XmlElement(name = "emulation")
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

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.ALL)
	@XmlElement(name = "favorites")
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

	public List<DbFavoritesSection> getFavoritesInternal() {
		if (favorites == null) {
			favorites = new ArrayList<DbFavoritesSection>();
		}
		return favorites;
	}

	@OneToMany(mappedBy = "dbConfig", cascade = CascadeType.PERSIST)
	@XmlElement(name = "filter")
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

}
