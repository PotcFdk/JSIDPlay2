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
import resid_builder.resid.FilterModelConfig;
import sidplay.audio.AudioConfig;
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
					EmulationSection emulation = util.getConfig()
							.getEmulation();
					ChipModel userSidModel = emulation.getUserSidModel();
					sid1Model.getSelectionModel().select(
							userSidModel != null ? userSidModel : util
									.getBundle().getString("AUTO"));
					ChipModel stereoSidModel = emulation.getStereoSidModel();
					sid2Model.getSelectionModel().select(
							stereoSidModel != null ? stereoSidModel : util
									.getBundle().getString("AUTO"));
					ChipModel thirdSidModel = emulation.getThirdSIDModel();
					sid3Model.getSelectionModel().select(
							thirdSidModel != null ? thirdSidModel : util
									.getBundle().getString("AUTO"));
					ChipModel model = ChipModel.getChipModel(emulation, util
							.getPlayer().getTune(), 0);
					addFilters(model, 0);
					ChipModel stereoModel = ChipModel.getChipModel(emulation,
							util.getPlayer().getTune(), 1);
					addFilters(stereoModel, 1);
					ChipModel thirdModel = ChipModel.getChipModel(emulation,
							util.getPlayer().getTune(), 2);
					addFilters(thirdModel, 2);

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
	protected ComboBox<Object> sid1Model, sid2Model, sid3Model;
	@FXML
	protected ComboBox<ChipModel> defaultModel;
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

	private ObservableList<Object> sid1Models;
	private ObservableList<Object> sid2Models;
	private ObservableList<Object> sid3Models;
	private ObservableList<ChipModel> defaultModels;
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

		boost8580Enabled = util.getConfig().getEmulation().isDigiBoosted8580();

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
		mainVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setMainVolume(volumeDb);
					util.getPlayer().configureSIDBuilder(
							(b) -> b.setMixerVolume(0, util.getConfig()
									.getAudio()));
				});
		secondVolume.setValue(util.getConfig().getAudio().getSecondVolume()
				+ MAX_VOLUME_DB);
		secondVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setSecondVolume(volumeDb);
					util.getPlayer().configureSIDBuilder(
							(b) -> b.setMixerVolume(1, util.getConfig()
									.getAudio()));
				});
		thirdVolume.setValue(util.getConfig().getAudio().getThirdVolume()
				+ MAX_VOLUME_DB);
		thirdVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setThirdVolume(volumeDb);
					util.getPlayer().configureSIDBuilder(
							(b) -> b.setMixerVolume(2, util.getConfig()
									.getAudio()));
				});

		stereoModes = FXCollections.<String> observableArrayList();
		stereoModes.addAll(util.getBundle().getString("AUTO"), util.getBundle()
				.getString("FAKE_STEREO"),
				util.getBundle().getString("STEREO"), util.getBundle()
						.getString("3SID"));
		stereoMode.setItems(stereoModes);

		int dualSidBase = util.getConfig().getEmulation().getDualSidBase();
		int thirdSidBase = util.getConfig().getEmulation().getThirdSIDBase();
		if (util.getConfig().getEmulation().isForceStereoTune()) {
			if (dualSidBase == 0xd400) {
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
		sidToRead.getSelectionModel().select(
				util.getConfig().getEmulation().getSidNumToRead());

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

		ChipModel userSidModel = util.getConfig().getEmulation()
				.getUserSidModel();
		sid1Model.getSelectionModel().select(
				userSidModel != null ? userSidModel : util.getBundle()
						.getString("AUTO"));
		ChipModel stereoSidModel = util.getConfig().getEmulation()
				.getStereoSidModel();
		sid2Model.getSelectionModel().select(
				stereoSidModel != null ? stereoSidModel : util.getBundle()
						.getString("AUTO"));
		ChipModel thirdSidModel = util.getConfig().getEmulation()
				.getThirdSIDModel();
		sid3Model.getSelectionModel().select(
				thirdSidModel != null ? thirdSidModel : util.getBundle()
						.getString("AUTO"));
		ChipModel defautSidModel = util.getConfig().getEmulation()
				.getDefaultSidModel();
		defaultModel.getSelectionModel().select(defautSidModel);

		boosted8580.setSelected(boost8580Enabled);

		calculateFilterCurve(mainFilter.getSelectionModel().getSelectedItem(),
				0);
		calculateFilterCurve(
				secondFilter.getSelectionModel().getSelectedItem(), 1);
		calculateFilterCurve(thirdFilter.getSelectionModel().getSelectedItem(),
				2);

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		enableStereoSettings();

		duringInitialization = false;
	}

	private void enableStereoSettings() {
		EmulationSection emulation = util.getConfig().getEmulation();
		boolean second = AudioConfig.isSIDUsed(emulation, util.getPlayer()
				.getTune(), 1);
		boolean third = AudioConfig.isSIDUsed(emulation, util.getPlayer()
				.getTune(), 2);
		// stereo, only:
		mainBalance.setDisable(!second);
		secondVolume.setDisable(!second);
		secondBalance.setDisable(!second);
		sid2Model.setDisable(!second);
		secondFilter.setDisable(!second);
		secondFilterCurve.setDisable(!second);
		thirdVolume.setDisable(!third);
		thirdBalance.setDisable(!third);
		sid3Model.setDisable(!third);
		thirdFilter.setDisable(!third);
		thirdFilterCurve.setDisable(!third);
		// forced stereo, only:
		sidToRead.setDisable(!(second || third));
		baseAddress
				.setDisable(!(second && (emulation.isForceStereoTune() || (third && emulation
						.isForce3SIDTune()))));
		thirdAddress.setDisable(!(third && emulation.isForce3SIDTune()));
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Model() {
		EmulationSection emulation = util.getConfig().getEmulation();
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			emulation.setUserSidModel(null);
			ChipModel model = ChipModel.getChipModel(emulation, util
					.getPlayer().getTune(), 0);
			addFilters(model, 0);
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			emulation.setUserSidModel(userSidModel);
			addFilters(userSidModel, 0);
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
			addFilters(stereoModel, 1);
		} else {
			ChipModel stereoSidModel = (ChipModel) sid2Model
					.getSelectionModel().getSelectedItem();
			emulation.setStereoSidModel(stereoSidModel);
			addFilters(stereoSidModel, 1);
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
			addFilters(thirdModel, 2);
		} else {
			ChipModel thirdSidModel = (ChipModel) sid3Model.getSelectionModel()
					.getSelectedItem();
			emulation.setThirdSIDModel(thirdSidModel);
			addFilters(thirdSidModel, 2);
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
		addFilters(model, 0);
		ChipModel stereoModel = ChipModel.getChipModel(emulation, util
				.getPlayer().getTune(), 1);
		addFilters(stereoModel, 1);
		ChipModel thirdModel = ChipModel.getChipModel(emulation, util
				.getPlayer().getTune(), 2);
		addFilters(thirdModel, 2);
		updateChipModels();
	}

	@FXML
	private void setBaseAddress() {
		Integer decode = Integer.decode(baseAddress.getText());
		util.getConfig().getEmulation().setDualSidBase(decode);

		String thirdSid = util.getBundle().getString("3SID");
		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		if (decode == 0xd400) {
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
		restart();
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
		restart();
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
		restart();
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
		final String filterName = mainFilter.getSelectionModel()
				.getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setFilter(!filterDisabled);

		ChipModel model;
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(emulation, util.getPlayer()
					.getTune(), 0);
		} else {
			model = (ChipModel) sid1Model.getSelectionModel().getSelectedItem();
		}
		if (util.getConfig().getEmulation().getEmulation()
				.equals(Emulation.RESIDFP)) {
			if (model == ChipModel.MOS6581) {
				emulation.setReSIDfpFilter6581(filterName);
			} else {
				emulation.setReSIDfpFilter8580(filterName);
			}
		} else {
			if (model == ChipModel.MOS6581) {
				emulation.setFilter6581(filterName);
			} else {
				emulation.setFilter8580(filterName);
			}
		}

		updateChipModels();
		if (!duringInitialization) {
			calculateFilterCurve(filterName, 0);
		}
	}

	@FXML
	private void setSecondFilter() {
		final String filterName = secondFilter.getSelectionModel()
				.getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setStereoFilter(!filterDisabled);

		ChipModel model;
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(emulation, util.getPlayer()
					.getTune(), 1);
		} else {
			model = (ChipModel) sid2Model.getSelectionModel().getSelectedItem();
		}
		if (util.getConfig().getEmulation().getEmulation()
				.equals(Emulation.RESIDFP)) {
			if (model == ChipModel.MOS6581) {
				emulation.setReSIDfpStereoFilter6581(filterName);
			} else {
				emulation.setReSIDfpStereoFilter8580(filterName);
			}
		} else {
			if (model == ChipModel.MOS6581) {
				emulation.setStereoFilter6581(filterName);
			} else {
				emulation.setStereoFilter8580(filterName);
			}
		}

		updateChipModels();
		if (!duringInitialization) {
			calculateFilterCurve(filterName, 1);
		}
	}

	@FXML
	private void setThirdFilter() {
		final String filterName = thirdFilter.getSelectionModel()
				.getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setThirdSIDFilter(!filterDisabled);

		ChipModel model;
		if (sid3Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(emulation, util.getPlayer()
					.getTune(), 2);
		} else {
			model = (ChipModel) sid3Model.getSelectionModel().getSelectedItem();
		}
		if (util.getConfig().getEmulation().getEmulation()
				.equals(Emulation.RESIDFP)) {
			if (model == ChipModel.MOS6581) {
				emulation.setReSIDfp3rdSIDFilter6581(filterName);
			} else {
				emulation.setReSIDfp3rdSIDFilter8580(filterName);
			}
		} else {
			if (model == ChipModel.MOS6581) {
				emulation.setThirdSIDFilter6581(filterName);
			} else {
				emulation.setThirdSIDFilter8580(filterName);
			}
		}

		updateChipModels();
		if (!duringInitialization) {
			calculateFilterCurve(filterName, 2);
		}
	}

	private void updateChipModels() {
		util.getPlayer().updateSIDs();
		util.getPlayer().configureSIDs((num, sid) -> {
			sid.setFilter(util.getConfig(), num);
			sid.setFilterEnable(util.getConfig().getEmulation(), num);
			sid.input(boost8580Enabled ? sid.getInputDigiBoost() : 0);
		});
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	private void calculateFilterCurve(final String filterName, int num) {
		IFilterSection filter = null;
		for (final IFilterSection filterSection : util.getConfig().getFilter()) {
			if (filterSection.getName().equals(filterName)) {
				filter = filterSection;
				break;
			}
		}

		boolean secondSidUsed = AudioConfig.isSIDUsed(util.getConfig()
				.getEmulation(), util.getPlayer().getTune(), 1);
		boolean thirdSidUsed = AudioConfig.isSIDUsed(util.getConfig()
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
					series.getData().add(
							new XYChart.Data<Number, Number>(i,
									(int) FilterModelConfig.estimateFrequency(
											dacZero, i)));
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
									(int) residfp_builder.resid.FilterModelConfig
											.estimateFrequency(filter, i)));
				}
			}
			filterCurve.getData().add(series);
		}
	}

	private void addFilters(final ChipModel model, int num) {
		boolean filterEnable;
		ObservableList<String> filters;
		ComboBox<String> filter;
		switch (num) {
		case 0:
			filterEnable = util.getConfig().getEmulation().isFilter();
			filters = mainFilters;
			filter = mainFilter;
			break;
		case 1:
			filterEnable = util.getConfig().getEmulation().isStereoFilter();
			filters = secondFilters;
			filter = secondFilter;
			break;
		case 2:
			filterEnable = util.getConfig().getEmulation().isThirdSIDFilter();
			filters = thirdFilters;
			filter = thirdFilter;
			break;
		default:
			throw new RuntimeException("Maximum supported SIDS exceeded!");
		}
		String item = null;
		if (filterEnable) {
			if (util.getConfig().getEmulation().getEmulation()
					.equals(Emulation.RESIDFP)) {
				switch (num) {
				case 0:
					if (model == ChipModel.MOS6581) {
						item = util.getConfig().getEmulation()
								.getReSIDfpFilter6581();
					} else if (model == ChipModel.MOS8580) {
						item = util.getConfig().getEmulation()
								.getReSIDfpFilter8580();
					}
					break;
				case 1:
					if (model == ChipModel.MOS6581) {
						item = util.getConfig().getEmulation()
								.getReSIDfpStereoFilter6581();
					} else if (model == ChipModel.MOS8580) {
						item = util.getConfig().getEmulation()
								.getReSIDfpStereoFilter8580();
					}
				case 2:
					if (model == ChipModel.MOS6581) {
						item = util.getConfig().getEmulation()
								.getReSIDfp3rdSIDFilter6581();
					} else if (model == ChipModel.MOS8580) {
						item = util.getConfig().getEmulation()
								.getReSIDfp3rdSIDFilter8580();
					}
					break;
				default:
					throw new RuntimeException(
							"Maximum supported SIDS exceeded!");
				}
			} else {
				if (model == ChipModel.MOS6581) {
					item = util.getConfig().getEmulation().getFilter6581();
				} else if (model == ChipModel.MOS8580) {
					item = util.getConfig().getEmulation().getFilter8580();
				}
			}
		}

		filters.clear();
		filters.add("");
		for (IFilterSection filterSection : util.getConfig().getFilter()) {
			if (util.getConfig().getEmulation().getEmulation()
					.equals(Emulation.RESIDFP)) {
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