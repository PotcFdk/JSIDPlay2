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

import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IC1541Section;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;
import sidplay.ini.intf.IJoystickSection;
import sidplay.ini.intf.IPrinterSection;
import sidplay.ini.intf.ISidPlay2Section;

/**
 * INI configuration file support responsible to load and save all emulator
 * settings.
 * 
 * @author Ken Händel
 * 
 */
public class IniConfig implements IConfig {
	/** Name of our config file. */
	private static final String FILE_NAME = "sidplay2.ini";

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	private ISidPlay2Section sidplay2Section;
	private IC1541Section c1541Section;
	private IPrinterSection printerSection;
	private IJoystickSection joystickSection;
	private IConsoleSection consoleSection;
	private IAudioSection audioSection;
	private IEmulationSection emulationSection;

	protected IniReader iniReader;

	private void clear() {
		sidplay2Section = new IniSidplay2Section(iniReader);
		c1541Section = new IniC1541Section(iniReader);
		printerSection = new IniPrinterSection(iniReader);
		joystickSection = new IniJoystickSection(iniReader);
		consoleSection = new IniConsoleSection(iniReader);
		audioSection = new IniAudioSection(iniReader);
		emulationSection = new IniEmulationSection(iniReader);
	}

	@Override
	public List<? extends IFilterSection> getFilter() {
		final List<IFilterSection> filters = new ArrayList<IFilterSection>();
		for (final String heading : iniReader.listSections()) {
			if (!heading.matches("Filter.*")) {
				continue;
			}

			filters.add(new IniFilterSection(iniReader, heading));
		}

		return filters;
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
			for (final String s : new String[] {
					System.getProperty("user.dir"),
					System.getProperty("user.home"), }) {
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
			// No external config file in the ui version
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
		final InputStream is = getClass().getClassLoader().getResourceAsStream(
				"sidplay/ini/" + FILE_NAME);
		System.out.println("Use internal INI file: " + "sidplay/ini/"
				+ FILE_NAME);

		try {
			iniReader = new IniReader(is);
			clear();
			/* Set the current version so that we detect old versions in future. */
			sidplay2Section.setVersion(REQUIRED_CONFIG_VERSION);
			is.close();
		} catch (final IOException e) {
			return;
		}
	}

	public void write() {
		if (!iniReader.isDirty()) {
			return;
		}

		try {
			iniReader.save(iniPath.getAbsolutePath());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final ISidPlay2Section getSidplay2() {
		return sidplay2Section;
	}

	@Override
	public final IC1541Section getC1541() {
		return c1541Section;
	}

	@Override
	public final IPrinterSection getPrinter() {
		return printerSection;
	}

	@Override
	public final IJoystickSection getJoystick() {
		return joystickSection;
	}

	@Override
	public final IConsoleSection getConsole() {
		return consoleSection;
	}

	@Override
	public final IAudioSection getAudio() {
		return audioSection;
	}

	@Override
	public final IEmulationSection getEmulation() {
		return emulationSection;
	}

}
