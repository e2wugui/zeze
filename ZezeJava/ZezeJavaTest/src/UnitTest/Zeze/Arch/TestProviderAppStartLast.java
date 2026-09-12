package UnitTest.Zeze.Arch;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import Zeze.Arch.LoadBase;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.IModule;
import Zeze.Services.LoginQueueAgent;
import demo.SimpleApp;

/**
 * FND3-34 回归：ProviderApp.startLast 必须恰好执行一次，且 LoginQueueAgent 只创建一个。
 * <ul>
 * <li>二次调用 startLast 必须快速失败（IllegalStateException），不得静默重跑全套初始化；</li>
 * <li>load 已预置 LoginQueueAgent 时（server.xml 同时存在 LoginQueueAgent 配置节），
 * startLast 必须复用预置实例——旧实现无条件新建覆盖，预置 agent 泄漏并以相同
 * (serverId,ip,port) 身份向 LoginQueueServer 重复注册。</li>
 * </ul>
 * 非 @Fast：SimpleApp 依赖 SM 环境（integrationTest 由 TestEnvLauncherListener 提供）。
 */
public class TestProviderAppStartLast {
	// 避开 TestRank 的 1..3 段与 TakeoverTimer demo.App 的 0。
	private static final int SERVER_ID = 90;

	private SimpleApp app;

	@BeforeEach
	public void setUp() throws Exception {
		(app = new SimpleApp(SERVER_ID)).start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app != null)
			app.stop();
		app = null;
	}

	@Test
	public void testReentryFailFast() {
		Map<String, IModule> modules = app.getModules();
		// 必须炸在入口守卫上：旧代码会先重跑绑定/订阅/timer再被别处绊倒（或无agentConf时静默重跑），
		// 同为IllegalStateException但不是同一个错，用消息区分。
		var ex = assertThrows(IllegalStateException.class,
				() -> app.getProviderApp().startLast(ProviderModuleBinds.load(""), modules));
		assertTrue(ex.getMessage().contains("already called"),
				"必须是入口重入守卫失败，而不是重跑半途被其它错误绊倒: " + ex.getMessage());
	}

	@Test
	public void testPresetLoginQueueAgentReused() throws Exception {
		var preset = new PresetAgentApp(SERVER_ID + 1);
		try {
			preset.start(); // server.xml 配有 LoginQueueAgent 节：旧实现会新建覆盖预置实例
			LoadBase load = preset.getProviderApp().providerImplement.getLoad();
			assertSame(preset.agent, load.getLoginQueueAgent(), "预置的LoginQueueAgent必须被复用，不得新建覆盖");
		} finally {
			preset.stop();
		}
	}

	private static class PresetAgentApp extends SimpleApp {
		LoginQueueAgent agent;

		PresetAgentApp(int serverId) throws Exception {
			super(serverId);
		}

		@Override
		protected void beforeStartLast() {
			var load = getProviderApp().providerImplement.getLoad();
			agent = new LoginQueueAgent(getZeze().getConfig(),
					getZeze().getConfig().getServerId(), load.getServiceIp(), load.getServicePort());
			load.setLoginQueueAgent(agent);
		}
	}
}
