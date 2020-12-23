package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.beust.jcommander.ParametersDelegate;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.config.IConfig;
import libsidplay.config.IFilterSection;
import ui.console.Console;
import ui.videoscreen.Video;

/**
 * 
 * Configuration of the UI version of JSIDPlay2.
 * 
 * @author ken
 *
 */
@Entity
@Access(AccessType.PROPERTY)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Configuration implements IConfig {

	private final List<FilterSection> INITIAL_FILTERS;
	{
		INITIAL_FILTERS = DEFAULTS.getFilterSection().stream().map((Function<IFilterSection, FilterSection>) f -> {
			FilterSection dbFilterSection = new FilterSection();
			if (f.isReSIDFilter8580()) {
				dbFilterSection.setName(f.getName());
				dbFilterSection.setFilter8580CurvePosition(f.getFilter8580CurvePosition());
			} else if (f.isReSIDFilter6581()) {
				dbFilterSection.setName(f.getName());
				dbFilterSection.setFilter6581CurvePosition(f.getFilter6581CurvePosition());
			} else if (f.isReSIDfpFilter8580()) {
				dbFilterSection.setName(f.getName());
				dbFilterSection.setK(f.getK());
				dbFilterSection.setB(f.getB());
				dbFilterSection.setVoiceNonlinearity(f.getVoiceNonlinearity());
				dbFilterSection.setResonanceFactor(f.getResonanceFactor());
			} else if (f.isReSIDfpFilter6581()) {
				dbFilterSection.setName(f.getName());
				dbFilterSection.setAttenuation(f.getAttenuation());
				dbFilterSection.setNonlinearity(f.getNonlinearity());
				dbFilterSection.setVoiceNonlinearity(f.getVoiceNonlinearity());
				dbFilterSection.setBaseresistance(f.getBaseresistance());
				dbFilterSection.setOffset(f.getOffset());
				dbFilterSection.setSteepness(f.getSteepness());
				dbFilterSection.setMinimumfetresistance(f.getMinimumfetresistance());
				dbFilterSection.setResonanceFactor(f.getResonanceFactor());
			}
			return dbFilterSection;
		}).collect(Collectors.toList());
	}

	private final List<ViewEntity> INITIAL_VIEWS;
	{
		INITIAL_VIEWS = new ArrayList<>(Arrays.asList(new ViewEntity(Console.ID), new ViewEntity(Video.ID)));
	}

	private final List<KeyTableEntity> INITIAL_KEYCODES;
	{
		INITIAL_KEYCODES = new ArrayList<>(Arrays.asList(new KeyTableEntity(KeyCode.A.getName(), KeyTableEntry.A),
				new KeyTableEntity(KeyCode.BACK_SLASH.getName(), KeyTableEntry.ARROW_LEFT),
				new KeyTableEntity(KeyCode.DIGIT1.getName(), KeyTableEntry.ONE),
				new KeyTableEntity(KeyCode.DIGIT2.getName(), KeyTableEntry.TWO),
				new KeyTableEntity(KeyCode.DIGIT3.getName(), KeyTableEntry.THREE),
				new KeyTableEntity(KeyCode.DIGIT4.getName(), KeyTableEntry.FOUR),
				new KeyTableEntity(KeyCode.DIGIT5.getName(), KeyTableEntry.FIVE),
				new KeyTableEntity(KeyCode.DIGIT6.getName(), KeyTableEntry.SIX),
				new KeyTableEntity(KeyCode.DIGIT7.getName(), KeyTableEntry.SEVEN),
				new KeyTableEntity(KeyCode.DIGIT8.getName(), KeyTableEntry.EIGHT),
				new KeyTableEntity(KeyCode.DIGIT9.getName(), KeyTableEntry.NINE),
				new KeyTableEntity(KeyCode.DIGIT0.getName(), KeyTableEntry.ZERO),
				new KeyTableEntity(KeyCode.OPEN_BRACKET.getName(), KeyTableEntry.PLUS),
				new KeyTableEntity(KeyCode.CLOSE_BRACKET.getName(), KeyTableEntry.MINUS),
				new KeyTableEntity(KeyCode.POUND.getName(), KeyTableEntry.POUND),
				new KeyTableEntity(KeyCode.HOME.getName(), KeyTableEntry.CLEAR_HOME),
				new KeyTableEntity(KeyCode.BACK_SPACE.getName(), KeyTableEntry.INS_DEL),

				new KeyTableEntity(KeyCode.Q.getName(), KeyTableEntry.Q),
				new KeyTableEntity(KeyCode.W.getName(), KeyTableEntry.W),
				new KeyTableEntity(KeyCode.E.getName(), KeyTableEntry.E),
				new KeyTableEntity(KeyCode.R.getName(), KeyTableEntry.R),
				new KeyTableEntity(KeyCode.T.getName(), KeyTableEntry.T),
				new KeyTableEntity(KeyCode.Y.getName(), KeyTableEntry.Y),
				new KeyTableEntity(KeyCode.U.getName(), KeyTableEntry.U),
				new KeyTableEntity(KeyCode.I.getName(), KeyTableEntry.I),
				new KeyTableEntity(KeyCode.O.getName(), KeyTableEntry.O),
				new KeyTableEntity(KeyCode.P.getName(), KeyTableEntry.P),
				new KeyTableEntity(KeyCode.SEMICOLON.getName(), KeyTableEntry.AT),
				new KeyTableEntity(KeyCode.PLUS.getName(), KeyTableEntry.STAR),
				new KeyTableEntity(KeyCode.LESS.getName(), KeyTableEntry.ARROW_UP),

				new KeyTableEntity(KeyCode.ESCAPE.getName(), KeyTableEntry.RUN_STOP),
				new KeyTableEntity(KeyCode.A.getName(), KeyTableEntry.A),
				new KeyTableEntity(KeyCode.S.getName(), KeyTableEntry.S),
				new KeyTableEntity(KeyCode.D.getName(), KeyTableEntry.D),
				new KeyTableEntity(KeyCode.F.getName(), KeyTableEntry.F),
				new KeyTableEntity(KeyCode.G.getName(), KeyTableEntry.G),
				new KeyTableEntity(KeyCode.H.getName(), KeyTableEntry.H),
				new KeyTableEntity(KeyCode.J.getName(), KeyTableEntry.J),
				new KeyTableEntity(KeyCode.K.getName(), KeyTableEntry.K),
				new KeyTableEntity(KeyCode.L.getName(), KeyTableEntry.L),
				new KeyTableEntity(KeyCode.BACK_QUOTE.getName(), KeyTableEntry.COLON),
				new KeyTableEntity(KeyCode.QUOTE.getName(), KeyTableEntry.SEMICOLON),
				new KeyTableEntity(KeyCode.SLASH.getName(), KeyTableEntry.EQUALS),
				new KeyTableEntity(KeyCode.ENTER.getName(), KeyTableEntry.RETURN),

				new KeyTableEntity(KeyCode.Z.getName(), KeyTableEntry.Z),
				new KeyTableEntity(KeyCode.X.getName(), KeyTableEntry.X),
				new KeyTableEntity(KeyCode.C.getName(), KeyTableEntry.C),
				new KeyTableEntity(KeyCode.V.getName(), KeyTableEntry.V),
				new KeyTableEntity(KeyCode.B.getName(), KeyTableEntry.B),
				new KeyTableEntity(KeyCode.N.getName(), KeyTableEntry.N),
				new KeyTableEntity(KeyCode.M.getName(), KeyTableEntry.M),
				new KeyTableEntity(KeyCode.COMMA.getName(), KeyTableEntry.COMMA),
				new KeyTableEntity(KeyCode.PERIOD.getName(), KeyTableEntry.PERIOD),
				new KeyTableEntity(KeyCode.MINUS.getName(), KeyTableEntry.SLASH),
				new KeyTableEntity(KeyCode.DOWN.getName(), KeyTableEntry.CURSOR_UP_DOWN),
				new KeyTableEntity(KeyCode.UP.getName(), KeyTableEntry.CURSOR_UP_DOWN),
				new KeyTableEntity(KeyCode.RIGHT.getName(), KeyTableEntry.CURSOR_LEFT_RIGHT),
				new KeyTableEntity(KeyCode.LEFT.getName(), KeyTableEntry.CURSOR_LEFT_RIGHT),

				new KeyTableEntity(KeyCode.SPACE.getName(), KeyTableEntry.SPACE),

				new KeyTableEntity(KeyCode.F1.getName(), KeyTableEntry.F1),
				new KeyTableEntity(KeyCode.F3.getName(), KeyTableEntry.F3),
				new KeyTableEntity(KeyCode.F5.getName(), KeyTableEntry.F5),
				new KeyTableEntity(KeyCode.F7.getName(), KeyTableEntry.F7)));
	}

	private final List<FavoritesSection> INITIAL_FAVORITES;
	{
		INITIAL_FAVORITES = new ArrayList<>(Arrays.asList(new FavoritesSection()));
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private SidPlay2Section sidplay2 = new SidPlay2Section();

	public void setSidplay2Section(SidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
	}

	@Embedded
	@Override
	public SidPlay2Section getSidplay2Section() {
		return sidplay2;
	}

	private OnlineSection online = new OnlineSection();

	@Embedded
	public OnlineSection getOnlineSection() {
		return online;
	}

	public void setOnlineSection(OnlineSection online) {
		this.online = online;
	}

	private C1541Section c1541 = new C1541Section();

	public void setC1541Section(C1541Section c1541) {
		this.c1541 = c1541;
	}

	@Override
	@Embedded
	public C1541Section getC1541Section() {
		return c1541;
	}

	private PrinterSection printer = new PrinterSection();

	public void setPrinterSection(PrinterSection printer) {
		this.printer = printer;
	}

	@Embedded
	@Override
	public PrinterSection getPrinterSection() {
		return printer;
	}

	private JoystickSection joystickSection = new JoystickSection();

	public void setJoystickSection(JoystickSection joystick) {
		this.joystickSection = joystick;
	}

	@Embedded
	public JoystickSection getJoystickSection() {
		return joystickSection;
	}

	private AudioSection audioSection = new AudioSection();

	public void setAudioSection(AudioSection audio) {
		this.audioSection = audio;
	}

	@Embedded
	@Override
	public AudioSection getAudioSection() {
		return audioSection;
	}

	@ParametersDelegate
	private EmulationSection emulationSection = new EmulationSection();

	public void setEmulationSection(EmulationSection emulation) {
		this.emulationSection = emulation;
	}

	@Embedded
	@Override
	public EmulationSection getEmulationSection() {
		return emulationSection;
	}

	@ParametersDelegate
	private WhatsSidSection whatsSidSection = new WhatsSidSection();

	public void setWhatsSidSection(WhatsSidSection whatsSidSection) {
		this.whatsSidSection = whatsSidSection;
	}

	@Embedded
	@Override
	public WhatsSidSection getWhatsSidSection() {
		return whatsSidSection;
	}

	private String currentFavorite;

	public void setCurrentFavorite(String currentFavorite) {
		this.currentFavorite = currentFavorite;
	}

	public String getCurrentFavorite() {
		return currentFavorite;
	}

	protected List<FavoritesSection> favorites = INITIAL_FAVORITES;

	private ObservableList<FavoritesSection> observableFavorites;

	public void setFavorites(List<FavoritesSection> favorites) {
		this.favorites = favorites;
	}

	@OneToMany(cascade = CascadeType.ALL)
	public List<FavoritesSection> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<>();
		}
		return getObservableFavorites();
	}

	@Transient
	public ObservableList<FavoritesSection> getObservableFavorites() {
		if (observableFavorites == null) {
			observableFavorites = FXCollections.<FavoritesSection>observableArrayList(favorites);
			Bindings.bindContent(favorites, observableFavorites);
		}
		return observableFavorites;
	}

	private Assembly64Section assembly64Section = new Assembly64Section();

	public void setAssembly64Section(Assembly64Section assembly64Section) {
		this.assembly64Section = assembly64Section;
	}

	@Embedded
	public Assembly64Section getAssembly64Section() {
		return assembly64Section;
	}

	protected List<ViewEntity> views = INITIAL_VIEWS;

	private ObservableList<ViewEntity> observableViews;

	@OneToMany(cascade = CascadeType.ALL)
	public List<ViewEntity> getViews() {
		if (observableViews == null) {
			observableViews = FXCollections.<ViewEntity>observableArrayList(views);
			Bindings.bindContent(views, observableViews);
		}
		return observableViews;
	}

	public void setViews(List<ViewEntity> views) {
		this.views = views;
	}

	private List<FilterSection> filter = INITIAL_FILTERS;

	public void setFilterSection(List<FilterSection> filter) {
		this.filter = filter;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@Override
	public List<FilterSection> getFilterSection() {
		if (filter == null) {
			filter = new ArrayList<>();
		}
		return filter;
	}

	private List<KeyTableEntity> keyCodeMap = INITIAL_KEYCODES;

	public void setKeyCodeMap(List<KeyTableEntity> keyCodeMap) {
		this.keyCodeMap = keyCodeMap;
	}

	@OneToMany(cascade = CascadeType.ALL)
	public List<KeyTableEntity> getKeyCodeMap() {
		return keyCodeMap;
	}

	public KeyTableEntry getKeyTabEntry(String key) {
		for (KeyTableEntity keyCode : keyCodeMap) {
			if (keyCode.getKeyCodeName().equals(key)) {
				return keyCode.getEntry();
			}
		}
		return null;
	}

}
