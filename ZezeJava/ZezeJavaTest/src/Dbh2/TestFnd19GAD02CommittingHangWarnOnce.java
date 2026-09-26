package Dbh2;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Dbh2.AbstractCommit;
import Zeze.Dbh2.CommitAgent;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-D02回归：桶侧eCommitting事务超龄告警（拍板：告警+文档化，不做自动终局）。
 * 2PC语义：协调者已保存commitPoint（eCommitting），桶侧无信息安全终局（误undo=跨桶部分
 * 提交），只能告警——onTimer对年龄≥10×bucketMaxTime仍eCommitting的事务error一次
 * （带tid/query地址/年龄），每tid去重；事务完结（commitBatch/undoBatch）回收告警集合。
 * bug时onTimer对eCommitting完全沉默，悬挂事务（锁占用+分裂挂起）不可观测。
 * 形态：进程内3节点raft桶直构（镜像TestFnd19GA01，端口错开）；协调者查询桩恒返
 * eCommitting（模拟"commitPoint存在但redo长期不到"）；反射老化createTime后手动驱动
 * onTimer三次观察去重（真实定时器并发触发亦被同一去重集合覆盖，断言不受其干扰）。
 */
public class TestFnd19GAD02CommittingHangWarnOnce {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();
	private static final long TID = 1;

	// 恒返eCommitting的协调者查询桩：测试不启动它（stop空操作防误触未start的service）。
	private static final class HangingCommitAgent extends CommitAgent {
		@Override
		public BTransactionState.Data query(String host, int port, long tid, int rpcTimeout) {
			var state = new BTransactionState.Data();
			state.setState(AbstractCommit.eCommitting);
			return state;
		}

		@Override
		public void stop() {
		}
	}

	// 捕获Dbh2StateMachine的ERROR日志（告警断言依据）。
	private static final class CaptureAppender extends AbstractAppender {
		final List<LogEvent> events = new CopyOnWriteArrayList<>();

		CaptureAppender() {
			super("fnd19gad02", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	// 进程内启动一个3节点raft桶（镜像TestFnd19GA01，端口19150-52与其错开）。
	// 每个节点独立loadFromString一份RaftConfig（Raft构造会改写配置对象，共享会导致节点身份错乱）；
	// 显式设置DbHome后所有节点目录落在tempDir下，由@TempDir统一清理。
	private static ArrayList<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static void stopBucket(ArrayList<Zeze.Dbh2.Dbh2> nodes, Dbh2Agent agent, RocksDatabase database)
			throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
		database.close();
	}

	private static Zeze.Dbh2.Dbh2 waitLeader(ArrayList<Zeze.Dbh2.Dbh2> nodes) throws InterruptedException {
		for (int i = 0; i < 300; ++i) { // 选举最多等15s
			for (var node : nodes)
				if (node.getRaft().isLeader())
					return node;
			//noinspection BusyWait
			Thread.sleep(50);
		}
		throw new IllegalStateException("no leader elected");
	}

	private static long hangWarnCount(CaptureAppender appender) {
		return appender.events.stream()
				.filter(e -> e.getLevel() == Level.ERROR)
				.filter(e -> e.getMessage().getFormattedMessage().contains("eCommitting transaction hang"))
				.count();
	}

	@Test
	public void testCommittingHangWarnsOnceAndDedups(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("fnd19gad02").toString());
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19150"/>
					<node Host="127.0.0.1" Port="19151"/>
					<node Host="127.0.0.1" Port="19152"/>
				</raft>
				""";
		var nodes = startBucket(database, raftConfig, tempDir);
		var agent = new Dbh2Agent(raftConfig);
		var appender = new CaptureAppender();
		appender.start(); // log4j2要求appender启动后才接收事件
		var smLogger = (Logger)LogManager.getLogger(Zeze.Dbh2.Dbh2StateMachine.class);
		smLogger.addAppender(appender);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("dbh2d02");
			meta.setTableName("t1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var leader = waitLeader(nodes);
			var sm = leader.getStateMachine();

			// 协调者查询替换为恒返eCommitting的桩（真实onTimer定时器同样走它）。
			Field commitAgentField = Zeze.Dbh2.Dbh2StateMachine.class.getDeclaredField("commitAgent");
			commitAgentField.setAccessible(true);
			commitAgentField.set(sm, new HangingCommitAgent());

			// 事务T1进入2PC进行中（prepare后不commit），存活于桶侧事务表。
			var key = new Binary(new byte[]{1});
			var batch = new BPrepareBatch.Data("", "dbh2d02", "t1", null);
			batch.getBatch().getPuts().put(key, new Binary(new byte[]{7}));
			batch.getBatch().setTid(TID);
			var f = agent.prepareBatch(batch);
			f.await();
			Assertions.assertEquals(0, f.get().getResultCode());
			Assertions.assertTrue(sm.getTransactions().containsKey(TID),
					"prepare applied must leave live transaction in leader map");

			// 老化createTime：反射置为2000s前，超过告警阈值10×bucketMaxTime（默认1000s）。
			var txn = sm.getTransactions().get(TID);
			Field createTimeField = Zeze.Dbh2.Dbh2Transaction.class.getDeclaredField("createTime");
			createTimeField.setAccessible(true);
			createTimeField.setLong(txn, System.currentTimeMillis() - 2_000_000L);

			// 手动驱动onTimer三次：必须告警且仅告警一次（每tid去重）。
			Method onTimer = Zeze.Dbh2.Dbh2StateMachine.class.getDeclaredMethod("onTimer");
			onTimer.setAccessible(true);
			onTimer.invoke(sm);
			onTimer.invoke(sm);
			onTimer.invoke(sm);
			Assertions.assertEquals(1, hangWarnCount(appender),
					"超龄eCommitting必须告警一次且去重（三次onTimer只error一次）");
			Assertions.assertTrue(sm.getTransactions().containsKey(TID),
					"eCommitting不得被自动终局（无undo）：事务必须仍在（2PC安全语义）");

			// 事务完结回收告警集合（集合有界性：随commit/undo清理）。
			agent.commitBatch(TID).await();
			Assertions.assertFalse(sm.getTransactions().containsKey(TID));
			Field warnedField = Zeze.Dbh2.Dbh2StateMachine.class.getDeclaredField("committingHangWarnedTids");
			warnedField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var warned = (ConcurrentHashMap.KeySetView<Long, Boolean>)warnedField.get(sm);
			Assertions.assertFalse(warned.contains(TID), "commitBatch后告警去重集合必须回收tid");
		} finally {
			smLogger.removeAppender(appender);
			appender.stop();
			stopBucket(nodes, agent, database);
		}
	}
}
