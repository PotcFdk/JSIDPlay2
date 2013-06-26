package ui.common;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;

public class TypeTextField extends TextField {

	private static final String NUMBERS = "[0-9]";
	private static final String FLOATS = "[0-9.]";

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
		if (type.equals("Long") || type.equals("Integer")
				|| type.equals("Date") || type.equals("Short")) {
			pattern = NUMBERS;
		} else if (type.equals("Float")) {
			pattern = FLOATS;
		} else if (type.equals("String")) {
			pattern = null;
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
				return new Long(0L);
			}
		} else if (type.get().equals("Integer") || type.get().equals("Date")) {
			try {
				return Integer.parseInt(getText());
			} catch (NumberFormatException e) {
				return new Integer(0);
			}
		} else if (type.get().equals("Short")) {
			try {
				return Short.parseShort(getText());
			} catch (NumberFormatException e) {
				return new Short((short) 0);
			}
		} else if (type.get().equals("Float")) {
			try {
				return Float.parseFloat(getText());
			} catch (NumberFormatException e) {
				return new Float(0.f);
			}
		}
		return getText();
	}

	public void setValue(Object value) {
		if (type.get().equals("Long")) {
			setText(String.valueOf((Long) value));
		} else if (type.get().equals("Integer") || type.get().equals("Date")) {
			setText(String.valueOf((Integer) value));
		} else if (type.get().equals("Short")) {
			setText(String.valueOf((Short) value));
		} else if (type.get().equals("Float")) {
			setText(String.valueOf((Float) value));
		}
		setText(value.toString());
	}
}
