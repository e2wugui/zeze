package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 记录装载的原子快照三元组：记录、装载时的强引用值与时间戳，供事务做冲突检测。 */
public record AtomicTupleRecord<K extends Comparable<K>, V extends Bean>(@NotNull Record1<K, V> record,
																		 @Nullable V strongRef,
																		 long timestamp) {

	@Override
	public String toString() {
		return record + " ref=" + strongRef + " time=" + timestamp;
	}
}
