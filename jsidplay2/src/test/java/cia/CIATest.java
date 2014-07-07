package cia;

import static javafx.scene.input.KeyCode.ENTER;

import org.junit.Assert;
import org.junit.Test;

import common.JSIDPlay2Test;

public class CIATest extends JSIDPlay2Test {

	@Override
	public void setup() {
		super.setup();
		config.getSidplay2().setLastDirectory("target/test-classes/cia");
	}

	@Test
	public void detectCIA6526() {
		click("#file");
		click("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("cia_detection.prg");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);

		Assert.assertTrue(player.getC64().getRAM()[0xc093] == 0x0c);
	}
	
	@Test
	public void timerBCountsA() {
		click("#file");
		click("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("cmp-b-counts-a.prg");
		push(ENTER);
		sleep(15000);

		Assert.assertTrue((player.getC64().getVIC().getRegisters()[0x20] & 0xff) == 0xff);
	}
}
