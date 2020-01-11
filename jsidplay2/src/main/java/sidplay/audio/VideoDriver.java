package sidplay.audio;

import java.util.function.BiConsumer;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;

public interface VideoDriver extends BiConsumer<VIC, int[]> {

	/**
	 * Propagates VIC pixel data for video drivers.
	 * 
	 * <B>Note:</B> Pixel format is ARGB and is updated frequently at a rate of
	 * screen refresh rate. {@link CPUClock#getScreenRefresh()}
	 */
	void accept(VIC vic, int[] pixels);

}
