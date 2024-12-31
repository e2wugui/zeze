package Zeze.Transaction.GTable;


import java.util.Map.Entry;
import javax.annotation.CheckForNull;

/**
 * Implementation of the {@code equals}, {@code hashCode}, and {@code toString} methods of {@code
 * Entry}.
 *
 * @author Jared Levy
 */
abstract class AbstractMapEntry<K extends Object, V extends Object>
		implements Entry<K, V> {

	@Override
	public abstract K getKey();

	@Override
	public abstract V getValue();

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(@CheckForNull Object object) {
		if (object instanceof Entry) {
			Entry<?, ?> that = (Entry<?, ?>) object;
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

	/** Returns a string representation of the form {@code {key}={value}}. */
	@Override
	public String toString() {
		return getKey() + "=" + getValue();
	}
}
