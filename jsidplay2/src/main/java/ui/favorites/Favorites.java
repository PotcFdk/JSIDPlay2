package ui.favorites;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import libsidutils.PathUtils;
import ui.JSIDPlay2Main;
import ui.common.C64Tab;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;
import ui.entities.config.SidPlay2Section;
import ui.events.IPlayTune;
import ui.events.IReplayTune;
import ui.events.ITuneStateChanged;
import ui.events.UIEvent;
import ui.events.favorites.IAddFavoritesTab;
import ui.events.favorites.IGetFavoritesTabs;
import ui.filefilter.FavoritesExtension;
import ui.filefilter.TuneFileExtensions;

public class Favorites extends C64Tab {

	private static final Image CURRENTLY_PLAYED_TAB = new Image(JSIDPlay2Main.class.getResource("icons/play.png").toString());

	@FXML
	private Button add, remove, selectAll, deselectAll, load, save, saveAs;
	@FXML
	private TextField renameTab;
	@FXML
	private TabPane favoritesList;
	@FXML
	private RadioButton off, normal, randomOne, randomAll, repeatOff, repeatOne;

	private FavoritesTab currentlyPlayedFavorites;
	protected Random random = new Random();

	@Override
	public String getBundleName() {
		return getClass().getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		if (getPlayer() == null) {
			return;
		}
		List<? extends FavoritesSection> favorites = getConfig().getFavorites();
		for (FavoritesSection favorite : favorites) {
			addTab(favorite);
		}
		favoritesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				// Save last selected tab
				if (newValue != null) {
					((Configuration) getConfig()).setCurrentFavorite(newValue.getText());
				}
			}
		});
		// Initially select last selected tab
		String currentFavorite = ((Configuration) getConfig()).getCurrentFavorite();
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig().getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final List<File> files = fileDialog.showOpenMultipleDialog(favoritesList.getScene().getWindow());
		if (files != null && files.size() > 0) {
			File file = files.get(0);
			getConfig().getSidplay2().setLastDirectory(file.getParentFile().getAbsolutePath());
			FavoritesTab selectedTab = getSelectedTab();
			selectedTab.addFavorites(files);
			renameTab(selectedTab, PathUtils.getBaseNameNoExt(file.getParentFile()));
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig().getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(new ExtensionFilter(FavoritesExtension.DESCRIPTION, FavoritesExtension.EXTENSION));
		final File file = fileDialog.showOpenDialog(favoritesList.getScene().getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(file.getParentFile().getAbsolutePath());
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig().getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(new ExtensionFilter(FavoritesExtension.DESCRIPTION, FavoritesExtension.EXTENSION));
		final File file = fileDialog.showSaveDialog(favoritesList.getScene().getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(file.getParentFile().getAbsolutePath());
			// then load the favorites
			try {
				getSelectedTab().saveFavorites(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void addTab() {
		List<FavoritesSection> favorites = ((Configuration) getConfig()).getFavorites();
		FavoritesSection favoritesSection = new FavoritesSection();
		favoritesSection.setName(getBundle().getString("NEW_TAB"));
		favorites.add(favoritesSection);
		addTab(favoritesSection);
	}

	@FXML
	private void renameTab() {
		FavoritesTab selectedTab = getSelectedTab();
		String name = renameTab.getText();
		renameTab(selectedTab, name);
	}

	@FXML
	private void enablePlayback() {

	}

	@FXML
	private void enableRepeat() {

	}

	private FavoritesTab getSelectedTab() {
		return (FavoritesTab) favoritesList.getSelectionModel().getSelectedItem();
	}

	private void addTab(final FavoritesSection favoritesSection) {
		final FavoritesTab newTab = new FavoritesTab();
		newTab.setText(favoritesSection.getName());
		newTab.restoreColumns(favoritesSection);
		newTab.setClosable(favoritesList.getTabs().size() != 0);
		newTab.setComponent(this);
		newTab.setOnClosed(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				newTab.removeAllFavorites();
			}

		});

		// XXX JavaFX: better initialization support using constructor
		// arguments?
		newTab.setPlayer(getPlayer());
		newTab.setConfig(getConfig());
		newTab.initialize(null, null);

		favoritesList.getTabs().add(newTab);
	}

	private void renameTab(FavoritesTab selectedTab, String name) {
		selectedTab.setText(name);
		selectedTab.getFavoritesSection().setName(name);
	}

	private void playNextTune(final ITuneStateChanged stateChanged) {
		if (repeatOne.isSelected()) {
			// repeat one tune
			getUiEvents().fireEvent(IReplayTune.class, new IReplayTune() {
			});
		} else if (randomAll.isSelected()) {
			// random all favorites tab
			favoritesList.getSelectionModel().select(Math.abs(random.nextInt(Integer.MAX_VALUE)) % favoritesList.getTabs().size());
			currentlyPlayedFavorites = (FavoritesTab) favoritesList.getSelectionModel().getSelectedItem();
			currentlyPlayedFavorites.playNextRandom();
		} else if (currentlyPlayedFavorites != null && randomOne.isSelected()) {
			// random one favorites tab
			currentlyPlayedFavorites.playNextRandom();
		} else if (currentlyPlayedFavorites != null && !off.isSelected() && repeatOff.isSelected()) {
			// normal playback
			currentlyPlayedFavorites.playNext(stateChanged.getTune());
		}
	}

	@Override
	public void notify(UIEvent event) {
		if (event.isOfType(IGetFavoritesTabs.class)) {
			IGetFavoritesTabs ifObj = (IGetFavoritesTabs) event.getUIEventImpl();
			// Inform about all tabs
			List<FavoritesTab> result = new ArrayList<FavoritesTab>();
			for (Tab tab : favoritesList.getTabs()) {
				result.add((FavoritesTab) tab);
			}
			ifObj.setFavoriteTabs(result, getSelectedTab().getText());
		} else if (event.isOfType(IAddFavoritesTab.class)) {
			final IAddFavoritesTab ifObj = (IAddFavoritesTab) event.getUIEventImpl();
			// Add a new tab
			addTab();
			FavoritesTab newTab = (FavoritesTab) favoritesList.getTabs().get(favoritesList.getTabs().size() - 1);
			newTab.setText(ifObj.getTitle());
			ifObj.setFavorites(newTab);
		} else if (event.isOfType(IPlayTune.class)) {
			IPlayTune ifObj = (IPlayTune) event.getUIEventImpl();
			if (ifObj.getComponent().equals(this)) {
				currentlyPlayedFavorites = getSelectedTab();
				if (currentlyPlayedFavorites != null) {
					for (Tab tab : favoritesList.getTabs()) {
						tab.setGraphic(null);
					}
					currentlyPlayedFavorites.setGraphic(new ImageView(CURRENTLY_PLAYED_TAB));
				}
			}
		} else if (event.isOfType(ITuneStateChanged.class)) {
			ITuneStateChanged ifObj = (ITuneStateChanged) event.getUIEventImpl();
			if (ifObj.naturalFinished()) {
				playNextTune(ifObj);
			}
		}

	}
}
