/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package libsidplay.components.mos656x;

import libsidplay.common.Event;

/**
 * Sprite class to handle all data for sprites.
 * 
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 * @author Antti S. Lankila (alankila@bel.fi)
 */
public final class Sprite extends Event {

	/** Sprite index */
	protected final int index;

	/** MOS656X instance this sprite belongs to */
	private final Sprite linkedListHead;

	/** Visible sprites in linked list; highest priority sprite first */
	protected Sprite nextVisibleSprite;

	@Override
	public void event() {
		/* already being displayed? ignore event */
		if (consuming) {
			return;
		}

		/* no display bit? ignore event. */
		if (! display) {
			return;
		}

		consuming = true;
		int delayLength = delayPixels;

		if (expandX) {
			firstXRead = (delayLength & 1) == 0;
			delayLength >>= 1;
		} else {
			firstXRead = true;
		}

		if (multiColor) {
			firstMultiColorRead = (delayLength & 1) == 0;
		} else {
			firstMultiColorRead = true;
		}

		consumedLineData = lineData << 8-delayLength;
		lineData = 0;

		prevPixel = 0;
		prevPriority = 0;
		expandXLatched = expandX;
		multiColorLatched = multiColor;
		priorityMask = priorityOverForegroundGraphics ? 0xffffffff : 0;

		/* put ourselves in appropriate place in the sprite linked list */
		Sprite current = linkedListHead;
		/* we are already being rendered? Don't reschedule, or it will jam. */
		while (true) {
			/* find a place to tuck ourselves in */
			if (current.nextVisibleSprite == null || current.nextVisibleSprite.index < index) {
				Sprite.this.nextVisibleSprite = current.nextVisibleSprite;
				current.nextVisibleSprite = Sprite.this;
				break;
			}

			current = current.nextVisibleSprite;
		}
	}

	public Sprite(final Sprite linkedListHead, final int index) {
		super("Sprite " + index);
		this.linkedListHead = linkedListHead;
		this.index = index;
		this.indexBits = (8 | index) * 0x11111111;
	}

	/** Position sprite start within character cell */
	private int delayPixels;

	/**
	 * Delay sprite data appearance by given count of pixels.
	 * 
	 * @param delayPixels
	 */
	protected void setDisplayStart(final int delayPixels) {
		this.delayPixels = delayPixels;
	}

	/** Is display enabled */
	private boolean display;
	/** Is sprite pixel pipeline active? */
	boolean consuming;
	/** This is the first read of multicolor pixel? (consuming read) */
	private boolean firstMultiColorRead;
	/** 32 bits of sprite data in reversed bit order (LSB = first out) */
	private int lineData;
	/** Sprite address byte */
	private byte pointerByte;
	/** Data byte to fetch next */
	private int mcBase;
	/** Data byte at DMA */
	private int mc;
	/** This is the first read of a line for an Y-expanded sprite? (no pointer increment) */
	private boolean firstYRead;
	/** This is the first read of a pixel for an X-expanded sprite? (consuming read) */
	private boolean firstXRead;

	/** Sprite position */
	private int x, y;
	/** Is the sprite current enabled */
	private boolean enabled;
	/** Is the sprite expanded horizontally? */
	private boolean expandX;
	/** Is the sprite expanded vertically? */
	private boolean expandY;
	/** Multicolor mode on? */
	private boolean multiColor;
	/** Does the sprite have priority over the screen background? */
	private boolean priorityOverForegroundGraphics;
	/**
	 * The masking to be used for migrating color 1 as foreground color
	 * during sprite priority bit handling
	 */
	private int priorityMask;

	/**
	 * Get the X-coordinate of the sprite
	 * 
	 * @return X-coordinate
	 */
	public final int getX() {
		return x;
	}

	/**
	 * Set the X-coordinate of the sprite
	 * 
	 * @param x new X-coordinate
	 */
	public final void setX(final int x) {
		this.x = x;
	}

