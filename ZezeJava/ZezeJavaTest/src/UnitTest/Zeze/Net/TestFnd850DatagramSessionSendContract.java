package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;

import Zeze.Net.Rpc;
import Zeze.Net.RpcSocketDisposedException;
import Zeze.Net.Service;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-50回归：DatagramSession.Send在sendTo抛IOException时以RuntimeException上报，
 * 违反AsyncSocket布尔契约——上层Rpc.Send在addRpcContext之后被异常穿透，
 * rpcContexts条目永久泄漏（超时定时器从未注册，无兜底）；且入口不检查closed，
 * close后Send仍真实发出数据报并谎报成功。
 * 修复后：IO失败close+返回false（对齐TcpSocket），入口检查closed；
 * close接入OnSocketDisposed（在飞Rpc立即失败，FND8-50补充/FND8-44同点）。
 */
@Fast
public class TestFnd850DatagramSessionSendContract {

	public static class NoReplyRpc extends Rpc<BValue, BValue> {
		public NoReplyRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 93;
		}

		@Override
		public int getProtocolId() {
			return 50;
		}
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	// 会话/整个socket关闭后Send必须返回false（不抛异常、不真实发出数据报）
	@Test
	public void testSendAfterCloseReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("test.fnd850.a");
		try {
			var socketA = service.bindUdp(new InetSocketAddress(0));
			var sessionA = socketA.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
			sessionA.close(null);
			Assertions.assertFalse(sessionA.Send(new byte[]{1, 2, 3}, 0, 3), "会话关闭后Send(byte[])必须false");
			Assertions.assertFalse(sessionA.Send(new NoReplyRpc()), "会话关闭后Send(Protocol)必须false");

			// socket整体关闭的级联路径同样：入口检查兜住（不再依赖sendTo抛异常）
			var socketB = service.bindUdp(new InetSocketAddress(0));
			var sessionB = socketB.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
			socketB.close();
			Assertions.assertTrue(sessionB.isClosed());
			Assertions.assertFalse(sessionB.Send(new byte[]{1}, 0, 1), "socket关闭级联后Send必须false");
		} finally {
			service.stop();
		}
	}

	// 布尔契约的上层收益：closed会话上SendForWalk返回false路径正常清理上下文，
	// future立即以Send Fail失败——修复前异常穿出SendForWait、rpcContexts永驻
	@Test
	public void testRpcOnClosedSessionCleaned() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("test.fnd850.b");
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var session = socket.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
			session.close(null);

			var rpc = new NoReplyRpc();
			var future = rpc.SendForWait(session, 60_000);
			Assertions.assertTrue(future.isCompletedExceptionally(), "closed会话上Send必须返回false并失败future");
			Throwable cause = null;
			try {
				future.join();
			} catch (Throwable e) {
				cause = e.getCause() != null ? e.getCause() : e;
			}
			Assertions.assertTrue(cause instanceof IllegalStateException, "期望Send Fail，实际=" + cause);
			Assertions.assertTrue(service.getRpcContextsToSender(session).isEmpty(),
					"发送失败路径必须清理rpcContexts（修复前异常穿透致永久泄漏）");
		} finally {
			service.stop();
		}
	}

	// 补充修法：会话close将在飞Rpc立即以RpcSocketDisposedException失败（对齐TcpSocket家族），
	// 而非干等60s超时
	@Test
	public void testCloseDisposesInflightRpc() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("test.fnd850.c");
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var session = socket.createSessionServer(
					new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
			Assertions.assertFalse(session.isClosed());

			var future = new NoReplyRpc().SendForWait(session, 60_000); // 60s超时：失败只能来自dispose
			Assertions.assertFalse(future.isDone(), "发送成功后future不得立即完成");
			session.close(null);
			await("inflight rpc disposed on close", 5_000, future::isDone);
			Assertions.assertTrue(future.isCompletedExceptionally());
			Throwable cause = null;
			try {
				future.join();
			} catch (Throwable e) {
				cause = e.getCause() != null ? e.getCause() : e;
			}
			Assertions.assertTrue(cause instanceof RpcSocketDisposedException,
					"close必须在飞Rpc立即失败（RpcSocketDisposedException），实际=" + cause);
		} finally {
			service.stop();
		}
	}
}
