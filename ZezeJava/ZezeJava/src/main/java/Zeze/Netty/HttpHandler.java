package Zeze.Netty;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import org.jspecify.annotations.Nullable;

/**
 * HTTP 路径处理器配置：事务级别/派发模式与请求、流式、WebSocket 回调的组合。
 */
public class HttpHandler {
	public final int MaxContentLength; // HTTP请求的body长度限制(只限GET请求时可以限制0长度)以及WebSocket的上传帧长度限制, 只用于非流模式
	public final TransactionLevel Level; // 事务级别
	public final DispatchMode Mode; // 线程派发模式
	public final @Nullable HttpBeginStreamHandle BeginStreamHandle; // 上行流处理函数。
	public final @Nullable HttpStreamContentHandle StreamContentHandle;
	public final @Nullable HttpEndStreamHandle EndStreamHandle; // 也用于普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。
	public final @Nullable HttpAbortStreamHandle AbortStreamHandle; // 上行流中止回调；为null时流中止回退调用EndStreamHandle（isStreamAborted()==true）
	public final @Nullable HttpWebSocketHandle WebSocketHandle;

	public HttpHandler(int maxContentLength, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   HttpEndStreamHandle fullHandle) {
		//noinspection ConstantValue
		if (fullHandle == null)
			throw new IllegalArgumentException("fullHandle is null");
		MaxContentLength = maxContentLength >= 0 ? maxContentLength : Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = fullHandle;
		AbortStreamHandle = null;
		WebSocketHandle = null;
	}

	/** 构造流式上传处理器（无独立中止回调时，中止回退调用 endStream 且 isStreamAborted 为 true）。 */
	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
					   HttpEndStreamHandle endStream) {
		this(level, mode, beginStream, streamContent, endStream, null);
	}

	/** 构造流式上传处理器：abortStream 非空时，正常完成调 endStream、提前中止调 abortStream，二者互斥。 */
	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
					   HttpEndStreamHandle endStream, @Nullable HttpAbortStreamHandle abortStream) {
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
		AbortStreamHandle = abortStream;
		WebSocketHandle = null;
	}

	/** 构造 multipart/文件上传流式处理器，自动接入中止清理回调。 */
	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   HttpMultipartHandle multipartHandle) {
		this(level, mode, checkMultipartHandle(multipartHandle), multipartHandle, multipartHandle, multipartHandle);
	}

	private static HttpMultipartHandle checkMultipartHandle(HttpMultipartHandle multipartHandle) {
		//noinspection ConstantValue
		if (multipartHandle == null)
			throw new IllegalArgumentException("multipartHandle is null");
		return multipartHandle;
	}

	public HttpHandler(int maxFrameLength, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
					   HttpWebSocketHandle webSocketHandle) {
		//noinspection ConstantValue
		if (webSocketHandle == null)
			throw new IllegalArgumentException("webSocketHandle is null");
		MaxContentLength = maxFrameLength >= 0 ? maxFrameLength : Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = null;
		AbortStreamHandle = null;
		WebSocketHandle = webSocketHandle;
	}

	public HttpHandler(@Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   HttpWebSocketHandle webSocketHandle) {
		this(64 * 1024, level, mode, webSocketHandle);
	}

	public final boolean isStreamMode() {
		return BeginStreamHandle != null;
	}

	public final boolean isWebSocketMode() {
		return WebSocketHandle != null;
	}
}
