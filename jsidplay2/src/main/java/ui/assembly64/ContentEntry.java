package ui.assembly64;

import org.eclipse.jetty.util.URIUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ContentEntry {

	private StringProperty idProperty = new SimpleStringProperty();
	private StringProperty nameProperty = new SimpleStringProperty();

	public ContentEntry() {
	}
	
	public String getId() {
		return idProperty.get();
	}

	public void setId(String id) {
		idProperty.set(id);
	}

	public String getName() {
		return nameProperty.get();
	}

	public void setName(String name) {
		nameProperty.set(name);
	}
	
	public String getDecodedName() {
		return URIUtil.decodePath(getName());
	}

}
