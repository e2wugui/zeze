package Zeze.Transaction;

public class AtomicTupleRecord<K extends Comparable<K>, V extends Bean> {
	public Record1<K, V> Record;
	public V StrongRef;
	public long Timestamp;

	public static <K extends Comparable<K>, V extends Bean> AtomicTupleRecord<K, V> create(Record1<K, V> r, V strongRef, long timestamp) {
		var atr = new AtomicTupleRecord<K, V>();
		atr.Record = r;
		atr.StrongRef = strongRef;
		atr.Timestamp = timestamp;
		//System.out.println(atr);
		return atr;
	}

	@Override
	public String toString() {
		return Record + " ref=" + StrongRef + " time=" + Timestamp;
	}
}
