package libsidplay.components.printer;

public interface IPaper {
	public enum Outputs {
		/**
		 * Output a newline.
		 */
		OUTPUT_NEWLINE,
		/**
		 * Output a black pixel.
		 */
		OUTPUT_PIXEL_BLACK,
		/**
		 * Output a white pixel.
		 */
		OUTPUT_PIXEL_WHITE
	}

	void open();

	void put(Outputs data);

	void close();
}
