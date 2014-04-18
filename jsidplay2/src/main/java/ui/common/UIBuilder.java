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

	private C64Stage c64Stage;

	private ConsolePlayer consolePlayer;
	private Player player;
	private Configuration config;

	private JavaFXBuilderFactory defaultBuilderFactory = new JavaFXBuilderFactory();

	public UIBuilder(C64Stage c64Stage, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		this.c64Stage = c64Stage;
		this.consolePlayer = consolePlayer;
		this.player = player;
		this.config = config;
	}

	@Override
	public Builder<?> getBuilder(Class<?> type) {
		if (UIPart.class.isAssignableFrom(type)) {
			try {
				Constructor<?> constructor = type.getConstructor(new Class[] {
						C64Stage.class, ConsolePlayer.class, Player.class,
						Configuration.class });
				return (Builder<?>) constructor.newInstance(this.c64Stage,
						this.consolePlayer, this.player, this.config);
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