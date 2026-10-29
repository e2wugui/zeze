package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import org.jspecify.annotations.Nullable;

import harness.Extra;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Get;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 收尾apply与锁外并发查询的"正向交错"确定性回归：查询线程已按旧meta通过入口校验、
 * 在读数据之前暂停，收尾（endSplit/endMove）在其间完整执行（meta整体替换+
 * deleteToEnd物理删除迁出键域），查询恢复后读数据——已迁往新桶、仍存在的键读到
 * 已删状态。此时Get不得以rc=0+Null应答权威"不存在"（客户端KV(true,null)不重试，
 * 读改写模式放大为默认值覆写），Walk/WalkKey不得按正常bucketEnd应答（客户端陈旧
 * 视图按桶尾推进，迁出键域整段静默丢失）；必须分别以eBucketMismatch/bucketRefuse
 * 应答，客户端走既有重路由/reload自愈。测试侧代理Bucket，在get或getData读取前暂停，
 * 其余操作委托给原桶，生产查询无需测试钩子。
 * 拓扑：单桶[Empty,Empty)端口19380-19382。
 */
@Fast
@Extra
public class TestQueryPausedAcrossFinalizeRedirectsNotFalseMiss {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static final String RAFT = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19380"/>
				<node Host="127.0.0.1" Port="19381"/>
				<node Host="127.0.0.1" Port="19382"/>
			</raft>
			""";

	private static final long MISMATCH = IModule.errorCode(AbstractDbh2.ModuleId, AbstractDbh2.eBucketMismatch);

	private static List<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(RAFT).getNodes().values()) {
			var nodeConfig = RAFT.replaceFirst("<raft ",
					"<raft DbHome=\"" + java.util.regex.Matcher.quoteReplacement(String.valueOf(tempDir.resolve(config.getName().replace(':', '_')))) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static Zeze.Dbh2.Dbh2 waitLeaderNode(List<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		for (var i = 0; i < 300; ++i) {
			for (var n : nodes)
				if (n.getRaft().isLeader())
					return n;
			Thread.sleep(50);
		}
		throw new AssertionError("raft leader not elected");
	}

	private static final AtomicInteger tid = new AtomicInteger();

	private static void put(Dbh2Agent agent, String db, String table, Binary key) throws Exception {
		var batch = new BPrepareBatch.Data("", db, table, null);
		batch.getBatch().getPuts().put(key, new Binary(new byte[] {(byte)9}));
		batch.getBatch().setTid(tid.incrementAndGet());
		agent.prepareBatch(batch).await();
		agent.commitBatch(batch.getBatch().getTid()).await();
	}

	// 确定性正向交错：读取前暂停→收尾apply完整执行→放行查询读数据。
	private static final class Interleave {
		final CountDownLatch queryAtPausePoint = new CountDownLatch(1);
		final CountDownLatch finalizeDone = new CountDownLatch(1);
		final Bucket bucket;
		final Bucket delegate;
		final AtomicBoolean armed = new AtomicBoolean(true);

		Interleave(Dbh2StateMachine stateMachine) throws ReflectiveOperationException {
			delegate = requireNonNull(stateMachine.getBucket());
			// 委托原实例而非spy复制字段：收尾替换的volatile meta必须与查询读取同源。
			// Bucket非final，使用子类mock无需inline agent，也不修改其他桶。
			bucket = mock(Bucket.class, withSettings().defaultAnswer(delegatesTo(delegate)));
			var field = Dbh2StateMachine.class.getDeclaredField("bucket");
			field.setAccessible(true);
			field.set(stateMachine, bucket);
		}

		void pauseGet(Binary key) throws Exception {
			doAnswer(invocation -> {
				pauseUntilFinalized();
				return delegate.get(key);
			}).when(bucket).get(key);
		}

		void pauseWalk() {
			doAnswer(invocation -> {
				pauseUntilFinalized();
				return delegate.getData();
			}).when(bucket).getData();
		}

		void pauseUntilFinalized() {
			if (!armed.compareAndSet(true, false))
				return; // 收尾线程/后续读取直通
			queryAtPausePoint.countDown();
			try {
				Assertions.assertTrue(finalizeDone.await(30, TimeUnit.SECONDS), "finalize within budget");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		void runFinalizeAndRelease(Runnable finalizeApply) {
			try {
				Assertions.assertTrue(queryAtPausePoint.await(30, TimeUnit.SECONDS), "query at pause point");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			try {
				finalizeApply.run(); // 收尾apply效果完整落在查询两读之间
			} finally {
				finalizeDone.countDown();
			}
		}
	}

	private static void setSingleBucketMeta(Dbh2Agent agent, String db, String table) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName(db);
		meta.setTableName(table);
		meta.setRaftConfig("");
		meta.setKeyFirst(Binary.Empty);
		meta.setKeyLast(Binary.Empty);
		agent.setBucketMeta(meta);
	}

	private static void closeNodes(List<Zeze.Dbh2.Dbh2> nodes, RocksDatabase rocks) throws Exception {
		for (var n : nodes) {
			n.close();
			LogSequence.deleteDirectory(new File(requireNonNull(n.getRaft().getRaftConfig().getDbHome())));
		}
		rocks.close();
	}

	@Timeout(120)
	@Test
	public void testGetPausedAfterMetaCheckAcrossEndSplitAnswersMismatch(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2ForwardSplit").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			setSingleBucketMeta(agent, db, table);
			var leaderNode = waitLeaderNode(nodes);
			var leader = leaderNode.getStateMachine();

			var key = new Binary(new byte[]{0x30}); // 分界0x28：key在迁出键域[0x28,∞)内
			put(agent, db, table, key);
			Assertions.assertNotNull(agent.get(db, table, key).getValue(), "迁出键收尾前可读");

			var interleave = new Interleave(leader);
			// 直驱生产handler，代理在meta检查通过后的实际读取前暂停。
			interleave.pauseGet(key);
			var get = new Get();
			get.Argument.setDatabase(db);
			get.Argument.setTable(table);
			get.Argument.setKey(key);
			var rc = new AtomicLong(-1);
			var getError = new AtomicReference<@Nullable Throwable>();
			var getThread = new Thread(() -> {
				try {
					rc.set(leaderNode.ProcessGetRequest(get));
				} catch (Throwable e) {
					getError.set(e);
				}
			});
			getThread.start();
			var from = requireNonNull(requireNonNull(leader.getBucket()).getBucketMeta()).copy();
			from.setKeyLast(new Binary(new byte[]{0x28}));
			var to = requireNonNull(leader.getBucket()).getBucketMeta().copy();
			to.setKeyFirst(new Binary(new byte[]{0x28}));
			to.setRaftConfig(RAFT);
			interleave.runFinalizeAndRelease(() -> leader.endSplit(from, to));
			getThread.join(30_000);
			Assertions.assertFalse(getThread.isAlive(), "get handler must finish");

			Assertions.assertNull(getError.get(), "handler不得抛异常");
			Assertions.assertNull(leader.getBucket().getData().get(key.bytesUnsafe(), key.getOffset(), key.size()),
					"迁出键必须已被物理删除（交错前提成立）");
			// 键已随迁移落入新桶仍存在：rc=0+Null即权威假缺失（缺陷形态）；修复必须eBucketMismatch。
			Assertions.assertFalse(rc.get() == 0 && get.Result.isNull(),
					"正向交错下Get不得权威null（键已迁新桶仍存在）");
			Assertions.assertEquals(MISMATCH, rc.get(), "正向交错下Get必须eBucketMismatch重路由");
		} finally {
			agent.close();
			closeNodes(nodes, rocks);
		}
	}

	@Timeout(120)
	@Test
	public void testGetPausedAfterMetaCheckAcrossEndMoveAnswersMismatch(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2ForwardMove").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			setSingleBucketMeta(agent, db, table);
			var leaderNode = waitLeaderNode(nodes);
			var leader = leaderNode.getStateMachine();

			var key = new Binary(new byte[]{0x55}); // endMove置死全桶：任意键
			put(agent, db, table, key);
			Assertions.assertNotNull(agent.get(db, table, key).getValue(), "迁出键收尾前可读");

			var interleave = new Interleave(leader);
			interleave.pauseGet(key);
			var get = new Get();
			get.Argument.setDatabase(db);
			get.Argument.setTable(table);
			get.Argument.setKey(key);
			var rc = new AtomicLong(-1);
			var getError = new AtomicReference<@Nullable Throwable>();
			var getThread = new Thread(() -> {
				try {
					rc.set(leaderNode.ProcessGetRequest(get));
				} catch (Throwable e) {
					getError.set(e);
				}
			});
			getThread.start();
			var to = requireNonNull(requireNonNull(leader.getBucket()).getBucketMeta()).copy();
			to.setRaftConfig(RAFT);
			interleave.runFinalizeAndRelease(() -> leader.endMove(to));
			getThread.join(30_000);
			Assertions.assertFalse(getThread.isAlive(), "get handler must finish");

			Assertions.assertNull(getError.get(), "handler不得抛异常");
			Assertions.assertFalse(rc.get() == 0 && get.Result.isNull(),
					"正向交错下Get不得权威null（键已迁新桶仍存在）");
			Assertions.assertEquals(MISMATCH, rc.get(), "正向交错下Get必须eBucketMismatch重路由");
		} finally {
			agent.close();
			closeNodes(nodes, rocks);
		}
	}

	@Timeout(120)
	@ParameterizedTest
	@CsvSource({"false,false", "false,true", "true,false", "true,true"})
	public void testWalkPausedAfterRefuseCheckAcrossEndSplitAnswersRefuse(boolean keyOnly, boolean desc,
			@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var db = "database";
		var table = "table1";
		var rocks = new RocksDatabase(tempDir.resolve("dbh2ForwardWalk").toString());
		var nodes = startBucket(rocks, tempDir);
		var agent = new Dbh2Agent(RAFT);
		try {
			setSingleBucketMeta(agent, db, table);
			var leaderNode = waitLeaderNode(nodes);
			var leader = leaderNode.getStateMachine();

			// 1..9全部在本桶；分界5，收尾后[5,∞)物理删除（5..9随迁移落入新桶）。
			for (var i = 1; i <= 9; ++i)
				put(agent, db, table, new Binary(new byte[]{(byte)i}));

			var interleave = new Interleave(leader);
			var cursorKey = new Binary(new byte[]{6}); // 排他游标6：剩余7,8,9全在迁出键域
			interleave.pauseWalk();
			var refused = new AtomicBoolean();
			var rows = new AtomicInteger(-1);
			var walkError = new AtomicReference<@Nullable Throwable>();
			var walkThread = new Thread(() -> {
				try {
					// 真实rpc路径：服务端工作线程在桶数据读取前暂停。
					if (keyOnly) {
						var result = agent.walkKey(cursorKey, 3, desc, null, Binary.Empty, Binary.Empty).Result;
						refused.set(result.isBucketRefuse());
						rows.set(result.getKeys().size());
					} else {
						var result = agent.walk(cursorKey, 3, desc, null, Binary.Empty, Binary.Empty).Result;
						refused.set(result.isBucketRefuse());
						rows.set(result.getKeyValues().size());
					}
				} catch (Throwable e) {
					walkError.set(e);
				}
			});
			walkThread.start();
			var from = requireNonNull(requireNonNull(leader.getBucket()).getBucketMeta()).copy();
			from.setKeyLast(new Binary(new byte[]{5}));
			var to = requireNonNull(leader.getBucket()).getBucketMeta().copy();
			to.setKeyFirst(new Binary(new byte[]{5}));
			to.setRaftConfig(RAFT);
			interleave.runFinalizeAndRelease(() -> leader.endSplit(from, to));
			walkThread.join(30_000);
			Assertions.assertFalse(walkThread.isAlive(), "walk rpc must finish");

			Assertions.assertNull(walkError.get(), "walk rpc不得失败");
			// 缺陷形态：迭代器创建晚于deleteRange，7..9已删→0行+bucketEnd=true正常应答，
			// 客户端陈旧视图按桶尾推进（本桶即视图尾桶时遍历静默终止），7..9整段丢失。
			Assertions.assertTrue(refused.get(),
					"正向交错下Walk必须bucketRefuse（reload重定位），不得按正常桶尾应答");
			Assertions.assertEquals(0, rows.get(), "refuse应答不得携带行");
		} finally {
			agent.close();
			closeNodes(nodes, rocks);
		}
	}
}
