package Zeze.Raft.RocksRaft;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;

/**
 * 与 LogMap2 完全对应，只是继承自 LogSortedMap1，value 使用 PSortedMap。
 */
public class LogSortedMap2<K extends Comparable<K>, V extends Bean> extends LogSortedMap1<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogSortedMap2<");

	private final Set<LogBean> changed = new HashSet<>(); // 收集阶段记录值 bean 的字段修改日志
	private final Map<K, LogBean> changedWithKey = new TreeMap<>(); // 按 key 索引的字段修改日志，用于编解码与 followerApply
	private final BeanValueFactory<V> valueFactory;
	private boolean decoded; // 解码日志已包含完整索引，不再按源 map 重建。

	static int typeId(Class<?> keyClass, Class<?> valueClass) {
		return Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass);
	}

	public LogSortedMap2(Class<K> keyClass, Class<V> valueClass) {
		super(typeId(keyClass, valueClass), keyClass, valueClass);
		valueFactory = BeanValueFactory.of(valueClass);
	}

	LogSortedMap2(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, BeanValueFactory<V> valueFactory) {
		super(typeId, keyCodecFuncs, null);
		this.valueFactory = valueFactory;
	}

	@Override
	@Nullable DynamicField dynamicField() {
		return valueFactory.field();
	}

	public final Set<LogBean> getChanged() {
		return changed;
	}

	public final Map<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogSortedMap2<>(getTypeId(), keyCodecFuncs, valueFactory);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void encode(ByteBuffer bb) {
		if (!decoded && getValue() != null) {
			changedWithKey.clear(); // 活日志重建最终视图，避免重复编码残留陈旧条目。
			for (var c : changed) {
				//noinspection DataFlowIssue
				Object pkey = c.getThis().mapKey();
				// 过滤已被 modified 覆盖、已移出 map 或非当前引用的陈旧 bean，避免 follower 侧空指针或误应用到新值。
				//noinspection SuspiciousMethodCalls
				if (!getModified().containsKey(pkey) && c.getThis() == getValue().get(pkey))
					changedWithKey.put((K)Objects.requireNonNull(pkey), c);
			}
		}
		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = keyCodecFuncs.encoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}

		// put 项值为 Bean 需手工编码，按 put / remove 两段输出以保持既有线格式。
		var modified = getModified();
		int puttedCount = 0;
		for (var e : modified.entrySet())
			if (e.getValue() != null)
				puttedCount++;
		bb.WriteUInt(puttedCount);
		for (var e : modified.entrySet()) {
			var v = e.getValue();
			if (v != null) {
				keyEncoder.accept(bb, e.getKey());
				v.encode(bb);
			}
		}
		bb.WriteUInt(modified.size() - puttedCount);
		for (var e : modified.entrySet())
			if (e.getValue() == null)
				keyEncoder.accept(bb, e.getKey());
	}

	@Override
	public void decode(IByteBuffer bb) {
		changed.clear();
		changedWithKey.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = valueFactory.readEdit(bb);
			changedWithKey.put(key, value);
		}

		// modified 手工解码（不走 super.decode）：put 项的 value 是 Bean，经 valueFactory 构造后 decode。
		var modified = getModified();
		modified.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			modified.put(key, valueFactory.read(bb));
		}
		for (int i = bb.ReadUInt(); i > 0; i--)
			modified.put(keyDecoder.apply(bb), null);
		decoded = true;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (changed.add((LogBean)vlog))
			changes.collect(recent, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Modified:");
		ByteBuffer.BuildSortedString(sb, getModified());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
