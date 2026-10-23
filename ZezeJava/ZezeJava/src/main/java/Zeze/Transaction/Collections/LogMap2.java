package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** PMap2 的消费日志：结构变化与最终仍在容器中的值 Bean 原位修改。 */
public class LogMap2<K, V extends Bean> extends LogBean {
	private final Meta2<K, V> meta;
	private @Nullable HashMap<K, V> replaced;
	private @Nullable Set<K> removed;
	private @Nullable HashMap<K, LogBean> changedWithKey;

	/** Borrow final notes without retaining the transaction state. decode always creates a fresh payload. */
	public LogMap2(int varId, @Nullable Bean self, Meta2<K, V> meta,
				   @Nullable HashMap<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, self);
		this.meta = meta;
		// 源端只借用最终事务增量；解码整体换成自己的容器，不清理借用数据。
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

	public final HashMap<K, V> getReplaced() {
		var values = replaced;
		return values != null ? values : (replaced = new HashMap<>());
	}

	public final Set<K> getRemoved() {
		var keys = removed;
		return keys != null ? keys : (removed = new HashSet<>());
	}

	public final HashMap<K, LogBean> getChangedWithKey() {
		var edits = changedWithKey;
		return edits != null ? edits : (changedWithKey = new HashMap<>());
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
		for (K key : removals)
			keyEncoder.accept(bb, key);
	}

	private void read(IByteBuffer bb) {
		var keyDecoder = meta.keyDecoder;
		int count = bb.ReadUInt();
		HashMap<K, LogBean> edits = null;
		if (count > 0) {
			edits = new HashMap<>();
			do {
				edits.put(keyDecoder.apply(bb), decodeLogBean(bb));
			} while (--count > 0);
		}
		count = bb.ReadUInt();
		HashMap<K, V> replacements = null;
		if (count > 0) {
			replacements = new HashMap<>();
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
		count = bb.ReadUInt();
		Set<K> removals = null;
		if (count > 0) {
			removals = new HashSet<>();
			do {
				removals.add(keyDecoder.apply(bb));
			} while (--count > 0);
		}
		// 读取完整载荷后再发布，既不清借用容器，也不发布半份解码结果。
		changedWithKey = edits;
		replaced = replacements;
		removed = removals;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addChild(Bean recent, Log vlog) {
		var child = (LogBean)vlog;
		var item = child.getThis();
		//noinspection DataFlowIssue
		K key = (K)item.mapKey();
		var source = (PMap2<K, V>)recent;
		if (replaced != null && replaced.containsKey(key) || removed != null && removed.contains(key)
			|| source.map.get(key) != item) {
			// 即便只剩陈旧引用编辑，也保留原来的空 Map 节点和源 Record 回调形态。
			return true;
		}
		return getChangedWithKey().putIfAbsent(Objects.requireNonNull(key), child) == null;
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

	public static <K, V extends Bean> LogMap2<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		var log = new LogMap2<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}
}
