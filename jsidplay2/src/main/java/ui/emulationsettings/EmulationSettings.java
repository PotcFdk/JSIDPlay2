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

public class EmulationSettings extends C64Window {

	protected final class EmulationChange implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable,
				State oldValue, State newValue) {
			if (newValue == State.RUNNING) {
				Platform.runLater(() -> {
					ChipModel userSidModel = util.getConfig().getEmulation()
							.getUserSidModel();
					sid1Model.getSelectionModel().select(
							userSidModel != null ? userSidModel : util
									.getBundle().getString("AUTO"));
					ChipModel stereoSidModel = util.getConfig().getEmulation()
							.getStereoSidModel();
					sid2Model.getSelectionModel().select(
							stereoSidModel != null ? stereoSidModel : util
									.getBundle().getString("AUTO"));
					ChipModel model = ChipModel.getChipModel(util.getConfig(),
							util.getPlayer().getTune());
					addFilters(model, false);
					ChipModel stereoModel = ChipModel.getStereoModel(
							util.getConfig(), util.getPlayer().getTune());
					addFilters(stereoModel, true);

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
	protected ComboBox<Object> sid1Model, sid2Model;
	@FXML
	protected ComboBox<ChipModel> defaultModel;
	@FXML
	private ComboBox<String> stereoMode, filter, stereoFilter, sidToRead;
	@FXML
	private TextField baseAddress;
	@FXML
	private CheckBox boosted8580;
	@FXML
	private Slider leftVolume, rightVolume, leftBalance, rightBalance;
	@FXML
	private LineChart<Number, Number> filterCurve, stereoFilterCurve;

	private ObservableList<Object> sid1Models;
	private ObservableList<Object> sid2Models;
	private ObservableList<ChipModel> defaultModels;
	private ObservableList<String> stereoModes, sidReads, filters,
			stereoFilters;

	private ChangeListener<State> emulationChange;

	private boolean duringInitialization;

	public EmulationSettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		duringInitialization = true;

		filters = FXCollections.<String> observableArrayList();
		filter.setItems(filters);
		stereoFilters = FXCollections.<String> observableArrayList();
		stereoFilter.setItems(stereoFilters);

		leftBalance.setValue(util.getConfig().getAudio().getLeftBalance());
		leftBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float balance = newValue.floatValue();
					util.getConfig().getAudio().setLeftBalance(balance);
					util.getPlayer().setBalance(0, balance);
				});
		rightBalance.setValue(util.getConfig().getAudio().getRightBalance());
		rightBalance.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float balance = newValue.floatValue();
					util.getConfig().getAudio().setRightBalance(balance);
					util.getPlayer().setBalance(1, balance);
				});

		leftVolume.setValue(util.getConfig().getAudio().getLeftVolume()
				+ MAX_VOLUME_DB);
		leftVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setLeftVolume(volumeDb);
					util.getPlayer().setMixerVolume(0, volumeDb);
				});
		rightVolume.setValue(util.getConfig().getAudio().getRightVolume()
				+ MAX_VOLUME_DB);
		rightVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setRightVolume(volumeDb);
					util.getPlayer().setMixerVolume(1, volumeDb);
				});

		stereoModes = FXCollections.<String> observableArrayList();
		stereoModes
				.addAll(util.getBundle().getString("AUTO"), util.getBundle()
						.getString("FAKE_STEREO"),
						util.getBundle().getString("STEREO"));
		stereoMode.setItems(stereoModes);

		int dualSidBase = util.getConfig().getEmulation().getDualSidBase();
		if (util.getConfig().getEmulation().isForceStereoTune()) {
			if (dualSidBase == 0xd400) {
				stereoMode.getSelectionModel().select(
						util.getBundle().getString("FAKE_STEREO"));
			} else {
				stereoMode.getSelectionModel().select(
						util.getBundle().getString("STEREO"));
			}
		} else {
			stereoMode.getSelectionModel().select(
					util.getBundle().getString("AUTO"));
		}

		baseAddress.setText(String.format("0x%4x", dualSidBase));

		sidReads = FXCollections.<String> observableArrayList();
		sidReads.addAll(util.getBundle().getString("FIRST_SID"), util
				.getBundle().getString("SECOND_SID"));
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
		ChipModel defautSidModel = util.getConfig().getEmulation()
				.getDefaultSidModel();
		defaultModel.getSelectionModel().select(defautSidModel);

		boosted8580.setSelected(util.getConfig().getEmulation()
				.isDigiBoosted8580());

		calculateFilterCurve(filter.getSelectionModel().getSelectedItem(),
				false);
		calculateFilterCurve(
				stereoFilter.getSelectionModel().getSelectedItem(), true);

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		enableStereoSettings();

		duringInitialization = false;
	}

	private void enableStereoSettings() {
		boolean stereo = AudioConfig.isStereo(util.getConfig(), util
				.getPlayer().getTune());
		// stereo, only:
		rightVolume.setDisable(!stereo);
		leftBalance.setDisable(!stereo);
		rightBalance.setDisable(!stereo);
		sid2Model.setDisable(!stereo);
		stereoFilter.setDisable(!stereo);
		stereoFilterCurve.setDisable(!stereo);
		// forced stereo, only:
		sidToRead.setDisable(!(stereo && util.getConfig().getEmulation()
				.isForceStereoTune()));
		baseAddress.setDisable(!(stereo && util.getConfig().getEmulation()
				.isForceStereoTune()));
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Model() {
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			util.getConfig().getEmulation().setUserSidModel(null);
			ChipModel model = ChipModel.getChipModel(util.getConfig(), util
					.getPlayer().getTune());
			addFilters(model, false);
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			util.getConfig().getEmulation().setUserSidModel(userSidModel);
			addFilters(userSidModel, false);
		}
		updateChipModels();
	}

	@FXML
	private void setSid2Model() {
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			util.getConfig().getEmulation().setStereoSidModel(null);
			ChipModel stereoModel = ChipModel.getStereoModel(util.getConfig(),
					util.getPlayer().getTune());
			addFilters(stereoModel, true);
		} else {
			ChipModel stereoSidModel = (ChipModel) sid2Model
					.getSelectionModel().getSelectedItem();
			util.getConfig().getEmulation().setStereoSidModel(stereoSidModel);
			addFilters(stereoSidModel, true);
		}
		updateChipModels();
	}

	@FXML
	private void setDefaultModel() {
		ChipModel defaultSidModel = (ChipModel) defaultModel
				.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulation().setDefaultSidModel(defaultSidModel);
		ChipModel model = ChipModel.getChipModel(util.getConfig(), util
				.getPlayer().getTune());
		addFilters(model, false);
		ChipModel stereoModel = ChipModel.getStereoModel(util.getConfig(), util
				.getPlayer().getTune());
		addFilters(stereoModel, true);
		updateChipModels();
	}

	@FXML
	private void setBaseAddress() {
		Integer decode = Integer.decode(baseAddress.getText());
		util.getConfig().getEmulation().setDualSidBase(decode);

		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		if (decode == 0xd400) {
			if (!stereoMode.getSelectionModel().getSelectedItem()
					.equals(fakeStereo)) {
				stereoMode.getSelectionModel().select(fakeStereo);
				return;
			}
		} else if (!stereoMode.getSelectionModel().getSelectedItem()
				.equals(stereo)) {
			stereoMode.getSelectionModel().select(stereo);
			baseAddress.setText(String.format("0x%4x",decode));
			return;
		}
		restart();
	}

	@FXML
	private void setStereoMode() {
		String stereo = util.getBundle().getString("STEREO");
		String fakeStereo = util.getBundle().getString("FAKE_STEREO");

		if (stereoMode.getSelectionModel().getSelectedItem().equals(fakeStereo)) {
			util.getConfig().getEmulation().setForceStereoTune(true);
			util.getConfig().getEmulation().setDualSidBase(0xd400);
			baseAddress.setText("0xd400");
		} else if (stereoMode.getSelectionModel().getSelectedItem()
				.equals(stereo)) {
			util.getConfig().getEmulation().setForceStereoTune(true);
			util.getConfig().getEmulation().setDualSidBase(0xd420);
			baseAddress.setText("0xd420");
		} else {
			util.getConfig().getEmulation().setForceStereoTune(false);
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
		boolean selected = boosted8580.isSelected();
		util.getConfig().getEmulation().setDigiBoosted8580(selected);
		util.getPlayer().configureSIDs(
				(num, sid) -> sid.input(selected ? 0x7FF : 0));
	}

	@FXML
	private void setFilter() {
		final String filterName = filter.getSelectionModel().getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setFilter(!filterDisabled);

		ChipModel model;
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(util.getConfig(), util.getPlayer()
					.getTune());
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
			calculateFilterCurve(filterName, false);
		}
	}

	@FXML
	private void setStereoFilter() {
		final String filterName = stereoFilter.getSelectionModel()
				.getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setStereoFilter(!filterDisabled);

		ChipModel model;
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getStereoModel(util.getConfig(), util.getPlayer()
					.getTune());
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
			calculateFilterCurve(filterName, true);
		}
	}

	private void updateChipModels() {
		util.getPlayer().updateSIDs();
		util.getPlayer().configureSIDs(
				(num, sid) -> {
					sid.setFilter(util.getConfig(), num != 0);
					sid.setFilterEnable(num != 0 ? util.getConfig()
							.getEmulation().isStereoFilter() : util.getConfig()
							.getEmulation().isFilter());
				});
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	private void calculateFilterCurve(final String filterName, boolean secondSID) {
		IFilterSection filter = null;
		for (final IFilterSection filterSection : util.getConfig().getFilter()) {
			if (filterSection.getName().equals(filterName)) {
				filter = filterSection;
				break;
			}
		}

		boolean stereo = AudioConfig.isStereo(util.getConfig(), util
				.getPlayer().getTune());

		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
		(secondSID ? stereoFilterCurve : filterCurve).getData().clear();
		if (filter != null && !(secondSID && !stereo)) {
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
											* filter
													.getFilter8580CurvePosition() / (FC_MAX - 1))));
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
			(secondSID ? stereoFilterCurve : filterCurve).getData().add(series);
		}
	}

	private void addFilters(final ChipModel model, boolean isStereo) {
		final boolean enable = isStereo ? util.getConfig().getEmulation()
				.isStereoFilter() : util.getConfig().getEmulation().isFilter();
		String item = null;
		if (enable) {
			if (util.getConfig().getEmulation().getEmulation()
					.equals(Emulation.RESIDFP)) {
				if (model == ChipModel.MOS6581) {
					item = isStereo ? util.getConfig().getEmulation()
							.getReSIDfpStereoFilter6581() : util.getConfig()
							.getEmulation().getReSIDfpFilter6581();
				} else if (model == ChipModel.MOS8580) {
					item = isStereo ? util.getConfig().getEmulation()
							.getReSIDfpStereoFilter8580() : util.getConfig()
							.getEmulation().getReSIDfpFilter8580();
				}
			} else {
				if (model == ChipModel.MOS6581) {
					item = util.getConfig().getEmulation().getFilter6581();
				} else if (model == ChipModel.MOS8580) {
					item = util.getConfig().getEmulation().getFilter8580();
				}
			}
		}

		(isStereo ? stereoFilters : filters).clear();
		(isStereo ? stereoFilters : filters).add("");
		for (IFilterSection filter : util.getConfig().getFilter()) {
			if (util.getConfig().getEmulation().getEmulation()
					.equals(Emulation.RESIDFP)) {
				if (filter.isReSIDfpFilter6581() && model == ChipModel.MOS6581) {
					(isStereo ? stereoFilters : filters).add(filter.getName());
				} else if (filter.isReSIDfpFilter8580()
						&& model == ChipModel.MOS8580) {
					(isStereo ? stereoFilters : filters).add(filter.getName());
				}
			} else {
				if (filter.isReSIDFilter6581() && model == ChipModel.MOS6581) {
					(isStereo ? stereoFilters : filters).add(filter.getName());
				} else if (filter.isReSIDFilter8580()
						&& model == ChipModel.MOS8580) {
					(isStereo ? stereoFilters : filters).add(filter.getName());
				}
			}
		}

		if (enable) {
			(isStereo ? stereoFilter : filter).getSelectionModel().select(item);
		} else {
			(isStereo ? stereoFilter : filter).getSelectionModel().select(0);
		}
	}

}