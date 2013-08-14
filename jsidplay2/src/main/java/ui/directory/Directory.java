package ui.directory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import libsidplay.components.DirEntry;
import libsidutils.zip.ZipEntryFileProxy;
import ui.common.C64AnchorPane;
import ui.entities.config.SidPlay2Section;
import ui.events.UIEvent;

public class Directory extends C64AnchorPane {

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
	private TableView<DirectoryItem> directory;
	@FXML
	private TableColumn<DirectoryItem, String> dirColumn;

	private ObservableList<DirectoryItem> directoryEntries = FXCollections
			.<DirectoryItem> observableArrayList();

	private ObjectProperty<File> autoStartFileProperty = new SimpleObjectProperty<File>();

	private int fontSet = TRUE_TYPE_FONT_BIG;
	private int fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;

	private File file;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getConfig() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		directory.setItems(directoryEntries);
		directory.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				DirectoryItem selectedItem = directory.getSelectionModel()
						.getSelectedItem();
				if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
					autoStartProgram();
				}
			}
		});
		directory.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
					autoStartProgram();
				}
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

	private void autoStartProgram() {
		try {
			DirectoryItem dirItem = directory.getSelectionModel()
					.getSelectedItem();
			DirEntry dirEntry = dirItem.getDirEntry();
			if (dirEntry != null) {
				File autoStartFile = new File(getConfig().getSidplay2()
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
		try {
			if (file instanceof ZipEntryFileProxy) {
				// Load file entry from ZIP
				ZipEntryFileProxy zipEntry = (ZipEntryFileProxy) file;
				file = ZipEntryFileProxy.extractFromZip(zipEntry, getConfig()
						.getSidplay2().getTmpDir());
			}
			this.file = file;
			dirColumn.setText(file.getName());
			directoryEntries.clear();
			try {
				libsidplay.components.Directory dir = PseudoDirectory
						.getDirectory(((SidPlay2Section) getConfig()
								.getSidplay2()).getHvscFile(), file,
								getConfig());
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
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	@Override
	public void notify(final UIEvent evt) {
	}
}
