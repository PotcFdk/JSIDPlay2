package netsiddev;

import java.io.IOException;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class SIDDeviceStage extends Stage implements SIDDeviceUIPart {

	protected SIDDeviceUIUtil util;
	private boolean wait;

	public SIDDeviceStage() {
		util = new SIDDeviceUIUtil();
		
		Scene scene = (Scene) util.parse(this);
		scene.setOnKeyPressed((ke) -> {
			if ((ke.getCode() == KeyCode.ESCAPE) || (ke.getCode() == KeyCode.ENTER)) {
				close();
			}
		});
		
		setScene(scene);
		resizableProperty().set(false);

		initStyle(StageStyle.UTILITY);
		setAlwaysOnTop(true);
		
		setTitle(util.getBundle().getString("TITLE"));
		centerOnScreen();
	}

	public void open() throws IOException {
		if (wait) {
			showAndWait();
		} else {
			show();
		}		
	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

}
