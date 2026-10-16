package Zeze.Arch;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Online.BLink;
import Zeze.Builtin.Online.BOnline;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Arch transmitInProcedure曾在过程事务内直接Send——过程redo（_tonline读冲突
 * 触发lockAndCheck整轮重执行）会向远端重复投递TransmitAccount；修复后对齐
 * Game.transmitEmbed，Send包进Transaction.whileCommit：过程体内不发送，提交成功后
 * 恰好一次（redo轮注册的回调随savepoint丢弃）。
 * <p>
 * 环境对齐TestTransmitUnknownAction（独立Memory库+独立serverId），加真实进程内GCM
 * （hasGlobal=true才走远端直发分支）与注入的记录型直发socket（不真实收发）。
 */
@Fast
public class TestArchTransmitWhileCommit {

	// @Fast固定端口：避开TestGcmStopWaitsDaemon(19718)等族内GCM判例端口。
	private static final int GCM_PORT = 19723;
	// 远端provider的serverId：非-1且非本机，groupByServer即路由到远端直发分支。
	private static final int REMOTE_SERVER_ID = 59999;

	private static final AtomicInteger NextServerId =
			new AtomicInteger(FastServerIds.TEST_ARCH_TRANSMIT_WHILE_COMMIT);
	private static @Nullable GlobalCacheManagerAsyncServer gcm;

	@BeforeAll
	public static void setUpGcm() throws Exception {
		Task.tryInitThreadPool();
		gcm = new GlobalCacheManagerAsyncServer();
		gcm.start(null, GCM_PORT, null);
	}

	@AfterAll
	public static void tearDownGcm() {
		if (gcm != null) {
			try {
				gcm.stop();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setGlobalCacheManagerHostNameOrAddress("127.0.0.1"); // hasGlobal=true：transmit走远端分支
		conf.setGlobalCacheManagerPort(GCM_PORT);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("arch_transmit_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	private static final class TestArchOnline extends Zeze.Arch.Online {
		TestArchOnline(AppBase app) {
			super(app);
		}
	}

	/** 记录型socket：只计Send次数，不真实收发。 */
	private static final class RecordingSocket extends AsyncSocket {
		final AtomicInteger sends = new AtomicInteger();

		RecordingSocket(Service service) {
			super(service);
		}

		@Override
		public boolean Send(byte [] bytes, int offset, int length) {
			sends.incrementAndGet();
			return true;
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}
	}

	@Test
	public void testTransmitDeferredToCommit() throws Exception {
		var zeze = newApp("ArchTransmitWhileCommit");
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		var providerApp = new ProviderApp(zeze);
		// 简版ProviderApp的providerDirectService为null：注入真实实例（transmitInProcedure读取它）
		var pds = new ProviderDirectService("archTransmitDirect", zeze);
		var pdField = ProviderApp.class.getDeclaredField("providerDirectService");
		pdField.setAccessible(true);
		pdField.set(providerApp, pds);
		// 注入记录型socket与远端provider会话：GetSocket(sessionId)能找到fake
		var fake = new RecordingSocket(pds);
		var socketMapField = Service.class.getDeclaredField("socketMap");
		socketMapField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var socketMap = (LongConcurrentHashMap<AsyncSocket>)socketMapField.get(pds);
		socketMap.put(fake.getSessionId(), fake);
		pds.providerByServerId.put(REMOTE_SERVER_ID, pds.newSession(fake));

		zeze.initialize(app);
		var archOnline = new TestArchOnline(app); // 表注册纯内存，须在start前构造
		zeze.start();
		try {
			// 目标账号在REMOTE_SERVER_ID登录（eLogined）→ 路由到远端直发分支
			var setupRc = zeze.newProcedure(() -> {
				var onlines = archOnline.getOrAddOnline("rt1");
				var login = new BOnline();
				login.setLink(new BLink("testLink", 0, AbstractOnline.eLogined));
				login.setServerId(REMOTE_SERVER_ID);
				onlines.getLogins().put("rc1", login);
				return Procedure.Success;
			}, "setupOnline").call();
			Assertions.assertEquals(Procedure.Success, setupRc);

			var method = Zeze.Arch.Online.class.getDeclaredMethod("transmitInProcedure",
					String.class, String.class, String.class, Collection.class, Binary.class);
			method.setAccessible(true);
			var targets = List.of(new BLoginKey("rt1", "rc1"));
			var duringBody = new int[1];
			var runRc = zeze.newProcedure(() -> {
				try {
					method.invoke(archOnline, "senderAcc", "senderCid", "transmit.archTest", targets, null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				duringBody[0] = fake.sends.get(); // 过程体内：Send必须尚未发生（被whileCommit推迟）
				return Procedure.Success;
			}, "runTransmit").call();
			Assertions.assertEquals(Procedure.Success, runRc);
			Assertions.assertEquals(0, duringBody[0],
					"过程体内不得真实Send：redo会整轮重执行，事务内Send将向远端重复投递");
			Assertions.assertEquals(1, fake.sends.get(), "提交后恰好发送一次");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
