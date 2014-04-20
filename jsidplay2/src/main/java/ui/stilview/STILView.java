package ui.stilview;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libsidplay.Player;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import sidplay.ConsolePlayer;
import ui.common.C64Window;
import ui.entities.config.Configuration;

public class STILView extends C64Window {
	private static final String STYLE_NORMAL = "styleNormal";
	private static final String STYLE_FILENAME = "styleFilename";
	private static final String STYLE_COMMENT = "styleComment";
	private static final String STYLE_SUBTUNE = "styleSubtune";
	private static final String STYLE_NAME = "styleName";
	private static final String STYLE_TITLE = "styleTitle";
	private static final String STYLE_AUTHOR = "styleAuthor";
	private static final String STYLE_ARTIST = "styleArtist";

	@FXML
	private TreeView<Object> tree;
	@FXML
	private VBox textArea;
	@FXML
	private SplitPane splitPane;

	public STILView(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
	}

	@FXML
	private void initialize() {
		splitPane.setDividerPosition(0, 0.3);
		tree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> {
					if (newValue != null) {
						setTextAreaFromTree(newValue.getValue());
					}
				});
	}

	public void setEntry(STILEntry entry) {
		STILEntryTreeItem root = new STILEntryTreeItem(entry);
		tree.setRoot(root);
		tree.getSelectionModel().select(root);
	}

	protected void setTextAreaFromTree(final Object comp) {
		textArea.getChildren().clear();
		writeSTIL(comp);
	}

	private void writeSTIL(final Object comp) {
		if (comp instanceof STILEntry) {
			final STILEntry entry = (STILEntry) comp;
			writeEntry(entry);
			for (int i = 0; i < entry.infos.size(); i++) {
				writeSTIL(entry.infos.get(i));
			}
			for (int i = 0; i < entry.subtunes.size(); i++) {
				writeSTIL(entry.subtunes.get(i));
			}
		} else if (comp instanceof TuneEntry) {
			final TuneEntry tuneEntry = (TuneEntry) comp;
			writeSubTune(tuneEntry);
			for (int i = 0; i < tuneEntry.infos.size(); i++) {
				writeSTIL(tuneEntry.infos.get(i));
			}
		} else if (comp instanceof Info) {
			final Info info = (Info) comp;
			writeInfo(info);
		}
	}

	private void writeEntry(final STILEntry entry) {
		addText(util.getBundle().getString("FILENAME"), entry.filename,
				STYLE_FILENAME);
		if (entry.globalComment != null) {
			addText("", entry.globalComment.trim(), STYLE_NORMAL);
		}
	}

	private void writeSubTune(final TuneEntry tuneEntry) {
		addNewLine();
		addText(util.getBundle().getString("SUBTUNE"),
				String.valueOf(tuneEntry.tuneNo) + " ", STYLE_SUBTUNE);
		if (tuneEntry.globalComment != null) {
			addText("", tuneEntry.globalComment.trim(), STYLE_COMMENT);
		}
	}

	private void writeInfo(final Info info) {
		if (info.comment != null) {
			addText("", info.comment.trim(), STYLE_NORMAL);
		}
		if (info.name != null) {
			addText(util.getBundle().getString("NAME"), info.name, STYLE_NAME);
		}
		if (info.author != null) {
			addText(util.getBundle().getString("AUTHOR"), info.author,
					STYLE_AUTHOR);
		}
		if (info.title != null) {
			addText(util.getBundle().getString("TITLE"), info.title,
					STYLE_TITLE);
		}
		if (info.artist != null) {
			addText(util.getBundle().getString("ARTIST"), info.artist,
					STYLE_ARTIST);
		}
	}

	private void addText(String heading, String text, String style) {
		HBox line = new HBox();
		Label headingLabel = new Label(heading);
		headingLabel.setWrapText(true);
		headingLabel.getStyleClass().add(style);
		line.getChildren().add(headingLabel);

		Label textLabel = new Label(text);
		textLabel.setWrapText(true);
		textLabel.getStyleClass().add(STYLE_NORMAL);
		line.getChildren().add(textLabel);

		textArea.getChildren().add(line);
	}

	private void addNewLine() {
		HBox line = new HBox();

		Label newline = new Label("\n");
		newline.setWrapText(true);
		newline.getStyleClass().add(STYLE_NORMAL);
		line.getChildren().add(newline);

		textArea.getChildren().add(line);
	}

}
