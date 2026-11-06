package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;

/**
 * 基础类型 Map 容器的增量日志：单容器 modified 记键差量，value 非 null 为 put、null 为 remove。
 */
public class LogMap1<K, V> extends LogMap<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogMap1<");

	protected final SerializeHelper.CodecFuncs<K> keyCodecFuncs;
	protected final SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs;

	private final HashMap<K, V> modified = new HashMap<>();

	public LogMap1(Class<K> keyClass, Class<V> valueClass) {
		this(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass);
	}

	LogMap1(int typeId, Class<K> keyClass, Class<V> valueClass) {
		super(typeId);
		keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogMap1(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.keyCodecFuncs = keyCodecFuncs;
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final HashMap<K, V> getModified() {
		return modified;
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
		setValue(Zeze.Collections.Persistents.map());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var keyEncoder = keyCodecFuncs.encoder;
		//noinspection DataFlowIssue
		var valueEncoder = valueCodecFuncs.encoder;
		// 两遍split编码：线格式与putted/removed双容器时代逐字节一致（两个size前缀段）。
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
			var currentLog = (LogMap1<K, V>)log;
			currentLog.setValue(this.getValue());
			currentLog.mergeChangeNote(this);
		} else
			currentSp.putLog(this);
	}

	private void mergeChangeNote(LogMap1<K, V> another) {
		// 单容器putAll即last-writer-wins，双容器时代的Put/Remove顺序问题由单槽互斥消除。
		modified.putAll(another.modified);
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap1<>(getTypeId(), keyCodecFuncs, valueCodecFuncs);
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
