package ui.common;

import java.net.URL;

import javafx.util.Builder;
import sidplay.Player;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

public interface UIPart extends Builder<Object> {

	default String getBundleName() {
		return getClass().getName();
	}

	default URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	default Object build() {
		return this;
	}

	default void doClose() {
	}

	/**
	 * For JavaFX Preview in Eclipse, only (Player with default configuration for
	 * the controller)
	 */
	default UIUtil onlyForEclipseJavaFXPreviewView() {
		ConfigService configService = new ConfigService(ConfigurationType.XML);
		UIUtil util = new UIUtil(null, new Player(configService.load()), this);
		configService.close();
		return util;
	}

}
