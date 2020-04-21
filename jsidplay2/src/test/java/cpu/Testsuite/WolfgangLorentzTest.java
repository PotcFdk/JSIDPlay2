package cpu.Testsuite;

import static javafx.scene.input.KeyCode.D;
import static javafx.scene.input.KeyCode.DIGIT1;
import static javafx.scene.input.KeyCode.DIGIT2;
import static javafx.scene.input.KeyCode.DIGIT3;
import static javafx.scene.input.KeyCode.DIGIT4;
import static javafx.scene.input.KeyCode.DIGIT6;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.I;
import static javafx.scene.input.KeyCode.K;
import static javafx.scene.input.KeyCode.PERIOD;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.SHIFT;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import common.JSIDPlay2Test;
import javafx.scene.input.KeyCode;
import sidplay.player.State;

public class WolfgangLorentzTest extends JSIDPlay2Test {

	@Before
	public void before() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/cpu/Testsuite/d64");
	}

	@Test
	public void testDisk1() {
		clickOn("#VIDEO");
		clickOn("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(D).release(SHIFT).type(I, S, K, DIGIT1, PERIOD, D, DIGIT6, DIGIT4);
		push(ENTER);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		sleep(C64_RESET_TIMEOUT);
		fastForward(SPEED_FACTOR);
		while (!checkDiskChange(2)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	@Test
	public void testDisk2() {
		clickOn("#VIDEO");
		clickOn("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(D).release(SHIFT).type(I, S, K, DIGIT2, PERIOD, D, DIGIT6, DIGIT4);
		push(ENTER);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		sleep(C64_RESET_TIMEOUT);
		fastForward(SPEED_FACTOR);
		while (!checkDiskChange(3)) {
			sleep(10000);
			if (checkTestFailed()) {
				Assert.fail();
			}
		}
	}

	@Test
	public void testDisk3() {
		clickOn("#VIDEO");
		clickOn("#floppy");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(D).release(SHIFT).type(I, S, K, DIGIT3, PERIOD, D, DIGIT6, DIGIT4);
		push(ENTER);
		player.resetC64("LOAD\"*\",8\rRUN\r");
		sleep(C64_RESET_TIMEOUT);
		fastForward(SPEED_FACTOR);
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
			if (checkScreenMessage("before", i, 1) || checkScreenMessage("init", i, 1)
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
