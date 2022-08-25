package Zeze.Netty;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;

public class HttpHandler {
	public final int MaxContentLength; // HTTP body的长度限制, 只用于非流模式
	public final TransactionLevel Level; // 事务级别
	public final DispatchMode Mode; // 线程派发模式
	public final HttpBeginStreamHandle BeginStreamHandle; // 上行流处理函数。
	public final HttpStreamContentHandle StreamContentHandle;
	public final HttpEndStreamHandle EndStreamHandle; // 也用于普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。

	public HttpHandler(int maxContentLength, TransactionLevel level, DispatchMode mode,
					   HttpEndStreamHandle fullHandle) {
		if (fullHandle == null)
			throw new IllegalArgumentException("beginStream is null");
		MaxContentLength = maxContentLength >= 0 ? maxContentLength : Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = fullHandle;
	}

	public HttpHandler(TransactionLevel level, DispatchMode mode, HttpBeginStreamHandle beginStream,
					   HttpStreamContentHandle streamContent, HttpEndStreamHandle endStream) {
		if (beginStream == null)
			throw new IllegalArgumentException("beginStream is null");
		if (endStream == null)
			throw new IllegalArgumentException("endStream is null");
		MaxContentLength = Integer.MAX_VALUE;
		Level = level != null ? level : TransactionLevel.Serializable;
		Mode = mode != null ? mode : DispatchMode.Normal;
		BeginStreamHandle = beginStream;
		StreamContentHandle = streamContent;
		EndStreamHandle = endStream;
	}

	public final boolean isStreamMode() {
		return BeginStreamHandle != null;
	}
}
