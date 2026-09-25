package UnitTest.Zeze.Services;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND14 svc-01回归：同步版release无incarnation判别——NormalClose的释放循环是全服务唯一
 * 不持session锁的在飞release：旧release在pending上阻塞期间，同serverId新连接ReLogin重绑
 * （保留所有权语义），延迟release醒来后 cs.modify==sender / cs.share.remove(sender) 恒命中
 * 同一常驻CacheHolder（sessions按serverId复用），把重绑会话保留的所有权连根移除（唯一持有者
 * 时global.remove）：客户端认为仍持有却被服务端撤销，第三方无竞争再获Modify即双写。
 * 修复：CacheHolder世代号generation（kick与成功重绑各递增，判例移植自异步版FND11 svc-03），
 * release进入时捕获、移除段前复查，不匹配即跳过移除（fail-closed：宁保留可收敛不误杀）。
 * <p>
 * 构造性场景（进程内同步服务器+裸协议客户端，Reduce到达后park，时序全部由测试控制）：
 * 1. A持Modify，S经降级与A共持Share；
 * 2. T acquireModify占住申请位，对A/S的Reduce均被park；
 * 3. S发NormalClose：快照{KEY}→解绑→释放循环不持session锁，阻塞等T的pending；
 * 4. S2（新连接）ReLogin重绑（世代+1，此后旧release全部过期）——不重新Acquire：
 *    ReLogin保留的所有权即被测对象，且消除与release唤醒次序的竞态；
 * 5. A应答降级+S死连接reduce超时→pending复位并唤醒延迟release到达移除门，
 *    NormalClose应答即释放循环完成的确证（替代固定宽限sleep）。
 * 断言：S的acquired保留KEY、CacheState身份不变（修复前：share清空+global.remove）。
 */
@Fast
// 同步版是进程内单例（CacheHolder硬编码引用static instance）；与其他使用该单例的@Fast测试
// 经ResourceLock串行（对齐TestFnd717/718判例）。
@ResourceLock("GlobalCacheManagerServer.instance")
public class TestFnd14Svc01GcmSyncReleaseGeneration {
	private static final int PORT = 19713; // @Fast固定端口独占
	private static final Binary KEY = new Binary("UnitTest.FND14_svc01.Key".getBytes(StandardCharsets.UTF_8));
	private static final int SERVER_ID_A = 9131;
	private static final int SERVER_ID_S = 9132;
	private static final int SERVER_ID_T = 9133;

	private static RawClient clientA;
	private static RawClient clientS;
	private static RawClient clientS2;
	private static RawClient clientT;

	private static GlobalCacheManagerServer gcm;

	/** 裸协议客户端：Login/ReLogin/Acquire正常收发；Reduce到达后park住由测试控制应答时机。 */
	private static final class RawClient extends Service {
		private final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();

