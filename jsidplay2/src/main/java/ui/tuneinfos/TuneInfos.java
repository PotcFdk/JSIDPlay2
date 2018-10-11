package ui.tuneinfos;

import java.io.File;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import ui.common.C64Window;
import ui.entities.collection.HVSCEntry;
import ui.musiccollection.SearchCriteria;
import ui.musiccollection.TuneInfo;

public class TuneInfos extends C64Window {

	@FXML
	private TableView<TuneInfo> tuneInfoTable;

	private ObservableList<TuneInfo> tuneInfos;

	public TuneInfos() {
	}

	public TuneInfos(Player player) {
		super(player);
	}

	@FXML
	protected void initialize() {
		tuneInfos = FXCollections.<TuneInfo>observableArrayList();
		SortedList<TuneInfo> sortedList = new SortedList<>(tuneInfos);
		sortedList.comparatorProperty().bind(tuneInfoTable.comparatorProperty());
		tuneInfoTable.setItems(sortedList);
	}

	public void showTuneInfos(File tuneFile, SidTune tune) {
		HVSCEntry entry = new HVSCEntry(() -> util.getPlayer().getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.), "",
				tuneFile, tune);
		tuneInfos
				.setAll(SearchCriteria
						.getAttributeValues(entry,
								field -> util.getBundle().getString(
										HVSCEntry.class.getSimpleName() + "." + field.getAttribute().getName()))
						.stream().map(info -> new TuneInfo(info.getKey(), info.getValue()))
						.collect(Collectors.toList()));
	}

}
