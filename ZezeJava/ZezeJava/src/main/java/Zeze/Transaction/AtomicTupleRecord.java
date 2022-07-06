package Zeze.Transaction;

public class AtomicTupleRecord<K extends Comparable<K>, V extends Bean> {
	public final Record1<K, V> Record;
	public final V StrongRef;
	public final long Timestamp;

	public AtomicTupleRecord(Record1<K, V> r, V strongRef, long timestamp) {
		Record = r;
		StrongRef = strongRef;
		Timestamp = timestamp;
	}

	@Override
	public String toString() {
		return Record + " ref=" + StrongRef + " time=" + Timestamp;
	}
}
