package applet.events;

import java.awt.Component;
import java.io.File;

/**
 * Insert disk, tape, cartridge.
 * 
 * @author Ken
 * 
 */
public interface IInsertMedia extends IEvent {
	enum MediaType {
		DISK, TAPE, CART
	}

	/**
	 * Get Media type to insert.
	 * 
	 * @return media type to insert
	 */
	MediaType getMediaType();

	/**
	 * Media file to insert.
	 * 
	 * @return media file (disk/tape/cartridge image file)
	 */
	File getSelectedMedia();

	/**
	 * Auto start this file contained within the selected media file.
	 * 
	 * @return file to start automatically
	 */
	File getAutostartFile();

	/**
	 * Component requesting the media to insert.
	 * 
	 * @return requesting component
	 */
	Component getComponent();

}
