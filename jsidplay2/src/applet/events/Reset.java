package applet.events;

import java.io.File;

/**
 * Reset C64 is almost the same as playing a tune using a null tune to play.
 * 
 * @author Ken
 * 
 */
public abstract class Reset implements IPlayTune {
	/**
	 * Tune is always null on a reset.
	 */
	@Override
	public final File getFile() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean switchToVideoTab() {
		return false;
	}

	/**
	 * Command to type after reset. This can be used for example to auto-load
	 * demos from a disk.
	 * 
	 * @return command to type
	 */
	public String getCommand() {
		return null;
	}
}
