package ui.entities.config;

import static server.restful.common.Connectors.HTTP;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_DIGI_BOOSTED_8580;
import static sidplay.ini.IniDefaults.DEFAULT_DUAL_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_ENGINE;
import static sidplay.ini.IniDefaults.DEFAULT_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_6581;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_8580;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_0;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_1;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_NUM_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.config.IEmulationSection;
import server.restful.common.Connectors;
import ui.common.FileToStringConverter;

@Embeddable
@Parameters(resourceBundle = "ui.entities.config.EmulationSection")
public class EmulationSection implements IEmulationSection {

	public static final boolean DEFAULT_DETECT_PSID64_CHIP_MODEL = true;

	/**
	 * Connection types of the built-in Android App Server providing REST-based
	 * web-services to play SIDs on the mobile.
	 */
	public static final Connectors DEFAULT_CONNECTORS = HTTP;
	/**
	 * Port of the built-in Android App Server providing REST-based web-services to
	 * play SIDs on the mobile.
	 */
	public static final int DEFAULT_APP_SERVER_PORT = 8080;
	/**
	 * Secure port of the built-in Android App Server providing REST-based
	 * web-services to play SIDs on the mobile.
	 */
	public static final int DEFAULT_APP_SERVER_SECURE_PORT = 8443;

	public EmulationSection() {
		Bindings.bindBidirectional(this.appServerKeystore, appServerKeystoreFile, new FileToStringConverter());
	}

	private ObjectProperty<Engine> engine = new SimpleObjectProperty<Engine>(DEFAULT_ENGINE);

	public ObjectProperty<Engine> engineProperty() {
		return engine;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Engine getEngine() {
		return this.engine.get();
	}

	@Override
	public void setEngine(Engine engine) {
		this.engine.set(engine);
	}

	private ObjectProperty<Emulation> defaultEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_EMULATION);

	public ObjectProperty<Emulation> defaultEmulationProperty() {
		return defaultEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getDefaultEmulation() {
		return this.defaultEmulation.get();
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
		this.defaultEmulation.set(emulation);
	}

	private ObjectProperty<Emulation> userEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_USER_EMULATION);

	public ObjectProperty<Emulation> userEmulationProperty() {
		return userEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getUserEmulation() {
		return this.userEmulation.get();
	}

	@Override
	public void setUserEmulation(Emulation userEmulation) {
		this.userEmulation.set(userEmulation);
	}

	private ObjectProperty<Emulation> stereoEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_STEREO_EMULATION);

