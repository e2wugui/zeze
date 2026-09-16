package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 复审R3回归（FND7-S1①/C③，判例 d2d7cf2bb 同型）：Service.stop 在持 Service 锁时调用
 * keepCheckTimer.cancel(true)。checkKeepAlive 的 tick 体在 TimerFuture 锁内执行
 * （Task.schedulePeriodCore 持 future.lock 跑 body），其中 onKeepAliveTimeout→socket.close
 * 会在 tick 线程【同步】回调 OnSocketClose——若子类覆写在回调中取本 Service 锁（现实实例：
 * RedoQueue.OnSocketClose），则 stop 持 Service 锁等 TimerFuture 锁（cancel 天然 join 在飞
 * tick）、tick 持 TimerFuture 锁在 OnSocketClose 里等 Service 锁，互喂永久挂起。
 * 修复后：stop 锁内只捕获句柄并置 null，cancel 在锁外调用（对齐 LoginQueue.stop 等三处
 * d2d7cf2bb 形态）。
 * 时序全由测试控制：把 tick 停在"已赢得close的CAS、已进入OnSocketClose、未取Service锁"，
 * 令 stop 先到达cancel，再放行tick——缺陷形态放行后仍互喂（红），修复形态tick取锁即过（绿）。
 * 自包含（本机随机端口），标 @Fast。
 */
@Fast
public class TestFnd7R3ServiceStopKeepCheckCancelUnderLock {
	/** OnSocketClose 覆写取 Service 锁（RedoQueue 形态）；并在取锁前设可控停点。 */
	private static final class ServiceEx extends Service {
		final CountDownLatch inCloseCallback = new CountDownLatch(1);
		final CountDownLatch proceedToLock = new CountDownLatch(1);

		ServiceEx() {
			super("TestFnd7R3StopCancel", new Config());
			// keepCheck：period=1s，KeepRecvTimeout=1s（活跃时间1秒即超时触发onKeepAliveTimeout）
			var opt = getConfig().getHandshakeOptions();
			opt.setKeepCheckPeriod(1);
			opt.setKeepRecvTimeout(1);
		}

		@Override
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			super.OnSocketAccept(so);
			so.setActiveRecvTime(); // FND7-63判据要求活跃时间曾被更新才参与检查
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			inCloseCallback.countDown();
			//noinspection ResultOfMethodCallIgnored
			proceedToLock.await(); // 停在"已进入回调（tick线程已赢得close的CAS）、未取Service锁"
			lock(); // 子类覆写取本Service锁（RedoQueue.OnSocketClose形态）
			try {
				super.OnSocketClose(so, e);
			} finally {
				unlock();
			}
		}
	}

	@Test
	@Timeout(60)
	public void testStopMustNotCancelKeepCheckTimerUnderServiceLock() throws Exception {
		Task.tryInitThreadPool();
		var server = new ServiceEx();
		var client = new Service("TestFnd7R3StopCancelClient", new Config());
		ExecutorService stopExecutor = null;
		try {
			int port;
			try (var s = new java.net.ServerSocket()) {
				s.bind(new InetSocketAddress("127.0.0.1", 0));
				port = s.getLocalPort();
			}
			server.newServerSocket(new InetSocketAddress("127.0.0.1", port), null);
			server.start(); // 启动keepCheck定时器（KeepCheckPeriod=1s）
			client.newClientSocket("127.0.0.1", port, null, null);

			// 等待tick触发：连接建立1秒后KeepRecvTimeout超时，tick线程close→OnSocketAccept侧
			// socket进入本类OnSocketClose停点（持TimerFuture锁、已赢close的CAS、未取Service锁）。
			Assertions.assertTrue(server.inCloseCallback.await(15, TimeUnit.SECONDS),
					"keepCheck tick必须在15s内进入OnSocketClose停点");

			// 后台启动stop：缺陷形态——持Service锁到达cancel等TimerFuture锁（永久）；
			// 修复形态——锁内段完成即解锁，cancel在锁外等TimerFuture锁。
			stopExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "UnitTest.FND7R3.stop"));
			var stopFuture = stopExecutor.submit(() -> {
				server.stop();
				return null;
			});
			Thread.sleep(1_000); // 给stop到达cancel留足时间（两形态都在等TimerFuture锁）

			// 放行tick：修复形态——OnSocketClose取Service锁（已释放）即过，tick完成释放
			// TimerFuture锁，cancel返回，stop完成；缺陷形态——tick在lock()上等stop持有的
			// Service锁、stop在cancel上等tick持有的TimerFuture锁，放行不解除。
			server.proceedToLock.countDown();
			stopFuture.get(10, TimeUnit.SECONDS); // 缺陷形态TimeoutException → 红
		} finally {
			server.proceedToLock.countDown();
			if (stopExecutor != null)
				stopExecutor.shutdownNow();
			// 停机兜底放守护线程且不join：缺陷形态下stop线程已死锁持Service锁，本线程再调stop
			// 会在同一把锁上永久挂起——测试线程必须无条件返回（红由stopFuture.get超时断言给出）。
			var cleanup = new Thread(() -> {
				try {
					server.stop();
					client.stop();
				} catch (Exception ignored) {
				}
			}, "UnitTest.FND7R3.cleanup");
			cleanup.setDaemon(true);
			cleanup.start();
		}
	}
}
