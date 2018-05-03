package ui.musiccollection;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TuneInfo {
	private StringProperty nameProperty = new SimpleStringProperty();
	private StringProperty valueProperty = new SimpleStringProperty();

	public TuneInfo(String name, String value) {
		setName(name);
		setValue(value);
	}

	public String getName() {
		return nameProperty.get();
	}

	public void setName(String name) {
		this.nameProperty.set(name);
	}

	public String getValue() {
		return valueProperty.get();
	}

	public void setValue(String value) {
		this.valueProperty.set(value);
	}
}
