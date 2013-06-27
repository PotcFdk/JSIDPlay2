/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package libsidplay.components.mos656x;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/* TODO
 * 
 * - vborder2.prg proves that whether graphics sequencer is on or off for a line is decided not from the
 *   vborder flag but something else. Perhaps there is a graphics sequencer master toggle, which is set
 *   at the start of line. At any rate, changing the vborder at middle of line must not stop graphics
 *   sequencer. (The on/off is distinct from isDisplayActive, which is graphics sequencer enabled, merely
 *   without badlines and thus reading idle data.)
 * 
 * - sprite damaged fetch region. Placement given for sprite 0, but other sprites
 *   all follow $10 increments. Properties:
 *   - sprite pixel at $162 repeats to $169 then is cut.
 *   - no sprite is displayed if sprite is positioned between $16a .. $16e
 *   - sprite works after $16f again.
 * 
 * - sprite pixel-exact display enable. Sprites show the idle data after $164
 *   position for sprite 1+ because DMA fetch hasn't happened yet. For sprite 0,
 *   the display goes off at $163, and DMA will have happened by $16f and thus
 *   it will never show any idle data.
 */

/**
 * Implements the functionality of the C64's VIC II chip
 * 
 * For a good German documentation on the MOS 6567/6569 Videocontroller
 * (VIC-II), see <a href=
 * 'http://www.minet.uni-jena.de/~andreasg/c64/vic_artikel/vic_artikel_1.htm'>http://www.minet.uni-jena.de/~andreasg/c64/vic_artikel/vic_artikel_1.htm</
 * a > , <a href=
 * 'http://cbmmuseum.kuto.de/zusatz_6569_vic2.html'>http://cbmmuseum.kuto.de/zusatz_6569_vic2.html</
 * a > or <a href=
 * 'http://unusedino.de/ec64/technical/misc/vic656x/vic656x-german.html'>http://unusedino.de/ec64/technical/misc/vic656x/vic656x-german.html</a
 * > . An English version of the documentation you can find at <a href=
 * 'http://www.unusedino.de/ec64/technical/misc/vic656x/vic656x.html'>http://www.unusedino.de/ec64/technical/misc/vic656x/vic656x.html</
 * a > .<br>
 * 
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 * @author Antti S. Lankila (alankila@bel.fi)
 */
public abstract class VIC extends Bank {
	/**
	 * Alpha channel of ARGBA pixel data.
	 */
	private static final int ALPHA = 0xff000000;

	/**
	 * Chip models supported by MOS656X.
	 */
	public static enum Model {
		// MOS6567R56A, /* Old NTSC */
		MOS6567R8, /* NTSC */
		MOS6569R1, /* PAL */
		MOS6569R3, /* PAL */
		MOS6569R4, /* PAL */
		MOS6569R5, /* PAL */
	}

	public static final String PROP_PIXELS = "pixels";

	/** First line when we check for bad lines */
	protected static final int FIRST_DMA_LINE = 0x30;

	/** Last line when we check for bad lines */
	protected static final int LAST_DMA_LINE = 0xf7;

	private static final byte COL_D021 = 0;
	private static final byte COL_D022 = 1;
	private static final byte COL_D023 = 2;
	// private static final byte COL_D024 = 3;
	private static final byte COL_CBUF = 4;
	private static final byte COL_CBUF_MC = 5;
	private static final byte COL_VBUF_L = 6;
	private static final byte COL_VBUF_H = 7;
	private static final byte COL_ECM = 8;
	private static final byte COL_NONE = 9;

	private static final byte[] videoModeColorDecoder = { COL_D021, COL_D021,
			COL_CBUF, COL_CBUF, /* ECM=0 BMM=0 MCM=0 */
			COL_D021, COL_D022, COL_D023, COL_CBUF_MC, /* ECM=0 BMM=0 MCM=1 */
			COL_VBUF_L, COL_VBUF_L, COL_VBUF_H, COL_VBUF_H, /* ECM=0 BMM=1 MCM=0 */
			COL_D021, COL_VBUF_H, COL_VBUF_L, COL_CBUF, /* ECM=0 BMM=1 MCM=1 */
			COL_ECM, COL_ECM, COL_CBUF, COL_CBUF, /* ECM=1 BMM=0 MCM=0 */
			COL_NONE, COL_NONE, COL_NONE, COL_NONE, /* ECM=1 BMM=0 MCM=1 */
			COL_NONE, COL_NONE, COL_NONE, COL_NONE, /* ECM=1 BMM=1 MCM=0 */
			COL_NONE, COL_NONE, COL_NONE, COL_NONE /* ECM=1 BMM=1 MCM=1 */
	};

	/* Current video mode */
	private int videoModeColorDecoderOffset;

	/** Current dynamic colors */
	private final byte[] videoModeColors = new byte[10];
	/** Current border color */
	private int borderColor;

