package ui.common;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;

public class TypeTextField extends TextField {

	private static final String NUMBERS = "[\\-0-9]";
	private static final String FLOATS = "[\\-0-9.,]";
	private static final String LOCAL_DATE = "[\\-0-9]";

	private StringProperty type = new SimpleStringProperty();

	public String getType() {
		return type.get();
	}

	public void setType(String type) {
		this.type.set(type);
		setPattern(type);
	}

	private String pattern;

	private void setPattern(String type) {
		if (type.equals("Long") || type.equals("Integer") || type.equals("Short")) {
			pattern = NUMBERS;
		} else if (type.equals("Float")) {
			pattern = FLOATS;
		} else if (type.equals("String")) {
			pattern = null;
		} else if (type.equals("LocalDate")) {
			pattern = LOCAL_DATE;
		} else {
			throw new RuntimeException("Unsupported data type: " + type);
		}
	}

	@Override
	public void replaceText(int start, int end, String text) {
		if (pattern == null || text.isEmpty() || text.matches(pattern)) {
			super.replaceText(start, end, text);
		}
	}

	@Override
	public void replaceSelection(String text) {
		if (pattern == null || text.isEmpty() || text.matches(pattern)) {
			super.replaceSelection(text);
		}
	}

	public Object getValue() {
		if (type.get().equals("Long")) {
			try {
				return Long.parseLong(getText());
			} catch (NumberFormatException e) {
				return Long.valueOf(0L);
			}
		} else if (type.get().equals("Integer")) {
			try {
				return Integer.parseInt(getText());
			} catch (NumberFormatException e) {
				return Integer.valueOf(0);
			}
		} else if (type.get().equals("LocalDate")) {
			try {
				return LocalDate.parse(getText(), DateTimeFormatter.ISO_DATE);
			} catch (Exception e) {
				try {
					return YearMonth.parse(getText());
				} catch (Exception e2) {
					try {
						return Year.parse(getText());
					} catch (Exception e3) {
						return null;
					}
				}
			}
		} else if (type.get().equals("Short")) {
			try {
				return Short.parseShort(getText());
			} catch (NumberFormatException e) {
				return Short.valueOf((short) 0);
			}
		} else if (type.get().equals("Float")) {
			try {
				return Float.parseFloat(getText());
			} catch (NumberFormatException e) {
				return Float.valueOf(0.f);
			}
		}
		return getText();
	}

	public void setValue(Object value) {
		if (type.get().equals("Long")) {
			setText(String.valueOf(value));
		} else if (type.get().equals("Integer")) {
			setText(String.valueOf(value));
		} else if (type.get().equals("LocalDate")) {
			if (value instanceof LocalDate) {
				setText(String.valueOf(value));
			} else if (value instanceof YearMonth) {
				setText(String.valueOf(value));
			} else if (value instanceof Year) {
				setText(String.valueOf(value));
			}
		} else if (type.get().equals("Short")) {
			setText(String.valueOf(value));
		} else if (type.get().equals("Float")) {
			setText(String.valueOf(value));
		}
		setText(value.toString());
	}
}
