package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;
import org.jspecify.annotations.Nullable;

import Zeze.Collections.Persistents;

/** 事务 Set（1系）：不可变值按值拷贝记账；框架无 PSet2。 */
public class PSet1<V> extends PSet<V> {
	protected final Set1Meta<V> meta;

	public PSet1(Class<V> valueClass) {
		meta = Set1Meta.get(valueClass);
	}

	// meta须来自Set1Meta工厂（公开唯一构建入口，已过Bean拦截），供生成代码常量化/调用点静态持有。
	public PSet1(Set1Meta<V> meta) {
		this.meta = meta;
	}

	@Override
	public boolean add(V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return setLog.add(item);
		}
		var newSet = set.plus(item);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			//noinspection unchecked
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
		if (c.isEmpty())
			return false;
		if (c instanceof PSet1)
			c = ((PSet1<? extends V>)c).getSet(); // more stable
		for (V v : c) {
			//noinspection ConstantValue
			if (v == null)
				throw new IllegalArgumentException("null item");
		}

		if (isManaged()) {
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return setLog.addAll(c);
		}
		// MapPSet.plusAll恒新建包装（same-instance前提不成立），逐项plus判定真实变化：
		// 单项plus无变化时返回同一实例，全已存在时正确返回false。
		var newSet = set;
		for (V v : c)
			newSet = newSet.plus(v);
		if (newSet == set)
			return false;
		set = newSet;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c.isEmpty() || isEmpty())
			return false;
		if (isManaged()) {
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			//noinspection unchecked
			return setLog.removeAll((Collection<? extends V>)c);
		}
		// 同addAll：MapPSet.minusAll恒新建包装，逐项minus判定真实变化。
		var newSet = set;
		for (Object v : c)
			newSet = newSet.minus(v);
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
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			setLog.clear();
		} else
			set = Persistents.set();
	}

	@Override
	public void followerApply(Log _log) {
		//noinspection unchecked
		var log = (LogSet1<V>)_log;
		var modified = log.getModified();
		if (modified.isEmpty())
			return;
		// 单遍分区批量应用（同PMap1.followerApply）：PRESENT槽位归plus批，null槽位归minus批。
		ArrayList<V> adds = null;
		ArrayList<V> removes = null;
		for (var e : modified.entrySet()) {
			if (e.getValue() != null) {
				if (adds == null)
					adds = new ArrayList<>(modified.size());
				adds.add(e.getKey());
			} else {
				if (removes == null)
					removes = new ArrayList<>(modified.size());
				removes.add(e.getKey());
			}
		}
		if (removes != null)
			set = set.minusAll(removes);
		if (adds != null)
			set = set.plusAll(adds);
	}

	public void assign(PSet1<V> pset) {
		var items = pset.getSet();
		if (isManaged()) {
			//noinspection unchecked
			var setLog = (LogSet<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			setLog.clear();
			setLog.addAll(items);
		} else
			set = items;
	}

	@Override
	public PSet1<V> copy() {
		var copy = new PSet1<>(meta);
		copy.set = getSet();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getSet();
		bb.WriteUInt(tmp.size());
		var encoder = meta.valueCodec.encoder;
		for (V e : tmp) {
			encoder.accept(bb, e);
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		var decoder = meta.valueCodec.decoder;
		int n = bb.ReadUIntPositive();
		if (n == 0)
			return;
		// JDK暂存+单发批量（语义等价，见PList1.decode注）。
		var items = new ArrayList<V>(initialDecodeCapacity(n));
		for (int i = n; i > 0; i--) {
			items.add(decoder.apply(bb));
		}
		addAll(items);
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return mode == ChangeCaptureMode.NONE ? new LogSet<>(this, set)
			: new LogSet1Delta<>(this, set);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogSet1<>(variableId(), this, meta, null);
		//noinspection unchecked
		var delta = (LogSet1Delta<V>)finalLog;
		return new LogSet1<>(variableId(), this, meta, delta.getModified());
	}
}
