package sid;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.D;
import static javafx.scene.input.KeyCode.DIGIT0;
import static javafx.scene.input.KeyCode.DIGIT2;
import static javafx.scene.input.KeyCode.DIGIT3;
import static javafx.scene.input.KeyCode.E;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyCode.G;
import static javafx.scene.input.KeyCode.H;
import static javafx.scene.input.KeyCode.I;
import static javafx.scene.input.KeyCode.L;
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
import static javafx.scene.input.KeyCode.W;
import static javafx.scene.input.KeyCode.X;
import static javafx.scene.input.KeyCode.Y;
import static javafx.scene.input.KeyCode.Z;

import org.junit.Assert;
import org.junit.Test;

import common.JSIDPlay2Test;
import javafx.scene.input.KeyCode;
import libsidplay.common.ChipModel;

public class SidTuneTest extends JSIDPlay2Test {

	@Test
	public void detectSidModelTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid");

		config.getEmulationSection().setUserSidModel(ChipModel.AUTO);
		config.getEmulationSection().setDefaultSidModel(ChipModel.MOS8580);
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(S, I, D).press(SHIFT).type(UNDERSCORE).release(SHIFT).type(D, E, T, E, C, T, I, O, N, PERIOD, P, R, G);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);
		Assert.assertTrue(checkScreenMessage("b", 1, 1));

		config.getEmulationSection().setDefaultSidModel(ChipModel.MOS6581);
		player.play(player.getTune());
		sleep(C64_RESET_TIMEOUT);
		Assert.assertTrue(checkScreenMessage("a", 1, 1));
	}

	@Test
	public void loadPSidStereoTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples/stereo/2sids/Nata");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(A).release(SHIFT).type(N, T, H, R, O, X).press(SHIFT).type(UNDERSCORE).release(SHIFT)
				.type(DIGIT2).press(SHIFT).type(S, I, D).release(SHIFT).type(PERIOD, S, I, D);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		player.getC64().configureSID(0, sid -> Assert.assertNotNull(sid));
		player.getC64().configureSID(1, sid -> Assert.assertNotNull(sid));
		// Driver
		Assert.assertTrue(checkRam(0x0400, new byte[] { 0x4c, 0x03, 0x10 }));
		// Load/Init
		Assert.assertTrue(checkRam(0x1000, new byte[] { 0x4c, 0x06, 0x10 }));
		// Play
		Assert.assertTrue(checkRam(0x1003, new byte[] { 0x4c, 0x40, 0x10 }));
	}

	@Test
	public void loadPSidTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(T).release(SHIFT).type(U, R, R, I, C, A, N).press(SHIFT).type(UNDERSCORE).release(SHIFT)
				.type(DIGIT2, MINUS).press(SHIFT).type(T).release(SHIFT).type(H, E).press(SHIFT).type(UNDERSCORE)
				.type(F).release(SHIFT).type(I, N, A, L).press(SHIFT).type(UNDERSCORE).type(F).release(SHIFT)
				.type(I, G, H, T, PERIOD, S, I, D);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		// Driver
		Assert.assertTrue(checkRam(0xc000, new byte[] { 0x4c, 0x00, 0x08 }));
		// Load/Play
		Assert.assertTrue(checkRam(0x0800, new byte[] { 0x4c, (byte) 0xb6, 0x66 }));
		// Init
		Assert.assertTrue(checkRam(0x0803, new byte[] { (byte) 0xa2, 0x35 }));
	}

	@Test
	public void loadP00Test() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(P, U, Z, Z, L, E, N, D).release(SHIFT).type(PERIOD, P, DIGIT0, DIGIT0);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		Assert.assertTrue(checkRam(0x0801, new byte[] { (byte) 0xde, 0x38, (byte) 0xa9 }));
	}

	@Test
	public void loadMusTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(A).release(SHIFT).type(N, G, I, E).press(SHIFT).type(UNDERSCORE).type(A).release(SHIFT)
				.type(PERIOD, M, U, S);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		// Driver
		Assert.assertTrue(checkRam(0x0400, new byte[] { 0x4c, (byte) 0x80, (byte) 0xec }));
		// Load
		Assert.assertTrue(checkRam(0x0900, new byte[] { (byte) 0xc9, 0x28 }));
		// Init
		Assert.assertTrue(checkRam(0xec60, new byte[] { (byte) 0xa2, 0x51 }));
		// Play
		Assert.assertTrue(checkRam(0xec80, new byte[] { (byte) 0xa9, 0x07 }));
	}

	@Test
	public void loadMusStereoTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples/stereo");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(S).release(SHIFT).type(A, F, E, T, Y).press(SHIFT).type(UNDERSCORE).type(D).release(SHIFT)
				.type(A, N, C, E, PERIOD, M, U, S);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		player.getC64().configureSID(0, sid -> Assert.assertNotNull(sid));
		player.getC64().configureSID(1, sid -> Assert.assertNotNull(sid));
		// Driver
		Assert.assertTrue(checkRam(0x0400, new byte[] { 0x4c, (byte) 0x96, (byte) 0xfc }));
		// Load
		Assert.assertTrue(checkRam(0x0900, new byte[] { 0x00 }));
		// Init
		Assert.assertTrue(checkRam(0xfc90, new byte[] { 0x20, 0x60, (byte) 0xec }));
		// Play
		Assert.assertTrue(checkRam(0xfc96, new byte[] { 0x20, (byte) 0x80, (byte) 0xec }));
	}

	@Test
	public void loadPrgTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		type(R, A, D, I, A, N, T, X).press(SHIFT).type(UNDERSCORE).release(SHIFT).type(S, P, I, R, A, L).press(SHIFT)
				.type(UNDERSCORE).release(SHIFT).type(S, I, L, I, C, O, N).press(SHIFT).type(UNDERSCORE).release(SHIFT)
				.type(T, O, W, E, R, S, PERIOD, P, R, G);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		Assert.assertTrue(checkScreenMessage("Spiral Silicon Towers", 2, 1));
	}

	@Test
	public void loadPSid3SidTest() {
		config.getSidplay2Section().setLastDirectory("src/test/resources/sid/examples/stereo/3sids");
		clickOn("#file");
		clickOn("#load");
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		press(SHIFT).type(A).release(SHIFT).type(R, C, A, D, E).press(SHIFT).type(UNDERSCORE).type(M)
				.release(SHIFT).type(E, M, O, R, I, E, S).press(SHIFT).type(UNDERSCORE).release(SHIFT).type(DIGIT3)
				.press(SHIFT).type(S, I, D).release(SHIFT).type(PERIOD, S, I, D);
		push(ENTER);
		sleep(C64_RESET_TIMEOUT);

		player.getC64().configureSID(0, sid -> Assert.assertNotNull(sid));
		player.getC64().configureSID(1, sid -> Assert.assertNotNull(sid));
		player.getC64().configureSID(2, sid -> Assert.assertNotNull(sid));
		// Driver
		Assert.assertTrue(checkRam(0x0400, new byte[] { 0x4c, (byte) 0x93, 0x28 }));
		// Load/Init
		Assert.assertTrue(checkRam(0x1000, new byte[] { 0x4c, (byte) 0xF1, 0x10 }));
		// Play
		Assert.assertTrue(checkRam(0x2890, new byte[] { 0x4c, (byte) 0x9C, 0x28 }));
	}

}