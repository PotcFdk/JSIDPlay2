package applet.favorites;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.PathUtils;
import applet.TuneTab;
import applet.collection.Collection;
import applet.events.IPlayTune;
import applet.events.ITuneStateChanged;
import applet.events.UIEvent;
import applet.events.favorites.IAddFavoritesTab;
import applet.events.favorites.IChangeFavoritesTab;
import applet.events.favorites.IFavoriteTabNames;
import applet.events.favorites.IGetFavorites;
import applet.events.favorites.IRemoveFavoritesTab;
import applet.filefilter.FavoritesFileFilter;
import applet.filefilter.TuneFileFilter;

@SuppressWarnings("serial")
public class Favorites extends TuneTab implements ListSelectionListener {

	/**
	 * file filter for tunes
	 */
	protected transient final FileFilter fPlayListFilter = new FavoritesFileFilter();

	/**
	 * file filter for tunes
	 */
	protected transient final FileFilter fTuneFilter = new TuneFileFilter();

	protected JButton add, remove, selectAll, deselectAll, load, save, saveAs;
	protected JCheckBox playbackEnable, repeatEnable;
	protected JTabbedPane favoriteList;
	protected JRadioButton normal, randomOne, randomAll, repeatOne;

	protected Player player;
	protected IConfig config;
	protected Collection hvsc, cgsc;

	protected File lastDir;
	protected PlayList currentlyPlayedFavorites;
	protected final Random random = new Random();

	public Favorites(Player pl, IConfig cfg, Collection hvsc, Collection cgsc) {
		this.player = pl;
		this.config = cfg;
		this.hvsc = hvsc;
		this.cgsc = cgsc;
		createContents();
	}

	public Collection getHvsc() {
		return hvsc;
	}

	public Collection getCgsc() {
		return cgsc;
	}

