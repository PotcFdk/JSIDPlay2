package applet.events;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the UI event factory. This class is a single instance.
 * 
 * @author Ken
 * 
 */
public class UIEventFactory {
	/**
	 * Single instance.
	 */
	private static UIEventFactory theInstance;
	/**
	 * Listeners to notify
	 */
	private Set<UIEventListener> listeners;
	/**
	 * Recursion prevention contains currently fired event classes.
	 */
	private Set<Class<? extends IEvent>> currentlyFired = new HashSet<Class<? extends IEvent>>();

	/**
	 * Get an instance.
	 * 
	 * @return single instance
	 */
	public static UIEventFactory getInstance() {
		if (theInstance == null) {
			theInstance = new UIEventFactory();
		}
		return theInstance;
	}

	/**
	 * Private constructor.
	 */
	private UIEventFactory() {
		listeners = new HashSet<UIEventListener>();
	}

	/**
	 * Add listener to notify for UI events.
	 * 
	 * @param listener
	 *            listener to notify
	 */
	public void addListener(final UIEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove listener.
	 * 
	 * @param listener
	 *            listener to remove
	 */
	public void removeListener(final UIEventListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners for the specified UI event.
	 * 
	 * @param ifaceType
	 *            event class
	 * @param iface
	 *            an instance of the UI event
	 */
	public void fireEvent(final Class<? extends IEvent> ifaceType,
			final IEvent iface) {

		if (currentlyFired.contains(ifaceType)) {
			return;
		}
		currentlyFired.add(ifaceType);

		final UIEvent uiEvent = new UIEvent(iface, ifaceType);
		// Create a copy to avoid ConcurrentModificationException!
		final UIEventListener[] copied = listeners
				.toArray(new UIEventListener[listeners.size()]);
		for (UIEventListener listener : copied) {
			listener.notify(uiEvent);
		}
		currentlyFired.remove(ifaceType);
	}

}
