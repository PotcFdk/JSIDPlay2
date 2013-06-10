package ui.events;

/**
 * UI event used for notification.
 * 
 * @author Ken
 * 
 */
public class UIEvent {

	/**
	 * UI event, that occurred.
	 */
	private IEvent uiEventImpl;
	/**
	 * Class of the UI event.
	 */
	private Class<? extends IEvent> ifaceType;

	/**
	 * Create a new UI event.
	 * 
	 * @param iface
	 *            UI event
	 * @param ifaceName
	 *            UI event class
	 */
	public UIEvent(final IEvent iface, final Class<? extends IEvent> ifaceType) {
		this.uiEventImpl = iface;
		this.ifaceType = ifaceType;
	}

	/**
	 * Get UI event that occurred.
	 * 
	 * @return UI event that occurred
	 */
	public IEvent getUIEventImpl() {
		return this.uiEventImpl;
	}

	/**
	 * Check this UI event, if it is of the desired type
	 * 
	 * @param cls
	 *            UI event class name to check
	 * @return is this event of the desired type?
	 */
	public boolean isOfType(final Class<? extends IEvent> cls) {
		return cls.isAssignableFrom(ifaceType);
	}
}
