package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.BRefused;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.ServiceConf;
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

/**
 * 远程提交模式的Commit超时契约（dbh2-03）三件：
 * ① prepare重定向循环必须在prepareMaxTime量级熔断——bug时循环无轮数/时长上限，
 * 总时长可远超rpcTimeout，客户端以rpcTimeout等待早判超时（确定失败语义）而
 * 服务端可能最终eCommitting提交成功（报失败实已提交）。
 * ② 应答解绑投递——ProcessCommitRequest在eCommitting持久化（decide）后必须立即
 * 应答；bug时同步等待commitBatch送达，应答被投递时延（rpcTimeout级）拖住甚至
 * 超时，客户端把"结果不确定"当确定失败。
 * ③ 客户端超时派生——远程分支Commit rpc超时必须=prepareMaxTime+rpcTimeout+5000
 * （覆盖服务端有界决策+应答），bug时传裸rpcTimeout（值级红）。
 * 形态对齐TestPrepareTimeoutUndoFailRemovesRecord（假agent桩+反射装表；
 * 本地提交模式CommitRocks落临时目录，System属性窗口只在构造期，
 * 不入@Fast并行车道）。
 */
public class TestCommitServerTimeoutContract {
	private static final String COMMIT_ROCKS_HOME_PROPERTY = "Dbh2CommitRocksHome";

	/** 发号可用的最小AbstractAgent（allocate本地递增，tid确定从1起，形态对齐既有桩）。 */
	private static final class SequencedTidStubAgent extends AbstractAgent {
		private long next = 1;

		private SequencedTidStubAgent() {
			config = new Config();
		}

		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			setCurrentAndCount(autoKey, next, pool);
			next += pool;
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
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
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(
				@NotNull BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Zeze.Component.Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}

