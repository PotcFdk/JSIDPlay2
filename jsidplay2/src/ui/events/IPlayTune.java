package ui.events;

import libsidplay.sidtune.SidTune;

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
	 * @return component, that requests the tune
	 */
	Object getComponent();

	/**
	 * Tune to play.
	 * 
	 * @return tune
	 */
	SidTune getSidTune();
}
