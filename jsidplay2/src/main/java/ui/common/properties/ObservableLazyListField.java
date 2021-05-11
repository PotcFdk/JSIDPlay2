package ui.common.properties;

import static javafx.beans.binding.Bindings.bindContent;

import java.util.List;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Lazy initialized observable list. Changes to the observable list are updated
 * in the original list, automatically. The original list must not be changed
 * directly anymore or unexpected results could occur.
 * 
 * @author ken
 *
 * @param <O> list element class
 */
public class ObservableLazyListField<O> extends LazyListField<O> {

	private ObservableList<O> observableList;

	@Override
	public final List<O> get(Supplier<List<O>> initialvalueSupplier) {
		super.get(initialvalueSupplier);
		if (observableList == null) {
			observableList = FXCollections.<O>observableArrayList(list);
			bindContent(list, observableList);
		}
		return observableList;
	}

}
