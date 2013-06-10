package sid;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.timing.Pause.pause;

import java.awt.Dimension;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JFrame;

import libsidplay.C64;

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

import applet.JSIDPlay2Main;

@GUITest
@RunWith(GUITestRunner.class)
public class SidTest extends FestSwingTestCaseTemplate {
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
					frame = (JFrame) new SwingEngine(sp)
							.render(JSIDPlay2.class
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

	@Test
	public void testSID6581() throws URISyntaxException {
		// Open emulation settings and choose 6581
		window.menuItem("EmulationSettings").click();
		window.dialog("EmulationDialogue").comboBox("SID1Model").selectItem(1);
		window.dialog("EmulationDialogue").close();

		window.menuItem("Load").click();
		window.fileChooser("Open").fileNameTextBox()
				.setText(getFilename("sid_detection.prg"));
		window.fileChooser("Open").approve();

		startTime = sp.getPlayer().time();
		// Wait until reset has been executed completely
		pause(new Condition("Wait after reset has been executed") {
			public boolean test() {
				return sp.getPlayer().time() - startTime > 3;
			}

		}, Long.MAX_VALUE);
		// Test, that first char on screen is space
		assertThat(ram[0x0400]).isEqualTo((byte) 32);
		// Start SID detection
		enterC64BasicCmd("SYS49152\r");
		// Wait until reset has been executed completely
		pause(new Condition("Wait until SID has been detected") {
			public boolean test() {
				return ram[0x0400] != 32;
			}

		}, Long.MAX_VALUE);
		// 6581 has been detected
		assertThat(ram[0x0400]).isEqualTo((byte) 01);

		// Reset settings
		window.menuItem("EmulationSettings").click();
		window.dialog("EmulationDialogue").comboBox("SID1Model").selectItem(0);
		window.dialog("EmulationDialogue").close();
	}

	@Test
	public void testSID8580() throws URISyntaxException {
		// Open emulation settings and choose 8580
		window.menuItem("EmulationSettings").click();
		window.dialog("EmulationDialogue").comboBox("SID1Model").selectItem(2);
		window.dialog("EmulationDialogue").close();

		window.menuItem("Load").click();
		window.fileChooser("Open").fileNameTextBox()
				.setText(getFilename("sid_detection.prg"));
		window.fileChooser("Open").approve();

		startTime = sp.getPlayer().time();
		// Wait until reset has been executed completely
		pause(new Condition("Wait after reset has been executed") {
			public boolean test() {
				return sp.getPlayer().time() - startTime > 3;
			}

		}, Long.MAX_VALUE);
		// Test, that first char on screen is space
		assertThat(ram[0x0400]).isEqualTo((byte) 32);
		// Start SID detection
		enterC64BasicCmd("SYS49152\r");
		// Wait until reset has been executed completely
		pause(new Condition("Wait until SID has been detected") {
			public boolean test() {
				return ram[0x0400] != 32;
			}

		}, Long.MAX_VALUE);
		// 8580 has been detected
		assertThat(ram[0x0400]).isEqualTo((byte) 02);

		// Reset settings
		window.menuItem("EmulationSettings").click();
		window.dialog("EmulationDialogue").comboBox("SID1Model").selectItem(0);
		window.dialog("EmulationDialogue").close();
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
		URL res = SidTest.class.getResource(filename);
		return new File(res.toURI()).getAbsolutePath().replace('\\', '/');
	}
}
