package libsidplay.components.mos656x;

import java.util.function.Consumer;

import libsidplay.common.VICChipModel;

public class PALEmulation {
	/** Alpha channel of ARGB pixel data. */
	private static final int ALPHA = 0xff000000;

	/**
	 * If PAL emulation is turned off: use this palette for VIC colors 0-15.
	 * https://www.pepto.de/projects/colorvic/2001/
	 */
	private final int[] vicPaletteNoPal = new int[] { 0x000000, 0xFFFFFF, 0x68372B, 0x70A4B2, 0x6F3D86, 0x588D43,
			0x352879, 0xB8C76F, 0x6F4F25, 0x433900, 0x9A6759, 0x444444, 0x6C6C6C, 0x9AD284, 0x6C5EB5, 0x959595, };
	/** Table for looking up color using a packed 2x8 value for even rasterlines */
	private final int[] combinedLinesEven = new int[256 * 256];
	/** Table for looking up color using a packed 2x8 value for odd rasterlines */
	private final int[] combinedLinesOdd = new int[256 * 256];
	/** VIC color palette for even rasterlines */
	private final byte[] linePaletteEven = new byte[16 * 16 * 16 * 16];
	/** VIC color palette for odd rasterlines */
	private final byte[] linePaletteOdd = new byte[16 * 16 * 16 * 16];
	/** Last line's color */
	private final byte[] previousLineDecodedColor = new byte[65 * 8];
	/** Prevailing table for looking up color for current line (odd/even) */
	private int[] combinedLinesCurrent;
	/** Prevailing VIC color palette for current line (odd/even) */
	private byte[] linePaletteCurrent;
	/** Index into last line */
	private int previousLineIndex;
	/** Previous sequencer data */
	private int oldGraphicsData;

	/** VIC chip model */
	private final VICChipModel model;

	/** Use PAL emulation? */
	private boolean palEmulationEnable;

	/** System's palette */
	private final Palette palette = new Palette();

	public PALEmulation(VICChipModel model) {
		this.model = model;
		this.palEmulationEnable = true;
	}

	public void setPalEmulationEnable(boolean palEmulationEnable) {
		this.palEmulationEnable = palEmulationEnable;
	}

	public void setVicPaletteNoPal(int[] vicPaletteNoPal) {
		assert vicPaletteNoPal.length == 16;

		System.arraycopy(vicPaletteNoPal, 0, vicPaletteNoPal, 0, vicPaletteNoPal.length);
	}

	/**
	 * Gets the currently used palette.
	 *
	 * @return The currently used palette.
	 */
	public Palette getPalette() {
		return palette;
	}

	/**
	 * Updates the palette using the current palette settings.
	 */
	public void updatePalette() {
		palette.calculatePalette(Palette.buildPaletteVariant(model));
		System.arraycopy(palette.getEvenLines(), 0, combinedLinesEven, 0, combinedLinesEven.length);
		System.arraycopy(palette.getOddLines(), 0, combinedLinesOdd, 0, combinedLinesOdd.length);
		System.arraycopy(palette.getEvenFiltered(), 0, linePaletteEven, 0, linePaletteEven.length);
		System.arraycopy(palette.getOddFiltered(), 0, linePaletteOdd, 0, linePaletteOdd.length);
	}

	/**
	 * Determine palette for current raster line.
	 *
	 * @param rasterY      current raster line
	 * @param isFrameStart a new frame is about to start?
	 */
	public void determineCurrentPalette(int rasterY, boolean isFrameStart) {
		if (isFrameStart) {
			/* current row odd? -> start with even, init, swap */
			linePaletteCurrent = (rasterY & 1) != 0 ? linePaletteEven : linePaletteOdd;
			combinedLinesCurrent = (rasterY & 1) != 0 ? combinedLinesEven : combinedLinesOdd;
			for (int i = 0; i < previousLineDecodedColor.length; i++) {
				previousLineDecodedColor[i] = linePaletteCurrent[0];
			}
		}
		linePaletteCurrent = linePaletteCurrent == linePaletteOdd ? linePaletteEven : linePaletteOdd;
		combinedLinesCurrent = combinedLinesCurrent == combinedLinesOdd ? combinedLinesEven : combinedLinesOdd;
		oldGraphicsData = 0;
		previousLineIndex = 0;
	}

	/**
	 * Draw eight pixels at once. Pixels arrive in 0x12345678 order (MSB to LSB).
	 *
	 * @param graphicsDataBuffer eight pixels each of 4 bits (VIC color value range
	 *                           0x0-0xF)
	 * @param pixelConsumer      consumer of the corresponding RGBA pixels
	 */
	public void drawPixels(int graphicsDataBuffer, Consumer<Integer> pixelConsumer) {
		/* Pixels arrive in 0x12345678 order. */
		for (int j = 0; j < 2; j++) {
			oldGraphicsData |= graphicsDataBuffer >>> 16;
			for (int i = 0; i < 4; i++) {
				oldGraphicsData <<= 4;
				final int vicColor = oldGraphicsData >>> 16;
				final byte lineColor = linePaletteCurrent[vicColor];
				final byte previousLineColor = previousLineDecodedColor[previousLineIndex];
				int rgbaColor;
				if (palEmulationEnable) {
					rgbaColor = ALPHA | combinedLinesCurrent[lineColor & 0xff | previousLineColor << 8 & 0xff00];
				} else {
					rgbaColor = ALPHA | vicPaletteNoPal[vicColor & 0x0f];
				}
				pixelConsumer.accept(rgbaColor);
				previousLineDecodedColor[previousLineIndex++] = lineColor;
			}
			graphicsDataBuffer <<= 16;
		}

	}

}
