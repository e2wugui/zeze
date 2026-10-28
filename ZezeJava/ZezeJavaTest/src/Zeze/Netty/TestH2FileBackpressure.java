package Zeze.Netty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletableFuture;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestH2FileBackpressure {
	@TempDir
	Path directory;

	static final class RecordingFile extends FileChannel {
		final CompletableFuture<Thread> readingThread = new CompletableFuture<>();
		@Override public int read(ByteBuffer dst, long position) {
			readingThread.complete(Thread.currentThread());
			dst.put((byte)1);
			return 1;
		}
		@Override protected void implCloseChannel() { }
		@Override public int read(ByteBuffer dst) { throw new UnsupportedOperationException(); }
		@Override public long read(ByteBuffer[] dsts, int offset, int length) { throw new UnsupportedOperationException(); }
		@Override public int write(ByteBuffer src) { throw new UnsupportedOperationException(); }
		@Override public long write(ByteBuffer[] srcs, int offset, int length) { throw new UnsupportedOperationException(); }
		@Override public long position() { throw new UnsupportedOperationException(); }
		@Override public FileChannel position(long value) { throw new UnsupportedOperationException(); }
		@Override public long size() { return 1; }
		@Override public FileChannel truncate(long size) { throw new UnsupportedOperationException(); }
		@Override public void force(boolean metadata) { throw new UnsupportedOperationException(); }
		@Override public long transferTo(long position, long count, WritableByteChannel target) { throw new UnsupportedOperationException(); }
		@Override public long transferFrom(ReadableByteChannel src, long position, long count) { throw new UnsupportedOperationException(); }
		@Override public int write(ByteBuffer src, long position) { throw new UnsupportedOperationException(); }
		@Override public MappedByteBuffer map(MapMode mode, long position, long size) { throw new UnsupportedOperationException(); }
		@Override public FileLock lock(long position, long size, boolean shared) { throw new UnsupportedOperationException(); }
		@Override public FileLock tryLock(long position, long size, boolean shared) { throw new UnsupportedOperationException(); }
	}

	static final class SlowTransport extends ChannelOutboundHandlerAdapter {
		final ArrayDeque<HttpContent> contents = new ArrayDeque<>();
		final ArrayDeque<ChannelPromise> promises = new ArrayDeque<>();
		long bytes;
		boolean ended;
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
			if (msg instanceof HttpContent content) {
				contents.add(content);
				promises.add(promise);
			} else {
				ReferenceCountUtil.release(msg);
				promise.trySuccess();
			}
		}

		void acceptOne() {
			var content = contents.remove();
			bytes += content.content().readableBytes();
			ended |= content instanceof LastHttpContent;
			ReferenceCountUtil.release(content);
			promises.remove().trySuccess();
		}
	}

	private static void awaitContent(EmbeddedChannel channel, SlowTransport transport) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (transport.contents.isEmpty()) {
			channel.runPendingTasks();
			assertTrue(System.nanoTime() < deadline, "waiting for one file chunk");
			if (transport.contents.isEmpty())
				Thread.sleep(1);
		}
	}

	@Test
	void readsOneChunkOnlyWhenWritableAndPreviousWriteCompletes() throws Exception {
		Task.tryInitThreadPool();
		var data = new byte[64 * 1024 * 8 + 17];
		var file = directory.resolve("data.bin");
		Files.write(file, data);
		var server = new HttpServer();
		var transport = new SlowTransport();
		var channel = new EmbeddedChannel(transport, new ChannelInboundHandlerAdapter());
		channel.attr(HttpExchange.h2StreamKey).set(Boolean.TRUE);
		var x = new HttpExchange(server, channel.pipeline().lastContext());
		x.request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/data.bin");
		try {
			channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
			HttpFileService.sendFile(x, file.toFile(), 0);
			channel.runPendingTasks();
			assertEquals(0, transport.contents.size(), "不可写时不得读取文件并排队内容");
			channel.unsafe().outboundBuffer().setUserDefinedWritability(1, true);
			awaitContent(channel, transport);
			assertEquals(1, transport.contents.size());
			while (!transport.ended) {
				assertEquals(1, transport.contents.size(), "仅允许一个未完成的文件块写");
				transport.acceptOne();
				if (!transport.ended)
					awaitContent(channel, transport);
			}
			assertEquals(data.length, transport.bytes);
		} finally {
			while (!transport.contents.isEmpty())
				transport.acceptOne();
			channel.finishAndReleaseAll();
			server.close();
		}
	}

	@Test
	void completedLastDataWriteCanEndAnUnwritableStream() throws Exception {
		Task.tryInitThreadPool();
		var path = directory.resolve("last.bin");
		Files.write(path, new byte[]{1, 2, 3});
		var server = new HttpServer();
		var transport = new SlowTransport();
		var channel = new EmbeddedChannel(transport, new ChannelInboundHandlerAdapter());
		try (var file = FileChannel.open(path, StandardOpenOption.READ)) {
			var x = new HttpExchange(server, channel.pipeline().lastContext());
			var completion = H2FileSender.send(x, file, 0, file.size(), 64 * 1024);
			awaitContent(channel, transport);
			assertEquals(1, transport.contents.size());
			channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
			transport.acceptOne();
			channel.runPendingTasks();
			assertEquals(1, transport.contents.size(), "零字节结束帧无需等待数据流控窗口");
			assertInstanceOf(LastHttpContent.class, transport.contents.peek());
			transport.acceptOne();
			assertTrue(completion.isSuccess());
		} finally {
			while (!transport.contents.isEmpty())
				transport.acceptOne();
			channel.finishAndReleaseAll();
			server.close();
		}
	}

	@Test
	void fileReadsLeaveTheStreamAdvancingThread() throws Exception {
		Task.tryInitThreadPool();
		var server = new HttpServer();
		var channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
		try (var file = new RecordingFile()) {
			// EmbeddedEventLoop executes the initial advancement inline on this thread.
			var advancingThread = Thread.currentThread();
			var x = new HttpExchange(server, channel.pipeline().lastContext());
			var completion = H2FileSender.send(x, file, 0, 1, 64 * 1024);
			assertNotSame(advancingThread, file.readingThread.get(5, TimeUnit.SECONDS));
			completion.cancel(false);
			channel.runPendingTasks();
		} finally {
			channel.finishAndReleaseAll();
			server.close();
		}
	}

	@Test
	void closedOrCancelledUnwritableStreamClosesItsFile() throws Exception {
		Task.tryInitThreadPool();
		var path = directory.resolve("cancel.bin");
		Files.write(path, new byte[128 * 1024]);
		var server = new HttpServer();
		try {
			for (boolean cancel : new boolean[]{false, true}) {
				var channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
				try (var file = FileChannel.open(path, StandardOpenOption.READ)) {
					channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
					var x = new HttpExchange(server, channel.pipeline().lastContext());
					var completion = H2FileSender.send(x, file, 0, file.size(), 64 * 1024);
					assertTrue(file.isOpen());
					if (cancel)
						completion.cancel(false);
					else
						channel.close();
					channel.runPendingTasks();
					assertTrue(completion.isDone());
					assertFalse(completion.isSuccess());
					assertFalse(file.isOpen());
				} finally {
					channel.finishAndReleaseAll();
				}
			}
		} finally {
			server.close();
		}
	}
}
