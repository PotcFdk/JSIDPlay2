package ui.common.properties;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ToggleGroup;

/**
 * Additional binding utilities.
 * 
 * @author ken
 *
 */
public class BindingUtils {

	private BindingUtils() {
	}

	/**
	 * Thread-save version of Bindings.bindBidirectional(). Config property is
	 * altered in the JavaFx UI-thread using Platform.runLater().
	 * 
	 * @param objectProperty property to bind and change thread-safe
	 * @param configProperty property to bind
	 */
	public static <T> void bindBidirectionalThreadSafe(final ObjectProperty<T> objectProperty,
			final ObjectProperty<T> configProperty, Runnable runnable) {

		objectProperty.addListener((obj, o, n) -> configProperty.setValue(objectProperty.get()));

		configProperty.addListener((obj, o, n) -> {
			runnable.run();
			Platform.runLater(() -> objectProperty.setValue(n));
		});

		objectProperty.setValue(configProperty.get());
	}

	/**
	 * Bind a JavaFx ToggleGroup to an Enum property.
	 * 
	 * Pre-requisite: JavaFx Toggle gets a user data with the Enum value as string.
	 * 
	 * @param toggleGroup    toggle to bind
	 * @param configProperty Enum property to bind
	 * @param clz            Enum class
	 */
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

	/**
	 * Bind a JavaFx ToggleGroup to an Boolean property.
	 * 
	 * Pre-requisite: JavaFx Toggle gets a user data with the Boolean value as
	 * string.
	 * 
	 * @param toggleGroup    Boolean property to bind
	 * @param configProperty Boolean property to bind
	 */
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
