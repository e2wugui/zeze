package Zeze.Transaction;

public class AtomicTupleRecord<K extends Comparable<K>, V extends Bean> {
	public final Record1<K, V> record;
	public final V strongRef;
	public final long timestamp;

	public AtomicTupleRecord(Record1<K, V> record, V strongRef, long timestamp) {
		this.record = record;
		this.strongRef = strongRef;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return record + " ref=" + strongRef + " time=" + timestamp;
	}
}
