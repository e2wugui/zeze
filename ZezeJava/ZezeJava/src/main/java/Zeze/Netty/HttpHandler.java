package Zeze.Netty;

import javax.validation.constraints.Max;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.handler.codec.http.HttpContent;

public abstract class HttpHandler {
	public int MaxContentLength = 8192; // -1 表示不限制，按流处理。
	public TransactionLevel Level = TransactionLevel.Serializable;
	public DispatchMode Mode = DispatchMode.Normal;
	// 普通请求处理函数，如果需要内部会自动把流合并到一个请求里面。
	public HttpFullRequestHandle FullRequestHandle;
	// 上行流处理函数。
	public HttpBeginStreamHandle BeginStreamHandle;
	public HttpStreamContentHandle StreamContentHandle;
	public HttpEndStreamHandle EndStreamHandle;

	public final boolean isStreamMode() {
		return MaxContentLength == -1;
	}
}
