package common;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotInterface;
import org.testfx.framework.junit.ApplicationTest;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidutils.Petscii;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.entities.config.Configuration;

public class JSIDPlay2Test extends ApplicationTest implements Timeouts {

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
		launch(JSidPlay2Main.class);
		try {
			do {
				Thread.sleep(JSIDPLAY2_STARTUP_SLEEP);
			} while (JSidPlay2Main.getInstance() == null);
		} catch (InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Before
	public void setup() throws AssertionError {
		JSidPlay2Main.getInstance().setCloseAction(() -> {
			abortTest = true;
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
		return super.sleep(milliseconds);
	}

	@Override
	public FxRobot sleep(long duration, TimeUnit timeUnit) {
		if (abortTest) {
			fail("Application closed!");
		}
		return super.sleep(duration, timeUnit);
	}

	/**
	 * Show primary stage.
	 * 
	 * @see org.testfx.framework.junit.ApplicationTest#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage stage) {
		stage.setOnCloseRequest((evt) -> evt.consume());
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
	 * Types the given text on the keyboard. Note: Typing depends on the operating
	 * system keyboard layout!
	 *
	 * @param text
	 */
	public void type(String text) {
		for (char ch : text.toCharArray()) {
			boolean isShiftedCharacter = ch == '_' || Character.isUpperCase(ch);
			if (isShiftedCharacter) {
				press(KeyCode.SHIFT);
			}
			type(determineKeyCode(ch));
			if (isShiftedCharacter) {
				release(KeyCode.SHIFT);
			}
			sleep(100);
		}
	}

	/**
	 * Type Ctrl-A to select all text.
	 */
	public void selectAll() {
		press(KeyCode.CONTROL);
		type(KeyCode.A);
		release(KeyCode.CONTROL);
		sleep(100);
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

	/**
	 * Convert character to KeyCode.
	 * 
	 * @param character
	 *            character to convert
	 * @return converted character as KeyCode
	 */
	private KeyCode determineKeyCode(char character) {
		KeyCode key = KeyCode.UNDEFINED;
		key = (character == '\n') ? KeyCode.ENTER : key;
		key = (character == '\t') ? KeyCode.TAB : key;
		switch (character) {
		case 'a':
		case 'A':
			return KeyCode.A;
		case 'b':
		case 'B':
			return KeyCode.B;
		case 'c':
		case 'C':
			return KeyCode.C;
		case 'd':
		case 'D':
			return KeyCode.D;
		case 'e':
		case 'E':
			return KeyCode.E;
		case 'f':
		case 'F':
			return KeyCode.F;
		case 'g':
		case 'G':
			return KeyCode.G;
		case 'h':
		case 'H':
			return KeyCode.H;
		case 'i':
		case 'I':
			return KeyCode.I;
		case 'j':
		case 'J':
			return KeyCode.J;
		case 'k':
		case 'K':
			return KeyCode.K;
		case 'l':
		case 'L':
			return KeyCode.L;
		case 'm':
		case 'M':
			return KeyCode.M;
		case 'n':
		case 'N':
			return KeyCode.N;
		case 'o':
		case 'O':
			return KeyCode.O;
		case 'p':
		case 'P':
			return KeyCode.P;
		case 'q':
		case 'Q':
			return KeyCode.Q;
		case 'r':
			return KeyCode.R;
		case 's':
		case 'S':
			return KeyCode.S;
		case 't':
		case 'T':
			return KeyCode.T;
		case 'u':
		case 'U':
			return KeyCode.U;
		case 'v':
		case 'V':
			return KeyCode.V;
		case 'w':
		case 'W':
			return KeyCode.W;
		case 'x':
		case 'X':
			return KeyCode.X;
		case 'y':
		case 'Y':
			return KeyCode.Y;
		case 'z':
		case 'Z':
			return KeyCode.Z;
		case '_':
			return KeyCode.UNDERSCORE;
		case '-':
			return KeyCode.MINUS;
		case '.':
			return KeyCode.PERIOD;
		case '0':
			return KeyCode.DIGIT0;
		case '1':
			return KeyCode.DIGIT1;
		case '2':
			return KeyCode.DIGIT2;
		case '3':
			return KeyCode.DIGIT3;
		case '4':
			return KeyCode.DIGIT4;
		case '5':
			return KeyCode.DIGIT5;
		case '6':
			return KeyCode.DIGIT6;
		case '7':
			return KeyCode.DIGIT7;
		case '8':
			return KeyCode.DIGIT8;
		case '9':
			return KeyCode.DIGIT9;

		default:
			break;
		}
		return key;
	}

	protected void fastForward(int speedFactor) {
		sleep(C64_RESET_TIMEOUT);
		player.configureMixer(mixer -> {
			for (int i = 0; i < speedFactor; i++) {
				mixer.fastForward();
			}
		});
	}

}
