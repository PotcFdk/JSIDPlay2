package applet.events;

/**
 * Listener to notify for UI events.
 * 
 * @author Ken
 * 
 */
public interface UIEventListener {

	/**
	 * Notify this listener for UI events.
	 * 
	 * @param evt
	 *            UI event
	 */
	void notify(final UIEvent evt);

}
