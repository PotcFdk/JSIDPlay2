package ui.favorites;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Random;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.TimeToStringConverter;
import ui.common.UIPart;
import ui.download.DownloadThread;
import ui.download.ProgressListener;
import ui.entities.config.FavoritesSection;
import ui.entities.config.OnlineSection;
import ui.entities.config.SidPlay2Section;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileExtensions;

public class Favorites extends C64VBox implements UIPart {

	public static final String ID = "FAVORITES";

	@FXML
	private Button autoConfiguration;
	@FXML
	private Button add, remove, selectAll, deselectAll, load, save, saveAs;
	@FXML
	private TabPane favoritesList;
	@FXML
	protected TextField renameTab, fadeInTime, fadeOutTime;
	@FXML
	private RadioButton off, normal, randomOne, randomAll, randomHVSC, repeatOff, repeatOne;

	private FavoritesTab currentlyPlayedFavorites;
	protected Random random = new Random();

	private PropertyChangeListener nextTuneListener;

	public Favorites() {
		super();
	}

	public Favorites(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		nextTuneListener = event -> {
			if (event.getNewValue() == State.END) {
				Platform.runLater(() -> playNextTune());
			}
		};
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		// Not already configured, yet?
		if (sidplay2Section.getHvscFile() != null) {
			setSongLengthDatabase(sidplay2Section.getHvsc());
			setSTIL(sidplay2Section.getHvsc());
		}
		autoConfiguration.setDisable(sidplay2Section.getHvscFile() == null);
		sidplay2Section.hvscProperty()
				.addListener((obj, o, n) -> autoConfiguration.setDisable(sidplay2Section.getHvscFile() == null));

		Bindings.bindBidirectional(fadeInTime.textProperty(), sidplay2Section.fadeInTimeProperty(),
				new TimeToStringConverter());
		sidplay2Section.fadeInTimeProperty()
				.addListener((obj, o, n) -> util.checkTextField(fadeInTime, () -> n.intValue() != -1,
						() -> util.getPlayer().getTimer().updateEnd(), "FADE_IN_LENGTH_TIP", "FADE_IN_LENGTH_FORMAT"));

		Bindings.bindBidirectional(fadeOutTime.textProperty(), sidplay2Section.fadeOutTimeProperty(),
				new TimeToStringConverter());
		sidplay2Section.fadeOutTimeProperty()
				.addListener((obj, o, n) -> util.checkTextField(fadeOutTime, () -> n.intValue() != -1,
						() -> util.getPlayer().getTimer().updateEnd(), "FADE_OUT_LENGTH_TIP",
						"FADE_OUT_LENGTH_FORMAT"));

		PlaybackType pt = sidplay2Section.getPlaybackType();
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
		case RANDOM_HVSC:
			randomHVSC.setSelected(true);
			break;
		default:
			off.setSelected(true);
			break;
		}
		if (util.getConfig().getSidplay2Section().isLoop()) {
			repeatOne.setSelected(true);
		} else {
			repeatOff.setSelected(true);
		}
		util.getPlayer().stateProperty().addListener(nextTuneListener);
		List<? extends FavoritesSection> favorites = util.getConfig().getFavorites();
		util.getConfig().getObservableFavorites()
				.addListener((ListChangeListener.Change<? extends FavoritesSection> change) -> {
					while (change.next()) {
						if (change.wasPermutated() || change.wasUpdated()) {
							continue;
						}
						if (change.wasAdded()) {
							List<? extends FavoritesSection> addedSubList = change.getAddedSubList();
							for (FavoritesSection favoritesSection : addedSubList) {
								addTab(favoritesSection);
							}
						}
					}
				});
		favoritesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			// Save last selected tab
			if (newValue != null) {
				util.getConfig().setCurrentFavorite(newValue.getText());
			}
		});
		Platform.runLater(() -> {
			// Initially select last selected tab
			String currentFavorite = util.getConfig().getCurrentFavorite();
			for (FavoritesSection favorite : favorites) {
				addTab(favorite);
			}
			if (currentFavorite != null) {
				for (Tab tab : favoritesList.getTabs()) {
					if (tab.getText().equals(currentFavorite)) {
						favoritesList.getSelectionModel().select(tab);
						currentlyPlayedFavorites = (FavoritesTab) getSelectedTab().getContent();
						break;
					}
				}
			}
		});
		Platform.runLater(() -> {
			favoritesList.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			});
			favoritesList.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					FavoritesTab selectedTab = (FavoritesTab) getSelectedTab().getContent();
					selectedTab.addFavorites(files);
				}
				event.setDropCompleted(success);
				event.consume();
			});
		});
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(nextTuneListener);
	}

	@FXML
	private void doAutoConfiguration() {
		autoConfiguration.setDisable(true);
		try {
			DownloadThread downloadThread = new DownloadThread(util.getConfig(),
					new ProgressListener(util, favoritesList.getScene()) {

						@Override
						public void downloaded(final File file) {
							Platform.runLater(() -> {
								autoConfiguration.setDisable(false);
								if (file != null) {
									List<FavoritesSection> favorites = util.getConfig().getFavorites();
									FavoritesSection favoritesSection = new FavoritesSection();
									String tabName = PathUtils.getFilenameWithoutSuffix(file.getName());
									favoritesSection.setName(tabName);
									favorites.add(favoritesSection);
									try {
										((FavoritesTab) getSelectedTab().getContent()).loadFavorites(file);
									} catch (IOException e1) {
										e1.printStackTrace();
									}
								}
							});
						}
					}, new URL(OnlineSection.JSIDPLAY2_JS2_URL), false);
			downloadThread.start();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void addFavorites() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final List<File> files = fileDialog.showOpenMultipleDialog(favoritesList.getScene().getWindow());
		if (files != null && files.size() > 0) {
			File file = files.get(0);
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			Tab tab = getSelectedTab();

			FavoritesTab selectedTab = (FavoritesTab) tab.getContent();
			selectedTab.addFavorites(files);
			renameTab(tab, PathUtils.getFilenameWithoutSuffix(file.getParentFile().getName()));
			tab.setText(selectedTab.getFavoritesSection().getName());
		}
	}

	@FXML
	private void removeFavorites() {
		((FavoritesTab) getSelectedTab().getContent()).removeSelectedFavorites();
	}

	@FXML
	private void selectAllFavorites() {
		((FavoritesTab) getSelectedTab().getContent()).selectAllFavorites();
	}

	@FXML
	private void clearSelection() {
		((FavoritesTab) getSelectedTab().getContent()).clearSelection();
	}

	@FXML
	private void loadFavorites() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(FavoritesExtension.DESCRIPTION, FavoritesExtension.EXTENSION));
		final File file = fileDialog.showOpenDialog(favoritesList.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			try {
				((FavoritesTab) getSelectedTab().getContent()).loadFavorites(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void saveFavoritesAs() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(FavoritesExtension.DESCRIPTION, FavoritesExtension.EXTENSION));
		final File file = fileDialog.showSaveDialog(favoritesList.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			File target = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()) + ".js2");
			try {
				((FavoritesTab) getSelectedTab().getContent()).saveFavorites(target);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void addTab() {
		List<FavoritesSection> favorites = util.getConfig().getFavorites();
		FavoritesSection favoritesSection = new FavoritesSection();
		favoritesSection.setName(util.getBundle().getString("NEW_TAB"));
		favorites.add(favoritesSection);
	}

	@FXML
	private void renameTab() {
		renameTab(getSelectedTab(), renameTab.getText());
		getSelectedTab().setText(renameTab.getText());
	}

	@FXML
	private void off() {
		util.getConfig().getSidplay2Section().setPlaybackType(PlaybackType.PLAYBACK_OFF);
	}

	@FXML
	private void normal() {
		util.getConfig().getSidplay2Section().setPlaybackType(PlaybackType.NORMAL);
	}

	@FXML
	private void randomOne() {
		util.getConfig().getSidplay2Section().setPlaybackType(PlaybackType.RANDOM_ONE);
	}

	@FXML
	private void randomAll() {
		util.getConfig().getSidplay2Section().setPlaybackType(PlaybackType.RANDOM_ALL);
	}

	@FXML
	private void randomHVSC() {
		util.getConfig().getSidplay2Section().setPlaybackType(PlaybackType.RANDOM_HVSC);
	}

	@FXML
	private void repeatOff() {
		util.getConfig().getSidplay2Section().setLoop(false);
	}

	@FXML
	private void repeatOne() {
		util.getConfig().getSidplay2Section().setLoop(true);
	}

	private void setSongLengthDatabase(String hvscRoot) {
		try {
			util.getPlayer().setSidDatabase(new SidDatabase(hvscRoot));
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private void setSTIL(String hvscRoot) {
		try (InputStream input = new TFileInputStream(new TFile(hvscRoot, STIL.STIL_FILE))) {
			util.getPlayer().setSTIL(new STIL(input));
		} catch (FileNotFoundException e) {
			System.err.println(String.format(util.getBundle().getString("ERR_FILE_NOT_FOUND"), e.getMessage()));
		} catch (NoSuchFieldException | IllegalAccessException | IOException e) {
			System.err.println(e.getMessage());
		}
	}

	Tab getSelectedTab() {
		return favoritesList.getSelectionModel().getSelectedItem();
	}

	protected void addTab(final FavoritesSection favoritesSection) {
		final FavoritesTab newTab = new FavoritesTab(util.getWindow(), util.getPlayer());
		if (favoritesSection.getName() == null) {
			favoritesSection.setName(util.getBundle().getString("NEW_TAB"));
		}
		Tab tab = new Tab(favoritesSection.getName(), newTab);
		newTab.restoreColumns(favoritesSection);
		tab.setClosable(favoritesList.getTabs().size() != 0);
		tab.setOnClosed(event -> newTab.removeAllFavorites());
		newTab.setFavorites(this);

		favoritesList.getTabs().add(tab);
		favoritesList.getSelectionModel().select(tab);
	}

	private void renameTab(Tab selectedTab, String name) {
		((FavoritesTab) selectedTab.getContent()).getFavoritesSection().setName(name);
	}

	protected void playNextTune() {
		SidPlay2Section sidPlay2Section = util.getConfig().getSidplay2Section();
		PlaybackType pt = sidPlay2Section.getPlaybackType();

		if (!sidPlay2Section.isLoop()) {
			if (pt == PlaybackType.RANDOM_ALL) {
				favoritesList.getSelectionModel()
						.select(Math.abs(random.nextInt(Integer.MAX_VALUE)) % favoritesList.getTabs().size());
				currentlyPlayedFavorites = (FavoritesTab) getSelectedTab().getContent();
				currentlyPlayedFavorites.playNextRandom();
			} else if (pt == PlaybackType.RANDOM_ONE && currentlyPlayedFavorites != null) {
				currentlyPlayedFavorites.playNextRandom();
			} else if (pt == PlaybackType.NORMAL && currentlyPlayedFavorites != null
					&& util.getPlayer().getTune() != null) {
				currentlyPlayedFavorites.playNext();
			} else {
				// PlaybackType.RANDOM_HVSC || PlaybackType.PLAYBACK_OFF
				if (currentlyPlayedFavorites != null) {
					currentlyPlayedFavorites.deselectCurrentlyPlayedHVSCEntry();
					currentlyPlayedFavorites = null;
				}
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
