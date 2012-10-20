package applet.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

	public Object getValue(Field field) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		String name = field.getName();
		String methodName = Character.toUpperCase(name.charAt(0))
				+ name.substring(1);
		String prefix = getMethodPrefix(field);
		Method method = object.getClass().getMethod(prefix + methodName);
		return method.invoke(object);
	}

	private String getMethodPrefix(Field field) {
		String prefix;
		if (field.getType().getName().equals("boolean")
				|| (field.getType().getPackage() != null && field.getType()
						.getPackage().getName().equals("java.lang.Boolean"))) {
			prefix = "is";
		} else {
			prefix = "get";
		}
		return prefix;
	}

	@Override
	public String toString() {
		if (getUserObject() instanceof Field) {
			Field field = (Field) getUserObject();
			try {
				Object value = getValue(field);
				return swixml.getLocalizer().getString(field.getName()) + "="
						+ value;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (getUserObject() instanceof Method) {
			Method method = (Method) getUserObject();
			return swixml.getLocalizer().getString(method.getName());
		}
		return swixml.getLocalizer().getString(
				getUserObject().getClass().getSimpleName());
	}

}
