package ui.common.properties;

import java.util.List;

/**
 * @author ken
 *
 * @param <O> list element class
 */
public class LazyListField<O> {

	private List<O> list;

	public final void set(List<O> list) {
		this.list = list;
	}

	public final List<O> get(List<O> initialvalue) {
		if (list == null) {
			list = initialvalue;
		}
		return list;
	}

}
