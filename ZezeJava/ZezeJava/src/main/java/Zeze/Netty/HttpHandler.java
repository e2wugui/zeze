package Zeze.Netty;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;

public class HttpHandler {
	public final long MaxContentLength; // HTTP body的长度限制
	public final TransactionLevel Level; // 事务级别
	public final DispatchMode Mode; // 线程派发模式
	public final HttpBeginStreamHandle BeginStreamHandle; // 上行流处理函数。
	public final HttpStreamContentHandle StreamContentHandle;
	public final HttpEndStreamHandle EndStreamHandle; // 也用于普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。

	public HttpHandler() {
		this(8192, TransactionLevel.Serializable, DispatchMode.Normal, null);
	}

	public HttpHandler(long maxContentLength, TransactionLevel level, DispatchMode mode,
					   HttpEndStreamHandle fullHandle) {
		MaxContentLength = Math.min(maxContentLength & Long.MAX_VALUE, Integer.MAX_VALUE); // 非流模式最大支持2G-1字节
		Level = level;
		Mode = mode;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = fullHandle;
	}

	public HttpHandler(long maxContentLength, TransactionLevel level, DispatchMode mode,
					   HttpBeginStreamHandle beginStream, HttpStreamContentHandle streamContent,
					   HttpEndStreamHandle endStream) {
		MaxContentLength = maxContentLength >= 0 ? maxContentLength : Long.MAX_VALUE;
		Level = level;
		Mode = mode;
		BeginStreamHandle = beginStream;
		StreamContentHandle = streamContent;
		EndStreamHandle = endStream;
	}

	public final boolean isStreamMode() {
		return BeginStreamHandle != null;
	}
}
