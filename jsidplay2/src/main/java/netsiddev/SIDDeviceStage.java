package netsiddev;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public abstract class SIDDeviceStage extends Stage implements SIDDeviceUIPart {

	private SIDDeviceUIUtil util = new SIDDeviceUIUtil();
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
		scene.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				stage.close();
			}
		});
		stage.setScene(scene);
		stage.getIcons().add(new Image(getBundle().getString("ICON")));
		stage.setTitle(getBundle().getString("TITLE"));
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

	public ResourceBundle getBundle() {
		return util.getBundle();
	}

	protected String getStyleSheetName() {
		return "/" + getClass().getName().replace('.', '/') + ".css";
	}

}
