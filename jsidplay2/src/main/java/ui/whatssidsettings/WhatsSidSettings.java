package ui.whatssidsettings;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import sidplay.Player;
import ui.common.C64Window;
import ui.entities.config.WhatsSidSection;

public class WhatsSidSettings extends C64Window {

	@FXML
	private CheckBox enable;

	@FXML
	private TextField url, username, password, captureTime, matchStartTime, matchRetryTime, minimumRelativeConfidence;

	public WhatsSidSettings() {
		super();
	}

	public WhatsSidSettings(Player player) {
		super(player);
	}

	@FXML
	protected void initialize() {
		WhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

		enable.selectedProperty().bindBidirectional(whatsSidSection.enableProperty());
		url.textProperty().bindBidirectional(whatsSidSection.urlProperty());
		username.textProperty().bindBidirectional(whatsSidSection.usernameProperty());
		password.textProperty().bindBidirectional(whatsSidSection.passwordProperty());
		Bindings.bindBidirectional(captureTime.textProperty(), whatsSidSection.captureTimeProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(matchStartTime.textProperty(), whatsSidSection.matchStartTimeProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(matchRetryTime.textProperty(), whatsSidSection.matchRetryTimeProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(minimumRelativeConfidence.textProperty(),
				whatsSidSection.minimumRelativeConfidenceProperty(), new NumberStringConverter());

	}

}
