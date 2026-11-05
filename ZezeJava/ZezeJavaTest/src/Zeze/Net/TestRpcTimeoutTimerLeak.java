package Zeze.Net;

import harness.Fast;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TimeoutManager;
import demo.Module1.BValue;

/**
 * Rpc 超时兜底定时器的生命周期契约（轮子换代版，断言对象=TimeoutManager 秒桶条目数）：
 * 1. 应答成功消费上下文时定时器必须被取消且条目移出秒桶（cancelTimeout→Timeout.cancel→
 *    TimeoutSet.remove）——不取消则定时器λ捕获的 rpc 全家桶滞留=速率×超时窗口（2026-11-01
 *    soak bench OOM 根因）。旧版断言 ScheduledThreadPoolExecutor 队列快照，Rpc 上轮子后对
 *    Rpc 恒真，且共享池残留制造降级窗假红（2026-11-05 台账），改钉轮子真身。
 * 2. 无应答时超时兜底路径不受取消逻辑影响：定时器照常触发，future 异常完成，桶排水后条目归零。
 */
@Fast
public class TestRpcTimeoutTimerLeak {
	private static final int NO_ANSWER_MARKER = Integer.MIN_VALUE; // 服务端见此值不回包，留给超时路径

	@Test
	public void testTimeoutTimerCancelledAfterResponse() throws Exception {
		var connected = new Zeze.Util.TaskCompletionSource<AsyncSocket>();
		var server = new Service("TestRpcTimerLeak.Server");
		Zeze.Util.Task.tryInitThreadPool();
		var probe = new TimerRpc();
		server.AddFactoryHandle(probe.getTypeId(),
				new Service.ProtocolFactoryHandle<>(TimerRpc::new, TestRpcTimeoutTimerLeak::processRequest));
		var listener = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		Client client = null;
		try {
			var local = listener.getLocalInet();
			Assertions.assertNotNull(local, "listen socket local address");
			int port = local.getPort();

			client = new Client(connected);
			client.AddFactoryHandle(probe.getTypeId(), new Service.ProtocolFactoryHandle<>(TimerRpc::new));

			AsyncSocket socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(connected.get(10, TimeUnit.SECONDS), "10s内连接必须建立");

			var manager = Task.getTimeoutManager();
			int baseline = manager.size();
			final int count = 500;
			for (int i = 0; i < count; ++i) {
				var rpc = new TimerRpc();
				rpc.Argument.setInt_1(i);
				rpc.SendForWait(socket, 60_000).await();
				// await 返回即应答已消费：removeRpcContext 已收口 cancelTimeout，
				// cancel 链必须把条目真正移出秒桶——轮上条目数应回落基线
				Assertions.assertEquals(i, rpc.Result.getInt_1(), "echo integrity");
			}
			awaitWheelDrained(manager, baseline,
					"timeout timers not cancelled after response (" + count + " rpcs sent)");
		} finally {
			if (client != null)
				client.stop();
			server.stop();
		}
	}

	@Test
	public void testTimeoutFallbackStillFires() throws Exception {
		var connected = new Zeze.Util.TaskCompletionSource<AsyncSocket>();
		var server = new Service("TestRpcTimerLeak.Server2");
		Zeze.Util.Task.tryInitThreadPool();
		var probe = new TimerRpc();
		server.AddFactoryHandle(probe.getTypeId(),
				new Service.ProtocolFactoryHandle<>(TimerRpc::new, TestRpcTimeoutTimerLeak::processRequest));
		var listener = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		Client client = null;
		try {
			int port = listener.getLocalInet().getPort();
			client = new Client(connected);
			client.AddFactoryHandle(probe.getTypeId(), new Service.ProtocolFactoryHandle<>(TimerRpc::new));
			AsyncSocket socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(connected.get(10, TimeUnit.SECONDS), "10s内连接必须建立");

			var manager = Task.getTimeoutManager();
			int baseline = manager.size();

			var rpc = new TimerRpc();
			rpc.Argument.setInt_1(NO_ANSWER_MARKER); // 服务端不回包
			var future = rpc.SendForWait(socket, 300);
			// TCS.get 的异常完成统一包装为 CompletionException（TaskCompletionSource.toResult），
			// cause 才是 RpcTimeoutException——断言要对 API 实际抛出形态。
			var ex = Assertions.assertThrows(java.util.concurrent.CompletionException.class,
					() -> future.get(5, TimeUnit.SECONDS), "无应答时future必须经超时异常完成");
			Assertions.assertTrue(ex.getCause() instanceof Zeze.Net.RpcTimeoutException,
					"cause must be RpcTimeoutException but was " + ex.getCause());
			Assertions.assertTrue(rpc.isTimeout(), "isTimeout flag");

			// 超时任务已触发并排水：桶内条目数回到基线
			awaitWheelDrained(manager, baseline, "fired timeout task must leave the wheel buckets");
		} finally {
			if (client != null)
				client.stop();
			server.stop();
		}
	}

	/** 等待式断言（轮子换代版，取代旧 STPE 队列快照——快照在共享池残留下降级窗假红，见
	 * 假红台账 2026-11-05）：轮上条目回到基线+松弛（+2 给并行类在飞条目），有界 10s，
	 * 超时带计数红。size() 与并发 cancel 竞态只偏大，轮询必然收敛。 */
	private static void awaitWheelDrained(TimeoutManager manager, int baseline, String msg) {
		var deadline = System.nanoTime() + 10_000_000_000L;
		while (manager.size() > baseline + 2) {
			if (System.nanoTime() - deadline > 0)
				Assertions.fail(msg + ": wheel size " + manager.size() + " > baseline " + baseline + " + 2 after 10s");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				Assertions.fail(msg + ": interrupted while waiting for wheel to drain");
			}
		}
	}

	public static long processRequest(Protocol<?> p) {
		var rpc = (TimerRpc)p;
		if (rpc.Argument.getInt_1() == NO_ANSWER_MARKER)
			return Procedure.Success; // 模拟服务端处理但不回包：在飞上下文留给超时兜底
		rpc.Result.setInt_1(rpc.Argument.getInt_1());
		rpc.SendResult();
		return Procedure.Success;
	}

	public static class TimerRpc extends Rpc<BValue, BValue> {
		public TimerRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 1;
		}

		@Override
		public int getProtocolId() {
			return -2;
		}
	}

	public static class Client extends Service {
		private final Zeze.Util.TaskCompletionSource<AsyncSocket> connected;

		public Client(Zeze.Util.TaskCompletionSource<AsyncSocket> connected) {
			super("TestRpcTimerLeak.Client");
			this.connected = connected;
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			connected.setResult(so);
		}
	}
}