	/**
	 * Get the Y-coordinate of the sprite
	 * 
	 * @param Y-coordinate
	 */
	public final int getY() {
		return y;
	}

	/**
	 * Set the Y-coordinate of the sprite
	 * 
	 * @param y new Y-coordinate
	 */
	public final void setY(final int y) {
		this.y = y;
	}

	/**
	 * Set whether the sprite has priority over the screen background
	 * 
	 * @param hasPriority
	 *            true if the sprite has priority over the screen content
	 */
	public final void setPriorityOverForegroundGraphics(final boolean priority) {
		if (priority == priorityOverForegroundGraphics) {
			return;
		}

		priorityOverForegroundGraphics = priority;
		/* the flag takes effect after 6 pixels,
		 * hence changes to it look like this. */
		if (priorityOverForegroundGraphics) {
			priorityMask = 0xff000000;
		} else {
			priorityMask = 0x00ffffff;
		}
	}

	/**
	 * Check whether the sprite is currently enabled
	 * 
	 * @return true if enabled
	 */
	public final boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set whether the sprite is currently enabled
	 * 
	 * @param enabled true if enabled
	 */
	public final void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Set expandX flag for horizontal expansion
	 * 
	 * @param expandX true if expanded
	 */
	public final void setExpandX(final boolean expandX) {
		this.expandX = expandX;
	}

	/**
	 * Set expandY flag for vertical expansion
	 *
	 * @param expandY true if expanded
	 * @param crunchCycle whether to do sprite crunch
	 */
	public final void setExpandY(final boolean expandY, final boolean crunchCycle) {
		if (this.expandY == expandY) {
			return;
		}
		this.expandY = expandY;

		/* clearing y expansion bit resets the flip-flop */
		if (! expandY) {
			firstYRead = false;
			/* line crunch. */
			if (crunchCycle) {
				mc = 42 & mc & mcBase | 21 & (mc | mcBase);
			}
		}
	}

	/**
	 * Change multicolor flag.
	 * 
	 * @param multiColor true if multicolor
	 */
	public final void setMulticolor(final boolean multiColor) {
		this.multiColor = multiColor;
	}

	/**
	 * Reset current DMA byte and expansion/multicolor flip-flops.
	 */
	public final void beginDMA() {
		if (isDMA()) {
			return;
		}
		mcBase = 0;
		firstYRead = false;
	}

	public final boolean isDMA() {
		return mcBase != 63;
	}

	public void setDisplay(final boolean display) {
		if (display && !allowDisplay) {
			return;
		}
		this.display = display;
	}

	/**
	 * Set the address we read the sprite data from
	 * 
	 * @param pointer
	 *            address to read from
	 */
	public final void setPointerByte(final byte pointerByte) {
		this.pointerByte = pointerByte;
	}

	public int getCurrentByteAddress() {
		final int pointer = (pointerByte & 0xff) << 6;
		int address = pointer | mc;
		mc = (mc + 1) & 0x3f;
		return address;
	}

	/**
	 * Store a sprite byte into sprite shift register for use.
	 */
	public final void setSpriteByte(int idx, byte value) {
		lineData &= ~(0xff << (idx * 8));
		lineData |= (value & 0xff) << (idx * 8);
	}
	
	/** Begin rendering sprite on a line */
	public void initDmaAccess() {
		mc = mcBase;
	}

	/** Increment sprite read pointer during Y expansion */
	public final void finishDmaAccess() {
		// we have to read this line again if the Y-expansion is set and this
		// was the first read
		if (! firstYRead) {
			mcBase = mc;
		}
	}

	/** Toggle sprite Y expansion flag */
	public final void expandYFlipFlop() {
		if (expandY) {
			firstYRead = !firstYRead;
		}
	}

	/** Read the color number of the next pixel of the current sprite line.
	 * 
	 * Colors 0, 1 are background colors,
	 * color 2 sprite color #2,
	 * colors 3-> sprite-specific colors.
	 */
	private boolean multiColorLatched;
	private int prevPixel;
	private boolean expandXLatched;

