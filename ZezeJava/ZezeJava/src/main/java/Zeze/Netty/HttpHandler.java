package Zeze.Netty;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpHandler {
	public final int MaxContentLength; // HTTP请求的body长度限制(只限GET请求时可以限制0长度), 只用于非流非WebSocket模式
	public final @NotNull TransactionLevel Level; // 事务级别
	public final @NotNull DispatchMode Mode; // 线程派发模式
	public final @Nullable HttpBeginStreamHandle BeginStreamHandle; // 上行流处理函数。
	public final @Nullable HttpStreamContentHandle StreamContentHandle;
	public final @Nullable HttpEndStreamHandle EndStreamHandle; // 也用于普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。
	public final @Nullable HttpWebSocketHandle WebSocketHandle;

	public HttpHandler(int maxContentLength, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   @NotNull HttpEndStreamHandle fullHandle) {
		//noinspection ConstantValue
		if (fullHandle == null)
			throw new IllegalArgumentException("fullHandle is null");
		MaxContentLength = maxContentLength >= 0 ? maxContentLength : Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = fullHandle;
		WebSocketHandle = null;
	}

	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   @NotNull HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
					   @NotNull HttpEndStreamHandle endStream) {
		//noinspection ConstantValue
		if (beginStream == null)
			throw new IllegalArgumentException("beginStream is null");
		//noinspection ConstantValue
		if (endStream == null)
			throw new IllegalArgumentException("endStream is null");
		MaxContentLength = Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = beginStream;
		StreamContentHandle = streamContent;
		EndStreamHandle = endStream;
		WebSocketHandle = null;
	}

	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   @NotNull HttpWebSocketHandle webSocketHandle) {
		//noinspection ConstantValue
		if (webSocketHandle == null)
			throw new IllegalArgumentException("webSocketHandle is null");
		MaxContentLength = Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = null;
		WebSocketHandle = webSocketHandle;
	}

	public final boolean isStreamMode() {
		return BeginStreamHandle != null;
	}

	public final boolean isWebSocketMode() {
		return WebSocketHandle != null;
	}
}
