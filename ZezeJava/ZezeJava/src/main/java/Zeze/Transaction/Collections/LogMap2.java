package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Util.Reflect;

public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private final Set<LogBean> Changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> ChangedWithKey = new HashMap<>(); // changed with key. using in encode/decode FollowerApply
	private final MethodHandle valueFactory;

	public LogMap2(Class<K> keyClass, Class<V> valueClass) {
		super("Zeze.Raft.RocksRaft.LogMap2<" + Reflect.GetStableName(keyClass) + ", "
				+ Reflect.GetStableName(valueClass) + '>', keyClass, valueClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	LogMap2(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		super(typeId, keyCodecFuncs, null);
		this.valueFactory = valueFactory;
	}

	public final Set<LogBean> getChanged() {
		return Changed;
	}

	public final HashMap<K, LogBean> getChangedWithKey() {
		return ChangedWithKey;
	}

	@Override
	public Log BeginSavepoint() {
		var dup = new LogMap2<K, V>(getTypeId(), keyCodecFuncs, valueFactory);
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	private boolean built = false;

	public boolean BuildChangedWithKey() {
		if (false == built && getValue() != null) {
			built = true;
			for (var c : Changed) {
				Object pkey = c.getThis().getMapKey();
				//noinspection SuspiciousMethodCalls
				if (!getReplaced().containsKey(pkey) && !getRemoved().contains(pkey))
					ChangedWithKey.put((K)pkey, c);
			}
			return true;
		}
		return false;
	}

	public void MergeChangedToReplaced() {
		if (BuildChangedWithKey()) {
			for (var e : ChangedWithKey.entrySet()) {
				getReplaced().put(e.getKey(), (V)e.getValue().getThis());
			}
		}
	}

	@Override
	public void Encode(ByteBuffer bb) {
		BuildChangedWithKey();

		bb.WriteUInt(ChangedWithKey.size());
		var keyEncoder = keyCodecFuncs.encoder;
		for (var e : ChangedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().Encode(bb);
		}

		// super.Encode(bb);
		bb.WriteUInt(getReplaced().size());
		for (var p : getReplaced().entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			p.getValue().Encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (var r : getRemoved())
			keyEncoder.accept(bb, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void Decode(ByteBuffer bb) {
		ChangedWithKey.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = new LogBean();
			value.Decode(bb);
			ChangedWithKey.put(key, value);
		}

		// super.Decode(bb);
		getReplaced().clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			V value;
			try {
				value = (V)valueFactory.invoke();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			value.Decode(bb);
			getReplaced().put(key, value);
		}
		getRemoved().clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			getRemoved().add(keyDecoder.apply(bb));
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		if (Changed.add((LogBean)vlog))
			changes.Collect(recent, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, getReplaced());
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, Changed);
		return sb.toString();
	}
}
