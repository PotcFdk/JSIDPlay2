/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package libsidplay.components.mos656x;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.pla.PLA;

/**
 * NTSC specialization of the VIC
 * 
 * @author alankila
 */
public class MOS6567 extends VIC {
	private final Palette palette = new Palette();

	public MOS6567(PLA pla, EventScheduler context) {
		super(pla, context, 65);
		updatePalette();
	}

	private static final int MAX_RASTERS = 263;
	private static final int CYCLES_PER_LINE = 65;
	private static final int FIRST_DISPLAY_LINE = 40;
	private static final int LAST_DISPLAY_LINE = 13;

	protected void doPHI1Fetch() {
		switch (lineCycle) {
		case 1:
		case 2:
		case 19:
		case 65:
			/* idle gap accesses are cycles outside screen region */
			phi1Data = vicReadMemoryPHI1(0x3fff);
			return;

		case 3:
		case 5:
		case 7:
		case 9:
		case 11:
		case 13:
		case 15:
		case 17: {
			/* sprite data pointer byte */
			final int n = lineCycle - 3 >> 1;
			phi1Data = vicReadMemoryPHI1(videoMatrixBase | 0x03f8 | n);
			return;
		}

		case 4:
		case 6:
		case 8:
		case 10:
		case 12:
		case 14:
		case 16:
		case 18: {
			int n = (lineCycle - 4) >> 1;
			if (sprites[n].isDMA()) {
				int address = sprites[n].getCurrentByteAddress();
				phi1Data = vicReadMemoryPHI1(address);
			} else {
				phi1Data = vicReadMemoryPHI1(0x3fff);
			}
			return;
		}

		default: {
			/* 25-64 */
			int n = lineCycle - 25;

			int address = 0x3fff;
			if ((registers[0x11] & 0x40) != 0) {
				address ^= 0x600;
			}
			if (isDisplayActive) {
				if ((registers[0x11] & 0x20) != 0) {
					address &= bitmapMemBase | vc << 3 | rc;
				} else {
					address &= charMemBase | (videoMatrixData[n] & 0xff) << 3
							| rc;
				}
				vc = vc + 1 & 0x3ff;
			}

			phi1Data = vicReadMemoryPHI1(address);
			return;
		}

		case 20:
		case 21:
		case 22:
		case 23:
		case 24: {
			/* dram refresh. */
			final int n = lineCycle - 20;
			final int offset = 0xff - rasterY * 5 - n & 0xff;
			phi1Data = vicReadMemoryPHI1(0x3f00 | offset);
		}
		}
	}

