package server.restful.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<T, U> extends LinkedHashMap<T, U> {

	private static final long serialVersionUID = 1L;

	private int maxSize;

	public LRUCache(int capacity) {
		super(capacity, 0.75f, true);
		this.maxSize = capacity;
	}

	@Override
	public U get(Object key) {
		return super.get(key);
	}

	@Override
	public U put(T key, U value) {
		return super.put(key, value);
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<T, U> eldest) {
		return this.size() > maxSize;
	}
}
