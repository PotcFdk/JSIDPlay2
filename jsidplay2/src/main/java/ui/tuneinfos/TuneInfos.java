package ui.tuneinfos;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import javax.persistence.metamodel.SingularAttribute;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import ui.common.C64Window;
import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;
import ui.musiccollection.TuneInfo;

public class TuneInfos extends C64Window {

	@FXML
	private TableView<TuneInfo> tuneInfoTable;

	private ObservableList<TuneInfo> tuneInfos;

	public TuneInfos(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		tuneInfos = FXCollections.<TuneInfo> observableArrayList();
		tuneInfoTable.setItems(tuneInfos);
	}

	public void showTuneInfos(File tuneFile, SidTune tune) {
		tuneInfos.clear();
		HVSCEntry entry = new HVSCEntry(() -> util.getPlayer()
				.getSidDatabaseInfo(db -> db.getFullSongLength(tune)), "",
				tuneFile, tune);

		for (Field field : HVSCEntry_.class.getDeclaredFields()) {
			if (field.getName().equals(HVSCEntry_.id.getName())) {
				continue;
			}
			if (!(SingularAttribute.class.isAssignableFrom(field.getType()))) {
				continue;
			}
			TuneInfo tuneInfo = new TuneInfo();
			String name = util.getBundle().getString(
					HVSCEntry.class.getSimpleName() + "." + field.getName());
			tuneInfo.setName(name);
			try {
				SingularAttribute<?, ?> singleAttribute = (SingularAttribute<?, ?>) field
						.get(entry);
				Object value = ((Method) singleAttribute.getJavaMember())
						.invoke(entry);
				tuneInfo.setValue(String.valueOf(value != null ? value : ""));
			} catch (IllegalArgumentException | IllegalAccessException
					| InvocationTargetException e) {
			}
			tuneInfos.add(tuneInfo);
		}
	}

	@Override
	@FXML
	protected void close() {
		super.close();
	}
}
