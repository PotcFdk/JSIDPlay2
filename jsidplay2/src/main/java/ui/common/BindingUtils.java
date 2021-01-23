package ui.common;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ToggleGroup;

public class BindingUtils {

	private BindingUtils() {
	}

	public static <T> void bindBidirectionalThreadSafe(final ObjectProperty<T> objectProperty,
			final ObjectProperty<T> configProperty) {

		objectProperty.addListener((obj, o, n) -> configProperty.setValue(objectProperty.get()));

		configProperty.addListener((obj, o, n) -> Platform.runLater(() -> objectProperty.setValue(n)));

		objectProperty.setValue(configProperty.get());
	}

	public static <T extends Enum<T>> void bindBidirectional(final ToggleGroup toggleGroup,
			final ObjectProperty<T> configProperty, final Class<T> clz) {

		toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> configProperty
				.setValue(Enum.valueOf(clz, String.valueOf(newValue.getUserData()))));

		configProperty.addListener((obj, o, n) -> toggleGroup.getToggles().stream()
				.filter(toggle -> Objects.equals(n, Enum.valueOf(clz, String.valueOf(toggle.getUserData()))))
				.forEach(toggle -> toggle.setSelected(true)));

		toggleGroup.getToggles().stream()
				.peek(toggle -> Objects.requireNonNull(toggle.getUserData(),
						"The ToggleGroup contains at least one Toggle without user data!"))
				.filter(toggle -> Objects.equals(configProperty.getValue(),
						Enum.valueOf(clz, String.valueOf(toggle.getUserData()))))
				.forEach(toggleGroup::selectToggle);
	}

	public static void bindBidirectional(final ToggleGroup toggleGroup, final BooleanProperty configProperty) {

		toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> configProperty
				.setValue(Boolean.TRUE.equals(Boolean.valueOf(String.valueOf(newValue.getUserData())))));

		configProperty.addListener((obj, o, n) -> toggleGroup.getToggles().stream()
				.filter(toggle -> Objects.equals(n, Boolean.valueOf(String.valueOf(toggle.getUserData()))))
				.forEach(toggle -> toggle.setSelected(true)));

		toggleGroup.getToggles().stream()
				.peek(toggle -> Objects.requireNonNull(toggle.getUserData(),
						"The ToggleGroup contains at least one Toggle without user data!"))
				.filter(toggle -> Objects.equals(configProperty.getValue(),
						Boolean.valueOf(String.valueOf(toggle.getUserData()))))
				.forEach(toggleGroup::selectToggle);
	}

}
