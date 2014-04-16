package ui.musiccollection;

import java.util.function.BooleanSupplier;

public final class SearchScope {
	private String name;
	private BooleanSupplier forward;

	public SearchScope(String name, BooleanSupplier forward) {
		this.name = name;
		this.forward = forward;
	}

	public BooleanSupplier getForward() {
		return forward;
	}

	@Override
	public String toString() {
		return name;
	}
}
