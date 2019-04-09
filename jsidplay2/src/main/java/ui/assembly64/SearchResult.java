package ui.assembly64;

import org.apache.commons.lang.StringEscapeUtils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchResult {

	private StringProperty idProperty = new SimpleStringProperty();
	private StringProperty nameProperty = new SimpleStringProperty();
	private StringProperty groupProperty = new SimpleStringProperty();
	private StringProperty yearProperty = new SimpleStringProperty();
	private StringProperty handleProperty = new SimpleStringProperty();
	private StringProperty eventProperty = new SimpleStringProperty();
	private StringProperty ratingProperty = new SimpleStringProperty();
	private StringProperty updatedProperty = new SimpleStringProperty();
	private StringProperty releasedProperty = new SimpleStringProperty();
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

	public String getYear() {
		return yearProperty.get();
	}

	public void setYear(String year) {
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

	public String getRating() {
		return ratingProperty.get();
	}

	public void setRating(String rating) {
		ratingProperty.set(rating);
	}

	public String getUpdated() {
		return updatedProperty.get();
	}

	public void setUpdated(String updated) {
		updatedProperty.set(updated);
	}

	public Category getCategory() {
		return categoryProperty.get();
	}

	public void setCategory(Category category) {
		categoryProperty.set(category);
	}

	public String getReleased() {
		return releasedProperty.get();
	}

	public void setReleased(String released) {
		releasedProperty.set(released);
	}
}
