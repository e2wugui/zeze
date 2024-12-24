package Zeze.Transaction.GTable;


import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of the {@code equals}, {@code hashCode}, and {@code toString} methods of {@code
 * Entry}.
 *
 * @author Jared Levy
 */
@GwtCompatible
abstract class AbstractMapEntry<K extends @Nullable Object, V extends @Nullable Object>
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
			return Objects.equal(this.getKey(), that.getKey())
					&& Objects.equal(this.getValue(), that.getValue());
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
