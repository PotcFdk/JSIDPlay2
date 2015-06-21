package ui.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import sidplay.Player;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

final class UIBuilder implements BuilderFactory {

	private C64Window window;

	private Player player;

	private JavaFXBuilderFactory defaultBuilderFactory = new JavaFXBuilderFactory();

	public UIBuilder(C64Window window, Player player) {
		this.window = window;
		this.player = player;
	}

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		if (UIPart.class.isAssignableFrom(type)) {
			try {
				Constructor<?> constructor = type.getConstructor(new Class[] {
						C64Window.class, Player.class });
				return (Builder<?>) constructor.newInstance(this.window,
						this.player);
			} catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(
						"UIPart implementations require a specific constructor!");
			}
		}
		return defaultBuilderFactory.getBuilder(type);
	}
}