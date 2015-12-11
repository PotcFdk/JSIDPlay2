package common;

import javafx.scene.Parent;
import javafx.stage.Stage;
import libpsid64.Screen;
import libsidplay.C64;
import libsidplay.common.Event;

import org.junit.Before;
import org.loadui.testfx.utils.FXTestUtils;

import sidplay.Player;
import ui.JSidPlay2Main;
import ui.JSidPlay2;
import ui.entities.config.Configuration;

public class JSIDPlay2Test extends GuiTest {

	protected Configuration config;
	protected Player player;

	@Before
	public void setup() {
		config = JSidPlay2Main.getInstance().getUtil().getConfig();
		player = JSidPlay2Main.getInstance().getUtil().getPlayer();
		player.setMenuHook((player) -> {
		});

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

	protected void schedule(java.util.function.Consumer<C64> c) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafe(new Event("Test Event") {
					@Override
					public void event() throws InterruptedException {
						c.accept(player.getC64());
					}
				});
	}

	@Override
	protected Stage launchApp() {
		FXTestUtils.launchApp(JSidPlay2Main.class);
		while (JSidPlay2Main.getInstance() == null) {
			sleep(1000);
		}
		final JSidPlay2 instance = JSidPlay2Main.getInstance();
		instance.getStage().setOnCloseRequest((evt) -> evt.consume());
		return instance.getStage();
	}

	@Override
	protected Parent getRootNode() {
		return stage.getScene().getRoot();
	}
}
