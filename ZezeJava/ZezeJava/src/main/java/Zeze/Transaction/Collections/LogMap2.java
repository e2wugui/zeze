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
				K pkey = (K)c.getThis().mapKey();
				if (!getReplaced().containsKey(pkey) && !getRemoved().contains(pkey))
					changedWithKey.put(pkey, c);
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void mergeChangedToReplaced() {
		if (buildChangedWithKey()) {
			for (var e : changedWithKey.entrySet()) {
				getReplaced().put(e.getKey(), (V)e.getValue().getThis());
			}
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		buildChangedWithKey();

		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = meta.keyEncoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}

		// super.encode(bb);
		bb.WriteUInt(getReplaced().size());
		for (var p : getReplaced().entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			p.getValue().encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (var r : getRemoved())
			keyEncoder.accept(bb, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changedWithKey.clear();
		var keyDecoder = meta.keyDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = new LogBean();
			value.decode(bb);
			changedWithKey.put(key, value);
		}

		// super.decode(bb);
		getReplaced().clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			V value;
			try {
				value = (V)meta.valueFactory.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				Task.forceThrow(e);
				return; // never run here
			}
			value.decode(bb);
			getReplaced().put(key, value);
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
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, getReplaced());
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
