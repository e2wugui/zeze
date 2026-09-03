package UnitTest.Zeze.Services;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND-S1-2：异步GCM acquire延续阶段重读 getUserState()，会话被daemon kick（置null）后NPE，
 * 异常发生在 acquireStatePending 置位之后且无复位——申请位永久泄漏，该key上所有后续
 * acquire/release 进入永不唤醒的等待（key冻结），且客户端重试在GCM侧无界堆积。
 * <p>
 * 构造性场景（进程内服务器+裸协议客户端，时序全部由测试控制）：
 * 1. A 登录并持有 KEY 的 Modify；
 * 2. B 登录，Acquire(KEY, Share)：服务端B占住申请位(StateShare)、向A发Reduce后park；
 * 3. 模拟守护kick B（断开连接并清空socket.userState）；
 * 4. A 应答Reduce（降到Invalid），B的延续阶段被唤醒。
 * <p>
 * 断言：申请位复位为Invalid（修复前延续阶段NPE后永久停留StateShare），
 * 且第三方C的Acquire能正常得到应答（修复前key冻结只能超时）。
 */
@Fast
public class TestGlobalCacheManagerAsyncAcquireKick {
	private static final int PORT = 19711; // @Fast固定端口独占
	private static final Binary KEY = new Binary("UnitTest.FND_S1_2.Key".getBytes(StandardCharsets.UTF_8));
	private static final int SERVER_ID_A = 9101;
	private static final int SERVER_ID_B = 9102;
	private static final int SERVER_ID_C = 9103;

	private static RawClient clientA;
	private static RawClient clientB;
	private static RawClient clientC;

	/** 裸协议客户端：Login/Acquire正常收发；Reduce到达后park住由测试控制应答时机。 */
	private static final class RawClient extends Service {
		private final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();

		RawClient(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(
					Reduce::new, this::processReduce, TransactionLevel.None, DispatchMode.Direct));
		}

		private long processReduce(Reduce rpc) {
			reduces.add(rpc); // 不自动应答
			return 0;
		}

		AsyncSocket connect() throws Exception {
			var connector = new Connector("127.0.0.1", PORT, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		GlobalCacheManagerAsyncServer.getInstance().start(null, PORT, null);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		for (var c : new RawClient[]{clientA, clientB, clientC})
			if (c != null)
				c.stop();
		GlobalCacheManagerAsyncServer.getInstance().stop();
	}

	private static void login(AsyncSocket socket, int serverId) throws Exception {
		var login = new Login();
		login.Argument.serverId = serverId;
		login.Argument.globalCacheManagerHashIndex = 0;
		Assertions.assertTrue(login.SendForWait(socket, 10_000).await(10_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
	}

	private static Object cacheStateOf(Binary key) throws Exception {
		var field = GlobalCacheManagerAsyncServer.class.getDeclaredField("global");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var global = (ConcurrentHashMap<Binary, Object>)field.get(GlobalCacheManagerAsyncServer.getInstance());
		return global.get(key);
	}

	private static int pendingOf(Binary key) throws Exception {
		var cs = cacheStateOf(key);
		if (cs == null)
			return GlobalCacheManagerConst.StateInvalid;
		var field = cs.getClass().getDeclaredField("acquireStatePending");
		field.setAccessible(true);
		return field.getInt(cs);
	}

	/** 模拟achillesHeelDaemon对超时会话的kick：清空socket.userState并断开连接。 */
	private static void kickSession(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerAsyncServer.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		var sessions = (LongConcurrentHashMap<?>)sessionsField.get(GlobalCacheManagerAsyncServer.getInstance());
		var holder = sessions.get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		var kick = holder.getClass().getDeclaredMethod("kick");
		kick.setAccessible(true);
		kick.invoke(holder);
	}

	private static void waitPending(int expected, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (pendingOf(KEY) == expected)
				return;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		Assertions.assertEquals(expected, pendingOf(KEY), message);
	}

	@Test
	@Timeout(60)
	public void testKickDuringAcquireMustNotFreezeKey() throws Exception {
		// 1. A登录并持有KEY的Modify
		clientA = new RawClient("UnitTest.FND_S1_2.A");
		var socketA = clientA.connect();
		login(socketA, SERVER_ID_A);
		var acquireA = new Acquire(KEY, GlobalCacheManagerConst.StateModify);
		Assertions.assertTrue(acquireA.SendForWait(socketA, 10_000).await(10_000), "A acquire await");
		Assertions.assertFalse(acquireA.isTimeout(), "A acquire timeout");
		Assertions.assertEquals(0, acquireA.getResultCode(), "A acquire resultCode");

		// 2. B登录，Acquire(KEY, Share)：服务端占住申请位、向A发Reduce后park在等待
		clientB = new RawClient("UnitTest.FND_S1_2.B");
		var socketB = clientB.connect();
		login(socketB, SERVER_ID_B);
		var acquireB = new Acquire(KEY, GlobalCacheManagerConst.StateShare);
		acquireB.SendForWait(socketB, 60_000); // 不等待结果（kick后应答发往已关闭连接）

		waitPending(GlobalCacheManagerConst.StateShare, 10_000, "B必须占住申请位");
		var reduce = clientA.reduces.poll(10, TimeUnit.SECONDS);
		Assertions.assertNotNull(reduce, "A必须收到Reduce");
		Thread.sleep(200); // 等B完全park（leaveAndWaitNotify）

		// 3. 模拟守护kick B：此后B的acquire延续阶段重读getUserState()得到null
		kickSession(SERVER_ID_B);

		// 4. A应答降级成功（降到Invalid），唤醒B的延续阶段
		reduce.Result.globalKey = reduce.Argument.globalKey; // encode要求非null
		reduce.Result.state = GlobalCacheManagerConst.StateInvalid;
		reduce.SendResult();

		// 5. 核心断言：申请位必须被复位（修复前：sender==null的NPE逃逸后永久停留StateShare）
		waitPending(GlobalCacheManagerConst.StateInvalid, 10_000, "kick后申请位必须复位");

		// 6. 第三方C的Acquire必须能得到应答（修复前：申请位泄漏冻结key，C只能超时）
		clientC = new RawClient("UnitTest.FND_S1_2.C");
		var socketC = clientC.connect();
		login(socketC, SERVER_ID_C);
		var acquireC = new Acquire(KEY, GlobalCacheManagerConst.StateModify);
		Assertions.assertTrue(acquireC.SendForWait(socketC, 10_000).await(10_000),
				"C的Acquire必须收到应答（key不能冻结）");
		Assertions.assertFalse(acquireC.isTimeout(), "修复前申请位泄漏会冻结key，C只能超时");
	}
}
