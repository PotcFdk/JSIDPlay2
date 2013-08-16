package ui.emulationsettings;

import java.net.URL;
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
import libsidplay.C64;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;
import resid_builder.ReSID;
import resid_builder.resid.FilterModelConfig;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.SID;
import sidplay.ini.intf.IFilterSection;
import ui.common.C64Stage;
import ui.events.IPlayTune;
import ui.events.UIEvent;

public class EmulationSettings extends C64Stage {

	/**
	 * Max SID filter FC value.
	 */
	private static final int FC_MAX = 2048;

	/**
	 * Max volume in DB.
	 */
	private static final float MAX_VOLUME_DB = 6.0f;

	private static final int STEP = 10;

	@FXML
	private ComboBox<Object> sid1Model, sid2Model;
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

	private ObservableList<Object> sid1Models = FXCollections
			.<Object> observableArrayList();
	private ObservableList<Object> sid2Models = FXCollections
			.<Object> observableArrayList();
	private ObservableList<String> filters = FXCollections
			.<String> observableArrayList();

	private boolean duringInitialization;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		duringInitialization = true;

		leftVolume.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number newValue) {
				float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
				getConfig().getAudio().setLeftVolume(volumeDb);
				getConsolePlayer().setSIDVolume(0, volumeDb);
			}
		});
		rightVolume.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number newValue) {
				float volumeDb = newValue.floatValue() - MAX_VOLUME_DB;
				getConfig().getAudio().setRightVolume(volumeDb);
				getConsolePlayer().setSIDVolume(1, volumeDb);
			}
		});

		sid1Models.addAll(getBundle().getString("AUTO"), ChipModel.MOS6581,
				ChipModel.MOS8580);
		sid1Model.setItems(sid1Models);
		sid2Models.addAll(getBundle().getString("LIKE_1ST_SID"),
				ChipModel.MOS6581, ChipModel.MOS8580);
		sid2Model.setItems(sid2Models);

		ChipModel userSidModel = getConfig().getEmulation().getUserSidModel();
		sid1Model.getSelectionModel().select(
				userSidModel != null ? userSidModel : getBundle().getString(
						"AUTO"));
		ChipModel stereoSidModel = getConfig().getEmulation()
				.getStereoSidModel();
		sid2Model.getSelectionModel().select(
				stereoSidModel != null ? stereoSidModel : getBundle()
						.getString("LIKE_1ST_SID"));

		baseAddress.setText(String.format("0x%4x", getConfig().getEmulation()
				.getDualSidBase()));
		forceStereo.setSelected(getConfig().getEmulation().isForceStereoTune());
		boosted8580.setSelected(getConfig().getEmulation().isDigiBoosted8580());
		filter.setItems(filters);

		calculateFilterCurve(filter.getSelectionModel().getSelectedItem());

		duringInitialization = false;
	}

	@FXML
	private void setSid1Model() {
		if (sid1Model.getSelectionModel().getSelectedItem()
				.equals(getBundle().getString("AUTO"))) {
			getConfig().getEmulation().setUserSidModel(null);
			if (getPlayer().getTune() != null) {
				switch (getPlayer().getTune().getInfo().sid1Model) {
				case MOS6581:
					setChipModel(ChipModel.MOS6581);
					addFilters(ChipModel.MOS6581);
					break;
				case MOS8580:
					setChipModel(ChipModel.MOS8580);
					addFilters(ChipModel.MOS8580);
					break;
				default:
					setChipModel(getConfig().getEmulation()
							.getDefaultSidModel());
					addFilters(getConfig().getEmulation().getDefaultSidModel());
					break;
				}
			} else {
				ChipModel defaultModel = getConfig().getEmulation()
						.getDefaultSidModel();
				setChipModel(defaultModel);
				addFilters(defaultModel);
			}
		} else {
			ChipModel userSidModel = (ChipModel) sid1Model.getSelectionModel()
					.getSelectedItem();
			getConfig().getEmulation().setUserSidModel(userSidModel);
			setChipModel(userSidModel);
			addFilters(userSidModel);
		}
		getConsolePlayer().updateSidEmulation();
	}

	@FXML
	private void setSid2Model() {
		if (sid2Model.getSelectionModel().getSelectedItem()
				.equals(getBundle().getString("LIKE_1ST_SID"))) {
			getConfig().getEmulation().setStereoSidModel(null);
		} else {
			ChipModel stereoSidModel = (ChipModel) sid2Model
					.getSelectionModel().getSelectedItem();
			getConfig().getEmulation().setStereoSidModel(stereoSidModel);
		}
		getConsolePlayer().updateSidEmulation();
	}

	@FXML
	private void setBaseAddress() {
		getConfig().getEmulation().setDualSidBase(
				Integer.decode(baseAddress.getText()));
		restart();
	}

	@FXML
	private void doForceStereo() {
		getConfig().getEmulation().setForceStereoTune(forceStereo.isSelected());
		restart();
	}

	@FXML
	private void setDigiBoost() {
		boolean selected = boosted8580.isSelected();
		getConfig().getEmulation().setDigiBoosted8580(selected);
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

		getConfig().getEmulation().setFilter(!filterDisabled);
		if (!filterDisabled) {
			final SIDEmu sid = getPlayer().getC64().getSID(0);
			if (sid != null) {
				final ChipModel model = sid.getChipModel();
				if (model == ChipModel.MOS6581) {
					getConfig().getEmulation().setFilter6581(filterName);
				} else {
					getConfig().getEmulation().setFilter8580(filterName);
				}
			}
		}

		IFilterSection[] filters = new IFilterSection[2];
		for (IFilterSection filter : getConfig().getFilter()) {
			if (filters[0] == null
					&& filter.getName().equals(
							getConfig().getEmulation().getFilter6581())
					&& filter.getFilter8580CurvePosition() == 0) {
				filters[0] = filter;
			} else if (filters[1] == null
					&& filter.getName().equals(
							getConfig().getEmulation().getFilter8580())
					&& filter.getFilter8580CurvePosition() != 0) {
				filters[1] = filter;
			}
		}

		for (int i = 0; i < C64.MAX_SIDS; i++) {
			final SIDEmu resid = getPlayer().getC64().getSID(i);
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
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public boolean switchToVideoTab() {
					return false;
				}

				@Override
				public Object getComponent() {
					return null;
				}

				@Override
				public SidTune getSidTune() {
					return getPlayer().getTune() != null ? getPlayer()
							.getTune() : null;
				}
				
			});
		}
	}

	private void calculateFilterCurve(final String filterName) {
		IFilterSection filterSid1 = null;
		for (final IFilterSection filter : getConfig().getFilter()) {
			if (filter.getName().equals(filterName)) {
				filterSid1 = filter;
				break;
			}
		}

		if (filterSid1 != null) {
			XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			series.setName(getBundle().getString("FILTERCURVE_TITLE"));
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
		final SIDEmu sid = getPlayer().getC64().getSID(num);
		if (sid instanceof ReSID) {
			return ((ReSID) sid).sid();
		} else {
			return null;
		}
	}

	private void addFilters(final ChipModel model) {
		final boolean enable = getConfig().getEmulation().isFilter();
		String item = null;
		if (enable) {
			if (model == ChipModel.MOS6581) {
				item = getConfig().getEmulation().getFilter6581();
			} else if (model == ChipModel.MOS8580) {
				item = getConfig().getEmulation().getFilter8580();
			}
		}
		filters.clear();
		filters.add("");

		if (model != null) {
			for (IFilterSection filter : getConfig().getFilter()) {
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

	@Override
	public void notify(final UIEvent event) {
		if (event.isOfType(IPlayTune.class)) {
			final IPlayTune ifObj = (IPlayTune) event.getUIEventImpl();
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					ChipModel userSidModel = getConfig().getEmulation()
							.getUserSidModel();
					sid1Model.getSelectionModel().select(
							userSidModel != null ? userSidModel : getBundle()
									.getString("AUTO"));
					if (userSidModel != null) {
						addFilters(userSidModel);
					} else {
						SidTune sidTune = ifObj.getSidTune();
						if (sidTune != null) {
							switch (sidTune.getInfo().sid1Model) {
							case MOS6581:
								addFilters(ChipModel.MOS6581);
								break;
							case MOS8580:
								addFilters(ChipModel.MOS8580);
								break;
							default:
								addFilters(getConfig().getEmulation()
										.getDefaultSidModel());
								break;
							}
						} else {
							addFilters(getConfig().getEmulation()
									.getDefaultSidModel());
						}
					}
				}
			});
		}
	}

}