package ui.common;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ToggleGroup;

public class BindingUtils {

	private BindingUtils() {
	}

	public static <T> void bindBidirectionalThreadSafe(final ObjectProperty<T> fxProperty,
			final ObjectProperty<T> configProperty) {
		fxProperty.addListener((obj, o, n) -> configProperty.setValue(fxProperty.get()));
		configProperty.addListener((obj, o, n) -> Platform.runLater(() -> fxProperty.setValue(n)));
		fxProperty.setValue(configProperty.get());
	}

	public static <T extends Enum<T>> void bindBidirectional(final ToggleGroup toggleGroup,
			final ObjectProperty<T> property, final Class<T> clz) {
		// Select initial toggle for current property state
		toggleGroup.getToggles().stream().peek(toggle -> {
			if (toggle.getUserData() == null) {
				throw new IllegalArgumentException("The ToggleGroup contains at least one Toggle without user data!");
			}
		}).filter(
				toggle -> Objects.equals(property.getValue(), Enum.valueOf(clz, String.valueOf(toggle.getUserData()))))
				.forEach(toggleGroup::selectToggle);

		// Update property value on toggle selection changes
		toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> property
				.setValue(Enum.valueOf(clz, String.valueOf(newValue.getUserData()))));

		// Update toggle selection on property value changes
		property.addListener((obj, o, n) -> toggleGroup.getToggles().stream()
				.filter(toggle -> Objects.equals(n, Enum.valueOf(clz, String.valueOf(toggle.getUserData()))))
				.forEach(toggle -> toggle.setSelected(true)));
	}

}
