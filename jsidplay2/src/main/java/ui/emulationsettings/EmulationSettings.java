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
import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;
import resid_builder.ReSID;
import resid_builder.resid.FilterModelConfig;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.SID;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.State;
import sidplay.ini.intf.IFilterSection;
import ui.common.C64Stage;
import ui.entities.config.Configuration;

public class EmulationSettings extends C64Stage {

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
					if (userSidModel != null) {
						addFilters(userSidModel);
					} else {
						SidTune sidTune = util.getPlayer().getTune();
						if (sidTune != null) {
							switch (sidTune.getInfo().sid1Model) {
							case MOS6581:
								addFilters(ChipModel.MOS6581);
								break;
							case MOS8580:
								addFilters(ChipModel.MOS8580);
								break;
							default:
								addFilters(util.getConfig().getEmulation()
										.getDefaultSidModel());
								break;
							}
						} else {
							addFilters(util.getConfig().getEmulation()
									.getDefaultSidModel());
						}
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

	public EmulationSettings(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
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
					util.getConsolePlayer().setSIDVolume(0, volumeDb);
				});
		rightVolume.setValue(util.getConfig().getAudio().getRightVolume()
				+ MAX_VOLUME_DB);
		rightVolume.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
					util.getConfig().getAudio().setRightVolume(volumeDb);
					util.getConsolePlayer().setSIDVolume(1, volumeDb);
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
		util.getConsolePlayer().stateProperty().addListener(emulationChange);

		duringInitialization = false;
	}

	@Override
	public void doClose() {
		util.getConsolePlayer().stateProperty().removeListener(emulationChange);
	}

	@FXML
	private void setSid1Model() {
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(util.getBundle().getString("AUTO"))) {
			util.getConfig().getEmulation().setUserSidModel(null);
			if (util.getPlayer().getTune() != null) {
				switch (util.getPlayer().getTune().getInfo().sid1Model) {
				case MOS6581:
					setChipModel(ChipModel.MOS6581);
					addFilters(ChipModel.MOS6581);
					break;
				case MOS8580:
					setChipModel(ChipModel.MOS8580);
					addFilters(ChipModel.MOS8580);
					break;
				default:
					setChipModel(util.getConfig().getEmulation()
							.getDefaultSidModel());
					addFilters(util.getConfig().getEmulation()
							.getDefaultSidModel());
					break;
				}
			} else {
				ChipModel defaultModel = util.getConfig().getEmulation()
						.getDefaultSidModel();
				setChipModel(defaultModel);
				addFilters(defaultModel);
			}
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			util.getConfig().getEmulation().setUserSidModel(userSidModel);
			setChipModel(userSidModel);
			addFilters(userSidModel);
		}
		util.getConsolePlayer().updateSidEmulation();
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
		util.getConsolePlayer().updateSidEmulation();
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
		final int input = selected ? 0x7FF : 0;
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			SID sid = getSID(i);
			if (sid != null) {
				sid.input(input);
			}
		}
	}

	@FXML
	private void setFilter() {
		final String filterName = filter.getSelectionModel().getSelectedItem();
		final boolean filterDisabled = "".equals(filterName);

		util.getConfig().getEmulation().setFilter(!filterDisabled);
		if (!filterDisabled) {
			final SIDEmu sid = util.getPlayer().getC64().getSID(0);
			if (sid != null) {
				final ChipModel model = sid.getChipModel();
				if (model == ChipModel.MOS6581) {
					util.getConfig().getEmulation().setFilter6581(filterName);
				} else {
					util.getConfig().getEmulation().setFilter8580(filterName);
				}
			}
		}

		IFilterSection[] filters = new IFilterSection[2];
		for (IFilterSection filter : util.getConfig().getFilter()) {
			if (filters[0] == null
					&& filter.getName().equals(
							util.getConfig().getEmulation().getFilter6581())
					&& filter.getFilter8580CurvePosition() == 0) {
				filters[0] = filter;
			} else if (filters[1] == null
					&& filter.getName().equals(
							util.getConfig().getEmulation().getFilter8580())
					&& filter.getFilter8580CurvePosition() != 0) {
				filters[1] = filter;
			}
		}

		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu resid = util.getPlayer().getC64().getSID(i);
			if (resid != null) {
				resid.setFilter(!filterDisabled);
				if (resid instanceof ReSID) {
					((ReSID) resid).filter(filters[0], filters[1]);
				}
			}
		}
		if (!duringInitialization) {
			calculateFilterCurve(filterName);
		}
	}

	private void restart() {
		// replay last tune
		if (!duringInitialization) {
			util.getConsolePlayer().playTune(util.getPlayer().getTune(), null);
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

	private void setChipModel(ChipModel m) {
		for (int i = 0; i < C64.MAX_SIDS; i++) {
			SID sid = getSID(i);
			if (sid != null) {
				sid.setChipModel(m);
			}
		}
	}

	private SID getSID(final int num) {
		final SIDEmu sid = util.getPlayer().getC64().getSID(num);
		if (sid instanceof ReSID) {
			return ((ReSID) sid).sid();
		} else {
			return null;
		}
	}

	protected void addFilters(final ChipModel model) {
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

		if (model != null) {
			for (IFilterSection filter : util.getConfig().getFilter()) {
				if (filter.getFilter8580CurvePosition() != 0
						^ model == ChipModel.MOS6581) {
					filters.add(filter.getName());
				}
			}
		}

		if (enable) {
			filter.getSelectionModel().select(item);
		} else {
			filter.getSelectionModel().select(0);
		}
	}

}