	/** contains the memory fetch data */
	protected byte phi1Data;
	/** Pixel color being rendered. */
	private int pixelColor;
	/** multicolor flip-flop state */
	private boolean mcFlip;
	/** xscroll-delayed phi1 data */
	private int phi1DataPipe;

	protected int[] combinedLinesCurrent;
	/** Table for looking up color using a packed 2x8 value for even rasterlines */
	protected final int[] combinedLinesEven = new int[256 * 256];
	/** Table for looking up color using a packed 2x8 value for odd rasterlines */
	protected final int[] combinedLinesOdd = new int[256 * 256];
	/** Prevailing VIC color palette for current line (odd/even) */
	protected byte[] linePaletteCurrent;
	/** VIC color palette for even rasterlines */
	protected final byte[] linePaletteEven = new byte[16 * 16 * 16 * 16];
	/** VIC color palette for odd rasterlines */
	protected final byte[] linePaletteOdd = new byte[16 * 16 * 16 * 16];
	/** Last line's color */
	protected final byte[] previousLineDecodedColor = new byte[65 * 8];
	/** Index into last line */
	protected int previousLineIndex;

	/** PLA chip */
	private final PLA pla;

	/** Property change support */
	protected final PropertyChangeSupport support;

	/** Credits */
	private static final String credit = "MOS656X (cycle-exact VICII) Emulation:\n"
			+ "\tCopyright (C) 2009 Joerg Jahnke <joergjahnke@users.sourceforge.net>\n"
			+ "\tCopyright (C) 2010 Antti S. Lankila <alankila@bel.fi>\n";

	// graphic modes
	/**
	 * raster IRQ flag
	 */
	private final static byte IRQ_RASTER = 1;
	/**
	 * sprite-background collision IRQ flag
	 */
	protected final static byte IRQ_SPRITE_BACKGROUND_COLLISION = 2;
	/**
	 * sprite-sprite collision IRQ flag
	 */
	protected final static byte IRQ_SPRITE_SPRITE_COLLISION = 4;
	/**
	 * Light-Pen IRQ flag
	 */
	private final static byte MOS656X_INTERRUPT_LP = 1 << 3;

	/** memory for chip registers */
	protected final byte[] registers = new byte[0x40];
	/** video counter, a 10 bit counter */
	protected int vc;
	/** video counter base, a 10 bit data register */
	protected int vcBase;
	/** row counter, a 3 bit counter */
	protected int rc;
	/** is the display active or idle? */
	protected boolean isDisplayActive;
	/** are bad lines enabled for this frame? */
	protected boolean areBadLinesEnabled;
	/** current raster line */
	protected int rasterY;
	/** Is rasterYIRQ condition true? */
	protected boolean rasterYIRQCondition;
	/** the 8 sprites */
	protected final Sprite[] sprites = new Sprite[8];
	/**
	 * The head of sprite linked list. The top is never rendered; it's just an
	 * anchor.
	 */
	protected final Sprite spriteLinkedListHead = new Sprite(null, -1);
	/** contains the color data of the current character */
	private final byte colorData[] = new byte[40];
	/** contains the video matrix data for the current character */
	protected final byte videoMatrixData[] = new byte[40];
	/** vertical border flip-flop */
	protected boolean showBorderVertical;
	/** main border flip-flop */
	private boolean showBorderMain;
	/** is the current line a bad line */
	protected boolean isBadLine;
	/** video matrix base address */
	protected int videoMatrixBase;
	/** character memory base address */
	protected int charMemBase;
	/** bitmap memory base address */
	protected int bitmapMemBase;
	/** vertical scrolling value */
	private int yscroll;
	/** horizontal scrolling value */
	protected byte xscroll;
	/** xscroll value within window region text columns 1 .. 39 */
	protected int latchedXscroll;
	/** internal IRQ flags */
	private byte irqFlags;
	/** masks for the IRQ flags */
	private byte irqMask;
	/**
	 * Output ARGB screen buffer as int32 array. MSB to LSB -> alpha, red,
	 * green, blue
	 */
	protected final int[] pixels = new int[48 * 312 * 8];
	/** Index of next pixel to paint */
	protected int nextPixel;
	/** Current visible line */
	protected int lineCycle;
	/** Is display rendering enabled? */
	protected boolean graphicsRendering;
	/** CPU's event context. */
	protected final EventScheduler context;
	/** Light pen coordinates */
	private byte lpx, lpy;
	/** Has light pen IRQ been triggered in this frame already? */
	protected boolean lpTriggered;
	/** Set when new frame starts. */
	protected boolean startOfFrame;
	/** Number of cycles per line. */
	private final int CYCLES_PER_LINE;
	/** Is CIA asserting lightpen? */
	private boolean lpAsserted;

	private byte latchedColor;

	private byte latchedVmd;

