package ui.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Popup;

public final class Toast {

	public static void makeText(Node ownerNode, String toastMsg, int seconds) {
		Popup popup = new Popup();
		popup.setAutoFix(true);
		popup.setAutoHide(true);
		popup.setHideOnEscape(true);

		Label label = new Label();
		label.setId("whatssid");
		label.setText(toastMsg);
		label.setOnMouseClicked(evt -> popup.hide());
		popup.getContent().add(label);

		Bounds userTextFieldBounds = ownerNode.getBoundsInLocal();
		Point2D popupLocation = ownerNode.localToScreen(userTextFieldBounds.getMaxX(), userTextFieldBounds.getMinY());
		popup.show(ownerNode, popupLocation.getX(), popupLocation.getY());

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.schedule(() -> Platform.runLater(() -> popup.hide()), seconds, TimeUnit.SECONDS);
	}
}