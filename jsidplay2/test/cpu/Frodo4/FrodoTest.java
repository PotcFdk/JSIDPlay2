package cpu.Frodo4;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.swing.timing.Pause.pause;

import java.awt.Dimension;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JFrame;

import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;

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

@GUITest
@RunWith(GUITestRunner.class)
public class FrodoTest extends FestSwingTestCaseTemplate {
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
	public void testFrodoDADB() throws URISyntaxException {
		window.menuItem("Load").click();
		window.fileChooser("Open").fileNameTextBox()
				.setText(getFilename("dadb.prg"));
		window.fileChooser("Open").approve();
		// Wait until reset has been executed completely
		pause(new Condition("Wait after reset has been executed") {
			public boolean test() {
				return sp.getPlayer().time() >= 3;
			}

		}, Long.MAX_VALUE);
		// Test, that screen color is white
		assertThat(c64.getVIC().getRegisters()[0x20]).isEqualTo((byte) 1);
		// Press and hold space
		c64.getEventScheduler().schedule(
				new Event("Press and hold down Space") {
					@Override
					public void event() throws InterruptedException {
						c64.getKeyboard().keyPressed(KeyTableEntry.SPACE);
					}
				}, 2000000);
		// Wait until screen color turns to black
		pause(new Condition("Screen color becomes black") {
			public boolean test() {
				return c64.getVIC().getRegisters()[0x20] == 0;
			}

		}, 3000);
		// Test, that screen color is now black
		assertThat(c64.getVIC().getRegisters()[0x20]).isEqualTo((byte) 0);
		// Release space
		c64.getEventScheduler().schedule(
				new Event("Press and hold down Space") {
					@Override
					public void event() throws InterruptedException {
						c64.getKeyboard().keyReleased(KeyTableEntry.SPACE);
					}
				}, 1);
		// Wait until screen color turns to black
		pause(new Condition("Screen color becomes white again") {
			public boolean test() {
				return c64.getVIC().getRegisters()[0x20] == 1;
			}

		}, 3000);
		// Test, that screen color is now white
		assertThat(c64.getVIC().getRegisters()[0x20]).isEqualTo((byte) 1);
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
		URL res = FrodoTest.class.getResource(filename);
		return new File(res.toURI()).getAbsolutePath().replace('\\', '/');
	}
}
