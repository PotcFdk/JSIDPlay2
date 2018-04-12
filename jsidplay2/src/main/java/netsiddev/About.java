package netsiddev;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class About extends SIDDeviceStage {

	/** Build date calculated from our own modify time */
	public static Calendar LAST_MODIFIED;

	static {
		try {
			URL us = About.class.getProtectionDomain().getCodeSource().getLocation();
			LAST_MODIFIED = Calendar.getInstance();
			LAST_MODIFIED.setTime(new Date(us.openConnection().getLastModified()));
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@FXML
	private Text credits;

	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}

	@FXML
	private void initialize() {
		credits.setText(String.format(getBundle().getString("CREDITS"), LAST_MODIFIED.get(Calendar.YEAR)));
		setWait(true);
	}
}
