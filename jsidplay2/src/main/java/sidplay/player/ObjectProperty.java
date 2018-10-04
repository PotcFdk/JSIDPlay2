package sidplay.player;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ObjectProperty<T> {

	private final String name;
	private final PropertyChangeSupport propertyChangeSupport;
	private T value;

	public ObjectProperty(String name, T initialValue) {
		this.name = name;
		this.value = initialValue;
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		propertyChangeSupport.firePropertyChange(name, this.value, value);
		this.value = value;
	}

	public void addListener(PropertyChangeListener propertyChangeListener) {
		propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
	}

	public void removeListener(PropertyChangeListener propertyChangeListener) {
		propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
	}
}
