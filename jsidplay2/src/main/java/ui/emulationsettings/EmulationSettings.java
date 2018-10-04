package ui.emulationsettings;

import static libsidplay.common.ChipModel.MOS6581;
import static libsidplay.common.ChipModel.MOS8580;
import static libsidplay.common.Emulation.RESID;
import static libsidplay.common.Emulation.RESIDFP;
import static libsidplay.common.Engine.HARDSID;
import static libsidplay.common.Engine.NETSID;
import static libsidplay.common.SIDChip.FC_MAX;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.EnumToStringConverter;
import ui.common.HexNumberToStringConverter;
import ui.common.NumberToStringConverter;
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

	private enum StereoMode {
		AUTO, STEREO, THREE_SID,
	}

	private enum SidReads {
		FIRST_SID, SECOND_SID, THIRD_SID
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
	private CheckBox boosted8580, fakeStereo, detectPSID64ChipModel;
	@FXML
	private Slider mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance, mainDelay,
			secondDelay, thirdDelay;
	@FXML
	private Label mainVolumeValue, secondVolumeValue, thirdVolumeValue;
	@FXML
	private LineChart<Number, Number> mainFilterCurve, secondFilterCurve, thirdFilterCurve;

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
	protected void initialize() {
		duringInitialization = true;
		ResourceBundle bundle = util.getBundle();
		AudioSection audioSection = util.getConfig().getAudioSection();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		muteVoice1.selectedProperty().bindBidirectional(emulationSection.muteVoice1Property());
		muteVoice1.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(0, muteVoice1.isSelected())));
		muteVoice2.selectedProperty().bindBidirectional(emulationSection.muteVoice2Property());
		muteVoice2.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(1, muteVoice2.isSelected())));
		muteVoice3.selectedProperty().bindBidirectional(emulationSection.muteVoice3Property());
		muteVoice3.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(2, muteVoice3.isSelected())));
		muteVoice4.selectedProperty().bindBidirectional(emulationSection.muteVoice4Property());
		muteVoice4.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(3, muteVoice4.isSelected())));
		muteVoice5.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice1Property());
		muteVoice5.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(0, muteVoice5.isSelected())));
		muteVoice6.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice2Property());
		muteVoice6.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(1, muteVoice6.isSelected())));
		muteVoice7.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice3Property());
		muteVoice7.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(2, muteVoice7.isSelected())));
		muteVoice8.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice4Property());
		muteVoice8.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(3, muteVoice8.isSelected())));
		muteVoice9.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice1Property());
		muteVoice9.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(0, muteVoice9.isSelected())));
		muteVoice10.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice2Property());
		muteVoice10.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(1, muteVoice10.isSelected())));
		muteVoice11.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice3Property());
		muteVoice11.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(2, muteVoice11.isSelected())));
		muteVoice12.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice4Property());
		muteVoice12.selectedProperty().addListener(
				(obj, o, n) -> util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(2, muteVoice12.isSelected())));

		mainFilters = FXCollections.<String>observableArrayList();
		mainFilter.setItems(mainFilters);
		secondFilters = FXCollections.<String>observableArrayList();
		secondFilter.setItems(secondFilters);
		thirdFilters = FXCollections.<String>observableArrayList();
		thirdFilter.setItems(thirdFilters);

		mainBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		mainBalance.valueProperty().bindBidirectional(audioSection.mainBalanceProperty());
		mainBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(0, newValue.floatValue()));
		});
		secondBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		secondBalance.valueProperty().bindBidirectional(audioSection.secondBalanceProperty());
		secondBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(1, newValue.floatValue()));
		});
		thirdBalance.setLabelFormatter(new NumberToStringConverter<Double>(1));
		thirdBalance.valueProperty().bindBidirectional(audioSection.thirdBalanceProperty());
		thirdBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(2, newValue.floatValue()));
		});

		mainDelay.valueProperty().bindBidirectional(audioSection.mainDelayProperty());
		mainDelay.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setDelay(0, newValue.intValue()));
		});
		secondDelay.valueProperty().bindBidirectional(audioSection.secondDelayProperty());
		secondDelay.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setDelay(1, newValue.intValue()));
		});
		thirdDelay.valueProperty().bindBidirectional(audioSection.thirdDelayProperty());
		thirdDelay.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setDelay(2, newValue.intValue()));
		});

		mainVolume.valueProperty().bindBidirectional(audioSection.mainVolumeProperty());
		mainVolumeValue.textProperty().bindBidirectional(audioSection.mainVolumeProperty(), new NumberToStringConverter<>(2));
		mainVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setVolume(0, newValue.floatValue()));
		});
		secondVolume.valueProperty().bindBidirectional(audioSection.secondVolumeProperty());
		secondVolumeValue.textProperty().bindBidirectional(audioSection.secondVolumeProperty(),
				new NumberToStringConverter<>(2));
		secondVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(b -> b.setVolume(1, newValue.floatValue()));
		});
		thirdVolume.valueProperty().bindBidirectional(audioSection.thirdVolumeProperty());
		thirdVolumeValue.textProperty().bindBidirectional(audioSection.thirdVolumeProperty(), new NumberToStringConverter<>(2));
		thirdVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(b -> b.setVolume(2, newValue.floatValue()));
		});

		stereoModes = FXCollections.<StereoMode>observableArrayList(StereoMode.values());
		stereoMode.setConverter(new EnumToStringConverter<StereoMode>(bundle));
		stereoMode.setItems(stereoModes);

		baseAddress.textProperty().bindBidirectional(emulationSection.dualSidBaseProperty(), new HexNumberToStringConverter());
		thirdAddress.textProperty().bindBidirectional(emulationSection.thirdSIDBaseProperty(), new HexNumberToStringConverter());

		sidReads = FXCollections.<SidReads>observableArrayList(SidReads.values());
		sidToRead.setConverter(new EnumToStringConverter<SidReads>(bundle));
		sidToRead.setItems(sidReads);
		sidToRead.getSelectionModel().select(emulationSection.getSidNumToRead());

		sid1Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid1Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid1Emulation.valueProperty().bindBidirectional(emulationSection.userEmulationProperty());
		sid1Emulation.setItems(sid1Emulations);

		sid2Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid2Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid2Emulation.valueProperty().bindBidirectional(emulationSection.stereoEmulationProperty());
		sid2Emulation.setItems(sid2Emulations);

		sid3Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid3Emulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		sid3Emulation.valueProperty().bindBidirectional(emulationSection.thirdEmulationProperty());
		sid3Emulation.setItems(sid3Emulations);

		sid1Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid1Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid1Model.valueProperty().bindBidirectional(emulationSection.userSidModelProperty());
		sid1Model.setItems(sid1Models);

		sid2Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid2Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid2Model.valueProperty().bindBidirectional(emulationSection.stereoSidModelProperty());
		sid2Model.setItems(sid2Models);

		sid3Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid3Model.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sid3Model.valueProperty().bindBidirectional(emulationSection.thirdSIDModelProperty());
		sid3Model.setItems(sid3Models);

		defaultModels = FXCollections.<ChipModel>observableArrayList(MOS6581, MOS8580);
		defaultModel.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		defaultModel.valueProperty().bindBidirectional(emulationSection.defaultSidModelProperty());
		defaultModel.setItems(defaultModels);

		defaultEmulations = FXCollections.<Emulation>observableArrayList(RESID, RESIDFP);
		defaultEmulation.setConverter(new EnumToStringConverter<Emulation>(bundle));
		defaultEmulation.valueProperty().bindBidirectional(emulationSection.defaultEmulationProperty());
		defaultEmulation.setItems(defaultEmulations);

		boosted8580.selectedProperty().bindBidirectional(emulationSection.digiBoosted8580Property());
		fakeStereo.selectedProperty().bindBidirectional(emulationSection.fakeStereoProperty());
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
		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);
		boolean isForcedStereo = emulationSection.isForceStereoTune();
		boolean isForced3Sid = emulationSection.isForce3SIDTune();
		if (isForced3Sid) {
			stereoMode.getSelectionModel().select(StereoMode.THREE_SID);
		} else if (isForcedStereo) {
			stereoMode.getSelectionModel().select(StereoMode.STEREO);
		} else {
			stereoMode.getSelectionModel().select(StereoMode.AUTO);
		}
		// stereo, only:
		mainBalance.setDisable(!second);
		mainDelay.setDisable(!second);
		secondVolume.setDisable(!second);
		secondBalance.setDisable(!second);
		secondDelay.setDisable(!second);
		sid2Emulation.setDisable(!second);
		sid2Model.setDisable(!second);
		secondFilter.setDisable(!second);
		secondFilterCurve.setDisable(!second);
		muteVoice5.setDisable(!second);
		muteVoice6.setDisable(!second);
		muteVoice7.setDisable(!second);
		muteVoice8.setDisable(!second);
		// 3-SID, only:
		thirdVolume.setDisable(!third);
		thirdBalance.setDisable(!third);
		thirdDelay.setDisable(!third);
		sid3Emulation.setDisable(!third);
		sid3Model.setDisable(!third);
		thirdFilter.setDisable(!third);
		thirdFilterCurve.setDisable(!third);
		muteVoice9.setDisable(!third);
		muteVoice10.setDisable(!third);
		muteVoice11.setDisable(!third);
		muteVoice12.setDisable(!third);
		// fake stereo, only:
		sidToRead.setDisable(!emulationSection.isFakeStereo());
		// forced stereo or forced 3-SID, only:
		baseAddress.setDisable(!(isForcedStereo || isForced3Sid));
		// forced 3-SID, only:
		thirdAddress.setDisable(!isForced3Sid);
		// fake stereo does not work with HardSID4U or SIDBlaster hardware
		fakeStereo.setDisable(
				emulationSection.getEngine() == HARDSID || emulationSection.getEngine() == Engine.SIDBLASTER);
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Emulation() {
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid2Emulation() {
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid3Emulation() {
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setDefaultEmulation() {
		// default emulation has an impact on all emulation settings
		// (because "Default" means: use default emulation)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid1Model() {
		// 1st chip model has an impact on all chip model settings
		// (because "Automatic" means: use 1st chip model)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid2Model() {
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid3Model() {
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setDefaultModel() {
		// default chip model has an impact on all chip model settings
		// (because "Automatic" means: use default chip model, if tune model is unknown)
		updateFilterList(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateFilterList(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateFilterList(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setBaseAddress() {
		enableStereoSettings(util.getPlayer().getTune());
		updateSIDChipConfiguration();
	}

	@FXML
	private void setThirdAddress() {
		enableStereoSettings(util.getPlayer().getTune());
		updateSIDChipConfiguration();
	}

	@FXML
	private void setFakeStereo() {
		enableStereoSettings(util.getPlayer().getTune());
		// fake stereo mode has an impact on mono and stereo filter curves
		updateFilterConfiguration(0, mainFilter, mainFilterCurve);
		updateFilterConfiguration(1, secondFilter, secondFilterCurve);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setStereoMode() {
		StereoMode mode = stereoMode.getSelectionModel().getSelectedItem();
		if (mode == StereoMode.THREE_SID) {
			util.getConfig().getEmulationSection().setForceStereoTune(true);
			util.getConfig().getEmulationSection().setForce3SIDTune(true);
		} else if (mode == StereoMode.STEREO) {
			util.getConfig().getEmulationSection().setForceStereoTune(true);
			util.getConfig().getEmulationSection().setForce3SIDTune(false);
		} else {
			util.getConfig().getEmulationSection().setForceStereoTune(false);
			util.getConfig().getEmulationSection().setForce3SIDTune(false);
		}
		enableStereoSettings(util.getPlayer().getTune());
		// stereo mode changes has an impact on all filter curves
		updateFilterConfiguration(0, mainFilter, mainFilterCurve);
		updateFilterConfiguration(1, secondFilter, secondFilterCurve);
		updateFilterConfiguration(2, thirdFilter, thirdFilterCurve);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSidToRead() {
		int sidNumToRead = sidToRead.getSelectionModel().getSelectedIndex();
		util.getConfig().getEmulationSection().setSidNumToRead(sidNumToRead);
	}

	@FXML
	private void setDigiBoost() {
		util.getPlayer().configureSIDs((num, sid) -> sid
				.input(util.getConfig().getEmulationSection().isDigiBoosted8580() ? sid.getInputDigiBoost() : 0));
	}

	@FXML
	private void setMainFilter() {
		updateFilterConfiguration(0, mainFilter, mainFilterCurve);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSecondFilter() {
		updateFilterConfiguration(1, secondFilter, secondFilterCurve);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setThirdFilter() {
		updateFilterConfiguration(2, thirdFilter, thirdFilterCurve);
		updateSIDChipConfiguration();
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
	 * Update filter settings of the specified SID number according to the currently
	 * selected filter.
	 * 
	 * @param sidNum      SID chip number
	 * @param filterBox   filter combo box
	 * @param filterCurve filter curve to update
	 */
	private void updateFilterConfiguration(int sidNum, ComboBox<String> filterBox,
			LineChart<Number, Number> filterCurve) {
		IEmulationSection emulationSection = util.getConfig().getEmulationSection();

		SidTune tune = util.getPlayer().getTune();
		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, sidNum);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, sidNum);

		String filterName = filterBox.getSelectionModel().getSelectedItem();
		boolean filterDisabled = filterName == null || filterName.isEmpty();

		emulationSection.setFilterEnable(sidNum, !filterDisabled);
		emulationSection.setFilterName(sidNum, engine, emulation, model, !filterDisabled ? filterName : null);

		drawFilterCurve(filterBox, filterCurve);
	}

	/**
	 * Update combo-box filter list according to the current emulation and chip
	 * model.
	 * 
	 * @param tune    currently played tune
	 * @param num     SID chip number
	 * @param filters resulting filter list to add matching filter names to
	 * @param filter  combo box to select currently selected filter
	 */
	private void updateFilterList(final SidTune tune, int num, ObservableList<String> filters,
			ComboBox<String> filter) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, num);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, num);
		String filterName = emulationSection.getFilterName(num, engine, emulation, model);
		boolean filterEnable = emulationSection.isFilterEnable(num);

		filters.clear();
		if (engine == NETSID) {
			filters.addAll(TrySetSidModel.getFilterNames(model));
		} else {
			filters.add("");
			for (IFilterSection filterSection : util.getConfig().getFilterSection()) {
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
	 * Draw filter curve of the specified SID number and filter name
	 * 
	 * @param filterBox filter combo box
	 * @param num       SID chip number
	 */
	private void drawFilterCurve(final ComboBox<String> filterBox, LineChart<Number, Number> filterCurve) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		SidTune tune = util.getPlayer().getTune();
		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);

		List<Data<Number, Number>> dataList = new ArrayList<>();

		Optional<FilterSection> optFilter = util.getConfig().getFilterSection().stream()
				.filter(f -> f.getName().equals(filterBox.getSelectionModel().getSelectedItem())).findFirst();
		if (optFilter.isPresent()) {
			FilterSection filter = optFilter.get();
			// stereo curve or 3-SID curve currently not used?
			if (!((filterCurve == secondFilterCurve && !second) || (filterCurve == thirdFilterCurve && !third))) {
				for (int fc = 0; fc < FC_MAX; fc++) {
					if (filter.isReSIDFilter6581() || filter.isReSIDFilter8580()) {
						double data = builder.resid.resid.FilterModelConfig.estimateFrequency(filter, fc);
						dataList.add(new XYChart.Data<Number, Number>(fc, data));
					} else if (filter.isReSIDfpFilter6581() || filter.isReSIDfpFilter8580()) {
						double data = builder.resid.residfp.FilterModelConfig.estimateFrequency(filter, fc);
						dataList.add(new XYChart.Data<Number, Number>(fc, data));
					}
				}
			}
		}
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setData(FXCollections.observableArrayList(dataList));

		List<XYChart.Series<Number, Number>> seriesList = Arrays.asList(series);
		filterCurve.setData(FXCollections.observableArrayList(seriesList));
	}

}