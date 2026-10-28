package Zeze.Netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.DefaultHttpContent;
import org.junit.jupiter.api.Test;
import io.netty.handler.stream.ChunkedNioFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHttpTerminationAfterLoopShutdown {
	@Test
	void admittedStreamWorkerKeepsItsRequestUntilItReturns() throws Exception {
		Task.tryInitThreadPool();
		var server = new HttpServer();
		var group = new DefaultEventLoopGroup(1);
		var channel = new LocalChannel();
		channel.pipeline().addLast(new ChannelInboundHandlerAdapter());
		var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
		var content = new DefaultHttpContent(Unpooled.buffer().writeByte(1));
		var started = new CountDownLatch(1);
		var resume = new CountDownLatch(1);
		var observed = new AtomicReference<io.netty.handler.codec.http.HttpRequest>();
		var ready = new AtomicReference<HttpExchange>();
		try {
			group.register(channel).sync();
			channel.eventLoop().submit(() -> {
				var x = new HttpExchange(server, channel.pipeline().lastContext());
				x.request = request;
				x.inStreamMode = true;
				x.handler = new HttpHandler(TransactionLevel.None, DispatchMode.Normal, (a, b, c, d) -> { },
						(a, b) -> {
							started.countDown();
							assertTrue(resume.await(5, TimeUnit.SECONDS));
							observed.set(a.request());
						}, a -> fail("no end callback on fallback thread"));
				x.fireBeginStream(request);
				x.fireStreamContentHandle(content);
				ready.set(x);
			}).sync();
			assertTrue(started.await(5, TimeUnit.SECONDS));
			group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
			ready.get().closeConnectionNow();
			assertSame(request, ready.get().request());
			resume.countDown();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (ready.get().request() != null && System.nanoTime() < deadline)
				Thread.sleep(1);
			assertSame(request, observed.get());
			assertEquals(0, request.refCnt());
			assertEquals(1, content.refCnt());
		} finally {
			resume.countDown();
			content.release();
			group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
			server.close();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void rejectedCloseReleasesQueuedWritesAndFailsTheirPromises(boolean force, @TempDir Path directory) throws Exception {
		Task.tryInitThreadPool();
		var server = new HttpServer();
		var group = new DefaultEventLoopGroup(1);
		var channel = new LocalChannel();
		channel.pipeline().addLast(new ChannelInboundHandlerAdapter());
		var exchange = new AtomicReference<HttpExchange>();
		var buffer = Unpooled.buffer().writeByte(1);
		var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
		var callbackCalls = new AtomicInteger();
		var file = directory.resolve("pending.bin");
		Files.write(file, new byte[]{1});
		try (var input = FileChannel.open(file, StandardOpenOption.READ)) {
			var chunks = new HttpChunkedInput(new ChunkedNioFile(input, 1));
			group.register(channel).sync();
			var promise = channel.newPromise();
			var filePromise = channel.newPromise();
			channel.eventLoop().submit(() -> {
				var ctx = channel.pipeline().lastContext();
				var first = new HttpExchange(server, ctx);
				assertTrue(first.registerResponseOrder());
				var second = new HttpExchange(server, ctx);
				second.request = request;
				second.inStreamMode = true;
				second.handler = new HttpHandler(1, TransactionLevel.None, DispatchMode.Direct,
						ignored -> callbackCalls.incrementAndGet());
				assertTrue(second.registerResponseOrder());
				second.writeResponse(buffer, true, promise);
				second.writeResponse(chunks, true, filePromise);
				exchange.set(second);
			}).sync();
			assertFalse(promise.isDone());
			group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
			try (var lateInput = FileChannel.open(file, StandardOpenOption.READ)) {
				var rejected = exchange.get().writeResponse(new HttpChunkedInput(new ChunkedNioFile(lateInput, 1)), true, null);
				assertFalse(rejected.isSuccess());
				assertFalse(lateInput.isOpen(), "投递被拒的未提交文件块也由框架收尾");
			}
			assertDoesNotThrow(() -> {
				if (force)
					exchange.get().closeConnectionNow();
				else
					exchange.get().close(null);
			});
			assertTrue(promise.await(5, TimeUnit.SECONDS));
			assertTrue(filePromise.await(5, TimeUnit.SECONDS));
			assertFalse(promise.isSuccess());
			assertFalse(input.isOpen());
			assertEquals(0, callbackCalls.get(), "停机资源清理不得在任意线程调用用户回调");
			assertEquals(0, buffer.refCnt());
			assertEquals(0, request.refCnt());
			assertDoesNotThrow(() -> exchange.get().closeConnectionNow());
			assertEquals(0, buffer.refCnt());
		} finally {
			group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
			server.close();
		}
	}
}
