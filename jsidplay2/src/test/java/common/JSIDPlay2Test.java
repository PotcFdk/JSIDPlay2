package common;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotInterface;
import org.testfx.framework.junit.ApplicationTest;

import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidutils.Petscii;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.entities.config.Configuration;

public class JSIDPlay2Test extends ApplicationTest {

	/**
	 * Sleep time to wait for JSIDPlay2 to start.
	 */
	protected static final int JSIDPLAY2_STARTUP_SLEEP = 1000;
	/**
	 * Timeout for the file browser to open.
	 */
	protected static final int FILE_BROWSER_OPENED_TIMEOUT = 2000;
	/**
	 * Timeout until the C64 has been reset completely.
	 */
	protected static final int C64_RESET_TIMEOUT = 5000;
	/**
	 * Timeout for a thread-safe scheduled event.
	 */
	protected static final int SCHEDULE_THREADSAFE_TIMEOUT = 2000;
	/**
	 * Maximum Fast forward speed.
	 */
	protected static final int SPEED_FACTOR = 3;

	protected Configuration config;
	protected Player player;

	private boolean abortTest;

	/**
	 * Launch JSIDPlay2.
	 * 
	 * @throws Exception
	 *             launch error
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		try {
			if (JSidPlay2Main.getInstance() == null) {
				launch(JSidPlay2Main.class);
			}
			do {
				Thread.sleep(JSIDPLAY2_STARTUP_SLEEP);
			} while (JSidPlay2Main.getInstance() == null);
		} catch (InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Before
	public final void jsidplay2Before() {
		JSidPlay2Main.getInstance().setCloseActionEnabler(() -> {
			// abort test on close operation
			abortTest = true;
			// do not close application
			return false;
		});
		abortTest = false;
		config = JSidPlay2Main.getInstance().getUtil().getConfig();
		player = JSidPlay2Main.getInstance().getUtil().getPlayer();
		player.setMenuHook((player) -> {
		});
	}

	@Override
	public FxRobot sleep(long milliseconds) {
		if (abortTest) {
			fail("Application closed!");
		}
		// we want to stay responsive for application abort
		while (milliseconds > 500) {
			milliseconds -= 500;
			super.sleep(500);
			if (abortTest) {
				fail("Application closed!");
			}
		}
		return super.sleep(milliseconds);
	}

	/**
	 * Show primary stage.
	 * 
	 * @see org.testfx.framework.junit.ApplicationTest#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage stage) {
		stage.setOnCloseRequest(evt -> evt.consume());
		stage.show();
	}

	/**
	 * Prevent a bug hitting the wrong menu-item, when mouse is moved diagonal
	 * 
	 * @see org.testfx.api.FxRobotInterface#clickOn(java.lang.String,
	 *      javafx.scene.input.MouseButton[])
	 */
	@Override
	public FxRobotInterface clickOn(String query, MouseButton... buttons) {
		return super.clickOn(query, buttons).moveBy(1, 1);
	}

	/**
	 * Check C64 RAM contents.
	 * 
	 * @param address
	 *            start address
	 * @param expected
	 *            expected byte contents at the specified address
	 * @return RAM contents equals
	 */
	protected boolean checkRam(int address, byte[] expected) {
		final byte[] ram = player.getC64().getRAM();
		for (int i = 0; i < expected.length; i++) {
			if (ram[address + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check a message on C64 video screen RAM.
	 * 
	 * @param expected
	 *            expected message visible on the video screen
	 * @param row
	 *            expected message appears on this row
	 * @param column
	 *            expected message appears on this column
	 * @return Video RAM contains the specified message
	 */
	protected boolean checkScreenMessage(String expected, int row, int column) {
		final byte[] ram = player.getC64().getRAM();
		final int offset = ((row - 1) * 40) + (column - 1);
		for (int i = 0; i < expected.length(); i++) {
			final byte screenCode = Petscii.iso88591ToPetscii(expected.charAt(i));
			if (ram[0x0400 + offset + i] != screenCode) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Schedule a thread-safe C64 event.
	 * 
	 * @param eventConsumer
	 *            event consumer with event behavior
	 */
	protected void schedule(java.util.function.Consumer<C64> eventConsumer) {
		player.getC64().getEventScheduler().scheduleThreadSafe(new Event("Test Event") {
			@Override
			public void event() throws InterruptedException {
				eventConsumer.accept(player.getC64());
			}
		});
	}

	protected void fastForward(int speedFactor) {
		player.configureMixer(mixer -> {
			for (int i = 0; i < speedFactor; i++) {
				mixer.fastForward();
			}
		});
	}

}
