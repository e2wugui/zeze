package Zeze.Raft.RocksRaft;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import org.jetbrains.annotations.NotNull;

/**
 * 与 LogMap1 完全对应，只是继承自 LogSortedMap，value 使用 PSortedMap。
 */
public class LogSortedMap1<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogSortedMap1<");

	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private Map<K, V> putted;
	private Set<K> removed;

	public LogSortedMap1(Class<K> keyClass, Class<V> valueClass) {
		this(keyClass, valueClass, null);
	}

	/** 自定义比较器须在复制双方的日志工厂一致配置；wire不携带比较器。 */
	public LogSortedMap1(Class<K> keyClass, Class<V> valueClass, Comparator<? super K> comparator) {
		this(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass, comparator);
	}

	LogSortedMap1(int typeId, Class<K> keyClass, Class<V> valueClass) {
		this(typeId, keyClass, valueClass, null);
	}

	LogSortedMap1(int typeId, Class<K> keyClass, Class<V> valueClass, Comparator<? super K> comparator) {
		super(typeId, comparator);
		putted = new TreeMap<>(comparator);
		removed = new TreeSet<>(comparator);
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogSortedMap1(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		putted = new TreeMap<>();
		removed = new TreeSet<>();
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final Map<K, V> getPutted() {
		return putted;
	}

	public final Set<K> getRemoved() {
		return removed;
	}

	@Override
	protected void onComparatorChanged(Comparator<? super K> comparator) {
		var newPutted = new TreeMap<K, V>(comparator);
		newPutted.putAll(putted);
		putted = newPutted;
		var newRemoved = new TreeSet<K>(comparator);
		newRemoved.addAll(removed);
		removed = newRemoved;
	}

	public final V get(K key) {
		return getValue().get(key);
	}

	public final void add(K key, V value) {
		put(key, value);
	}

	public final void put(K key, V value) {
		setValue(getValue().plus(key, value));
		putted.put(key, value);
		removed.remove(key);
	}

	public final void remove(K key) {
		setValue(getValue().minus(key));
		putted.remove(key);
		removed.add(key);
	}

	public final void clear() {
		for (var key : getValue().keySet())
			remove(key);
		setValue(org.pcollections.Empty.sortedMap());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(putted.size());
		var keyEncoder = keyCodecFuncs.encoder;
		var valueEncoder = valueCodecFuncs.encoder;
		for (var p : putted.entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			valueEncoder.accept(bb, p.getValue());
		}

		bb.WriteUInt(removed.size());
		for (var r : removed)
			keyEncoder.accept(bb, r);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		putted.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			var key = keyDecoder.apply(bb);
			var value = valueDecoder.apply(bb);
			putted.put(key, value);
		}

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			removed.add(keyDecoder.apply(bb));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSortedMap1<K, V>)log;
			currentLog.setValue(this.getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(LogSortedMap1<K, V> another) {
		// Put,Remove 需要确认有没有顺序问题
		// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
		for (var e : another.putted.entrySet()) {
			// replace 1,2,3 remove 4
			putted.put(e.getKey(), e.getValue());
			removed.remove(e.getKey());
		}
		for (var e : another.removed) {
			// replace 2,3 remove 1,4
			putted.remove(e);
			removed.add(e);
		}
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogSortedMap1<>(getTypeId(), keyCodecFuncs, valueCodecFuncs);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, putted);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
