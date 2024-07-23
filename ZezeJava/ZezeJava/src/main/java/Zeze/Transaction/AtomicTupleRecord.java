package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AtomicTupleRecord<K extends Comparable<K>, V extends Bean> {
	public final @NotNull Record1<K, V> record;
	public final @Nullable V strongRef;
	public final long timestamp;

	public AtomicTupleRecord(@NotNull Record1<K, V> record, @Nullable V strongRef, long timestamp) {
		this.record = record;
		this.strongRef = strongRef;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return record + " ref=" + strongRef + " time=" + timestamp;
	}
}
