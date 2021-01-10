package ui.common.properties;

import java.util.ArrayList;
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

	public ObservableLazyListField() {
	}

	public ObservableLazyListField(List<O> initialvalue) {
		this.list = initialvalue;
	}

	public final void set(List<O> favorites) {
		this.list = favorites;
	}

	public final List<O> get() {
		if (list == null) {
			list = new ArrayList<>();
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
