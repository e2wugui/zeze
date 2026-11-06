package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;

/**
 * 与 LogMap1 完全对应，只是继承自 LogSortedMap，value 使用 PSortedMap。
 */
public class LogSortedMap1<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogSortedMap1<");

	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs;

	/** 单容器记账（同LogMap1），TreeMap保持源容器比较器的键等价折叠。 */
	private Map<K, V> modified;

	public LogSortedMap1(Class<K> keyClass, Class<V> valueClass) {
		this(keyClass, valueClass, null);
	}

	/** 自定义比较器须在复制双方的日志工厂一致配置；wire不携带比较器。 */
	public LogSortedMap1(Class<K> keyClass, Class<V> valueClass, @Nullable Comparator<? super K> comparator) {
		this(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass, comparator);
	}

	LogSortedMap1(int typeId, Class<K> keyClass, Class<V> valueClass) {
		this(typeId, keyClass, valueClass, null);
	}

	LogSortedMap1(int typeId, Class<K> keyClass, Class<V> valueClass, @Nullable Comparator<? super K> comparator) {
		super(typeId, comparator);
		modified = new TreeMap<>(comparator);
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogSortedMap1(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		modified = new TreeMap<>();
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final Map<K, V> getModified() {
		return modified;
	}

	@Override
	protected void onComparatorChanged(@Nullable Comparator<? super K> comparator) {
		var newModified = new TreeMap<K, V>(comparator);
		newModified.putAll(modified);
		modified = newModified;
	}

	public final V get(K key) {
		//noinspection DataFlowIssue
		return getValue().get(key);
	}

	public final void add(K key, V value) {
		put(key, value);
	}

	public final void put(K key, V value) {
		//noinspection DataFlowIssue
		setValue(getValue().plus(key, value));
		modified.put(key, value);
	}

	public final void remove(K key) {
		//noinspection DataFlowIssue
		setValue(getValue().minus(key));
		modified.put(key, null);
	}

	public final void clear() {
		//noinspection DataFlowIssue
		for (var key : getValue().keySet())
			remove(key);
	}

	@Override
	public void encode(ByteBuffer bb) {
		var keyEncoder = keyCodecFuncs.encoder;
		//noinspection DataFlowIssue
		var valueEncoder = valueCodecFuncs.encoder;
		// 两遍split编码：线格式与putted/removed双容器时代逐字节一致。
		int puttedCount = 0;
		for (var e : modified.entrySet())
			if (e.getValue() != null)
				puttedCount++;
		bb.WriteUInt(puttedCount);
		for (var e : modified.entrySet()) {
			V v = e.getValue();
			if (v != null) {
				keyEncoder.accept(bb, e.getKey());
				valueEncoder.accept(bb, v);
			}
		}

		bb.WriteUInt(modified.size() - puttedCount);
		for (var e : modified.entrySet())
			if (e.getValue() == null)
				keyEncoder.accept(bb, e.getKey());
	}

	@Override
	public void decode(IByteBuffer bb) {
		modified.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		//noinspection DataFlowIssue
		var valueDecoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; --i)
			modified.put(keyDecoder.apply(bb), valueDecoder.apply(bb));

		for (int i = bb.ReadUInt(); i > 0; --i)
			modified.put(keyDecoder.apply(bb), null);
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
		// 单容器putAll即last-writer-wins，双容器时代的Put/Remove顺序问题由单槽互斥消除。
		modified.putAll(another.modified);
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
		sb.append(" Modified:");
		ByteBuffer.BuildSortedString(sb, modified);
		return sb.toString();
	}
}
