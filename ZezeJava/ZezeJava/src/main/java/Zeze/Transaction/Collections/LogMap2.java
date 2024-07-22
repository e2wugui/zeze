package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.pcollections.Empty;

public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> changedWithKey = new HashMap<>(); // changed with key. using in encode/decode followerApply
	private boolean built;

	public LogMap2(@NotNull Meta2<K, V> meta, @NotNull org.pcollections.PMap<K, V> value) {
		super(meta, value);
	}

	public LogMap2(@NotNull Class<K> keyClass, Class<V> valueClass) {
		super(Meta2.getMap2Meta(keyClass, valueClass), Empty.map());
	}

	// for dynamic
	public LogMap2(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super(Meta2.createDynamicMapMeta(keyClass, get, create), Empty.map());
	}

	public final @NotNull Set<LogBean> getChanged() {
		return changed;
	}

	public final @NotNull HashMap<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public @NotNull Log beginSavepoint() {
		var dup = new LogMap2<>(meta, getValue());
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		return dup;
	}

	public boolean buildChangedWithKey() {
		if (!built) {
			built = true;
			for (var c : changed) {
				@SuppressWarnings("unchecked")
				K k = (K)c.getThis().mapKey();
				if (!getReplaced().containsKey(k) // 新增的值是最新的，它的changed忽略。
						&& !getRemoved().contains(k) // 删除的值不用管了，它的changed忽略。
						&& getValue().containsKey(k) // 当前容器中必须存在key，加入以后产生了日志又被删除会违背这个条件。
				)
					changedWithKey.put(k, c);
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void mergeChangedToReplaced() {
		if (buildChangedWithKey()) {
			for (var e : changedWithKey.entrySet())
				getReplaced().put(e.getKey(), (V)e.getValue().getThis());
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		built = false;
		changedWithKey.clear();
		buildChangedWithKey();

		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = meta.keyEncoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			encodeLogBean(bb, e.getValue());
		}

		// super.encode(bb);
		bb.WriteUInt(getReplaced().size());
		for (var e : getReplaced().entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (K k : getRemoved())
			keyEncoder.accept(bb, k);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changedWithKey.clear();
		var keyDecoder = meta.keyDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			K k = keyDecoder.apply(bb);
			changedWithKey.put(k, decodeLogBean(bb));
		}
		built = true;

		// super.decode(bb);
		getReplaced().clear();
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				K k = keyDecoder.apply(bb);
				@SuppressWarnings("unchecked")
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				getReplaced().put(k, v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		getRemoved().clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			getRemoved().add(keyDecoder.apply(bb));
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (changed.add((LogBean)vlog))
			changes.collect(recent, this);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" replaced:");
		ByteBuffer.BuildSortedString(sb, getReplaced());
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" changed:");
		if (built)
			ByteBuffer.BuildSortedString(sb, changedWithKey);
		else
			ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