	// 不可达端口（假agent不产生任何真实网络请求，仅作agents表键）。
	private static final String RAFT_STUB = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19395"/>
				<node Host="127.0.0.1" Port="19396"/>
				<node Host="127.0.0.1" Port="19397"/>
			</raft>
			""";

	private static final long ROUND_DELAY_MS = 100; // 每重定向轮的受控时延
	private static final int REFUSE_ROUNDS = 40; // 排空拒绝的上限轮数（红路径由此收敛出循环）

	/**
	 * 假agent：redirect=true时prepareBatch延迟后以"拒绝重定向回自身"完结（无限自指
	 * 重定向链，超过REFUSE_ROUNDS轮后排空拒绝、成功完结）；false时立即成功（无拒绝）。
	 * undoBatch立即成功；commitBatch返回受控future（默认永不完结，由测试手动放行）。
	 */
	private static final class StubBucketAgent extends Dbh2Agent {
		private final boolean redirect;
		private final AtomicInteger prepareCalls = new AtomicInteger();
		private volatile TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> commitFuture;

		StubBucketAgent(boolean redirect) throws Exception {
			super(RAFT_STUB);
			this.redirect = redirect;
		}

		@Override
		public TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>> prepareBatch(BPrepareBatch.Data batch) {
			var tcs = new TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>();
			if (!redirect || prepareCalls.incrementAndGet() > REFUSE_ROUNDS)
				tcs.setResult(new PrepareBatch(batch)); // 立即成功 / 排空拒绝后的终轮成功
			else
				Thread.ofVirtual().start(() -> {
					try {
						Thread.sleep(ROUND_DELAY_MS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					var r = new PrepareBatch(batch);
					var refused = new BBatch.Data();
					refused.getPuts().putAll(batch.getBatch().getPuts());
					r.Result.getRefused().put(RAFT_STUB, refused); // 拒绝重定向回自身
					tcs.setResult(r);
				});
			return tcs;
		}

		@Override
		public TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> commitBatch(long tid) {
			var tcs = new TaskCompletionSourceX<RaftRpc<BBatchTid.Data, EmptyBean.Data>>();
			commitFuture = tcs; // 永不完结，由测试放行
			return tcs;
		}

		@Override
		public TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>> undoBatch(long tid) {
			var tcs = new TaskCompletionSourceX<RaftRpc<BBatchTid.Data, EmptyBean.Data>>();
			tcs.setResult(new UndoBatch());
			return tcs;
		}
	}

	/** 重定向路径会触发startRefreshMasterTable（真实MasterAgent网络等待），测试桩必须挡掉。 */
	private static final class StubManager extends Dbh2AgentManager {
		StubManager(AbstractAgent serviceAgent, Config config, int serverId) throws Exception {
			super(serviceAgent, config, serverId);
		}

		@Override
		public void startRefreshMasterTable(String masterName, String databaseName, String tableName) {
			// no-op：桩环境无master。
		}
	}

	// 本地提交模式测试配置：CommitService的acceptor（commitServiceAcceptor需要）+假桶。
	private static Config localCommitConfig() {
		var config = new Config();
		var serviceConf = new ServiceConf();
		serviceConf.addAcceptor(new Acceptor(19399, "127.0.0.1"));
		config.getServiceConfMap().put("Zeze.Dbh2.Commit", serviceConf);
		return config;
	}

	@SuppressWarnings("unchecked")
	private static void registerAgent(Dbh2AgentManager manager, String raft, Dbh2Agent agent) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("agents");
		field.setAccessible(true);
		var agents = (ConcurrentHashMap<String, Dbh2Agent>)field.get(manager);
		agents.put(raft, agent);
	}

	private static Commit getCommit(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("commit");
		field.setAccessible(true);
		return (Commit)field.get(manager);
	}

	private static CommitRocks getRocks(Dbh2AgentManager manager) throws Exception {
		return getCommit(manager).getRocks();
	}

	private static Dbh2AgentManager newLocalManager(Path home, int serverId) throws Exception {
		// CommitRocks的home只在本构造窗口读取（Dbh2CommitRocksHome+serverId），构造完成即还原。
		var priorHome = System.getProperty(COMMIT_ROCKS_HOME_PROPERTY);
		System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, home.toString());
		try {
			return new StubManager(new SequencedTidStubAgent(), localCommitConfig(), serverId);
		} finally {
			if (null != priorHome)
				System.setProperty(COMMIT_ROCKS_HOME_PROPERTY, priorHome);
			else
				System.clearProperty(COMMIT_ROCKS_HOME_PROPERTY);
		}
	}

	private static BPrepareBatches.Data singleBucketBatches() {
		var batches = new BPrepareBatches.Data();
		var batch = new BPrepareBatch.Data("", "database", "table1", null);
		batch.getBatch().getPuts().put(Zeze.Net.Binary.Empty, Zeze.Net.Binary.Empty);
		batches.getDatas().put(RAFT_STUB, batch);
		return batches;
	}

	private static void setPrepareMaxTime(Dbh2AgentManager manager, long value) throws Exception {
		var field = Dbh2Config.class.getDeclaredField("prepareMaxTime");
		field.setAccessible(true);
		field.setLong(manager.getDbh2Config(), value);
	}

	/** ①重定向循环熔断：prepareMaxTime(800ms)量级内必须抛"max prepare time"。 */
	@Test
	public void testRedirectLoopFusedWithinPrepareMaxTime(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var manager = newLocalManager(tempDir.resolve("rocksFuse"), 843);
		try {
			setPrepareMaxTime(manager, 800);
			var stub = new StubBucketAgent(true);
			registerAgent(manager, RAFT_STUB, stub);
			var rocks = getRocks(manager);

			var state = new BTransactionState.Data();
			state.getBuckets().add(RAFT_STUB);

			var t0 = System.currentTimeMillis();
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> rocks.prepare("127.0.0.1", 1, state, singleBucketBatches(), null));
			var cost = System.currentTimeMillis() - t0;
			Assertions.assertTrue(ex.getMessage().contains("max prepare time"),
					"熔断异常必须是prepare时限异常，实际: " + ex);
			// bug时循环顶无检查，只能等排空拒绝（40轮×100ms=4s）后由循环后检查兜底。
			Assertions.assertTrue(cost < 2500,
					"重定向循环必须在prepareMaxTime(800ms)量级熔断，实际耗时" + cost + "ms");
		} finally {
			manager.stop();
		}
	}

	/** ②应答解绑：eCommitting落盘即应答，投递不阻塞处理器；投递由异步路径完成并清理记录。 */
	@Test
	public void testReplySentOnceCommitPointPersisted(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var manager = newLocalManager(tempDir.resolve("rocksReply"), 844);
		var stub = new StubBucketAgent(false);
		try {
			registerAgent(manager, RAFT_STUB, stub);
			var commit = getCommit(manager);
			var rocks = getRocks(manager);

			var handlerDone = new CountDownLatch(1);
			var handlerError = new AtomicReference<Throwable>();
			var r = new Zeze.Builtin.Dbh2.Commit.Commit();
			r.Argument = singleBucketBatches();
			var handler = Thread.ofPlatform().daemon().start(() -> {
				try {
					commit.ProcessCommitRequest(r);
				} catch (Throwable t) {
					handlerError.set(t);
				}
				handlerDone.countDown();
			});

			// 等decide完成点：commitPoint落eCommitting（tid确定=1）。
			var deadline = System.currentTimeMillis() + 10_000;
			while (System.currentTimeMillis() < deadline) {
				var q = rocks.query(1L);
				if (null != q && Commit.eCommitting == q.getState())
					break;
				//noinspection BusyWait
				Thread.sleep(20);
			}
			Assertions.assertNotNull(rocks.query(1L), "commitPoint必须落盘");
			Assertions.assertEquals(Commit.eCommitting, rocks.query(1L).getState());

			// eCommitting落盘后2秒内处理器必须返回（bug时同步等commitBatch投递，阻塞
			// rpcAwaitTimeoutMs=rpcTimeout+5s）。
			Assertions.assertTrue(handlerDone.await(2, TimeUnit.SECONDS),
					"eCommitting落盘即应答，投递不得阻塞处理器");
			Assertions.assertNull(handlerError.get(), "处理器必须正常完成");

			// 放行投递：异步deliver完成commitBatch循环并清理事务记录。
			var startDeadline = System.currentTimeMillis() + 10_000;
			while (System.currentTimeMillis() < startDeadline && null == stub.commitFuture)
				//noinspection BusyWait
				Thread.sleep(20); // 等异步deliver发起commitBatch
			var future = stub.commitFuture;
			Assertions.assertNotNull(future, "commitBatch投递必须已发起");
			future.setResult(new Zeze.Builtin.Dbh2.CommitBatch());
			var removed = System.currentTimeMillis() + 10_000;
			while (System.currentTimeMillis() < removed && null != rocks.query(1L))
				//noinspection BusyWait
				Thread.sleep(20);
			Assertions.assertNull(rocks.query(1L), "投递成功后事务记录必须清理（redoTimer兜底路径闭合）");
		} finally {
			var future = stub.commitFuture; // 红路径兜底：放行悬挂的投递等待
			if (null != future)
				future.setResult(new Zeze.Builtin.Dbh2.CommitBatch());
			manager.stop();
		}
	}

	/** ③客户端超时派生：远程分支Commit超时必须=prepareMaxTime+rpcTimeout+5000。 */
	@Test
	public void testRemoteCommitTimeoutDerived(@TempDir Path tempDir) throws Exception {
		var xml = tempDir.resolve("remoteCommit.xml");
		Files.writeString(xml, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze Dbh2LocalCommit="false">
					<CustomizeConf Name="Dbh2Config" CommitServerAddress="127.0.0.1:1"/>
				</zeze>
				""");
		var manager = new Dbh2AgentManager(new SequencedTidStubAgent(), Config.load(xml.toString()));
		// 反射换成捕获桩：记录远程分支实际传入的Commit rpc超时。
		var capturing = new CapturingCommitAgent();
		Field field = Dbh2AgentManager.class.getDeclaredField("commitAgent");
		field.setAccessible(true);
		field.set(manager, capturing);
		try {
			manager.commit(singleBucketBatches());
			var conf = manager.getDbh2Config();
			var expected = conf.getPrepareMaxTime() + conf.getRpcTimeout() + 5_000L;
			Assertions.assertEquals(expected, capturing.capturedTimeout,
					"远程Commit超时必须覆盖服务端有界决策+应答（prepareMaxTime+rpcTimeout+5000）");
			Assertions.assertEquals(expected, conf.getCommitRpcTimeout(), "派生getter必须等于公式值");
		} finally {
			manager.stop();
		}
	}

	/** 捕获远程分支超时参数的最小CommitAgent桩。 */
	private static final class CapturingCommitAgent extends CommitAgent {
		volatile long capturedTimeout = -1;

		@Override
		public void commit(String host, int port, BPrepareBatches.Data batches, int rpcTimeout) {
			capturedTimeout = rpcTimeout;
		}
	}
}
