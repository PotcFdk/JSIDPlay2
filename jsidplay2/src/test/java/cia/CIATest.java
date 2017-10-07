package cia;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.B;
import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.D;
import static javafx.scene.input.KeyCode.E;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.G;
import static javafx.scene.input.KeyCode.I;
import static javafx.scene.input.KeyCode.M;
import static javafx.scene.input.KeyCode.MINUS;
import static javafx.scene.input.KeyCode.N;
import static javafx.scene.input.KeyCode.O;
import static javafx.scene.input.KeyCode.P;
import static javafx.scene.input.KeyCode.PERIOD;
import static javafx.scene.input.KeyCode.R;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.SHIFT;
import static javafx.scene.input.KeyCode.T;
import static javafx.scene.input.KeyCode.U;
import static javafx.scene.input.KeyCode.UNDERSCORE;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import common.JSIDPlay2Test;
import javafx.scene.input.KeyCode;

public class CIATest extends JSIDPlay2Test {

	@Before
	public void before() {
		config.getSidplay2Section().setLastDirectory("target/test-classes/cia");
	}

	@Test
	public void detectCIA6526() {
		clickOn("#VIDEO");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(C, I, A).press(SHIFT).type(UNDERSCORE).release(SHIFT).type(D, E, T, E, C, T, I, O, N, PERIOD, P, R, G);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		Assert.assertTrue(player.getC64().getRAM()[0xc093] == 0x0c);
	}

	@Test
	public void timerBCountsA() {
		clickOn("#VIDEO");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(C, M, P, MINUS, B, MINUS, C, O, U, N, T, S, MINUS, A, PERIOD, P, R, G);
		push(ENTER);
		sleep(15000);

		Assert.assertTrue((player.getC64().getVIC().getRegisters()[0x20] & 0xff) == 0xff);
	}
}
