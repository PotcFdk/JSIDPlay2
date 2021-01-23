package ui.common.properties;

import java.util.List;
import java.util.function.Supplier;

/**
 * Lazy-initialized list.
 * 
 * @author ken
 *
 * @param <O> list element class
 */
public class LazyListField<O> {

	private List<O> list;

	public final void set(List<O> list) {
		this.list = list;
	}

	public final List<O> get(Supplier<List<O>> initialvalueSupplier) {
		if (list == null) {
			list = initialvalueSupplier.get();
		}
		return list;
	}

}
