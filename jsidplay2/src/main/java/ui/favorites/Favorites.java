package ui.favorites;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidplay.Player;
import libsidplay.player.State;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.SidDatabase;
import sidplay.ini.IniReader;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileExtensions;

public class Favorites extends Tab implements UIPart {

	public static final String ID = "FAVORITES";
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

	private UIUtil util;

	private FavoritesTab currentlyPlayedFavorites;
	protected Random random = new Random();
	private C64Window window;

	public Favorites(C64Window window, Player player) {
		this.window = window;
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig()
				.getSidplay2();

		// Not already configured, yet?
		if (sidPlay2Section.getHvsc() != null) {
			setSongLengthDatabase(sidPlay2Section.getHvsc());
			setSTIL(sidPlay2Section.getHvsc());
		}

		final int seconds = sidPlay2Section.getDefaultPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60,
				seconds % 60));
		sidPlay2Section.defaultPlayLengthProperty().addListener(
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
		if (util.getConfig().getSidplay2().isLoop()) {
			repeatOne.setSelected(true);
		} else {
			repeatOff.setSelected(true);
		}
		util.getPlayer().stateProperty()
				.addListener((observable, oldValue, newValue) -> {
					if (newValue == State.EXIT) {
						Platform.runLater(() -> playNextTune());
					}
				});
		List<? extends FavoritesSection> favorites = util.getConfig()
				.getFavorites();
		util.getConfig()
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
							((Configuration) util.getConfig())
									.setCurrentFavorite(newValue.getText());
						}
					});
		Platform.runLater(() -> {
			// Initially select last selected tab
			String currentFavorite = ((Configuration) util.getConfig())
					.getCurrentFavorite();
			for (FavoritesSection favorite : favorites) {
				addTab(favorite);
			}
			if (currentFavorite != null) {
				for (Tab tab : favoritesList.getTabs()) {
					if (tab.getText().equals(currentFavorite)) {
						favoritesList.getSelectionModel().select(tab);
					}
				}
			}
		});
		Platform.runLater(() -> {
			favoritesList.getScene().setOnDragOver((event) -> {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			});
			favoritesList.getScene().setOnDragDropped((event) -> {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					FavoritesTab selectedTab = getSelectedTab();
					selectedTab.addFavorites(files);
				}
				event.setDropCompleted(success);
				event.consume();
			});
		});
	}

	@FXML
	private void addFavorites() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TuneFileExtensions.DESCRIPTION,
						TuneFileExtensions.EXTENSIONS));
		final List<File> files = fileDialog
				.showOpenMultipleDialog(favoritesList.getScene().getWindow());
		if (files != null && files.size() > 0) {
			File file = files.get(0);
			util.getConfig().getSidplay2().setLastDirectory(file.getParent());
			FavoritesTab selectedTab = getSelectedTab();
			selectedTab.addFavorites(files);
			renameTab(selectedTab,
					PathUtils.getBaseNameNoExt(file.getParentFile().getName()));
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(FavoritesExtension.DESCRIPTION,
						FavoritesExtension.EXTENSION));
		final File file = fileDialog.showOpenDialog(favoritesList.getScene()
				.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2().setLastDirectory(file.getParent());
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(FavoritesExtension.DESCRIPTION,
						FavoritesExtension.EXTENSION));
		final File file = fileDialog.showSaveDialog(favoritesList.getScene()
				.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2().setLastDirectory(file.getParent());
			File target = new File(file.getParentFile(),
					PathUtils.getBaseNameNoExt(file.getName()) + ".js2");
			try {
				getSelectedTab().saveFavorites(target);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void doEnableSldb() {
		util.getConfig().getSidplay2()
				.setEnableDatabase(enableSldb.isSelected());
		util.getPlayer().getTimer().updateEnd();
	}

	@FXML
	private void playSingleSong() {
		util.getConfig().getSidplay2().setSingle(singleSong.isSelected());
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			util.getConfig().getSidplay2().setDefaultPlayLength(secs);
			util.getPlayer().getTimer().updateEnd();
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void addTab() {
		List<FavoritesSection> favorites = ((Configuration) util.getConfig())
				.getFavorites();
		FavoritesSection favoritesSection = new FavoritesSection();
		favoritesSection.setName(util.getBundle().getString("NEW_TAB"));
		favorites.add(favoritesSection);
	}

	@FXML
	private void renameTab() {
		renameTab(getSelectedTab(), renameTab.getText());
	}

	@FXML
	private void off() {
		((SidPlay2Section) util.getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.PLAYBACK_OFF);
	}

	@FXML
	private void normal() {
		((SidPlay2Section) util.getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.NORMAL);
	}

	@FXML
	private void randomOne() {
		((SidPlay2Section) util.getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.RANDOM_ONE);
	}

	@FXML
	private void randomAll() {
		((SidPlay2Section) util.getConfig().getSidplay2())
				.setPlaybackType(PlaybackType.RANDOM_ALL);
	}

	@FXML
	private void repeatOff() {
		util.getConfig().getSidplay2().setLoop(false);
	}

	@FXML
	private void repeatOne() {
		util.getConfig().getSidplay2().setLoop(true);
	}

	private void setSongLengthDatabase(String hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				SidDatabase.SONGLENGTHS_FILE))) {
			util.getPlayer().setSidDatabase(new SidDatabase(input));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setSTIL(String hvscRoot) {
		try (TFileInputStream input = new TFileInputStream(new TFile(hvscRoot,
				STIL.STIL_FILE))) {
			util.getPlayer().setSTIL(new STIL(input));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private FavoritesTab getSelectedTab() {
		return (FavoritesTab) favoritesList.getSelectionModel()
				.getSelectedItem();
	}

	protected void addTab(final FavoritesSection favoritesSection) {
		final FavoritesTab newTab = new FavoritesTab(this.window,
				util.getPlayer());
		if (favoritesSection.getName() == null) {
			favoritesSection.setName(util.getBundle().getString("NEW_TAB"));
		}
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

		favoritesList.getTabs().add(newTab);
	}

	private void renameTab(FavoritesTab selectedTab, String name) {
		selectedTab.setText(name);
		selectedTab.getFavoritesSection().setName(name);
	}

	protected void playNextTune() {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig()
				.getSidplay2();
		PlaybackType pt = sidPlay2Section.getPlaybackType();

		if (!util.getConfig().getSidplay2().isLoop()) {
			if (pt == PlaybackType.RANDOM_ALL) {
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
					&& util.getPlayer().getTune() != null
					&& pt != PlaybackType.PLAYBACK_OFF) {
				// normal playback
				currentlyPlayedFavorites.playNext();
			}
		}
	}

	void setCurrentlyPlayedFavorites(FavoritesTab currentlyPlayedFavorites) {
		this.currentlyPlayedFavorites = currentlyPlayedFavorites;
	}

	ObservableList<Tab> getFavoriteTabs() {
		return favoritesList.getTabs();
	}
}