	/**
	 * Creates a new instance of VIC6569
	 * 
	 * @param pla
	 * @param context
	 * @param cpl
	 */
	public VIC(PLA pla, final EventScheduler context, int cpl) {
		this.pla = pla;
		this.context = context;

		CYCLES_PER_LINE = cpl;

		support = new PropertyChangeSupport(this);

		for (int i = 0; i < sprites.length; i++) {
			sprites[i] = new Sprite(spriteLinkedListHead, i);
		}
	}

	/**
	 * Get screen as RGB data
	 * 
	 * @return C64 screen pixels as RGB data
	 */
	public final int[] getPixels() {
		return pixels;
	}

	/**
	 * Read the x-coordinate of a sprite
	 * 
	 * @param spriteNo
	 *            no of the sprite (0-7)
	 * @return x-coordinate
	 */
	private int readSpriteXCoordinate(final int spriteNo) {
		return (registers[0x00 + (spriteNo << 1)] & 0xff)
				+ ((registers[0x10] & 1 << spriteNo) != 0 ? 256 : 0);
	}

	/**
	 * Read the RSEL flag which determines whether we display 24 or 25 lines of
	 * text
	 * 
	 * @return true if RSEL is set and we use 25 lines, otherwise false
	 */
	protected boolean readRSEL() {
		return (registers[0x11] & 8) != 0;
	}

	/**
	 * Read the CSEL flag which determines whether we display 38 or 40 columns
	 * of text
	 * 
	 * @return true if CSEL is set and we use 40 columns, otherwise false
	 */
	private boolean readCSEL() {
		return (registers[0x16] & 8) != 0;
	}

	/**
	 * Read the DEN flag which tells whether the display is enabled
	 * 
	 * @return true if DEN is set, otherwise false
	 */
	protected boolean readDEN() {
		return (registers[0x11] & 0x10) != 0;
	}

	/**
	 * Read the value of the raster line IRQ
	 * 
	 * @return raster line when to trigger an IRQ
	 */
	protected int readRasterLineIRQ() {
		return (registers[0x12] & 0xff) + ((registers[0x11] & 0x80) << 1);
	}

	protected boolean evaluateIsBadLine() {
		return areBadLinesEnabled && rasterY >= FIRST_DMA_LINE
				&& rasterY <= LAST_DMA_LINE && (rasterY & 7) == yscroll;
	}

	/** Signal CPU interrupt if requested by VIC. */
	private void handleIrqState() {
		/* signal an IRQ unless we already signaled it */
		if ((irqFlags & irqMask & 0x0f) != 0) {
			if ((irqFlags & 0x80) == 0) {
				interrupt(true);
				irqFlags |= 0x80;
			}
		} else if ((irqFlags & 0x80) != 0) {
			interrupt(false);
			irqFlags &= 0x7f;
		}
	}

	/**
	 * Get the video memory base address, which is determined by the inverted
	 * bits 0-1 of port A on CIA 2, plus the video matrix base address plus the
	 * character data base address plus the bitmap memory base address.
	 */
	private void determineVideoMemoryBaseAddresses() {
		videoMatrixBase = (registers[0x18] & 0xf0) << 6;
		charMemBase = (registers[0x18] & 0x0e) << 10;
		bitmapMemBase = (registers[0x18] & 0x08) << 10;
	}

	/**
	 * Set an IRQ flag and trigger an IRQ if the corresponding IRQ mask is set.
	 * The IRQ only gets activated, i.e. flag 0x80 gets set, if it was not
	 * active before.
	 */
	protected void activateIRQFlag(final byte flag) {
		irqFlags |= flag;
		handleIrqState();
	}

	/**
	 * Read video matrix and color data for the next character
	 */
	protected void doVideoMatrixAccess() {
		final int displayCycle = lineCycle - 24;
		videoMatrixData[displayCycle] = vicReadMemoryPHI2(videoMatrixBase | vc);
		colorData[displayCycle] = vicReadColorMemoryPHI2(vc);
	}

	/*
	 * This table can be used to quadruple every bit, for instance %1011 ->
	 * %1111000011111111. Additionally, the inverse of input is stored on the
	 * high pits, making the complete value %00001111000000001111000011111111.
	 */
	protected static final int[] singleColorLUT = new int[16];
	static {
		for (int in = 0; in < 16; in++) {
			int out = 0;
			for (int b = 0; b < 4; b++) {
				if ((in & (1 << b)) != 0) {
					out |= 0xf << (b << 2);
				}
			}
			singleColorLUT[in] = out | (0xffff ^ out) << 16;
		}
	}

	/** Previous sequencer data */
	protected int oldGraphicsData;

