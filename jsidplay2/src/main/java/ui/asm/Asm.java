package ui.asm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Scanner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import kickassu.errors.AsmError;
import kickassu.exceptions.AsmErrorException;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.WebUtils;
import libsidutils.assembler.KickAssembler;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class Asm extends Tab implements UIPart {

	public static final String ID = "ASM";

	private static final String HOMEPAGE_URL = "http://www.theweb.dk/KickAssembler/";
	private static final String ASM_RESOURCE = "asm";
	private static final String ASM_EXAMPLE = "/ui/asm/Asm.asm";

	@FXML
	private TextArea contents;

	@FXML
	private TableView<Variable> variablesTable;
	@FXML
	private TableColumn<Variable, String> varNameColumn, varValueColumn;
	@FXML
	private Label status;

	private KickAssembler assembler;
	private ObservableList<Variable> variables;

	private UIUtil util;

	public Asm(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		status.setPrefHeight(Double.MAX_VALUE);
		assembler = new KickAssembler();
		InputStream is = Asm.class.getResourceAsStream(ASM_EXAMPLE);
		try (Scanner s = new Scanner(is, "ISO-8859-1")) {
			contents.setText(s.useDelimiter("\\A").next());
		}
		varNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		varNameColumn.setOnEditCommit(
				(evt) -> evt.getTableView().getItems().get(evt.getTablePosition().getRow()).setName(evt.getNewValue()));
		varValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		varValueColumn.setOnEditCommit((evt) -> evt.getTableView().getItems().get(evt.getTablePosition().getRow())
				.setValue(evt.getNewValue()));
		variables = FXCollections.<Variable> observableArrayList();
		variablesTable.setItems(variables);
		varNameColumn.prefWidthProperty().bind(variablesTable.widthProperty().multiply(0.4));
		varValueColumn.prefWidthProperty().bind(variablesTable.widthProperty().multiply(0.6));
	}

	@FXML
	private void gotoHomepage() {
		WebUtils.browse(HOMEPAGE_URL);
	}

	@FXML
	private void addVariable() {
		final Variable variable = new Variable();
		variable.setName("test");
		variable.setValue("1");
		variables.add(variable);
	}

	@FXML
	private void removeVariable() {
		final Variable selectedItem = variablesTable.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			variables.remove(selectedItem);
		}
	}

	@FXML
	private void compile() {
		try {
			HashMap<String, String> globals = new HashMap<String, String>();
			variables.stream().forEach((var) -> globals.put(var.getName(), var.getValue()));
			InputStream asm = new ByteArrayInputStream(contents.getText().getBytes("UTF-8"));
			byte[] assembly = assembler.assemble(ASM_RESOURCE, asm, globals);
			InputStream is = new ByteArrayInputStream(assembly);
			SidTune tune = SidTune.load("assembly.prg", is);
			status.setText("");
			util.getPlayer().play(tune);
		} catch (AsmErrorException e) {
			if (e.getError() != null) {
				highlightError(e.getError());
			}
			status.setText(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	private void highlightError(AsmError e) {
		int pos = 0;
		int line = 0;
		if (e.getRange()!=null) {
			try (Scanner s = new Scanner(contents.getText())) {
				s.useDelimiter("\n");
				while (s.hasNext() && line < e.getRange().getStartLineNo()-1) {
					pos += s.next().length() + 1;
					line++;
				}
			}
			contents.positionCaret(pos + e.getRange().getStartLinePos() - 1);
		}
		contents.selectNextWord();
	}
}
