package ui;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;

public class LameUI extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Lame lameUI = new Lame();
		try {
			lameUI.open(primaryStage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		launch(args);
	}

}
