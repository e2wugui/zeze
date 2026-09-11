package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import harness.Fast;
import org.junit.jupiter.api.Test;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.OutInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND3-19：LogList2.encode 写出的 changed.index 是最终列表坐标（编码时重算），而
 * CollList2.followerApply 旧实现的 newest 启发式收集的是 op 发生时刻坐标——两个坐标系
 * 在"op之后有remove位移"或"加删相消撞号"时错位：
 * 1. 双重应用：add(C)+remove(0)+编辑C：newest={4}不含C的最终index 3 → 冗余changed被
 *    follower再应用一次（OP_ADD携带的value编码于提交时刻、已含编辑后状态）；
 * 2. 编辑丢失：add(3,X)+remove(3)+编辑D：newest={3}撞上D的最终index 3 → D的编辑被误跳过。
 * 修复（对齐经典 Transaction.Collections.LogList2）：encode 侧维护 addSet 身份过滤，
 * 结构op携带bean的冗余changed条目不再写进日志；follower 侧删除 newest 直接全量应用。
 * 纯单元驱动：手工begin事务并在事务存活期encode（与生产appendLog一致，op携带value
 * 穿日志编码出提交时刻状态），手工镜像 _final_commit_ 在容器内的collect子链。
 */
@Fast
public class TestList2FollowerApplyCoordinate {
	static {
		// decode侧LogBean.decode -> Log.create(typeId)需要工厂：内部list<int>的LogList1<Integer>。
		Rocks.registerLog(() -> new LogList1<>(Integer.class));
	}

	/** list元素bean：内含list<int>（非幂等容器，双重应用可观测）。 */
	public static final class BItem extends Bean {
		private final CollList1<Integer> _l = new CollList1<>(Integer.class);

		public BItem() {
			_l.variableId(1);
		}

		public CollList1<Integer> getL() {
			return _l;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			_l.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_l.decode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_l.initRootInfo(root, this);
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_l.followerApply(vlog);
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_l.leaderApplyNoRecursive(vlog);
		}
	}

	/** 记录值bean：含 CollList2&lt;BItem&gt;。 */
	public static final class BListBean extends Bean {
		private final CollList2<BItem> _list;

		public BListBean() {
			_list = new CollList2<>(BItem.class);
			_list.variableId(1);
		}

		public CollList2<BItem> getList() {
			return _list;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
			_list.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_list.decode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_list.initRootInfo(root, this);
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				if (vlog.getVariableId() == 1)
					_list.followerApply(vlog);
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			if (vlog.getVariableId() == 1)
				_list.leaderApplyNoRecursive(vlog);
		}
	}

	private static BListBean newBListBeanWith(int n) {
		var bean = new BListBean();
		for (int i = 0; i < n; i++)
			bean.getList().add(new BItem()); // unmanaged：直接进字段
		return bean;
	}

	// 手工begin事务执行action（返回编辑过的item），镜像_final_commit_在容器内的collect
	// 子链（叶子日志包进item的LogBean放进changed）。调用方在事务存活期使用返回的日志
	// （encode穿日志读到提交时刻状态），用完负责Transaction.destroy()。
	@SuppressWarnings("unchecked")
	private static LogList2<BItem> collectWriterLog(BListBean writer, Function<CollList2<BItem>, List<BItem>> action) {
		writer.getList().initRootInfo(
				new Record.RootInfo(new Record<>(Integer.class), new TableKey("TestList2FollowerApplyCoordinate", 1)), writer);
		var t = Transaction.create();
		t.begin();
		try {
			var edited = action.apply(writer.getList());
			var listLog = (LogList2<BItem>)t.getLog(writer.objectId() + 1);
			assertNotNull(listLog);
			for (var item : edited) {
				var leafLog = t.getLog(item.objectId() + 1);
				assertNotNull(leafLog, "edited item must has leaf log");
				var lb = item.createLogBean(); // LogBean{this=item}
				lb.getVariablesOrNew().put(leafLog.getVariableId(), leafLog);
				listLog.getChanged().put(lb, new OutInt());
			}
			return listLog;
		} catch (Throwable e) {
			Transaction.destroy();
			throw e;
		}
	}

	private static LogList2<BItem> encodeDecode(LogList2<BItem> listLog) {
		var bb = ByteBuffer.Allocate();
		listLog.encode(bb);
		var decoded = new LogList2<>(BItem.class);
		decoded.decode(ByteBuffer.Wrap(bb.CopyIf()));
		return decoded;
	}

	@Test
	public void testAddThenRemoveShiftDoubleApply() {
		var writer = newBListBeanWith(4); // [A,B,C,D]
		var newbie = new BItem();
		try {
			var listLog = collectWriterLog(writer, list -> {
				list.add(newbie);   // ADD@4
				list.remove(0);     // REMOVE@0：newbie最终index=3
				newbie.getL().add(9);
				return List.of(newbie);
			});

			// leader提交时刻状态：穿日志读取（与生产事务内一致）
			assertEquals(4, writer.getList().size());
			assertEquals(1, writer.getList().get(3).getL().size());
			assertEquals(9, writer.getList().get(3).getL().get(0));

			var decoded = encodeDecode(listLog);

			// 核心（修复前红）：结构op携带bean（∈addSet）的冗余changed不得编码进日志
			assertTrue(decoded.getChanged().isEmpty());

			// follower应用：ADD携带newbie提交时刻状态（内部[9]），不再叠加changed
			var follower = newBListBeanWith(4);
			follower.getList().followerApply(decoded);
			assertEquals(4, follower.getList().size());
			assertEquals(1, follower.getList().get(3).getL().size()); // 修复前红：[9,9]
			assertEquals(9, follower.getList().get(3).getL().get(0));
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public void testAddRemoveSameSlotLostEdit() {
		var writer = newBListBeanWith(4); // [A,B,C,D]
		try {
			var edited = new ArrayList<BItem>();
			var listLog = collectWriterLog(writer, list -> {
				var d = list.get(3); // 事务日志建立前读取：字段里的pre-image item
				var x = new BItem();
				list.add(3, x);      // ADD@3
				list.remove(3);      // REMOVE@3（删掉x）
				d.getL().add(7);     // 编辑d（无结构op，编辑只存在于changed）
				edited.add(d);
				return edited;
			});
			var d = edited.get(0);

			// leader提交时刻状态：final列表里的d仍是pre-image的那个（加删相消，列表不变）
			assertEquals(4, writer.getList().size());
			assertSame(d, writer.getList().get(3));
			assertEquals(1, writer.getList().get(3).getL().size());

			var decoded = encodeDecode(listLog);

			// d不是结构op携带的bean：它的编辑必须保留在changed里
			assertEquals(1, decoded.getChanged().size());

			// follower应用：d的编辑不得被op时index启发式误跳过（修复前红：newest={3}撞号）
			var follower = newBListBeanWith(4);
			follower.getList().followerApply(decoded);
			assertEquals(4, follower.getList().size());
			assertEquals(1, follower.getList().get(3).getL().size()); // 修复前红：编辑丢失[]
			assertEquals(7, follower.getList().get(3).getL().get(0));
		} finally {
			Transaction.destroy();
		}
	}
}
