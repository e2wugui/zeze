package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTimerLoadFailure {
	@Test
	void failedMiddleNodeStopsStartupAndCanBeReloaded() throws Exception {
		Task.tryInitThreadPool();
		var app = new Application("TimerLoadFailure", TakeoverTestEnv.newConf("off", 60_000, 60_000));
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		try {
			app.start();
			timer.loadCustomClassAnd();
			assertEquals(0, app.newProcedure(() -> {
				var root = timer._tNodeRoot.getOrAdd(app.getConfig().getServerId());
				root.setHeadNodeId(11);
				root.setTailNodeId(13);
				for (long id = 11; id <= 13; id++) {
					var node = timer._tNodes.getOrAdd(id);
					node.setNextNodeId(id == 13 ? 11 : id + 1);
					node.setPrevNodeId(id == 11 ? 13 : id - 1);
					var name = "load-" + id;
					var entry = new BTimer();
					entry.setTimerName(name);
					if (id != 12)
						entry.getTimerObj().setBean(futureTimer());
					node.getTimers().put(name, entry);
					var index = timer._tIndexs.getOrAdd(name);
					index.setNodeId(id);
					index.setSerialId(id);
				}
				return 0;
			}, "TimerLoadFailure.seed").call());
			assertThrows(IllegalStateException.class, timer::start,
					"中间节点回滚必须使启动失败，不能跳过节点后报成功");
			assertFalse(timer.timerFutures.containsKey("load-13"), "失败后不应继续装载后续节点");
			assertTrue(timer.timerFutures.isEmpty(), "失败启动应撤销先前节点的调度");
			assertEquals(0, app.newProcedure(() -> {
				timer._tNodes.get(12L).getTimers().get("load-12").getTimerObj().setBean(futureTimer());
				return 0;
			}, "TimerLoadFailure.repair").call());
			timer.start();
			for (long id = 11; id <= 13; id++)
				assertTrue(timer.timerFutures.containsKey("load-" + id), "重启必须装载所有节点");
		} finally {
			timer.stop();
			app.stop();
		}
	}

	private static BSimpleTimer futureTimer() {
		var simple = new BSimpleTimer();
		simple.setNextExpectedTime(System.currentTimeMillis() + 3_600_000);
		return simple;
	}
}
