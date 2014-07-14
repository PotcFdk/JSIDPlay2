package libsidplay.components.printer.mps803;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.iec.IECBus;
import libsidplay.components.iec.SerialIECDevice;
import libsidplay.components.printer.IPaper;
import libsidplay.components.printer.UserportPrinterEnvironment;
import libsidplay.components.printer.paper.ConsolePaper;

/**
 * Note: Normally this printer is connected as a serial device, this emulation
 * uses a connection via user port as well<BR>
 * 
 * <TABLE>
 * <TR>
 * <TD>General Specifications</TD>
 * </TR>
 * <TR>
 * <TD>A. Print method</TD>
 * <TD>Impact Dot Matrix</TD>
 * </TR>
 * <TR>
 * <TD>B. Printing direction</TD>
 * <TD>Bi-directional</TD>
 * </TR>
 * <TR>
 * <TD>C. Character matrix</TD>
 * <TD>7x6 dot matrix</TD>
 * </TR>
 * <TR>
 * <TD>D. Characters</TD>
 * <TD>Upper/lower case characters, numerals, symbols, and PET graphic
 * characters</TD>
 * </TR>
 * <TR>
 * <TD>E. Bit Image type</TD>
 * <TD>7 Vertical Dots Bit Image Printing</TD>
 * </TR>
 * <TR>
 * <TD>F. Character codes</TD>
 * <TD>CBM ASCII CODE (8 Bit}</TD>
 * </TR>
 * <TR>
 * <TD>G. Character composition</TD>
 * <TD>Vertical: 7 dots [0.09", 2.4mm), Horizontal: 6 dots [0.08", 2.2mm)</TD>
 * </TR>
 * <TR>
 * <TD>H. Dot size</TD>
 * <TD>0.3mm (wire diameter), Horizontal pitch 1/60", Vertical pitch 1/72"</TD>
 * </TR>
 * <TR>
 * <TD>I. Print speed</TD>
 * <TD>60 characters per second</TD>
 * </TR>
 * <TR>
 * <TD>J. Column width</TD>
 * <TD>80 characters</TD>
 * </TR>
 * <TR>
 * <TD>K. Column spacing</TD>
 * <TD>10 characters/inch</TD>
 * </TR>
 * <TR>
 * <TD>L. Line spacing</TD>
 * <TD>6 lines/inch (USA) or 8 lines/inch (Europe}. 72/7 lines/inch in bit image
 * printing</TD>
 * </TR>
 * <TR>
 * <TD>M. Line feed speed</TD>
 * <TD>4 lines/sec .... in character printing, 5.6 lines/sec ... in bit image
 * printing</TD>
 * </TR>
 * <TR>
 * <TD>N. Paper feed method</TD>
 * <TD>Friction feed, Tractor feed optional</TD>
 * </TR>
 * <TR>
 * <TD>0. Paper width</TD>
 * <TD>Cut sheet, A4 210.8mm (8.3"'}, Letter size 2l6mm (8.5"), Continuous,
 * 101.6mm (4.0") to 254mm (10jr), With optional tractor feed</TD>
 * </TR>
 * <TR>
 * <TD>P. Number of copies</TD>
 * <TD>Original + 2 copies</TD>
 * </TR>
 * <TR>
 * <TD>Q. Inked ribbon</TD>
 * <TD>Cassette type fabric ribbon [black], 8mm x 10 meters</TD>
 * </TR>
 * <TR>
 * <TD>R. Ribbon life</TD>
 * <TD>1.2 x 10 characters</TD>
 * </TR>
 * </TABLE>
 * 
 * @author Ken Händel
 * 
 */
