package ui.common.properties;

import java.util.function.Function;

import javafx.beans.value.WritableValue;

/**
 * Lazy initialized shadowed property. A property is stored as a single value as
 * long as no property is requested.
 * 
 * https://www.dummies.com/programming/java/creating-properties-more-efficiently-in-javafx/
 *
 * @author khaendel
 *
 * @param <P> JavaFX object property class
 * @param <V> JavaFX object property value class
 */
public class ShadowField<P extends WritableValue<V>, V> {

	private Function<V, P> propertyCreator;

	private V _shadowValue;

	private P property;

	public ShadowField(Function<V, P> propertyCreator, V initialValue) {
		this.propertyCreator = propertyCreator;
		_shadowValue = initialValue;
	}

	public final V get() {
		return property == null ? _shadowValue : property.getValue();
	}

	public final void set(V value) {
		if (property == null) {
			_shadowValue = value;
		} else {
			property.setValue(value);
		}
	}

	public P property() {
		if (property == null) {
			property = propertyCreator.apply(_shadowValue);
		}
		return property;
	}

	@Override
	public String toString() {
		return String.valueOf(get());
	}
}
