package ui.common;

import java.net.URL;

import javafx.fxml.Initializable;
import ui.events.UIEventListener;

public interface UIPart extends UIEventListener, Initializable {

	String getBundleName();

	URL getFxml();
}
