package Zeze.Arch;

import static org.junit.jupiter.api.Assertions.*;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Builtin.Provider.SetDisableChoice;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@Fast
public class TestProviderInitDisableChoice {

	@Test
	void initDisableChoicePreservedAcrossStartLastAndReconnect() throws Exception {
		try (var env = new Fixture()) {
			env.providerService.initDisableChoiceFromLinks(true);

			// startLast 之前握手：必须宣告 disableChoice=true，且不发送 SetDisableChoice。
			env.providerService.OnHandshakeDone(env.linkSocket);
			assertEquals(List.of(true), env.linkSocket.announces);
			assertTrue(env.linkSocket.setDisableChoices.isEmpty());

			// startLast 完成后：仍保持 initDisableChoiceFromLinks(true)，不得静默翻回 false。
			env.providerApp.startLast(ProviderModuleBinds.load(""), new HashMap<>());
			assertTrue(env.providerApp.isOnlineReady());
			assertTrue(env.providerApp.isUserDisableChoice());
			assertEquals(List.of(true), env.linkSocket.setDisableChoices);

			// Linkd 重连：AnnounceProviderInfo 与 SetDisableChoice 均应保持 true。
			var reconnect1 = env.replaceLinkSocket();
			env.providerService.OnHandshakeDone(reconnect1);
			assertEquals(List.of(true), reconnect1.announces);
			assertEquals(List.of(true), reconnect1.setDisableChoices);

			// 显式放量后：切换为 false，且后续 Linkd 重连不再先宣告 true。
			env.providerService.setDisableChoiceFromLinks(false);
			assertFalse(env.providerApp.isUserDisableChoice());
			assertEquals(List.of(true, false), reconnect1.setDisableChoices);

			var reconnect2 = env.replaceLinkSocket();
			env.providerService.OnHandshakeDone(reconnect2);
			assertEquals(List.of(false), reconnect2.announces,
					"after enabling choice, reconnect must announce disableChoice=false directly");
			assertEquals(List.of(false), reconnect2.setDisableChoices);
		}
	}

	@Test
	void defaultInitDisableChoiceEnablesLinksAfterStartLast() throws Exception {
		try (var env = new Fixture()) {
			// 默认未调用 initDisableChoiceFromLinks：启动前禁止选择，startLast 后开启选择。
			env.providerService.OnHandshakeDone(env.linkSocket);
			assertEquals(List.of(true), env.linkSocket.announces);
			assertTrue(env.linkSocket.setDisableChoices.isEmpty());

			env.providerApp.startLast(ProviderModuleBinds.load(""), new HashMap<>());
			assertTrue(env.providerApp.isOnlineReady());
			assertFalse(env.providerApp.isUserDisableChoice());
			assertEquals(List.of(false), env.linkSocket.setDisableChoices);

			var reconnect = env.replaceLinkSocket();
			env.providerService.OnHandshakeDone(reconnect);
			assertEquals(List.of(false), reconnect.announces);
			assertEquals(List.of(false), reconnect.setDisableChoices);
		}
	}

	private static final class Fixture extends AppBase implements AutoCloseable {
		final Application app;
		final ProviderService providerService;
		final ProviderDirectService directService;
		final ProviderApp providerApp;
		final Connector linkConnector;
		RecordingSocket linkSocket;

		Fixture() throws Exception {
			Task.tryInitThreadPool();
			int serverId = FastServerIds.takeoverPoolNext();
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setTakeoverMode("off");
			conf.setServerId(serverId);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseType(Config.DbType.Memory);
			dbConf.setDatabaseUrl("provider_init_disable_choice_" + serverId);
			conf.getDatabaseConfMap().put("", dbConf);

			app = ProviderDirectTestSupport.newAppWithFakeAgent("ProviderInitDisableChoice" + serverId, conf);
			providerService = new ProviderService("ProviderInitDisableChoice.links." + serverId, app);
			directService = new ProviderDirectService("ProviderInitDisableChoice.direct." + serverId, app);
			providerApp = new ProviderApp(app, new ProviderOnly(), providerService,
					"ProviderInitDisableChoice#", new ProviderDirect(), directService,
					"ProviderInitDisableChoice.linkd", new LoadConfig());

			linkConnector = new Connector("127.0.0.1", 0, false);
			linkSocket = attachSocket(linkConnector);
			providerService.getLinks().put(linkConnector.getName(), linkConnector);

			app.initialize(this);
			app.start();
		}

		RecordingSocket replaceLinkSocket() throws Exception {
			linkSocket = attachSocket(linkConnector);
			return linkSocket;
		}

		private RecordingSocket attachSocket(Connector connector) throws Exception {
			var socket = new RecordingSocket(providerService, connector);
			var field = Connector.class.getDeclaredField("socket");
			field.setAccessible(true);
			field.set(connector, socket);
			return socket;
		}

		@Override
		public Application getZeze() {
			return app;
		}

		@Override
		public void close() throws Exception {
			providerService.stop();
			directService.stop();
			app.stop();
		}
	}

	private static final class RecordingSocket extends AsyncSocket {
		private final Connector connector;
		final List<Boolean> announces = new CopyOnWriteArrayList<>();
		final List<Boolean> setDisableChoices = new CopyOnWriteArrayList<>();

		RecordingSocket(Service service, Connector connector) {
			super(service);
			this.connector = connector;
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public Connector getConnector() {
			return connector;
		}

		@Override
		public boolean Send(Protocol<?> protocol) {
			if (protocol instanceof AnnounceProviderInfo announce)
				announces.add(announce.Argument.isDisableChoice());
			else if (protocol instanceof SetDisableChoice setDisableChoice)
				setDisableChoices.add(setDisableChoice.Argument.isDisableChoice());
			return true;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
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
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}
	}
}
