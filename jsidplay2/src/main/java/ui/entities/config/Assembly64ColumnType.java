package ui.entities.config;

public enum Assembly64ColumnType {
	NAME(300), GROUP(150), YEAR(150), HANDLE(150), EVENT(150), RATING(150), CATEGORY(150), UPDATED(150), RELEASED(150);

	private double defaultWidth;

	private Assembly64ColumnType(double defaultWidth) {
		this.defaultWidth = defaultWidth;
	}

	public final double getDefaultWidth() {
		return defaultWidth;
	}

}