	/**
	 * This monster method calculates:
	 * <ul>
	 * <li>graphics sequencer output,
	 * <li>sprite sequencer output,
	 * <li>border sequencer output
	 * </ul>
	 * and combines all of them together.
	 */
	protected void drawSpritesAndGraphics() {
		/** Current graphics fetch cycle number */
		int renderCycle = lineCycle - 26;
		if (renderCycle < 0) {
			renderCycle += CYCLES_PER_LINE;
		}

		/** Graphics sequencer foreground region. 0xf at slot when occupied. */
		int priorityData = 0;
		/** Graphics pixel data. */
		int graphicsDataBuffer = 0;

		/* Render 32 bits 16 bits (= 4 pixels) at a time. */
		for (int pixel = 0; pixel < 32;) {
			/* At midpoint -> set video mode bits. */
			if (pixel == 16) {
				videoModeColorDecoderOffset |= ((registers[0x11] & 0x60 | registers[0x16] & 0x10) >> 2);
			}

			/* XScroll matched -> load new data. */
			if (pixel == latchedXscroll) {
				videoModeColors[COL_CBUF] = latchedColor;
				videoModeColors[COL_CBUF_MC] = (byte) (latchedColor & 0x7);
				videoModeColors[COL_VBUF_L] = (byte) (latchedVmd & 0xf);
				videoModeColors[COL_VBUF_H] = (byte) (latchedVmd >> 4 & 0xf);
				videoModeColors[COL_ECM] = videoModeColors[COL_D021
						+ ((latchedVmd >> 6) & 0x3)];

				mcFlip = true;
				if (renderCycle < 40 && !showBorderVertical) {
					latchedVmd = isDisplayActive ? videoMatrixData[renderCycle]
							: 0;
					latchedColor = isDisplayActive ? colorData[renderCycle] : 0;
					phi1DataPipe ^= (phi1DataPipe ^ phi1Data << 16) & 0xff0000;
				}
			}

			/*
			 * Calculate size of renderable chunk: either until next 16 bits, or
			 * to next xscroll.
			 */
			int end = pixel + 16 & 0xf0;
			if (pixel < latchedXscroll) {
				end = Math.min(end, latchedXscroll);
			}

			/*
			 * This monster asks a question: are we going to render multicolor
			 * pixels now?
			 */
			if (((videoModeColorDecoderOffset & 4) != 0 && !(videoModeColorDecoderOffset == 4 && videoModeColors[COL_CBUF] < 8))) {

				/*
				 * It would be great if the below expressoin could be SIMDified,
				 * but that has proven to be very difficult.
				 */
				while (pixel < end) {
					/* Read next pixel (maybe) */
					if (mcFlip) {
						pixelColor = phi1DataPipe >>> 30;
					}
					phi1DataPipe <<= 1;
					mcFlip = !mcFlip;

					graphicsDataBuffer <<= 4;
					priorityData <<= 4;

					/*
					 * Convert to one of the 9 possible values VIC can output at
					 * a time
					 */
					int color = videoModeColorDecoder[videoModeColorDecoderOffset
							| pixelColor];
					/* Convert to final color index in the vic-ii palette. */
					graphicsDataBuffer |= videoModeColors[color];
					/* Separate data channel for sprite priority handling */
					priorityData |= pixelColor > 1 ? 0xf : 0;

					pixel += 4;
				}
			} else {
				int bits = end - pixel;
				/* Number of f's equals the number of pixels. */
				int mask = (-1 >>> -bits);

				/* Extract bits of input */
				int inputBits = phi1DataPipe >>> (-bits >> 2);
				phi1DataPipe <<= bits >> 2;
				mcFlip ^= (bits & 4) != 0;

				int videoModeColorsSIMD = videoModeColors[videoModeColorDecoder[videoModeColorDecoderOffset]] << 16
						| videoModeColors[videoModeColorDecoder[videoModeColorDecoderOffset | 3]];
				videoModeColorsSIMD |= videoModeColorsSIMD << 4;
				videoModeColorsSIMD |= videoModeColorsSIMD << 8;

				/* Get decoded value of 0xABCDabcd representing 4-bit input. */
				int priorityBits = singleColorLUT[inputBits];
				/* Generate BG and FG simultaneously. */
				int out = videoModeColorsSIMD & priorityBits;
				/* Merge */
				out |= out >>> 16;
				/* Place merged data where it is wanted. */
				graphicsDataBuffer <<= bits;
				graphicsDataBuffer |= out & mask;
				/* Separate data channel for sprite priority handling */
				priorityData <<= bits;
				priorityData |= priorityBits & mask;

				pixel = end;
			}
		}

		/*
		 * This should happen on the 6th, 7th or such pixel. It's apparently
		 * related to the fall time in NMOS, and can be observed to change with
		 * system temperature.
		 */
		videoModeColorDecoderOffset &= ((registers[0x11] & 0x60 | registers[0x16] & 0x10) >> 2);

		/* Sprite sequencer */
		int opaqueSpritePixels = 0;
		Sprite prev = spriteLinkedListHead;
		Sprite current = prev.nextVisibleSprite;
		while (current != null) {
			int spriteForegroundMask = current.calculateNext8Pixels();

			/* Handle sprite-bg collision */
			if ((spriteForegroundMask & priorityData) != 0) {
				if (registers[0x1f] == 0) {
					activateIRQFlag(IRQ_SPRITE_BACKGROUND_COLLISION);
				}
				registers[0x1f] |= 1 << current.index;
			}

			/*
			 * Handle sprite-sprite collision. It's easy to detect that a
			 * collision occurred, but hard to find which two sprites collided.
			 * For this purpose, we store the sprite index which contributed a
			 * pixel into the low bits of a pixel slot, and keep the 4th bit as
			 * a mask that we can use to reliably identify the collision.
			 */
			if ((opaqueSpritePixels & spriteForegroundMask) != 0) {
				if (registers[0x1e] == 0) {
					activateIRQFlag(IRQ_SPRITE_SPRITE_COLLISION);
				}
				registers[0x1e] |= 1 << current.index;

				for (int pixel = 0; pixel < 32; pixel += 4) {
					/* non-transparent from us? */
					if ((spriteForegroundMask >> pixel & 0xf) == 0) {
						continue;
					}
					/* non-transparent from other? */
					int otherSprite = opaqueSpritePixels >> pixel & 0xf;
					if (otherSprite == 0) {
						/* no pixel set at that slot? Set ourselves */
						opaqueSpritePixels |= (current.index | 8) << pixel;
					} else {
						/*
						 * Collision; register the other sprite as colliding,
						 * but keep the old value, as it doesn't matter whether
						 * it contains the other or ourselves: both collision
						 * bits are set on 0x1e once we finish.
						 */
						registers[0x1e] |= 1 << (otherSprite & 0x7);
					}
				}
			} else {
				opaqueSpritePixels |= current.indexBits & spriteForegroundMask;
			}

			final int priorityMask = current.getNextPriorityMask();
			spriteForegroundMask &= ~priorityData | priorityMask;
			graphicsDataBuffer ^= (current.colorBuffer ^ graphicsDataBuffer)
					& spriteForegroundMask;

			/* exhausted this sprite's data? */
			if (current.consuming) {
				prev = current;
			} else {
				prev.nextVisibleSprite = current.nextVisibleSprite;
			}

			current = current.nextVisibleSprite;
		}

		/* Border unit */
		if ((renderCycle == 1 || renderCycle == 39) && !readCSEL()) {
			if (showBorderMain) {
				graphicsDataBuffer ^= (graphicsDataBuffer ^ borderColor) & 0xfffffff0;
			}
			showBorderMain = showBorderVertical || renderCycle == 39;
			if (showBorderMain) {
				graphicsDataBuffer ^= (graphicsDataBuffer ^ borderColor) & 0x0000000f;
			}
		} else {
			if (showBorderMain) {
				graphicsDataBuffer = borderColor;
			}
			if ((renderCycle == 0 || renderCycle == 40) && readCSEL()) {
				showBorderMain = showBorderVertical || renderCycle == 40;
			}
		}

		/* Pixels arrive in 0x12345678 order. */
		for (int j = 0; j < 2; j++) {
			oldGraphicsData |= graphicsDataBuffer >>> 16;
			for (int i = 0; i < 4; i++) {
				oldGraphicsData <<= 4;
				final byte lineColor = linePaletteCurrent[oldGraphicsData >>> 16];
				final byte previousLineColor = previousLineDecodedColor[previousLineIndex];
				pixels[nextPixel++] = ALPHA
						| combinedLinesCurrent[lineColor & 0xff
								| previousLineColor << 8 & 0xff00];
				previousLineDecodedColor[previousLineIndex++] = lineColor;
			}
			graphicsDataBuffer <<= 16;
		}
	}

