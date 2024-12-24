package Zeze.Transaction.GTable;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CompatibleWith;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible
public interface Table<R, C, V> {
	boolean contains(@CompatibleWith("R") @Nullable Object var1, @CompatibleWith("C") @Nullable Object var2);

	boolean containsRow(@CompatibleWith("R") @Nullable Object var1);

	boolean containsColumn(@CompatibleWith("C") @Nullable Object var1);

	boolean containsValue(@CompatibleWith("V") @Nullable Object var1);

	V get(@CompatibleWith("R") @Nullable Object var1, @CompatibleWith("C") @Nullable Object var2);

	boolean isEmpty();

	int size();

	boolean equals(@Nullable Object var1);

	int hashCode();

	void clear();

	@CanIgnoreReturnValue
	@Nullable V put(R var1, C var2, V var3);

	void putAll(com.google.common.collect.Table<? extends R, ? extends C, ? extends V> var1);

	@CanIgnoreReturnValue
	@Nullable V remove(@CompatibleWith("R") @Nullable Object var1, @CompatibleWith("C") @Nullable Object var2);

	Map<C, V> row(R var1);

	Map<R, V> column(C var1);

	Set<com.google.common.collect.Table.Cell<R, C, V>> cellSet();

	Set<R> rowKeySet();

	Set<C> columnKeySet();

	Collection<V> values();

	Map<R, Map<C, V>> rowMap();

	Map<C, Map<R, V>> columnMap();

	public interface Cell<R, C, V> {
		@Nullable R getRowKey();

		@Nullable C getColumnKey();

		@Nullable V getValue();

		boolean equals(@Nullable Object var1);

		int hashCode();
	}
}
