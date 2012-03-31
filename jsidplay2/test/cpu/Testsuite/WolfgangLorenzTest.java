package cpu.Testsuite;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.timing.Pause.pause;

import java.awt.Dimension;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JFrame;

import libsidplay.C64;
import libsidplay.common.Event;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.junit.v4_5.runner.GUITestRunner;
import org.fest.swing.testing.FestSwingTestCaseTemplate;
import org.fest.swing.timing.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.swixml.SwingEngine;

import applet.JSIDPlay2;

/**
 * This test executes the Wolfgang Lorenz Test Suite. All 3 disks are loaded and
 * all the tests are executed. If an error occurs a screenshot will be taken of
 * the desktop, that we can check what happened.
 * 
 * @author Ken
 * 
 */
@GUITest
@RunWith(GUITestRunner.class)
public class WolfgangLorenzTest extends FestSwingTestCaseTemplate {

	/**
	 * Maximum expected test time per disk (timeout check).
	 */
	private static final int MAX_TEST_TIME = 60 * 60;
	/**
	 * User Interface to test.
	 */
	protected static FrameFixture window;
	/**
	 * Implementation to test.
	 */
	protected static final JSIDPlay2 sp = new JSIDPlay2(new String[0]);
	/**
	 * Commodore 64.
	 */
	protected final C64 c64 = sp.getPlayer().getC64();
	/**
	 * System RAM used for assertions.
	 */
	protected final byte[] ram = c64.getRAM();
	/**
	 * Last time a test has been started.
	 */
	protected int startTime;

