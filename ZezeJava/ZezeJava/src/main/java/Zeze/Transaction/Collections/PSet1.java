package Zeze.Transaction.Collections;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;

public class PSet1<V> extends PSet<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public PSet1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.LogSet1<" + Reflect.GetStableName(valueClass) + '>');
	}

	private PSet1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.Add(item);
		}
		var newSet = _set.plus(item);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.Remove((V)item);
		}
		var newSet = _set.minus(item);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.AddAll(c);
		}
		var newSet = _set.plusAll(c);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (isManaged()) {
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.RemoveAll((Collection<? extends V>)c);
		}
		var newSet = _set.minusAll(c);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			setLog.Clear();
		} else
			_set = org.pcollections.Empty.set();
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogSet1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_set);
		return log;
	}

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogSet1<V>)_log;
		_set = _set.plusAll(log.getAdded()).minusAll(log.getRemoved());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void ResetChildrenRootInfo() {
	}

	@Override
	public PSet1<V> CopyBean() {
		var copy = new PSet1<>(logTypeId, valueCodecFuncs);
		copy._set = _set;
		return copy;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		var tmp = getSet();
		bb.WriteUInt(tmp.size());
		var encoder = valueCodecFuncs.encoder;
		for (var e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		clear();
		var decoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
