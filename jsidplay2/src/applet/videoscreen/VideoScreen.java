package applet.videoscreen;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import libsidplay.Player;
import libsidplay.components.OvImageIcon;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.keyboard.Keyboard;
import libsidplay.components.mos656x.VIC;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import resid_builder.resid.ISIDDefs.ChipModel;
import sidplay.ini.intf.IConfig;
import applet.JSIDPlay2;
import applet.TuneTab;
import applet.events.IInsertMedia;
import applet.events.IUpdateUI;
import applet.events.UIEvent;
import applet.filechooser.ImageFileChooser;
import applet.filefilter.CartFileFilter;
import applet.filefilter.DiskFileFilter;
import applet.filefilter.TapeFileFilter;
import applet.ui.DKnob2;
import applet.ui.JNiceButton;

/**
 * Java AWT to C64 keyboard mapper.
 * 
 * <PRE>
 * Operation is based on hybrid of symbolic and positional mapping.
 * Some unobvious mappings have been chosen:
 * 
 * Esc = STOP, shift+Esc = RUN
 * F11 = NMI
 * § = arrow left
 * | = arrow up
 * ¦ = pi
 * </PRE>
 */
public class VideoScreen extends TuneTab {
	/**
	 * Time to hide the cursor, if the mouse is not moved (hundredth seconds).
	 */
	private static final int TIME_TO_HIDE_CURSOR = 30;

	/**
	 * Old C64 layout.
	 */
	private final ImageIcon c64Image = new ImageIcon(
			JSIDPlay2.class.getResource("icons/c64.png"));
	/**
	 * New C64 layout.
	 */
	private final ImageIcon c64cImage = new ImageIcon(
			JSIDPlay2.class.getResource("icons/c64c.png"));

	private SwingEngine swix;

	protected JSplitPane splitpane;
	protected C64Canvas c64;
	protected JNiceButton virtualKeyboard, datasette, floppy, cartridge;
	protected DKnob2 brightness, contrast, gamma, saturation, phaseShift,
			offset, tint, blur, bleed;

	protected Player player;
	protected IConfig config;

	protected Keyboard keyboard;
	protected int moveCounter, keyLocation;
	protected ComponentInputMap im = new ComponentInputMap(this);
	protected ActionMap am = new ActionMap();

	private String fLastTapeImageName, fLastFloppyImageName,
			fLastCartImageName;

