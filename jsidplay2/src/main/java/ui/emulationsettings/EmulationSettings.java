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
import libsidplay.State;
import resid_builder.resid.ChipModel;
import resid_builder.resid.FilterModelConfig;
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
									.getBundle().getString("LIKE_1ST_SID"));
					if (userSidModel != null) {
						addFilters(userSidModel);
					} else {
						ChipModel model = ChipModel.getChipModel(
								util.getConfig(), util.getPlayer().getTune());
						addFilters(model);
					}
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
	private ComboBox<String> filter;
	@FXML
	private TextField baseAddress;
	@FXML
	private CheckBox forceStereo, boosted8580;
	@FXML
	private Slider leftVolume, rightVolume;
	@FXML
	private LineChart<Number, Number> filterCurve;

	private ObservableList<Object> sid1Models;
	private ObservableList<Object> sid2Models;
	private ObservableList<String> filters;

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

		leftVolume.setValue(util.getConfig().getAudio().getLeftVolume()
				+ MAX_VOLUME_DB);
		leftVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setLeftVolume(volumeDb);
					util.getPlayer().setSIDVolume(0, volumeDb);
				});
		rightVolume.setValue(util.getConfig().getAudio().getRightVolume()
				+ MAX_VOLUME_DB);
		rightVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setRightVolume(volumeDb);
					util.getPlayer().setSIDVolume(1, volumeDb);
				});

		sid1Models = FXCollections.<Object> observableArrayList();
		sid1Models.addAll(util.getBundle().getString("AUTO"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid1Model.setItems(sid1Models);
		sid2Models = FXCollections.<Object> observableArrayList();
		sid2Models.addAll(util.getBundle().getString("LIKE_1ST_SID"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid2Model.setItems(sid2Models);

		ChipModel userSidModel = util.getConfig().getEmulation()
				.getUserSidModel();
		sid1Model.getSelectionModel().select(
				userSidModel != null ? userSidModel : util.getBundle()
						.getString("AUTO"));
		ChipModel stereoSidModel = util.getConfig().getEmulation()
				.getStereoSidModel();
		sid2Model.getSelectionModel().select(
				stereoSidModel != null ? stereoSidModel : util.getBundle()
						.getString("LIKE_1ST_SID"));

		baseAddress.setText(String.format("0x%4x", util.getConfig()
				.getEmulation().getDualSidBase()));
		forceStereo.setSelected(util.getConfig().getEmulation()
				.isForceStereoTune());
		boosted8580.setSelected(util.getConfig().getEmulation()
				.isDigiBoosted8580());

		calculateFilterCurve(filter.getSelectionModel().getSelectedItem());

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
			addFilters(model);
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			util.getConfig().getEmulation().setUserSidModel(userSidModel);
			addFilters(userSidModel);
		}
		util.getPlayer().updateChipModel();
		util.getPlayer().setFilter(util.getConfig());
	}

	@FXML
	private void setSid2Model() {
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("LIKE_1ST_SID"))) {
			util.getConfig().getEmulation().setStereoSidModel(null);
		} else {
			ChipModel stereoSidModel = (ChipModel) sid2Model
					.getSelectionModel().getSelectedItem();
			util.getConfig().getEmulation().setStereoSidModel(stereoSidModel);
		}
		util.getPlayer().updateChipModel();
		util.getPlayer().setFilter(util.getConfig());
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
		util.getPlayer().setDigiBoost(selected);
	}

	@FXML
	private void setFilter() {
		final String filterName = filter.getSelectionModel().getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		util.getConfig().getEmulation().setFilter(!filterDisabled);

		ChipModel model;
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			model = ChipModel.getChipModel(util.getConfig(), util.getPlayer()
					.getTune());
		} else {
			model = (ChipModel) sid1Model.getSelectionModel().getSelectedItem();
		}
		if (model == ChipModel.MOS6581) {
			util.getConfig().getEmulation().setFilter6581(filterName);
		} else {
			util.getConfig().getEmulation().setFilter8580(filterName);
		}

		util.getPlayer().updateChipModel();
		util.getPlayer().setFilter(util.getConfig());
		if (!duringInitialization) {
			calculateFilterCurve(filterName);
		}
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getPlayer().playTune(util.getPlayer().getTune(), null);
		}
	}

	private void calculateFilterCurve(final String filterName) {
		IFilterSection filterSid1 = null;
		for (final IFilterSection filter : util.getConfig().getFilter()) {
			if (filter.getName().equals(filterName)) {
				filterSid1 = filter;
				break;
			}
		}

		if (filterSid1 != null) {
			XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			series.setName(util.getBundle().getString("FILTERCURVE_TITLE"));
			if (filterSid1.getFilter6581CurvePosition() != 0) {
				double dacZero = FilterModelConfig.getDacZero(filterSid1
						.getFilter6581CurvePosition());
				for (int i = 0; i < FC_MAX; i += STEP) {
					series.getData().add(
							new XYChart.Data<Number, Number>(i,
									(int) FilterModelConfig.estimateFrequency(
											dacZero, i)));
				}
			} else {
				for (int i = 0; i < FC_MAX; i += STEP) {
					series.getData()
							.add(new XYChart.Data<Number, Number>(
									i,
									(int) (i
											* filterSid1
													.getFilter8580CurvePosition() / (FC_MAX - 1))));
				}
			}
			filterCurve.getData().clear();
			filterCurve.getData().add(series);
		}
	}

	private void addFilters(final ChipModel model) {
		final boolean enable = util.getConfig().getEmulation().isFilter();
		String item = null;
		if (enable) {
			if (model == ChipModel.MOS6581) {
				item = util.getConfig().getEmulation().getFilter6581();
			} else if (model == ChipModel.MOS8580) {
				item = util.getConfig().getEmulation().getFilter8580();
			}
		}

		filters.clear();
		filters.add("");
		for (IFilterSection filter : util.getConfig().getFilter()) {
			if (filter.getFilter8580CurvePosition() != 0
					^ model == ChipModel.MOS6581) {
				filters.add(filter.getName());
			}
		}

		if (enable) {
			filter.getSelectionModel().select(item);
		} else {
			filter.getSelectionModel().select(0);
		}
	}

}