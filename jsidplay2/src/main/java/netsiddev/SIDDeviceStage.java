package netsiddev;

import java.io.IOException;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public abstract class SIDDeviceStage extends Stage implements SIDDeviceUIPart {

	protected SIDDeviceUIUtil util;
	private boolean wait;

	public SIDDeviceStage() {
		util = new SIDDeviceUIUtil();
	}

	public void open() throws IOException {
		open(this);
		centerOnScreen();
	}

	public void open(Stage stage) throws IOException {
		Scene scene = (Scene) util.parse(this);
		scene.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				stage.close();
			}
		});
		stage.setScene(scene);
		stage.getIcons().add(new Image(util.getBundle().getString("ICON")));
		stage.setTitle(util.getBundle().getString("TITLE"));
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

}
