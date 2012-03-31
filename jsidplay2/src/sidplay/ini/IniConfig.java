/**
 *                         Sidplay2 config file reader.
 *                         ----------------------------
 *  begin                : Sun Mar 25 2001
 *  copyright            : (C) 2000 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package sidplay.ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import libsidutils.zip.ZipEntryFileProxy;


import resid_builder.resid.ISIDDefs.ChipModel;

/**
 * INI configuration file support responsible to load and save all emulator
 * settings.
 * 
 * @author Ken Händel
 * 
 */
public class IniConfig {
	/** Bump this each time you want to invalidate sidplay2.ini files on disk */
	protected static final int REQUIRED_CONFIG_VERSION = 18;

	/** Name of our config file. */
	private static final String FILE_NAME = "sidplay2.ini";

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	private IniSidplay2Section sidplay2Section;
	private IniC1541Section c1541Section;
	private IniPrinterSection printerSection;
	private IniJoystickSection joystickSection;
	private IniConsoleSection consoleSection;
	private IniAudioSection audioSection;
	private IniEmulationSection emulationSection;
	private IniFavoritesSection favoritesSection;

	protected IniReader iniReader;

	private void clear() {
		sidplay2Section = new IniSidplay2Section(iniReader);
		c1541Section = new IniC1541Section(iniReader);
		printerSection = new IniPrinterSection(iniReader);
		joystickSection = new IniJoystickSection(iniReader);
		consoleSection = new IniConsoleSection(iniReader);
		audioSection = new IniAudioSection(iniReader);
		emulationSection = new IniEmulationSection(iniReader);
		favoritesSection = new IniFavoritesSection(iniReader);
	}

	public IniFilterSection getFilter(final String filterName) {
		return new IniFilterSection(iniReader, filterName);
	}

	public String[] getFilterList(final ChipModel model) {
		final List<String> filters = new ArrayList<String>();
		for (final String heading : iniReader.listSections()) {
			if (!heading.matches("Filter.*")) {
				continue;
			}

			if (getFilter(heading).getFilter8580CurvePosition() != 0 ^ model == ChipModel.MOS6581) {
				filters.add(heading);
			}
		}

		return filters.toArray(new String[] {});
	}

	public IniConfig() {
		iniPath = getINIPath();
		read();
	}

	private void read() {
		if (iniPath != null && iniPath.exists()) {
			FileInputStream is = null;
			try {
				is = new FileInputStream(iniPath);
				iniReader = new IniReader(is);
				clear();
				/* validate loaded configuration */
				if (sidplay2Section.getVersion() == REQUIRED_CONFIG_VERSION) {
					return;
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
			createINIBackup(iniPath);
		}

		readInternal();
	}

	/**
	 * Determine INI filename.
	 * <OL>
	 * <LI>If INI file exists in the user directory, then use it, else
	 * <LI>use INI file in the current working directory
	 * </OL>
	 * 
	 * @return the absolute path name of the INI file to use
	 */
	private File getINIPath() {
		try {
			File configPlace = null;
			for (final String s : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
				if (s == null) {
					continue;
				}

				configPlace = new File(s, FILE_NAME);
				if (configPlace.exists()) {
					return configPlace;
				}
			}
			return configPlace;
		} catch (final AccessControlException e) {
			// No external config file in the applet version
			return null;
		}

	}

	/**
	 * Create backup of old INI file
	 * 
	 * @param iniFile
	 *            the INI file to backup
	 */
	private void createINIBackup(final File iniFile) {
		iniFile.renameTo(new File(iniFile.getParentFile(), FILE_NAME + ".bak"));
	}

	private void readInternal() {
		final InputStream is = getClass().getClassLoader().getResourceAsStream("sidplay/ini/" + FILE_NAME);
		System.out.println("Use internal INI file: " + "sidplay/ini/" + FILE_NAME);

		try {
			iniReader = new IniReader(is);
			clear();
			/* Set the current version so that we detect old versions in future. */
			iniReader.setProperty("SIDPlay2", "Version", REQUIRED_CONFIG_VERSION);
			is.close();
		} catch (final IOException e) {
			return;
		}
	}

	public void write() {
		if (! iniReader.isDirty()) {
			return;
		}

		try {
			iniReader.save(iniPath.getAbsolutePath());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final IniSidplay2Section sidplay2() {
		return sidplay2Section;
	}

	public final IniC1541Section c1541() {
		return c1541Section;
	}
	
	public final IniPrinterSection printer() {
		return printerSection;
	}

	public final IniJoystickSection joystick() {
		return joystickSection;
	}

	public final IniConsoleSection console() {
		return consoleSection;
	}

	public final IniAudioSection audio() {
		return audioSection;
	}

	public final IniEmulationSection emulation() {
		return emulationSection;
	}

	public final IniFavoritesSection favorites() {
		return favoritesSection;
	}

	public final IniFilterSection filter(final ChipModel model) {
		if (model == ChipModel.MOS8580) {
			return new IniFilterSection(iniReader, emulationSection.getFilter8580());
		} else {
			return new IniFilterSection(iniReader, emulationSection.getFilter6581());
		}
	}
	
	public String getHVSCName(final File file) {
		String hvsc = sidplay2().getHvsc();
		return getCollectionRelName(file, hvsc);
	}

	public String getCGSCName(final File file) {
		String cgsc = sidplay2().getCgsc();
		return getCollectionRelName(file, cgsc);
	}

	public static String getCollectionRelName(final File file,
			String collectionRoot) {
		try {
			if (collectionRoot == null || collectionRoot.length() == 0) {
				return null;
			}
			if (file instanceof ZipEntryFileProxy) {
				final int indexOf = file.getPath().indexOf('/');
				if (indexOf == -1) {
					return null;
				}
				return file.getPath().substring(indexOf);
			}
			final String canonicalPath = file.getCanonicalPath();
			final String collCanonicalPath = new File(collectionRoot)
					.getCanonicalPath();
			if (canonicalPath.startsWith(collCanonicalPath)) {
				final String name = canonicalPath.substring(
						collCanonicalPath.length()).replace('\\', '/');
				if (name.startsWith("/")) {
					return name;
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
