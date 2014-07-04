package common;

import javafx.scene.Parent;
import libpsid64.Screen;
import libsidplay.Player;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.loadui.testfx.GuiTest;
import org.loadui.testfx.utils.FXTestUtils;

import ui.JSIDPlay2Main;
import ui.entities.config.Configuration;

public class JSIDPlay2Test {
	protected static final int FILE_BROWSER_OPENED_TIMEOUT = 1000;
	protected static final int SID_TUNE_LOADED_TIMEOUT = 5000;

	protected static GuiTest ctrl;

	protected static Configuration config;
	protected static Player player;

	@BeforeClass
	public static void setUpClass() {
		FXTestUtils.launchApp(JSIDPlay2Main.class);
		ctrl = new GuiTest() {
			@Override
			protected Parent getRootNode() {
				return JSIDPlay2Main.getInstance().getStage().getScene()
						.getRoot();
			}
		};
		while (JSIDPlay2Main.getInstance() == null)
			ctrl.sleep(500);
		config = JSIDPlay2Main.getInstance().getUtil().getConfig();
		player = JSIDPlay2Main.getInstance().getUtil().getPlayer();
		player.setMenuHook((player) -> {
		});
	}

	protected void assertRam(int address, byte[] expected) {
		final byte[] ram = player.getC64().getRAM();
		for (int i = 0; i < expected.length; i++) {
			final String message = String.format("%d(%d)!=%d(%d)", ram[address
					+ i], address + i, expected[i], i);
			Assert.assertTrue(message, ram[address + i] == expected[i]);
		}
	}

	protected void assertScreenMessage(String expected, int row, int column) {
		final byte[] ram = player.getC64().getRAM();
		final int offset = ((row - 1) * 40) + (column - 1);
		for (int i = 0; i < expected.length(); i++) {
			final byte screenCode = Screen.iso2scr(expected.charAt(i));
			final String message = String.format("%d(%d)!=%d(%d)", ram[0x0400
					+ offset + i], offset + i, screenCode, i);
			Assert.assertTrue(message, ram[0x0400 + offset + i] == screenCode);
		}
	}
}
