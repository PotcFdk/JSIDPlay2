package ui.assembly64;

import java.time.LocalDate;

import org.apache.commons.lang.StringEscapeUtils;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchResult {

	private StringProperty idProperty = new SimpleStringProperty();
	private StringProperty nameProperty = new SimpleStringProperty();
	private StringProperty groupProperty = new SimpleStringProperty();
	private IntegerProperty yearProperty = new SimpleIntegerProperty();
	private StringProperty handleProperty = new SimpleStringProperty();
	private StringProperty eventProperty = new SimpleStringProperty();
	private IntegerProperty ratingProperty = new SimpleIntegerProperty();
	private ObjectProperty<LocalDate> updatedProperty = new SimpleObjectProperty<LocalDate>();
	private ObjectProperty<LocalDate> releasedProperty = new SimpleObjectProperty<LocalDate>();
	private ObjectProperty<Category> categoryProperty = new SimpleObjectProperty<Category>();

	public SearchResult() {
	}

	public String getId() {
		return idProperty.get();
	}

	public void setId(String id) {
		idProperty.set(id);
	}

	public String getName() {
		return StringEscapeUtils.unescapeHtml(nameProperty.get());
	}

	public void setName(String name) {
		nameProperty.set(name);
	}

	public String getGroup() {
		return groupProperty.get();
	}

	public void setGroup(String group) {
		groupProperty.set(group);
	}

	public Integer getYear() {
		return yearProperty.get();
	}

	public void setYear(Integer year) {
		yearProperty.set(year);
	}

	public String getHandle() {
		return handleProperty.get();
	}

	public void setHandle(String handle) {
		handleProperty.set(handle);
	}

	public String getEvent() {
		return eventProperty.get();
	}

	public void setEvent(String event) {
		eventProperty.set(event);
	}

	public Integer getRating() {
		return ratingProperty.get();
	}

	public void setRating(Integer rating) {
		ratingProperty.set(rating);
	}

	public LocalDate getUpdated() {
		return updatedProperty.get();
	}

	public void setUpdated(LocalDate updated) {
		updatedProperty.set(updated);
	}

	public Category getCategory() {
		return categoryProperty.get();
	}

	public void setCategory(Category category) {
		categoryProperty.set(category);
	}

	public LocalDate getReleased() {
		return releasedProperty.get();
	}

	public void setReleased(LocalDate released) {
		releasedProperty.set(released);
	}
}
