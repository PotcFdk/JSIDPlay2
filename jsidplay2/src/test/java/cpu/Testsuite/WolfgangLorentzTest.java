package cpu.Testsuite;

import static javafx.scene.input.KeyCode.ENTER;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import sidplay.player.State;
import common.JSIDPlay2Test;

public class WolfgangLorentzTest extends JSIDPlay2Test {

	@Before
	public void setup() {
		super.setup();
		config.getSidplay2Section().setLastDirectory(
				"target/test-classes/cpu/Testsuite/d64");
	};

	@Test
	public void testDisk1() {
		click("#VIDEO");
		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk1.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		while (!checkDiskChange(2)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	@Test
	public void testDisk2() {
		click("#VIDEO");
		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk2.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		while (!checkDiskChange(3)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	@Test
	public void testDisk3() {
		click("#VIDEO");
		click("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		type("Disk3.d64");
		push(ENTER);
		sleep(SID_TUNE_LOADED_TIMEOUT);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		while (!checkFinish()) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	private boolean checkTestFailed() {
		if (player.stateProperty().get().equals(State.QUIT)) {
			return true;
		}
		for (int i = 1; i < 40; i++) {
			if (checkScreenMessage("before", i, 1)
					|| checkScreenMessage("init", i, 1)
					|| checkScreenMessage("right", i, 1)) {
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

	private boolean checkFinish() {
		for (int i = 1; i < 40; i++) {
			if (checkScreenMessage("completed", i, 18)) {
				return true;
			}
		}
		return false;
	}
}
