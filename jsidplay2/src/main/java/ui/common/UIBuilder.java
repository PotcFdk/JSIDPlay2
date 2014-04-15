package ui.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.entities.config.Configuration;

final class UIBuilder implements BuilderFactory {
	private ConsolePlayer consolePlayer;
	private Player player;
	private Configuration config;

	private JavaFXBuilderFactory defaultBuilderFactory = new JavaFXBuilderFactory();

	public UIBuilder(ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		this.consolePlayer = consolePlayer;
		this.player = player;
		this.config = config;
	}

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		if (UIPart.class.isAssignableFrom(type)) {
			try {
				Constructor<?> constructor = type
						.getConstructor(new Class[] { ConsolePlayer.class,
								Player.class, Configuration.class });
				return (Builder<?>) constructor.newInstance(this.consolePlayer,
						this.player, this.config);
			} catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(
						"UIPart implementations requires a 3-arg constructor to provide the model");
			}
		}
		return defaultBuilderFactory.getBuilder(type);
	}
}