	private final Event event = new Event("MOS6567") {
		@Override
		public final void event() throws InterruptedException {
			if (lineCycle == CYCLES_PER_LINE) {
				lineCycle = 0;
			}
			lineCycle++;

			if (graphicsRendering
					&& (lineCycle >= 23 || lineCycle < 71 - CYCLES_PER_LINE)) {
				drawSpritesAndGraphics();
			} else {
				spriteCollisionsOnly();
			}

			doPHI1Fetch();

			switch (lineCycle) {

			case 1:
				/* idle gap access */
				for (final Sprite sprite : sprites) {
					if (sprite.isEnabled() && sprite.getY() == (rasterY & 0xff)) {
						sprite.beginDMA();
						sprite.setAllowDisplay(true);
					} else {
						sprite.setAllowDisplay(false);
					}
					sprite.expandYFlipFlop();
				}

				setBA(!sprites[0].isDMA());
				break;

			case 2:
				/* idle gap access */
				setBA(!sprites[0].isDMA() && !sprites[1].isDMA());
				break;

			case 3:
				/* sprite 0 pointer access */
				for (final Sprite sprite : sprites) {
					if (sprite.isEnabled() && sprite.getY() == (rasterY & 0xff)) {
						sprite.setDisplay(true);
					}
					if (!sprite.isDMA()) {
						sprite.setDisplay(false);
					}

					sprite.initDmaAccess();
				}
				fetchSpritePointer(0);
				// already done: env.signalAEC(!sprites[0].isDMA() &&
				// !sprites[1].isDMA());
				break;

			case 4:
				if (rc == 7) {
					vcBase = vc;
					isDisplayActive = isBadLine;
				}
				if (isDisplayActive) {
					rc = rc + 1 & 7;
				}

				/* sprite 0 data access */
				fetchSpriteData(0);
				setBA(!sprites[0].isDMA() && !sprites[1].isDMA()
						&& !sprites[2].isDMA());
				break;

			case 5:
				/* sprite 1 pointer access */
				fetchSpritePointer(1);
				setBA(!sprites[1].isDMA() && !sprites[2].isDMA());
				break;

			case 6:
				/* sprite 1 data access */
				fetchSpriteData(1);
				setBA(!sprites[1].isDMA() && !sprites[2].isDMA()
						&& !sprites[3].isDMA());
				break;

			case 7:
				/* sprite 2 pointer access */
				fetchSpritePointer(2);
				setBA(!sprites[2].isDMA() && !sprites[3].isDMA());
				break;

			case 8:
				/* sprite 2 data access */
				fetchSpriteData(2);
				setBA(!sprites[2].isDMA() && !sprites[3].isDMA()
						&& !sprites[4].isDMA());
				break;

			case 9:
				fetchSpritePointer(3);
				setBA(!sprites[3].isDMA() && !sprites[4].isDMA());
				break;

			case 10: {
				if (rasterY == MAX_RASTERS - 1) {
					vcBase = 0;
					/*
					 * last line is 1 cycle longer than it appears to be. To set
					 * rasterY = 0 at next cycle, we use a flag.
					 */
					startOfFrame = true;
				} else {
					rasterY++;
					rasterYIRQEdgeDetector.event();
				}

				// increase raster counter
				/*
				 * since raster Y just changed, we need to find out if we have
				 * entered a badline. the CPU can change the conditions any
				 * time, but since here VIC changes the raster, we *must* check
				 * badline on this cycle. In particular, linecrunch depends on
				 * canceling the bad line between the cycles 10 - 24.
				 */
				if (rasterY == FIRST_DMA_LINE) {
					areBadLinesEnabled = readDEN();
				}

				/*
				 * mysteriously, isDisplayActive is determined already on this
				 * cycle. Normally it takes 1 clock to take effect.
				 */
				isBadLine = evaluateIsBadLine();
				isDisplayActive |= isBadLine;

				final int narrowing = readRSEL() ? 0 : 4;
				if (rasterY == FIRST_DMA_LINE + 3 + narrowing && readDEN()) {
					showBorderVertical = false;
				}
				if (rasterY == LAST_DMA_LINE + 4 - narrowing) {
					showBorderVertical = true;
				}

				latchedXscroll = xscroll << 2;
				oldGraphicsData = 0;
				previousLineIndex = 0;
				if (rasterY == FIRST_DISPLAY_LINE) {
					/* current row odd? -> start with even, init, swap */
					linePaletteCurrent = (rasterY & 1) != 0 ? linePaletteEven
							: linePaletteOdd;
					combinedLinesCurrent = (rasterY & 1) != 0 ? combinedLinesEven
							: combinedLinesOdd;
					graphicsRendering = true;
					nextPixel = 0;
					for (int i = 0; i < previousLineDecodedColor.length; i++) {
						previousLineDecodedColor[i] = linePaletteCurrent[0];
					}
				}
				linePaletteCurrent = linePaletteCurrent == linePaletteOdd ? linePaletteEven : linePaletteOdd;
				combinedLinesCurrent = combinedLinesCurrent == combinedLinesOdd ? combinedLinesEven : combinedLinesOdd;

				if (rasterY == LAST_DISPLAY_LINE + 1) {
					graphicsRendering = false;
					support.firePropertyChange("pixels", null, pixels);
				}
				setBA(!sprites[3].isDMA() && !sprites[4].isDMA()
						&& !sprites[5].isDMA());
				fetchSpriteData(3);
				break;
			}

			case 11: {
				/* sprite 3 data access */
				if (startOfFrame) {
					startOfFrame = false;
					rasterY = 0;
					rasterYIRQEdgeDetector.event();
					lpTriggered = false;
					lightpenEdgeDetector();
				}

				/* sprite 4 pointer access */
				fetchSpritePointer(4);
				setBA(!sprites[4].isDMA() && !sprites[5].isDMA());
				break;
			}

			case 12:
				/* sprite 4 data access */
				fetchSpriteData(4);
				setBA(!sprites[4].isDMA() && !sprites[5].isDMA()
						&& !sprites[6].isDMA());
				break;

			case 13:
				/* sprite 5 pointer access */
				fetchSpritePointer(5);
				setBA(!sprites[5].isDMA() && !sprites[6].isDMA());
				break;

			case 14:
				/* sprite 5 data access access */
				fetchSpriteData(5);
				setBA(!sprites[5].isDMA() && !sprites[6].isDMA()
						&& !sprites[7].isDMA());
				break;

			case 15:
				/* sprite 6 pointer & data data access */
				fetchSpritePointer(6);
				setBA(!sprites[6].isDMA() && !sprites[7].isDMA());
				break;

			case 16:
				/* sprite 6 data access */
				fetchSpriteData(6);
				break;

			case 17:
				/* sprite 7 pointer access */
				fetchSpritePointer(7);
				setBA(!sprites[7].isDMA());
				break;

			case 18:
				/* sprite 7 data access */
				fetchSpriteData(7);
				break;

			case 19:
				setBA(true);
				break;

			case 20:
				/* dram refresh 0 cycle */
				break;

			case 21:
				/* dram refresh 1 */
				setBA(!isBadLine);
				break;

			case 22:
				/* dram refresh 2 */
				break;

			case 23:
				/* dram refresh 3 */
				vc = vcBase;
				if (isBadLine) {
					rc = 0;
				}
				break;

			case 24:
				/* dram refresh 4 */
				if (isBadLine) {
					doVideoMatrixAccess();
				}
				break;

			case 25:
				/* graphics memory access 0 */
				if (isBadLine) {
					doVideoMatrixAccess();
				}

				for (final Sprite sprite : sprites) {
					if (sprite.isDMA()) {
						sprite.finishDmaAccess();
					}
				}
				break;

			default:
				/* graphics memory access */
				if (isBadLine) {
					doVideoMatrixAccess();
				}
				break;

			case 64:
				setBA(true);
				break;

			case 65:
				for (final Sprite sprite : sprites) {
					if (sprite.isEnabled() && sprite.getY() == (rasterY & 0xff)) {
						sprite.beginDMA();
					}
				}

				setBA(!sprites[0].isDMA());
				break;
			}
			
			context.schedule(this, 1);
		}
	};

