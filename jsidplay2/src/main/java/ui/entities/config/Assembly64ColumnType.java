package ui.entities.config;

public enum Assembly64ColumnType {
	NAME("name", String.class, 300), GROUP("group", String.class, 150), YEAR("year", Integer.class, 150),
	HANDLE("handle", String.class, 150), EVENT("event", String.class, 150), RATING("rating", Integer.class, 150),
	CATEGORY("category", String.class, 150), UPDATED("updated", String.class, 150),
	RELEASED("released", String.class, 150);

	private String columnProperty;
	private Class<?> columnClass;
	private double defaultWidth;

	private Assembly64ColumnType(String columnProperty, Class<?> columnClass, double defaultWidth) {
		this.columnProperty = columnProperty;
		this.defaultWidth = defaultWidth;
		this.columnClass = columnClass;
	}

	public String getColumnProperty() {
		return columnProperty;
	}

	public Class<?> getColumnClass() {
		return columnClass;
	}

	public double getDefaultWidth() {
		return defaultWidth;
	}
}