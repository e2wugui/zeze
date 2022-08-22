package Zeze.Netty;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;

public class HttpHandler {
	public final long MaxContentLength; // HTTP body的长度限制
	public final TransactionLevel Level; // 事务级别
	public final DispatchMode Mode; // 线程派发模式
	// 普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。
	public final HttpFullRequestHandle FullRequestHandle;
	// 上行流处理函数。
	public final HttpBeginStreamHandle BeginStreamHandle;
	public final HttpStreamContentHandle StreamContentHandle;
	public final HttpEndStreamHandle EndStreamHandle;

	public HttpHandler() {
		this(8192, TransactionLevel.Serializable, DispatchMode.Normal, null);
	}

	public HttpHandler(long maxContentLength, TransactionLevel level, DispatchMode mode,
					   HttpFullRequestHandle fullHandle) {
		MaxContentLength = Math.min(maxContentLength, Integer.MAX_VALUE); // 非流模式最大支持2G-1字节
		Level = level;
		Mode = mode;
		FullRequestHandle = fullHandle;
		BeginStreamHandle = null;
		StreamContentHandle = null;
		EndStreamHandle = null;
	}

	public HttpHandler(long maxContentLength, TransactionLevel level, DispatchMode mode,
					   HttpBeginStreamHandle beginStream, HttpStreamContentHandle streamContent,
					   HttpEndStreamHandle endStream) {
		MaxContentLength = maxContentLength;
		Level = level;
		Mode = mode;
		FullRequestHandle = null;
		BeginStreamHandle = beginStream;
		StreamContentHandle = streamContent;
		EndStreamHandle = endStream;
	}

	public final boolean isStreamMode() {
		return BeginStreamHandle != null;
	}
}
