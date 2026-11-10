package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.UnaryOperator;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;
import Zeze.Util.Task;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;

/** 事务 List（2系）：Bean 值受管，原位修改同样记账。 */
public class PList2<V extends Bean> extends PList<V> {
	protected final List2Meta<V> meta;

	public PList2(Class<V> valueClass) {
		meta = List2Meta.get(valueClass);
	}

	public PList2(List2Meta<V> meta) {
		this.meta = meta;
	}

	public V createValue() {
		try {
			//noinspection unchecked
			return (V)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	@Override
	public boolean add(V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		DynamicField.checkValueOwner(meta.dynamicField, item);

		if (isManaged()) {
			// 先校验写权限，避免失败时把输入留在受管状态。
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			item.initRootInfoWithRedo(rootInfo, this);
			return listLog.add(item);
		}
		list = list.plus(item);
		return true;
	}

	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			//noinspection unchecked
			return listLog.remove((V)item);
		}
		var newList = list.minus(item);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@Override
	public void clear() {
		if (isEmpty())
			return;
		if (isManaged()) {
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			listLog.clear();
		} else
			list = Persistents.vector();
	}

	@Override
	public V set(int index, V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		DynamicField.checkValueOwner(meta.dynamicField, item);

		if (isManaged()) {
			// 挂接不会随事务回滚，须先校验索引。
			var cur = getList();
			if (index < 0 || index >= cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			item.initRootInfoWithRedo(rootInfo, this);
			return listLog.set(index, item);
		}
		V old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		DynamicField.checkValueOwner(meta.dynamicField, item);

		if (isManaged()) {
			// 先校验插入索引，避免越界时改动 Bean 的归属。
			var cur = getList();
			if (index < 0 || index > cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			item.initRootInfoWithRedo(rootInfo, this);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			return listLog.remove(index);
		}
		V old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public boolean addAll(Collection<? extends V> items) {
		if (items.isEmpty())
			return false;
		if (items instanceof PList2)
			items = ((PList2<? extends V>)items).getList(); // more stable
		if (isManaged()) {
			// 先校验整批输入，避免中途失败时留下已挂接的 Bean。
			for (V v : items) {
				//noinspection ConstantValue
				if (v == null) // 对齐非托管分支与add/PList1：null在initRootInfoWithRedo处解引用NPE，先验拒绝
					throw new IllegalArgumentException("null item");
				DynamicField.checkValueOwner(meta.dynamicField, v);
				// 已受管项不预检：挂接循环内经initRootInfoWithRedo→throwAbort整体中止（277e6e501 Abort契约）
			}
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			for (V v : items)
				v.initRootInfoWithRedo(rootInfo, this);
			return listLog.addAll(items);
		}
		for (V v : items) {
			//noinspection ConstantValue
			if (v == null)
				throw new IllegalArgumentException("null item");
			DynamicField.checkValueOwner(meta.dynamicField, v);
		}
		list = list.plusAll(items);
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c.isEmpty() || isEmpty())
			return false;
		if (isManaged()) {
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			//noinspection unchecked
			return listLog.removeAll((Collection<? extends V>)c);
		}
		// minusAll同为"逐元素删首个出现"，自实现契约语义（删全部出现），
		// 与托管路径（LogList2继承LogList1的removeAll）行为对齐；从高索引往低删保持非命中元素顺序。
		// 命中检测走线性equals：生成bean覆写equals但不覆写hashCode（值等哈希不等），HashSet漏命中。
		var hit = new ArrayList<>(c);
		var newList = list;
		for (var i = newList.size() - 1; i >= 0; i--)
			if (hit.contains(newList.get(i)))
				newList = newList.minus(i);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@Override
	public void replaceAll(UnaryOperator<V> operator) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<V>(size());
		if (isManaged()) {
			// 对同一快照求值，每项只调用一次 operator。全部校验后才挂接。
			var origin = getList();
			for (V v : origin) {
				V newV = operator.apply(v);
				//noinspection ConstantValue
				if (newV == null) // 对齐非托管分支：null在initRootInfoWithRedo或日志路径解引用NPE，先验拒绝
					throw new IllegalStateException("null item");
				// 已受管项不预检：挂接循环内经initRootInfoWithRedo→throwAbort整体中止（277e6e501 Abort契约）
				DynamicField.checkValueOwner(meta.dynamicField, newV);
				tmpList.add(newV);
			}
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			int i = 0;
			for (V v : origin) {
				V newV = tmpList.get(i++);
				if (newV != v)
					newV.initRootInfoWithRedo(rootInfo, this);
			}
			listLog.clear();
			listLog.addAll(tmpList);
		} else {
			for (V v : this) {
				v = operator.apply(v);
				//noinspection ConstantValue
				if (v == null)
					throw new IllegalStateException("null item");
				DynamicField.checkValueOwner(meta.dynamicField, v);
				tmpList.add(v);
			}
			list = Persistents.<V>vector().plusAll(tmpList);
		}
	}

	@Override
	public void sort(@Nullable Comparator<? super V> c) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<>(this);
		tmpList.sort(c);
		if (isManaged()) {
			//noinspection unchecked
			var listLog = (LogList<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(this);
			listLog.clear();
			listLog.addAll(tmpList);
		} else
			list = Persistents.<V>vector().plusAll(tmpList);
	}

	@Override
	public void followerApply(Log _log) {
		//noinspection unchecked
		var log = (LogList2<V>)_log;
		DynamicField.checkTarget(log.dynamicField(), meta.dynamicField);
		var tmp = list;
		for (var opLog : log.operationsForRead()) {
			tmp = switch (opLog.op) {
				case LogList1.OpLog.OP_MODIFY -> {
					var value = opLog.requireValue();
					value.initRootInfo(rootInfo, this);
					yield tmp.with(opLog.index, value);
				}
				case LogList1.OpLog.OP_ADD -> {
					var value = opLog.requireValue();
					value.initRootInfo(rootInfo, this);
					yield tmp.plus(opLog.index, value);
				}
				case LogList1.OpLog.OP_REMOVE -> tmp.minus(opLog.index);
				case LogList1.OpLog.OP_CLEAR -> Persistents.vector();
				default -> tmp;
			};
		}

		// apply changed
		for (var e : log.changesForRead().entrySet()) {
			// 原位修改使用最终列表的索引。越界说明回放状态已分歧，应中断应用。
			tmp.get(e.getValue().value).followerApply(e.getKey());
		}
		list = tmp;
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (V v : list)
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		for (V v : list)
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public PList2<V> copy() {
		return copy(meta);
	}

	private PList2<V> copy(List2Meta<V> targetMeta) {
		// 深拷贝值，避免副本继续引用原记录的受管 Bean。
		var copy = new PList2<>(targetMeta);
		var newList = Persistents.<V>vector();
		for (var v : getList())
			newList = newList.plus(DynamicField.copyValue(targetMeta.dynamicField, v));
		copy.list = newList;
		return copy;
	}

	/** 深拷贝元素赋值（区别于1系assign的引用共享：2系值可变，别名会让两个容器互相可见修改）。 */
	public void assign(PList2<V> plist) {
		if (this == plist)
			return; // 自赋值：先clear后copy时源已空，原有内容会永久丢失
		// 使用目标映射校验和复制，成功后才清空。
		var copy = meta.dynamicField != null ? plist.copy(meta) : plist.copy();
		clear();
		addAll(copy);
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		for (V v : tmp)
			v.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		int n = bb.ReadUIntPositive();
		if (n == 0)
			return;
		// JDK暂存+单发批量（语义等价，见PList1.decode注）；try只包valueFactory的invoke。
		var items = new ArrayList<V>(initialDecodeCapacity(n));
		try {
			for (int i = n; i > 0; i--) {
				//noinspection unchecked
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				items.add(v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		addAll(items);
	}

	public <D extends Data> void addAllData(Collection<D> dataList) {
		Bean.toBeanList(dataList, this);
	}

	public <D extends Data> void toDataList(Collection<D> dataList) {
		Bean.toDataList(getList(), dataList);
	}

	public <D extends Data> ArrayList<D> toDataList() {
		var beanList = getList();
		var dataList = new ArrayList<D>(beanList.size());
		Bean.toDataList(beanList, dataList);
		return dataList;
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return mode == ChangeCaptureMode.NONE ? new LogList<>(this, list)
			: new LogList2Delta<>(this, list);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		if (finalLog == null)
			return new LogList2<>(variableId(), this, meta, null, null);
		//noinspection unchecked
		var delta = (LogList2Delta<V>)finalLog;
		return new LogList2<>(variableId(), this, meta, delta.getOpLogs(), delta.addedIdentities());
	}
}
