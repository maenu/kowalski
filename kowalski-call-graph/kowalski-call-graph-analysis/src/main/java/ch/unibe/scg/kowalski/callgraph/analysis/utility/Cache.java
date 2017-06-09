package ch.unibe.scg.kowalski.callgraph.analysis.utility;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Cache<K, V> extends ConcurrentHashMap<K, V> {

	private static final long serialVersionUID = 1L;

	public Cache() {
		super();
	}

	public synchronized V getOrPut(K k, Supplier<V> valueSupplier) {
		if (this.containsKey(k)) {
			return this.get(k);
		}
		V v = valueSupplier.get();
		this.put(k, v);
		return v;
	}

}
