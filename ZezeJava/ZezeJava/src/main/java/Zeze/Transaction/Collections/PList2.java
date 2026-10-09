package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.HasManagedException;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

/** 事务 List（2系）：Bean 值受管，原位修改同样记账。 */
@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class PList2<V extends Bean> extends PList<V> {
	protected final @NotNull List2Meta<V> meta;

	public PList2(@NotNull Class<V> valueClass) {
		meta = List2Meta.get(valueClass);
	}

	public PList2(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) { // only for DynamicBean value
		meta = List2Meta.createDynamic(get, create);
	}

	public PList2(@NotNull List2Meta<V> meta) {
		this.meta = meta;
	}

	public @NotNull V createValue() {
		try {
			return (V)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	@Override
	public boolean add(@NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			// 先取写权限后挂接：无当前事务/记录不可写时getCurrentVerifyWrite抛出，
			// 若先挂接（initRootInfoWithRedo改写bean归属，普通字段写不受回滚保护），
			// 输入bean永久污染（isManaged=true，复用抛HasManagedException）。
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			item.initRootInfoWithRedo(rootInfo, this);
			return listLog.add(item);
		}
		list = list.plus(item);
		return true;
	}

	@Override
	public boolean remove(@NotNull Object item) {
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
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
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = Empty.vector();
	}

	@Override
	public @NotNull V set(int index, @NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			// initRootInfoWithRedo直接改写bean归属且不受事务回滚保护，越界IOOBE必须在挂接前
			// 抛出（TreePVector.get/with的检查在listLog.set内、挂接之后），否则调用方catch后
			// 复用bean携带脏归属——复用抛HasManagedException，原位字段修改的日志被encode期
			// 静默丢弃。
			var cur = getList();
			if (index < 0 || index >= cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			item.initRootInfoWithRedo(rootInfo, this);
			return listLog.set(index, item);
		}
		V old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, @NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			// 同set，先验界（add合法域0<=index<=size）后挂接，越界IOOBE不得
			// 留下携带脏归属的bean。
			var cur = getList();
			if (index < 0 || index > cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			item.initRootInfoWithRedo(rootInfo, this);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public @NotNull V remove(int index) {
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		V old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends V> items) {
		if (items.isEmpty())
			return false;
		if (items instanceof PList2)
			items = ((PList2<? extends V>)items).getList(); // more stable
		if (isManaged()) {
			// 双循环（对齐PList1.addAll"先全量校验、后入日志"）：单循环"边验边改"时，靠后null
			// 抛出前靠前item的initRootInfoWithRedo已改写——普通字段写不受事务回滚保护，
			// 调用方catch后复用bean即携带脏归属。
			for (V v : items) {
				if (v == null) // 对齐非托管分支与add/PList1：null在initRootInfoWithRedo处解引用NPE，先验拒绝
					throw new IllegalArgumentException("null item");
				if (v.isManaged()) // 批量原子性：后段项的HasManagedException不得留下已挂接的前段项
					throw new HasManagedException();
			}
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			for (V v : items)
				v.initRootInfoWithRedo(rootInfo, this);
			return listLog.addAll(items);
		}
		for (V v : items) {
			if (v == null)
				throw new IllegalArgumentException("null item");
		}
		list = list.plusAll(items);
		return true;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		if (c.isEmpty() || isEmpty())
			return false;
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
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
	public void replaceAll(@NotNull UnaryOperator<V> operator) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<V>(size());
		if (isManaged()) {
			// 双循环（对齐PList1.replaceAll"先全量求值校验、后入日志"）：operator只应用一次；
			// 单循环"边验边改"时，靠后null抛ISE前靠前newV的initRootInfoWithRedo已改写——
			// 普通字段写不受事务回滚保护，调用方catch后复用bean即携带脏归属。
			// origin快照只取一次，保证两轮循环元素配对（operator若改容器也不失配）。
			var origin = getList();
			for (V v : origin) {
				V newV = operator.apply(v);
				if (newV == null) // 对齐非托管分支：null在initRootInfoWithRedo或日志路径解引用NPE，先验拒绝
					throw new IllegalStateException("null item");
				if (newV != v && newV.isManaged()) // 批量原子性：预检归属
					throw new HasManagedException();
				tmpList.add(newV);
			}
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
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
				if (v == null)
					throw new IllegalStateException("null item");
				tmpList.add(v);
			}
			list = Empty.<V>vector().plusAll(tmpList);
		}
	}

	@Override
	public void sort(@Nullable Comparator<? super V> c) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<>(this);
		tmpList.sort(c);
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(tmpList);
		} else
			list = Empty.<V>vector().plusAll(tmpList);
	}

	@Override
	public @NotNull LogBean createLogBean() {
		return new LogList2<>(parent(), variableId(), this, list, meta);
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		var log = (LogList2<V>)_log;
		var tmp = list;
		for (var opLog : log.getOpLogs()) {
			tmp = switch (opLog.op) {
				case LogList1.OpLog.OP_MODIFY -> {
					opLog.value.initRootInfo(rootInfo, this);
					yield tmp.with(opLog.index, opLog.value);
				}
				case LogList1.OpLog.OP_ADD -> {
					opLog.value.initRootInfo(rootInfo, this);
					yield tmp.plus(opLog.index, opLog.value);
				}
				case LogList1.OpLog.OP_REMOVE -> tmp.minus(opLog.index);
				case LogList1.OpLog.OP_CLEAR -> Empty.vector();
				default -> tmp;
			};
		}

		// apply changed
		for (var e : log.getChanged().entrySet()) {
			// 正常重放下index必在界内（LogList2.encode只保留最终列表中存在的bean并按最终列表计算index），
			// 越界只能是先行分歧（日志丢失/重复/交错应用）：直接get抛IndexOutOfBoundsException，
			// 由驱动方裁决——raft路径Rocks.followerApply统一catch+fatalKill（镜像实现CollList2同款）；
				// History回放路径批中断而非warn+skip尽力而为。抛出只证明应用路径无硬分歧，
			// 不证明最终数值一致：错位应用（stale index落在界内指向错误元素）不抛异常、
			// 历史缺失不抛异常，仍由Verify.verifyAndClear全量对账兜底。抛出时list未提交，容器保持原状。
			tmp.get(e.getValue().value).followerApply(e.getKey());
		}
		list = tmp;
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		for (V v : list)
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		for (V v : list)
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public @NotNull PList2<V> copy() {
		// 深拷贝值Bean：浅拷贝共享原记录树内的受管活Bean，经ReadOnly.copy()交出的“副本”
		// 拿到的是可变后门（copy.get(k).setField改的是原记录），且副本元素再put进受管容器
		// 抛HasManagedException——与CollOne.copy()/生成代码Bean.assign的深拷贝语义对齐。
		var copy = new PList2<>(meta);
		var newList = org.pcollections.Empty.<V>vector();
		for (var v : getList())
			newList = newList.plus((V)v.copy());
		copy.list = newList;
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		for (V v : tmp)
			v.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		try {
			for (int i = bb.ReadUIntPositive(); i > 0; i--) {
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				add(v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public <D extends Data> void addAllData(@NotNull Collection<D> dataList) {
		Bean.toBeanList(dataList, this);
	}

	public <D extends Data> void toDataList(@NotNull Collection<D> dataList) {
		Bean.toDataList(getList(), dataList);
	}

	public <D extends Data> @NotNull ArrayList<D> toDataList() {
		var beanList = getList();
		var dataList = new ArrayList<D>(beanList.size());
		Bean.toDataList(beanList, dataList);
		return dataList;
	}
}
