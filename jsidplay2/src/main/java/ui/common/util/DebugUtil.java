package ui.common.util;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ui.JSidPlay2Main;

public class DebugUtil {

	private static final String LOG_CONFIG_RES = "/sidplay/logconfig.properties";

	public static void init() {
		try {
			// turn off HSQL logging re-configuration
			System.setProperty("hsqldb.reconfig_logging", "false");
			// configure JSIDPlay2 logging (java util logging)
			LogManager.getLogManager().readConfiguration(JSidPlay2Main.class.getResourceAsStream(LOG_CONFIG_RES));
		} catch (final IOException e) {
			Logger.getAnonymousLogger().severe("Could not load " + LOG_CONFIG_RES + ": " + e.getMessage());
		}
	}
}
