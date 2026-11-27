package Zeze.Transaction.GTable;


import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;

/** {@code Map.Entry} 骨架实现，提供标准的 {@code equals}、{@code hashCode} 与 {@code toString}。 */
abstract class AbstractMapEntry<K extends @Nullable Object, V extends @Nullable Object> implements Entry<K, V> {
	@Override
	public abstract K getKey();

	@Override
	public abstract V getValue();

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (object instanceof Entry<?, ?> that) {
			return Utils.equal(this.getKey(), that.getKey())
					&& Utils.equal(this.getValue(), that.getValue());
		}
		return false;
	}

	@Override
	public int hashCode() {
		K k = getKey();
		V v = getValue();
		return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
	}

	/** 返回 {@code {key}={value}} 格式的字符串表示。 */
	@Override
	public String toString() {
		return getKey() + "=" + getValue();
	}
}
