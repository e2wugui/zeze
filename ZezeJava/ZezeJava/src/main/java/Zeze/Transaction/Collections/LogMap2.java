package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;

public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> changedWithKey = new HashMap<>(); // changed with key. using in encode/decode followerApply
	private boolean built;

	public LogMap2(Class<K> keyClass, Class<V> valueClass) {
		super(Meta2.getMap2Meta(keyClass, valueClass));
	}

	LogMap2(Meta2<K, V> meta) {
		super(meta);
	}

	public final Set<LogBean> getChanged() {
		return changed;
	}

	public final HashMap<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap2<>(meta);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	public boolean buildChangedWithKey() {
		if (!built && getValue() != null) {
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
	public void encode(ByteBuffer bb) {
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
	public void decode(ByteBuffer bb) {
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
			} catch (RuntimeException | Error e) {
				throw e;
			} catch (Throwable e) { // MethodHandle.invoke
				throw new RuntimeException(e);
			}
			value.decode(bb);
			getReplaced().put(key, value);
		}
		getRemoved().clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			getRemoved().add(keyDecoder.apply(bb));
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (changed.add((LogBean)vlog))
			changes.collect(recent, this);
	}

	@Override
	public String toString() {
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
