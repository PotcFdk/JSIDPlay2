package ui.entities.config;

public enum Assembly64ColumnType {
	NAME("name", 300), GROUP("group", 150), YEAR("year", 150), HANDLE("handle", 150), EVENT("event", 150),
	RATING("rating", 150), CATEGORY("category", 150), UPDATED("updated", 150);

	private String columnProperty;
	private double defaultWidth;

	private Assembly64ColumnType(String columnProperty, double defaultWidth) {
		this.columnProperty = columnProperty;
		this.defaultWidth = defaultWidth;
	}

	public String getColumnProperty() {
		return columnProperty;
	}

	public double getDefaultWidth() {
		return defaultWidth;
	}
}