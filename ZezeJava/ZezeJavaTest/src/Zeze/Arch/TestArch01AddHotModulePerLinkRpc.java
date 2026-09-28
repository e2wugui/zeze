package Zeze.Arch;

import java.net.SocketAddress;

import Zeze.Application;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND17 arch-01回归：addHotModule用单个Bind/Subscribe Rpc实例循环发送到所有link，
 * ≥2条就绪连接时第二次Send踩Rpc一次性守卫抛IllegalStateException，直落HotManager
 * 不可回滚区halt(111222)整进程硬杀。修复后每个link各自new实例（对齐OnHandshakeDone
 * 的每连接新实例形态）。
 * 构造性场景：no-op FakeAgent（registerService/editService全空转）+ 两个注入就绪
 * CountingSocket的Connector，直接调用addHotModule——修复前本用例红
 * （IllegalStateException），修复后绿且两条link各收到一帧Bind/Subscribe。
 */
@Fast
public class TestArch01AddHotModulePerLinkRpc {

	/** 最小IModule桩：仅提供getId（map键）与名字。 */
	private static final class StubModule implements IModule {
		@Override
		public @NotNull String getFullName() {
			return "Zeze.Arch.TestFnd17StubModule";
		}

		@Override
		public @NotNull String getName() {
			return "TestFnd17StubModule";
		}

		@Override
		public int getId() {
			return 1701;
		}
	}

	/** 计帧AsyncSocket桩：Send只计数不触碰网络（对齐TestFnd888的FakeSocket形态）。 */
	private static final class CountingSocket extends AsyncSocket {
		int frames;

		CountingSocket(@NotNull Zeze.Net.Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			++frames;
			return true;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}
	}

	/** 最小AppBase桩：appBase平时由启动流程initialize，测试直接注入。 */
	private static final class StubAppBase extends Zeze.AppBase {
		private final @NotNull Application app;

		StubAppBase(@NotNull Application app) {
			this.app = app;
		}

		@Override
		public @NotNull Application getZeze() {
			return app;
		}
	}

	/** FakeAgent补丁：subscribeService的返回值被addHotModule丢弃，直接给null，
	 * 避免继承实现的空列表getFirst()抛NoSuchElementException。 */
	private static final class AddHotModuleAgent extends ProviderDirectTestSupport.FakeAgent {
		@Override
		public @NotNull Agent.SubscribeState subscribeService(@NotNull BSubscribeInfo info) {
			//noinspection ContractViolation (test stub，返回值被丢弃)
			return null;
		}
	}

	/** 公共装配：disable模式Application + no-op SM + 轻量ProviderApp（回设providerApp）。 */
	private static ProviderService newProviderService(@NotNull String name) throws Exception {
		var conf = new Zeze.Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		var app = new Application(name, conf);
		app.initialize(new StubAppBase(app));
		var smField = Application.class.getDeclaredField("serviceManager");
		smField.setAccessible(true);
		smField.set(app, new AddHotModuleAgent());

		var providerApp = new ProviderApp(app); // 发布打包用轻量构造：装配fake providerService
		var ps = providerApp.providerService;
		ps.providerApp = providerApp; // 轻量构造不回设，测试补上（同包访问）
		return ps;
	}

	@Test
	public void testStaticBindReachesEveryReadyLink() throws Exception {
		var ps = newProviderService("a7fnd17arch01");
		var providerApp = ps.providerApp;

		var so1 = new CountingSocket(ps);
		var so2 = new CountingSocket(ps);
		ps.getLinks().put("link1", readyConnector("link1", so1));
		ps.getLinks().put("link2", readyConnector("link2", so2));

		var config = new BModule.Data(); // 非dynamic：走Bind分支
		ps.addHotModule(new StubModule(), config);

		Assertions.assertEquals(1, so1.frames, "link1必须收到新模块的Bind帧");
		Assertions.assertEquals(1, so2.frames, "link2必须收到新模块的Bind帧（修复前第二个Send抛IllegalStateException，本用例红）");
		Assertions.assertTrue(providerApp.staticBinds.containsKey(1701), "静态绑定须登记");
	}

	@Test
	public void testDynamicSubscribeReachesEveryReadyLink() throws Exception {
		var ps = newProviderService("a7fnd17arch01d");
		var providerApp = ps.providerApp;

		var so1 = new CountingSocket(ps);
		var so2 = new CountingSocket(ps);
		ps.getLinks().put("link1", readyConnector("link1", so1));
		ps.getLinks().put("link2", readyConnector("link2", so2));

		var config = new BModule.Data();
		config.setDynamic(true);
		ps.addHotModule(new StubModule(), config);

		Assertions.assertEquals(1, so1.frames, "link1必须收到新模块的Subscribe帧");
		Assertions.assertEquals(1, so2.frames, "link2必须收到新模块的Subscribe帧（修复前第二个Send抛IllegalStateException，本用例红）");
		Assertions.assertTrue(providerApp.dynamicModules.containsKey(1701), "动态订阅须登记");
	}

	/** 构造不连接的Connector，反射把futureSocket直接置为就绪桩（TryGetReadySocket即getNow）。 */
	private static Connector readyConnector(@NotNull String name, @NotNull AsyncSocket so) throws Exception {
		var connector = new Connector("127.0.0.1", 1, false);
		var field = Connector.class.getDeclaredField("futureSocket");
		field.setAccessible(true);
		((TaskCompletionSource<AsyncSocket>)field.get(connector)).setResult(so);
		return connector;
	}
}
