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

	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final Map<K, LogBean> changedWithKey = new TreeMap<>(); // changed with key. using in encode/decode followerApply
	private final BeanValueFactory<V> valueFactory;
	private boolean decoded; // 解码后的索引是完整日志，不从旧的源Map重新构建。

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
			changedWithKey.clear(); // 重建最终视图，重复编码不能保留已删除/覆盖的旧changed。
			for (var c : changed) {
				//noinspection DataFlowIssue
				Object pkey = c.getThis().mapKey();
				// 第三条过滤对齐Transaction.Collections.LogMap2Base.addChild：
				// 编辑时bean已不在最终map（remove不detach，编辑已删bean的陈旧引用仍会被collect，
				// 其key来自更早事务，modified单查找已拦住put/remove覆盖），不过滤则follower侧必取到null；
				// 且必须是key当前值本身——put覆盖后旧bean同样不detach，陈旧引用的字段日志
				// 不得应用到覆盖后的新值上。
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

		// modified 手工编码（不走 super.encode）：put 项的 value 是 Bean，用 bean.encode 而非 codec。
		// 两遍split：线格式与putted/removed双容器时代逐字节一致。
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
