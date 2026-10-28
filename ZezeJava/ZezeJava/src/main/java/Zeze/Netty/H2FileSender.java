package Zeze.Netty;

import java.io.EOFException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.concurrent.RejectedExecutionException;
import Zeze.Util.TaskSpec;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

/** One file, one outstanding read/write. Stream advancement runs on the EventLoop; reads use the Task pool. */
final class H2FileSender extends ChannelInboundHandlerAdapter {
	private final HttpExchange exchange;
	private final FileChannel file;
	private final int chunkSize;
	private final ChannelPromise completion;
	private long position;
	private long remaining;
	private boolean writing;

	private H2FileSender(HttpExchange exchange, FileChannel file, long position, long remaining, int chunkSize) {
		this.exchange = exchange;
		this.file = file;
		this.position = position;
		this.remaining = remaining;
		this.chunkSize = chunkSize;
		completion = exchange.context.newPromise();
	}

	static ChannelFuture send(HttpExchange exchange, FileChannel file, long position, long length, int chunkSize) {
		var sender = new H2FileSender(exchange, file, position, length, chunkSize);
		var loop = exchange.context.channel().eventLoop();
		if (loop.inEventLoop())
			sender.start();
		else {
			try {
				loop.execute(sender::start);
			} catch (RejectedExecutionException e) {
				sender.fail(e);
			}
		}
		return sender.completion;
	}

	private void start() {
		if (!exchange.context.channel().isActive()) {
			fail(new ClosedChannelException());
			return;
		}
		exchange.context.pipeline().addLast(this);
		advance();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		completion.addListener(__ -> {
			try {
				file.close();
			} catch (Exception e) {
				Netty.logger.warn("close HTTP/2 file failed", e);
			}
			if (ctx.pipeline().context(this) != null)
				ctx.pipeline().remove(this);
		});
	}

	private void advance() {
		if (writing || completion.isDone())
			return;
		try {
			if (remaining == 0) {
				writing = true;
				exchange.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, completion);
				return;
			}
			if (!exchange.context.channel().isWritable())
				return;
			writing = true;
			long readPosition = position;
			int readSize = (int)Math.min(chunkSize, remaining);
			TaskSpec.ofAction(() -> readChunk(readPosition, readSize)).name("H2FileSender.readChunk").runNow();
		} catch (Exception e) {
			fail(e);
		}
	}

	private void readChunk(long readPosition, int readSize) {
		if (completion.isDone())
			return;
		try {
			var buffer = java.nio.ByteBuffer.allocate(readSize);
			int count = file.read(buffer, readPosition);
			if (count <= 0)
				throw new EOFException("file truncated during HTTP/2 response");
			exchange.context.executor().execute(() -> writeChunk(buffer.array(), count));
		} catch (Exception e) {
			fail(e);
		}
	}

	private void writeChunk(byte[] bytes, int count) {
		if (completion.isDone())
			return;
		position += count;
		remaining -= count;
		try {
			exchange.writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(bytes, 0, count)), true, null)
					.addListener(future -> {
						writing = false;
						if (!future.isSuccess()) {
							fail(future.cause());
							return;
						}
						try {
							// A write can complete inline. Queue advancement to avoid recursive
							// reads and to give writability / cancellation events a turn.
							exchange.context.executor().execute(this::advance);
						} catch (RejectedExecutionException e) {
							fail(e);
						}
					});
		} catch (Exception e) {
			fail(e);
		}
	}

	private void fail(Throwable cause) {
		try {
			file.close();
		} catch (Exception closeError) {
			cause.addSuppressed(closeError);
		}
		if (completion.tryFailure(cause))
			exchange.context.close();
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		advance();
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		fail(new ClosedChannelException());
		super.channelInactive(ctx);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		if (!completion.isDone())
			fail(new ClosedChannelException());
	}
}
