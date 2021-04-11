package ui.emulationsettings;

import static libsidplay.common.ChipModel.MOS6581;
import static libsidplay.common.ChipModel.MOS8580;
import static libsidplay.common.Emulation.RESID;
import static libsidplay.common.Emulation.RESIDFP;
import static libsidplay.common.Engine.HARDSID;
import static libsidplay.common.Engine.NETSID;
import static libsidplay.common.Engine.SIDBLASTER;
import static libsidplay.common.SIDChip.FC_MAX;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_DIGI_BOOSTED_8580;
import static sidplay.ini.IniDefaults.DEFAULT_DUAL_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
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
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_NUM_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;
import static ui.entities.config.EmulationSection.DEFAULT_DETECT_PSID64_CHIP_MODEL;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import builder.netsiddev.commands.TrySetSidModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SidReads;
import libsidplay.common.StereoMode;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.converter.EnumToStringConverter;
import ui.common.converter.HexNumberToStringConverter;
import ui.common.converter.NumberToStringConverter;
import ui.entities.config.AudioSection;
import ui.entities.config.EmulationSection;
import ui.entities.config.FilterSection;

public class EmulationSettings extends C64Window {

	protected final class EmulationChange implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getNewValue() == State.START) {
				Platform.runLater(() -> updateSettingsForTune(util.getPlayer().getTune()));
			}
		}
	}

	@FXML
	private CheckBox muteVoice1, muteVoice2, muteVoice3, muteVoice4, muteVoice5, muteVoice6, muteVoice7, muteVoice8,
			muteVoice9, muteVoice10, muteVoice11, muteVoice12;
	@FXML
	private ComboBox<Emulation> sid1Emulation, sid2Emulation, sid3Emulation, defaultEmulation;
	@FXML
	private ComboBox<ChipModel> sid1Model, sid2Model, sid3Model, defaultModel;
	@FXML
	private ComboBox<String> mainFilter, secondFilter, thirdFilter;
	@FXML
	private ComboBox<SidReads> sidToRead;
	@FXML
	private ComboBox<StereoMode> stereoMode;
	@FXML
	private TextField baseAddress, thirdAddress;
	@FXML
	private CheckBox boosted8580, fakeStereo, detectPSID64ChipModel, filter, stereoFilter, thirdSidFilter;
	@FXML
	private Slider mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance, mainDelay,
			secondDelay, thirdDelay;
	@FXML
	private Label mainVolumeValue, secondVolumeValue, thirdVolumeValue;
	@FXML
	private LineChart<Number, Number> mainFilterCurve, secondFilterCurve, thirdFilterCurve;

	@FXML
	private Button copy;

	private ObservableList<Emulation> sid1Emulations, sid2Emulations, sid3Emulations, defaultEmulations;
	private ObservableList<ChipModel> sid1Models, sid2Models, sid3Models, defaultModels;
	private ObservableList<String> mainFilters, secondFilters, thirdFilters;
	private ObservableList<SidReads> sidReads;
	private ObservableList<StereoMode> stereoModes;

	private PropertyChangeListener emulationChange;

	private boolean duringInitialization;

	public EmulationSettings() {
		super();
	}

	public EmulationSettings(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		duringInitialization = true;
		ResourceBundle bundle = util.getBundle();
		AudioSection audioSection = util.getConfig().getAudioSection();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		muteVoice1.selectedProperty().bindBidirectional(emulationSection.muteVoice1Property());
		muteVoice1.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(0, n.booleanValue())));
		muteVoice2.selectedProperty().bindBidirectional(emulationSection.muteVoice2Property());
		muteVoice2.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(1, n.booleanValue())));
		muteVoice3.selectedProperty().bindBidirectional(emulationSection.muteVoice3Property());
		muteVoice3.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(2, n.booleanValue())));
		muteVoice4.selectedProperty().bindBidirectional(emulationSection.muteVoice4Property());
		muteVoice4.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(3, n.booleanValue())));
		muteVoice5.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice1Property());
		muteVoice5.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(0, n.booleanValue())));
		muteVoice6.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice2Property());
		muteVoice6.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(1, n.booleanValue())));
		muteVoice7.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice3Property());
		muteVoice7.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(2, n.booleanValue())));
		muteVoice8.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice4Property());
		muteVoice8.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(3, n.booleanValue())));
		muteVoice9.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice1Property());
		muteVoice9.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(0, n.booleanValue())));
		muteVoice10.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice2Property());
		muteVoice10.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(1, n.booleanValue())));
		muteVoice11.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice3Property());
		muteVoice11.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(2, n.booleanValue())));
		muteVoice12.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice4Property());
		muteVoice12.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(3, n.booleanValue())));

		mainFilters = FXCollections.<String>observableArrayList();
		mainFilter.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		mainFilter.setItems(mainFilters);
		secondFilters = FXCollections.<String>observableArrayList();
		secondFilter.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		secondFilter.setItems(secondFilters);
		thirdFilters = FXCollections.<String>observableArrayList();
		thirdFilter.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		thirdFilter.setItems(thirdFilters);

		mainBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		mainBalance.valueProperty().bindBidirectional(audioSection.mainBalanceProperty());
		mainBalance.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setBalance(0, n.floatValue())));
		secondBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		secondBalance.valueProperty().bindBidirectional(audioSection.secondBalanceProperty());
		secondBalance.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setBalance(1, n.floatValue())));
		thirdBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		thirdBalance.valueProperty().bindBidirectional(audioSection.thirdBalanceProperty());
		thirdBalance.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setBalance(2, n.floatValue())));

		mainDelay.valueProperty().bindBidirectional(audioSection.mainDelayProperty());
		mainDelay.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setDelay(0, n.intValue())));
		secondDelay.valueProperty().bindBidirectional(audioSection.secondDelayProperty());
		secondDelay.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setDelay(1, n.intValue())));
		thirdDelay.valueProperty().bindBidirectional(audioSection.thirdDelayProperty());
		thirdDelay.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setDelay(2, n.intValue())));

		mainVolume.valueProperty().bindBidirectional(audioSection.mainVolumeProperty());
		mainVolumeValue.textProperty().bindBidirectional(audioSection.mainVolumeProperty(),
				new NumberToStringConverter<>(2));
		mainVolume.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(m -> m.setVolume(0, n.floatValue())));

		secondVolume.valueProperty().bindBidirectional(audioSection.secondVolumeProperty());
		secondVolumeValue.textProperty().bindBidirectional(audioSection.secondVolumeProperty(),
				new NumberToStringConverter<>(2));
		secondVolume.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(b -> b.setVolume(1, n.floatValue())));

		thirdVolume.valueProperty().bindBidirectional(audioSection.thirdVolumeProperty());
		thirdVolumeValue.textProperty().bindBidirectional(audioSection.thirdVolumeProperty(),
				new NumberToStringConverter<>(2));
		thirdVolume.valueProperty()
				.addListener((obj, o, n) -> util.getPlayer().configureMixer(b -> b.setVolume(2, n.floatValue())));

		stereoModes = FXCollections.<StereoMode>observableArrayList(StereoMode.values());
		stereoMode.setConverter(new EnumToStringConverter<StereoMode>(bundle));
		stereoMode.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		stereoMode.setItems(stereoModes);

		baseAddress.textProperty().bindBidirectional(emulationSection.dualSidBaseProperty(),
				new HexNumberToStringConverter());
		baseAddress.textProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		thirdAddress.textProperty().bindBidirectional(emulationSection.thirdSIDBaseProperty(),
				new HexNumberToStringConverter());
		thirdAddress.textProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());

		sidReads = FXCollections.<SidReads>observableArrayList(SidReads.values());
		sidToRead.setConverter(new EnumToStringConverter<SidReads>(bundle));
		sidToRead.setItems(sidReads);
		sidToRead.valueProperty().addListener((onj, o, n) -> emulationSection.setSidToRead(n));
		sidToRead.getSelectionModel().select(emulationSection.getSidToRead());

		sid1Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid1Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid1Emulation.valueProperty().bindBidirectional(emulationSection.userEmulationProperty());
		sid1Emulation.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid1Emulation.setItems(sid1Emulations);

		sid2Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid2Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid2Emulation.valueProperty().bindBidirectional(emulationSection.stereoEmulationProperty());
		sid2Emulation.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid2Emulation.setItems(sid2Emulations);

		sid3Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid3Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid3Emulation.valueProperty().bindBidirectional(emulationSection.thirdEmulationProperty());
		sid3Emulation.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid3Emulation.setItems(sid3Emulations);

		sid1Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid1Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid1Model.valueProperty().bindBidirectional(emulationSection.userSidModelProperty());
		sid1Model.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid1Model.setItems(sid1Models);

		sid2Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid2Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid2Model.valueProperty().bindBidirectional(emulationSection.stereoSidModelProperty());
		sid2Model.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid2Model.setItems(sid2Models);

		sid3Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid3Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid3Model.valueProperty().bindBidirectional(emulationSection.thirdSIDModelProperty());
		sid3Model.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		sid3Model.setItems(sid3Models);

		defaultModels = FXCollections.<ChipModel>observableArrayList(MOS6581, MOS8580);
		defaultModel.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		defaultModel.valueProperty().bindBidirectional(emulationSection.defaultSidModelProperty());
		defaultModel.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		defaultModel.setItems(defaultModels);

		defaultEmulations = FXCollections.<Emulation>observableArrayList(RESID, RESIDFP);
		defaultEmulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		defaultEmulation.valueProperty().bindBidirectional(emulationSection.defaultEmulationProperty());
		defaultEmulation.valueProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());
		defaultEmulation.setItems(defaultEmulations);

		boosted8580.selectedProperty().bindBidirectional(emulationSection.digiBoosted8580Property());
		boosted8580.selectedProperty().addListener((obj, o, n) -> util.getPlayer().configureSIDs(
				(num, sid) -> sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0)));

		fakeStereo.selectedProperty().bindBidirectional(emulationSection.fakeStereoProperty());
		fakeStereo.selectedProperty().addListener((obj, o, n) -> updateSIDChipConfiguration());

		filter.selectedProperty().bindBidirectional(emulationSection.filterProperty());
		filter.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setFilterEnable(emulationSection, 0)));
		stereoFilter.selectedProperty().bindBidirectional(emulationSection.stereoFilterProperty());
		stereoFilter.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setFilterEnable(emulationSection, 1)));
		thirdSidFilter.selectedProperty().bindBidirectional(emulationSection.thirdSIDFilterProperty());
		thirdSidFilter.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setFilterEnable(emulationSection, 2)));

		detectPSID64ChipModel.selectedProperty().bindBidirectional(emulationSection.detectPSID64ChipModelProperty());

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		Platform.runLater(() -> {
			updateSettingsForTune(util.getPlayer().getTune());
			duringInitialization = false;
		});
	}

	private void updateSettingsForTune(SidTune tune) {
		updateFilterList(tune, 0, mainFilters, mainFilter);
		updateFilterList(tune, 1, secondFilters, secondFilter);
		updateFilterList(tune, 2, thirdFilters, thirdFilter);
		enableStereoSettings(tune);
	}

	private void enableStereoSettings(SidTune tune) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		boolean hardwareBasedSid = emulationSection.getEngine() == HARDSID
				|| emulationSection.getEngine() == SIDBLASTER;
		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);
		boolean isForcedStereo = emulationSection.isForceStereoTune();
		boolean isForced3Sid = emulationSection.isForce3SIDTune();

		stereoMode.getSelectionModel().select(emulationSection.getStereoMode());
		mainVolume.setDisable(hardwareBasedSid);
		secondVolume.setDisable(hardwareBasedSid);
		thirdVolume.setDisable(hardwareBasedSid);
		sid1Emulation.setDisable(hardwareBasedSid);
		sid1Model.setDisable(hardwareBasedSid);
		mainFilter.setDisable(hardwareBasedSid);
		mainFilterCurve.setDisable(hardwareBasedSid);
		copy.setDisable(hardwareBasedSid);
		// stereo, only:
		mainBalance.setDisable(!second || hardwareBasedSid);
		mainDelay.setDisable(!second);
		secondBalance.setDisable(!second || hardwareBasedSid);
		secondDelay.setDisable(!second);
		sid2Emulation.setDisable(!second || hardwareBasedSid);
		sid2Model.setDisable(!second || hardwareBasedSid);
		secondFilter.setDisable(!second || hardwareBasedSid);
		secondFilterCurve.setDisable(!second || hardwareBasedSid);
		stereoFilter.setDisable(!second);
		// 3-SID, only:
		thirdBalance.setDisable(!third || hardwareBasedSid);
		thirdDelay.setDisable(!third);
		sid3Emulation.setDisable(!third || hardwareBasedSid);
		sid3Model.setDisable(!third || hardwareBasedSid);
		thirdFilter.setDisable(!third || hardwareBasedSid);
		thirdFilterCurve.setDisable(!third || hardwareBasedSid);
		thirdSidFilter.setDisable(!third);
		// fake stereo, only:
		sidToRead.setDisable(!emulationSection.isFakeStereo());
		// forced stereo or forced 3-SID, only:
		baseAddress.setDisable(!isForcedStereo && !isForced3Sid);
		// forced 3-SID, only:
		thirdAddress.setDisable(!isForced3Sid);
		// no SID boost for hardware SIDs
		boosted8580.setDisable(hardwareBasedSid);
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Emulation() {
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
	}

	@FXML
	private void setSid2Emulation() {
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
	}

	@FXML
	private void setSid3Emulation() {
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	@FXML
	private void setDefaultEmulation() {
		// default emulation has an impact on all emulation settings
		// (because "Default" means: use default emulation)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	@FXML
	private void setSid1Model() {
		// 1st chip model has an impact on all chip model settings
		// (because "Automatic" means: use 1st chip model)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	@FXML
	private void setSid2Model() {
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
	}

	@FXML
	private void setSid3Model() {
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	@FXML
	private void setDefaultModel() {
		// default chip model has an impact on all chip model settings
		// (because "Automatic" means: use default chip model, if tune model is unknown)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	@FXML
	private void setBaseAddress() {
		enableStereoSettings(util.getPlayer().getTune());
	}

	@FXML
	private void setThirdAddress() {
		enableStereoSettings(util.getPlayer().getTune());
	}

	@FXML
	private void setFakeStereo() {
		enableStereoSettings(util.getPlayer().getTune());
		// fake stereo mode has an impact on mono and stereo filter curves
		drawFilterCurve(util.getPlayer().getTune(), 0, mainFilter, mainFilterCurve);
		drawFilterCurve(util.getPlayer().getTune(), 1, secondFilter, secondFilterCurve);
	}

	@FXML
	private void setStereoMode() {
		util.getConfig().getEmulationSection().setStereoMode(stereoMode.getSelectionModel().getSelectedItem());

		enableStereoSettings(util.getPlayer().getTune());
		// stereo mode changes has an impact on all filter curves
		drawFilterCurve(util.getPlayer().getTune(), 0, mainFilter, mainFilterCurve);
		drawFilterCurve(util.getPlayer().getTune(), 1, secondFilter, secondFilterCurve);
		drawFilterCurve(util.getPlayer().getTune(), 2, thirdFilter, thirdFilterCurve);
	}

	@FXML
	private void setSidToRead() {
	}

	@FXML
	private void setDigiBoost() {
	}

	@FXML
	private void setMainFilter() {
		drawFilterCurve(util.getPlayer().getTune(), 0, mainFilter, mainFilterCurve);
	}

	@FXML
	private void setSecondFilter() {
		drawFilterCurve(util.getPlayer().getTune(), 1, secondFilter, secondFilterCurve);
	}

	@FXML
	private void setThirdFilter() {
		drawFilterCurve(util.getPlayer().getTune(), 2, thirdFilter, thirdFilterCurve);
	}

	@FXML
	private void restoreDefaults() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		AudioSection audioSection = util.getConfig().getAudioSection();

		emulationSection.setMuteVoice1(DEFAULT_MUTE_VOICE1);
		emulationSection.setMuteVoice2(DEFAULT_MUTE_VOICE2);
		emulationSection.setMuteVoice3(DEFAULT_MUTE_VOICE3);
		emulationSection.setMuteVoice4(DEFAULT_MUTE_VOICE4);
		emulationSection.setMuteStereoVoice1(DEFAULT_MUTE_STEREO_VOICE1);
		emulationSection.setMuteStereoVoice2(DEFAULT_MUTE_STEREO_VOICE2);
		emulationSection.setMuteStereoVoice3(DEFAULT_MUTE_STEREO_VOICE3);
		emulationSection.setMuteStereoVoice4(DEFAULT_MUTE_STEREO_VOICE4);
		emulationSection.setMuteThirdSIDVoice1(DEFAULT_MUTE_THIRDSID_VOICE1);
		emulationSection.setMuteThirdSIDVoice2(DEFAULT_MUTE_THIRDSID_VOICE2);
		emulationSection.setMuteThirdSIDVoice3(DEFAULT_MUTE_THIRDSID_VOICE3);
		emulationSection.setMuteThirdSIDVoice4(DEFAULT_MUTE_THIRDSID_VOICE4);

		audioSection.setMainBalance(DEFAULT_MAIN_BALANCE);
		audioSection.setSecondBalance(DEFAULT_SECOND_BALANCE);
		audioSection.setThirdBalance(DEFAULT_THIRD_BALANCE);
		audioSection.setMainDelay(DEFAULT_MAIN_DELAY);
		audioSection.setSecondDelay(DEFAULT_SECOND_DELAY);
		audioSection.setThirdDelay(DEFAULT_THIRD_DELAY);
		audioSection.setMainVolume(DEFAULT_MAIN_VOLUME);
		audioSection.setSecondVolume(DEFAULT_SECOND_VOLUME);
		audioSection.setThirdVolume(DEFAULT_THIRD_VOLUME);

		emulationSection.setForceStereoTune(DEFAULT_FORCE_STEREO_TUNE);
		emulationSection.setForce3SIDTune(DEFAULT_FORCE_3SID_TUNE);
		emulationSection.setDualSidBase(DEFAULT_DUAL_SID_BASE);
		emulationSection.setThirdSIDBase(DEFAULT_THIRD_SID_BASE);
		emulationSection.setSidNumToRead(DEFAULT_SID_NUM_TO_READ);

		emulationSection.setDefaultSidModel(DEFAULT_SID_MODEL);
		emulationSection.setDefaultEmulation(DEFAULT_EMULATION);

		emulationSection.setDigiBoosted8580(DEFAULT_DIGI_BOOSTED_8580);
		emulationSection.setFakeStereo(DEFAULT_FAKE_STEREO);
		emulationSection.setDetectPSID64ChipModel(DEFAULT_DETECT_PSID64_CHIP_MODEL);

		emulationSection.setUserEmulation(DEFAULT_USER_EMULATION);
		emulationSection.setStereoEmulation(DEFAULT_STEREO_EMULATION);
		emulationSection.setThirdEmulation(DEFAULT_3SID_EMULATION);

		emulationSection.setUserSidModel(DEFAULT_USER_MODEL);
		emulationSection.setStereoSidModel(DEFAULT_STEREO_MODEL);
		emulationSection.setThirdSIDModel(DEFAULT_3SID_MODEL);

		emulationSection.setFilter(DEFAULT_USE_FILTER);
		emulationSection.setStereoFilter(DEFAULT_USE_STEREO_FILTER);
		emulationSection.setThirdSIDFilter(DEFAULT_USE_3SID_FILTER);

		emulationSection.setFilter6581(DEFAULT_FILTER_6581);
		emulationSection.setFilter8580(DEFAULT_FILTER_8580);
		emulationSection.setStereoFilter6581(DEFAULT_STEREO_FILTER_6581);
		emulationSection.setStereoFilter8580(DEFAULT_STEREO_FILTER_8580);
		emulationSection.setThirdSIDFilter6581(DEFAULT_3SID_FILTER_6581);
		emulationSection.setThirdSIDFilter8580(DEFAULT_3SID_FILTER_8580);

		emulationSection.setReSIDfpFilter6581(DEFAULT_ReSIDfp_FILTER_6581);
		emulationSection.setReSIDfpFilter8580(DEFAULT_ReSIDfp_FILTER_8580);
		emulationSection.setReSIDfpStereoFilter6581(DEFAULT_ReSIDfp_STEREO_FILTER_6581);
		emulationSection.setReSIDfpStereoFilter8580(DEFAULT_ReSIDfp_STEREO_FILTER_8580);
		emulationSection.setReSIDfpThirdSIDFilter6581(DEFAULT_ReSIDfp_3SID_FILTER_6581);
		emulationSection.setReSIDfpThirdSIDFilter8580(DEFAULT_ReSIDfp_3SID_FILTER_8580);

		emulationSection.setNetSIDFilter6581(DEFAULT_NETSID_FILTER_6581);
		emulationSection.setNetSIDFilter8580(DEFAULT_NETSID_FILTER_8580);
		emulationSection.setNetSIDStereoFilter6581(DEFAULT_NETSID_STEREO_FILTER_6581);
		emulationSection.setNetSIDStereoFilter8580(DEFAULT_NETSID_STEREO_FILTER_8580);
		emulationSection.setNetSIDThirdSIDFilter6581(DEFAULT_NETSID_3SID_FILTER_6581);
		emulationSection.setNetSIDThirdSIDFilter8580(DEFAULT_NETSID_3SID_FILTER_8580);

		updateSettingsForTune(util.getPlayer().getTune());
	}

	@FXML
	private void doCopy() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		emulationSection.setStereoEmulation(emulationSection.getUserEmulation());
		emulationSection.setThirdEmulation(emulationSection.getUserEmulation());

		emulationSection.setStereoSidModel(emulationSection.getUserSidModel());
		emulationSection.setThirdSIDModel(emulationSection.getUserSidModel());

		emulationSection.setStereoFilter(emulationSection.isFilter());
		emulationSection.setThirdSIDFilter(emulationSection.isFilter());

		emulationSection.setStereoFilter6581(emulationSection.getFilter6581());
		emulationSection.setThirdSIDFilter6581(emulationSection.getFilter6581());
		emulationSection.setStereoFilter8580(emulationSection.getFilter8580());
		emulationSection.setThirdSIDFilter8580(emulationSection.getFilter8580());

		emulationSection.setReSIDfpStereoFilter6581(emulationSection.getReSIDfpFilter6581());
		emulationSection.setReSIDfpThirdSIDFilter6581(emulationSection.getReSIDfpFilter6581());
		emulationSection.setReSIDfpStereoFilter8580(emulationSection.getReSIDfpFilter8580());
		emulationSection.setReSIDfpThirdSIDFilter8580(emulationSection.getReSIDfpFilter8580());

		emulationSection.setNetSIDStereoFilter6581(emulationSection.getNetSIDFilter6581());
		emulationSection.setNetSIDThirdSIDFilter6581(emulationSection.getNetSIDFilter6581());
		emulationSection.setNetSIDStereoFilter8580(emulationSection.getNetSIDFilter8580());
		emulationSection.setNetSIDThirdSIDFilter8580(emulationSection.getNetSIDFilter8580());

		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
	}

	/**
	 * Update SID configuration on-the-fly.
	 */
	private void updateSIDChipConfiguration() {
		if (!duringInitialization) {
			util.getPlayer().updateSIDChipConfiguration();
		}
	}

	/**
	 * Update combo-box filter list according to the current emulation and chip
	 * model.
	 *
	 * @param tune    currently played tune
	 * @param sidNum  SID chip number
	 * @param filters resulting filter list to add matching filter names to
	 * @param filter  combo box to select currently selected filter
	 */
	private void updateFilterList(final SidTune tune, int sidNum, ObservableList<String> filters,
			ComboBox<String> filter) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		List<FilterSection> filterSections = util.getConfig().getFilterSection();

		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, sidNum);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, sidNum);
		String filterName = emulationSection.getFilterName(sidNum, engine, emulation, model);
		boolean filterEnable = emulationSection.isFilterEnable(sidNum);

		filters.clear();
		if (engine == NETSID) {
			filters.addAll(TrySetSidModel.getFilterNames(model));
		} else {
			filters.add(""/* filter disabled */);
			for (IFilterSection filterSection : filterSections) {
				switch (model) {
				case MOS6581:
					if (emulation.equals(RESIDFP) && filterSection.isReSIDfpFilter6581()
							|| emulation.equals(RESID) && filterSection.isReSIDFilter6581()) {
						filters.add(filterSection.getName());
					}
					break;
				case MOS8580:
					if (emulation.equals(RESIDFP) && filterSection.isReSIDfpFilter8580()
							|| emulation.equals(RESID) && filterSection.isReSIDFilter8580()) {
						filters.add(filterSection.getName());
					}
					break;
				default:
					break;
				}
			}
		}
		if (filterEnable) {
			filter.getSelectionModel().select(filterName);
		} else {
			filter.getSelectionModel().select(0);
		}
	}

	/**
	 * Update filter settings of the specified SID number according to the currently
	 * selected filter and re-draw filter curve.
	 *
	 * @param sidNum      SID chip number
	 * @param filterBox   filter combo box
	 * @param filterCurve filter curve to update
	 */
	private void drawFilterCurve(SidTune tune, int sidNum, ComboBox<String> filterBox,
			LineChart<Number, Number> filterCurve) {
		IEmulationSection emulationSection = util.getConfig().getEmulationSection();
		List<FilterSection> filterSections = util.getConfig().getFilterSection();

		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, sidNum);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, sidNum);
		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);

		String filterName = filterBox.getSelectionModel().getSelectedItem();
		boolean filterDisabled = filterName == null || filterName.isEmpty();

		emulationSection.setFilterEnable(sidNum, !filterDisabled);
		emulationSection.setFilterName(sidNum, engine, emulation, model, !filterDisabled ? filterName : null);

		List<Data<Number, Number>> dataList = new ArrayList<>();

		Optional<FilterSection> optFilter = filterSections.stream().filter(f -> f.getName().equals(filterName))
				.findFirst();
		if (optFilter.isPresent()) {
			FilterSection filterSection = optFilter.get();
			// stereo curve or 3-SID curve currently used?
			if (!(filterCurve == secondFilterCurve && !second || filterCurve == thirdFilterCurve && !third)) {
				for (int fc = 1; fc < FC_MAX; fc++) {
					if (filterSection.isReSIDFilter6581() || filterSection.isReSIDFilter8580()) {
						double data = builder.resid.resid.FilterModelConfig.estimateFrequency(filterSection, fc);
						dataList.add(new XYChart.Data<>(fc, data));
					} else if (filterSection.isReSIDfpFilter6581() || filterSection.isReSIDfpFilter8580()) {
						double data = builder.resid.residfp.FilterModelConfig.estimateFrequency(filterSection, fc);
						dataList.add(new XYChart.Data<>(fc, data));
					}
				}
			}
		}
		if (dataList.isEmpty()) {
			dataList.add(new XYChart.Data<>(0, 0));
		}
		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		series.setData(FXCollections.observableArrayList(dataList));

		List<XYChart.Series<Number, Number>> seriesList = Arrays.asList(series);
		filterCurve.setData(FXCollections.observableArrayList(seriesList));
	}

}