	public ObjectProperty<Emulation> stereoEmulationProperty() {
		return stereoEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getStereoEmulation() {
		return this.stereoEmulation.get();
	}

	@Override
	public void setStereoEmulation(Emulation stereoEmulation) {
		this.stereoEmulation.set(stereoEmulation);
	}

	private ObjectProperty<Emulation> thirdEmulation = new SimpleObjectProperty<Emulation>(DEFAULT_3SID_EMULATION);

	public ObjectProperty<Emulation> thirdEmulationProperty() {
		return thirdEmulation;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getThirdEmulation() {
		return this.thirdEmulation.get();
	}

	@Override
	public void setThirdEmulation(Emulation thirdEmulation) {
		this.thirdEmulation.set(thirdEmulation);
	}

	private ObjectProperty<CPUClock> defaultClockSpeed = new SimpleObjectProperty<CPUClock>(DEFAULT_CLOCK_SPEED);

	public ObjectProperty<CPUClock> defaultClockSpeedProperty() {
		return defaultClockSpeed;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getDefaultClockSpeed() {
		return this.defaultClockSpeed.get();
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
		this.defaultClockSpeed.set(speed);
	}

	private ObjectProperty<CPUClock> userClockSpeed = new SimpleObjectProperty<CPUClock>();

	public ObjectProperty<CPUClock> userClockSpeedProperty() {
		return userClockSpeed;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getUserClockSpeed() {
		return userClockSpeed.get();
	}

	@Override
	public void setUserClockSpeed(CPUClock userClockSpeed) {
		this.userClockSpeed.set(userClockSpeed);
	}

	private ObjectProperty<ChipModel> defaultSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_SID_MODEL);

	public ObjectProperty<ChipModel> defaultSidModelProperty() {
		return defaultSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getDefaultSidModel() {
		return defaultSidModel.get();
	}

	@Override
	public void setDefaultSidModel(ChipModel defaultSidModel) {
		this.defaultSidModel.set(defaultSidModel);
	}

	private BooleanProperty detectPSID64ChipModel = new SimpleBooleanProperty(DEFAULT_DETECT_PSID64_CHIP_MODEL);

	public boolean isDetectPSID64ChipModel() {
		return detectPSID64ChipModel.get();
	}

	public void setDetectPSID64ChipModel(boolean detectPSID64ChipModel) {
		this.detectPSID64ChipModel.set(detectPSID64ChipModel);
	}

	public BooleanProperty detectPSID64ChipModelProperty() {
		return detectPSID64ChipModel;
	}

	private ObjectProperty<ChipModel> userSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_USER_MODEL);

	public ObjectProperty<ChipModel> userSidModelProperty() {
		return userSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getUserSidModel() {
		return userSidModel.get();
	}

	@Override
	public void setUserSidModel(ChipModel userSidModel) {
		this.userSidModel.set(userSidModel);
	}

	private ObjectProperty<ChipModel> stereoSidModel = new SimpleObjectProperty<ChipModel>(DEFAULT_STEREO_MODEL);

	public ObjectProperty<ChipModel> stereoSidModelProperty() {
		return stereoSidModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getStereoSidModel() {
		return stereoSidModel.get();
	}

	@Override
	public void setStereoSidModel(ChipModel stereoSidModel) {
		this.stereoSidModel.set(stereoSidModel);
	}

	private ObjectProperty<ChipModel> thirdSIDModel = new SimpleObjectProperty<ChipModel>(DEFAULT_3SID_MODEL);

	public ObjectProperty<ChipModel> thirdSIDModelProperty() {
		return thirdSIDModel;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getThirdSIDModel() {
		return thirdSIDModel.get();
	}

	@Override
	public void setThirdSIDModel(ChipModel stereoSidModel) {
		this.thirdSIDModel.set(stereoSidModel);
	}

	private ObjectProperty<Integer> hardsid6581 = new SimpleObjectProperty<Integer>(DEFAULT_HARD_SID_6581);

	public ObjectProperty<Integer> hardsid6581Property() {
		return hardsid6581;
	}

	@Override
	public int getHardsid6581() {
		return hardsid6581.get();
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581.set(hardsid6581);
	}

	private ObjectProperty<Integer> hardsid8580 = new SimpleObjectProperty<Integer>(DEFAULT_HARD_SID_8580);

	public ObjectProperty<Integer> hardsid8580Property() {
		return hardsid8580;
	}

	@Override
	public int getHardsid8580() {
		return hardsid8580.get();
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580.set(hardsid8580);
	}

	private ObjectProperty<ChipModel> sidBlaster0ModelProperty = new SimpleObjectProperty<ChipModel>(DEFAULT_SIDBLASTER_0);

	public ObjectProperty<ChipModel> sidBlaster0ModelProperty() {
		return sidBlaster0ModelProperty;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getSidBlaster0Model() {
		return sidBlaster0ModelProperty.get();
	}

	@Override
	public void setSidBlaster0Model(ChipModel sidBlaster0Model) {
		this.sidBlaster0ModelProperty.set(sidBlaster0Model);
	}

	private ObjectProperty<ChipModel> sidBlaster1ModelProperty = new SimpleObjectProperty<ChipModel>(DEFAULT_SIDBLASTER_1);

	public ObjectProperty<ChipModel> sidBlaster1ModelProperty() {
		return sidBlaster1ModelProperty;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getSidBlaster1Model() {
		return sidBlaster1ModelProperty.get();
	}

	@Override
	public void setSidBlaster1Model(ChipModel sidBlaster1Model) {
		this.sidBlaster1ModelProperty.set(sidBlaster1Model);
	}

	private ObjectProperty<ChipModel> sidBlaster2ModelProperty = new SimpleObjectProperty<ChipModel>(DEFAULT_SIDBLASTER_2);

	public ObjectProperty<ChipModel> sidBlaster2ModelProperty() {
		return sidBlaster2ModelProperty;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getSidBlaster2Model() {
		return sidBlaster2ModelProperty.get();
	}

	@Override
	public void setSidBlaster2Model(ChipModel sidBlaster2Model) {
		this.sidBlaster2ModelProperty.set(sidBlaster2Model);
	}

	private StringProperty netSidDevHostProperty = new SimpleStringProperty(DEFAULT_NETSIDDEV_HOST);

	public StringProperty netSidDevHostProperty() {
		return netSidDevHostProperty;
	}

	@Override
	public String getNetSIDDevHost() {
		return netSidDevHostProperty.get();
	}

	@Override
	public void setNetSIDDevHost(String hostname) {
		this.netSidDevHostProperty.set(hostname);
	}

	private ObjectProperty<Integer> netSidDevPortProperty = new SimpleObjectProperty<Integer>(DEFAULT_NETSIDDEV_PORT);

	public ObjectProperty<Integer> netSidDevPortProperty() {
		return netSidDevPortProperty;
	}

	@Override
	public int getNetSIDDevPort() {
		return netSidDevPortProperty.get();
	}

	@Override
	public void setNetSIDDevPort(int port) {
		this.netSidDevPortProperty.set(port);
	}

	private ObjectProperty<Connectors> appServerConnectors = new SimpleObjectProperty<Connectors>(DEFAULT_CONNECTORS);

	public ObjectProperty<Connectors> appServerConnectorsProperty() {
		return appServerConnectors;
	}

	@Enumerated(EnumType.STRING)
	public Connectors getAppServerConnectors() {
		return this.appServerConnectors.get();
	}

	@Parameter(names = { "--appServerConnectors" }, descriptionKey = "APP_SERVER_CONNECTORS")
	public void setAppServerConnectors(Connectors appServerConnectors) {
		this.appServerConnectors.set(appServerConnectors);
	}

	private ObjectProperty<Integer> appServerPortProperty = new SimpleObjectProperty<Integer>(DEFAULT_APP_SERVER_PORT);

	public ObjectProperty<Integer> appServerPortProperty() {
		return appServerPortProperty;
	}

	public int getAppServerPort() {
		return appServerPortProperty.get();
	}

	@Parameter(names = { "--appServerPort" }, descriptionKey = "APP_SERVER_PORT")
	public void setAppServerPort(int port) {
		this.appServerPortProperty.set(port);
	}

	private ObjectProperty<Integer> appServerSecurePortProperty = new SimpleObjectProperty<Integer>(
			DEFAULT_APP_SERVER_SECURE_PORT);

	public ObjectProperty<Integer> appServerSecurePortProperty() {
		return appServerSecurePortProperty;
	}

	public int getAppServerSecurePort() {
		return appServerSecurePortProperty.get();
	}

	@Parameter(names = { "--appServerSecurePort" }, descriptionKey = "APP_SERVER_SECURE_PORT")
	public void setAppServerSecurePort(int securePort) {
		this.appServerSecurePortProperty.set(securePort);
	}

	private ObjectProperty<File> appServerKeystoreFile = new SimpleObjectProperty<File>();
	private StringProperty appServerKeystore = new SimpleStringProperty();

	public ObjectProperty<File> appServerKeystoreFileProperty() {
		return appServerKeystoreFile;
	}

	public String getAppServerKeystoreFile() {
		return this.appServerKeystore.get();
	}

	@Parameter(names = { "--appServerKeystore" }, descriptionKey = "APP_SERVER_KEYSTORE")
	public void setAppServerKeystoreFile(String appServerKeystoreFile) {
		this.appServerKeystore.set(appServerKeystoreFile);
	}

	private StringProperty appServerKeyManagerPasswordProperty = new SimpleStringProperty();

	public StringProperty appServerKeyManagerPasswordProperty() {
		return appServerKeyManagerPasswordProperty;
	}

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	public String getAppServerKeyManagerPassword() {
		return appServerKeyManagerPasswordProperty.get();
	}

	@Parameter(names = { "--appServerManagerPassword" }, descriptionKey = "APP_SERVER_MANAGER_PASSWORD")
	public void setAppServerKeyManagerPassword(String appServerKeyManagerPassword) {
		this.appServerKeyManagerPasswordProperty.set(appServerKeyManagerPassword);
	}

	private StringProperty appServerKeystorePasswordProperty = new SimpleStringProperty();

	public StringProperty appServerKeystorePasswordProperty() {
		return appServerKeystorePasswordProperty;
	}

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	public String getAppServerKeystorePassword() {
		return appServerKeystorePasswordProperty.get();
	}

	@Parameter(names = { "--appServerKeystorePassword" }, descriptionKey = "APP_SERVER_KEYSTORE_PASSWORD")
	public void setAppServerKeystorePassword(String appServerKeyStorePassword) {
		this.appServerKeystorePasswordProperty.set(appServerKeyStorePassword);
	}

	private BooleanProperty filter = new SimpleBooleanProperty(DEFAULT_USE_FILTER);

	public BooleanProperty filterProperty() {
		return filter;
	}

	@Override
	public boolean isFilter() {
		return filter.get();
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter.set(isFilter);
	}

	private BooleanProperty stereoFilter = new SimpleBooleanProperty(DEFAULT_USE_STEREO_FILTER);

	public BooleanProperty stereoFilterProperty() {
		return stereoFilter;
	}

	@Override
	public boolean isStereoFilter() {
		return stereoFilter.get();
	}

	@Override
	public void setStereoFilter(boolean isFilter) {
		this.stereoFilter.set(isFilter);
	}

	private BooleanProperty thirdSIDFilter = new SimpleBooleanProperty(DEFAULT_USE_3SID_FILTER);

	public BooleanProperty thirdSIDFilterProperty() {
		return thirdSIDFilter;
	}

	@Override
	public boolean isThirdSIDFilter() {
		return thirdSIDFilter.get();
	}

	@Override
	public void setThirdSIDFilter(boolean isFilter) {
		this.thirdSIDFilter.set(isFilter);
	}

	private ObjectProperty<Integer> sidNumToRead = new SimpleObjectProperty<Integer>(DEFAULT_SID_NUM_TO_READ);

	public ObjectProperty<Integer> sidNumToReadProperty() {
		return sidNumToRead;
	}

	@Override
	public int getSidNumToRead() {
		return sidNumToRead.get();
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		this.sidNumToRead.set(sidNumToRead);
	}

	private BooleanProperty digiBoosted8580 = new SimpleBooleanProperty(DEFAULT_DIGI_BOOSTED_8580);

	public BooleanProperty digiBoosted8580Property() {
		return digiBoosted8580;
	}

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580.get();
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580.set(isDigiBoosted8580);
	}

	private BooleanProperty fakeStereo = new SimpleBooleanProperty(DEFAULT_FAKE_STEREO);

	public BooleanProperty fakeStereoProperty() {
		return fakeStereo;
	}

	@Override
	public boolean isFakeStereo() {
		return fakeStereo.get();
	}

	public void setFakeStereo(boolean fakeStereo) {
		this.fakeStereo.set(fakeStereo);
	}

	private IntegerProperty dualSidBase = new SimpleIntegerProperty(DEFAULT_DUAL_SID_BASE);

	public IntegerProperty dualSidBaseProperty() {
		return dualSidBase;
	}

	@Override
	public int getDualSidBase() {
		return dualSidBase.get();
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase.set(dualSidBase);
	}

	private IntegerProperty thirdSIDBase = new SimpleIntegerProperty(DEFAULT_THIRD_SID_BASE);

	public IntegerProperty thirdSIDBaseProperty() {
		return thirdSIDBase;
	}

	@Override
	public int getThirdSIDBase() {
		return thirdSIDBase.get();
	}

	@Override
	public void setThirdSIDBase(int dualSidBase) {
		this.thirdSIDBase.set(dualSidBase);
	}

	private BooleanProperty forceStereoTune = new SimpleBooleanProperty(DEFAULT_FORCE_STEREO_TUNE);

	public BooleanProperty forceStereoTuneProperty() {
		return forceStereoTune;
	}

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune.get();
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune.set(isForceStereoTune);
	}

	private BooleanProperty force3SIDTune = new SimpleBooleanProperty(DEFAULT_FORCE_3SID_TUNE);

	public BooleanProperty force3SIDTuneProperty() {
		return force3SIDTune;
	}

	@Override
	public boolean isForce3SIDTune() {
		return force3SIDTune.get();
	}

	@Override
	public void setForce3SIDTune(boolean isForceStereoTune) {
		this.force3SIDTune.set(isForceStereoTune);
	}

	private BooleanProperty muteVoice1 = new SimpleBooleanProperty(DEFAULT_MUTE_VOICE1);

	public BooleanProperty muteVoice1Property() {
		return muteVoice1;
	}

	@Override
	public boolean isMuteVoice1() {
		return muteVoice1.get();
	}

	@Override
	public void setMuteVoice1(boolean mute) {
		muteVoice1.set(mute);
	}

	private BooleanProperty muteVoice2 = new SimpleBooleanProperty(DEFAULT_MUTE_VOICE2);

	public BooleanProperty muteVoice2Property() {
		return muteVoice2;
	}

	@Override
	public boolean isMuteVoice2() {
		return muteVoice2.get();
	}

	@Override
	public void setMuteVoice2(boolean mute) {
		muteVoice2.set(mute);
	}

	private BooleanProperty muteVoice3 = new SimpleBooleanProperty(DEFAULT_MUTE_VOICE3);

	public BooleanProperty muteVoice3Property() {
		return muteVoice3;
	}

	@Override
	public boolean isMuteVoice3() {
		return muteVoice3.get();
	}

	@Override
	public void setMuteVoice3(boolean mute) {
		muteVoice3.set(mute);
	}

	private BooleanProperty muteVoice4 = new SimpleBooleanProperty(DEFAULT_MUTE_VOICE4);

	public BooleanProperty muteVoice4Property() {
		return muteVoice4;
	}

	@Override
	public boolean isMuteVoice4() {
		return muteVoice4.get();
	}

	@Override
	public void setMuteVoice4(boolean mute) {
		muteVoice4.set(mute);
	}

	private BooleanProperty muteStereoVoice1 = new SimpleBooleanProperty(DEFAULT_MUTE_STEREO_VOICE1);

	public BooleanProperty muteStereoVoice1Property() {
		return muteStereoVoice1;
	}

	@Override
	public boolean isMuteStereoVoice1() {
		return muteStereoVoice1.get();
	}

	@Override
	public void setMuteStereoVoice1(boolean mute) {
		muteStereoVoice1.set(mute);
	}

	private BooleanProperty muteStereoVoice2 = new SimpleBooleanProperty(DEFAULT_MUTE_STEREO_VOICE2);

	public BooleanProperty muteStereoVoice2Property() {
		return muteStereoVoice2;
	}

	@Override
	public boolean isMuteStereoVoice2() {
		return muteStereoVoice2.get();
	}

	@Override
	public void setMuteStereoVoice2(boolean mute) {
		muteStereoVoice2.set(mute);
	}

	private BooleanProperty muteStereoVoice3 = new SimpleBooleanProperty(DEFAULT_MUTE_STEREO_VOICE3);

	public BooleanProperty muteStereoVoice3Property() {
		return muteStereoVoice3;
	}

	@Override
	public boolean isMuteStereoVoice3() {
		return muteStereoVoice3.get();
	}

	@Override
	public void setMuteStereoVoice3(boolean mute) {
		muteStereoVoice3.set(mute);
	}

	private BooleanProperty muteStereoVoice4 = new SimpleBooleanProperty(DEFAULT_MUTE_STEREO_VOICE4);

	public BooleanProperty muteStereoVoice4Property() {
		return muteStereoVoice4;
	}

	@Override
	public boolean isMuteStereoVoice4() {
		return muteStereoVoice4.get();
	}

	@Override
	public void setMuteStereoVoice4(boolean mute) {
		muteStereoVoice4.set(mute);
	}

	private BooleanProperty muteThirdSIDVoice1 = new SimpleBooleanProperty(DEFAULT_MUTE_THIRDSID_VOICE1);

	public BooleanProperty muteThirdSIDVoice1Property() {
		return muteThirdSIDVoice1;
	}

	@Override
	public boolean isMuteThirdSIDVoice1() {
		return muteThirdSIDVoice1.get();
	}

	@Override
	public void setMuteThirdSIDVoice1(boolean mute) {
		muteThirdSIDVoice1.set(mute);
	}

	private BooleanProperty muteThirdSIDVoice2 = new SimpleBooleanProperty(DEFAULT_MUTE_THIRDSID_VOICE2);

	public BooleanProperty muteThirdSIDVoice2Property() {
		return muteThirdSIDVoice2;
	}

	@Override
	public boolean isMuteThirdSIDVoice2() {
		return muteThirdSIDVoice2.get();
	}

	@Override
	public void setMuteThirdSIDVoice2(boolean mute) {
		muteThirdSIDVoice2.set(mute);
	}

	private BooleanProperty muteThirdSIDVoice3 = new SimpleBooleanProperty(DEFAULT_MUTE_THIRDSID_VOICE3);

	public BooleanProperty muteThirdSIDVoice3Property() {
		return muteThirdSIDVoice3;
	}

	@Override
	public boolean isMuteThirdSIDVoice3() {
		return muteThirdSIDVoice3.get();
	}

	@Override
	public void setMuteThirdSIDVoice3(boolean mute) {
		muteThirdSIDVoice3.set(mute);
	}

	private BooleanProperty muteThirdSIDVoice4 = new SimpleBooleanProperty(DEFAULT_MUTE_THIRDSID_VOICE4);

	public BooleanProperty muteThirdSIDVoice4Property() {
		return muteThirdSIDVoice4;
	}

	@Override
	public boolean isMuteThirdSIDVoice4() {
		return muteThirdSIDVoice4.get();
	}

	@Override
	public void setMuteThirdSIDVoice4(boolean mute) {
		muteStereoVoice4.set(mute);
	}

	private StringProperty netSIDFilter6581 = new SimpleStringProperty(DEFAULT_NETSID_FILTER_6581);

	public StringProperty netSIDFilter6581Property() {
		return netSIDFilter6581;
	}

	@Override
	public String getNetSIDFilter6581() {
		return netSIDFilter6581.get();
	}

	@Override
	public void setNetSIDFilter6581(String netSIDFilter6581) {
		this.netSIDFilter6581.set(netSIDFilter6581);
	}

	private StringProperty netSIDStereoFilter6581 = new SimpleStringProperty(DEFAULT_NETSID_STEREO_FILTER_6581);

	public StringProperty netSIDStereoFilter6581Property() {
		return netSIDStereoFilter6581;
	}

	@Override
	public String getNetSIDStereoFilter6581() {
		return netSIDStereoFilter6581.get();
	}

	@Override
	public void setNetSIDStereoFilter6581(String netSIDFilter6581) {
		this.netSIDStereoFilter6581.set(netSIDFilter6581);
	}

	private StringProperty netSID3rdSIDFilter6581 = new SimpleStringProperty(DEFAULT_NETSID_3SID_FILTER_6581);

	public StringProperty netSID3rdSIDFilter6581Property() {
		return netSID3rdSIDFilter6581;
	}

	@Override
	public String getNetSIDThirdSIDFilter6581() {
		return netSID3rdSIDFilter6581.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter6581(String netSIDFilter6581) {
		this.netSID3rdSIDFilter6581.set(netSIDFilter6581);
	}

	private StringProperty netSIDFilter8580 = new SimpleStringProperty(DEFAULT_NETSID_FILTER_8580);

	public StringProperty netSIDFilter8580Property() {
		return netSIDFilter8580;
	}

	@Override
	public String getNetSIDFilter8580() {
		return netSIDFilter8580.get();
	}

	@Override
	public void setNetSIDFilter8580(String netSIDFilter8580) {
		this.netSIDFilter8580.set(netSIDFilter8580);
	}

	private StringProperty netSIDStereoFilter8580 = new SimpleStringProperty(DEFAULT_NETSID_STEREO_FILTER_8580);

	public StringProperty netSIDStereoFilter858Property() {
		return netSIDStereoFilter8580;
	}

	@Override
	public String getNetSIDStereoFilter8580() {
		return netSIDStereoFilter8580.get();
	}

	@Override
	public void setNetSIDStereoFilter8580(String netSIDFilter8580) {
		this.netSIDStereoFilter8580.set(netSIDFilter8580);
	}

	private StringProperty netSID3rdSIDFilter8580 = new SimpleStringProperty(DEFAULT_NETSID_3SID_FILTER_8580);

	public StringProperty netSID3rdSIDFilter8580Property() {
		return netSID3rdSIDFilter8580;
	}

	@Override
	public String getNetSIDThirdSIDFilter8580() {
		return netSID3rdSIDFilter8580.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter8580(String netSIDFilter8580) {
		this.netSID3rdSIDFilter8580.set(netSIDFilter8580);
	}

	private StringProperty filter6581 = new SimpleStringProperty(DEFAULT_FILTER_6581);

	public StringProperty filter6581Property() {
		return filter6581;
	}

	@Override
	public String getFilter6581() {
		return filter6581.get();
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581.set(filter6581);
	}

	private StringProperty stereoFilter6581 = new SimpleStringProperty(DEFAULT_STEREO_FILTER_6581);

	public StringProperty stereoFilter6581Property() {
		return stereoFilter6581;
	}

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581.get();
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581.set(filter6581);
	}

	private StringProperty thirdSIDFilter6581 = new SimpleStringProperty(DEFAULT_3SID_FILTER_6581);

	public StringProperty thirdSIDFilter6581Property() {
		return thirdSIDFilter6581;
	}

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581.get();
	}

	@Override
	public void setThirdSIDFilter6581(String filter6581) {
		this.thirdSIDFilter6581.set(filter6581);
	}

	private StringProperty filter8580 = new SimpleStringProperty(DEFAULT_FILTER_8580);

	public StringProperty filter8580Property() {
		return filter8580;
	}

	@Override
	public String getFilter8580() {
		return filter8580.get();
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580.set(filter8580);
	}

	private StringProperty stereoFilter8580 = new SimpleStringProperty(DEFAULT_STEREO_FILTER_8580);

	public StringProperty stereoFilter8580Property() {
		return stereoFilter8580;
	}

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580.get();
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580.set(filter8580);
	}

	private StringProperty thirdSIDFilter8580 = new SimpleStringProperty(DEFAULT_3SID_FILTER_8580);

	public StringProperty thirdSIDFilter8580Property() {
		return thirdSIDFilter8580;
	}

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580.get();
	}

	@Override
	public void setThirdSIDFilter8580(String filter8580) {
		this.thirdSIDFilter8580.set(filter8580);
	}

	private StringProperty reSIDfpFilter6581 = new SimpleStringProperty(DEFAULT_ReSIDfp_FILTER_6581);

	public StringProperty reSIDfpFilter6581Property() {
		return reSIDfpFilter6581;
	}

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581.get();
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581.set(reSIDfpFilter6581);
	}

	private StringProperty reSIDfpStereoFilter6581 = new SimpleStringProperty(DEFAULT_ReSIDfp_STEREO_FILTER_6581);

	public StringProperty reSIDfpStereoFilter6581Property() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581.get();
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581.set(reSIDfpFilter6581);
	}

	private StringProperty reSIDfp3rdSIDFilter6581 = new SimpleStringProperty(DEFAULT_ReSIDfp_3SID_FILTER_6581);

	public StringProperty reSIDfp3rdSIDFilter6581Property() {
		return reSIDfp3rdSIDFilter6581;
	}

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfp3rdSIDFilter6581.get();
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String reSIDfpFilter6581) {
		this.reSIDfp3rdSIDFilter6581.set(reSIDfpFilter6581);
	}

	private StringProperty reSIDfpFilter8580 = new SimpleStringProperty(DEFAULT_ReSIDfp_FILTER_8580);

	public StringProperty reSIDfpFilter8580Property() {
		return reSIDfpFilter8580;
	}

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580.get();
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580.set(reSIDfpFilter8580);
	}

	private StringProperty reSIDfpStereoFilter8580 = new SimpleStringProperty(DEFAULT_ReSIDfp_STEREO_FILTER_8580);

	public StringProperty reSIDfpStereoFilter858Property() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580.get();
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580.set(reSIDfpFilter8580);
	}

	private StringProperty reSIDfp3rdSIDFilter8580 = new SimpleStringProperty(DEFAULT_ReSIDfp_3SID_FILTER_8580);

	public StringProperty reSIDfp3rdSIDFilter8580Property() {
		return reSIDfp3rdSIDFilter8580;
	}

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return reSIDfp3rdSIDFilter8580.get();
	}

	@Override
	public void setReSIDfpThirdSIDFilter8580(String reSIDfpFilter8580) {
		this.reSIDfp3rdSIDFilter8580.set(reSIDfpFilter8580);
	}

}
