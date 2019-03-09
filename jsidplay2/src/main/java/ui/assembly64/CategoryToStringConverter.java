package ui.assembly64;

import javafx.util.StringConverter;

public class CategoryToStringConverter<T> extends StringConverter<Category> {

	@Override
	public Category fromString(String categoryString) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(Category category) {
		return !category.equals(Category.ALL) ? category.getDescription() : "All Content";
	}

}
