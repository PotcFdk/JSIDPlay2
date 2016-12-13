package ui.emulationsettings;

import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SIDChip;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.sidtune.SidTune;
import netsiddev_builder.commands.TrySetSidModel;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.HexNumberToString;
import ui.common.NumberToString;
import ui.entities.config.AudioSection;
import ui.entities.config.EmulationSection;
import ui.entities.config.FilterSection;

public class EmulationSettings extends C64Window {

	protected final class EmulationChange implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
			if (newValue == State.START) {
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

	private static final int STEP = 3;

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
	private CheckBox boosted8580;
	@FXML
	private CheckBox fakeStereo;
	@FXML
	private Slider mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance;
	@FXML
	Label mainVolumeValue, secondVolumeValue, thirdVolumeValue;
	@FXML
	private LineChart<Number, Number> mainFilterCurve, secondFilterCurve, thirdFilterCurve;

	private ObservableList<Emulation> sid1Emulations, sid2Emulations, sid3Emulations, defaultEmulations;
	private ObservableList<ChipModel> sid1Models, sid2Models, sid3Models, defaultModels;
	private ObservableList<String> mainFilters, secondFilters, thirdFilters;
	private ObservableList<SidReads> sidReads;
	private ObservableList<StereoMode> stereoModes;

	private ChangeListener<State> emulationChange;

	private boolean duringInitialization;

	public EmulationSettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		duringInitialization = true;

		ResourceBundle bundle = util.getBundle();
		AudioSection audioSection = util.getConfig().getAudioSection();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		mainFilters = FXCollections.<String>observableArrayList();
		mainFilter.setItems(mainFilters);
		secondFilters = FXCollections.<String>observableArrayList();
		secondFilter.setItems(secondFilters);
		thirdFilters = FXCollections.<String>observableArrayList();
		thirdFilter.setItems(thirdFilters);

		mainBalance.setLabelFormatter(new NumberToString<Double>(1));
		mainBalance.valueProperty().bindBidirectional(audioSection.mainBalanceProperty());
		mainBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(0, newValue.floatValue()));
		});
		secondBalance.setLabelFormatter(new NumberToString<Double>(1));
		secondBalance.valueProperty().bindBidirectional(audioSection.secondBalanceProperty());
		secondBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(1, newValue.floatValue()));
		});
		thirdBalance.setLabelFormatter(new NumberToString<Double>(1));
		thirdBalance.valueProperty().bindBidirectional(audioSection.thirdBalanceProperty());
		thirdBalance.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setBalance(2, newValue.floatValue()));
		});

		mainVolume.valueProperty().bindBidirectional(audioSection.mainVolumeProperty());
		mainVolumeValue.textProperty().bindBidirectional(audioSection.mainVolumeProperty(), new NumberToString<>(2));
		mainVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(m -> m.setVolume(0, newValue.floatValue()));
		});
		secondVolume.valueProperty().bindBidirectional(audioSection.secondVolumeProperty());
		secondVolumeValue.textProperty().bindBidirectional(audioSection.secondVolumeProperty(),
				new NumberToString<>(2));
		secondVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(b -> b.setVolume(1, newValue.floatValue()));
		});
		thirdVolume.valueProperty().bindBidirectional(audioSection.thirdVolumeProperty());
		thirdVolumeValue.textProperty().bindBidirectional(audioSection.thirdVolumeProperty(), new NumberToString<>(2));
		thirdVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
			util.getPlayer().configureMixer(b -> b.setVolume(2, newValue.floatValue()));
		});

		stereoModes = FXCollections.<StereoMode>observableArrayList(StereoMode.values());
		stereoMode.setConverter(new EnumToString<StereoMode>(bundle));
		stereoMode.setItems(stereoModes);

		baseAddress.textProperty().bindBidirectional(emulationSection.dualSidBaseProperty(), new HexNumberToString());
		thirdAddress.textProperty().bindBidirectional(emulationSection.thirdSIDBaseProperty(), new HexNumberToString());

		sidReads = FXCollections.<SidReads>observableArrayList(SidReads.values());
		sidToRead.setConverter(new EnumToString<SidReads>(bundle));
		sidToRead.setItems(sidReads);
		sidToRead.getSelectionModel().select(emulationSection.getSidNumToRead());

		sid1Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid1Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid1Emulation.valueProperty().bindBidirectional(emulationSection.userEmulationProperty());
		sid1Emulation.setItems(sid1Emulations);

		sid2Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid2Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid2Emulation.valueProperty().bindBidirectional(emulationSection.stereoEmulationProperty());
		sid2Emulation.setItems(sid2Emulations);

		sid3Emulations = FXCollections.<Emulation>observableArrayList(Emulation.values());
		sid3Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid3Emulation.valueProperty().bindBidirectional(emulationSection.thirdEmulationProperty());
		sid3Emulation.setItems(sid3Emulations);

		sid1Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid1Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid1Model.valueProperty().bindBidirectional(emulationSection.userSidModelProperty());
		sid1Model.setItems(sid1Models);

		sid2Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid2Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid2Model.valueProperty().bindBidirectional(emulationSection.stereoSidModelProperty());
		sid2Model.setItems(sid2Models);

		sid3Models = FXCollections.<ChipModel>observableArrayList(ChipModel.values());
		sid3Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid3Model.valueProperty().bindBidirectional(emulationSection.thirdSIDModelProperty());
		sid3Model.setItems(sid3Models);

		defaultModels = FXCollections.<ChipModel>observableArrayList(ChipModel.MOS6581, ChipModel.MOS8580);
		defaultModel.setConverter(new EnumToString<ChipModel>(bundle));
		defaultModel.valueProperty().bindBidirectional(emulationSection.defaultSidModelProperty());
		defaultModel.setItems(defaultModels);

		defaultEmulations = FXCollections.<Emulation>observableArrayList(Emulation.RESID, Emulation.RESIDFP);
		defaultEmulation.setConverter(new EnumToString<Emulation>(bundle));
		defaultEmulation.valueProperty().bindBidirectional(emulationSection.defaultEmulationProperty());
		defaultEmulation.setItems(defaultEmulations);

		boosted8580.selectedProperty().bindBidirectional(emulationSection.digiBoosted8580Property());
		fakeStereo.selectedProperty().bindBidirectional(emulationSection.fakeStereoProperty());

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		Platform.runLater(() -> {
			updateSettingsForTune(util.getPlayer().getTune());
			duringInitialization = false;
		});
	}

	private void updateSettingsForTune(SidTune tune) {
		addFilters(tune, 0, mainFilters, mainFilter);
		addFilters(tune, 1, secondFilters, secondFilter);
		addFilters(tune, 2, thirdFilters, thirdFilter);
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
		secondVolume.setDisable(!second);
		secondBalance.setDisable(!second);
		sid2Emulation.setDisable(!second);
		sid2Model.setDisable(!second);
		secondFilter.setDisable(!second);
		secondFilterCurve.setDisable(!second);
		// 3-SID, only:
		thirdVolume.setDisable(!third);
		thirdBalance.setDisable(!third);
		sid3Emulation.setDisable(!third);
		sid3Model.setDisable(!third);
		thirdFilter.setDisable(!third);
		thirdFilterCurve.setDisable(!third);
		// fake stereo, only:
		sidToRead.setDisable(!(emulationSection.isFakeStereo()));
		// forced stereo or forced 3-SID, only:
		baseAddress.setDisable(!(isForcedStereo || isForced3Sid));
		// forced 3-SID, only:
		thirdAddress.setDisable(!isForced3Sid);
		// fake stereo does not work for HardSID4U
		fakeStereo.setDisable(emulationSection.getEngine() == Engine.HARDSID);
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Emulation() {
		addFilters(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid2Emulation() {
		addFilters(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid3Emulation() {
		addFilters(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setDefaultEmulation() {
		// default emulation has an impact on all emulation settings
		setSid1Emulation();
		setSid2Emulation();
		setSid3Emulation();
	}

	@FXML
	private void setSid1Model() {
		addFilters(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid2Model() {
		addFilters(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setSid3Model() {
		addFilters(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateSIDChipConfiguration();
	}

	@FXML
	private void setDefaultModel() {
		// default chip model has an impact on all chip model settings
		setSid1Model();
		setSid2Model();
		setSid3Model();
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
		setMainFilter();
		setSecondFilter();
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
		setMainFilter();
		setSecondFilter();
		setThirdFilter();
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
		setFilter(0, mainFilter);
		updateSIDChipConfiguration();
		drawFilterCurve(mainFilter, mainFilterCurve);
	}

	@FXML
	private void setSecondFilter() {
		setFilter(1, secondFilter);
		updateSIDChipConfiguration();
		drawFilterCurve(secondFilter, secondFilterCurve);
	}

	@FXML
	private void setThirdFilter() {
		setFilter(2, thirdFilter);
		updateSIDChipConfiguration();
		drawFilterCurve(thirdFilter, thirdFilterCurve);
	}

	/**
	 * Set filter name of the specified SID number according to the current
	 * emulation and chip model
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param filterBox
	 *            filter combo box
	 */
	private void setFilter(int sidNum, ComboBox<String> filterBox) {
		IEmulationSection emulationSection = util.getConfig().getEmulationSection();

		String filterName = filterBox.getSelectionModel().getSelectedItem();
		boolean filterDisabled = "".equals(filterName);
		emulationSection.setFilterEnable(sidNum, !filterDisabled);

		SidTune tune = util.getPlayer().getTune();
		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, sidNum);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, sidNum);
		emulationSection.setFilterName(sidNum, engine, emulation, model, !filterDisabled ? filterName : null);
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
	 * Draw filter curve of the specified SID number and filter name
	 * 
	 * @param filterBox
	 *            filter combo box
	 * @param num
	 *            SID chip number
	 */
	private void drawFilterCurve(final ComboBox<String> filterBox, LineChart<Number, Number> filterCurve) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
		filterCurve.getData().clear();

		SidTune tune = util.getPlayer().getTune();

		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);

		Optional<FilterSection> optFilter = util.getConfig().getFilterSection().stream()
				.filter(f -> f.getName().equals(filterBox.getSelectionModel().getSelectedItem())).findFirst();
		if (optFilter.isPresent()) {
			FilterSection filter = optFilter.get();
			// stereo curve or 3-SID curve currently not used?
			if (!((filterCurve == secondFilterCurve && !second) || (filterCurve == thirdFilterCurve && !third))) {
				for (int fc = 0; fc < SIDChip.FC_MAX; fc += STEP) {
					if (filter.isReSIDFilter6581() || filter.isReSIDFilter8580()) {
						double data = resid_builder.resid.FilterModelConfig.estimateFrequency(filter, fc);
						series.getData().add(new XYChart.Data<Number, Number>(fc, data));
					} else if (filter.isReSIDfpFilter6581() || filter.isReSIDfpFilter8580()) {
						double data = resid_builder.residfp.FilterModelConfig.estimateFrequency(filter, fc);
						series.getData().add(new XYChart.Data<Number, Number>(fc, data));
					}
				}
			}
		}
		filterCurve.getData().add(series);
	}

	/**
	 * Add filters according to the current emulation and chip model of the
	 * currently played tune.
	 * 
	 * @param tune
	 *            currently played tune
	 * @param num
	 *            SID chip number
	 * @param filters
	 *            resulting filter list to add matching filter names to
	 * @param filter
	 *            combo box to select currently selected filter
	 */
	private void addFilters(final SidTune tune, int num, ObservableList<String> filters, ComboBox<String> filter) {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		boolean filterEnable = emulationSection.isFilterEnable(num);

		Engine engine = emulationSection.getEngine();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune, num);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, num);
		String filterName = filterEnable ? emulationSection.getFilterName(num, engine, emulation, model) : null;

		filters.clear();
		if (engine == Engine.NETSID) {
			filters.addAll(TrySetSidModel.getFilterNames(model));
		} else {
			filters.add("");
			for (IFilterSection filterSection : util.getConfig().getFilterSection()) {
				if (emulation.equals(Emulation.RESIDFP)) {
					if (filterSection.isReSIDfpFilter6581() && model == ChipModel.MOS6581) {
						filters.add(filterSection.getName());
					} else if (filterSection.isReSIDfpFilter8580() && model == ChipModel.MOS8580) {
						filters.add(filterSection.getName());
					}
				} else {
					if (filterSection.isReSIDFilter6581() && model == ChipModel.MOS6581) {
						filters.add(filterSection.getName());
					} else if (filterSection.isReSIDFilter8580() && model == ChipModel.MOS8580) {
						filters.add(filterSection.getName());
					}
				}
			}
		}
		if (filterEnable) {
			filter.getSelectionModel().select(filterName);
		} else {
			filter.getSelectionModel().select(0);
		}
	}

}