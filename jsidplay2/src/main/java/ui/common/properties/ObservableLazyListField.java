package ui.common.properties;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author ken
 *
 * @param <O> list element class
 */
public class ObservableLazyListField<O> {

	protected List<O> list;

	private ObservableList<O> observableList;

	public final void set(List<O> list) {
		this.list = list;
	}

	public final List<O> get(List<O> initialvalue) {
		if (list == null) {
			list = initialvalue;
		}
		return getObservableList();
	}

	public ObservableList<O> getObservableList() {
		if (observableList == null) {
			observableList = FXCollections.<O>observableArrayList(list);
			Bindings.bindContent(list, observableList);
		}
		return observableList;
	}

}
