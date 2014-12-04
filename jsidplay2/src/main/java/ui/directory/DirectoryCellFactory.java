package ui.directory;

import java.io.InputStream;

import ui.JSIDPlay2Main;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Font;
import javafx.util.Callback;

/**
 * Classify SidDump table cells for CSS styling.
 * 
 * @author Ken Händel
 * 
 */
public class DirectoryCellFactory
		implements
		Callback<TableColumn<DirectoryItem, String>, TableCell<DirectoryItem, String>> {

	protected Font c64Font;
	{
		InputStream fontStream = JSIDPlay2Main.class
				.getResourceAsStream("fonts/C64_Elite_Mono_v1.0-STYLE.ttf");
		c64Font = Font.loadFont(fontStream, 10);
	}

	@Override
	public TableCell<DirectoryItem, String> call(
			final TableColumn<DirectoryItem, String> column) {
		final TableCell<DirectoryItem, String> cell = new TableCell<DirectoryItem, String>() {
			@Override
			public void updateItem(String value, boolean empty) {
				super.updateItem(value, empty);
				if (!empty) {
					setText(value);
				} else {
					setText("");
				}
				setFont(c64Font);
			}
		};
		return cell;
	}
}
