package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Config;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/** Sequential applications reopen one Memory checkpoint bucket; no concurrent cache sharing is assumed. */
@Fast
@Isolated
public class TestTimerTakeoverVersion {
	private static final String TimerId = "transferred-versioned-timer";
	private static final int DeadServerId = 777;
	private static final long NodeId = 777_991;

	public static class NoopHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
		}
	}

	private static final class Env implements AutoCloseable {
		final Application app;
		final TakeoverTestEnv.AccessibleTimer timer;

		Env(Config conf) throws Exception {
			Task.tryInitThreadPool();
			app = new Application("TestTimerTakeoverVersion" + conf.getServerId(), conf);
			timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
			app.start();
			timer.loadCustomClassAnd();
			timer.start();
		}

		void transaction(FuncLong action) {
			Assertions.assertEquals(0L, app.newProcedure(action, "TestTimerTakeoverVersion.transaction").call());
		}

		@Override
		public void close() throws Exception {
			try {
				timer.stop();
			} finally {
				app.stop();
			}
		}
	}

	@Test
	public void inheritedTimerVersionVetoesAnOlderSecondTakeover() throws Exception {
		var firstConf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		firstConf.setAppVersion(5);
		var firstServerId = firstConf.getServerId();
		var databaseUrl = firstConf.getDatabaseConfMap().get("").getDatabaseUrl();
		long firstEpoch;
		try (var first = new Env(firstConf)) {
			first.transaction(() -> {
				var root = first.timer._tNodeRoot.getOrAdd(DeadServerId);
				root.setVersion(firstConf.getAppVersion());
				root.setHeadNodeId(NodeId);
				root.setTailNodeId(NodeId);
				root.setLoadSerialNo(1);
				var node = first.timer._tNodes.getOrAdd(NodeId);
				node.setNextNodeId(NodeId);
				node.setPrevNodeId(NodeId);
				var simple = new BSimpleTimer();
				simple.setNextExpectedTime(System.currentTimeMillis() + 3_600_000);
				simple.setPeriod(60_000);
				simple.setRemainTimes(-1);
				var row = new BTimer(TimerId, NoopHandle.class.getName(), 0);
				row.setTimerObj(simple);
				node.getTimers().put(TimerId, row);
				first.timer._tIndexs.insert(TimerId, new BIndex(DeadServerId, NodeId, 1, firstConf.getAppVersion()));
				return 0L;
			});
			TakeoverTestEnv.forgeLease(first.app, DeadServerId, 1, System.currentTimeMillis() - 1_000);
			first.app.getTakeover().tryTransfer(DeadServerId);
			TakeoverTestEnv.waitTryTransferQueue();
			first.transaction(() -> {
				var index = first.timer.getTimerIndex(TimerId);
				Assertions.assertNotNull(index);
				Assertions.assertEquals(firstServerId, index.getServerId(), "compatible first takeover must succeed");
				return 0L;
			});
			Assertions.assertEquals(0L, TakeoverTestEnv.readLease(first.app, DeadServerId)[1]);
			firstEpoch = TakeoverTestEnv.readLease(first.app, firstServerId)[0];
			first.app.checkpointRun();
		}

		var secondConf = TakeoverTestEnv.newConf("on", 600_000, 600_000);
		secondConf.setAppVersion(0);
		secondConf.getDatabaseConfMap().get("").setDatabaseUrl(databaseUrl);
		try (var second = new Env(secondConf)) {
			TakeoverTestEnv.forgeLease(second.app, firstServerId, firstEpoch, System.currentTimeMillis() - 1_000);
			second.app.getTakeover().tryTransfer(firstServerId);
			TakeoverTestEnv.waitTryTransferQueue();

			Assertions.assertNotEquals(0L, TakeoverTestEnv.readLease(second.app, firstServerId)[1],
					"older application must veto the inherited timer chain instead of tombstoning its owner");
			second.transaction(() -> {
				Assertions.assertEquals(NodeId, second.timer._tNodeRoot.get(firstServerId).getHeadNodeId(),
						"veto must preserve the versioned source chain");
				Assertions.assertEquals(firstServerId, second.timer.getTimerIndex(TimerId).getServerId());
				return 0L;
			});
		}
	}
}
