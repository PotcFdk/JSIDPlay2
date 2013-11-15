package netsiddev;

import java.net.URL;

import javafx.fxml.Initializable;

public interface SIDDeviceUIPart extends Initializable {

	String getBundleName();

	URL getFxml();
}
