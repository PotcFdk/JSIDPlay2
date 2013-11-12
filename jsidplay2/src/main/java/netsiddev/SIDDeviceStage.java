package netsiddev;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public abstract class SIDDeviceStage extends Stage implements SIDDevice {

	private SIDDeviceUtil util = new SIDDeviceUtil();
	private boolean wait;

	@Override
	public String getBundleName() {
		return getClass().getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}

	public void open() throws IOException {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) throws IOException {
		Scene scene = (Scene) util.parse(this);
		scene.getStylesheets().add(getStyleSheetName());
		stage.setScene(scene);
		stage.getIcons().add(new Image(getBundle().getString("ICON")));
		stage.setTitle(getBundle().getString("TITLE"));
		stage.setOnCloseRequest((event) -> doCloseWindow());
		if (wait) {
			stage.showAndWait();
		} else {
			stage.show();
		}
	}

	public boolean isWait() {
		return wait;
	}
	
	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	protected final void setPlayedGraphics(Node node) {
		util.setPlayedGraphics(node);
	}
	
	public ResourceBundle getBundle() {
		return util.getBundle();
	}

	protected String getStyleSheetName() {
		return "/" + getClass().getName().replace('.', '/') + ".css";
	}

	protected void doCloseWindow() {
	}

	public DoubleProperty getProgressValue() {
		return null;
	}

}
