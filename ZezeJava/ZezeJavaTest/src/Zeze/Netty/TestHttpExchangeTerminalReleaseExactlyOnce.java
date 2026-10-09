package Zeze.Netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 终态释放必须有单一执行者：end-stream任务finally（worker线程）与close的
 * closeInEventLoop（EventLoop）并发到达时，request只允许被release恰好一次。
 * 修复前两条线程各自"判空→release→置空"，交错时双双读到非空request，
 * 双重release触发IllegalReferenceCountException（池化缓冲复用后还会破坏他人请求）。
 * 修复后终态释放统一由EventLoop执行，worker只投递通知。
 */
@Fast
public class TestHttpExchangeTerminalReleaseExactlyOnce {

	// release入口计数+屏障放大窗口：两个释放者同时到达时屏障同步放行（确定性双release）；
	// 只有一个释放者时500ms超时自行放行（修复后的正常路径）。
	static final class GatedRequest extends DefaultFullHttpRequest {
		final AtomicInteger releaseArrivals = new AtomicInteger();
		private final CyclicBarrier bothInRelease;

		GatedRequest(CyclicBarrier bothInRelease) {
			super(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", Unpooled.buffer(1).writeByte(42));
			this.bothInRelease = bothInRelease;
		}

		@Override
		public boolean release() {
			releaseArrivals.incrementAndGet();
			try {
				bothInRelease.await(500, TimeUnit.MILLISECONDS);
			} catch (Exception ignored) {
			}
			return super.release();
		}
	}

	@Test
	public void concurrentEndStreamFinallyAndCloseReleaseExactlyOnce() throws Exception {
		Task.tryInitThreadPool();
		var group = new DefaultEventLoopGroup(1);
		var server = new HttpServer();
		var bothInRelease = new CyclicBarrier(2);
		var request = new GatedRequest(bothInRelease);
		var workerFailure = new AtomicReference<Throwable>();
		var exchangeReady = new CountDownLatch(1);
		final HttpExchange[] exchange = new HttpExchange[1];
		try {
			var ch = new LocalChannel();
			ch.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
				@Override
				public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise p) {
					io.netty.util.ReferenceCountUtil.release(msg);
					p.setSuccess();
				}
			});
			ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
				@Override
				public void handlerAdded(ChannelHandlerContext ctx) {
					var x = new HttpExchange(server, ctx);
					x.request = request;
					x.handler = new HttpHandler(1024, TransactionLevel.None, DispatchMode.Normal, ignored -> {
					});
					x.endStreamTaskPending = true;
					exchange[0] = x;
					exchangeReady.countDown();
				}
			});
			group.register(ch).sync();
			Assertions.assertTrue(exchangeReady.await(10, TimeUnit.SECONDS));

			// worker执行真实的end-stream收尾（close(null)→finally释放路径），
			// close的空写future由EventLoop完成并触发closeInEventLoop。
			var worker = new Thread(() -> {
				try {
					exchange[0].invokeEndStream();
				} catch (Throwable e) {
					workerFailure.set(e);
				}
			}, "end-stream-finally");
			worker.start();
			worker.join(10_000);
			// 等EventLoop侧（closeInEventLoop或投递的释放任务）跑完
			group.next().submit(() -> {
			}).await(10, TimeUnit.SECONDS);
			ch.close().sync();

			Assertions.assertEquals(1, request.releaseArrivals.get(),
					"request的终态release必须恰好一次（终态释放单一执行者）");
			Assertions.assertNull(workerFailure.get(), "worker侧不得出现release竞态异常");
			Assertions.assertEquals(0, request.refCnt(), "必须完整释放");
			Assertions.assertNull(exchange[0].request(), "释放后request置空");
		} finally {
			group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
		}
	}
}
