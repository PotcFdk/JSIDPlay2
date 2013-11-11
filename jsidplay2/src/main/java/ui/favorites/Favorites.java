package ui.favorites;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidutils.PathUtils;
import sidplay.consoleplayer.State;
import sidplay.ini.IniReader;
import ui.common.C64Tab;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileExtensions;

public class Favorites extends C64Tab {

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private Button add, remove, selectAll, deselectAll, load, save, saveAs;
	@FXML
	private TabPane favoritesList;
	@FXML
	protected TextField defaultTime, renameTab;
	@FXML
	protected CheckBox enableSldb, singleSong;
	@FXML
	private RadioButton off, normal, randomOne, randomAll, repeatOff,
			repeatOne;

	private FavoritesTab currentlyPlayedFavorites;
	protected Random random = new Random();

	@Override
	public void initialize(URL url, ResourceBundle bundle) {
		if (getPlayer() == null) {
			return;
		}
		SidPlay2Section sidPlay2Section = (SidPlay2Section) getConfig()
				.getSidplay2();

		final int seconds = sidPlay2Section.getPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60,
				seconds % 60));
		sidPlay2Section.playLengthProperty().addListener(
				(observable, oldValue, newValue) -> defaultTime.setText(String
						.format("%02d:%02d", newValue.intValue() / 60,
								newValue.intValue() % 60)));

		enableSldb.setSelected(sidPlay2Section.isEnableDatabase());
		sidPlay2Section.enableDatabaseProperty().addListener(
				(observable, oldValue, newValue) -> enableSldb
						.setSelected(newValue));
		singleSong.setSelected(sidPlay2Section.isSingle());
		sidPlay2Section.singleProperty().addListener(
				(observable, oldValue, newValue) -> singleSong
						.setSelected(newValue));
		PlaybackType pt = sidPlay2Section.getPlaybackType();
		switch (pt) {
		case PLAYBACK_OFF:
			off.setSelected(true);
			break;
		case NORMAL:
			normal.setSelected(true);
			break;
		case RANDOM_ONE:
			randomOne.setSelected(true);
			break;
		case RANDOM_ALL:
			randomAll.setSelected(true);
			break;
		default:
			off.setSelected(true);
			break;
		}
		RepeatType rt = sidPlay2Section.getRepeatType();
		switch (rt) {
		case REPEAT_OFF:
			repeatOff.setSelected(true);
			break;
		case REPEAT_ONE:
			repeatOne.setSelected(true);
			break;
		default:
			repeatOff.setSelected(true);
			break;
		}
		getConsolePlayer().stateProperty().addListener(
				(observable, oldValue, newValue) -> {
					if (newValue == State.EXIT) {
						Platform.runLater(() -> playNextTune());
					}
				});
		List<? extends FavoritesSection> favorites = getConfig().getFavorites();
		for (FavoritesSection favorite : favorites) {
			addTab(favorite);
		}
		getConfig()
				.getObservableFavorites()
				.addListener(
						(ListChangeListener.Change<? extends FavoritesSection> change) -> {
							while (change.next()) {
								if (change.wasPermutated()
										|| change.wasUpdated()) {
									continue;
								}
								if (change.wasAdded()) {
									List<? extends FavoritesSection> addedSubList = change
											.getAddedSubList();
									for (FavoritesSection favoritesSection : addedSubList) {
										addTab(favoritesSection);
									}
								}
							}
						});
		favoritesList
				.getSelectionModel()
				.selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> {
					// Save last selected tab
						if (newValue != null) {
							((Configuration) getConfig())
									.setCurrentFavorite(newValue.getText());
						}
					});
		// Initially select last selected tab
		String currentFavorite = ((Configuration) getConfig())
				.getCurrentFavorite();
		if (currentFavorite != null) {
			for (Tab tab : favoritesList.getTabs()) {
				if (tab.getText().equals(currentFavorite)) {
					favoritesList.getSelectionModel().select(tab);
				}
			}
		}
	}

	@FXML
	private void addFavorites() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TuneFileExtensions.DESCRIPTION,
						TuneFileExtensions.EXTENSIONS));
		final List<File> files = fileDialog
				.showOpenMultipleDialog(favoritesList.getScene().getWindow());
		if (files != null && files.size() > 0) {
			File file = files.get(0);
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			FavoritesTab selectedTab = getSelectedTab();
			selectedTab.addFavorites(files);
			renameTab(selectedTab,
					PathUtils.getBaseNameNoExt(file.getParentFile()));
		}
	}

	@FXML
	private void removeFavorites() {
		getSelectedTab().removeSelectedFavorites();
	}

	@FXML
	private void selectAllFavorites() {
		getSelectedTab().selectAllFavorites();
	}

	@FXML
	private void clearSelection() {
		getSelectedTab().clearSelection();
	}

	@FXML
	private void loadFavorites() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(FavoritesExtension.DESCRIPTION,
						FavoritesExtension.EXTENSION));
		final File file = fileDialog.showOpenDialog(favoritesList.getScene()
				.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				getSelectedTab().loadFavorites(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void saveFavoritesAs() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(FavoritesExtension.DESCRIPTION,
						FavoritesExtension.EXTENSION));
		final File file = fileDialog.showSaveDialog(favoritesList.getScene()
				.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			File target = new File(file.getParentFile(),
					PathUtils.getBaseNameNoExt(file) + ".js2");
			// then load the favorites
			try {
				getSelectedTab().saveFavorites(target);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void doEnableSldb() {
		getConfig().getSidplay2().setEnableDatabase(enableSldb.isSelected());
		getConsolePlayer().setSLDb(enableSldb.isSelected());
	}

	@FXML
	private void playSingleSong() {
		getConfig().getSidplay2().setSingle(singleSong.isSelected());
		getConsolePlayer().getTrack().setSingle(singleSong.isSelected());
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			getConsolePlayer().getTimer().setDefaultLength(secs);
			getConfig().getSidplay2().setPlayLength(secs);
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(getBundle().getString("DEFAULT_LENGTH_FORMAT"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void addTab() {
		List<FavoritesSection> favorites = ((Configuration) getConfig())
				.getFavorites();
		FavoritesSection favoritesSection = new FavoritesSection();
		favoritesSection.setName(getBundle().getString("NEW_TAB"));
		favorites.add(favoritesSection);
	}

	@FXML
	private void renameTab() {
		renameTab(getSelectedTab(), renameTab.getText());
	}

	@FXML
	private void off() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.PLAYBACK_OFF);
	}

	@FXML
	private void normal() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.NORMAL);
	}

	@FXML
	private void randomOne() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.RANDOM_ONE);
	}

	@FXML
	private void randomAll() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.RANDOM_ALL);
	}

	@FXML
	private void repeatOff() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setRepeatType(RepeatType.REPEAT_OFF);
	}

	@FXML
	private void repeatOne() {
		((SidPlay2Section) getConfig().getSidplay2())
				.setRepeatType(RepeatType.REPEAT_ONE);
	}

	private FavoritesTab getSelectedTab() {
		return (FavoritesTab) favoritesList.getSelectionModel()
				.getSelectedItem();
	}

	protected void addTab(final FavoritesSection favoritesSection) {
		final FavoritesTab newTab = new FavoritesTab();
		newTab.setText(favoritesSection.getName());
		newTab.restoreColumns(favoritesSection);
		newTab.setClosable(favoritesList.getTabs().size() != 0);
		newTab.setOnClosed(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				newTab.removeAllFavorites();
			}

		});

		newTab.setFavorites(this);
		newTab.setPlayer(getPlayer());
		newTab.setConsolePlayer(getConsolePlayer());
		newTab.setConfig(getConfig());
		newTab.initialize(null, null);

		favoritesList.getTabs().add(newTab);
	}

	private void renameTab(FavoritesTab selectedTab, String name) {
		selectedTab.setText(name);
		selectedTab.getFavoritesSection().setName(name);
	}

	protected void playNextTune() {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) getConfig()
				.getSidplay2();
		PlaybackType pt = sidPlay2Section.getPlaybackType();
		RepeatType rt = sidPlay2Section.getRepeatType();

		if (rt == RepeatType.REPEAT_ONE) {
			// repeat one tune
			getConsolePlayer().playTune(getPlayer().getTune(), null);
		} else if (pt == PlaybackType.RANDOM_ALL) {
			// random all favorites tab
			favoritesList.getSelectionModel().select(
					Math.abs(random.nextInt(Integer.MAX_VALUE))
							% favoritesList.getTabs().size());
			currentlyPlayedFavorites = getSelectedTab();
			currentlyPlayedFavorites.playNextRandom();
		} else if (currentlyPlayedFavorites != null
				&& pt == PlaybackType.RANDOM_ONE) {
			// random one favorites tab
			currentlyPlayedFavorites.playNextRandom();
		} else if (currentlyPlayedFavorites != null
				&& pt != PlaybackType.PLAYBACK_OFF) {
			// normal playback
			currentlyPlayedFavorites
					.playNext(getPlayer().getTune().getInfo().file);
		}
	}

	void setCurrentlyPlayedFavorites(FavoritesTab currentlyPlayedFavorites) {
		this.currentlyPlayedFavorites = currentlyPlayedFavorites;
	}

	ObservableList<Tab> getFavoriteTabs() {
		return favoritesList.getTabs();
	}
}