		RawClient(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(ReLogin.TypeId_, new ProtocolFactoryHandle<>(
					ReLogin::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			// NormalClose的应答解码需要工厂（本测试发起NormalClose；handle为null，应答走rpc上下文）
			AddFactoryHandle(NormalClose.TypeId_, new ProtocolFactoryHandle<>(
					NormalClose::new, null, TransactionLevel.None, DispatchMode.Direct));
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
		gcm = GlobalCacheManagerServer.getInstance();
		// reduceTimeout（=maxNetPing+serverProcessTime）设~10s：S死连接上的reduce超时决定
		// pending解除与延迟release唤醒时刻，必须显著大于测试步进间隔。
		var cfg = gcm.getGcmConfig();
		setInt(cfg, "maxNetPing", 1000);
		setInt(cfg, "serverProcessTime", 9000);
		gcm.start(null, PORT, null);
	}

	private static void setInt(Object obj, String fieldName, int value) throws Exception {
		var field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.setInt(obj, value);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		for (var c : new RawClient[]{clientA, clientS, clientS2, clientT})
			if (c != null)
				c.stop();
		if (gcm != null) {
			gcm.stop();
			gcm = null;
		}
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
		var field = GlobalCacheManagerServer.class.getDeclaredField("global");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var global = (ConcurrentHashMap<Binary, Object>)field.get(gcm);
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

	private static Object sessionOf(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerServer.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		var sessions = (LongConcurrentHashMap<?>)sessionsField.get(gcm);
		var holder = sessions.get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		return holder;
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
	@Timeout(90)
	public void testNormalCloseDelayedReleaseMustNotRevokeRebindPreserved() throws Exception {
		// 1. A持Modify；S申请Share触发对A的降级，A应答StateShare后双方共持Share
		clientA = new RawClient("UnitTest.FND14_svc01.A");
		var socketA = clientA.connect();
		login(socketA, SERVER_ID_A);
		var acquireA = new Acquire(KEY, GlobalCacheManagerConst.StateModify);
		Assertions.assertTrue(acquireA.SendForWait(socketA, 10_000).await(10_000), "A acquire await");
		Assertions.assertEquals(0, acquireA.getResultCode(), "A acquire resultCode");

		clientS = new RawClient("UnitTest.FND14_svc01.S");
		var socketS = clientS.connect();
		login(socketS, SERVER_ID_S);
		var acquireS = new Acquire(KEY, GlobalCacheManagerConst.StateShare);
		var futureS = acquireS.SendForWait(socketS, 30_000); // 挂起等A降级，稍后await
		var reduceA1 = clientA.reduces.poll(10, java.util.concurrent.TimeUnit.SECONDS);
		Assertions.assertNotNull(reduceA1, "A必须收到首个Reduce");
		reduceA1.Result.globalKey = reduceA1.Argument.globalKey;
		reduceA1.Result.state = GlobalCacheManagerConst.StateShare; // 降级共存
		reduceA1.SendResult();
		Assertions.assertTrue(futureS.await(30_000), "S acquire must complete");
		Assertions.assertEquals(0, acquireS.getResultCode(), "S acquire resultCode");
		var csAtSetup = cacheStateOf(KEY); // 身份基准：修复前终局将被移除

		// 2. T acquireModify占住申请位；对A/S的Reduce均park（不自动应答）
		clientT = new RawClient("UnitTest.FND14_svc01.T");
		var socketT = clientT.connect();
		login(socketT, SERVER_ID_T);
		var acquireT = new Acquire(KEY, GlobalCacheManagerConst.StateModify);
		acquireT.SendForWait(socketT, 60_000); // 结果不重要（预期失败），不await
		waitPending(GlobalCacheManagerConst.StateModify, 10_000, "T必须占住申请位");
		var reduceA2 = clientA.reduces.poll(10, java.util.concurrent.TimeUnit.SECONDS);
		Assertions.assertNotNull(reduceA2, "A必须收到第二个Reduce");
		// S的reduce同样park：S的所有权只能由延迟release移除（移除源唯一化）
		var reduceS1 = clientS.reduces.poll(10, java.util.concurrent.TimeUnit.SECONDS);
		Assertions.assertNotNull(reduceS1, "S必须收到Reduce");
		Thread.sleep(200); // 等T完全park

		// 3. S发NormalClose：快照{KEY}→解绑（此后S2可重绑）→释放循环不持session锁，阻塞等T的pending
		var normalClose = new NormalClose();
		var futureNc = normalClose.SendForWait(socketS, 60_000); // 循环结束才有应答，稍后await作完成确证
		var holderS = sessionOf(SERVER_ID_S);
		var sessionIdField = holderS.getClass().getDeclaredField("sessionId");
		sessionIdField.setAccessible(true);
		long deadline = System.currentTimeMillis() + 10_000;
		while ((long)sessionIdField.get(holderS) != 0 && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(20);
		Assertions.assertEquals(0L, sessionIdField.get(holderS), "NormalClose必须先解绑（此后S2可重绑）");
		Thread.sleep(200); // 宽限：解绑到release进入pending等待是同一handler线程的相邻两步，确保世代捕获先于S2重绑

		// 4. S复活：新连接ReLogin重绑（世代+1，此后旧release全部过期）——不重新Acquire：
		// ReLogin保留的所有权即被测对象（重新Acquire会先撞DeadLock检查21，不可达）
		clientS2 = new RawClient("UnitTest.FND14_svc01.S2");
		var socketS2 = clientS2.connect();
		var relogin = new ReLogin();
		relogin.Argument.serverId = SERVER_ID_S;
		relogin.Argument.globalCacheManagerHashIndex = 0;
		Assertions.assertTrue(relogin.SendForWait(socketS2, 10_000).await(10_000), "relogin await");
		Assertions.assertEquals(0, relogin.getResultCode(), "relogin resultCode（无释放屏障）");

		// 5. A应答降级到Invalid；S死连接的reduce由超时兜底→T失败释放申请位并唤醒延迟release
		reduceA2.Result.globalKey = reduceA2.Argument.globalKey;
		reduceA2.Result.state = GlobalCacheManagerConst.StateInvalid;
		reduceA2.SendResult();
		waitPending(GlobalCacheManagerConst.StateInvalid, 30_000, "pending必须复位");
		// 释放循环完成的确证：NormalClose应答到达（延迟release已按世代跳过移除）
		Assertions.assertTrue(futureNc.await(30_000), "NormalClose释放循环必须完成");

		// 6. 核心断言：重绑会话保留的所有权不得被延迟release撤销
		var acquiredField = holderS.getClass().getDeclaredField("acquired");
		acquiredField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var acquired = (ConcurrentHashMap<Binary, Integer>)acquiredField.get(holderS);
		Assertions.assertTrue(acquired.containsKey(KEY), "重绑后保留的所有权不得被延迟release撤销");
		Assertions.assertSame(csAtSetup, cacheStateOf(KEY), "仍有持有者，CacheState不得被移除重建");
		Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingOf(KEY), "pending复位");
	}
}
