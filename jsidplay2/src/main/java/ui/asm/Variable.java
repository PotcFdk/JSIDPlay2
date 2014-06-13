package ui.asm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Variable {
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty value = new SimpleStringProperty();

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public String getValue() {
		return value.get();
	}

	public void setValue(String value) {
		this.value.set(value);
	}
}
