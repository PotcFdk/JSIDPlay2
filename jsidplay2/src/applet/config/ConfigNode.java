package applet.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.tree.DefaultMutableTreeNode;

import org.swixml.SwingEngine;

public class ConfigNode extends DefaultMutableTreeNode {
	private Object object;
	private SwingEngine swixml;

	public ConfigNode(SwingEngine swixml, Object object, Object data) {
		super(data);
		this.swixml = swixml;
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public <T> void setValue(T text) {
		Field field = (Field) getUserObject();
		String name = field.getName();
		String methodName = Character.toUpperCase(name.charAt(0))
				+ name.substring(1);
		String prefix = getMethodPrefix(field, false);
		try {
			Method method = object.getClass().getMethod(prefix + methodName,
					field.getType());
			method.invoke(object, text);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new RuntimeException(String.format(
					"Could not set Value: %s for field %s", text,
					field.getName()));
		}
	}

	public Object getValue() {
		Field field = (Field) getUserObject();
		String name = field.getName();
		String methodName = Character.toUpperCase(name.charAt(0))
				+ name.substring(1);
		String prefix = getMethodPrefix(field, true);
		try {
			Method method = object.getClass().getMethod(prefix + methodName);
			return method.invoke(object);
		} catch (Exception e) {
			return null;
		}
	}

	private String getMethodPrefix(Field field, boolean isGetter) {
		String prefix;
		if (field.getType().getName().equals("boolean")
				|| (field.getType().getPackage() != null && field.getType()
						.getPackage().getName().equals("java.lang.Boolean"))) {
			if (isGetter) {
				prefix = "is";
			} else {
				prefix = "set";
			}
		} else {
			if (isGetter) {
				prefix = "get";
			} else {
				prefix = "set";
			}
		}
		return prefix;
	}

	@Override
	public String toString() {
		if (getUserObject() instanceof Field) {
			Field field = (Field) getUserObject();
			return swixml.getLocalizer().getString(field.getName()) + "="
					+ getValue();
		} else if (getUserObject() instanceof Method) {
			Method method = (Method) getUserObject();
			return swixml.getLocalizer().getString(
					getObject().getClass().getSimpleName() + "_"
							+ method.getName());
		} else if (getUserObject() instanceof String) {
			return (String) getUserObject();
		} else {
			return swixml.getLocalizer().getString(
					getUserObject().getClass().getSimpleName());
		}
	}

}
