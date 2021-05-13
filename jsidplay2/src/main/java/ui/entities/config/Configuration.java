package ui.entities.config;

import static java.util.stream.Collectors.toList;
import static libsidplay.components.keyboard.KeyTableEntry.A;
import static libsidplay.components.keyboard.KeyTableEntry.ARROW_LEFT;
import static libsidplay.components.keyboard.KeyTableEntry.ARROW_UP;
import static libsidplay.components.keyboard.KeyTableEntry.AT;
import static libsidplay.components.keyboard.KeyTableEntry.B;
import static libsidplay.components.keyboard.KeyTableEntry.C;
import static libsidplay.components.keyboard.KeyTableEntry.CLEAR_HOME;
import static libsidplay.components.keyboard.KeyTableEntry.COLON;
import static libsidplay.components.keyboard.KeyTableEntry.COMMA;
import static libsidplay.components.keyboard.KeyTableEntry.CURSOR_LEFT_RIGHT;
import static libsidplay.components.keyboard.KeyTableEntry.CURSOR_UP_DOWN;
import static libsidplay.components.keyboard.KeyTableEntry.D;
import static libsidplay.components.keyboard.KeyTableEntry.E;
import static libsidplay.components.keyboard.KeyTableEntry.EIGHT;
import static libsidplay.components.keyboard.KeyTableEntry.EQUALS;
import static libsidplay.components.keyboard.KeyTableEntry.F;
import static libsidplay.components.keyboard.KeyTableEntry.F1;
import static libsidplay.components.keyboard.KeyTableEntry.F3;
import static libsidplay.components.keyboard.KeyTableEntry.F5;
import static libsidplay.components.keyboard.KeyTableEntry.F7;
import static libsidplay.components.keyboard.KeyTableEntry.FIVE;
import static libsidplay.components.keyboard.KeyTableEntry.FOUR;
import static libsidplay.components.keyboard.KeyTableEntry.G;
import static libsidplay.components.keyboard.KeyTableEntry.H;
import static libsidplay.components.keyboard.KeyTableEntry.I;
import static libsidplay.components.keyboard.KeyTableEntry.INS_DEL;
import static libsidplay.components.keyboard.KeyTableEntry.J;
import static libsidplay.components.keyboard.KeyTableEntry.K;
import static libsidplay.components.keyboard.KeyTableEntry.L;
import static libsidplay.components.keyboard.KeyTableEntry.M;
import static libsidplay.components.keyboard.KeyTableEntry.MINUS;
import static libsidplay.components.keyboard.KeyTableEntry.N;
import static libsidplay.components.keyboard.KeyTableEntry.NINE;
import static libsidplay.components.keyboard.KeyTableEntry.O;
import static libsidplay.components.keyboard.KeyTableEntry.ONE;
import static libsidplay.components.keyboard.KeyTableEntry.P;
import static libsidplay.components.keyboard.KeyTableEntry.PERIOD;
import static libsidplay.components.keyboard.KeyTableEntry.PLUS;
import static libsidplay.components.keyboard.KeyTableEntry.POUND;
import static libsidplay.components.keyboard.KeyTableEntry.Q;
import static libsidplay.components.keyboard.KeyTableEntry.R;
import static libsidplay.components.keyboard.KeyTableEntry.RETURN;
import static libsidplay.components.keyboard.KeyTableEntry.RUN_STOP;
import static libsidplay.components.keyboard.KeyTableEntry.S;
import static libsidplay.components.keyboard.KeyTableEntry.SEMICOLON;
import static libsidplay.components.keyboard.KeyTableEntry.SEVEN;
import static libsidplay.components.keyboard.KeyTableEntry.SIX;
import static libsidplay.components.keyboard.KeyTableEntry.SLASH;
import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static libsidplay.components.keyboard.KeyTableEntry.STAR;
import static libsidplay.components.keyboard.KeyTableEntry.T;
import static libsidplay.components.keyboard.KeyTableEntry.THREE;
import static libsidplay.components.keyboard.KeyTableEntry.TWO;
import static libsidplay.components.keyboard.KeyTableEntry.U;
import static libsidplay.components.keyboard.KeyTableEntry.V;
import static libsidplay.components.keyboard.KeyTableEntry.W;
import static libsidplay.components.keyboard.KeyTableEntry.X;
import static libsidplay.components.keyboard.KeyTableEntry.Y;
import static libsidplay.components.keyboard.KeyTableEntry.Z;
import static libsidplay.components.keyboard.KeyTableEntry.ZERO;
import static sidplay.ini.IniDefaults.DEFAULTS;
import static ui.entities.config.KeyTableEntity.of;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.KeyCode;
import libsidplay.config.IConfig;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.LazyListField;
import ui.common.properties.ObservableLazyListField;
import ui.common.properties.ShadowField;
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

	public static final List<FilterSection> DEFAULT_FILTERS = DEFAULTS.getFilterSection().stream()
			.map(FilterSection::new).collect(toList());

	public static final List<ViewEntity> DEFAULT_VIEWS = Arrays.asList(new ViewEntity(Console.ID),
			new ViewEntity(Video.ID));

	public static final List<FavoritesSection> DEFAULT_FAVORITES = Arrays.asList(new FavoritesSection());

	public static final List<KeyTableEntity> DEFAULT_KEYCODES = Arrays.asList(of(KeyCode.A.getName(), A),
			of(KeyCode.BACK_SLASH.getName(), ARROW_LEFT), of(KeyCode.DIGIT1.getName(), ONE),
			of(KeyCode.DIGIT2.getName(), TWO), of(KeyCode.DIGIT3.getName(), THREE), of(KeyCode.DIGIT4.getName(), FOUR),
			of(KeyCode.DIGIT5.getName(), FIVE), of(KeyCode.DIGIT6.getName(), SIX), of(KeyCode.DIGIT7.getName(), SEVEN),
			of(KeyCode.DIGIT8.getName(), EIGHT), of(KeyCode.DIGIT9.getName(), NINE), of(KeyCode.DIGIT0.getName(), ZERO),
			of(KeyCode.OPEN_BRACKET.getName(), PLUS), of(KeyCode.CLOSE_BRACKET.getName(), MINUS),
			of(KeyCode.POUND.getName(), POUND), of(KeyCode.HOME.getName(), CLEAR_HOME),
			of(KeyCode.BACK_SPACE.getName(), INS_DEL),

			of(KeyCode.Q.getName(), Q), of(KeyCode.W.getName(), W), of(KeyCode.E.getName(), E),
			of(KeyCode.R.getName(), R), of(KeyCode.T.getName(), T), of(KeyCode.Y.getName(), Y),
			of(KeyCode.U.getName(), U), of(KeyCode.I.getName(), I), of(KeyCode.O.getName(), O),
			of(KeyCode.P.getName(), P), of(KeyCode.SEMICOLON.getName(), AT), of(KeyCode.PLUS.getName(), STAR),
			of(KeyCode.LESS.getName(), ARROW_UP),

			of(KeyCode.ESCAPE.getName(), RUN_STOP), of(KeyCode.A.getName(), A), of(KeyCode.S.getName(), S),
			of(KeyCode.D.getName(), D), of(KeyCode.F.getName(), F), of(KeyCode.G.getName(), G),
			of(KeyCode.H.getName(), H), of(KeyCode.J.getName(), J), of(KeyCode.K.getName(), K),
			of(KeyCode.L.getName(), L), of(KeyCode.BACK_QUOTE.getName(), COLON), of(KeyCode.QUOTE.getName(), SEMICOLON),
			of(KeyCode.SLASH.getName(), EQUALS), of(KeyCode.ENTER.getName(), RETURN),

			of(KeyCode.Z.getName(), Z), of(KeyCode.X.getName(), X), of(KeyCode.C.getName(), C),
			of(KeyCode.V.getName(), V), of(KeyCode.B.getName(), B), of(KeyCode.N.getName(), N),
			of(KeyCode.M.getName(), M), of(KeyCode.COMMA.getName(), COMMA), of(KeyCode.PERIOD.getName(), PERIOD),
			of(KeyCode.MINUS.getName(), SLASH), of(KeyCode.DOWN.getName(), CURSOR_UP_DOWN),
			of(KeyCode.UP.getName(), CURSOR_UP_DOWN), of(KeyCode.RIGHT.getName(), CURSOR_LEFT_RIGHT),
			of(KeyCode.LEFT.getName(), CURSOR_LEFT_RIGHT),

			of(KeyCode.SPACE.getName(), SPACE),

			of(KeyCode.F1.getName(), F1), of(KeyCode.F3.getName(), F3), of(KeyCode.F5.getName(), F5),
			of(KeyCode.F7.getName(), F7));

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	@JsonIgnore
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private SidPlay2Section sidplay2 = new SidPlay2Section();

	@Embedded
	@Override
	public SidPlay2Section getSidplay2Section() {
		return sidplay2;
	}

	public void setSidplay2Section(SidPlay2Section sidplay2) {
		this.sidplay2 = sidplay2;
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

	@Override
	@Embedded
	public C1541Section getC1541Section() {
		return c1541;
	}

	public void setC1541Section(C1541Section c1541) {
		this.c1541 = c1541;
	}

	private PrinterSection printer = new PrinterSection();

	@Embedded
	@Override
	public PrinterSection getPrinterSection() {
		return printer;
	}

	public void setPrinterSection(PrinterSection printer) {
		this.printer = printer;
	}

	private JoystickSection joystickSection = new JoystickSection();

	@Embedded
	public JoystickSection getJoystickSection() {
		return joystickSection;
	}

	public void setJoystickSection(JoystickSection joystick) {
		this.joystickSection = joystick;
	}

	private AudioSection audioSection = new AudioSection();

	@Embedded
	@Override
	public AudioSection getAudioSection() {
		return audioSection;
	}

	public void setAudioSection(AudioSection audio) {
		this.audioSection = audio;
	}

	@ParametersDelegate
	private EmulationSection emulationSection = new EmulationSection();

	@Embedded
	@Override
	public EmulationSection getEmulationSection() {
		return emulationSection;
	}

	public void setEmulationSection(EmulationSection emulation) {
		this.emulationSection = emulation;
	}

	@ParametersDelegate
	private WhatsSidSection whatsSidSection = new WhatsSidSection();

	@Embedded
	@Override
	public WhatsSidSection getWhatsSidSection() {
		return whatsSidSection;
	}

	public void setWhatsSidSection(WhatsSidSection whatsSidSection) {
		this.whatsSidSection = whatsSidSection;
	}

	private ShadowField<StringProperty, String> currentFavorite = new ShadowField<>(SimpleStringProperty::new, null);

	public String getCurrentFavorite() {
		return currentFavorite.get();
	}

	public void setCurrentFavorite(String currentFavorite) {
		this.currentFavorite.set(currentFavorite);
	}

	public StringProperty currentFavoriteProperty() {
		return currentFavorite.property();
	}

	private ObservableLazyListField<FavoritesSection> favorites = new ObservableLazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<FavoritesSection> getFavorites() {
		return favorites.get(() -> new ArrayList<>(DEFAULT_FAVORITES));
	}

	public void setFavorites(List<FavoritesSection> favorites) {
		this.favorites.set(favorites);
	}

	private Assembly64Section assembly64Section = new Assembly64Section();

	@Embedded
	public Assembly64Section getAssembly64Section() {
		return assembly64Section;
	}

	public void setAssembly64Section(Assembly64Section assembly64Section) {
		this.assembly64Section = assembly64Section;
	}

	private ObservableLazyListField<ViewEntity> views = new ObservableLazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<ViewEntity> getViews() {
		return views.get(() -> new ArrayList<>(DEFAULT_VIEWS));
	}

	public void setViews(List<ViewEntity> views) {
		this.views.set(views);
	}

	private LazyListField<FilterSection> filter = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	@Override
	public List<FilterSection> getFilterSection() {
		return filter.get(() -> DEFAULT_FILTERS.stream().map(FilterSection::new).collect(toList()));
	}

	public void setFilterSection(List<FilterSection> filter) {
		this.filter.set(filter);
	}

	private LazyListField<KeyTableEntity> keyCodeMap = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<KeyTableEntity> getKeyCodeMap() {
		return keyCodeMap.get(() -> DEFAULT_KEYCODES.stream().map(KeyTableEntity::new).collect(toList()));
	}

	public void setKeyCodeMap(List<KeyTableEntity> keyCodeMap) {
		this.keyCodeMap.set(keyCodeMap);
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