	/**
	 * This version just detects sprite-sprite collisions. It is appropriate to
	 * use outside renderable screen, where graphics sequencer is known to have
	 * quiesced.
	 */
	protected final void spriteCollisionsOnly() {
		int opaqueSpritePixels = 0;

		Sprite prev = spriteLinkedListHead;
		Sprite current = prev.nextVisibleSprite;

		while (current != null) {
			int spriteForegroundMask = current.calculateNext8Pixels();
			if ((opaqueSpritePixels & spriteForegroundMask) != 0) {
				if (registers[0x1e] == 0) {
					activateIRQFlag(IRQ_SPRITE_SPRITE_COLLISION);
				}
				registers[0x1e] |= 1 << current.index;

				for (int pixel = 0; pixel < 32; pixel += 4) {
					if ((spriteForegroundMask >> pixel & 1) == 0) {
						continue;
					}
					int otherSprite = opaqueSpritePixels >> pixel & 0xf;
					if (otherSprite == 0) {
						opaqueSpritePixels |= (current.index | 8) << pixel;
					} else {
						registers[0x1e] |= 1 << (otherSprite & 0x7);
					}
				}
			} else {
				opaqueSpritePixels |= current.indexBits & spriteForegroundMask;
			}

			if (current.consuming) {
				prev = current;
			} else {
				prev.nextVisibleSprite = current.nextVisibleSprite;
			}

			current = current.nextVisibleSprite;
		}
	}

