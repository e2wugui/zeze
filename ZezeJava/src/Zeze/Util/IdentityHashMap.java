package Zeze.Util;

import Zeze.*;

public final class IdentityHashMap<K, V> extends java.util.concurrent.ConcurrentHashMap<K, V> {
	private static class IdentityEqualityComparer implements IEqualityComparer<K> {
		public final boolean equals(K x, K y) {
			return x == y;
		}

		public final int hashCode(K obj) {
			return RuntimeHelpers.hashCode(obj);
		}
	}
	private static IdentityEqualityComparer comparer = new IdentityEqualityComparer();

	public IdentityHashMap() {
		super(comparer);
	}
}