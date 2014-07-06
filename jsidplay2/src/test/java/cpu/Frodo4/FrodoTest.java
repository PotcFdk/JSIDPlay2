package cpu.Frodo4;

import static javafx.scene.input.KeyCode.ENTER;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import common.JSIDPlay2Test;

public class FrodoTest extends JSIDPlay2Test {

	@Before
	public void setup() {
		super.setup();
		config.getSidplay2().setLastDirectory("target/test-classes/cpu/Frodo4");
	};

	@Test
	public void testDADB() {
		click("#file");
		click("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("dadb.prg");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);

		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
		click("#screen");
		schedule((c64) -> c64.getKeyboard().keyPressed(KeyTableEntry.SPACE));
		sleep(2000);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
		schedule((c64) -> c64.getKeyboard().keyReleased(KeyTableEntry.SPACE));
		sleep(2000);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);

	}

	@Test
	public void testDE00all() {
		click("#file");
		click("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("de00all-sys49152.prg");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);

		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
		click("#screen");
		schedule((c64) -> c64.getKeyboard().keyPressed(KeyTableEntry.SPACE));
		sleep(2000);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
		schedule((c64) -> c64.getKeyboard().keyReleased(KeyTableEntry.SPACE));
		sleep(2000);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);

	}

	private void schedule(java.util.function.Consumer<C64> c) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafe(new Event("Test Event") {
					@Override
					public void event() throws InterruptedException {
						c.accept(player.getC64());
					}
				});
	}

}
