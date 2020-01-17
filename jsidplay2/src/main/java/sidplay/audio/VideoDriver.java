package sidplay.audio;

import java.util.function.Consumer;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;

public interface VideoDriver extends Consumer<VIC> {

	/**
	 * Propagates VIC pixel data for video drivers.<BR>
	 * Pixels can be accessed using {@link VIC#getPixels()}
	 * 
	 * <B>Note:</B> Pixel format is ARGB and is updated frequently at a rate of
	 * screen refresh rate. {@link CPUClock#getScreenRefresh()}
	 */
	void accept(VIC vic);

}
