package applet.editors;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.swixml.SwingEngine;

public class EditorUtils {

	private SwingEngine swix;

	public EditorUtils(Object client) {
		swix = new SwingEngine(client);
		swix.getTaglib().registerTag("shorttextfield", ShortTextField.class);
		swix.getTaglib().registerTag("inttextfield", IntTextField.class);
		swix.getTaglib().registerTag("longtextfield", LongTextField.class);
		swix.getTaglib().registerTag("floattextfield", FloatTextField.class);
		swix.getTaglib().registerTag("chartextfield", CharTextField.class);
	}

	@SuppressWarnings("rawtypes")
	public JComponent render(Class<?> type) {
		JComponent editor = createEditor(getEditorNameForType(type));
		if (type != null && Enum.class.isAssignableFrom(type)) {
			addEnumConstantsToComboBox(
					(JComboBox) swix.getIdMap().get("combo"), type);
		}
		return editor;
	}

	private String getEditorNameForType(Class<?> fieldType) {
		if (fieldType == null) {
			return "NoEditor";
		}
		if (fieldType == String.class) {
			return String.class.getSimpleName();
		} else if (fieldType == Short.class || fieldType == short.class) {
			return Short.class.getSimpleName();
		} else if (fieldType == Integer.class || fieldType == int.class) {
			return Integer.class.getSimpleName();
		} else if (fieldType == Long.class || fieldType == long.class) {
			return Long.class.getSimpleName();
		} else if (fieldType == Boolean.class || fieldType == boolean.class) {
			return Boolean.class.getSimpleName();
		} else if (Enum.class.isAssignableFrom(fieldType)) {
			return Enum.class.getSimpleName();
		} else if (fieldType == Float.class || fieldType == float.class) {
			return Float.class.getSimpleName();
		} else if (fieldType == Character.class || fieldType == char.class) {
			return Character.class.getSimpleName();
		} else if (fieldType == File.class) {
			return File.class.getSimpleName();
		} else if (fieldType == Date.class) {
			return "Year";
		} else {
			throw new RuntimeException("unsupported type: "
					+ fieldType.getSimpleName());
		}
	}

	private JComponent createEditor(String editorName) {
		try {
			return (JComponent) swix.render(EditorUtils.class
					.getResource(editorName + ".xml"));
		} catch (Exception e) {
			throw new RuntimeException("Undefined editor: " + editorName
					+ ".xml");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JComponent addEnumConstantsToComboBox(JComboBox combo, Class type) {
		ActionListener[] actionListeners = combo.getActionListeners();
		for (ActionListener actionListener : actionListeners) {
			combo.removeActionListener(actionListener);
		}
		combo.addItem(null);
		Class<? extends Enum> en = type;
		for (Enum val : en.getEnumConstants()) {
			combo.addItem(val);
		}
		for (ActionListener actionListener : actionListeners) {
			combo.addActionListener(actionListener);
		}
		return combo;
	}
}
