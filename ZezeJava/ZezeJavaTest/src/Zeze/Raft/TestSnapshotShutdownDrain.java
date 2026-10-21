package Zeze.Raft;

import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutionException;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static java.util.Objects.requireNonNull;

@Fast
public class TestSnapshotShutdownDrain {
	private static Raft createRaft(Path home, StateMachine sm) throws Exception {
		Task.tryInitThreadPool();
		// 不启动 server，不绑定端口；独立临时目录，不使用 Application 或全局内存库。
		var conf = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19490" DbHome="%s">
				    <node Host="127.0.0.1" Port="19490"/>
				    <node Host="127.0.0.1" Port="19491"/>
				    <node Host="127.0.0.1" Port="19492"/>
				</raft>
				""".formatted(home));
		return new Raft(sm, null, conf, new Config());
	}

	@Test
	void shutdownWaitsForRunningSnapshotBeforeClosingLogs(@TempDir Path home) throws Exception {
		verifyShutdownDrain(home, false);
	}

	@Test
	void failedSnapshotAlsoReleasesShutdown(@TempDir Path home) throws Exception {
		verifyShutdownDrain(home, true);
	}

	private static void verifyShutdownDrain(Path home, boolean fail) throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var sm = new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) throws Exception {
				entered.countDown();
				assertTrue(release.await(5, TimeUnit.SECONDS));
				// 模拟快照最终读取/提交阶段：关闭必须等待这段使用日志库的工作结束。
				assertNotNull(requireNonNull(getRaft()).getLogSequence().lastAppliedLogTermIndex());
				if (fail)
					throw new IOException("snapshot failed after resources used");
				return new SnapshotResult();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
		var raft = createRaft(home, sm);
		var workers = Executors.newFixedThreadPool(2);
		try {
			var snapshot = workers.submit(() -> {
				raft.getLogSequence().snapshot();
				return null;
			});
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			var shutdownEntered = new CountDownLatch(1);
			var shutdown = workers.submit(() -> {
				shutdownEntered.countDown();
				raft.shutdown();
				return null;
			});
			assertTrue(shutdownEntered.await(5, TimeUnit.SECONDS));
			assertThrows(TimeoutException.class, () -> shutdown.get(200, TimeUnit.MILLISECONDS),
					"运行中的快照尚未结束，shutdown 不得返回并允许外部删除 DbHome");
			release.countDown();
			if (fail)
				assertInstanceOf(IOException.class,
						assertThrows(ExecutionException.class, () -> snapshot.get(5, TimeUnit.SECONDS)).getCause());
			else
				snapshot.get(5, TimeUnit.SECONDS);
			shutdown.get(5, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			workers.shutdown();
			assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
			raft.shutdown();
		}
	}

	@Test
	void queuedSnapshotDoesNotStartAfterShutdown(@TempDir Path home) throws Exception {
		var runs = new AtomicInteger();
		var sm = new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				runs.incrementAndGet();
				return new SnapshotResult();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
		var raft = createRaft(home, sm);
		try {
			raft.shutdown();
			raft.getLogSequence().snapshot(); // 已排队任务晚于 shutdown 才获得执行机会。
			assertEquals(0, runs.get(), "不得在已关闭的状态机上启动新快照");
		} finally {
			raft.shutdown();
		}
	}
}
