package ui.assembly64;

public enum Age {
	ALL(-1), ONE_DAY(1), TWO_DAYS(2), FOUR_DAYS(4), ONE_WEEK(7), TWO_WEEKS(14), THREE_WEEKS(21), ONE_MONTH(31),
	TWO_MONTH(62);

	private int days;

	private Age(int days) {
		this.days = days;
	}

	public int getDays() {
		return days;
	}
}
