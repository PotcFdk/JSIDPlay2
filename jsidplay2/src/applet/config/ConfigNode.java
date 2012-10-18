package applet.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.tree.DefaultMutableTreeNode;

public class ConfigNode extends DefaultMutableTreeNode {
	private Object object;

	public ConfigNode(Object object, Object data) {
		super(data);
		this.object = object;
	}

	@Override
	public String toString() {
		if (getUserObject() instanceof Field) {
			Field field = (Field) getUserObject();
			try {
				String name = field.getName();
				String methodName = Character.toUpperCase(name.charAt(0))
						+ name.substring(1);
				String prefix;
				if (field.getType().getName().equals("boolean")
						|| (field.getType().getPackage() != null && field
								.getType().getPackage().getName()
								.equals("java.lang.Boolean"))) {
					prefix = "is";
				} else {
					prefix = "get";
				}
				Method method = object.getClass()
						.getMethod(prefix + methodName);
				return field.getName() + "-" + method.invoke(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (getUserObject() instanceof Method) {
			Method method = (Method) getUserObject();
			return method.getName().substring(3);
		}
		return getUserObject().getClass().getSimpleName();
	}

}
