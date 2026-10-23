package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** PSortedMap2 的消费日志：结构变化与最终仍在容器中的值 Bean 原位修改。 */
public class LogSortedMap2<K extends Comparable<K>, V extends Bean> extends LogBean {
	private final Meta2<K, V> meta;
	private final @Nullable Comparator<? super K> comparator;
	private @Nullable Map<K, V> replaced;
	private @Nullable Set<K> removed;
	private @Nullable Map<K, LogBean> changedWithKey;

	public LogSortedMap2(@Nullable Bean belong, int varId, @Nullable Bean self,
			org.pcollections.PSortedMap<K, V> value, Meta2<K, V> meta) {
		this(belong, varId, self, value.comparator(), meta, null, null);
	}

	LogSortedMap2(@Nullable Bean belong, int varId, @Nullable Bean self,
			@Nullable Comparator<? super K> comparator, Meta2<K, V> meta,
			@Nullable Map<K, V> replaced, @Nullable Set<K> removed) {
		super(belong, varId, self);
		this.meta = meta;
		this.comparator = comparator;
		// 源端借用最终事务增量，解码时整体换成独立容器；编码和监听器均不修改它们。
		this.replaced = replaced;
		this.removed = removed;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final @Nullable Comparator<? super K> comparator() {
		return comparator;
	}

	public final Map<K, V> getReplaced() {
		var values = replaced;
		return values != null ? values : (replaced = new TreeMap<>(comparator));
	}

	public final Set<K> getRemoved() {
		var keys = removed;
		return keys != null ? keys : (removed = new TreeSet<>(comparator));
	}

	public final Map<K, LogBean> getChangedWithKey() {
		var edits = changedWithKey;
		return edits != null ? edits : (changedWithKey = new TreeMap<>(comparator));
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
		for (K k : removals)
			keyEncoder.accept(bb, k);
	}

	@Override
	public void decode(IByteBuffer bb) {
		if (getThis() != null)
			throw new IllegalStateException("cannot decode into a source SortedMap2 log");
		var keyDecoder = meta.keyDecoder;
		Map<K, LogBean> newChangedWithKey = null;
		int count = bb.ReadUInt();
		if (count > 0) {
			var edits = new TreeMap<K, LogBean>(comparator);
			for (; count > 0; --count)
				edits.put(keyDecoder.apply(bb), decodeLogBean(bb));
			newChangedWithKey = edits;
		}
		Map<K, V> newReplaced = null;
		count = bb.ReadUInt();
		if (count > 0) {
			var replacements = new TreeMap<K, V>(comparator);
			try {
				for (; count > 0; --count) {
					K k = keyDecoder.apply(bb);
					//noinspection unchecked,DataFlowIssue
					V v = (V)meta.valueFactory.invoke();
					v.decode(bb);
					replacements.put(k, v);
				}
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
			newReplaced = replacements;
		}
		Set<K> newRemoved = null;
		count = bb.ReadUInt();
		if (count > 0) {
			var removals = new TreeSet<K>(comparator);
			for (; count > 0; --count)
				removals.add(keyDecoder.apply(bb));
			newRemoved = removals;
		}
		// 不清理源端借用的事务容器；失败的解码也不发布半份消费载荷。
		changedWithKey = newChangedWithKey;
		replaced = newReplaced;
		removed = newRemoved;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		var child = (LogBean)vlog;
		var childBean = Objects.requireNonNull(child.getThis(), "source Bean log has no value");
		K key = (K)childBean.mapKey();
		var source = (PSortedMap2<K, V>)Objects.requireNonNull(getThis(), "decoded log cannot collect");
		// 已替换/删除的值随结构增量重放；旧 Bean 即使仍保留 mapKey，也不能作用于新值。
		// source.map 已在最终提交阶段更新，TreeMap/TreeSet 保留源容器的键等价关系。
		if (replacedForRead().containsKey(key) || removedForRead().contains(key) || source.map.get(key) != childBean) {
			changes.collect(recent, this);
			return;
		}
		if (getChangedWithKey().putIfAbsent(Objects.requireNonNull(key), child) == null)
			changes.collect(recent, this);
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
