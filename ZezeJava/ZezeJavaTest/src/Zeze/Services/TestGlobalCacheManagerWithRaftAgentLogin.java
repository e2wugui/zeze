package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose;
import Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.TcpSocket;
import Zeze.Raft.LeaderIs;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * GlobalCacheManagerWithRaftAgent.RaftAgent 登录/重登重试、切主等待与停机清理行为验证：
 * 1. 稳定 Leader 下首次 Login/ReLogin 返回 BindSocketFail 时无需切主即可自动退避重试成功，
 *    且携带 debugMode 与配置的 GlobalCacheManagerLoginTimeout；
 * 2. waitLoginSuccess() 期间触发 startNewLogin() 替换 future 并完成新 future 时正常返回，
 *    不抛 CancellationException 也不触发 FastFail；
 * 3. close() 期间存在待决 Login 时无递归、无重试定时器泄漏，且等待线程立即退出。
 */
@Fast
public class TestGlobalCacheManagerWithRaftAgentLogin {

	private static Application newTestApp(int serverId, int loginTimeoutMs) throws Exception {
		var config = new Config();
		config.setServerId(serverId);
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		config.setGlobalCacheManagerLoginTimeout(loginTimeoutMs);
		return new Application("UnitTest.RaftGcmAgentLogin." + serverId, config);
	}

	private static LeaderIs newLeaderIs(String leaderId, long term) throws Exception {
		var leaderIs = new LeaderIs();
		Object arg = leaderIs.Argument;
		var setLeaderId = arg.getClass().getDeclaredMethod("setLeaderId", String.class);
		setLeaderId.setAccessible(true);
		setLeaderId.invoke(arg, leaderId);
		var setTerm = arg.getClass().getDeclaredMethod("setTerm", long.class);
		setTerm.setAccessible(true);
		setTerm.invoke(arg, term);
		var setLeader = arg.getClass().getDeclaredMethod("setLeader", boolean.class);
		setLeader.setAccessible(true);
		setLeader.invoke(arg, true);
		return leaderIs;
	}

	@Test
	@Timeout(60)
	public void testStableLeaderRetriesLoginAndReLoginAfterBindSocketFail(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var loginAttempts = new AtomicInteger();
		var reloginAttempts = new AtomicInteger();
		var observedDebugMode = new AtomicBoolean();
		var serverSocketRef = new AtomicReference<@Nullable AsyncSocket>();
		var portRef = new AtomicInteger();

		var fakeServer = new HandshakeServer("UnitTest.FakeRaftGcmServer", new Config()) {
			{
				AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(Login::new, rpc -> {
					observedDebugMode.set(rpc.Argument.isDebugMode());
					if (loginAttempts.incrementAndGet() == 1) {
						rpc.SendResultCode(GlobalCacheManagerConst.LoginBindSocketFail);
						return 0L;
					}
					rpc.Result.setMaxNetPing(1500);
					rpc.Result.setServerProcessTime(10_000);
					rpc.Result.setServerReleaseTimeout(60_000);
					rpc.SendResultCode(0);
					return 0L;
				}, TransactionLevel.None, DispatchMode.Direct));
				AddFactoryHandle(ReLogin.TypeId_, new ProtocolFactoryHandle<>(ReLogin::new, rpc -> {
					if (reloginAttempts.incrementAndGet() == 1) {
						rpc.SendResultCode(GlobalCacheManagerConst.ReLoginBindSocketFail);
						return 0L;
					}
					rpc.SendResultCode(0);
					return 0L;
				}, TransactionLevel.None, DispatchMode.Direct));
				AddFactoryHandle(NormalClose.TypeId_, new ProtocolFactoryHandle<>(NormalClose::new, rpc -> {
					rpc.SendResultCode(0);
					return 0L;
				}, TransactionLevel.None, DispatchMode.Direct));
				AddFactoryHandle(LeaderIs.TypeId_, new ProtocolFactoryHandle<>(LeaderIs::new, null,
						TransactionLevel.None, DispatchMode.Direct));
			}

			@Override
			public void OnHandshakeDone(AsyncSocket so) throws Exception {
				super.OnHandshakeDone(so);
				serverSocketRef.set(so);
				newLeaderIs("127.0.0.1_" + portRef.get(), 1L).Send(so);
			}
		};

		GlobalCacheManagerWithRaftAgent gcmAgent = null;
		try {
			var listener = (TcpSocket)fakeServer.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
			int port = requireNonNull(listener.getLocalInet()).getPort();
			portRef.set(port);
			var raftXml = tempDir.resolve("raft-gcm-retry.xml");
			Files.writeString(raftXml, """
					<?xml version="1.0" encoding="utf-8"?>
					<raft Name="test_raft_gcm_login_retry">
						<node Host="127.0.0.1" Port="%d"/>
					</raft>
					""".formatted(port));

			var app = newTestApp(7001, 12_345);
			gcmAgent = new GlobalCacheManagerWithRaftAgent(app, new String[]{raftXml.toString()});
			// start() -> waitLoginSuccess()：首次 Login 收到 LoginBindSocketFail 后，稳定 Leader 不再发 LeaderIs，
			// RaftAgent 必须自动退避重试并在第二次 Login 完成后返回。
			gcmAgent.start();

			var raftAgent = gcmAgent.getAgents()[0];
			Assertions.assertEquals(2, loginAttempts.get(), "Login must retry on LoginBindSocketFail");
			Assertions.assertEquals(1L, raftAgent.getLoginTimes().get());
			Assertions.assertEquals(Reflect.inDebugMode, observedDebugMode.get(), "Login must carry Reflect.inDebugMode");

			// 触发同一 Leader 新 term 的 ReLogin：首次返回 ReLoginBindSocketFail，无需再次切主即可重试成功。
			var so = requireNonNull(serverSocketRef.get());
			newLeaderIs("127.0.0.1_" + port, 2L).Send(so);
			long deadline = System.currentTimeMillis() + 15_000;
			while (raftAgent.getLoginTimes().get() < 2 && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(2L, raftAgent.getLoginTimes().get(), "ReLogin must retry and succeed");
			Assertions.assertEquals(2, reloginAttempts.get(), "ReLogin must retry on ReLoginBindSocketFail");
			raftAgent.waitLoginSuccess();
		} finally {
			if (gcmAgent != null)
				gcmAgent.stop();
			fakeServer.stop();
		}
	}

