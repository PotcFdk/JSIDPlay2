package ui.events;

/**
 * Progress bar support.
 * 
 * @author Ken
 * 
 */
public interface IMadeProgress extends IEvent {
	/**
	 * Inform about the current progress (0-100)
	 * 
	 * @return progress as percentage
	 */
	int getPercentage();
}
