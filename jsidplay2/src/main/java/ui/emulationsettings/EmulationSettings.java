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
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.common.SIDChip;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.DoubleToString;
import ui.common.EnumToString;
import ui.entities.config.EmulationSection;
import ui.entities.config.FilterSection;

public class EmulationSettings extends C64Window {

	protected final class EmulationChange implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable,
				State oldValue, State newValue) {
			if (newValue == State.START) {
				Platform.runLater(() -> {
					updateSettingsForTune(util.getPlayer().getTune());
				});
			}
		}
	}

	private static final int STEP = 3;

	@FXML
	private ComboBox<Emulation> sid1Emulation, sid2Emulation, sid3Emulation,
			defaultEmulation;
	@FXML
	private ComboBox<ChipModel> sid1Model, sid2Model, sid3Model, defaultModel;
	@FXML
	private ComboBox<String> stereoMode, mainFilter, secondFilter, thirdFilter,
			sidToRead;
	@FXML
	private TextField baseAddress, thirdAddress;
	@FXML
	private CheckBox boosted8580;
	@FXML
	private Slider mainVolume, secondVolume, thirdVolume, mainBalance,
			secondBalance, thirdBalance;
	@FXML
	private LineChart<Number, Number> mainFilterCurve, secondFilterCurve,
			thirdFilterCurve;

	private ObservableList<Emulation> sid1Emulations, sid2Emulations,
			sid3Emulations, defaultEmulations;
	private ObservableList<ChipModel> sid1Models, sid2Models, sid3Models,
			defaultModels;
	private ObservableList<String> stereoModes, sidReads, mainFilters,
			secondFilters, thirdFilters;

	private ChangeListener<State> emulationChange;

	private boolean duringInitialization;

	public EmulationSettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		duringInitialization = true;

		ResourceBundle bundle = util.getBundle();
		EmulationSection emulationSection = util.getConfig()
				.getEmulationSection();

		mainFilters = FXCollections.<String> observableArrayList();
		mainFilter.setItems(mainFilters);
		secondFilters = FXCollections.<String> observableArrayList();
		secondFilter.setItems(secondFilters);
		thirdFilters = FXCollections.<String> observableArrayList();
		thirdFilter.setItems(thirdFilters);

		mainBalance.setValue(util.getConfig().getAudioSection()
				.getMainBalance());
		mainBalance.setLabelFormatter(new DoubleToString(1));
		mainBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setMainBalance(newValue.floatValue());
					util.getPlayer().configureMixer(
							m -> m.setBalance(0, newValue.floatValue()));
				});
		secondBalance.setValue(util.getConfig().getAudioSection()
				.getSecondBalance());
		secondBalance.setLabelFormatter(new DoubleToString(1));
		secondBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setSecondBalance(newValue.floatValue());
					util.getPlayer().configureMixer(
							m -> m.setBalance(1, newValue.floatValue()));
				});
		thirdBalance.setValue(util.getConfig().getAudioSection()
				.getThirdBalance());
		thirdBalance.setLabelFormatter(new DoubleToString(1));
		thirdBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setThirdBalance(newValue.floatValue());
					util.getPlayer().configureMixer(
							m -> m.setBalance(2, newValue.floatValue()));
				});

		mainVolume.setValue(util.getConfig().getAudioSection().getMainVolume());
		mainVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setMainVolume(newValue.floatValue());
					util.getPlayer().configureMixer(
							m -> m.setVolume(0, newValue.floatValue()));
				});
		secondVolume.setValue(util.getConfig().getAudioSection()
				.getSecondVolume());
		secondVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setSecondVolume(newValue.floatValue());
					util.getPlayer().configureMixer(
							b -> b.setVolume(1, newValue.floatValue()));
				});
		thirdVolume.setValue(util.getConfig().getAudioSection()
				.getThirdVolume());
		thirdVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					util.getConfig().getAudioSection()
							.setThirdVolume(newValue.floatValue());
					util.getPlayer().configureMixer(
							b -> b.setVolume(2, newValue.floatValue()));
				});

		stereoModes = FXCollections.<String> observableArrayList();
		stereoModes.addAll(bundle.getString("AUTO"),
				bundle.getString("FAKE_STEREO"), bundle.getString("STEREO"),
				bundle.getString("3SID"));
		stereoMode.setItems(stereoModes);

		baseAddress.setText(String.format("0x%4x",
				emulationSection.getDualSidBase()));
		thirdAddress.setText(String.format("0x%4x",
				emulationSection.getThirdSIDBase()));

		sidReads = FXCollections.<String> observableArrayList();
		sidReads.addAll(bundle.getString("FIRST_SID"),
				bundle.getString("SECOND_SID"), bundle.getString("THIRD_SID"));
		sidToRead.setItems(sidReads);
		sidToRead.getSelectionModel()
				.select(emulationSection.getSidNumToRead());

		sid1Emulations = FXCollections
				.<Emulation> observableArrayList(Emulation.values());
		sid1Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid1Emulation.setItems(sid1Emulations);
		sid1Emulation.getSelectionModel().select(
				emulationSection.getUserEmulation());

		sid2Emulations = FXCollections
				.<Emulation> observableArrayList(Emulation.values());
		sid2Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid2Emulation.setItems(sid2Emulations);
		sid2Emulation.getSelectionModel().select(
				emulationSection.getStereoEmulation());

		sid3Emulations = FXCollections
				.<Emulation> observableArrayList(Emulation.values());
		sid3Emulation.setConverter(new EnumToString<Emulation>(bundle));
		sid3Emulation.setItems(sid3Emulations);
		sid3Emulation.getSelectionModel().select(
				emulationSection.getThirdEmulation());

		sid1Models = FXCollections.<ChipModel> observableArrayList(ChipModel
				.values());
		sid1Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid1Model.setItems(sid1Models);
		sid1Model.getSelectionModel()
				.select(emulationSection.getUserSidModel());

		sid2Models = FXCollections.<ChipModel> observableArrayList(ChipModel
				.values());
		sid2Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid2Model.setItems(sid2Models);
		sid2Model.getSelectionModel().select(
				emulationSection.getStereoSidModel());

		sid3Models = FXCollections.<ChipModel> observableArrayList(ChipModel
				.values());
		sid3Model.setConverter(new EnumToString<ChipModel>(bundle));
		sid3Model.setItems(sid3Models);
		sid3Model.getSelectionModel().select(
				emulationSection.getThirdSIDModel());

		defaultModels = FXCollections.<ChipModel> observableArrayList();
		defaultModels.addAll(ChipModel.MOS6581, ChipModel.MOS8580);
		defaultModel.setConverter(new EnumToString<ChipModel>(bundle));
		defaultModel.setItems(defaultModels);
		defaultModel.getSelectionModel().select(
				emulationSection.getDefaultSidModel());

		defaultEmulations = FXCollections.<Emulation> observableArrayList();
		defaultEmulations.addAll(Emulation.RESID, Emulation.RESIDFP);
		defaultEmulation.setConverter(new EnumToString<Emulation>(bundle));
		defaultEmulation.setItems(defaultEmulations);
		defaultEmulation.getSelectionModel().select(
				emulationSection.getDefaultEmulation());

		boosted8580.setSelected(emulationSection.isDigiBoosted8580());

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
		EmulationSection emulationSection = util.getConfig()
				.getEmulationSection();
		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);
		int sidBase = SidTune.getSIDAddress(emulationSection, tune, 0);
		int dualSidBase = SidTune.getSIDAddress(emulationSection, tune, 1);
		boolean isForcedStereo = second && emulationSection.isForceStereoTune();
		boolean isFakeStereo = isForcedStereo && dualSidBase == sidBase;
		boolean isForced3Sid = third && emulationSection.isForce3SIDTune();
		if (isFakeStereo) {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("FAKE_STEREO"));
		} else if (isForced3Sid) {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("3SID"));
		} else if (isForcedStereo) {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("STEREO"));
		} else {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("AUTO"));
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
		sidToRead.setDisable(!(isFakeStereo));
		// forced stereo or forced 3-SID, only:
		baseAddress.setDisable(!(isForcedStereo || isForced3Sid));
		// forced 3-SID, only:
		thirdAddress.setDisable(!isForced3Sid);
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Emulation() {
		Emulation emulation = sid1Emulation.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulationSection().setUserEmulation(emulation);
		setSid1Model();
	}

	@FXML
	private void setSid2Emulation() {
		Emulation emulation = sid2Emulation.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulationSection().setStereoEmulation(emulation);
		setSid2Model();
	}

	@FXML
	private void setSid3Emulation() {
		Emulation emulation = sid3Emulation.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulationSection().setThirdEmulation(emulation);
		setSid3Model();
	}

	@FXML
	private void setDefaultEmulation() {
		Emulation emulation = defaultEmulation.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulationSection().setDefaultEmulation(emulation);
		// default emulation has an impact on all emulation settings
		setSid1Emulation();
		setSid2Emulation();
		setSid3Emulation();
	}

	@FXML
	private void setSid1Model() {
		ChipModel chipModel = sid1Model.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulationSection().setUserSidModel(chipModel);
		addFilters(util.getPlayer().getTune(), 0, mainFilters, mainFilter);
		updateChipModels();
	}

	@FXML
	private void setSid2Model() {
		ChipModel chipModel = sid2Model.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulationSection().setStereoSidModel(chipModel);
		addFilters(util.getPlayer().getTune(), 1, secondFilters, secondFilter);
		updateChipModels();
	}

	@FXML
	private void setSid3Model() {
		ChipModel chipModel = sid3Model.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulationSection().setThirdSIDModel(chipModel);
		addFilters(util.getPlayer().getTune(), 2, thirdFilters, thirdFilter);
		updateChipModels();
	}

	@FXML
	private void setDefaultModel() {
		ChipModel chipModel = (ChipModel) defaultModel.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulationSection().setDefaultSidModel(chipModel);
		// default chip model has an impact on all chip model settings
		setSid1Model();
		setSid2Model();
		setSid3Model();
	}

	@FXML
	private void setBaseAddress() {
		Integer dualSidBase = Integer.decode(baseAddress.getText());
		util.getConfig().getEmulationSection().setDualSidBase(dualSidBase);
		enableStereoSettings(util.getPlayer().getTune());
		updateChipModels();
	}

	@FXML
	private void setThirdAddress() {
		Integer decode = Integer.decode(thirdAddress.getText());
		util.getConfig().getEmulationSection().setThirdSIDBase(decode);
		enableStereoSettings(util.getPlayer().getTune());
		updateChipModels();
	}

	@FXML
	private void setStereoMode() {
		String mode = stereoMode.getSelectionModel().getSelectedItem();

		String thirdSid = util.getBundle().getString("3SID");
		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		if (mode.equals(fakeStereo)) {
			util.getConfig().getEmulationSection().setForceStereoTune(true);
			util.getConfig().getEmulationSection().setForce3SIDTune(false);
			util.getConfig().getEmulationSection().setDualSidBase(0xd400);
			baseAddress.setText("0xd400");
		} else if (mode.equals(thirdSid)) {
			util.getConfig().getEmulationSection().setForceStereoTune(true);
			util.getConfig().getEmulationSection().setForce3SIDTune(true);
			util.getConfig().getEmulationSection().setDualSidBase(0xd420);
			util.getConfig().getEmulationSection().setThirdSIDBase(0xd440);
			baseAddress.setText("0xd420");
			thirdAddress.setText("0xd440");
		} else if (mode.equals(stereo)) {
			util.getConfig().getEmulationSection().setForceStereoTune(true);
			util.getConfig().getEmulationSection().setForce3SIDTune(false);
			util.getConfig().getEmulationSection().setDualSidBase(0xd420);
			baseAddress.setText("0xd420");
		} else {
			util.getConfig().getEmulationSection().setForceStereoTune(false);
			util.getConfig().getEmulationSection().setForce3SIDTune(false);
		}
		enableStereoSettings(util.getPlayer().getTune());
		updateChipModels();
		// stereo mode changes has an impact on all filter curves
		drawFilterCurve(mainFilter, mainFilterCurve);
		drawFilterCurve(secondFilter, secondFilterCurve);
		drawFilterCurve(thirdFilter, thirdFilterCurve);
	}

	@FXML
	private void setSidToRead() {
		int sidNumToRead = sidToRead.getSelectionModel().getSelectedIndex();
		util.getConfig().getEmulationSection().setSidNumToRead(sidNumToRead);
	}

	@FXML
	private void setDigiBoost() {
		boolean boost8580Enabled = boosted8580.isSelected();
		util.getConfig().getEmulationSection()
				.setDigiBoosted8580(boost8580Enabled);
		util.getPlayer().configureSIDs(
				(num, sid) -> sid.input(boost8580Enabled ? sid
						.getInputDigiBoost() : 0));
	}

	@FXML
	private void setMainFilter() {
		setFilter(0, mainFilter);
		updateChipModels();
		drawFilterCurve(mainFilter, mainFilterCurve);
	}

	@FXML
	private void setSecondFilter() {
		setFilter(1, secondFilter);
		updateChipModels();
		drawFilterCurve(secondFilter, secondFilterCurve);
	}

	@FXML
	private void setThirdFilter() {
		setFilter(2, thirdFilter);
		updateChipModels();
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
		IEmulationSection emulationSection = util.getConfig()
				.getEmulationSection();

		String filterName = filterBox.getSelectionModel().getSelectedItem();
		boolean filterDisabled = "".equals(filterName);
		emulationSection.setFilterEnable(sidNum, !filterDisabled);

		SidTune tune = util.getPlayer().getTune();
		Emulation emulation = Emulation.getEmulation(emulationSection, tune,
				sidNum);
		ChipModel model = ChipModel
				.getChipModel(emulationSection, tune, sidNum);
		emulationSection.setFilterName(sidNum, emulation, model,
				!filterDisabled ? filterName : null);
	}

	/**
	 * Update SID configuration on-the-fly.
	 */
	private void updateChipModels() {
		if (!duringInitialization) {
			util.getPlayer().getC64().getEventScheduler()
					.scheduleThreadSafe(new Event("Update SIDs") {
						@Override
						public void event() throws InterruptedException {
							util.getPlayer().createOrUpdateSIDs();
						}
					});
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
	private void drawFilterCurve(final ComboBox<String> filterBox,
			LineChart<Number, Number> filterCurve) {
		EmulationSection emulationSection = util.getConfig()
				.getEmulationSection();
		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
		filterCurve.getData().clear();

		SidTune tune = util.getPlayer().getTune();

		boolean second = SidTune.isSIDUsed(emulationSection, tune, 1);
		boolean third = SidTune.isSIDUsed(emulationSection, tune, 2);

		Optional<FilterSection> optFilter = util
				.getConfig()
				.getFilterSection()
				.stream()
				.filter(f -> f.getName().equals(
						filterBox.getSelectionModel().getSelectedItem()))
				.findFirst();
		if (optFilter.isPresent()) {
			FilterSection filter = optFilter.get();
			// stereo curve or 3-SID curve currently not used?
			if (!((filterCurve == secondFilterCurve && !second) || (filterCurve == thirdFilterCurve && !third))) {
				for (int fc = 0; fc < SIDChip.FC_MAX; fc += STEP) {
					if (filter.isReSIDFilter6581()
							|| filter.isReSIDFilter8580()) {
						double data = resid_builder.resid.FilterModelConfig
								.estimateFrequency(filter, fc);
						series.getData()
								.add(new XYChart.Data<Number, Number>(fc,
										(int) data));
					} else if (filter.isReSIDfpFilter6581()
							|| filter.isReSIDfpFilter8580()) {
						double data = resid_builder.residfp.FilterModelConfig
								.estimateFrequency(filter, fc);
						series.getData()
								.add(new XYChart.Data<Number, Number>(fc,
										(int) data));
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
	private void addFilters(final SidTune tune, int num,
			ObservableList<String> filters, ComboBox<String> filter) {
		EmulationSection emulationSection = util.getConfig()
				.getEmulationSection();

		boolean filterEnable = emulationSection.isFilterEnable(num);

		Emulation emulation = Emulation.getEmulation(emulationSection, tune,
				num);
		ChipModel model = ChipModel.getChipModel(emulationSection, tune, num);
		String filterName = filterEnable ? emulationSection.getFilterName(num,
				emulation, model) : null;

		filters.clear();
		filters.add("");
		for (IFilterSection filterSection : util.getConfig().getFilterSection()) {
			if (emulation.equals(Emulation.RESIDFP)) {
				if (filterSection.isReSIDfpFilter6581()
						&& model == ChipModel.MOS6581) {
					filters.add(filterSection.getName());
				} else if (filterSection.isReSIDfpFilter8580()
						&& model == ChipModel.MOS8580) {
					filters.add(filterSection.getName());
				}
			} else {
				if (filterSection.isReSIDFilter6581()
						&& model == ChipModel.MOS6581) {
					filters.add(filterSection.getName());
				} else if (filterSection.isReSIDFilter8580()
						&& model == ChipModel.MOS8580) {
					filters.add(filterSection.getName());
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