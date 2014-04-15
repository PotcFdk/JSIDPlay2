package ui.directory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import libsidplay.Player;
import libsidplay.components.DirEntry;
import sidplay.ConsolePlayer;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.Configuration;

public class Directory extends AnchorPane implements UIPart {

	/**
	 * Upper case letters.
	 */
	private static final int TRUE_TYPE_FONT_BIG = 0xe000;
	/**
	 * Lower case letters.
	 */
	private static final int TRUE_TYPE_FONT_SMALL = 0xe100;
	/**
	 * Inverse Upper case letters.
	 */
	private static final int TRUE_TYPE_FONT_INVERSE_BIG = 0xe200;
	/**
	 * Inverse Lower case letters.
	 */
	private static final int TRUE_TYPE_FONT_INVERSE_SMALL = 0xe300;

	@FXML
	protected TableView<DirectoryItem> directory;
	@FXML
	private TableColumn<DirectoryItem, String> dirColumn;

	private UIUtil util;

	private ObservableList<DirectoryItem> directoryEntries;

	private ObjectProperty<File> autoStartFileProperty = new SimpleObjectProperty<File>();

	private int fontSet = TRUE_TYPE_FONT_BIG;
	private int fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;

	private File file;

	public Directory(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		util = new UIUtil(consolePlayer, player, config, this);
		getChildren().add((Node) util.parse());
	}

	@FXML
	private void initialize() {
		directoryEntries = FXCollections.<DirectoryItem> observableArrayList();
		directory.setItems(directoryEntries);
		directory.setOnKeyPressed((event) -> {
			DirectoryItem selectedItem = directory.getSelectionModel()
					.getSelectedItem();
			if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
				autoStartProgram();
			}
		});
		directory.setOnMousePressed((event) -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				autoStartProgram();
			}
		});
	}

	@FXML
	private void doSwitchFont() {
		if (fontSet == TRUE_TYPE_FONT_BIG) {
			fontSet = TRUE_TYPE_FONT_SMALL;
		} else {
			fontSet = TRUE_TYPE_FONT_BIG;
		}
		if (fontSetHeader == TRUE_TYPE_FONT_INVERSE_BIG) {
			fontSetHeader = TRUE_TYPE_FONT_INVERSE_SMALL;
		} else {
			fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;
		}
		loadPreview(file);
	}

	public ObjectProperty<File> getAutoStartFileProperty() {
		return autoStartFileProperty;
	}

	protected void autoStartProgram() {
		try {
			DirectoryItem dirItem = directory.getSelectionModel()
					.getSelectedItem();
			DirEntry dirEntry = dirItem.getDirEntry();
			if (dirEntry != null) {
				File autoStartFile = new File(util.getConfig().getSidplay2()
						.getTmpDir(), dirEntry.getValidFilename() + ".prg");
				autoStartFile.deleteOnExit();
				dirEntry.save(autoStartFile);
				autoStartFileProperty.set(autoStartFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadPreview(File file) {
		if (file == null) {
			return;
		}
		this.file = file;
		dirColumn.setText(file.getName());
		directoryEntries.clear();
		try {
			libsidplay.components.Directory dir = PseudoDirectory.getDirectory(
					util.getConsolePlayer(), file, util.getConfig());
			if (dir != null) {
				// Print directory title/id
				DirectoryItem headerItem = new DirectoryItem();
				headerItem.setText(print(dir.toString(), fontSetHeader));
				directoryEntries.add(headerItem);
				List<DirEntry> dirEntries = dir.getDirEntries();
				// Print directory entries
				for (DirEntry dirEntry : dirEntries) {
					DirectoryItem dirItem = new DirectoryItem();
					dirItem.setText(print(dirEntry.toString(), fontSet));
					dirItem.setDirEntry(dirEntry);
					directoryEntries.add(dirItem);
				}
				// Print directory result
				if (dir.getStatusLine() != null) {
					DirectoryItem dirItem = new DirectoryItem();
					dirItem.setText(print(dir.getStatusLine(), fontSet));
					directoryEntries.add(dirItem);
				}
			} else {
				throw new IOException();
			}
		} catch (IOException ioE) {
			DirectoryItem dirItem = new DirectoryItem();
			dirItem.setText(print("SORRY, NO PREVIEW AVAILABLE!",
					TRUE_TYPE_FONT_BIG));
			directoryEntries.add(dirItem);
		}
		DirectoryItem dirItem = new DirectoryItem();
		dirItem.setText(print("READY.", TRUE_TYPE_FONT_BIG));
		directoryEntries.add(dirItem);
	}

	private String print(final String s, int fontSet) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			buf.append(print(s.charAt(i), fontSet));
		}
		return buf.toString();
	}

	private String print(final char c, int fontSet) {
		if ((c & 0x60) == 0) {
			return String.valueOf((char) (c | 0x40 | (fontSet ^ 0x0200)));
		} else {
			return String.valueOf((char) (c | fontSet));
		}
	}

}
