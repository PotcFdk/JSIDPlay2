package ui.assembly64;

import javafx.util.StringConverter;

public class ZeroContainingRatingConverter extends StringConverter<Integer> {

	@Override
	public Integer fromString(String rating) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public String toString(Integer rating) {
		if (rating == 0) {
			return "All Content";
		}
		return String.valueOf(rating);
	}

}
