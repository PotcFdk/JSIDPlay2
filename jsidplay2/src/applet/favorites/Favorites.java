package applet.favorites;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
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
import applet.TuneTab;
import applet.collection.Collection;
import applet.entities.config.service.DbConfigService;
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

	private DbConfigService dbConfigService;

	public Favorites(EntityManager em, Player pl, IConfig cfg, Collection hvsc,
			Collection cgsc) {
		this.player = pl;
		this.config = cfg;
		this.hvsc = hvsc;
		this.cgsc = cgsc;
		dbConfigService = new DbConfigService(em);

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

							@Override
							public String getTitle() {
								return title;
							}

							@Override
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
							@Override
							public int getIndex() {
								return index;
							}

							@Override
							public String getTitle() {
								return title;
							}

							@Override
							public String getFileName() {
								return name;
							}

							@Override
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

						@Override
						public void actionPerformed(ActionEvent e) {
							getUiEvents().fireEvent(IAddFavoritesTab.class,
									new IAddFavoritesTab() {

										@Override
										public String getTitle() {
											return getSwix().getLocalizer()
													.getString("NEW_TAB");
										}

										@Override
										public void setFavorites(
												IFavorites favorites) {

										}

									});
						}

					}));

			restoreFavorites();
			reloadRestoredFavorites();

			favoriteList.addChangeListener(new ChangeListener() {

				@Override
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
								@Override
								public int getIndex() {
									return index;
								}

								@Override
								public String getTitle() {
									return favoriteList.getTitleAt(index);
								}

								@Override
								public String getFileName() {
									Component comp = favoriteList
											.getComponentAt(index);
									if (comp instanceof IFavorites) {
										IFavorites fav = (IFavorites) comp;
										return fav.getFileName();
									}
									return null;
								}

								@Override
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
		List<? extends IFavoritesSection> favorites = config.getFavorites();
		int i = 0;
		for (IFavoritesSection favorite : favorites) {
			final String filename = favorite.getFilename();
			if (filename == null) {
				continue;
			}
			final int index = i;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
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
									@Override
									public int getIndex() {
										return index;
									}

									@Override
									public String getTitle() {
										return title;
									}

									@Override
									public String getFileName() {
										return filename.substring(0,
												filename.lastIndexOf('.'))
												+ FavoritesFileFilter.EXT_FAVORITES;
									}

									@Override
									public boolean isSelected() {
										return false;
									}
								});
						// END change event for migrated files
					}
				}

			});
			i++;
		}
	}

	private void restoreFavorites() {
		List<? extends IFavoritesSection> favorites = config.getFavorites();
		for (IFavoritesSection favorite : favorites) {
			final String title = favorite.getName();
			addTab(title);
		}
		if (favorites.size() == 0) {
			// add a first tab
			getUiEvents().fireEvent(IAddFavoritesTab.class,
					new IAddFavoritesTab() {

						@Override
						public String getTitle() {
							return getSwix().getLocalizer()
									.getString("NEW_TAB");
						}

						@Override
						public void setFavorites(IFavorites newFavorites) {
						}

					});
		} else {
			String title = config.getCurrentFavorite();
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

					@Override
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

												@Override
												public int getIndex() {
													return index;
												}

												@Override
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

					@Override
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
						@Override
						public int getIndex() {
							return index;
						}

						@Override
						public String getTitle() {
							return title;
						}

						@Override
						public String getFileName() {
							return name;
						}

						@Override
						public boolean isSelected() {
							return false;
						}
					});
		}
	}

	@Override
	public void notify(UIEvent event) {
		if (event.isOfType(IAddFavoritesTab.class)) {
			final IAddFavoritesTab ifObj = (IAddFavoritesTab) event
					.getUIEventImpl();

			dbConfigService.addFavorite(config, ifObj.getTitle());

			IFavorites newTab = addTab(ifObj.getTitle());
			ifObj.setFavorites(newTab);
			// System.err.println("Add title=" + ifObj.getTitle());
		} else if (event.isOfType(IRemoveFavoritesTab.class)) {
			final IRemoveFavoritesTab ifObj = (IRemoveFavoritesTab) event
					.getUIEventImpl();
			removeTab(ifObj.getIndex());

			dbConfigService.removeFavorite(config, ifObj.getIndex());

			// System.err.println("Remove index=" + ifObj.getIndex() + ",
			// title="
			// + ifObj.getTitle());
		} else if (event.isOfType(IChangeFavoritesTab.class)) {
			final IChangeFavoritesTab ifObj = (IChangeFavoritesTab) event
					.getUIEventImpl();
			changeTab(ifObj);

			IFavoritesSection toChange = config.getFavorites().get(
					ifObj.getIndex());
			toChange.setName(ifObj.getTitle());
			toChange.setFilename(ifObj.getFileName());

			config.setCurrentFavorite(ifObj.getTitle());

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

}
