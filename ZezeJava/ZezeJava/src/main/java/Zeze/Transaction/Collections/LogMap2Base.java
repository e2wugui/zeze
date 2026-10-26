package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** Map2 消费日志的共用载荷；具体家族只决定容器、键顺序和最终源值的定位。 */
abstract class LogMap2Base<K, V extends Bean> extends LogBean {
	final Meta2<K, V> meta;
	private @Nullable Map<K, V> replaced;
	private @Nullable Set<K> removed;
	private @Nullable Map<K, LogBean> changedWithKey;

	LogMap2Base(int varId, @Nullable Bean self, Meta2<K, V> meta,
			@Nullable Map<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, self);
		this.meta = meta;
		// 源端只借用最终事务增量；解码创建独立容器，不修改借用数据。
		this.replaced = replaced;
		this.removed = removed;
	}

	abstract <T> Map<K, T> newMap();

	abstract Set<K> newKeySet();

	abstract boolean isCurrentValue(Bean source, K key, Bean value);

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public Map<K, V> getReplaced() {
		var values = replaced;
		return values != null ? values : (replaced = newMap());
	}

	public final Set<K> getRemoved() {
		var keys = removed;
		return keys != null ? keys : (removed = newKeySet());
	}

	public Map<K, LogBean> getChangedWithKey() {
		var edits = changedWithKey;
		return edits != null ? edits : (changedWithKey = newMap());
	}

	// 内部只读入口不为已知为空的部分分配容器。
	final Map<K, V> replacedForRead() {
		return replaced != null ? replaced : Collections.emptyMap();
	}

	final Set<K> removedForRead() {
		return removed != null ? removed : Collections.emptySet();
	}

	final Map<K, LogBean> changedForRead() {
		return changedWithKey != null ? changedWithKey : Collections.emptyMap();
	}

	/**
	 * 在源提交的同步监听回调中遍历新增、替换和原位修改项的完整值；删除项见 getRemoved()。
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
		// 收集时已过滤 replaced/removed 覆盖的字段修改，两个更新部分互斥。
		replacedForRead().forEach(action);
		for (var entry : changed.entrySet())
			//noinspection DataFlowIssue,unchecked
			action.accept(entry.getKey(), (V)entry.getValue().getThis());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var edits = changedForRead();
		var replacements = replacedForRead();
		var removals = removedForRead();
		var keyEncoder = meta.keyEncoder;
		bb.WriteUInt(edits.size());
		for (var e : edits.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			encodeLogBean(bb, e.getValue());
		}
		bb.WriteUInt(replacements.size());
		for (var e : replacements.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
		bb.WriteUInt(removals.size());
		for (K key : removals)
			keyEncoder.accept(bb, key);
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
		Map<K, V> replacements = null;
		if (count > 0) {
			replacements = newMap();
			try {
				do {
					K key = keyDecoder.apply(bb);
					//noinspection unchecked,DataFlowIssue
					V value = (V)meta.valueFactory.invoke();
					value.decode(bb);
					replacements.put(key, value);
				} while (--count > 0);
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}
		count = bb.ReadUIntPositive();
		Set<K> removals = null;
		if (count > 0) {
			removals = newKeySet();
			do {
				removals.add(keyDecoder.apply(bb));
			} while (--count > 0);
		}
		// 读取完整载荷后再发布，既不清借用容器，也不发布半份解码结果。
		changedWithKey = edits;
		replaced = replacements;
		removed = removals;
	}

	@Override
	public boolean addChild(Bean source, Log vlog) {
		var child = (LogBean)vlog;
		var item = child.getThis();
		//noinspection DataFlowIssue,unchecked
		K key = (K)item.mapKey();
		//noinspection DataFlowIssue
		if (replaced != null && replaced.containsKey(key)
				|| removed != null && removed.contains(key)
				|| !isCurrentValue(source, key, item)) {
			// 过滤被替换、删除和陈旧引用的字段修改，仍保留空 Map 节点及源 Record 回调。
			return true;
		}
		return getChangedWithKey().putIfAbsent(key, child) == null;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" replaced:");
		ByteBuffer.BuildSortedString(sb, replacedForRead());
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removedForRead());
		sb.append(" changed:");
		ByteBuffer.BuildSortedString(sb, changedForRead());
		return sb.toString();
	}
}