	public Action showVirtualKeyboard = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getC64Canvas().switchKeyboard();
		}
	};
	
	public Action insertTape = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			// Choose tape file
			final ImageFileChooser fileDialog = new ImageFileChooser(config,
					new TapeFileFilter());
			final int rc = fileDialog.showDialog(VideoScreen.this, getSwix()
					.getLocalizer().getString("ATTACH_TAPE"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				final File selectedFile = fileDialog.getSelectedFile();

				getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.TAPE;
					}

					@Override
					public File getSelectedMedia() {
						return selectedFile;
					}

					@Override
					public File getAutostartFile() {
						return fileDialog.getAutostartFile();
					}
					
					@Override
					public Component getComponent() {
						return VideoScreen.this;
					}
				});
			}
		}
	};

	public Action insertDisk = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final ImageFileChooser fileDialog = new ImageFileChooser(config,
					new DiskFileFilter());
			final int rc = fileDialog.showDialog(VideoScreen.this, getSwix()
					.getLocalizer().getString("ATTACH_DISK"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				final File selectedFile = fileDialog.getSelectedFile();
				// attach disk
				getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.DISK;
					}

					@Override
					public File getSelectedMedia() {
						return selectedFile;
					}

					@Override
					public File getAutostartFile() {
						return fileDialog.getAutostartFile();
					}
					
					@Override
					public Component getComponent() {
						return VideoScreen.this;
					}
				});
			}
		}
	};

	public Action insertCartridge = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new ImageFileChooser(config,
					new CartFileFilter());
			final int rc = fileDialog.showDialog(VideoScreen.this, getSwix()
					.getLocalizer().getString("ATTACH_CART"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				final File selectedFile = fileDialog.getSelectedFile();
				getUiEvents().fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.CART;
					}

					@Override
					public File getSelectedMedia() {
						return selectedFile;
					}

					@Override
					public File getAutostartFile() {
						return null;
					}
					
					@Override
					public Component getComponent() {
						return VideoScreen.this;
					}
				});
			}
		}
	};

	// maps key codes from key events to keys of the C64
	private final static Map<Integer, KeyTableEntry> keycodeKeymap = new HashMap<Integer, KeyTableEntry>();

	/* Positionally mapped characters. */
	static {
		/* These keys can be found on every keyboard that exists, pretty much. */

		/* Insert defined for convenience. */
		keycodeKeymap.put(KeyEvent.VK_BACK_SPACE, KeyTableEntry.INS_DEL);
		keycodeKeymap.put(KeyEvent.VK_ESCAPE, KeyTableEntry.RUN_STOP);
		keycodeKeymap.put(KeyEvent.VK_ENTER, KeyTableEntry.RETURN);
		keycodeKeymap.put(KeyEvent.VK_TAB, KeyTableEntry.CTRL);
		keycodeKeymap.put(KeyEvent.VK_HOME, KeyTableEntry.CLEAR_HOME);
		keycodeKeymap.put(KeyEvent.VK_INSERT, shift(KeyTableEntry.INS_DEL));
		keycodeKeymap.put(KeyEvent.VK_SPACE, KeyTableEntry.SPACE);

		/* F2, F4, F6, F8 defined for convenience. */
		keycodeKeymap.put(KeyEvent.VK_F1, KeyTableEntry.F1);
		keycodeKeymap.put(KeyEvent.VK_F2, shift(KeyTableEntry.F1));
		keycodeKeymap.put(KeyEvent.VK_F3, KeyTableEntry.F3);
		keycodeKeymap.put(KeyEvent.VK_F4, shift(KeyTableEntry.F3));
		keycodeKeymap.put(KeyEvent.VK_F5, KeyTableEntry.F5);
		keycodeKeymap.put(KeyEvent.VK_F6, shift(KeyTableEntry.F5));
		keycodeKeymap.put(KeyEvent.VK_F7, KeyTableEntry.F7);
		keycodeKeymap.put(KeyEvent.VK_F8, shift(KeyTableEntry.F7));

		keycodeKeymap.put(KeyEvent.VK_1, KeyTableEntry.ONE);
		keycodeKeymap.put(KeyEvent.VK_2, KeyTableEntry.TWO);
		keycodeKeymap.put(KeyEvent.VK_3, KeyTableEntry.THREE);
		keycodeKeymap.put(KeyEvent.VK_4, KeyTableEntry.FOUR);
		keycodeKeymap.put(KeyEvent.VK_5, KeyTableEntry.FIVE);
		keycodeKeymap.put(KeyEvent.VK_6, KeyTableEntry.SIX);
		keycodeKeymap.put(KeyEvent.VK_7, KeyTableEntry.SEVEN);
		keycodeKeymap.put(KeyEvent.VK_8, KeyTableEntry.EIGHT);
		keycodeKeymap.put(KeyEvent.VK_9, KeyTableEntry.NINE);
		keycodeKeymap.put(KeyEvent.VK_0, KeyTableEntry.ZERO);

		keycodeKeymap.put(KeyEvent.VK_Q, KeyTableEntry.Q);
		keycodeKeymap.put(KeyEvent.VK_W, KeyTableEntry.W);
		keycodeKeymap.put(KeyEvent.VK_E, KeyTableEntry.E);
		keycodeKeymap.put(KeyEvent.VK_R, KeyTableEntry.R);
		keycodeKeymap.put(KeyEvent.VK_T, KeyTableEntry.T);
		keycodeKeymap.put(KeyEvent.VK_Y, KeyTableEntry.Y);
		keycodeKeymap.put(KeyEvent.VK_U, KeyTableEntry.U);
		keycodeKeymap.put(KeyEvent.VK_I, KeyTableEntry.I);
		keycodeKeymap.put(KeyEvent.VK_O, KeyTableEntry.O);
		keycodeKeymap.put(KeyEvent.VK_P, KeyTableEntry.P);

		keycodeKeymap.put(KeyEvent.VK_A, KeyTableEntry.A);
		keycodeKeymap.put(KeyEvent.VK_S, KeyTableEntry.S);
		keycodeKeymap.put(KeyEvent.VK_D, KeyTableEntry.D);
		keycodeKeymap.put(KeyEvent.VK_F, KeyTableEntry.F);
		keycodeKeymap.put(KeyEvent.VK_G, KeyTableEntry.G);
		keycodeKeymap.put(KeyEvent.VK_H, KeyTableEntry.H);
		keycodeKeymap.put(KeyEvent.VK_J, KeyTableEntry.J);
		keycodeKeymap.put(KeyEvent.VK_K, KeyTableEntry.K);
		keycodeKeymap.put(KeyEvent.VK_L, KeyTableEntry.L);

		keycodeKeymap.put(KeyEvent.VK_Z, KeyTableEntry.Z);
		keycodeKeymap.put(KeyEvent.VK_X, KeyTableEntry.X);
		keycodeKeymap.put(KeyEvent.VK_C, KeyTableEntry.C);
		keycodeKeymap.put(KeyEvent.VK_V, KeyTableEntry.V);
		keycodeKeymap.put(KeyEvent.VK_B, KeyTableEntry.B);
		keycodeKeymap.put(KeyEvent.VK_N, KeyTableEntry.N);
		keycodeKeymap.put(KeyEvent.VK_M, KeyTableEntry.M);

		/* Left, Up defined for convenience. */
		keycodeKeymap.put(KeyEvent.VK_LEFT,
				shift(KeyTableEntry.CURSOR_LEFT_RIGHT));
		keycodeKeymap.put(KeyEvent.VK_RIGHT, KeyTableEntry.CURSOR_LEFT_RIGHT);
		keycodeKeymap.put(KeyEvent.VK_UP, shift(KeyTableEntry.CURSOR_UP_DOWN));
		keycodeKeymap.put(KeyEvent.VK_DOWN, KeyTableEntry.CURSOR_UP_DOWN);

		/* Arrows on numpad and < is the left arrow */
		keycodeKeymap.put(KeyEvent.VK_LESS, KeyTableEntry.ARROW_LEFT);
		keycodeKeymap.put(KeyEvent.VK_NUMPAD4, KeyTableEntry.ARROW_LEFT);
		keycodeKeymap.put(KeyEvent.VK_NUMPAD8, KeyTableEntry.ARROW_UP);

		/* HACKS BEGIN! */

		/*
		 * Every key after this point is quite likely to depend on user's
		 * keymap. I have these keys directly available: + - , . ' <, and a
		 * bunch of dead keys, and numpad
		 */
		keycodeKeymap.put(KeyEvent.VK_MINUS, KeyTableEntry.MINUS);
		keycodeKeymap.put(KeyEvent.VK_COMMA, KeyTableEntry.COMMA);
		keycodeKeymap.put(KeyEvent.VK_PERIOD, KeyTableEntry.PERIOD);
		/*
		 * The * is on the ' key, and I absolutely need it to be able to use
		 * this emu worth crap. Sorry...
		 */
		keycodeKeymap.put(KeyEvent.VK_QUOTE, unshift(KeyTableEntry.STAR));

		/* My laptop needs shift plus, how to accomplish this? */
		keycodeKeymap.put(KeyEvent.VK_NUMBER_SIGN, KeyTableEntry.PLUS);
		keycodeKeymap.put(KeyEvent.VK_PLUS, unshift(KeyTableEntry.STAR));

		/*
		 * I don't have these keys, except some of these on numpad, but they
		 * don't appear to work.
		 */
		keycodeKeymap.put(KeyEvent.VK_ASTERISK, unshift(KeyTableEntry.STAR));
		keycodeKeymap.put(KeyEvent.VK_AT, KeyTableEntry.AT);
		keycodeKeymap.put(KeyEvent.VK_COLON, KeyTableEntry.COLON);
		keycodeKeymap.put(KeyEvent.VK_EQUALS, KeyTableEntry.EQUALS);
		keycodeKeymap.put(KeyEvent.VK_SEMICOLON, KeyTableEntry.SEMICOLON);
		keycodeKeymap.put(KeyEvent.VK_SLASH, KeyTableEntry.SLASH);

		/*
		 * And finally bunch of keys I simply can't type at all unless I can do
		 * this
		 */
		keycodeKeymap.put(KeyEvent.VK_PAGE_UP, KeyTableEntry.SEMICOLON);
		keycodeKeymap.put(KeyEvent.VK_PAGE_DOWN, KeyTableEntry.COLON);
		keycodeKeymap.put(KeyEvent.VK_DELETE, KeyTableEntry.EQUALS);
		keycodeKeymap.put(KeyEvent.VK_END, KeyTableEntry.SLASH);

		/* Missing something? F9, F10, F12 are free. */
	}

	private static KeyTableEntry shift(final KeyTableEntry in) {
		return new KeyTableEntry(in, true);
	}

	private static KeyTableEntry unshift(final KeyTableEntry in) {
		return new KeyTableEntry(in, false);
	}

	private void addInputForAllModifiers(final int keycode, final String name) {
		im.put(KeyStroke.getKeyStroke(keycode, 0, false), "p" + name);
		im.put(KeyStroke.getKeyStroke(keycode, InputEvent.SHIFT_DOWN_MASK,
				false), "p" + name);
		im.put(KeyStroke
				.getKeyStroke(keycode, InputEvent.CTRL_DOWN_MASK, false), "p"
				+ name);
		im.put(KeyStroke.getKeyStroke(keycode, InputEvent.SHIFT_DOWN_MASK
				| InputEvent.CTRL_DOWN_MASK, false), "p" + name);

		im.put(KeyStroke.getKeyStroke(keycode, 0, true), "r" + name);
		im.put(KeyStroke
				.getKeyStroke(keycode, InputEvent.SHIFT_DOWN_MASK, true), "r"
				+ name);
		im.put(KeyStroke.getKeyStroke(keycode, InputEvent.CTRL_DOWN_MASK, true),
				"r" + name);
		im.put(KeyStroke.getKeyStroke(keycode, InputEvent.SHIFT_DOWN_MASK
				| InputEvent.CTRL_DOWN_MASK, true), "r" + name);
	}

	public VideoScreen(final Player pl, final IConfig cfg) {
		this.player = pl;
		this.config = cfg;
		this.keyboard = player.getC64().getKeyboard();

		/* RESTORE key */
		addInputForAllModifiers(KeyEvent.VK_F11, "f11");
		am.put("pf11", new AbstractAction() {
			public void actionPerformed(final ActionEvent arg0) {
				keyboard.restore();
			}
		});

		/* Handle modifiers */
		addInputForAllModifiers(KeyEvent.VK_CONTROL, "ctrl");
		am.put("pctrl", new AbstractAction() {
			public void actionPerformed(final ActionEvent ae) {
				keyboard.cbm(true);
			}
		});
		am.put("rctrl", new AbstractAction() {
			public void actionPerformed(final ActionEvent ae) {
				keyboard.cbm(false);
			}
		});

		addInputForAllModifiers(KeyEvent.VK_SHIFT, "shift");
		am.put("pshift", new AbstractAction() {
			public void actionPerformed(final ActionEvent ae) {
				if (keyLocation == KeyEvent.KEY_LOCATION_LEFT) {
					keyboard.leftShift(true);
				} else {
					keyboard.rightShift(true);
				}
			}
		});
		am.put("rshift", new AbstractAction() {
			public void actionPerformed(final ActionEvent ae) {
				if (keyLocation == KeyEvent.KEY_LOCATION_LEFT) {
					keyboard.leftShift(false);
				} else {
					keyboard.rightShift(false);
				}
			}
		});

		for (int ke : keycodeKeymap.keySet()) {
			final KeyTableEntry kte = keycodeKeymap.get(ke);
			addInputForAllModifiers(ke, "c" + ke);
			am.put("pc" + ke, new AbstractAction() {
				public void actionPerformed(final ActionEvent ae) {
					keyboard.keyPressed(kte);
				}
			});
			am.put("rc" + ke, new AbstractAction() {
				public void actionPerformed(final ActionEvent ae) {
					keyboard.keyReleased(kte);
				}
			});
		}

		setInputMap(WHEN_IN_FOCUSED_WINDOW, im);
		setActionMap(am);
		createContents();
	}

	@Override
	protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e,
			final int condition, final boolean pressed) {
		// gather information of for example the SHIFT key (left/right)?
		keyLocation = e.getKeyLocation();
		return super.processKeyBinding(ks, e, condition, pressed);
	}

	private void createContents() {
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("dknob", DKnob2.class);
			swix.getTaglib().registerTag("c64canvas", C64Canvas.class);
			swix.getTaglib().registerTag("nicebutton", JNiceButton.class);
			swix.insert(VideoScreen.class.getResource("VideoScreen.xml"), this);

			fillComboBoxes();
			setDefaultsAndActions();

			// Use a fixed divider location, always!
			splitpane.addPropertyChangeListener(new PropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt
							.getPropertyName())) {
						splitpane.setDividerLocation(splitpane
								.getMaximumDividerLocation());
					}
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		c64.addMouseMotionListener(new MouseMotionAdapter() {

			public void mouseMoved(MouseEvent e) {
				// Cursor gets moved? Hidden mouse cursor re-appears
				moveCounter = 0;
					RootPaneContainer root = (RootPaneContainer) getTopLevelAncestor();
					root.getGlassPane().setVisible(false);
				}

		});
		{
			final VIC vic = player.getC64().getVIC();
			brightness.setValue(vic.getPalette().getBrightness());
			contrast.setValue(vic.getPalette().getContrast());
			gamma.setValue(vic.getPalette().getGamma());
			saturation.setValue(vic.getPalette().getSaturation());
			offset.setValue(vic.getPalette().getOffset());
			phaseShift.setValue(vic.getPalette().getPhaseShift());
			tint.setValue(vic.getPalette().getTint());
			blur.setValue(vic.getPalette().getLuminanceC());
			bleed.setValue(vic.getPalette().getDotCreep());
		}

		for (final DKnob2 knob : new DKnob2[] { brightness, contrast, gamma,
				saturation, offset, phaseShift, tint, blur, bleed }) {
			knob.addChangeListener(new ChangeListener() {
				public void stateChanged(final ChangeEvent e) {
					final VIC vic = player.getC64().getVIC();
					vic.getPalette().setBrightness(brightness.getValue());
					vic.getPalette().setContrast(contrast.getValue());
					vic.getPalette().setGamma(gamma.getValue());
					vic.getPalette().setSaturation(saturation.getValue());
					vic.getPalette().setOffset(offset.getValue());
					vic.getPalette().setPhaseShift(phaseShift.getValue());
					vic.getPalette().setTint(tint.getValue());
					vic.getPalette().setLuminanceC(blur.getValue());
					vic.getPalette().setDotCreep(bleed.getValue());
					vic.updatePalette();
				}
			});

		}
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	@Override
	public void setTune(final Player m_engine, final SidTune m_tune) {
		c64.setupVideoScreen(player.getC64());

		// Overlay for the C64 screen, if the internal player is used.
		if (player.getTune() != null && player.getC64().getSID(0) != null
				&& player.getTune().getInfo().playAddr != 0) {
			if (player.getC64().getSID(0).getChipModel() == ChipModel.MOS6581) {
				// Old SID chip model? Old C64 screen
				c64.getScreenCanvas().setC64Image(c64Image);
			} else {
				// New SID chip model? New C64 screen
				c64.getScreenCanvas().setC64Image(c64cImage);
			}
		} else {
			// Normal VIC screen
			c64.getScreenCanvas().setC64Image(null);
		}
		repaint();
	}

	public void notify(final UIEvent evt) {
		if (evt.isOfType(IUpdateUI.class)) {
			if (++moveCounter > TIME_TO_HIDE_CURSOR) {
				moveCounter = 0;
				// Cursor not moved for some time? Hide cursor
				final Point p = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(p, c64);
				if (c64.contains(p) && isVisible() && hasFocus()) {
					RootPaneContainer root = (RootPaneContainer) getTopLevelAncestor();
					root.getGlassPane().setCursor(INVISIBLE_CURSOR);
					root.getGlassPane().setVisible(true);
				}
			}
			// Get status information of the first disk drive
			final C1541 c1541 = player.getFloppies()[0];
			final Datasette c1530 = player.getDatasette();

			OvImageIcon datasetteIcon = c1530.getIcon();
			if (datasette.getIcon() != datasetteIcon) {
				datasette.setIcon(datasetteIcon);
			}
			String tapeImageName = datasetteIcon.getImageName();
			if (fLastTapeImageName != tapeImageName) {
				datasette.repaint();
				fLastTapeImageName = tapeImageName;
			}
			OvImageIcon floppyIcon = c1541.getIcon();
			String floppyImageName = floppyIcon.getImageName();
			if (floppy.getIcon() != floppyIcon) {
				floppy.setIcon(c1541.getIcon());
			}
			if (fLastFloppyImageName != floppyImageName) {
				floppy.repaint();
				fLastFloppyImageName = floppyImageName;
			}
			OvImageIcon cartIcon = player.getC64().getIcon();
			String cartImageName = cartIcon.getImageName();
			if (!player.getC64().getIcon().equals(cartridge.getIcon())) {
				cartridge.setIcon(player.getC64().getIcon());
			}
			if (!cartImageName.equals(fLastCartImageName)) {
				cartridge.repaint();
				fLastCartImageName = cartImageName;
			}
		}
	}

	/**
	 * Create a new blank cursor image. Transparent 16 x 16 pixel cursor image.
	 */
	private BufferedImage invisibleCursorImg = new BufferedImage(16, 16,
			BufferedImage.TYPE_INT_ARGB);
	/**
	 * Create a new blank cursor.
	 */
	private Cursor INVISIBLE_CURSOR = Toolkit.getDefaultToolkit()
			.createCustomCursor(invisibleCursorImg, new Point(0, 0),
					"blank cursor");

	public SwingEngine getSwix() {
		return swix;
	}

	public C64Canvas getC64Canvas() {
		return c64;
	}
}
