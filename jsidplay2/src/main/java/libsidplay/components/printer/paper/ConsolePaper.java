package libsidplay.components.printer.paper;

import libsidplay.components.printer.IPaper;

public class ConsolePaper implements IPaper {
	/**
	 * Print printer output to console.
	 * 
	 * @param out
	 *            output type to print
	 */
	public final void put(final Outputs out) {
		switch (out) {
		case OUTPUT_NEWLINE:
			// newline
			System.out.print('\n');
			break;
		case OUTPUT_PIXEL_BLACK:
			// black pixel
			System.out.print('*');
			break;
		default:
			// white pixel
			System.out.print(' ');
			break;
		}
	}

	/**
	 * Nothing to do.
	 */
	public final void open() {
	}

	/**
	 * Nothing to do.
	 */
	public final void close() {
	}

}
