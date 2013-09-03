package ui.common;

import java.net.URL;

import javafx.fxml.Initializable;

public interface UIPart extends Initializable {

	String getBundleName();

	URL getFxml();
}
