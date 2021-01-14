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
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_DEVICE_LIST;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_SERIAL_NUMBER;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_WRITE_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_NUM_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_MODE;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_SYNC_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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
import libsidplay.common.Ultimate64Mode;
import libsidplay.config.IEmulationSection;
import libsidplay.config.converter.FileToStringConverter;
import server.restful.common.Connectors;
import ui.common.converter.FileAttributeConverter;
import ui.common.converter.FileXmlAdapter;
import ui.common.properties.LazyListField;
import ui.common.properties.ShadowField;

@Embeddable
@Parameters(resourceBundle = "ui.entities.config.EmulationSection")
public class EmulationSection implements IEmulationSection {

	/**
	 * Auto-detect PSID tune settings.
	 */
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

	/**
	 * Hostname of the Ultimate64 streaming target to receive audio/video streams by
	 * UDP network packets.
	 */
	public static final String DEFAULT_ULTIMATE64_STREAMING_TARGET = "127.0.0.1";

	/**
	 * Port of the Ultimate64 streaming target to receive audio streams by UDP
	 * network packets.
	 */
	public static final int DEFAULT_ULTIMATE64_STREAMING_AUDIO_PORT = 30000;

	/**
	 * Port of the Ultimate64 streaming target to receive video streams by UDP
	 * network packets.
	 */
	public static final int DEFAULT_ULTIMATE64_STREAMING_VIDEO_PORT = 30001;

	private static final List<DeviceMapping> DEFAULT_SIDBLASTER_DEVICES = DEFAULT_SIDBLASTER_DEVICE_LIST.stream()
			.map(deviceMapping -> new DeviceMapping(deviceMapping.getSerialNum(), deviceMapping.getChipModel(), true))
			.collect(Collectors.toList());

