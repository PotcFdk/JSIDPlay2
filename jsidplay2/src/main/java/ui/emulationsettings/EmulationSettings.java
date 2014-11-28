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
	private ComboBox<String> filter, stereoFilter;
	@FXML
	private TextField baseAddress;
	@FXML
	private CheckBox forceStereo, boosted8580;
	@FXML
	private Slider leftVolume, rightVolume;
	@FXML
	private LineChart<Number, Number> filterCurve, stereoFilterCurve;

	private ObservableList<Object> sid1Models;
	private ObservableList<Object> sid2Models;
	private ObservableList<ChipModel> defaultModels;
	private ObservableList<String> filters, stereoFilters;

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

		baseAddress.setText(String.format("0x%4x", util.getConfig()
				.getEmulation().getDualSidBase()));
		forceStereo.setSelected(util.getConfig().getEmulation()
				.isForceStereoTune());
		boosted8580.setSelected(util.getConfig().getEmulation()
				.isDigiBoosted8580());

		calculateFilterCurve(filter.getSelectionModel().getSelectedItem(), false);
		calculateFilterCurve(stereoFilter.getSelectionModel().getSelectedItem(), true);

		emulationChange = new EmulationChange();
		util.getPlayer().stateProperty().addListener(emulationChange);

		duringInitialization = false;
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
			ChipModel stereoModel = ChipModel.getStereoModel(util.getConfig(), util
					.getPlayer().getTune());
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
		util.getConfig().getEmulation()
				.setDualSidBase(Integer.decode(baseAddress.getText()));
		restart();
	}

	@FXML
	private void doForceStereo() {
		util.getConfig().getEmulation()
				.setForceStereoTune(forceStereo.isSelected());
		restart();
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
		final String filterName = stereoFilter.getSelectionModel().getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		IEmulationSection emulation = util.getConfig().getEmulation();
		emulation.setFilter(!filterDisabled);

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
		util.getPlayer().configureSIDs((num, sid) -> {
			sid.setFilter(util.getConfig(), num != 0);
			sid.setFilterEnable(util.getConfig().getEmulation().isFilter());
		});
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	private void calculateFilterCurve(final String filterName, boolean isStereo) {
		IFilterSection filterSid1 = null;
		for (final IFilterSection filter : util.getConfig().getFilter()) {
			if (filter.getName().equals(filterName)) {
				filterSid1 = filter;
				break;
			}
		}

		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
		(isStereo?stereoFilterCurve:filterCurve).getData().clear();
		if (filterSid1 != null) {
			if (filterSid1.isReSIDFilter6581()) {
				double dacZero = FilterModelConfig.getDacZero(filterSid1
						.getFilter6581CurvePosition());
				for (int i = 0; i < FC_MAX; i += STEP) {
					series.getData().add(
							new XYChart.Data<Number, Number>(i,
									(int) FilterModelConfig.estimateFrequency(
											dacZero, i)));
				}
			} else if (filterSid1.isReSIDFilter8580()) {
				for (int i = 0; i < FC_MAX; i += STEP) {
					series.getData()
							.add(new XYChart.Data<Number, Number>(
									i,
									(int) (i
											* filterSid1
													.getFilter8580CurvePosition() / (FC_MAX - 1))));
				}
			} else if (filterSid1.isReSIDfpFilter6581()
					|| filterSid1.isReSIDfpFilter8580()) {
				for (int i = 0; i < FC_MAX; i += STEP) {
					series.getData()
							.add(new XYChart.Data<Number, Number>(
									i,
									(int) residfp_builder.resid.FilterModelConfig
											.estimateFrequency(filterSid1, i)));
				}
			}
			(isStereo?stereoFilterCurve:filterCurve).getData().add(series);
		}
	}

	private void addFilters(final ChipModel model, boolean isStereo) {
		final boolean enable = util.getConfig().getEmulation().isFilter();
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

		(isStereo?stereoFilters:filters).clear();
		(isStereo?stereoFilters:filters).add("");
		for (IFilterSection filter : util.getConfig().getFilter()) {
			if (util.getConfig().getEmulation().getEmulation()
					.equals(Emulation.RESIDFP)) {
				if (filter.isReSIDfpFilter6581() && model == ChipModel.MOS6581) {
					(isStereo?stereoFilters:filters).add(filter.getName());
				} else if (filter.isReSIDfpFilter8580()
						&& model == ChipModel.MOS8580) {
					(isStereo?stereoFilters:filters).add(filter.getName());
				}
			} else {
				if (filter.isReSIDFilter6581() && model == ChipModel.MOS6581) {
					(isStereo?stereoFilters:filters).add(filter.getName());
				} else if (filter.isReSIDFilter8580()
						&& model == ChipModel.MOS8580) {
					(isStereo?stereoFilters:filters).add(filter.getName());
				}
			}
		}

		if (enable) {
			(isStereo?stereoFilter:filter).getSelectionModel().select(item);
		} else {
			(isStereo?stereoFilter:filter).getSelectionModel().select(0);
		}
	}

}