	/**
	 * Create the UI to test.
	 */
	@BeforeClass
	public static void setUp() {
		JFrame frameFix = GuiActionRunner.execute(new GuiQuery<JFrame>() {
			protected JFrame executeInEDT() {
				sp.init();
				sp.start();
				JFrame frame = null;
				try {
					frame = (JFrame) new SwingEngine(sp).render(JSIDPlay2.class
							.getResource("JSIDPlay2.xml"));
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					frame.add(sp);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return frame;
			}
		});
		window = new FrameFixture(frameFix);
		window.show(new Dimension(1024, 768));
	}

	/**
	 * Create the UI to test.
	 */
	@AfterClass
	public static void tearDown() {
		GuiActionRunner.execute(new GuiQuery<Boolean>() {
			protected Boolean executeInEDT() {
				sp.stop();
				sp.destroy();
				return Boolean.TRUE;
			}
		});
		window.close();
	}

	/**
	 * Test Case of the Wolfgang Lorenz test suite (Disk 1).
	 * 
	 * @throws URISyntaxException
	 *             resource not found
	 */
	@Test
	public void testWolfgangLorenzTestSuiteDisk1() throws URISyntaxException {
		wolfgangLorenzTest("d64/Disk1.d64", 1);
	}

	/**
	 * Test Case of the Wolfgang Lorenz test suite (Disk 2).
	 * 
	 * @throws URISyntaxException
	 *             resource not found
	 */
	@Test
	public void testWolfgangLorenzTestSuiteDisk2() throws URISyntaxException {
		wolfgangLorenzTest("d64/Disk2.d64", 2);
	}

	/**
	 * Test Case of the Wolfgang Lorenz test suite (Disk 3).
	 * 
	 * @throws URISyntaxException
	 *             resource not found
	 */
	@Test
	public void testWolfgangLorenzTestSuiteDisk3() throws URISyntaxException {
		wolfgangLorenzTest("d64/Disk3.d64", 3);
	}

	/**
	 * Insert a disk containing the tests, load the first entry and run.
	 * 
	 * @param testResource
	 *            disk to load
	 * @param diskNum
	 *            disk number
	 * @throws URISyntaxException
	 *             resource not found
	 */
	protected void wolfgangLorenzTest(final String testResource,
			final int diskNum) throws URISyntaxException {
		// Insert disk
		window.button("Floppy").click();
		window.fileChooser("InsertDisk").fileNameTextBox()
				.setText(getFilename(testResource));
		window.fileChooser("InsertDisk").approve();
		// Type in load instruction
		c64.getEventScheduler().schedule(new Event("Load First entry of Disk") {
			@Override
			public void event() throws InterruptedException {
				if (diskNum == 1) {
					// First disk? Load first test
					enterC64BasicCmd("LOAD\"*\",8,1\rRUN\r");
				} else {
					// Next disks notify of disk swapping
					enterC64BasicCmd("\r");
				}
				startTime = sp.getPlayer().time();
			}
		}, 2000000);
		// Wait for test result
		pause(new Condition("Disk has finished (positive or negative)") {
			public boolean test() {
				// Disk change requested?
				boolean diskChangeCondition = checkC64Memory(ascii2petscii("PLEASE INSERT DISK "
						+ (diskNum + 1)));
				// Completely Finished?
				boolean finishCondition = checkC64Memory(ascii2petscii("FINISH"));
				// A specific test failed?
				boolean testFailedCondition = checkC64Memory(ascii2petscii("RIGHT"));
				// Maximum expected test time per disk exceeded?
				boolean maxTestTime = sp.getPlayer().time() - startTime >= MAX_TEST_TIME;
				// We wait until one of these conditions is met
				return diskChangeCondition || finishCondition
						|| testFailedCondition || maxTestTime;
			}

		}, Long.MAX_VALUE);

		// Wait a second until the error message has been completely painted
		pause(1000);

		// Test, if an error message appeared
		assertThat(checkC64Memory(ascii2petscii("RIGHT"))).isFalse();
		// Test, if the test exceeds the maximum test time
		assertThat(sp.getPlayer().time() - startTime).isLessThan(MAX_TEST_TIME);
		if (diskNum == 3) {
			// Test of successful completion of all tests
			assertThat(checkC64Memory(ascii2petscii("FINISH"))).isTrue();
		} else {
			// Test, if the disk change is requested, which is expected behavior
			// after a successful completion of one disk
			assertThat(
					checkC64Memory(ascii2petscii("PLEASE INSERT DISK "
							+ (diskNum + 1)))).isTrue();
		}
	}

	/**
	 * Enter a BASIC command.
	 * 
	 * @param text
	 *            command to enter
	 */
	protected void enterC64BasicCmd(final String text) {
		for (int i = 0; i < Math.min(text.length(), 16); i++) {
			ram[0x277 + i] = (byte) text.charAt(i);
		}
		ram[0xc6] = (byte) Math.min(text.length(), 16);
	}

	/**
	 * Check for a message on the screen (at the beginning of a line).
	 * 
	 * @param petscii
	 *            message to check (PETSCII not ASCII!)
	 * @return message detected
	 */
	protected boolean checkC64Memory(final byte[] petscii) {
		for (int screen = 0x400; screen < 0x800; screen += 0x28) {
			if (checkC64Memory(screen, petscii)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check for a message on the screen at a specific location.
	 * 
	 * @param start
	 *            memory location
	 * @param petscii
	 *            message to check (PETSCII not ASCII!)
	 * @return message detected
	 */
	private boolean checkC64Memory(int start, byte[] petscii) {
		for (int i = 0; i < petscii.length; i++) {
			if (ram[start + i] != petscii[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Convert ASCIi to PETSCII characters
	 * 
	 * @param ascii
	 *            ascii characters to convert
	 * @return resulting petscii characters
	 */
	protected static byte[] ascii2petscii(String ascii) {
		byte[] petscii = new byte[ascii.length()];
		for (int i = 0; i < ascii.length(); i++) {
			char charAt = ascii.charAt(i);
			if (Character.isLetter(charAt)) {
				petscii[i] = (byte) ((int) charAt - 'A' + 1);
			} else {
				petscii[i] = (byte) charAt;
			}
		}
		return petscii;
	}

	/**
	 * Get a test resources filename.
	 * 
	 * @param filename
	 *            relative path
	 * @return absolute path name
	 * @throws URISyntaxException
	 *             path name syntax error
	 */
	protected static String getFilename(String filename)
			throws URISyntaxException {
		URL res = WolfgangLorenzTest.class.getResource(filename);
		return new File(res.toURI()).getAbsolutePath().replace('\\', '/');
	}

}
