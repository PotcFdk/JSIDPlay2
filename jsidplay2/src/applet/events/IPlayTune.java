package applet.events;

import java.awt.Component;
import java.io.File;

/**
 * A tune should be played.
 * 
 * @author Ken
 * 
 */
public interface IPlayTune extends IEvent {
	/**
	 * Switch to the Video tab.
	 * 
	 * @return switch to Video screen?
	 */
	boolean switchToVideoTab();

	/**
	 * Tune to play.
	 * 
	 * @return tune file
	 */
	File getFile();

	/**
	 * @return component, that requests the tune
	 */
	Component getComponent();
}
