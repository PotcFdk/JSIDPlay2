package common;

import javafx.scene.Parent;
import javafx.stage.Stage;
import libpsid64.Screen;
import libsidplay.Player;

import org.junit.Before;
import org.loadui.testfx.utils.FXTestUtils;

import ui.JSIDPlay2Main;
import ui.entities.config.Configuration;

public class JSIDPlay2Test extends GuiTest {
	protected static final int FILE_BROWSER_OPENED_TIMEOUT = 1000;
	protected static final int SID_TUNE_LOADED_TIMEOUT = 5000;

	protected Configuration config;
	protected Player player;

	@Before
	public void setup() {
		config = JSIDPlay2Main.getInstance().getUtil().getConfig();
		player = JSIDPlay2Main.getInstance().getUtil().getPlayer();
	}

	protected boolean checkRam(int address, byte[] expected) {
		final byte[] ram = player.getC64().getRAM();
		for (int i = 0; i < expected.length; i++) {
			if (ram[address + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	protected boolean checkScreenMessage(String expected, int row, int column) {
		final byte[] ram = player.getC64().getRAM();
		final int offset = ((row - 1) * 40) + (column - 1);
		for (int i = 0; i < expected.length(); i++) {
			final byte screenCode = Screen.iso2scr(expected.charAt(i));
			if (ram[0x0400 + offset + i] != screenCode) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected Stage launchApp() {
		FXTestUtils.launchApp(JSIDPlay2Main.class);
		while (JSIDPlay2Main.getInstance() == null) {
			sleep(1000);
		}
		return JSIDPlay2Main.getInstance().getStage();
	}

	@Override
	protected Parent getRootNode() {
		return stage.getScene().getRoot();
	}
}
