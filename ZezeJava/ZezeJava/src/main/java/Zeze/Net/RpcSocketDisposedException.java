package Zeze.Net;

import java.io.Serial;

/**
 * 连接释放（{@link Service#OnSocketDisposed}）时在飞 Rpc 立即失败的异常（复审R3，FND7-S1③）。
 * 单例无栈形态对齐 {@link RpcTimeoutException}：区分"等满超时"与"连接已释放立即失败"，
 * 便于调用方诊断与分类处理。
 */
public final class RpcSocketDisposedException extends RuntimeException {
	@Serial private static final long serialVersionUID = 5539754481106978336L;

	private static final RpcSocketDisposedException instance = new RpcSocketDisposedException();

	public static RpcSocketDisposedException getInstance() {
		return instance;
	}

	private RpcSocketDisposedException() {
		super(null, null, false, false);
	}

	public RpcSocketDisposedException(String message) {
		super(message);
	}
}
