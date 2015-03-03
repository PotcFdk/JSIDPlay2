package ui.emulationsettings;

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
import libsidplay.Player;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.player.State;
import libsidplay.sidtune.SidTune;
import resid_builder.resid.FilterModelConfig;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;
import ui.common.C64Window;
import ui.entities.config.EmulationSection;

public class EmulationSettings extends C64Window {

	protected final class EmulationChange implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable,
				State oldValue, State newValue) {
			if (newValue == State.RUNNING) {
				Platform.runLater(() -> {
					initSidModel(0);
					initSidModel(1);
					initSidModel(2);
					EmulationSection emulation = util.getConfig()
							.getEmulation();
					ChipModel model = ChipModel.getChipModel(emulation, util
							.getPlayer().getTune(), 0);
					addFilters(model, 0, mainFilters, mainFilter);
					ChipModel stereoModel = ChipModel.getChipModel(emulation,
							util.getPlayer().getTune(), 1);
					addFilters(stereoModel, 1, secondFilters, secondFilter);
					ChipModel thirdModel = ChipModel.getChipModel(emulation,
							util.getPlayer().getTune(), 2);
					addFilters(thirdModel, 2, thirdFilters, thirdFilter);

					enableStereoSettings();
				});
			}
		}
	}

	/**
	 * Max SID filter FC value.
	 */
	private static final int FC_MAX = 2048;

	/**
	 * Max volume in DB.
	 */
	private static final float MAX_VOLUME_DB = 6.0f;

	private static final int STEP = 3;

	@FXML
	protected ComboBox<Object> sid1Model, sid2Model, sid3Model, sid1Emulation,
			sid2Emulation, sid3Emulation;
	@FXML
	protected ComboBox<ChipModel> defaultModel;
	@FXML
	protected ComboBox<Emulation> defaultEmulation;
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

	private boolean boost8580Enabled;

	private ObservableList<Object> sid1Emulations, sid2Emulations,
			sid3Emulations, sid1Models, sid2Models, sid3Models;
	private ObservableList<ChipModel> defaultModels;
	private ObservableList<Emulation> defaultEmulations;
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

		EmulationSection emulationSection = util.getConfig().getEmulation();

		boost8580Enabled = emulationSection.isDigiBoosted8580();

		mainFilters = FXCollections.<String> observableArrayList();
		mainFilter.setItems(mainFilters);
		secondFilters = FXCollections.<String> observableArrayList();
		secondFilter.setItems(secondFilters);
		thirdFilters = FXCollections.<String> observableArrayList();
		thirdFilter.setItems(thirdFilters);

		mainBalance.setValue(util.getConfig().getAudio().getMainBalance());
		mainBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float balance = newValue.floatValue();
					util.getConfig().getAudio().setMainBalance(balance);
					util.getPlayer().configureSIDBuilder(
							b -> b.setBalance(0, util.getConfig().getAudio()));
				});
		secondBalance.setValue(util.getConfig().getAudio().getSecondBalance());
		secondBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float balance = newValue.floatValue();
					util.getConfig().getAudio().setSecondBalance(balance);
					util.getPlayer().configureSIDBuilder(
							b -> b.setBalance(1, util.getConfig().getAudio()));
				});
		thirdBalance.setValue(util.getConfig().getAudio().getThirdBalance());
		thirdBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float balance = newValue.floatValue();
					util.getConfig().getAudio().setThirdBalance(balance);
					util.getPlayer().configureSIDBuilder(
							b -> b.setBalance(2, util.getConfig().getAudio()));
				});

		mainVolume.setValue(util.getConfig().getAudio().getMainVolume()
				+ MAX_VOLUME_DB);
		mainVolume
				.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float volumeDb = newValue.floatValue()
									- MAX_VOLUME_DB;
							util.getConfig().getAudio().setMainVolume(volumeDb);
							util.getPlayer().configureSIDBuilder(
									(b) -> b.setVolume(0, util.getConfig()
											.getAudio()));
						});
		secondVolume.setValue(util.getConfig().getAudio().getSecondVolume()
				+ MAX_VOLUME_DB);
		secondVolume.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float volumeDb = newValue.floatValue()
									- MAX_VOLUME_DB;
							util.getConfig().getAudio()
									.setSecondVolume(volumeDb);
							util.getPlayer().configureSIDBuilder(
									(b) -> b.setVolume(1, util.getConfig()
											.getAudio()));
						});
		thirdVolume.setValue(util.getConfig().getAudio().getThirdVolume()
				+ MAX_VOLUME_DB);
		thirdVolume.valueProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							float volumeDb = newValue.floatValue()
									- MAX_VOLUME_DB;
							util.getConfig().getAudio()
									.setThirdVolume(volumeDb);
							util.getPlayer().configureSIDBuilder(
									(b) -> b.setVolume(2, util.getConfig()
											.getAudio()));
						});

		stereoModes = FXCollections.<String> observableArrayList();
		stereoModes.addAll(util.getBundle().getString("AUTO"), util.getBundle()
				.getString("FAKE_STEREO"),
				util.getBundle().getString("STEREO"), util.getBundle()
						.getString("3SID"));
		stereoMode.setItems(stereoModes);

		int sidBase = SidTune.getSIDAddress(emulationSection, util.getPlayer()
				.getTune(), 0);
		int dualSidBase = emulationSection.getDualSidBase();
		int thirdSidBase = emulationSection.getThirdSIDBase();
		if (emulationSection.isForceStereoTune()) {
			if (dualSidBase == sidBase) {
				stereoMode.getSelectionModel().select(
						util.getBundle().getString("FAKE_STEREO"));
			} else if (thirdSidBase != 0) {
				stereoMode.getSelectionModel().select(
						util.getBundle().getString("3SID"));
			} else {
				stereoMode.getSelectionModel().select(
						util.getBundle().getString("STEREO"));
			}
		} else {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("AUTO"));
		}

		baseAddress.setText(String.format("0x%4x", dualSidBase));
		thirdAddress.setText(String.format("0x%4x", thirdSidBase));

		sidReads = FXCollections.<String> observableArrayList();
		sidReads.addAll(util.getBundle().getString("FIRST_SID"), util
				.getBundle().getString("SECOND_SID"), util.getBundle()
				.getString("THIRD_SID"));
		sidToRead.setItems(sidReads);
		sidToRead.getSelectionModel()
				.select(emulationSection.getSidNumToRead());

		sid1Emulations = FXCollections.<Object> observableArrayList();
		sid1Emulations.addAll(util.getBundle().getString("AUTO"),
				Emulation.RESID, Emulation.RESIDFP);
		sid1Emulation.setItems(sid1Emulations);
		sid2Emulations = FXCollections.<Object> observableArrayList();
		sid2Emulations.addAll(util.getBundle().getString("AUTO"),
				Emulation.RESID, Emulation.RESIDFP);
		sid2Emulation.setItems(sid2Emulations);
		sid3Emulations = FXCollections.<Object> observableArrayList();
		sid3Emulations.addAll(util.getBundle().getString("AUTO"),
				Emulation.RESID, Emulation.RESIDFP);
		sid3Emulation.setItems(sid3Emulations);

		sid1Models = FXCollections.<Object> observableArrayList();
		sid1Models.addAll(util.getBundle().getString("AUTO"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid1Model.setItems(sid1Models);
		sid2Models = FXCollections.<Object> observableArrayList();
		sid2Models.addAll(util.getBundle().getString("AUTO"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid2Model.setItems(sid2Models);
		sid3Models = FXCollections.<Object> observableArrayList();
		sid3Models.addAll(util.getBundle().getString("AUTO"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid3Model.setItems(sid3Models);
		defaultModels = FXCollections.<ChipModel> observableArrayList();
		defaultModels.addAll(ChipModel.MOS6581, ChipModel.MOS8580);
		defaultModel.setItems(defaultModels);

		defaultEmulations = FXCollections.<Emulation> observableArrayList();
		defaultEmulations.addAll(Emulation.RESID, Emulation.RESIDFP);
		defaultEmulation.setItems(defaultEmulations);

		initSidModel(0);
		Emulation userEmulation = emulationSection.getUserEmulation();
		sid1Emulation.getSelectionModel().select(
				userEmulation != null ? userEmulation : util.getBundle()
						.getString("AUTO"));
		initSidModel(1);
		Emulation stereoEmulation = emulationSection.getStereoEmulation();
		sid2Emulation.getSelectionModel().select(
				stereoEmulation != null ? stereoEmulation : util.getBundle()
						.getString("AUTO"));
		initSidModel(2);
		Emulation thirdEmulation = emulationSection.getThirdEmulation();
		sid3Emulation.getSelectionModel().select(
				thirdEmulation != null ? thirdEmulation : util.getBundle()
						.getString("AUTO"));
		ChipModel defautSidModel = emulationSection.getDefaultSidModel();
		defaultModel.getSelectionModel().select(defautSidModel);
		Emulation defaultSidEmulation = emulationSection.getDefaultEmulation();
		defaultEmulation.getSelectionModel().select(defaultSidEmulation);

		boosted8580.setSelected(boost8580Enabled);

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		enableStereoSettings();

		duringInitialization = false;
		calculateFilterCurve(mainFilter.getSelectionModel().getSelectedItem(),
				0);
		calculateFilterCurve(
				secondFilter.getSelectionModel().getSelectedItem(), 1);
		calculateFilterCurve(thirdFilter.getSelectionModel().getSelectedItem(),
				2);

	}

	private void initSidModel(int sidNum) {
		EmulationSection emulation = util.getConfig().getEmulation();
		ChipModel userSidModel;
		switch (sidNum) {
		case 0:
			userSidModel = emulation.getUserSidModel();
			sid1Model.getSelectionModel().select(
					userSidModel != null ? userSidModel : util.getBundle()
							.getString("AUTO"));
			break;
		case 1:
			userSidModel = emulation.getStereoSidModel();
			sid2Model.getSelectionModel().select(
					userSidModel != null ? userSidModel : util.getBundle()
							.getString("AUTO"));
			break;
		case 2:
			userSidModel = emulation.getThirdSIDModel();
			sid3Model.getSelectionModel().select(
					userSidModel != null ? userSidModel : util.getBundle()
							.getString("AUTO"));
			break;
		default:
			throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");
		}
	}

	private void enableStereoSettings() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		int sidBase = SidTune.getSIDAddress(emulationSection, util.getPlayer()
				.getTune(), 0);
		int dualSidBase = SidTune.getSIDAddress(emulationSection, util
				.getPlayer().getTune(), 1);
		boolean second = SidTune.isSIDUsed(emulationSection, util.getPlayer()
				.getTune(), 1);
		boolean third = SidTune.isSIDUsed(emulationSection, util.getPlayer()
				.getTune(), 2);
		// stereo, only:
		mainBalance.setDisable(!second);
		secondVolume.setDisable(!second);
		secondBalance.setDisable(!second);
		sid2Emulation.setDisable(!second);
		sid2Model.setDisable(!second);
		secondFilter.setDisable(!second);
		secondFilterCurve.setDisable(!second);
		thirdVolume.setDisable(!third);
		thirdBalance.setDisable(!third);
		sid3Emulation.setDisable(!third);
		sid3Model.setDisable(!third);
		thirdFilter.setDisable(!third);
		thirdFilterCurve.setDisable(!third);
		// fake stereo, only:
		sidToRead.setDisable(!(second && sidBase == dualSidBase));
		// forced stereo or forced 3-SID, only:
		baseAddress
				.setDisable(!(second && emulationSection.isForceStereoTune() || (third && emulationSection
						.isForce3SIDTune())));
		// forced 3-SID, only:
		thirdAddress.setDisable(!(third && emulationSection.isForce3SIDTune()));
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Emulation() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		if (sid1Emulation.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulationSection.setUserEmulation(null);
		} else {
			Emulation userEmulation = (Emulation) sid1Emulation
					.getSelectionModel().getSelectedItem();
			emulationSection.setUserEmulation(userEmulation);
		}
		setSid1Model();
	}

	@FXML
	private void setSid2Emulation() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		if (sid2Emulation.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulationSection.setStereoEmulation(null);
		} else {
			Emulation userEmulation = (Emulation) sid2Emulation
					.getSelectionModel().getSelectedItem();
			emulationSection.setStereoEmulation(userEmulation);
		}
		setSid2Model();
	}

	@FXML
	private void setSid3Emulation() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		if (sid3Emulation.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulationSection.setThirdEmulation(null);
		} else {
			Emulation userEmulation = (Emulation) sid3Emulation
					.getSelectionModel().getSelectedItem();
			emulationSection.setThirdEmulation(userEmulation);
		}
		setSid3Model();
	}

	@FXML
	private void setSid1Model() {
		EmulationSection emulation = util.getConfig().getEmulation();
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulation.setUserSidModel(null);
			ChipModel model = ChipModel.getChipModel(emulation, util
					.getPlayer().getTune(), 0);
			addFilters(model, 0, mainFilters, mainFilter);
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			emulation.setUserSidModel(userSidModel);
			addFilters(userSidModel, 0, mainFilters, mainFilter);
		}
		updateChipModels();
	}

	@FXML
	private void setSid2Model() {
		EmulationSection emulation = util.getConfig().getEmulation();
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulation.setStereoSidModel(null);
			ChipModel stereoModel = ChipModel.getChipModel(emulation, util
					.getPlayer().getTune(), 1);
			addFilters(stereoModel, 1, secondFilters, secondFilter);
		} else {
			ChipModel stereoSidModel = (ChipModel) sid2Model
					.getSelectionModel().getSelectedItem();
			emulation.setStereoSidModel(stereoSidModel);
			addFilters(stereoSidModel, 1, secondFilters, secondFilter);
		}
		updateChipModels();
	}

	@FXML
	private void setSid3Model() {
		EmulationSection emulation = util.getConfig().getEmulation();
		if (sid3Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulation.setThirdSIDModel(null);
			ChipModel thirdModel = ChipModel.getChipModel(emulation, util
					.getPlayer().getTune(), 2);
			addFilters(thirdModel, 2, thirdFilters, thirdFilter);
		} else {
			ChipModel thirdSidModel = (ChipModel) sid3Model.getSelectionModel()
					.getSelectedItem();
			emulation.setThirdSIDModel(thirdSidModel);
			addFilters(thirdSidModel, 2, thirdFilters, thirdFilter);
		}
		updateChipModels();
	}

	@FXML
	private void setDefaultModel() {
		EmulationSection emulation = util.getConfig().getEmulation();
		ChipModel defaultSidModel = (ChipModel) defaultModel
				.getSelectionModel().getSelectedItem();
		emulation.setDefaultSidModel(defaultSidModel);
		ChipModel model = ChipModel.getChipModel(emulation, util.getPlayer()
				.getTune(), 0);
		addFilters(model, 0, mainFilters, mainFilter);
		ChipModel stereoModel = ChipModel.getChipModel(emulation, util
				.getPlayer().getTune(), 1);
		addFilters(stereoModel, 1, secondFilters, secondFilter);
		ChipModel thirdModel = ChipModel.getChipModel(emulation, util
				.getPlayer().getTune(), 2);
		addFilters(thirdModel, 2, thirdFilters, thirdFilter);
		updateChipModels();
	}

	@FXML
	private void setDefaultEmulation() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		Emulation defaultSidEmulation = (Emulation) defaultEmulation
				.getSelectionModel().getSelectedItem();
		emulationSection.setDefaultEmulation(defaultSidEmulation);
		// default SID model has an impact on all chip model settings
		setSid1Model();
		setSid2Model();
		setSid3Model();
	}

	@FXML
	private void setBaseAddress() {
		EmulationSection emulationSection = util.getConfig().getEmulation();
		Integer decode = Integer.decode(baseAddress.getText());
		emulationSection.setDualSidBase(decode);

		String thirdSid = util.getBundle().getString("3SID");
		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		int sidBase = SidTune.getSIDAddress(emulationSection, util.getPlayer()
				.getTune(), 0);
		if (decode == sidBase) {
			if (!stereoMode.getSelectionModel().getSelectedItem()
					.equals(fakeStereo)) {
				stereoMode.getSelectionModel().select(fakeStereo);
				return;
			}
		} else if (!(stereoMode.getSelectionModel().getSelectedItem()
				.equals(stereo) || stereoMode.getSelectionModel()
				.getSelectedItem().equals(thirdSid))) {
			stereoMode.getSelectionModel().select(stereo);
			baseAddress.setText(String.format("0x%4x", decode));
			return;
		}
		updateChipModels();
	}

	@FXML
	private void setThirdAddress() {
		Integer decode = Integer.decode(thirdAddress.getText());
		util.getConfig().getEmulation().setThirdSIDBase(decode);

		String thirdSid = util.getBundle().getString("3SID");

		if (!stereoMode.getSelectionModel().getSelectedItem().equals(thirdSid)) {
			stereoMode.getSelectionModel().select(thirdSid);
			thirdAddress.setText(String.format("0x%4x", decode));
			return;
		}
		updateChipModels();
	}

	@FXML
	private void setStereoMode() {
		String thirdSid = util.getBundle().getString("3SID");
		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		if (stereoMode.getSelectionModel().getSelectedItem().equals(fakeStereo)) {
			util.getConfig().getEmulation().setForceStereoTune(true);
			util.getConfig().getEmulation().setForce3SIDTune(false);
			util.getConfig().getEmulation().setDualSidBase(0xd400);
			baseAddress.setText("0xd400");
		} else if (stereoMode.getSelectionModel().getSelectedItem()
				.equals(thirdSid)) {
			util.getConfig().getEmulation().setForceStereoTune(true);
			util.getConfig().getEmulation().setForce3SIDTune(true);
			util.getConfig().getEmulation().setDualSidBase(0xd420);
			util.getConfig().getEmulation().setThirdSIDBase(0xd440);
			baseAddress.setText("0xd420");
			thirdAddress.setText("0xd440");
		} else if (stereoMode.getSelectionModel().getSelectedItem()
				.equals(stereo)) {
			util.getConfig().getEmulation().setForceStereoTune(true);
			util.getConfig().getEmulation().setForce3SIDTune(false);
			util.getConfig().getEmulation().setDualSidBase(0xd420);
			baseAddress.setText("0xd420");
		} else {
			util.getConfig().getEmulation().setForceStereoTune(false);
			util.getConfig().getEmulation().setForce3SIDTune(false);
		}
		updateChipModels();
	}

	@FXML
	private void setSidToRead() {
		int sidNumToRead = sidToRead.getSelectionModel().getSelectedIndex();
		util.getConfig().getEmulation().setSidNumToRead(sidNumToRead);
	}

	@FXML
	private void setDigiBoost() {
		boost8580Enabled = boosted8580.isSelected();
		util.getConfig().getEmulation().setDigiBoosted8580(boost8580Enabled);
		util.getPlayer().configureSIDs(
				(num, sid) -> sid.input(boost8580Enabled ? sid
						.getInputDigiBoost() : 0));
	}

	@FXML
	private void setMainFilter() {
		setFilter(0, mainFilter, sid1Model);
	}

	@FXML
	private void setSecondFilter() {
		setFilter(1, secondFilter, sid2Model);
	}

	@FXML
	private void setThirdFilter() {
		setFilter(2, thirdFilter, sid3Model);
	}

	private void setFilter(int sidNum, ComboBox<String> filterBox,
			ComboBox<Object> chipModelBox) {
		final String filterName = filterBox.getSelectionModel()
				.getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulationSection = util.getConfig().getEmulation();
		emulationSection.setFilterEnable(sidNum, !filterDisabled);

		ChipModel model;
		if (chipModelBox.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(emulationSection, util.getPlayer()
					.getTune(), sidNum);
		} else {
			model = (ChipModel) chipModelBox.getSelectionModel()
					.getSelectedItem();
		}
		Emulation emulation = Emulation.getEmulation(emulationSection, util
				.getPlayer().getTune(), sidNum);

		emulationSection.setFilterName(sidNum, emulation, model, filterName);

		updateChipModels();
		calculateFilterCurve(filterName, sidNum);
	}

	private void updateChipModels() {
		if (!duringInitialization) {
			util.getPlayer().createOrUpdateSIDs();
			enableStereoSettings();
		}
	}

	private void calculateFilterCurve(final String filterName, int num) {
		if (!duringInitialization) {
			IFilterSection filter = null;
			for (final IFilterSection filterSection : util.getConfig()
					.getFilter()) {
				if (filterSection.getName().equals(filterName)) {
					filter = filterSection;
					break;
				}
			}

			boolean secondSidUsed = SidTune.isSIDUsed(util.getConfig()
					.getEmulation(), util.getPlayer().getTune(), 1);
			boolean thirdSidUsed = SidTune.isSIDUsed(util.getConfig()
					.getEmulation(), util.getPlayer().getTune(), 2);

			LineChart<Number, Number> filterCurve;
			switch (num) {
			case 0:
				filterCurve = mainFilterCurve;
				break;
			case 1:
				filterCurve = secondFilterCurve;
				break;
			case 2:
				filterCurve = thirdFilterCurve;
				break;
			default:
				throw new RuntimeException("Maximum supported SIDS exceeded!");
			}
			XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
			filterCurve.getData().clear();
			if (filter != null && !(num == 1 && !secondSidUsed)
					&& !(num == 2 && !thirdSidUsed)) {
				if (filter.isReSIDFilter6581()) {
					double dacZero = FilterModelConfig.getDacZero(filter
							.getFilter6581CurvePosition());
					for (int i = 0; i < FC_MAX; i += STEP) {
						series.getData()
								.add(new XYChart.Data<Number, Number>(i,
										(int) FilterModelConfig
												.estimateFrequency(dacZero, i)));
					}
				} else if (filter.isReSIDFilter8580()) {
					for (int i = 0; i < FC_MAX; i += STEP) {
						series.getData()
								.add(new XYChart.Data<Number, Number>(
										i,
										(int) (i
												* filter.getFilter8580CurvePosition() / (FC_MAX - 1))));
					}
				} else if (filter.isReSIDfpFilter6581()
						|| filter.isReSIDfpFilter8580()) {
					for (int i = 0; i < FC_MAX; i += STEP) {
						series.getData()
								.add(new XYChart.Data<Number, Number>(
										i,
										(int) resid_builder.residfp.FilterModelConfig
												.estimateFrequency(filter, i)));
					}
				}
				filterCurve.getData().add(series);
			}
		}
	}

	private void addFilters(final ChipModel model, int num,
			ObservableList<String> filters, ComboBox<String> filter) {
		boolean filterEnable = util.getConfig().getEmulation()
				.isFilterEnable(num);
		Emulation emulation = Emulation.getEmulation(util.getConfig()
				.getEmulation(), util.getPlayer().getTune(), num);
		String item = null;
		if (filterEnable) {
			item = util.getConfig().getEmulation()
					.getFilterName(num, emulation, model);
		}

		filters.clear();
		filters.add("");
		for (IFilterSection filterSection : util.getConfig().getFilter()) {
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
			filter.getSelectionModel().select(item);
		} else {
			filter.getSelectionModel().select(0);
		}
	}

}