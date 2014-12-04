package cpu.Frodo4;

import static javafx.scene.input.KeyCode.ENTER;
import static libsidplay.components.keyboard.KeyTableEntry.SPACE;

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
		click("#VIDEO");
		schedule(c64 -> c64.getKeyboard().keyPressed(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
		schedule(c64 -> c64.getKeyboard().keyReleased(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
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
		click("#VIDEO");
		schedule(c64 -> c64.getKeyboard().keyPressed(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
		schedule(c64 -> c64.getKeyboard().keyReleased(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
	}

}
