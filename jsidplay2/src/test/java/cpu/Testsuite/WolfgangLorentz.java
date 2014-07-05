package cpu.Testsuite;

import static javafx.scene.input.KeyCode.ENTER;
import static libsidplay.sidtune.SidTune.RESET;

import org.junit.Assert;
import org.junit.Test;

import common.JSIDPlay2Test;

public class WolfgangLorentz extends JSIDPlay2Test {

	@Test
	public void testDisk1() {
		config.getSidplay2().setLastDirectory(
				"target/test-classes/cpu/Testsuite/d64");
		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk1.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		player.setCommand("LOAD\"*\",8\rRUN\r");
		player.play(RESET);
		while (!checkDiskChange(2)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk2.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		click("#screen");
		press(ENTER);
		player.setCommand("LOAD\"*\",8,1\rRUN\r");
		while (!checkDiskChange(3)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}

		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk3.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		click("#screen");
		press(ENTER);
		player.setCommand("LOAD\"*\",8,1\rRUN\r");
		while (!checkScreenMessage("ready.", 39, 1)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	private boolean checkTestFailed() {
		for (int i = 1; i < 40; i++) {
			if (checkScreenMessage("before", i, 1)
					|| checkScreenMessage("init", i, 1)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkDiskChange(int num) {
		for (int i = 1; i < 40; i++) {
			if (checkScreenMessage("please insert disk " + num, i, 1)) {
				return true;
			}
		}
		return false;
	}
}
