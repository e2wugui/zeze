package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** Map2 消费日志的共用载荷；具体家族只决定容器、键顺序和最终源值的定位。 */
abstract class LogMap2Base<K, V extends Bean> extends LogBean {
	final Meta2<K, V> meta;
	/**
	 * 结构增量单容器：value 非 null 为替换项，null 为删除标记。
	 * 与 changedWithKey（原位修改）互斥由 addChild 单查找保证（替换/删除直接吸收键槽）。
	 */
	private @Nullable Map<K, V> modified;
	private @Nullable Map<K, LogBean> changedWithKey;

	LogMap2Base(int varId, @Nullable Bean self, Meta2<K, V> meta, @Nullable Map<K, V> modified) {
		super(varId, self);
		this.meta = meta;
		// 源端只借用最终事务增量；解码创建独立容器，不修改借用数据。
		this.modified = modified;
	}

	abstract <T> Map<K, T> newMap();

	abstract boolean isCurrentValue(Bean source, K key, Bean value);

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public Map<K, V> getModified() {
		var values = modified;
		return values != null ? values : (modified = newMap());
	}

	public Map<K, LogBean> getChangedWithKey() {
		var edits = changedWithKey;
		return edits != null ? edits : (changedWithKey = newMap());
	}

	// 内部只读入口不为已知为空的部分分配容器。
	final Map<K, V> modifiedForRead() {
		return modified != null ? modified : Collections.emptyMap();
	}

	final Map<K, LogBean> changedForRead() {
		return changedWithKey != null ? changedWithKey : Collections.emptyMap();
	}

	/**
	 * 在源提交的同步监听回调中遍历新增、替换和原位修改项的完整值；删除项见 forEachRemoved()。
	 * 每个更新项回调一次，不保证遍历顺序（包括 SortedMap）。
	 * Bean 为借用的源对象，不能修改或在回调后保留；解码日志不支持此操作。
	 * 执行 action 前校验全部原位修改，遍历不改变日志内容或物化联合集合。
	 */
	public final void forEachUpsert(BiConsumer<? super K, ? super V> action) {
		if (getThis() == null)
			throw new IllegalStateException("listener projection requires a collected source log");
		var changed = changedForRead();
		// 在任何业务 action 前拒绝缺少源 Bean 的增量，避免半次投影产生副作用。
		changed.forEach((key, edit) -> {
			if (edit.getThis() == null)
				throw new IllegalStateException("listener projection requires source Beans");
		});
		// 收集时已过滤被 modified 覆盖的字段修改（addChild 单查找），两个更新部分互斥。
		modifiedForRead().forEach((key, value) -> {
			if (value != null)
				action.accept(key, value);
		});
		for (var entry : changed.entrySet())
			//noinspection DataFlowIssue,unchecked
			action.accept(entry.getKey(), (V)entry.getValue().getThis());
	}

	/** 在源提交的同步监听回调中遍历删除键（modified 中 value 为 null 的槽位）；约束同 forEachUpsert()。 */
	public final void forEachRemoved(Consumer<? super K> action) {
		if (getThis() == null)
			throw new IllegalStateException("listener projection requires a collected source log");
		modifiedForRead().forEach((key, value) -> {
			if (value == null)
				action.accept(key);
		});
	}

	@Override
	public void encode(ByteBuffer bb) {
		var edits = changedForRead();
		var m = modifiedForRead();
		var keyEncoder = meta.keyEncoder;
		bb.WriteUInt(edits.size());
		for (var e : edits.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			encodeLogBean(bb, e.getValue());
		}
		// 两遍split编码（同LogMap1）：线格式与replaced/removed双容器时代逐字节一致。
		int replacedCount = 0;
		for (var e : m.entrySet())
			if (e.getValue() != null)
				replacedCount++;
		bb.WriteUInt(replacedCount);
		for (var e : m.entrySet()) {
			V v = e.getValue();
			if (v != null) {
				keyEncoder.accept(bb, e.getKey());
				v.encode(bb);
			}
		}
		bb.WriteUInt(m.size() - replacedCount);
		for (var e : m.entrySet())
			if (e.getValue() == null)
				keyEncoder.accept(bb, e.getKey());
	}

	final void read(IByteBuffer bb) {
		var keyDecoder = meta.keyDecoder;
		int count = bb.ReadUIntPositive();
		Map<K, LogBean> edits = null;
		if (count > 0) {
			edits = newMap();
			do {
				edits.put(keyDecoder.apply(bb), decodeLogBean(bb));
			} while (--count > 0);
		}
		count = bb.ReadUIntPositive();
		Map<K, V> m = null;
		if (count > 0) {
			m = newMap();
			try {
				do {
					K key = keyDecoder.apply(bb);
					//noinspection unchecked,DataFlowIssue
					V value = (V)meta.valueFactory.invoke();
					value.decode(bb);
					m.put(key, value);
				} while (--count > 0);
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}
		count = bb.ReadUIntPositive();
		if (count > 0)
			do {
				if (m == null)
					m = newMap();
				m.put(keyDecoder.apply(bb), null);
			} while (--count > 0);
		// 读取完整载荷后再发布，既不清借用容器，也不发布半份解码结果。
		changedWithKey = edits;
		modified = m;
	}

	@Override
	public boolean addChild(Bean source, Log vlog) {
		var child = (LogBean)vlog;
		var item = child.getThis();
		//noinspection DataFlowIssue,unchecked
		K key = (K)item.mapKey();
		//noinspection DataFlowIssue
		if (modified != null && modified.containsKey(key)
				|| !isCurrentValue(source, key, item)) {
			// 过滤被替换、删除和陈旧引用的字段修改，仍保留空 Map 节点及源 Record 回调。
			return true;
		}
		return getChangedWithKey().putIfAbsent(key, child) == null;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" modified:");
		ByteBuffer.BuildSortedString(sb, modifiedForRead());
		sb.append(" changed:");
		ByteBuffer.BuildSortedString(sb, changedForRead());
		return sb.toString();
	}
}
