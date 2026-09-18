package UnitTest.Zeze.Services;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-69 回归：重放第二阶段 subscribeServicesAsync 首行 waitConnectorReady 可同步
 * 抛出（future 创建之前 whenComplete 不可达），原样穿透后被上层 ofAction 吞掉，
 * scheduleReplayRetry 不被调用——FND4-65"重连后状态最终必达"重试链断裂。
 * 修复：phase-2 调用补同步 try/catch（对齐 raft 版 onLoginSuccess 双保险）+ 空订阅守卫。
 * <p>
 * phase-2 同步抛出的触发窗口是亚毫秒级连接死亡巧合（phase-1 应答到达与 phase-2 等待
 * 之间），黑盒无法确定性复现；本用例端到端钉住共享的重试链不变量：健康连接重放不登记
 * 重试；连接死后重放的同步异常必须转化为重试登记（链不断），修复前该异常会原样抛出
 * replayRegistersAndSubscribes 之外（直接反射调用即可观测，不依赖 ofAction 吞噬）。
 */
@Fast
public class TestFnd869AgentReplaySubscribeRetry {
	// a5专属固定端口（@Fast固定端口独占契约；SM同端口绑TCP+UDP，26/28xxx段在动态端口范围外）
	private static final int PORT = 28610;

	@Test
	public void testReplayFailureAlwaysRegistersRetry() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys")); // RocksDB需要父目录存在
		var sm = new ServiceManagerServer(null, PORT, new Config(), "autokeys/a5-fnd869");
		Agent agent = null;
		try {
			var conf = new Config();
			conf.setServerId(5969);
			agent = new Agent(conf);
			agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", PORT));
			agent.start();
			agent.waitReady();
			agent.subscribeService(new BSubscribeInfo("Fnd869.Svc", 0)); // 订阅态进入重放源

			var replay = Agent.class.getDeclaredMethod("replayRegistersAndSubscribes");
			replay.setAccessible(true);
			var retryTaskField = Agent.class.getDeclaredField("replayRetryTask");
			retryTaskField.setAccessible(true);

			// 基线：健康连接重放（phase-1空edit + phase-2重订阅都成功），不得登记重试
			final var healthyAgent = agent;
			Assertions.assertDoesNotThrow(() -> replay.invoke(healthyAgent), "健康重放必须无异常");
			Assertions.assertNull(retryTaskField.get(agent), "健康重放不得登记重试任务");

			// 断链：关SM后连接死、无自动重连（Connector默认autoReconnect=false），
			// 重放的同步异常（waitConnectorReady 5s超时抛出）必须被catch并转化为重试登记
			sm.close();
			sm = null;
			var client = agent.getClient();
			for (int i = 0; i < 300 && client.getSocket() != null; i++)
				Thread.sleep(10);
			Assertions.assertNull(client.getSocket(), "前置：连接应已断开");
			Assertions.assertDoesNotThrow(() -> replay.invoke(healthyAgent),
					"重放内任何同步异常必须被内部catch转化为重试，不得原样穿透（链断=FND8-69）");
			Assertions.assertNotNull(retryTaskField.get(agent), "重放失败必须登记重试任务（FND4-65链不变量）");
		} finally {
			if (agent != null)
				agent.stop();
			if (sm != null)
				sm.close();
		}
	}
}
