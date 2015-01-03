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
package netsiddev.ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sidplay.ini.IniAudioSection;
import sidplay.ini.IniFilterSection;
import sidplay.ini.IniReader;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IFilterSection;

/**
 * INI configuration file support responsible to load and save all emulator
 * settings.
 * 
 * @author Ken Händel
 * 
 */
public class JSIDDeviceConfig {
	/** Bump this each time you want to invalidate jsiddevice.ini files on disk */
	protected static final int REQUIRED_CONFIG_VERSION = 1;

	/** Name of our config file. */
	private static final String FILE_NAME = "jsiddevice.ini";

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	/** Device name prefix of JSIDDEVICE 1.0 */
	private static final String JSIDDEVICE1_NAME_PREFIX = "JSidDevice";
	
	private IniJSIDDeviceSection jsiddeviceSection;

	private IAudioSection audioSection;

	protected IniReader iniReader;

	private String[] filterList;
	
	private void clear() {
		jsiddeviceSection = new IniJSIDDeviceSection(iniReader);
		audioSection = new IniAudioSection(iniReader);
		
		final List<String> filters = new ArrayList<String>();
		final List<String> filtersResidfp = new ArrayList<String>();
		final List<String> filtersResid = new ArrayList<String>();
		
		for (final String heading : iniReader.listSections()) {
			if (!heading.matches("Filter.*")) {
				continue;
			}
			
			String filterName = heading.substring(6);
			
			if (filterName.startsWith(JSIDDEVICE1_NAME_PREFIX)) {
				filters.add(filterName);
			} else {
				IFilterSection iniFilter =  getFilter(filterName);
				if (iniFilter.isReSIDfpFilter6581() || iniFilter.isReSIDfpFilter8580()) {
					filtersResidfp.add(filterName);
				} else {
					filtersResid.add(filterName);
				}
			}
		}

		Collections.sort(filters);
		Collections.sort(filtersResidfp);
		Collections.sort(filtersResid);

		filters.addAll(filtersResid);
		filters.addAll(filtersResidfp);
		
		filterList = filters.toArray(new String[] {});
	}

	public IFilterSection getFilter(final String filterName) {
		return new IniFilterSection(iniReader, "Filter" + filterName);
	}

	public String[] getFilterList() {
		return filterList;
	}

	public JSIDDeviceConfig() {
		iniPath = getINIPath();
		read();
	}

	private void read() {
		if (iniPath != null && iniPath.exists()) {
			try {
				iniReader = new IniReader(new FileInputStream(iniPath));
				clear();
				/* validate loaded configuration */
				if (jsiddeviceSection.getVersion() == REQUIRED_CONFIG_VERSION) {
					return;
				}
				createINIBackup(iniPath);
			} catch (final Exception e) {
				System.err.println("INI reading failed: " + e.getMessage());
			}

			System.out.println("INI file old/broken (version=" + jsiddeviceSection.getVersion() + "). Using default settings.");
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
		iniFile.renameTo(new File(iniFile.getParent(), FILE_NAME + ".bak"));
	}

	private void readInternal() {
		final InputStream is = getClass().getClassLoader().getResourceAsStream("netsiddev/ini/" + FILE_NAME);

		try {
			iniReader = new IniReader(is);
			clear();
			is.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
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

	public IniJSIDDeviceSection jsiddevice() {
		return jsiddeviceSection;
	}

	public final IAudioSection audio() {
		return audioSection;
	}

}