	private int consumedLineData;

	/** Allow display to be enabled. */
	private boolean allowDisplay;

	/** Sprite colors: 0, 1, and our own color. */
	private final int[] color = new int[4];

	private int prevPriority;

	public final void setColor(int idx, int val) {
		color[idx] = val;
	}

	public final int getColor(int idx) {
		return color[idx];
	}
	
	protected int colorBuffer;

	protected final int indexBits;

	/**
	 * Generate graphics data for the next 8 pixels for the sprite
	 * 
	 * Foreground pixels are identified by having any bit set.
	 * 
	 * @return packed pixel array with colors 0, 1, 2, 3+index
	 */
	public final int calculateNext8Pixels() {
		int priorityBuffer = 0;

		/* Common case of unexpanded sprite. We happen to know how to do these
		 * very efficiently. Watch...
		 */
		if (!expandX && !expandXLatched && !multiColor && !multiColorLatched) {
			int priorityH = VIC.singleColorLUT[consumedLineData >>> 28];
			consumedLineData <<= 4;
			int priorityL = VIC.singleColorLUT[consumedLineData >>> 28];
			consumedLineData <<= 4;
			if (consumedLineData == 0) {
				consuming = false;
			}
			int priority = priorityH << 16 | priorityL & 0xffff;
			colorBuffer = color[2] * 0x11111111 & priority;
			return priority;
		}
		
		colorBuffer = 0;
		
		/* sprite state:
		 * 
		 * 1) multicolor 4-state: on, off, turning on, turning off
		 * 2) expand-x 4-state: on, off, turning on, turning off
		 * 3) expandx current flop
		 * 4) multicolor current flop
		 * 
		 * yielding 64 different decoding combinations + first pixel handling
		 * for the flop-related cases.
		 */

		for (int pixelIndex = 0; pixelIndex < 8; pixelIndex ++) {
			switch (pixelIndex) {
			case 6:
				if (expandXLatched != expandX) {
					expandXLatched = expandX;
					if (! expandXLatched) {
						firstXRead = true;
					}
				}
				break;
			case 7:
				if (multiColorLatched != multiColor) {
					multiColorLatched = multiColor;
					firstMultiColorRead = false;
				}
				break;
			}

			if (firstXRead) {
				if (multiColorLatched) {
					if (firstMultiColorRead) {
						if (consumedLineData == 0) {
							consuming = false;
						}
						
						prevPriority = (consumedLineData >>> 30) != 0 ? 0xf : 0;
						prevPixel = color[consumedLineData >>> 30];
					}
					firstMultiColorRead = !firstMultiColorRead;
				} else {
					if (consumedLineData == 0) {
						consuming = false;
					}
					
					prevPriority = consumedLineData >> 31 & 0xf;
					prevPixel = prevPriority & color[2];
				}
				consumedLineData <<= 1;
			}
			if (expandXLatched) {
				firstXRead = !firstXRead;
			}

			colorBuffer <<= 4;
			colorBuffer |= prevPixel;
			
			priorityBuffer <<= 4;
			priorityBuffer |= prevPriority;
		}
		
		return priorityBuffer;
	}

	/**
	 * Damage the sprite display around the pointer fetch region.
	 * 
	 * The 3rd pixel into that region is duplicated 9 times, and nothing after
	 * that is shown.
	 */
	public void repeatPixels() {
		consumedLineData &= 0xc0000000;
		if ((consumedLineData & 0x40000000) != 0) {
			consumedLineData |= 0x7f800000;
		}
	}

	public int getNextPriorityMask() {
		final int mask = priorityMask;
		priorityMask = (mask >>> 24) * 0x01010101;
		return mask;
	}

	public void setAllowDisplay(final boolean allowDisplay) {
		this.allowDisplay = allowDisplay;
	}
}
