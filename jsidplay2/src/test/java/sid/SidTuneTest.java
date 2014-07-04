package sid;

import static javafx.scene.input.KeyCode.ENTER;
import javafx.scene.control.Label;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

import common.JSIDPlay2Test;

public class SidTuneTest extends JSIDPlay2Test {
	@Before
	public void setUp() {
		config.getSidplay2().setLastDirectory(
				"target/test-classes/sid/examples");
	}

	@Test
	public void loadPSidStereoTest() {
		config.getSidplay2().setLastDirectory(
				"target/test-classes/sid/examples/stereo/2sids/Nata");
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.sid");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		Assert.assertTrue(player.getC64().getSID(0) != null);
		Assert.assertTrue(player.getC64().getSID(1) != null);
		// Driver
		assertRam(0x0400, new byte[] { 0x4c, 0x03, 0x10 });
		// Load/Init
		assertRam(0x1000, new byte[] { 0x4c, 0x06, 0x10 });
		// Play
		assertRam(0x1003, new byte[] { 0x4c, 0x40, 0x10 });
	}

	@Test
	public void loadPSidTest() {
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.sid");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		// Driver
		assertRam(0xc000, new byte[] { 0x4c, 0x00, 0x08 });
		// Load/Play
		assertRam(0x0800, new byte[] { 0x4c, (byte) 0xb6, 0x66 });
		// Init
		assertRam(0x0803, new byte[] { (byte) 0xa2, 0x35 });
	}

	@Test
	public void loadP00Test() {
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.p00");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		assertRam(0x0801, new byte[] { (byte) 0xde, 0x38, (byte) 0xa9 });
	}

	@Test
	public void loadMusTest() {
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.mus");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		// Driver
		assertRam(0x0400, new byte[] { 0x4c, (byte) 0x80, (byte) 0xec });
		// Load
		assertRam(0x0900, new byte[] { (byte) 0xc9, 0x28 });
		// Init
		assertRam(0xec60, new byte[] { (byte) 0xa2, 0x51 });
		// Play
		assertRam(0xec80, new byte[] { (byte) 0xa9, 0x07 });
	}

	@Test
	public void loadMusStereoTest() {
		config.getSidplay2().setLastDirectory(
				"target/test-classes/sid/examples/stereo");
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.mus");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		Assert.assertTrue(player.getC64().getSID(0) != null);
		Assert.assertTrue(player.getC64().getSID(1) != null);
		// Driver
		assertRam(0x0400, new byte[] { 0x4c, (byte) 0x96, (byte) 0xfc });
		// Load
		assertRam(0x0900, new byte[] { 0x00 });
		// Init
		assertRam(0xfc90, new byte[] { 0x20, 0x60, (byte) 0xec });
		// Play
		assertRam(0xfc96, new byte[] { 0x20, (byte) 0x80, (byte) 0xec });
	}

	@Test
	public void loadPrgTest() {
		ctrl.click("#file");
		ctrl.click("#load");
		ctrl.sleep(FILE_BROWSER_OPENED_TIMEOUT);
		ctrl.type("test.prg");
		ctrl.press(ENTER).release(ENTER);
		ctrl.sleep(SID_TUNE_LOADED_TIMEOUT);

		assertScreenMessage("Spiral Silicon Towers", 2, 1);
	}

	@Test
	public void statusTest() {
		Label status = ((Label) GuiTest.find("#status"));

		Assert.assertNotNull(status);
	}

}