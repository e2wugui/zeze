package Zeze.Transaction.Collections;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PSet1<V> extends PSet<V> {
	protected final @NotNull Meta1<V> meta;

	public PSet1(@NotNull Class<V> valueClass) {
		meta = Meta1.getSet1Meta(valueClass);
	}

	private PSet1(@NotNull Meta1<V> meta) {
		this.meta = meta;
	}

	@Override
	public boolean add(@NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return setLog.add(item);
		}
		var newSet = set.plus(item);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(@NotNull Object item) {
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
	public boolean addAll(@NotNull Collection<? extends V> c) {
		if (c.isEmpty())
			return false;
		if (c instanceof PSet1)
			c = ((PSet1<? extends V>)c).getSet(); // more stable
		for (V v : c) {
			if (v == null)
				throw new IllegalArgumentException("null item");
		}

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
	public boolean removeAll(@NotNull Collection<?> c) {
		if (c.isEmpty() || isEmpty())
			return false;
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
		if (isEmpty())
			return;
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			setLog.clear();
		} else
			set = Empty.set();
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var log = new LogSet1<>(meta);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(set);
		return log;
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogSet1<V>)_log;
		set = set.minusAll(log.getRemoved()).plusAll(log.getAdded());
	}

	public void assign(@NotNull PSet1<V> pset) {
		var items = pset.getSet();
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			setLog.clear();
			setLog.addAll(items);
		} else
			set = items;
	}

	@Override
	public @NotNull PSet1<V> copy() {
		var copy = new PSet1<>(meta);
		copy.set = getSet();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getSet();
		bb.WriteUInt(tmp.size());
		var encoder = meta.valueEncoder;
		for (V e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		var decoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
