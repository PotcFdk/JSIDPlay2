package applet.events;

import java.net.URL;

/**
 * Request a web browser to jump to a URL.
 * 
 * @author Ken
 * 
 */
public interface IGotoURL extends IEvent {
	/**
	 * URL to jump to.
	 * 
	 * @return URL target
	 */
	URL getCollectionURL();
}