	/**
	 * In certain cases, CPU sees the stale bus data from VIC. VIC reads on
	 * every cycle, and this describes what it reads.
	 */
	public final byte getLastReadByte() {
		return phi1Data;
	}

	/**
	 * Get memory address of sprite data.
	 * 
	 * @param n
	 *            sprite number
	 */
	protected void fetchSpritePointer(final int n) {
		Sprite sprite = sprites[n];
		sprite.setPointerByte(phi1Data);
		final int x = sprites[n].getX();
		if (x >= 0x160 + 0x10 * n && x <= 0x167 + 0x10 * n) {
			sprite.event();
		}
		sprite.repeatPixels();

		sprite.setSpriteByte(2,
				vicReadMemoryPHI2(sprites[n].getCurrentByteAddress()));
	}

	/**
	 * Fetch 1 byte of memory starting from the sprite address.
	 * 
	 * @param n
	 *            sprite number
	 */
	protected void fetchSpriteData(final int n) {
		Sprite sprite = sprites[n];
		sprite.setSpriteByte(1, phi1Data);
		sprite.setSpriteByte(0,
				vicReadMemoryPHI2(sprite.getCurrentByteAddress()));

		final int x = sprites[n].getX();
		if (x == 0x16f + 0x10 * n) {
			sprites[n].event();
		} else {
			handleSpriteVisibilityEvent(sprites[n]);
		}
	}

	/**
	 * Read VIC register.
	 * 
	 * @param register
	 *            Register to read.
	 */
	@Override
	public final byte read(int register) {
		register &= 0x3f;

		final byte value;
		switch (register) {
		// control register 1
		case 0x11: {
			value = (byte) ((registers[register] & 0x7f) | (rasterY & 0x100) >> 1);
			break;
		}

		// Raster Counter
		case 0x12:
			value = (byte) rasterY;
			break;

		case 0x13:
			value = lpx;
			break;

		case 0x14:
			value = lpy;
			break;

		case 0x19:
			// Interrupt Pending Register
			value = (byte) (irqFlags | 0x70);
			break;

		case 0x1a:
			// Interrupt Mask Register
			value = (byte) (irqMask | 0xf0);
			break;

		case 0x1e:
		case 0x1f: {
			// clear sprite collision registers after read
			final byte result = registers[register];
			registers[register] = 0;
			value = result;
			break;
		}

		// for addresses < $20 read from register directly, when < $2f set
		// bits of high nibble to 1, for >= $2f return $ff
		default:
			if (register < 0x20) {
				value = registers[register];
			} else if (register < 0x2f) {
				value = (byte) (registers[register] | 0xf0);
			} else {
				value = (byte) 0xff;
			}
		}

		setSpriteDataFromCPU(value);
		return value;
	}

	/** Display is enabled because badline condition was on for at least 1 clock */
	private final Event makeDisplayActive = new Event(
			"Activate display due to badline") {
		@Override
		public void event() {
			isDisplayActive = true;
		}
	};
	/** AEC state was updated. */
	private final Event badLineStateChange = new Event("Update AEC signal") {
		@Override
		public void event() {
			setBA(!isBadLine);
		}
	};

	/** RasterY IRQ edge detector. */
	protected final Event rasterYIRQEdgeDetector = new Event("RasterY changed") {
		@Override
		public void event() {
			final boolean oldRasterYIRQCondition = rasterYIRQCondition;
			rasterYIRQCondition = rasterY == readRasterLineIRQ();
			if (!oldRasterYIRQCondition && rasterYIRQCondition) {
				activateIRQFlag(IRQ_RASTER);
			}
		}
	};

	/** Handle lightpen state change */
	protected void lightpenEdgeDetector() {
		if (!lpAsserted) {
			return;
		}

		if (lpTriggered) {
			return;
		}
		lpTriggered = true;

		lpx = (byte) getCurrentSpriteCycle();
		lpx++;
		if (lpx == CYCLES_PER_LINE) {
			lpx = 0;
		}

		lpx <<= 2;
		lpx += context.phase() == Phase.PHI1 ? 1 : 2;

		lpy = (byte) (rasterY & 0xff);
		if (lineCycle == 9) {
			lpy++;
		}

		activateIRQFlag(MOS656X_INTERRUPT_LP);
	}