	private ShadowField<ObjectProperty<Engine>, Engine> engine = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_ENGINE);

	public ObjectProperty<Engine> engineProperty() {
		return engine.property();
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

	private ShadowField<ObjectProperty<Emulation>, Emulation> defaultEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_EMULATION);

	public ObjectProperty<Emulation> defaultEmulationProperty() {
		return defaultEmulation.property();
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

	private ShadowField<ObjectProperty<Emulation>, Emulation> userEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_USER_EMULATION);

	public ObjectProperty<Emulation> userEmulationProperty() {
		return userEmulation.property();
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

	private ShadowField<ObjectProperty<Emulation>, Emulation> stereoEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_STEREO_EMULATION);

	public ObjectProperty<Emulation> stereoEmulationProperty() {
		return stereoEmulation.property();
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

	private ShadowField<ObjectProperty<Emulation>, Emulation> thirdEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_3SID_EMULATION);

	public ObjectProperty<Emulation> thirdEmulationProperty() {
		return thirdEmulation.property();
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

	private ShadowField<ObjectProperty<CPUClock>, CPUClock> defaultClockSpeed = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_CLOCK_SPEED);

	public ObjectProperty<CPUClock> defaultClockSpeedProperty() {
		return defaultClockSpeed.property();
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

	private ShadowField<ObjectProperty<CPUClock>, CPUClock> userClockSpeed = new ShadowField<>(
			SimpleObjectProperty::new, null);

	public ObjectProperty<CPUClock> userClockSpeedProperty() {
		return userClockSpeed.property();
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

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> defaultSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SID_MODEL);

	public ObjectProperty<ChipModel> defaultSidModelProperty() {
		return defaultSidModel.property();
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

	private ShadowField<BooleanProperty, Boolean> detectPSID64ChipModel = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DETECT_PSID64_CHIP_MODEL);

	public boolean isDetectPSID64ChipModel() {
		return detectPSID64ChipModel.get();
	}

	public void setDetectPSID64ChipModel(boolean detectPSID64ChipModel) {
		this.detectPSID64ChipModel.set(detectPSID64ChipModel);
	}

	public BooleanProperty detectPSID64ChipModelProperty() {
		return detectPSID64ChipModel.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> userSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_USER_MODEL);

	public ObjectProperty<ChipModel> userSidModelProperty() {
		return userSidModel.property();
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

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> stereoSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_STEREO_MODEL);

	public ObjectProperty<ChipModel> stereoSidModelProperty() {
		return stereoSidModel.property();
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

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> thirdSIDModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_3SID_MODEL);

	public ObjectProperty<ChipModel> thirdSIDModelProperty() {
		return thirdSIDModel.property();
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

	private ShadowField<ObjectProperty<Integer>, Integer> hardsid6581 = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_HARD_SID_6581);

	public ObjectProperty<Integer> hardsid6581Property() {
		return hardsid6581.property();
	}

	@Override
	public int getHardsid6581() {
		return hardsid6581.get();
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581.set(hardsid6581);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> hardsid8580 = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_HARD_SID_8580);

	public ObjectProperty<Integer> hardsid8580Property() {
		return hardsid8580.property();
	}

	@Override
	public int getHardsid8580() {
		return hardsid8580.get();
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580.set(hardsid8580);
	}

	private LazyListField<DeviceMapping> sidBlasterDeviceList = new LazyListField<>();

	public void setSidBlasterDeviceList(List<DeviceMapping> sidBlasterDeviceList) {
		this.sidBlasterDeviceList.set(sidBlasterDeviceList);
	}

	@OneToMany(cascade = CascadeType.ALL)
	@Override
	public List<DeviceMapping> getSidBlasterDeviceList() {
		return sidBlasterDeviceList.get(new ArrayList<>(DEFAULT_SIDBLASTER_DEVICES));
	}

	private ShadowField<ObjectProperty<Integer>, Integer> sidBlasterWriteBufferSize = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SIDBLASTER_WRITE_BUFFER_SIZE);

	public ObjectProperty<Integer> sidBlasterWriteBufferSizeProperty() {
		return sidBlasterWriteBufferSize.property();
	}

	@Override
	public int getSidBlasterWriteBufferSize() {
		return sidBlasterWriteBufferSize.get();
	}

	@Override
	public void setSidBlasterWriteBufferSize(int sidBlasterWriteBufferSize) {
		this.sidBlasterWriteBufferSize.set(sidBlasterWriteBufferSize);
	}

	private ShadowField<StringProperty, String> sidBlasterSerialNumber = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_SIDBLASTER_SERIAL_NUMBER);

	@Override
	public String getSidBlasterSerialNumber() {
		return sidBlasterSerialNumber.get();
	}

	@Override
	public void setSidBlasterSerialNumber(String sidBlasterSerialNumber) {
		this.sidBlasterSerialNumber.set(sidBlasterSerialNumber);
	}

	public StringProperty sidBlasterSerialNumberProperty() {
		return sidBlasterSerialNumber.property();
	}

	private ShadowField<StringProperty, String> netSidDevHost = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSIDDEV_HOST);

	public StringProperty netSidDevHostProperty() {
		return netSidDevHost.property();
	}

	@Override
	public String getNetSIDDevHost() {
		return netSidDevHost.get();
	}

	@Override
	public void setNetSIDDevHost(String hostname) {
		this.netSidDevHost.set(hostname);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> netSidDevPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_NETSIDDEV_PORT);

	public ObjectProperty<Integer> netSidDevPortProperty() {
		return netSidDevPort.property();
	}

	@Override
	public int getNetSIDDevPort() {
		return netSidDevPort.get();
	}

	@Override
	public void setNetSIDDevPort(int port) {
		this.netSidDevPort.set(port);
	}

	private ShadowField<ObjectProperty<Ultimate64Mode>, Ultimate64Mode> ultimate64Mode = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_MODE);

	public ObjectProperty<Ultimate64Mode> ultimate64ModeProperty() {
		return ultimate64Mode.property();
	}

	@Enumerated(EnumType.STRING)
	@Override
	public Ultimate64Mode getUltimate64Mode() {
		return ultimate64Mode.get();
	}

	@Override
	public void setUltimate64Mode(Ultimate64Mode ultimate64Mode) {
		this.ultimate64Mode.set(ultimate64Mode);
	}

	private ShadowField<StringProperty, String> ultimate64Host = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ULTIMATE64_HOST);

	public StringProperty ultimate64HostProperty() {
		return ultimate64Host.property();
	}

	@Override
	public String getUltimate64Host() {
		return ultimate64Host.get();
	}

	@Override
	public void setUltimate64Host(String hostname) {
		this.ultimate64Host.set(hostname);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64Port = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_ULTIMATE64_PORT);

	public ObjectProperty<Integer> ultimate64PortProperty() {
		return ultimate64Port.property();
	}

	@Override
	public int getUltimate64Port() {
		return ultimate64Port.get();
	}

	@Override
	public void setUltimate64Port(int port) {
		this.ultimate64Port.set(port);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64SyncDelay = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_SYNC_DELAY);

	public ObjectProperty<Integer> ultimate64SyncDelayProperty() {
		return ultimate64SyncDelay.property();
	}

	@Override
	public int getUltimate64SyncDelay() {
		return ultimate64SyncDelay.get();
	}

	@Override
	public void setUltimate64SyncDelay(int syncDelay) {
		this.ultimate64SyncDelay.set(syncDelay);
	}

	private ShadowField<StringProperty, String> ultimate64StreamingTarget = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ULTIMATE64_STREAMING_TARGET);

	public StringProperty ultimate64StreamingTargetProperty() {
		return ultimate64StreamingTarget.property();
	}

	public String getUltimate64StreamingTarget() {
		return ultimate64StreamingTarget.get();
	}

	public void setUltimate64StreamingTarget(String ultimate64StreamingTarget) {
		this.ultimate64StreamingTarget.set(ultimate64StreamingTarget);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64StreamingAudioPort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_STREAMING_AUDIO_PORT);

	public ObjectProperty<Integer> ultimate64StreamingAudioPortProperty() {
		return ultimate64StreamingAudioPort.property();
	}

	public int getUltimate64StreamingAudioPort() {
		return ultimate64StreamingAudioPort.get();
	}

	public void setUltimate64StreamingAudioPort(int ultimate64StreamingAudioPort) {
		this.ultimate64StreamingAudioPort.set(ultimate64StreamingAudioPort);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64StreamingVideoPort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_STREAMING_VIDEO_PORT);

	public ObjectProperty<Integer> ultimate64StreamingVideoPortProperty() {
		return ultimate64StreamingVideoPort.property();
	}

	public int getUltimate64StreamingVideoPort() {
		return ultimate64StreamingVideoPort.get();
	}

	public void setUltimate64StreamingVideoPort(int ultimate64StreamingVideoPort) {
		this.ultimate64StreamingVideoPort.set(ultimate64StreamingVideoPort);
	}

	private ShadowField<ObjectProperty<Connectors>, Connectors> appServerConnectors = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_CONNECTORS);

	public ObjectProperty<Connectors> appServerConnectorsProperty() {
		return appServerConnectors.property();
	}

	@Enumerated(EnumType.STRING)
	public Connectors getAppServerConnectors() {
		return this.appServerConnectors.get();
	}

	@Parameter(names = { "--appServerConnectors" }, descriptionKey = "APP_SERVER_CONNECTORS", order = 0)
	public void setAppServerConnectors(Connectors appServerConnectors) {
		this.appServerConnectors.set(appServerConnectors);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> appServerPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_APP_SERVER_PORT);

	public ObjectProperty<Integer> appServerPortProperty() {
		return appServerPort.property();
	}

	public int getAppServerPort() {
		return appServerPort.get();
	}

	@Parameter(names = { "--appServerPort" }, descriptionKey = "APP_SERVER_PORT", order = 1)
	public void setAppServerPort(int port) {
		this.appServerPort.set(port);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> appServerSecurePort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_APP_SERVER_SECURE_PORT);

	public ObjectProperty<Integer> appServerSecurePortProperty() {
		return appServerSecurePort.property();
	}

	public int getAppServerSecurePort() {
		return appServerSecurePort.get();
	}

	@Parameter(names = { "--appServerSecurePort" }, descriptionKey = "APP_SERVER_SECURE_PORT", order = 2)
	public void setAppServerSecurePort(int securePort) {
		this.appServerSecurePort.set(securePort);
	}

	private ShadowField<ObjectProperty<File>, File> appServerKeystore = new ShadowField<>(SimpleObjectProperty::new,
			null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getAppServerKeystoreFile() {
		return this.appServerKeystore.get();
	}

	@Parameter(names = {
			"--appServerKeystore" }, descriptionKey = "APP_SERVER_KEYSTORE", converter = FileToStringConverter.class, order = 3)
	public void setAppServerKeystoreFile(File appServerKeystoreFile) {
		this.appServerKeystore.set(appServerKeystoreFile);
	}

	private ShadowField<StringProperty, String> appServerKeystorePassword = new ShadowField<>(SimpleStringProperty::new,
			null);

	public StringProperty appServerKeystorePasswordProperty() {
		return appServerKeystorePassword.property();
	}

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	public String getAppServerKeystorePassword() {
		return appServerKeystorePassword.get();
	}

	@Parameter(names = { "--appServerKeystorePassword" }, descriptionKey = "APP_SERVER_KEYSTORE_PASSWORD", order = 4)
	public void setAppServerKeystorePassword(String appServerKeyStorePassword) {
		this.appServerKeystorePassword.set(appServerKeyStorePassword);
	}

	private ShadowField<StringProperty, String> appServerKeyAlias = new ShadowField<>(SimpleStringProperty::new, null);

	public StringProperty appServerKeyAliasProperty() {
		return appServerKeyAlias.property();
	}

	public String getAppServerKeyAlias() {
		return appServerKeyAlias.get();
	}

	@Parameter(names = { "--appServerKeyAlias" }, descriptionKey = "APP_SERVER_KEY_ALIAS", order = 5)
	public void setAppServerKeyAlias(String appServerKeyAlias) {
		this.appServerKeyAlias.set(appServerKeyAlias);
	}

	private ShadowField<StringProperty, String> appServerKeyPassword = new ShadowField<>(SimpleStringProperty::new,
			null);

	public StringProperty appServerKeyPasswordProperty() {
		return appServerKeyPassword.property();
	}

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	public String getAppServerKeyPassword() {
		return appServerKeyPassword.get();
	}

	@Parameter(names = { "--appServerKeyPassword" }, descriptionKey = "APP_SERVER_KEY_PASSWORD", order = 6)
	public void setAppServerKeyPassword(String appServerKeyPassword) {
		this.appServerKeyPassword.set(appServerKeyPassword);
	}

	private ShadowField<BooleanProperty, Boolean> filter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_FILTER);

	public BooleanProperty filterProperty() {
		return filter.property();
	}

	@Override
	public boolean isFilter() {
		return filter.get();
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter.set(isFilter);
	}

	private ShadowField<BooleanProperty, Boolean> stereoFilter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_STEREO_FILTER);

	public BooleanProperty stereoFilterProperty() {
		return stereoFilter.property();
	}

	@Override
	public boolean isStereoFilter() {
		return stereoFilter.get();
	}

	@Override
	public void setStereoFilter(boolean isFilter) {
		this.stereoFilter.set(isFilter);
	}

	private ShadowField<BooleanProperty, Boolean> thirdSIDFilter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_3SID_FILTER);

	public BooleanProperty thirdSIDFilterProperty() {
		return thirdSIDFilter.property();
	}

	@Override
	public boolean isThirdSIDFilter() {
		return thirdSIDFilter.get();
	}

	@Override
	public void setThirdSIDFilter(boolean isFilter) {
		this.thirdSIDFilter.set(isFilter);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> sidNumToRead = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_SID_NUM_TO_READ);

	public ObjectProperty<Integer> sidNumToReadProperty() {
		return sidNumToRead.property();
	}

	@Override
	public int getSidNumToRead() {
		return sidNumToRead.get();
	}

	@Override
	public void setSidNumToRead(int sidNumToRead) {
		this.sidNumToRead.set(sidNumToRead);
	}

	private ShadowField<BooleanProperty, Boolean> digiBoosted8580 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DIGI_BOOSTED_8580);

	public BooleanProperty digiBoosted8580Property() {
		return digiBoosted8580.property();
	}

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580.get();
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580.set(isDigiBoosted8580);
	}

	private ShadowField<BooleanProperty, Boolean> fakeStereo = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FAKE_STEREO);

	public BooleanProperty fakeStereoProperty() {
		return fakeStereo.property();
	}

	@Override
	public boolean isFakeStereo() {
		return fakeStereo.get();
	}

	@Override
	public void setFakeStereo(boolean fakeStereo) {
		this.fakeStereo.set(fakeStereo);
	}

	private ShadowField<IntegerProperty, Number> dualSidBase = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DUAL_SID_BASE);

	public IntegerProperty dualSidBaseProperty() {
		return dualSidBase.property();
	}

	@Override
	public int getDualSidBase() {
		return dualSidBase.get().intValue();
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase.set(dualSidBase);
	}

	private ShadowField<IntegerProperty, Number> thirdSIDBase = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_THIRD_SID_BASE);

	public IntegerProperty thirdSIDBaseProperty() {
		return thirdSIDBase.property();
	}

	@Override
	public int getThirdSIDBase() {
		return thirdSIDBase.get().intValue();
	}

	@Override
	public void setThirdSIDBase(int dualSidBase) {
		this.thirdSIDBase.set(dualSidBase);
	}

	private ShadowField<BooleanProperty, Boolean> forceStereoTune = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FORCE_STEREO_TUNE);

	public BooleanProperty forceStereoTuneProperty() {
		return forceStereoTune.property();
	}

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune.get();
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune.set(isForceStereoTune);
	}

	private ShadowField<BooleanProperty, Boolean> force3SIDTune = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FORCE_3SID_TUNE);

	public BooleanProperty force3SIDTuneProperty() {
		return force3SIDTune.property();
	}

	@Override
	public boolean isForce3SIDTune() {
		return force3SIDTune.get();
	}

	@Override
	public void setForce3SIDTune(boolean isForceStereoTune) {
		this.force3SIDTune.set(isForceStereoTune);
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE1);

	public BooleanProperty muteVoice1Property() {
		return muteVoice1.property();
	}

	@Override
	public boolean isMuteVoice1() {
		return muteVoice1.get();
	}

	@Override
	public void setMuteVoice1(boolean mute) {
		muteVoice1.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE2);

	public BooleanProperty muteVoice2Property() {
		return muteVoice2.property();
	}

	@Override
	public boolean isMuteVoice2() {
		return muteVoice2.get();
	}

	@Override
	public void setMuteVoice2(boolean mute) {
		muteVoice2.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE3);

	public BooleanProperty muteVoice3Property() {
		return muteVoice3.property();
	}

	@Override
	public boolean isMuteVoice3() {
		return muteVoice3.get();
	}

	@Override
	public void setMuteVoice3(boolean mute) {
		muteVoice3.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE4);

	public BooleanProperty muteVoice4Property() {
		return muteVoice4.property();
	}

	@Override
	public boolean isMuteVoice4() {
		return muteVoice4.get();
	}

	@Override
	public void setMuteVoice4(boolean mute) {
		muteVoice4.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE1);

	public BooleanProperty muteStereoVoice1Property() {
		return muteStereoVoice1.property();
	}

	@Override
	public boolean isMuteStereoVoice1() {
		return muteStereoVoice1.get();
	}

	@Override
	public void setMuteStereoVoice1(boolean mute) {
		muteStereoVoice1.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE2);

	public BooleanProperty muteStereoVoice2Property() {
		return muteStereoVoice2.property();
	}

	@Override
	public boolean isMuteStereoVoice2() {
		return muteStereoVoice2.get();
	}

	@Override
	public void setMuteStereoVoice2(boolean mute) {
		muteStereoVoice2.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE3);

	public BooleanProperty muteStereoVoice3Property() {
		return muteStereoVoice3.property();
	}

	@Override
	public boolean isMuteStereoVoice3() {
		return muteStereoVoice3.get();
	}

	@Override
	public void setMuteStereoVoice3(boolean mute) {
		muteStereoVoice3.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE4);

	public BooleanProperty muteStereoVoice4Property() {
		return muteStereoVoice4.property();
	}

	@Override
	public boolean isMuteStereoVoice4() {
		return muteStereoVoice4.get();
	}

	@Override
	public void setMuteStereoVoice4(boolean mute) {
		muteStereoVoice4.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE1);

	public BooleanProperty muteThirdSIDVoice1Property() {
		return muteThirdSIDVoice1.property();
	}

	@Override
	public boolean isMuteThirdSIDVoice1() {
		return muteThirdSIDVoice1.get();
	}

	@Override
	public void setMuteThirdSIDVoice1(boolean mute) {
		muteThirdSIDVoice1.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE2);

	public BooleanProperty muteThirdSIDVoice2Property() {
		return muteThirdSIDVoice2.property();
	}

	@Override
	public boolean isMuteThirdSIDVoice2() {
		return muteThirdSIDVoice2.get();
	}

	@Override
	public void setMuteThirdSIDVoice2(boolean mute) {
		muteThirdSIDVoice2.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE3);

	public BooleanProperty muteThirdSIDVoice3Property() {
		return muteThirdSIDVoice3.property();
	}

	@Override
	public boolean isMuteThirdSIDVoice3() {
		return muteThirdSIDVoice3.get();
	}

	@Override
	public void setMuteThirdSIDVoice3(boolean mute) {
		muteThirdSIDVoice3.set(mute);
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE4);

	public BooleanProperty muteThirdSIDVoice4Property() {
		return muteThirdSIDVoice4.property();
	}

	@Override
	public boolean isMuteThirdSIDVoice4() {
		return muteThirdSIDVoice4.get();
	}

	@Override
	public void setMuteThirdSIDVoice4(boolean mute) {
		muteThirdSIDVoice4.set(mute);
	}

	private ShadowField<StringProperty, String> netSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_FILTER_6581);

	public StringProperty netSIDFilter6581Property() {
		return netSIDFilter6581.property();
	}

	@Override
	public String getNetSIDFilter6581() {
		return netSIDFilter6581.get();
	}

	@Override
	public void setNetSIDFilter6581(String netSIDFilter6581) {
		this.netSIDFilter6581.set(netSIDFilter6581);
	}

	private ShadowField<StringProperty, String> netSIDStereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_STEREO_FILTER_6581);

	public StringProperty netSIDStereoFilter6581Property() {
		return netSIDStereoFilter6581.property();
	}

	@Override
	public String getNetSIDStereoFilter6581() {
		return netSIDStereoFilter6581.get();
	}

	@Override
	public void setNetSIDStereoFilter6581(String netSIDFilter6581) {
		this.netSIDStereoFilter6581.set(netSIDFilter6581);
	}

	private ShadowField<StringProperty, String> netSID3rdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_3SID_FILTER_6581);

	public StringProperty netSID3rdSIDFilter6581Property() {
		return netSID3rdSIDFilter6581.property();
	}

	@Override
	public String getNetSIDThirdSIDFilter6581() {
		return netSID3rdSIDFilter6581.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter6581(String netSIDFilter6581) {
		this.netSID3rdSIDFilter6581.set(netSIDFilter6581);
	}

	private ShadowField<StringProperty, String> netSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_FILTER_8580);

	public StringProperty netSIDFilter8580Property() {
		return netSIDFilter8580.property();
	}

	@Override
	public String getNetSIDFilter8580() {
		return netSIDFilter8580.get();
	}

	@Override
	public void setNetSIDFilter8580(String netSIDFilter8580) {
		this.netSIDFilter8580.set(netSIDFilter8580);
	}

	private ShadowField<StringProperty, String> netSIDStereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_STEREO_FILTER_8580);

	public StringProperty netSIDStereoFilter858Property() {
		return netSIDStereoFilter8580.property();
	}

	@Override
	public String getNetSIDStereoFilter8580() {
		return netSIDStereoFilter8580.get();
	}

	@Override
	public void setNetSIDStereoFilter8580(String netSIDFilter8580) {
		this.netSIDStereoFilter8580.set(netSIDFilter8580);
	}

	private ShadowField<StringProperty, String> netSID3rdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_3SID_FILTER_8580);

	public StringProperty netSID3rdSIDFilter8580Property() {
		return netSID3rdSIDFilter8580.property();
	}

	@Override
	public String getNetSIDThirdSIDFilter8580() {
		return netSID3rdSIDFilter8580.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter8580(String netSIDFilter8580) {
		this.netSID3rdSIDFilter8580.set(netSIDFilter8580);
	}

	private ShadowField<StringProperty, String> filter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_FILTER_6581);

	public StringProperty filter6581Property() {
		return filter6581.property();
	}

	@Override
	public String getFilter6581() {
		return filter6581.get();
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581.set(filter6581);
	}

	private ShadowField<StringProperty, String> stereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_STEREO_FILTER_6581);

	public StringProperty stereoFilter6581Property() {
		return stereoFilter6581.property();
	}

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581.get();
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581.set(filter6581);
	}

	private ShadowField<StringProperty, String> thirdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_3SID_FILTER_6581);

	public StringProperty thirdSIDFilter6581Property() {
		return thirdSIDFilter6581.property();
	}

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581.get();
	}

	@Override
	public void setThirdSIDFilter6581(String filter6581) {
		this.thirdSIDFilter6581.set(filter6581);
	}

	private ShadowField<StringProperty, String> filter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_FILTER_8580);

	public StringProperty filter8580Property() {
		return filter8580.property();
	}

	@Override
	public String getFilter8580() {
		return filter8580.get();
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580.set(filter8580);
	}

	private ShadowField<StringProperty, String> stereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_STEREO_FILTER_8580);

	public StringProperty stereoFilter8580Property() {
		return stereoFilter8580.property();
	}

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580.get();
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580.set(filter8580);
	}

	private ShadowField<StringProperty, String> thirdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_3SID_FILTER_8580);

	public StringProperty thirdSIDFilter8580Property() {
		return thirdSIDFilter8580.property();
	}

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580.get();
	}

	@Override
	public void setThirdSIDFilter8580(String filter8580) {
		this.thirdSIDFilter8580.set(filter8580);
	}

	private ShadowField<StringProperty, String> reSIDfpFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_FILTER_6581);

	public StringProperty reSIDfpFilter6581Property() {
		return reSIDfpFilter6581.property();
	}

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581.get();
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581.set(reSIDfpFilter6581);
	}

	private ShadowField<StringProperty, String> reSIDfpStereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_STEREO_FILTER_6581);

	public StringProperty reSIDfpStereoFilter6581Property() {
		return reSIDfpStereoFilter6581.property();
	}

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581.get();
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581.set(reSIDfpFilter6581);
	}

	private ShadowField<StringProperty, String> reSIDfp3rdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_3SID_FILTER_6581);

	public StringProperty reSIDfp3rdSIDFilter6581Property() {
		return reSIDfp3rdSIDFilter6581.property();
	}

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfp3rdSIDFilter6581.get();
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String reSIDfpFilter6581) {
		this.reSIDfp3rdSIDFilter6581.set(reSIDfpFilter6581);
	}

	private ShadowField<StringProperty, String> reSIDfpFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_FILTER_8580);

	public StringProperty reSIDfpFilter8580Property() {
		return reSIDfpFilter8580.property();
	}

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580.get();
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580.set(reSIDfpFilter8580);
	}

	private ShadowField<StringProperty, String> reSIDfpStereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_STEREO_FILTER_8580);

	public StringProperty reSIDfpStereoFilter858Property() {
		return reSIDfpStereoFilter8580.property();
	}

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580.get();
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580.set(reSIDfpFilter8580);
	}

	private ShadowField<StringProperty, String> reSIDfp3rdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_3SID_FILTER_8580);

	public StringProperty reSIDfp3rdSIDFilter8580Property() {
		return reSIDfp3rdSIDFilter8580.property();
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