public abstract class MPS803 extends SerialIECDevice implements
		UserportPrinterEnvironment {

	private static final byte[] MPS803_CHARSET_BIN = new byte[3584];
	static {
		try (DataInputStream is = new DataInputStream(
				MPS803.class
						.getResourceAsStream("/libsidplay/roms/mps803char.bin"))) {
			is.readFully(MPS803_CHARSET_BIN);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Maximum column with.
	 */
	protected static final int COLUMN_WIDTH = 80;
	/**
	 * Character width in dots.
	 */
	protected static final int CHAR_WIDTH = 6;
	/**
	 * Character height in dots.
	 */
	protected static final int CHAR_HEIGHT = 7;
	/**
	 * Maximum number of dots horizontally (6 pixels * 80 characters).
	 */
	public static final int MAX_WIDTH = COLUMN_WIDTH * CHAR_WIDTH;

	//
	// Special printer commands.
	//

	/**
	 * Bit image printing.
	 */
	private static final byte BIT_IMAGE_PRINTING = 8;
	/**
	 * Line feed character.
	 */
	private static final byte LINE_FEED = 10;
	/**
	 * Carriage return character.
	 */
	private static final byte CARRIAGE_RETURN = 13;
	/**
	 * Enhance mode on (Print characters in double width).
	 */
	private static final byte ENHANCE_ON = 14;
	/**
	 * Enhance mode off.
	 */
	private static final byte ENHANCE_OFF = 15;
	/**
	 * Dot Address Determination (Advance to a start position).
	 */
	private static final byte DOT_ADDRESS_TERMINATION = 16;
	/**
	 * Business mode (lower/upper-case characters).
	 */
	private static final byte BUSINESS_MODE = 17;
	/**
	 * Graphic mode (upper-case/graphic characters).
	 */
	private static final byte GRAPHIC_MODE = (byte) (BUSINESS_MODE | 0x80);
	/**
	 * Turn on reverse mode (print black pixels white and vice-versa).
	 */
	private static final byte REVERSE_ON = 18;
	/**
	 * Turn off reverse mode.
	 */
	private static final byte REVERSE_OFF = (byte) (REVERSE_ON | 0x80);
	/**
	 * Repeat Bit Image Printing (print repeated bit image data).
	 */
	private static final byte REPEAT_BIT_IMAGE = 26;
	/**
	 * Initiate escape sequence.
	 */
	private static final byte ESCAPE = 27;

	//
	// printer states
	//

	/**
	 * REVERSE_ON state.
	 */
	protected static final int STATE_REVERSE = 0x01;
	/**
	 * GRAPHIC_MODE state (BUSINESS_MODE, if not set).
	 */
	protected static final int STATE_GRAPHIC = 0x02;
	/**
	 * BIT_IMAGE_PRINTING state.
	 */
	protected static final int STATE_BIT_IMAGE_PRINTING = 0x04;
	/**
	 * ENHANCE_ON state.
	 */
	protected static final int STATE_ENHANCE = 0x08;
	/**
	 * REPEAT_BIT_IMAGE state.
	 */
	protected static final int STATE_REPEAT_BIT_IMAGE = 0x10;
	/**
	 * ESCAPE state
	 */
	protected static final int STATE_ESCAPE = 0x20;

	/**
	 * Current printer state (bit-mask of the fields above).
	 */
	protected int state;

	/**
	 * Buffered line data to print (480 dots horizontal * 7 dots vertical).
	 */
	protected boolean lineBuffer[][];
	/**
	 * Current line buffer horizontal pixel position.
	 */
	protected int lineBufferPos;
	/**
	 * Dot Address Determination (expected number of the following digits).
	 */
	protected int expectedDigits;
	/**
	 * Dot Address Determination. Buffered high byte.
	 */
	protected byte digitHighByte;
	/**
	 * Repeat Bit Image Printing. Number of repetitions to print (1..256).
	 */
	protected int repeatN;
	/**
	 * Bit counter of the bit image printing.
	 */
	protected int bitCnt;

	/**
	 * Secondary address.
	 */
	protected int secondary;
	/**
	 * Userport value.
	 */
	protected byte value;
	/**
	 * Strobe signal.
	 */
	protected boolean strobe;

	/**
	 * Paper to print to.
	 */
	protected IPaper paper;

	/**
	 * Create a printer.
	 * 
	 * @param p
	 *            primary device number
	 * @param s
	 *            secondary device number
	 */
	public MPS803(final IECBus bus, final int p, final int s) {
		super(bus);
		this.prnr = p;
		this.secondary = s;
		// set default paper
		setPaper(new ConsolePaper());
		setDeviceEnable(false);
		reset();
	}

	/**
	 * Reset printer.
	 */
	@Override
	public void reset() {
		super.reset();
		state = 0;
		lineBuffer = new boolean[MAX_WIDTH][CHAR_HEIGHT];
		lineBufferPos = 0;
		expectedDigits = 0;
		digitHighByte = 0;
		repeatN = 0;
		bitCnt = 0;
	}

	public void printerUserportWriteStrobe(final boolean s) {
		// strobe hi->lo?
		if (strobe && !s) {
			putc(value);
			// signal lo->hi
			setBusy(true);
		}
		strobe = s;
	}

	public final void printerUserportWriteData(final byte b) {
		value = b;
	}

	/**
	 * Turn on/off printer. The paper is opened or closed.
	 * 
	 * @param on
	 *            true (on), false (off)
	 */
	public void turnPrinterOnOff(final boolean on) {
		if (on) {
			/* Switch printer on. */
			paper.open();
		} else {
			/* Switch printer off. */
			paper.close();
		}
		setDeviceEnable(on);
	}

	/**
	 * Set printer paper output.
	 * 
	 * @param p
	 *            paper to be used
	 */
	public void setPaper(final IPaper p) {
		this.paper = p;
	}

	/**
	 * This method implements the state machine. Various modes can be turned
	 * on/off by sending specific commands to the printer. Additionally
	 * characters (normal or graphical) can be printed here.
	 * 
	 * @param c
	 *            byte code to print or printer command
	 */
	public void putc(final byte c) {
		if (lineBufferPos >= MAX_WIDTH) {
			/*
			 * When the printer uses up more than 480 dots, then it prints out
			 * the line and then stops and tells you that it's READY for more
			 * information.
			 */
			writeLine();
		}

		// Dot Address Determination implementation
		if (expectedDigits != 0) {
			/*
			 * CHR$(27);CHR${16);CHR$(nH);CHR$(nL)
			 */

			// already two digits read?
			if (expectedDigits-- == 1) {
				// decode start position
				int startPos = ((digitHighByte & 0xff) << 8) | (c & 0xff);
				if (startPos >= COLUMN_WIDTH << 3) {
					// When a number greater than 639 is specified, the dot is
					// printed from the beginning of the next line.
					putc(CARRIAGE_RETURN);
				} else {
					lineBufferPos = startPos;
				}
				// end escape sequence
				unsetState(STATE_ESCAPE);
			} else {
				// buffer high byte
				digitHighByte = c;
			}
			return;
		}

		if (isState(STATE_ESCAPE) && c != DOT_ADDRESS_TERMINATION) {
			// there is only one escape sequence available
			// unexpected escape code ends escape sequence
			unsetState(STATE_ESCAPE);
		}
		// Repeat Bit Image Printing
		if (isState(STATE_REPEAT_BIT_IMAGE)) {
			/*
			 * CHR$(8) CHR$(26);CHR$(n);CHR$(Bit Image Data)
			 */
			int n = c & 0xff;
			if (n == 0) {
				// When 0 is specified for "n", it is read as 256.
				n = 256;
			}
			repeatN = n;
			unsetState(STATE_REPEAT_BIT_IMAGE);
			return;
		}

		if (isState(STATE_BIT_IMAGE_PRINTING) && (c & 128) != 0) {
			printBitmask(c);
			return;
		}

		switch (c) {
		case BIT_IMAGE_PRINTING:
			setState(STATE_BIT_IMAGE_PRINTING);
			bitCnt = 0;
			break;

		case LINE_FEED:
			/*
			 * By sending LF Code [CHR$(10)] | to your printer, all data in the
			 * print buffer is printed and the paper is advanced one line.
			 */
			writeLine();
			break;

		case CARRIAGE_RETURN:
			/*
			 * By sending CR Code [CHR$(13)] to your printer, all data in the
			 * print buffer is printed and the paper is advanced one line.
			 */
			writeLine();
			// A Carriage Return turns off REVERSE FIELD and quote mode.
			unsetState(STATE_GRAPHIC);
			break;

		case ENHANCE_ON:
			/*
			 * Enhance ON CHR$(14)
			 */
			setState(STATE_ENHANCE);
			// CHR$(14) and CHR$(15) cancel bit image graphic printing code
			// [CHR$(8)]
			if (isState(STATE_BIT_IMAGE_PRINTING))
				bitmodeOff();
			break;

		case ENHANCE_OFF:
			/*
			 * Enhance OFF CHR$(15)
			 */
			unsetState(STATE_ENHANCE);
			// CHR$(14) and CHR$(15) cancel bit image graphic printing code
			// [CHR$(8)]
			if (isState(STATE_BIT_IMAGE_PRINTING))
				bitmodeOff();
			break;

		case DOT_ADDRESS_TERMINATION:
			/*
			 * CHR$(27);CHR${16);CHR$(nH);CHR$(nL)
			 * 
			 * This code sequence specifies print start position in dot units.
			 * nH and nL are 2-byte binary numbers (0 through 639) which
			 * indicate dots where printing starts.
			 */
			expectedDigits = 2;
			break;

		case BUSINESS_MODE:
			unsetState(STATE_GRAPHIC);
			break;

		case REVERSE_ON:
			/*
			 * Reverse ON CHR$(18)
			 */
			setState(STATE_REVERSE);
			break;

		case REPEAT_BIT_IMAGE:
			/*
			 * CHR$(8) ... CHR$(26);CHR$(n);CHR$(Bit Image Data)
			 * 
			 * This codes sequence specifies the repeated printing of bit image
			 * data, "n" is a binary number (0 through 255} which specifies the
			 * desired number of the printed repetition; followed by one-byte
			 * bit image data to be printing repeatedly
			 */
			setState(STATE_REPEAT_BIT_IMAGE);
			repeatN = 0;
			bitCnt = 0;
			break;

		case ESCAPE:
			/*
			 * Initiate escape sequence.
			 */
			setState(STATE_ESCAPE);
			break;

		case GRAPHIC_MODE:
			/*
			 * Graphic mode (upper-case/graphic characters).
			 */
			setState(STATE_GRAPHIC);
			break;

		case REVERSE_OFF:
			/*
			 * Reverse OFF CHR$(146}
			 */
			unsetState(STATE_REVERSE);
			break;

		default:
			if (isState(STATE_BIT_IMAGE_PRINTING))
				return;

			printCBMChar(c);
		}
	}

	/**
	 * Print a character code according to the current mode.
	 * 
	 * @param rawchar
	 *            character code to print
	 */
	private void printCBMChar(final byte rawchar) {
		int c = rawchar & 0xff;

		if (isState(STATE_GRAPHIC)) {
			// graphic characters start at character code 256 in the char-set
			// ROM
			c += 256;
		}

		for (int y = 0; y < CHAR_HEIGHT; y++) {
			if (isState(STATE_ENHANCE)) {
				/*
				 * All characters following the CHR$(14) are printed
				 * double-width using dot matrix that is 7 dots high and 12 dots
				 * wide.
				 */
				for (int x = 0; lineBufferPos + x * 2 + 1 < 480
						&& x < CHAR_WIDTH; x++) {
					lineBuffer[lineBufferPos + x * 2][y] = getCharsetBit(c, x,
							y);
					lineBuffer[lineBufferPos + x * 2 + 1][y] = getCharsetBit(c,
							x, y);
				}
			} else {
				/*
				 * Your printer normally generates a character using dot matrix
				 * that is 7 dots high and 6 dots wide.
				 */
				for (int x = 0; lineBufferPos + x < 480 && x < CHAR_WIDTH; x++)
					lineBuffer[lineBufferPos + x][y] = getCharsetBit(c, x, y);
			}
		}

		lineBufferPos += isState(STATE_ENHANCE) ? 12 : CHAR_WIDTH;
	}

	/**
	 * Get a bit of the char-set ROM.
	 * 
	 * @param chr
	 *            character code
	 * @param bit
	 *            bit to check
	 * @param row
	 *            row number of the character code
	 * @return bit is set?
	 */
	private boolean getCharsetBit(final int chr, final int bit, final int row) {
		/*
		 * By sending the code [CHR$(18)] to your printer, you have turned on
		 * the REVERSE FIELD mode. This prints white letters on a black
		 * background.
		 */
		boolean reverse = isState(STATE_REVERSE);
		return (MPS803_CHARSET_BIN[chr * CHAR_HEIGHT + row] & (1 << (CHAR_HEIGHT - bit))) != 0 ? !reverse
				: reverse;
	}

	/**
	 * Print buffered line to output device.
	 */
	private void writeLine() {
		for (int y = 0; y < CHAR_HEIGHT; y++) {
			for (int x = 0; x < MAX_WIDTH; x++)
				paper.put(lineBuffer[x][y] ? IPaper.Outputs.OUTPUT_PIXEL_BLACK
						: IPaper.Outputs.OUTPUT_PIXEL_WHITE);
			paper.put(IPaper.Outputs.OUTPUT_NEWLINE);
		}

		// output some missing rows
		if (!isState(STATE_BIT_IMAGE_PRINTING)) {
			/* bitmode: 9 rows/inch (7lines/row * 9rows/inch=63 lines/inch) */
			/* charmode: 6 rows/inch (7lines/row * 6rows/inch=42 lines/inch) */
			/* --> 63lines/inch - 42lines/inch = 21lines/inch missing */
			/* --> 21lines/inch / 9row/inch = 3lines/row missing */
			paper.put(IPaper.Outputs.OUTPUT_NEWLINE);
			paper.put(IPaper.Outputs.OUTPUT_NEWLINE);
			paper.put(IPaper.Outputs.OUTPUT_NEWLINE);
		}

		// clear printed line buffer and rewind position
		for (int x = 0; x < MAX_WIDTH; x++) {
			for (int y = 0; y < CHAR_HEIGHT; y++) {
				lineBuffer[x][y] = false;
			}
		}
		lineBufferPos = 0;
	}

	/**
	 * Bit image printing.
	 * 
	 * @param c
	 *            The bitmask to print.
	 */
	private void printBitmask(final byte c) {
		for (int y = 0; y < CHAR_HEIGHT; y++) {
			lineBuffer[lineBufferPos][y] = (c & (1 << (CHAR_WIDTH - y))) != 0;
		}
		bitCnt++;
		lineBufferPos++;
	}

	/**
	 * Turn off bit image printing (print image bits beforehand).
	 */
	private void bitmodeOff() {
		// respect repetition value set earlier
		for (int i = 0; i < repeatN; i++) {
			for (int x = 0; x < bitCnt; x++) {
				for (int y = 0; y < CHAR_HEIGHT; y++) {
					lineBuffer[lineBufferPos + x][y] = lineBuffer[lineBufferPos
							- bitCnt + x][y];
				}
			}
			lineBufferPos += bitCnt;
		}
		unsetState(STATE_BIT_IMAGE_PRINTING);
	}

	/**
	 * Check printer state, if the state is enabled.
	 * 
	 * @param s
	 *            state to check
	 * @return state is currently enabled?
	 */
	private boolean isState(final int s) {
		return (state & s) != 0;
	}

	/**
	 * Enable printer state.
	 * 
	 * @param s
	 *            state to enable
	 */
	private void setState(final int s) {
		state |= s;
	}

	/**
	 * Disable printer state.
	 * 
	 * @param s
	 *            state to disable
	 */
	private void unsetState(final int s) {
		state &= ~s;
	}

	/**
	 * Signal busy printer.
	 * 
	 * @param flag
	 *            busy flag
	 */
	public abstract void setBusy(final boolean flag);

	private byte status;

	@Override
	public void open(int device, byte secondary) {
		/*
		 * Secondary address sets the character mode globally
		 * 
		 * (SA=0: Graphic Mode, SA=7: Business Mode)
		 */
		if ((secondary & 0x0f) == 0) {
			setState(STATE_GRAPHIC);
		}
		status = 0;
	}

	@Override
	public void close(int device, byte secondary) {
		status = 0;
	}

	@Override
	public void listenTalk(int device, byte secondary) {
		status = 0;
	}

	@Override
	public void unlisten(int device, byte secondary) {
		status = 0;
	}

	@Override
	public void untalk(int device, byte secondary) {
		status = 0;
	}

	@Override
	public byte read(int device, byte secondary) {
		status = 0;
		return 0;
	}

	@Override
	public void write(int device, byte secondary, byte data) {
		/*
		 * Secondary address sets the character mode globally
		 * 
		 * (SA=0: Graphic Mode, SA=7: Business Mode)
		 */
		if ((secondary & 0x0f) == 0) {
			setState(STATE_GRAPHIC);
		}
		putc(data);
		status = 0;
	}

	@Override
	public byte getStatus() {
		return status;
	}

}