	private void createContents() {
		try {
			swix = new SwingEngine(this);
			swix.insert(Favorites.class.getResource("Favorites.xml"), this);

			fillComboBoxes();
			setDefaults();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Action addFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				JFileChooser fileDialog = new JFileChooser(lastDir);
				fileDialog.setFileFilter(fTuneFilter);
				fileDialog.setMultiSelectionEnabled(true);
				fileDialog
						.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int rc = fileDialog.showOpenDialog(Favorites.this);
				if (rc == JFileChooser.APPROVE_OPTION
						&& fileDialog.getSelectedFile() != null) {
					lastDir = fileDialog.getSelectedFile().getParentFile();
					File files[] = fileDialog.getSelectedFiles();
					fav.addToFavorites(files);
				}
			}
		}
	};

	public Action removeFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				fav.removeSelectedRows();
			}
		}
	};

	public Action selectAllFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				fav.selectFavorites();
			}
		}
	};

	public Action deselectAllFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				fav.deselectFavorites();
			}
		}
	};

	public Action loadFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileDialog = new JFileChooser(lastDir);
			fileDialog.setFileFilter(fPlayListFilter);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(Favorites.this);
			int rc = fileDialog.showOpenDialog(containerFrame);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				lastDir = fileDialog.getSelectedFile().getParentFile();
				final String name;
				if (!fileDialog.getSelectedFile().getAbsolutePath()
						.endsWith(FavoritesFileFilter.EXT_FAVORITES)) {
					name = fileDialog.getSelectedFile().getAbsolutePath()
							+ FavoritesFileFilter.EXT_FAVORITES;
				} else {
					name = fileDialog.getSelectedFile().getAbsolutePath();
				}
				String baseName = new File(name).getName();
				int lastIndexOf = baseName.lastIndexOf('.');
				final String title;
				if (lastIndexOf != -1) {
					title = baseName.substring(0, lastIndexOf);
				} else {
					title = baseName;
				}

				// add a first tab
				getUiEvents().fireEvent(IAddFavoritesTab.class,
						new IAddFavoritesTab() {

							public String getTitle() {
								return title;
							}

							public void setFavorites(IFavorites favorites) {

							}

						});

				final int index = favoriteList.getSelectedIndex();
				Component comp = favoriteList.getComponentAt(index);
				if (comp instanceof IFavorites) {
					IFavorites fav = (IFavorites) comp;
					fav.loadFavorites(name);
				}
				getUiEvents().fireEvent(IChangeFavoritesTab.class,
						new IChangeFavoritesTab() {
							public int getIndex() {
								return index;
							}

							public String getTitle() {
								return title;
							}

							public String getFileName() {
								return name;
							}

							public boolean isSelected() {
								return false;
							}
						});
			}
		}
	};

	public Action saveFavorites = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				if (fav.getFileName() == null) {
					saveAs();
				} else {
					fav.saveFavorites(fav.getFileName());
				}
			}
		}
	};

	public Action saveFavoritesAs = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			saveAs();
		}
	};

	public Action enablePlayback = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean playbackSelected = playbackEnable.isSelected();
			normal.setEnabled(playbackSelected);
			randomOne.setEnabled(playbackSelected);
			randomAll.setEnabled(playbackSelected);
			repeatEnable.setEnabled(playbackSelected);
			repeatOne.setEnabled(playbackSelected);
			boolean repeatSelected = repeatEnable.isSelected();
			repeatOne.setEnabled(repeatSelected);
		}
	};

	public Action enableRepeat = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean repeatSelected = repeatEnable.isSelected();
			repeatOne.setEnabled(repeatSelected);
			repeatOne.setSelected(repeatSelected);
		}
	};

	private SwingEngine swix;

	private void setDefaults() {
		{
			favoriteList.setTabComponentAt(0, new ButtonTabComponent(
					favoriteList, "", "icons/addtab.png", swix.getLocalizer()
							.getString("ADD_A_NEW_TAB"), new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							getUiEvents().fireEvent(IAddFavoritesTab.class,
									new IAddFavoritesTab() {

										public String getTitle() {
											return getSwix().getLocalizer()
													.getString("NEW_TAB");
										}

										public void setFavorites(
												IFavorites favorites) {

										}

									});
						}

					}));

			restoreFavorites();
			reloadRestoredFavorites();

			favoriteList.addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					final int index = favoriteList.getSelectedIndex();
					if (index == -1 || index == favoriteList.getTabCount() - 1) {
						return;
					}
					{
						Component comp = favoriteList.getComponentAt(index);
						if (comp instanceof IFavorites) {
							IFavorites fav = (IFavorites) comp;
							remove.setEnabled(fav.getSelection().length > 0);
						}
					}
					getUiEvents().fireEvent(IChangeFavoritesTab.class,
							new IChangeFavoritesTab() {
								public int getIndex() {
									return index;
								}

								public String getTitle() {
									return favoriteList.getTitleAt(index);
								}

								public String getFileName() {
									Component comp = favoriteList
											.getComponentAt(index);
									if (comp instanceof IFavorites) {
										IFavorites fav = (IFavorites) comp;
										return fav.getFileName();
									}
									return null;
								}

								public boolean isSelected() {
									return true;
								}
							});
				}
			});
		}
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	private void reloadRestoredFavorites() {
		IFavoritesSection favorites = config.getFavorites();
		String[] stringToList = stringToList(favorites.getFavoritesFilenames());
		for (int i = 0; i < stringToList.length; i++) {
			final String filename = stringToList[i];
			if (filename == null) {
				continue;
			}
			final int index = i;
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					Component comp = favoriteList.getComponentAt(index);
					if (comp instanceof IFavorites) {
						IFavorites fav = (IFavorites) comp;
						fav.loadFavorites(filename);

						// BEGIN change event for migrated files
						String baseName = new File(filename).getName();
						int lastIndexOf = baseName.lastIndexOf('.');
						final String title;
						if (lastIndexOf != -1) {
							title = baseName.substring(0, lastIndexOf);
						} else {
							title = baseName;
						}
						getUiEvents().fireEvent(IChangeFavoritesTab.class,
								new IChangeFavoritesTab() {
									public int getIndex() {
										return index;
									}

									public String getTitle() {
										return title;
									}

									public String getFileName() {
										return filename.substring(0,
												filename.lastIndexOf('.'))
												+ FavoritesFileFilter.EXT_FAVORITES;
									}

									public boolean isSelected() {
										return false;
									}
								});
						// END change event for migrated files
					}
				}

			});
		}
	}

	private void restoreFavorites() {
		IFavoritesSection favorites = config.getFavorites();
		String[] stringToList = stringToList(favorites.getFavoritesTitles());
		for (int i = 0; i < stringToList.length; i++) {
			final String title = stringToList[i];
			addTab(title);
		}
		if (stringToList.length == 0) {
			// add a first tab
			getUiEvents().fireEvent(IAddFavoritesTab.class,
					new IAddFavoritesTab() {

						public String getTitle() {
							return getSwix().getLocalizer()
									.getString("NEW_TAB");
						}

						public void setFavorites(IFavorites newFavorites) {
						}

					});
		} else {
			String title = favorites.getFavoritesCurrent();
			int index = favoriteList.indexOfTab(title);
			favoriteList.setSelectedIndex(Math.max(index, 0));
		}
	}

	private IFavorites addTab(String newTitle) {
		final int lastIndex = favoriteList.getTabCount() - 1;
		final PlayList favorites = new PlayList(player, config, this);
		favorites.getPlayListTable().getSelectionModel()
				.addListSelectionListener(this);
		favoriteList.insertTab(newTitle, null, favorites, null, lastIndex);
		favoriteList.setTabComponentAt(lastIndex, new ButtonTabComponent(
				favoriteList, newTitle, "icons/closetab.png", swix
						.getLocalizer().getString("CLOSE_THIS_TAB"),
				new ActionListener() {

					public void actionPerformed(final ActionEvent e) {
						ButtonTabComponent.TabButton button = (ButtonTabComponent.TabButton) e
								.getSource();
						final int index = favoriteList
								.indexOfTabComponent(button.getComp());
						final String title = favoriteList.getTitleAt(index);
						if (favoriteList.getTabCount() > 2) {
							if (index < favoriteList.getComponentCount() - 1) {
								int response = JOptionPane.NO_OPTION;
								if (!favorites.isEmpty()) {
									response = JOptionPane.showConfirmDialog(
											Favorites.this,
											String.format(
													getSwix()
															.getLocalizer()
															.getString(
																	"REMOVE_ALL"),
													title),
											getSwix().getLocalizer().getString(
													"REMOVE_FAVORITES"),
											JOptionPane.YES_NO_OPTION);
								} else {
									response = JOptionPane.YES_OPTION;
								}

								if (response == JOptionPane.YES_OPTION) {
									// remove tab
									getUiEvents().fireEvent(
											IRemoveFavoritesTab.class,
											new IRemoveFavoritesTab() {

												public int getIndex() {
													return index;
												}

												public String getTitle() {
													return title;
												}
											});
								}
							}
						}
					}

				}));

		favoriteList.setSelectedIndex(lastIndex);
		return favorites;
	}

	private void removeTab(int index) {
		favoriteList.removeTabAt(index);
		favoriteList.setSelectedIndex(index > 0 ? index - 1 : 0);
	}

	private void changeTab(IChangeFavoritesTab changeFavoritesTab) {
		favoriteList.setTitleAt(changeFavoritesTab.getIndex(),
				changeFavoritesTab.getTitle());
	}

	private void playNextTune(ITuneStateChanged stateChanged) {
		if (playbackEnable == null) {
			// gui not created yet, ignore!
			return;
		}
		if (playbackEnable.isSelected()) {
			File tune = stateChanged.getTune();
			File nextFile = null;
			if (currentlyPlayedFavorites != null && repeatEnable.isSelected()
					&& repeatOne.isSelected()) {
				// repeat one tune
				setPlayingIcon(currentlyPlayedFavorites);
				nextFile = tune;
			} else if (randomAll.isSelected()) {
				// random all playlists
				int index = Math.abs(random.nextInt(Integer.MAX_VALUE))
						% (favoriteList.getTabCount() - 1);
				Component comp = favoriteList.getComponentAt(index);
				setPlayingIcon(comp);
				favoriteList.setSelectedComponent(comp);
				IFavorites fav = (IFavorites) comp;
				nextFile = fav.getNextRandomFile(tune);
			} else if (currentlyPlayedFavorites != null
					&& randomOne.isSelected()) {
				// random one playlist
				IFavorites fav = currentlyPlayedFavorites;
				setPlayingIcon(currentlyPlayedFavorites);
				nextFile = fav.getNextRandomFile(tune);
			} else if (currentlyPlayedFavorites != null
					&& !repeatEnable.isSelected()) {
				// normal playback
				IFavorites fav = currentlyPlayedFavorites;
				setPlayingIcon(currentlyPlayedFavorites);
				nextFile = fav.getNextFile(tune);
			}
			if (nextFile != null) {
				// System.err.println("Play Next File: " + nextFilename);
				final File file = nextFile;
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						// play tune
						getUiEvents().fireEvent(IPlayTune.class,
								new IPlayTune() {
									@Override
									public boolean switchToVideoTab() {
										return false;
									}

									@Override
									public File getFile() {
										return file;
									}

									@Override
									public Component getComponent() {
										return Favorites.this;
									}
								});
					}

				});
			}
		}
	}

	protected void saveAs() {
		JFileChooser fileDialog = new JFileChooser(lastDir);
		fileDialog.setFileFilter(fPlayListFilter);
		final Frame containerFrame = JOptionPane
				.getFrameForComponent(Favorites.this);
		int rc = fileDialog.showSaveDialog(containerFrame);
		if (rc == JFileChooser.APPROVE_OPTION
				&& fileDialog.getSelectedFile() != null) {
			lastDir = fileDialog.getSelectedFile().getParentFile();
			final String name;
			if (!fileDialog.getSelectedFile().getAbsolutePath()
					.endsWith(FavoritesFileFilter.EXT_FAVORITES)) {
				name = fileDialog.getSelectedFile().getAbsolutePath()
						+ FavoritesFileFilter.EXT_FAVORITES;
			} else {
				name = fileDialog.getSelectedFile().getAbsolutePath();
			}
			final int index = favoriteList.getSelectedIndex();
			Component comp = favoriteList.getComponentAt(index);
			if (comp instanceof IFavorites) {
				IFavorites fav = (IFavorites) comp;
				fav.saveFavorites(name);
			}
			String baseName = new File(name).getName();
			int lastIndexOf = baseName.lastIndexOf('.');
			final String title;
			if (lastIndexOf != -1) {
				title = baseName.substring(0, lastIndexOf);
			} else {
				title = baseName;
			}
			getUiEvents().fireEvent(IChangeFavoritesTab.class,
					new IChangeFavoritesTab() {
						public int getIndex() {
							return index;
						}

						public String getTitle() {
							return title;
						}

						public String getFileName() {
							return name;
						}

						public boolean isSelected() {
							return false;
						}
					});
		}
	}

	public void notify(UIEvent event) {
		if (event.isOfType(IAddFavoritesTab.class)) {
			final IAddFavoritesTab ifObj = (IAddFavoritesTab) event
					.getUIEventImpl();
			IFavorites newTab = addTab(ifObj.getTitle());
			ifObj.setFavorites(newTab);

			final IFavoritesSection favorites = config.getFavorites();
			String[] stringToList = stringToList(favorites.getFavoritesTitles());
			final String[] newFavoritesTitles = new String[stringToList.length + 1];
			for (int i = 0; i < stringToList.length; i++) {
				newFavoritesTitles[i] = stringToList[i];
			}
			newFavoritesTitles[stringToList.length] = ifObj.getTitle();
			favorites.setFavoritesTitles(listToString(newFavoritesTitles));
			String[] stringToList2 = stringToList(favorites
					.getFavoritesFilenames());
			final String[] newFavoritesFilenames = new String[stringToList2.length + 1];
			for (int i = 0; i < stringToList2.length; i++) {
				newFavoritesFilenames[i] = stringToList2[i];
			}
			newFavoritesFilenames[stringToList2.length] = null;
			favorites
					.setFavoritesFilenames(listToString(newFavoritesFilenames));
			// System.err.println("Add title=" + ifObj.getTitle());
		} else if (event.isOfType(IRemoveFavoritesTab.class)) {
			final IRemoveFavoritesTab ifObj = (IRemoveFavoritesTab) event
					.getUIEventImpl();
			removeTab(ifObj.getIndex());

			final IFavoritesSection favorites = config.getFavorites();
			String[] stringToList = stringToList(favorites.getFavoritesTitles());
			final String[] newFavoriteTitles = new String[stringToList.length - 1];
			int j = 0;
			for (int i = 0; i < stringToList.length; i++) {
				if (i != ifObj.getIndex()) {
					newFavoriteTitles[j] = stringToList[i];
					j++;
				}
			}
			favorites.setFavoritesTitles(listToString(newFavoriteTitles));

			String[] stringToList2 = stringToList(favorites
					.getFavoritesFilenames());
			final String[] newFavoriteFilenames = new String[stringToList2.length - 1];
			int k = 0;
			for (int i = 0; i < stringToList2.length; i++) {
				if (i != ifObj.getIndex()) {
					newFavoriteFilenames[k] = stringToList2[i];
					k++;
				}
			}
			favorites.setFavoritesFilenames(listToString(newFavoriteFilenames));

			// System.err.println("Remove index=" + ifObj.getIndex() + ",
			// title="
			// + ifObj.getTitle());
		} else if (event.isOfType(IChangeFavoritesTab.class)) {
			final IChangeFavoritesTab ifObj = (IChangeFavoritesTab) event
					.getUIEventImpl();
			changeTab(ifObj);

			final IFavoritesSection favorites = config.getFavorites();
			String[] stringToList = stringToList(favorites.getFavoritesTitles());
			String[] newTitles = new String[stringToList.length];
			for (int i = 0; i < newTitles.length; i++) {
				if (i == ifObj.getIndex()) {
					newTitles[i] = ifObj.getTitle();
				} else {
					newTitles[i] = stringToList[i];
				}
			}
			favorites.setFavoritesTitles(listToString(newTitles));
			String[] stringToList2 = stringToList(favorites
					.getFavoritesFilenames());
			String[] newFilenames = new String[stringToList2.length];
			for (int i = 0; i < newFilenames.length; i++) {
				if (i == ifObj.getIndex()) {
					newFilenames[i] = ifObj.getFileName();
				} else {
					newFilenames[i] = stringToList2[i];
				}
			}
			if (ifObj.getFileName() != null) {
				final String relativePath = PathUtils.getPath(new File(ifObj
						.getFileName()));
				if (relativePath != null) {
					newFilenames[ifObj.getIndex()] = relativePath;
				}
			}
			favorites.setFavoritesFilenames(listToString(newFilenames));
			if (ifObj.isSelected()) {
				favorites.setFavoritesCurrent(ifObj.getTitle());
			}
			// System.err.println("Change index=" + ifObj.getTitle()
			// + ", filename=" + ifObj.getFileName());
		} else if (event.isOfType(IFavoriteTabNames.class)) {
			IFavoriteTabNames ifObj = (IFavoriteTabNames) event
					.getUIEventImpl();

			ArrayList<String> result = new ArrayList<String>();
			for (int i = 0; i < favoriteList.getTabCount() - 1; i++) {
				result.add(favoriteList.getTitleAt(i));
			}

			ifObj.setFavoriteTabNames(
					result.toArray(new String[result.size()]),
					favoriteList.getTitleAt(favoriteList.getSelectedIndex()));
		} else if (event.isOfType(IGetFavorites.class)) {
			IGetFavorites ifObj = (IGetFavorites) event.getUIEventImpl();
			int index = ifObj.getIndex();
			IFavorites favorites = (IFavorites) favoriteList
					.getComponentAt(index);
			ifObj.setFavorites(favorites);
		} else if (event.isOfType(ITuneStateChanged.class)) {
			ITuneStateChanged ifObj = (ITuneStateChanged) event
					.getUIEventImpl();
			setPlayingIcon(null);
			if (ifObj.naturalFinished()) {
				playNextTune(ifObj);
			}
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
	}

	public JTabbedPane getTabbedPane() {
		return favoriteList;
	}

	public void setCurrentlyPlayedFavorites(PlayList favoritesPanel) {
		currentlyPlayedFavorites = favoritesPanel;
		setPlayingIcon(favoritesPanel);
	}

	private void setPlayingIcon(Component comp) {
		for (int i = 0; i < favoriteList.getTabCount(); i++) {
			ButtonTabComponent tabComponent = (ButtonTabComponent) favoriteList
					.getTabComponentAt(i);
			if (tabComponent == null)
				continue;
			if (favoriteList.getComponentAt(i) != comp) {
				tabComponent.fPlayButton.setVisible(false);
			} else {
				tabComponent.fPlayButton.setVisible(true);
			}
		}
	}

	public FileFilter getTuneFilter() {
		return fTuneFilter;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		DefaultListSelectionModel model = (DefaultListSelectionModel) e
				.getSource();
		remove.setEnabled(!model.isSelectionEmpty());
	}

	public SwingEngine getSwix() {
		return swix;
	}

	private static String[] stringToList(final String str) {
		if (str == null) {
			return new String[0];
		}
		final ArrayList<String> result = new ArrayList<String>();
		final StringTokenizer tok = new StringTokenizer(str, ",", false);
		while (tok.hasMoreElements()) {
			final String name = (String) tok.nextElement();
			if ("null".equals(name)) {
				result.add(null);
			} else {
				result.add(name);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static String listToString(final String[] list) {
		if (list == null) {
			return "";
		}
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			if (i != 0) {
				result.append(",");
			}
			result.append(list[i]);
		}
		return result.toString();
	}

}
