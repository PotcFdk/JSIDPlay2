package applet.events;

import java.io.File;

public interface ICollectionChanged extends IEvent {
	enum CollectionType {
		HVSC, CGSC, DEMOS, HVMEC, MAGS
	}
	
	CollectionType getColectionType();
	File getCollectionRoot();
}
