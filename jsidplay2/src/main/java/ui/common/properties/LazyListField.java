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

	protected List<O> list;

	public final void set(List<O> list) {
		this.list = list;
	}

	public List<O> get(Supplier<List<O>> initialValueSupplier) {
		if (list == null) {
			list = initialValueSupplier.get();
		}
		return list;
	}

}
