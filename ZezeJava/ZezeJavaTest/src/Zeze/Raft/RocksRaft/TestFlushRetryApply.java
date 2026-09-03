package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Rocks.followerApply 对"内存已应用但flush失败"条目的幂等重试（FND-R2-4）。
 * apply 流程是"先改内存、后flush"：flush 抛 RocksDBException 时内存已是最终
 * 状态而 lastApplied 未推进，之后的重试经 readLog 解码走增量 followerApply，
 * 把 OP_ADD 等按索引增量日志叠加到已应用状态上——双重应用并被后续提交复制。
 * 修复：flush 失败包装为 Rocks.FlushException，并把已应用的记录集合记到
 * pendingFlushApplies（key=RaftLog.Index，校验term）；重试命中时跳过内存变更
 * 只重试flush。leaderApply 同机制（Transaction.leaderApply）。
 * 这里直接构造 Rocks（不 start server，无网络与选举流量），用手写的含
 * CollList1（增量重放不幂等）的 bean 复现双重应用场景：
 * 1. 常规 apply（无补偿记录）正常应用并落盘；
 * 2. 模拟"上次apply内存已变更但flush失败"（putPendingFlush 记录记录集合），
 *    重试同一(term,index)条目必须只重试flush、不重放增量日志；
 * 3. 同index不同term（旧条目被截断后新条目复用index）必须忽略过期补偿记录，
 *    按全新条目应用。
 * 真实的 flush 失败注入（磁盘满等）与集群级重试序列属故障注入/integration量级，
 * 由提交信息中的推演核查覆盖；flush 失败后 follower 在后续 AppendEntries 上
 * 的重试触发见 LogSequence.followerOnAppendEntries 的 commitIndex>lastApplied 分支。
 */
@Fast
public class TestFlushRetryApply {
	private static final String raftName = "127.0.0.1:17650";
	private static final String dbHome = "TestFlushRetryApply.raft";
	private static final String templateName = "tFlushRetry";

	// 含 CollList1 的最小 bean：list 的增量日志（LogList1 OP_ADD 按索引追加）
	// 重放不幂等，是 FND-R2-4 双重应用的载体。
	public static final class BListBean extends Bean {
		private final CollList1<Integer> _list;

		public BListBean() {
			_list = new CollList1<>(Integer.class);
			_list.variableId(1);
		}

		public CollList1<Integer> getList() {
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
		public void decode(Zeze.Serialize.IByteBuffer bb) {
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

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17650" DbHome="TestFlushRetryApply.raft">
					<node Host="127.0.0.1" Port="17650"/>
					<node Host="127.0.0.1" Port="17651"/>
					<node Host="127.0.0.1" Port="17652"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	private static Rocks newRocks() throws Exception {
		var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
		rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
		return rocks;
	}

	// 在过程中执行修改并捕获收集到的 Changes：appendLog 因非leader抛 RaftRetry，
	// 过程按失败返回；Changes 已在 _final_commit_ 里收集完成（保存在事务对象上）。
	private static Changes captureChanges(Rocks rocks, FuncLong func) throws Exception {
		final Transaction[] ts = new Transaction[1];
		var rc = rocks.newProcedure(() -> {
			ts[0] = Transaction.getCurrent();
			return func.call();
		}).call();
		assertEquals(Zeze.Transaction.Procedure.RaftRetry, rc); // not leader
		var changes = ts[0].getChanges();
		assertNotNull(changes);
		return changes;
	}

	// 直写存储层做初始数据（绕过事务，模拟上个条目已成功应用落盘的状态）。
	private static void seedStorage(Table<Integer, BListBean> table, int key, Integer... items) throws Exception {
		var seed = table.newValue();
		for (var item : items)
			seed.getList().add(item); // 未托管，直接修改
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var valBB = ByteBuffer.Allocate();
		seed.encode(valBB);
		table.getRocksTable().put(keyBB.CopyIf(), valBB.CopyIf());
	}

	// 读存储层的最终值（不经缓存，验证flush真的落盘）。
	private static List<Integer> readStorage(Table<Integer, BListBean> table, int key) throws Exception {
		var keyBB = ByteBuffer.Allocate();
		table.encodeKey(keyBB, key);
		var bytes = table.getRocksTable().get(keyBB.CopyIf());
		var out = new ArrayList<Integer>();
		if (bytes != null) {
			var value = table.newValue();
			value.decode(ByteBuffer.Wrap(bytes));
			for (var item : value.getList())
				out.add(item);
		}
		return out;
	}

	@Test
	public void testFlushFailRetryIsIdempotent() throws Exception {
		try (var rocks = newRocks()) {
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			seedStorage(table, 1, 10, 20); // 初始 [10,20]

			// Edit状态的增量修改：list.add(30) -> LogList1 OP_ADD。
			var editChanges = captureChanges(rocks, () -> {
				table.getOrAdd(1).getList().add(30);
				return 0L;
			});
			// 真实路径中 leader 的 saveLog 先走 Changes.encode（内部对每个 Record 调
			// setTableByName 解析出 table；follower 路径则在 decode 里做同样的事）。
			// 捕获的Changes只经历了collect，这里补上encode步骤，使后续直接调用
			// followerApply 时 Record.table 已就绪。
			editChanges.encode(ByteBuffer.Allocate());
			var holder = new RaftLog(1, 2, editChanges);

			// 1. 常规apply：增量重放一次，[10,20] -> [10,20,30]，落盘。
			rocks.followerApply(editChanges, holder);
			assertEquals(List.of(10, 20, 30), readStorage(table, 1));
			assertNull(rocks.takePendingFlush(2, 1)); // 无补偿记录

			// 2. 模拟"上次apply已完成内存变更但flush失败"：生产代码在 FlushException
			//    时把已应用的记录集合记入 pendingFlushApplies。重试同一(term,index)：
			//    必须跳过增量重放（否则[10,20,30,30]双重应用）只重试flush。
			var record = table.getLruCache().get(1);
			assertNotNull(record);
			rocks.putPendingFlush(2, 1, List.of(record));
			rocks.followerApply(editChanges, holder);
			assertEquals(List.of(10, 20, 30), readStorage(table, 1)); // 修复前：[10,20,30,30]
			assertNull(rocks.takePendingFlush(2, 1)); // 补偿记录被消费

			// 3. 同index不同term（旧条目被截断、新条目复用index）：过期补偿必须被
			//    忽略并按全新条目应用——此时增量重放是两个不同条目的正常语义。
			rocks.putPendingFlush(2, 1, List.of(record)); // term=1 的过期补偿
			var holderTerm2 = new RaftLog(2, 2, editChanges);
			rocks.followerApply(editChanges, holderTerm2);
			assertEquals(List.of(10, 20, 30, 30), readStorage(table, 1)); // 全新应用
			assertNull(rocks.takePendingFlush(2, 2)); // 无残留
		}
	}

	// putPendingFlush/takePendingFlush 的 term 校验与消费语义（补偿机制的独立单元）。
	@Test
	public void testPendingFlushTermValidation() throws Exception {
		try (var rocks = newRocks()) {
			rocks.putPendingFlush(5, 1, List.of());
			assertNull(rocks.takePendingFlush(5, 2)); // term不匹配：返回null并丢弃过期记录
			assertNull(rocks.takePendingFlush(5, 1)); // 已被上一行消费

			rocks.putPendingFlush(6, 3, List.of());
			assertNotNull(rocks.takePendingFlush(6, 3)); // term匹配：返回记录集合
		}
	}
}
