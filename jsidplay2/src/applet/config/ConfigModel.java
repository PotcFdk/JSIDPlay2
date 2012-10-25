package applet.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;

public class ConfigModel extends DefaultTreeModel {

	private SwingEngine swixml;

	public ConfigModel() {
		super(null);
	}

	@Override
	public boolean isLeaf(Object node) {
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
		if (treeNode.getUserObject() instanceof Method) {
			return false;
		} else if (treeNode.getUserObject() instanceof Field) {
			return true;
		} else if (treeNode.getUserObject().getClass().isPrimitive()
				|| treeNode.getUserObject().getClass().getPackage().getName()
						.startsWith("java.lang")) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int getChildCount(Object parent) {
		ConfigNode treeNode = (ConfigNode) parent;
		try {
			if (treeNode.getUserObject() instanceof Method) {
				Method method = (Method) treeNode.getUserObject();
				Object methodObject = treeNode.getObject();
				Object object = method.invoke(methodObject);
				if (object instanceof List) {
					List list = (List) object;
					return list.size();
				}
				return object.getClass().getDeclaredFields().length;
			} else {
				Object obj = treeNode.getUserObject();
				Field[] declaredFields = obj.getClass().getDeclaredFields();
				int fieldCount = 0;
				for (Field field : declaredFields) {
					if (isIgnorableField(field)) {
						continue;
					}
					fieldCount++;
				}
				return fieldCount;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getChild(Object parent, int index) {
		ConfigNode treeNode = (ConfigNode) parent;
		try {
			if (treeNode.getUserObject() instanceof Method) {
				Method method = (Method) treeNode.getUserObject();
				Object methodObject = treeNode.getObject();
				Object object = method.invoke(methodObject);
				if (object instanceof List) {
					List list = (List) object;
					return new ConfigNode(swixml, object, list.get(index));
				}
				return new ConfigNode(swixml, object, object.getClass()
						.getDeclaredFields()[index]);
			} else {
				Object obj = treeNode.getUserObject();
				Field[] fields = obj.getClass().getDeclaredFields();
				Object[] childs = new Object[fields.length];
				int fieldCount = 0;
				for (Field field : fields) {
					if (isIgnorableField(field)) {
						continue;
					}
					if (field.getType().isPrimitive()
							|| field.getType().getPackage().getName()
									.startsWith("java.lang")) {
						childs[fieldCount++] = field;
					} else {
						String name = field.getName();
						String methodName = Character.toUpperCase(name
								.charAt(0)) + name.substring(1);
						Method method = obj.getClass().getMethod(
								"get" + methodName);
						childs[fieldCount++] = method;
					}
				}
				return new ConfigNode(swixml, obj, childs[index]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return super.getIndexOfChild(parent, child);
	}

	public void setRootUserObject(SwingEngine swixml, IConfig config) {
		this.swixml = swixml;
		setRoot(new ConfigNode(swixml, null, config));
	}

	private boolean isIgnorableField(Field field) {
		return field.getName().equals("dbConfig")
				|| field.getName().equals("id");
	}

}
