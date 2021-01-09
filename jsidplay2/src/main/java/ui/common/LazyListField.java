package ui.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ken
 *
 * @param <O> list element class
 */
public class LazyListField<O> {

	private List<O> initialvalue;

	private List<O> list;

	public LazyListField() {
	}

	public LazyListField(List<O> initialvalue) {
		this.initialvalue = initialvalue;
	}

	public final void set(List<O> list) {
		this.list = list;
	}

	public final List<O> get() {
		if (list == null) {
			list = new ArrayList<>(initialvalue);
		}
		return list;
	}

	/**
	 * If initial value could not be determined earlier!
	 */
	public final List<O> get(List<O> initialvalue) {
		if (list == null) {
			list = new ArrayList<>(initialvalue);
		}
		return list;
	}

}