	@Test
	@Timeout(60)
	public void testWaitLoginSuccessSurvivesStartNewLoginWithoutFastFail(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftXml = tempDir.resolve("raft-gcm-replace.xml");
		Files.writeString(raftXml, """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="test_raft_gcm_replace_future">
					<node Host="127.0.0.1" Port="19631"/>
				</raft>
				""");
		var app = newTestApp(7002, 5000);
		var gcmAgent = new GlobalCacheManagerWithRaftAgent(app, new String[]{raftXml.toString()});
		var workers = Executors.newSingleThreadExecutor();
		try {
			var raftAgent = gcmAgent.getAgents()[0];
			var waiter = workers.submit(() -> {
				raftAgent.waitLoginSuccess();
				return Boolean.TRUE;
			});

			Thread.sleep(200);
			var startNewLogin = GlobalCacheManagerWithRaftAgent.RaftAgent.class.getDeclaredMethod("startNewLogin");
			startNewLogin.setAccessible(true);
			@SuppressWarnings("unchecked")
			var replacement = (TaskCompletionSource<Boolean>)requireNonNull(startNewLogin.invoke(raftAgent));
			Thread.sleep(200);
			replacement.setResult(true);

			Assertions.assertTrue(waiter.get(10, TimeUnit.SECONDS),
					"waitLoginSuccess must re-read replaced future and succeed instead of throwing CancellationException");

			var lastErrorTimeField = GlobalCacheManagerWithRaftAgent.RaftAgent.class.getDeclaredField("lastErrorTime");
			lastErrorTimeField.setAccessible(true);
			Assertions.assertEquals(0L, lastErrorTimeField.getLong(raftAgent),
					"leader switch future replacement must not trigger FastFail");
		} finally {
			workers.shutdownNow();
			gcmAgent.stop();
		}
	}

	@Test
	@Timeout(60)
	public void testCloseWithPendingLoginDoesNotRecurseAndUnblocksWaiter(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var raftXml = tempDir.resolve("raft-gcm-close.xml");
		Files.writeString(raftXml, """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="test_raft_gcm_close_pending">
					<node Host="127.0.0.1" Port="19632"/>
				</raft>
				""");
		var app = newTestApp(7003, 15_000);
		var gcmAgent = new GlobalCacheManagerWithRaftAgent(app, new String[]{raftXml.toString()});
		var workers = Executors.newSingleThreadExecutor();
		try {
			var raftAgent = gcmAgent.getAgents()[0];
			var onSetLeader = GlobalCacheManagerWithRaftAgent.RaftAgent.class
					.getDeclaredMethod("raftOnSetLeader", Zeze.Raft.Agent.class);
			onSetLeader.setAccessible(true);
			onSetLeader.invoke(raftAgent, raftAgent.getRaftClient());

			var pendingField = Zeze.Raft.Agent.class.getDeclaredField("pending");
			pendingField.setAccessible(true);
			var pending = (Zeze.Util.ConcurrentHashMapOrdered<?, ?>)requireNonNull(
					pendingField.get(raftAgent.getRaftClient()));
			Assertions.assertEquals(1, pending.size(), "Login rpc must be pending");
			var pendingLogin = (Login)pending.iterator().next();
			Assertions.assertEquals(15_000, pendingLogin.getTimeout(),
					"Login must apply GlobalCacheManagerLoginTimeout");
			Assertions.assertEquals(Reflect.inDebugMode, pendingLogin.Argument.isDebugMode(),
					"Login must carry Reflect.inDebugMode");

			var waiter = workers.submit(() -> {
				try {
					raftAgent.waitLoginSuccess();
					return null;
				} catch (Throwable e) {
					return e;
				}
			});

			Thread.sleep(200);
			long closeAt = System.currentTimeMillis();
			Assertions.assertDoesNotThrow(gcmAgent::stop);

			var ex = waiter.get(10, TimeUnit.SECONDS);
			long elapsedAfterClose = System.currentTimeMillis() - closeAt;
			Assertions.assertNotNull(ex, "waitLoginSuccess must fail after close");
			Assertions.assertInstanceOf(IllegalStateException.class, ex);
			Assertions.assertTrue(requireNonNull(ex.getMessage()).contains("closed"),
					"expect closed error, but: " + ex.getMessage());
			Assertions.assertTrue(elapsedAfterClose < 5_000,
					"waitLoginSuccess must exit immediately on close, elapsed=" + elapsedAfterClose);

			var retryTaskField = GlobalCacheManagerWithRaftAgent.RaftAgent.class.getDeclaredField("loginRetryTask");
			retryTaskField.setAccessible(true);
			Assertions.assertNull(retryTaskField.get(raftAgent), "no retry timer may leak after close");
		} finally {
			workers.shutdownNow();
			gcmAgent.stop();
		}
	}
}
