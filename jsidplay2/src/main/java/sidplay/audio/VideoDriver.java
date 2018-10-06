package sidplay.audio;

import java.util.function.Consumer;

import libsidplay.common.CPUClock;

public interface VideoDriver extends AudioDriver, Consumer<int[]> {

	/**
	 * Propagates VIC pixel data for video drivers.
	 * 
	 * <B>Note:</B> Pixel format is BGRA and is updated frequently at a rate of
	 * screen refresh rate. {@link CPUClock#getScreenRefresh()}
	 */
	void accept(int[] bgraData);

}