	/**
	 * Write to VIC register.
	 * 
	 * @param register
	 *            Register to write to.
	 * @param data
	 *            Data byte to write.
	 */
	@Override
	public final void write(int register, final byte data) {
		register &= 0x3f;
		registers[register] = data;

		setSpriteDataFromCPU(data);

		switch (register) {
		// x-coordinate of a spriteID has been modified
		case 0x00:
		case 0x02:
		case 0x04:
		case 0x06:
		case 0x08:
		case 0x0a:
		case 0x0c:
		case 0x0e: {
			// determine sprite to modify
			final int n = register >> 1;
			sprites[n].setX(readSpriteXCoordinate(n));
			handleSpriteVisibilityEvent(sprites[n]);
			break;
		}

		// y-coordinate of a spriteID has been modified
		case 0x01:
		case 0x03:
		case 0x05:
		case 0x07:
		case 0x09:
		case 0x0b:
		case 0x0d:
		case 0x0f:
			sprites[register >> 1].setY(data & 0xff);
			break;

		case 0x10: {
			// bit 9 of a sprite x-coordinate has been modified
			// recalculate all sprite x-coordinates
			for (int i = 0; i < 8; i++) {
				sprites[i].setX(readSpriteXCoordinate(i));
				handleSpriteVisibilityEvent(sprites[i]);
			}
			break;
		}

		case 0x11:
			// the graphics mode might have changed plus y-scroll value
			yscroll = data & 7;

			/* display enabled at any cycle of line 48 enables badlines */
			if (rasterY == FIRST_DMA_LINE) {
				areBadLinesEnabled |= readDEN();
			}

			/*
			 * At cycle 9, it's too late for the CPU to affect VIC's decisions
			 * for that line -- no more PHI1 cycles with that rasterY value
			 * remain.
			 */
			if (lineCycle != 9) {
				final int narrowing = readRSEL() ? 0 : 4;
				if (rasterY == FIRST_DMA_LINE + 3 + narrowing && readDEN()) {
					showBorderVertical = false;
				}
				if (rasterY == LAST_DMA_LINE + 4 - narrowing) {
					showBorderVertical = true;
				}
			}

			/* Re-evaluate badline condition */
			final boolean oldBadLine = isBadLine;
			isBadLine = evaluateIsBadLine();

			/*
			 * Schedule display activation if badlines are enabled for one
			 * clock.
			 */
			if (!oldBadLine && isBadLine) {
				context.schedule(makeDisplayActive, 1, Phase.PHI2);
			}

			/* Within the display range, VIC signals AEC changes on next PHI1 */
			if ((isBadLine ^ oldBadLine) && lineCycle > 20 && lineCycle < 63) {
				context.schedule(badLineStateChange, 0, Phase.PHI1);
			}
			// $FALL-THROUGH$

		case 0x12:
			/* check raster Y irq condition changes at the next PHI1 */
			context.schedule(rasterYIRQEdgeDetector, 0, Phase.PHI1);
			break;

		// the sprite enable byte has changed
		case 0x15:
			for (int i = 0; i < 8; i++) {
				sprites[i].setEnabled((data & 1 << i) != 0);
			}
			break;

		case 0x16: {
			/* Blank the pixels left side of the character in buffer2. */
			xscroll = (byte) (registers[0x16] & 7);
			int renderCycle = lineCycle - 26;
			if (renderCycle < 0) {
				renderCycle += CYCLES_PER_LINE;
			}
			/* the xscroll adjustment is not seen on the last cycle. */
			if (renderCycle != 39) {
				latchedXscroll = xscroll << 2;
			}
			break;
		}

		// the sprite y-expansion byte has changed
		case 0x17: {
			for (int i = 0; i < 8; i++) {
				final boolean expandY = (data & 1 << i) != 0;
				sprites[i].setExpandY(expandY, lineCycle == 24);
			}
			break;
		}

		// the cached video memory base addresses might have changed
		case 0x18:
			determineVideoMemoryBaseAddresses();
			break;

		// VIC Interrupt Flag Register
		case 0x19:
			irqFlags &= ~data & 0x0f | 0x80;
			handleIrqState();
			break;

		// IRQ Mask Register
		case 0x1a:
			irqMask = (byte) (data & 0x0f);
			handleIrqState();
			break;

		// the sprite priority byte has changed
		case 0x1b: {
			for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
				this.sprites[i]
						.setPriorityOverForegroundGraphics((data & m) == 0);
			}
			break;
		}

		// Sprites O-7 Multi-Color Mode Selection
		case 0x1c:
			for (int i = 0, m = 1; i < 8; ++i, m <<= 1) {
				this.sprites[i].setMulticolor((data & m) != 0);
			}
			break;

		// the sprite x-expansion byte has changed
		case 0x1d: {
			for (int i = 0, m = 1; i < 8; i++, m <<= 1) {
				this.sprites[i].setExpandX((data & m) != 0);
			}
			break;
		}

		// the border color was changed
		case 0x20:
			borderColor = (data & 0xf) * 0x11111111;
			break;

		// store color in internal registers
		case 0x21:
		case 0x22:
		case 0x23:
		case 0x24: {
			final int n = register - 0x21;
			videoModeColors[n] = (byte) (data & 0xf);
			break;
		}

		case 0x25:
			// sprite color one has been changed
			for (int i = 0; i < 8; i++) {
				sprites[i].setColor(1, (byte) (data & 0xf));
			}
			registers[register] |= 0xf0;
			break;

		case 0x26:
			// sprite color three has been changed
			for (int i = 0; i < 8; i++) {
				sprites[i].setColor(3, (byte) (data & 0xf));
			}
			registers[register] |= 0xf0;
			break;
		case 0x27:
		case 0x28:
		case 0x29:
		case 0x2a:
		case 0x2b:
		case 0x2c:
		case 0x2d:
		case 0x2e:
			// sprite color two has been changed, this can be done per sprite
			sprites[register - 0x27].setColor(2, (byte) (data & 0xf));
			this.registers[register] |= 0xf0;
			break;
		}
	}

	/**
	 * If CPU reads/writes to VIC at just the cycle VIC is supposed to do a PHI2
	 * fetch for sprite data, the data fetched is set from the interaction with
	 * CPU.
	 * 
	 * @param data The {@link Sprite} data to set.
	 */
	private void setSpriteDataFromCPU(byte data) {
		if (lineCycle >= 4 && lineCycle < 20) {
			Sprite damagedSprite = sprites[(lineCycle - 4) >> 1];
			damagedSprite.setSpriteByte((lineCycle & 1) == 0 ? 2 : 0, data);
		}
	}

	private int getCurrentSpriteCycle() {
		int spriteCoordinate = lineCycle - 23;
		if (spriteCoordinate < 0) {
			spriteCoordinate += CYCLES_PER_LINE;
		}
		return spriteCoordinate;
	}

	/**
	 * Schedule the rendering to begin
	 *
	 * @param sprite The {@link Sprite} to handle the visibility event of.
	 */
	private void handleSpriteVisibilityEvent(final Sprite sprite) {
		/* New coordinate in pipeline: cancel any pending event... */
		context.cancel(sprite);

		/* vic sprite x coordinate comparison will never trigger */
		if (sprite.getX() >> 3 >= CYCLES_PER_LINE) {
			return;
		}

		final int xpos = (sprite.getX() >> 3);

		/* calculate cycles until sprite can display */
		int count = xpos - getCurrentSpriteCycle();
		/*
		 * Wrap to nearest CYCLE when comparison will match.
		 * 
		 * VICE test spritex proves that setting location very near will not
		 * work: it takes one cycle for VIC to adjust to the new sprite
		 * position.
		 */
		if (count <= 0) {
			count += CYCLES_PER_LINE;
		}
		if (count > CYCLES_PER_LINE) {
			count -= CYCLES_PER_LINE;
		}

		/* Delay sprite by 0 .. 7 pixels. */
		sprite.setDisplayStart((sprite.getX() & 7));
		context.schedule(sprite, count, Event.Phase.PHI2);
	}

	/**
	 * Resets this VIC II Chip.
	 */
	public void reset() {
		spriteLinkedListHead.nextVisibleSprite = null;
		for (final Sprite s : sprites) {
			s.consuming = false;
		}

		// clear the screen
		for (int i = 0; i < pixels.length; ++i) {
			pixels[i] = ALPHA;
		}
		graphicsRendering = false;

		// reset all registers
		Arrays.fill(registers, (byte) 0);
		vc = 0;
		vcBase = 0;
		rc = 0;
		isDisplayActive = false;
		areBadLinesEnabled = false;
		rasterY = 0;
		phi1Data = 0;
		showBorderVertical = true;
		xscroll = 0;
		yscroll = 0;
		irqFlags = 0;
		irqMask = 0;
		nextPixel = 0;
		lpx = 0;
		lpy = 0;
		determineVideoMemoryBaseAddresses();
	}

	/**
	 * Gets the credit string.
	 *
	 * @return The credit string.
	 */
	public static String credits() {
		return credit;
	}

	public final void addPropertyChangeListener(
			final PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
		support.addPropertyChangeListener(listener);
	}

	/**
	 * Gets the currently used palette.
	 *
	 * @return The currently used palette.
	 */
	public abstract Palette getPalette();

	/**
	 * Updates the palette
	 */
	public abstract void updatePalette();

	/**
	 * Trigger the lightpen.
	 * Sets the lightpen usage flag.
	 */
	public void triggerLightpen() {
		lpAsserted = true;
		lightpenEdgeDetector();
	}

	/**
	 * Clears the lightpen usage flag.
	 */
	public void clearLightpen() {
		lpAsserted = false;
	}

	protected void interrupt(boolean b) {
		pla.setIRQ(b);
	}

	protected void setBA(boolean b) {
		pla.setBA(b);
	}

	protected byte vicReadColorMemoryPHI2(int address) {
		return pla.vicReadColorMemoryPHI2(address);
	}

	protected byte vicReadMemoryPHI1(int address) {
		return pla.vicReadMemoryPHI1(address);
	}

	protected byte vicReadMemoryPHI2(int address) {
		return pla.vicReadMemoryPHI2(address);
	}

	public abstract int getBorderWidth();

	public abstract int getBorderHeight();

	public byte[] getRegisters() {
		return registers;
	}
}