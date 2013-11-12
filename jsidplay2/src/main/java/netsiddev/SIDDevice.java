package netsiddev;

import java.net.URL;

import javafx.fxml.Initializable;

public interface SIDDevice extends Initializable {

	String getBundleName();

	URL getFxml();
}
