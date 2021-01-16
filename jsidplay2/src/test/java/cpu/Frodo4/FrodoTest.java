package cpu.Frodo4;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.B;
import static javafx.scene.input.KeyCode.D;
import static javafx.scene.input.KeyCode.DIGIT0;
import static javafx.scene.input.KeyCode.DIGIT1;
import static javafx.scene.input.KeyCode.DIGIT2;
import static javafx.scene.input.KeyCode.DIGIT4;
import static javafx.scene.input.KeyCode.DIGIT5;
import static javafx.scene.input.KeyCode.DIGIT9;
import static javafx.scene.input.KeyCode.E;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.G;
import static javafx.scene.input.KeyCode.L;
import static javafx.scene.input.KeyCode.MINUS;
import static javafx.scene.input.KeyCode.P;
import static javafx.scene.input.KeyCode.PERIOD;
import static javafx.scene.input.KeyCode.R;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.Y;
import static libsidplay.components.keyboard.KeyTableEntry.SPACE;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import common.JSIDPlay2Test;
import javafx.scene.input.KeyCode;

public class FrodoTest extends JSIDPlay2Test {

	@Before
	public void before() {
		config.getSidplay2Section().setLastDirectory(new File("src/test/resources/cpu/Frodo4"));
	}

	@Test
	public void testDADB() {
		clickOn("#VIDEO");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(D, A, D, B, PERIOD, P, R, G);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
		clickOn("#VIDEO");
		schedule(c64 -> c64.getKeyboard().keyPressed(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
		schedule(c64 -> c64.getKeyboard().keyReleased(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
	}

	@Test
	public void testDE00all() {
		clickOn("#VIDEO");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(D, E, DIGIT0, DIGIT0, A, L, L, MINUS, S, Y, S, DIGIT4, DIGIT9, DIGIT1, DIGIT5, DIGIT2, PERIOD, P, R, G);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
		clickOn("#VIDEO");
		schedule(c64 -> c64.getKeyboard().keyPressed(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 1);
		schedule(c64 -> c64.getKeyboard().keyReleased(SPACE));
		sleep(SCHEDULE_THREADSAFE_TIMEOUT);
		Assert.assertTrue(player.getC64().getVIC().getRegisters()[0x20] == 0);
	}

}
