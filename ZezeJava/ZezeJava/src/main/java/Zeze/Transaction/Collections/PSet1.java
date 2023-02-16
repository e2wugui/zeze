package Zeze.Transaction.Collections;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;

public class PSet1<V> extends PSet<V> {
	private static final long logTypeIdHead = Bean.hash64("Zeze.Transaction.Collections.LogSet1<");

	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public PSet1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Bean.hashLog(logTypeIdHead, valueClass);
	}

	private PSet1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return setLog.Add(item);
		}
		var newSet = set.plus(item);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return setLog.remove((V)item);
		}
		var newSet = set.minus(item);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return setLog.addAll(c);
		}
		var newSet = set.plusAll(c);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (isManaged()) {
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return setLog.removeAll((Collection<? extends V>)c);
		}
		var newSet = set.minusAll(c);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			setLog.clear();
		} else
			set = org.pcollections.Empty.set();
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogSet1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(set);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogSet1<V>)_log;
		set = set.plusAll(log.getAdded()).minusAll(log.getRemoved());
	}

	@Override
	public PSet1<V> copy() {
		var copy = new PSet1<>(logTypeId, valueCodecFuncs);
		copy.set = set;
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getSet();
		bb.WriteUInt(tmp.size());
		var encoder = valueCodecFuncs.encoder;
		for (var e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void decode(ByteBuffer bb) {
		clear();
		var decoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
