package applet.events;

/**
 * Ask for SID filter change.
 * 
 * @author Ken
 * 
 */
public interface IChangeFilter extends IEvent {
	/**
	 * Get filter name to change to.
	 * 
	 * @return
	 */
	String getFilterName();
}
