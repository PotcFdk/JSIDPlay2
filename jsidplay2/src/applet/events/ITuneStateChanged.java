package applet.events;

import java.io.File;

/**
 * The state of a tune changed
 * 
 * @author Ken H�ndel
 * 
 */
public interface ITuneStateChanged extends IEvent {

	/**
	 * @return the tune of the state change
	 */
	File getTune();

	/**
	 * @return Natural finished?
	 */
	boolean naturalFinished();

}
