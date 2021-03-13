package ui.assembly64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

	public static final Category ALL = new Category() {
		@Override
		public Integer getId() {
			return Integer.valueOf(-1);
		}
	};

	private IntegerProperty idProperty = new SimpleIntegerProperty();
	private StringProperty nameProperty = new SimpleStringProperty();
	private StringProperty typeProperty = new SimpleStringProperty();
	private StringProperty descriptionProperty = new SimpleStringProperty();

	public Category() {
	}

	public Integer getId() {
		return idProperty.get();
	}

	public void setId(Integer id) {
		idProperty.set(id);
	}

	public String getName() {
		return nameProperty.get();
	}

	public void setName(String name) {
		nameProperty.set(name);
	}

	public String getType() {
		return typeProperty.get();
	}

	public void setType(String type) {
		typeProperty.set(type);
	}

	public String getDescription() {
		return descriptionProperty.get();
	}

	public void setDescription(String description) {
		descriptionProperty.set(description);
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
