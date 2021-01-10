package ui.common.properties;

import java.util.function.Function;

import javafx.beans.value.WritableValue;

/**
 * https://www.dummies.com/programming/java/creating-properties-more-efficiently-in-javafx/
 *
 * @author khaendel
 *
 * @param <P> JavaFX object property class
 * @param <O> JavaFX object property value class
 */
public class ShadowField<P extends WritableValue<O>, O> {

	private O _shadowFieldValue;

	private P property;

	private Function<O, P> propertyCreator;

	public ShadowField(O initialValue, Function<O, P> propertyCreator) {
		_shadowFieldValue = initialValue;
		this.propertyCreator = propertyCreator;
	}

	public final O get() {
		return property == null ? _shadowFieldValue : property.getValue();
	}

	public final void set(O value) {
		if (property == null) {
			_shadowFieldValue = value;
		} else {
			property.setValue(value);
		}
	}

	public P property() {
		if (property == null) {
			property = propertyCreator.apply(_shadowFieldValue);
		}
		return property;
	}

}
