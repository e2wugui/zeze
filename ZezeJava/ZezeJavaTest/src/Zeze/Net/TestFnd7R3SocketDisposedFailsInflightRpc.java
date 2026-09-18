package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * 复审R3回归（FND7-S1③）：Service.OnSocketDisposed 一直是 no-op——连接关闭释放（dispose）后，
 * 仍挂在该连接上的在飞 Rpc 上下文无人唤醒，等待方（SendForWait 的 future / Send(handle) 的回调）
 * 只能干等 Rpc 超时（默认5s）：被踢/断线的同步调用方平白挂满超时预算，密集踢连接的场景
 * （GCM achillesHeelDaemon 踢死会话、服务stop）放大为成批的滞后唤醒。
 * 修复后：默认 OnSocketDisposed 经 getRpcContextsToSender+removeRpcContexts 收集并移除在飞
 * 上下文，future 立即以 RpcSocketDisposedException 失败、handle 立即以 ErrorSendFail 派发。
 * 与 Connector.autoReconnect 的交互（全部立即失败的理由）：框架层没有任何"在飞Rpc随重连重发"
 * 的机制——重连只重建socket，重发是应用层以新上下文重新Send（同实例重发注册新sid并移除旧条目），
 * 故立即失败不破坏任何重发语义；本测试同步覆盖future与handle两形态。
 * 自包含（本机随机端口），标 @Fast。
 */
@Fast
public class TestFnd7R3SocketDisposedFailsInflightRpc {
	private static boolean waitUntil(java.util.function.BooleanSupplier cond, long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (cond.getAsBoolean())
				return true;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		return cond.getAsBoolean();
	}

	@Test
	@Timeout(60) // 25s观察窗+10s连接等待+尾部断言的最坏路径余量（原30s配5s窗，窗扩后同步放宽）
	public void testInflightRpcFailsImmediatelyOnSocketDisposed() throws Exception {
		Task.tryInitThreadPool();
		// 服务端：收满两个KeepAlive请求后踢连接（不发应答）——复现"被踢连接在途Rpc"场景。
		var kickCount = new java.util.concurrent.atomic.AtomicInteger();
		var server = new Service("TestFnd7R3KickServer", new Config());
		server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
				rpc -> {
					if (kickCount.incrementAndGet() >= 2)
						rpc.getSender().close(); // 模拟kick：两个请求都在途后不给应答直接断
					return 0L;
				}, TransactionLevel.None, DispatchMode.Direct));
		var client = new Service("TestFnd7R3VictimClient", new Config());
		// client侧也注册KeepAlive工厂：dispose失败派发handle需要factoryHandle（Level/Mode）。
		client.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(KeepAlive::new,
				null, TransactionLevel.None, DispatchMode.Direct));
		try {
			int port;
			try (var s = new java.net.ServerSocket()) {
				s.bind(new InetSocketAddress("127.0.0.1", 0));
				port = s.getLocalPort();
			}
			server.newServerSocket(new InetSocketAddress("127.0.0.1", port), null);
			//noinspection ResultOfMethodCallIgnored
			client.newClientSocket("127.0.0.1", port, null, null);

			// 等连接建立进入client的socketMap（OnSocketConnected完成）。
			AsyncSocket connected = null;
			var deadline = System.currentTimeMillis() + 10_000;
			while (connected == null) {
				connected = client.GetSocket();
				if (connected == null) {
					if (System.currentTimeMillis() >= deadline)
						Assertions.fail("10s内连接未建立");
					//noinspection BusyWait
					Thread.sleep(20);
				}
			}

			// future形态：SendForWait，超时给足30s（红形态下等不到dispose唤醒）。
			var waitRpc = new KeepAlive();
			var future = waitRpc.SendForWait(connected, 30_000);
			// handle形态：Send(handle)，回调被派发即置latch。
			var handleInvoked = new CountDownLatch(1);
			var handleResultCode = new AtomicReference<Long>();
			var handleRpc = new KeepAlive();
			Assertions.assertTrue(handleRpc.Send(connected, (ProtocolHandle<Rpc<EmptyBean, EmptyBean>>)rpc -> {
				handleResultCode.set(rpc.getResultCode());
				handleInvoked.countDown();
				return 0L;
			}, 30_000), "handle形态发送必须成功");

			// 服务端kick后连接dispose：修复形态两等待方被dispose唤醒（先于30s Rpc超时）；
			// 缺陷形态OnSocketDisposed为no-op，只能等30s Rpc超时。
			// 判别靠唤醒者（异常类型/结果码，见下方断言），不靠墙钟竞速：dispose任务是共享selector
			// （Selectors.getInstance()进程级单例）上的排队任务，并行测试类的Direct重活内联在同一条
			// selector线程上执行时，dispose唤醒可晚于任何固定观察窗（30轮压测轮21/29的5s窗即此，
			// 非产品缺陷——唤醒仍先于Rpc超时发生）。25s窗+30s Rpc超时双保险：真缺陷（永不唤醒）在
			// 25s窗红；26-29s迟到者由异常类型断言兜住（超时唤醒的cause是TimeoutException）。
			Assertions.assertTrue(waitUntil(() -> future.isDone() && handleInvoked.getCount() == 0, 25_000),
					"连接dispose后在飞Rpc（future+handle两形态）必须已被dispose唤醒"
							+ "（缺陷形态：只能等30s Rpc超时，观察窗内无动静）");

			// future形态：异常类型可诊断（区别于超时），resultCode同步置ErrorSendFail。
			Assertions.assertTrue(future.isCompletedExceptionally(), "future必须以异常完成");
			var cause = new AtomicReference<Throwable>();
			try {
				future.get(1, TimeUnit.SECONDS);
			} catch (Exception e) {
				cause.set(e.getCause() != null ? e.getCause() : e);
			}
			Assertions.assertTrue(cause.get() instanceof RpcSocketDisposedException,
					() -> "期望RpcSocketDisposedException但得到: " + cause.get());
			Assertions.assertEquals(Procedure.ErrorSendFail, waitRpc.getResultCode());
			// handle形态：立即派发且带ErrorSendFail。
			Assertions.assertTrue(handleInvoked.await(5, TimeUnit.SECONDS), "handle必须被立即派发");
			Assertions.assertEquals(Procedure.ErrorSendFail, (long)handleResultCode.get());
		} finally {
			try {
				client.stop();
			} catch (Exception ignored) {
			}
			try {
				server.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
