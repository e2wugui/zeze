package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import harness.Extra;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.BRefused;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.RpcTimeoutException;
import Zeze.Raft.RaftRpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** prepare失败后的undo必须经全部桶确认才删重放记录；在途登记始终释放，供既有redo接管。 */
@Extra
public class TestPrepareRollbackRetainsReplay {
	private static final String COMMIT_ROCKS_HOME_PROPERTY = "Dbh2CommitRocksHome";

	/** 发号可用的最小AbstractAgent（allocate本地递增，形态对齐TestAllocateTid128NoLockHold桩）。 */
	private static final class SequencedTidStubAgent extends AbstractAgent {
		private long next = 1;

		private SequencedTidStubAgent() {
			config = new Config();
		}

		@Override
		protected void allocate(AutoKey autoKey, int pool) {
			setCurrentAndCount(autoKey, next, pool);
			next += pool;
		}

		@Override
		protected boolean allocateAsync(String globalName, int allocCount,
										ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void waitReady() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void editService(BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
				BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Zeze.Component.Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}

	// 均为不可达端口（假agent不产生任何真实网络请求，仅作agents表键）。
	private static final String RAFT_REACHABLE = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19290"/>
				<node Host="127.0.0.1" Port="19291"/>
				<node Host="127.0.0.1" Port="19292"/>
			</raft>
			""";

	private static final String RAFT_UNREACHABLE = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19295"/>
				<node Host="127.0.0.1" Port="19296"/>
				<node Host="127.0.0.1" Port="19297"/>
			</raft>
			""";

	/**
	 * 假agent：prepareBatch立即成功（结果码0、无拒绝）；undoBatch成功或以RpcTimeoutException
	 * 完结（对齐真实rpc超时的完结形态，Rpc超时任务即setException(RpcTimeoutException)）。
	 */
	private static Dbh2Agent fakeAgent(String raftXml, java.util.concurrent.atomic.AtomicInteger undoFailure) throws Exception {
		return new Dbh2Agent(raftXml) {
			@Override
			public TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>> prepareBatch(
					BPrepareBatch.Data batch) {
				var tcs = new TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>();
				tcs.setResult(new PrepareBatch(batch));
				return tcs;
			}

			@Override
			public TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> undoBatch(long tid) {
				var tcs = new TaskCompletionSourceX<RaftRpc<BBatchTid.Data, EmptyBean.Data>>();
				if (undoFailure.get() == 1)
					tcs.setException(RpcTimeoutException.getInstance());
				else {
					var result = new UndoBatch();
					if (undoFailure.get() == 2)
						result.setResultCode(Zeze.Transaction.Procedure.RaftRetry);
					if (undoFailure.get() == 3)
						throw new IllegalStateException("undo send failed");
					tcs.setResult(result);
				}
				return tcs;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static void registerAgent(Dbh2AgentManager manager, String raft, Dbh2Agent agent) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("agents");
		field.setAccessible(true);
		var agents = (ConcurrentHashMap<String, Dbh2Agent>)field.get(manager);
		agents.put(raft, agent);
	}

	private static CommitRocks getCommitRocks(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("commit");
		field.setAccessible(true);
		return requireNonNull(((Commit)field.get(manager)).getRocks());
	}

	private static int inflightCount(CommitRocks rocks) throws Exception {
		Field field = CommitRocks.class.getDeclaredField("inFlightTids");
		field.setAccessible(true);
		return ((java.util.Set<?>)field.get(rocks)).size();
	}

	private static int countEntries(CommitRocks rocks, String fieldName) throws Exception {
		Field field = CommitRocks.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		var table = (Zeze.Util.RocksDatabase.Table)field.get(rocks);
		try (var it = table.iterator()) {
			int count = 0;
			for (it.seekToFirst(); it.isValid(); it.next())
				++count;
			return count;
		}
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 3})
	public void testPrepareTimeoutUndoFailureRetainsReplay(int failure, @TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		// CommitRocks的home只在本构造窗口读取（Dbh2CommitRocksHome+serverId），构造完成即还原。
		var priorHome = System.getProperty(COMMIT_ROCKS_HOME_PROPERTY);
		System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, tempDir.resolve("CommitRocks").toString());
		final Dbh2AgentManager manager;
		try {
			manager = new Dbh2AgentManager(new SequencedTidStubAgent(), new Config(), 833);
		} finally {
			if (null != priorHome)
				System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, priorHome);
			else
				System.clearProperty(COMMIT_ROCKS_HOME_PROPERTY);
		}
		try {
			// prepareMaxTime置-1：prepare全部成功后立即判超时，确定性进入超时分支
			//（配置解析强制prepareMaxTime>=rpcTimeout+1000，无法经配置到达，测试专用反射）。
			var maxTimeField = Dbh2Config.class.getDeclaredField("prepareMaxTime");
			maxTimeField.setAccessible(true);
			maxTimeField.setLong(manager.getDbh2Config(), -1);

			// RAFT_REACHABLE：prepare/undo均成功；RAFT_UNREACHABLE：仅state中存在，undo撞上失败。
			var undoFailure = new java.util.concurrent.atomic.AtomicInteger(failure);
			registerAgent(manager, RAFT_REACHABLE, fakeAgent(RAFT_REACHABLE, new java.util.concurrent.atomic.AtomicInteger()));
			registerAgent(manager, RAFT_UNREACHABLE, fakeAgent(RAFT_UNREACHABLE, undoFailure));

			var rocks = getCommitRocks(manager);

			var batches = new BPrepareBatches.Data();
			batches.getDatas().put(RAFT_REACHABLE, new BPrepareBatch.Data("", "database", "table1", null));

			var state = new BTransactionState.Data();
			state.getBuckets().add(RAFT_REACHABLE);
			state.getBuckets().add(RAFT_UNREACHABLE);

			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> rocks.prepare("127.0.0.1", 1, state, batches, null));
			Assertions.assertTrue(ex.getMessage().contains("max prepare time"),
					"超时异常必须是主异常（undo失败挂suppressed），实际: " + ex);
			Assertions.assertTrue(ex.getSuppressed().length > 0, "undo失败必须挂到超时异常的suppressed");

			// 失败请求结束后移除在途登记，持久记录交给redo。
			Assertions.assertEquals(0, inflightCount(rocks),
					"超时分支必须移除在途登记（bug时tid永久滞留inFlightTids，redoTimer终身免疫）");
			Assertions.assertEquals(1, countEntries(rocks, "commitIndex"), "undo未确认时必须保留重放索引");
			Assertions.assertEquals(1, countEntries(rocks, "commitPoint"), "undo未确认时必须保留参与方记录");
			undoFailure.set(0);
			var redo = CommitRocks.class.getDeclaredMethod("redoTimer");
			redo.setAccessible(true);
			redo.invoke(rocks);
			Assertions.assertEquals(0, countEntries(rocks, "commitIndex"), "恢复后redo应完成undo并删除索引");
			Assertions.assertEquals(0, countEntries(rocks, "commitPoint"), "全部确认后才删除记录");
		} finally {
			manager.stop();
		}
	}
}
