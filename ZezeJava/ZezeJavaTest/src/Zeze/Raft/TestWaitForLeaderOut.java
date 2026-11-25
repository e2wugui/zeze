package Zeze.Raft;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent.waitForLeader 三参重载的 out 参数回填（FND-R1-3）。
 * driveOutNotSuggestMajorityLeader 依赖 out.value 查询活跃建议多数派连接并循环
 * 等待新 leader（Agent.java:704/:718），但三参重载从不回填 out，主路径必然 NPE。
 * 修复：创建 agent 后立即回填；out 非 null 时 agent 生命周期移交调用方
 * （waitForLeader 不再 stop——stop 会回收 client，回填出去的是已失效的 agent）。
 * 这里不启动任何 raft server：节点地址无监听，waitForLeader 必然超时返回 null，
 * 但 out.value 契约与超时路径无关，创建即回填。
 * "leader 非 null 且非 suggestMajority 才触发的 :704 NPE 主场景"需要真实集群，
 * 由提交信息中的推演核查覆盖。
 */
@Fast
public class TestWaitForLeaderOut {
	private static final String dbHome = "TestWaitForLeaderOut.raft";

	// 3个无监听的节点地址；SuggestMajority默认true，通过waitForLeader入口检查。
	// 显式DbHome：RaftConfig构造时会创建RocksDatabase（Agent构造含PersistentAtomicLong，
	// RaftConfig.loadFromString本身也需要DbHome目录），测试后统一清理。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:19990" DbHome="TestWaitForLeaderOut.raft">
					<node Host="127.0.0.1" Port="19990"/>
					<node Host="127.0.0.1" Port="19991"/>
					<node Host="127.0.0.1" Port="19992"/>
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

	// 超时（无leader）路径：out.value 也必须已回填——回填发生在创建后、与结果无关。
	@Test
	public void testOutFilledEvenOnTimeout() throws Exception {
		var out = new OutObject<Agent>();
		var leader = Agent.waitForLeader(newRaftConfig(), 1000, out); // 无server，1s超时返回null
		if (leader != null) // 端口被测试环境其他进程占用时才可能，此时跳过本断言场景
			return;
		assertNotNull(out.value, "waitForLeader(raftConfig, timeoutMs, out) must fill out.value");
		// out非null时生命周期归调用方：waitForLeader不得在内部stop（client被回收后
		// getActiveSuggestMajorityConnectors将NPE）。这里由测试扮演调用方负责stop。
		out.value.stop();
	}

	@Test
	public void testWaitForLeaderExcludesStoppedOldLeaderAndAcceptsNewLeader(@TempDir Path home) throws Exception {
		var conf = RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:19995" DbHome="%s">
					<node Host="127.0.0.1" Port="19995"/>
					<node Host="127.0.0.1" Port="19996"/>
					<node Host="127.0.0.1" Port="19997" SuggestMajority="false"/>
				</raft>
				""".formatted(home));
		var agent = new Agent("waitForLeader", conf, new Config());
		try {
			var netConfig = Objects.requireNonNull(agent.getClient()).getConfig();
			var oldLeaderConn = netConfig.findConnector("127.0.0.1_19997");
			var newLeaderConn = netConfig.findConnector("127.0.0.1_19995");
			assertNotNull(oldLeaderConn);
			assertNotNull(newLeaderConn);
			oldLeaderConn.setAutoReconnect(false);
			newLeaderConn.setAutoReconnect(false);

			var oldLeaderIs = new LeaderIs();
			oldLeaderIs.Argument.setTerm(1);
			oldLeaderIs.Argument.setLeaderId(oldLeaderConn.getName());
			oldLeaderIs.Argument.setLeader(true);
			assertTrue(agent.setLeader(oldLeaderIs, new Agent.ConnectorProxy(oldLeaderConn)));

			var result = new AtomicReference<@Nullable Connector>();
			var done = new CountDownLatch(1);
			var waiter = new Thread(() -> {
				try {
					result.set(Agent.waitForLeader(agent, conf, Set.of(oldLeaderConn.getName()), 5000));
				} finally {
					done.countDown();
				}
			});
			waiter.start();

			// 进入 waitForLeader 后应立即清除指向被排除旧 Leader 的引用。
			long clearDeadline = System.currentTimeMillis() + 2000;
			while (agent.getLeader() != null && System.currentTimeMillis() < clearDeadline) {
				//noinspection BusyWait
				Thread.sleep(10);
			}
			assertNull(agent.getLeader(), "waitForLeader(excludeNames) 必须清除指向已排除旧 Leader 的引用");

			// 再次通告被排除的旧 Leader 不得唤醒等待者。
			assertTrue(agent.setLeader(oldLeaderIs, new Agent.ConnectorProxy(oldLeaderConn)));
			assertFalse(done.await(100, TimeUnit.MILLISECONDS), "被排除的旧 Leader 不得满足新 Leader 等待条件");

			// 通告不在排除集合内的新 Leader 应立即唤醒并返回新 Leader 连接器。
			var newLeaderIs = new LeaderIs();
			newLeaderIs.Argument.setTerm(2);
			newLeaderIs.Argument.setLeaderId(newLeaderConn.getName());
			newLeaderIs.Argument.setLeader(true);
			assertTrue(agent.setLeader(newLeaderIs, new Agent.ConnectorProxy(newLeaderConn)));

			assertTrue(done.await(3, TimeUnit.SECONDS), "新 Leader 选出后应立即完成等待");
			assertEquals(newLeaderConn.getName(), result.get() != null ? result.get().getName() : null);
			waiter.join(1000);
		} finally {
			agent.stop();
		}
	}
}
