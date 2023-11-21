package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogMap2<");

	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> changedWithKey = new HashMap<>(); // changed with key. using in encode/decode followerApply
	private final MethodHandle valueFactory;

	public LogMap2(Class<K> keyClass, Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	LogMap2(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		super(typeId, keyCodecFuncs, null);
		this.valueFactory = valueFactory;
	}

	public final Set<LogBean> getChanged() {
		return changed;
	}

	public final HashMap<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap2<K, V>(getTypeId(), keyCodecFuncs, valueFactory);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void encode(ByteBuffer bb) {
		if (getValue() != null) {
			for (var c : changed) {
				Object pkey = c.getThis().mapKey();
				//noinspection SuspiciousMethodCalls
				if (!getPutted().containsKey(pkey) && !getRemoved().contains(pkey))
					changedWithKey.put((K)pkey, c);
			}
		}
		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = keyCodecFuncs.encoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}

		// super.encode(bb);
		bb.WriteUInt(getPutted().size());
		for (var p : getPutted().entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			p.getValue().encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (var r : getRemoved())
			keyEncoder.accept(bb, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(IByteBuffer bb) {
		changedWithKey.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = new LogBean();
			value.decode(bb);
			changedWithKey.put(key, value);
		}

		// super.decode(bb);
		getPutted().clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			V value;
			try {
				value = (V)valueFactory.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				Task.forceThrow(e);
				return; // never run here
			}
			value.decode(bb);
			getPutted().put(key, value);
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
		ByteBuffer.BuildSortedString(sb, getPutted());
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