	@Override
	public final void reset() {
		super.reset();
		lineCycle = 9; // preincremented at event
		context.schedule(event, 0, Phase.PHI1);
	}

	@Override
	protected void lightpenEdgeDetector() {
		if (rasterY != MAX_RASTERS - 1) {
			super.lightpenEdgeDetector();
		}
	}

	@Override
	public int getBorderWidth() {
		return (40 + 4 + 4) * 8;
	}

	@Override
	public int getBorderHeight() {
		return LAST_DISPLAY_LINE - FIRST_DISPLAY_LINE + MAX_RASTERS;
	}

	@Override
	public void updatePalette() {
		palette.calculatePalette(Palette.buildPaletteVariant(VIC.Model.MOS6567R8));
		System.arraycopy(palette.getEvenLines(), 0, combinedLinesEven, 0, combinedLinesEven.length);
		System.arraycopy(palette.getOddLines(), 0, combinedLinesOdd, 0, combinedLinesOdd.length);
		System.arraycopy(palette.getEvenFiltered(), 0, linePaletteEven, 0, linePaletteEven.length);
		System.arraycopy(palette.getOddFiltered(), 0, linePaletteOdd, 0, linePaletteOdd.length);
	}

	@Override
	public Palette getPalette() {
		return palette;
	}
}