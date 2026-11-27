package Zeze.Net;

import java.io.Serial;

/**
 * 连接释放（{@link Service#OnSocketDisposed}）时在飞 Rpc 立即失败的异常（单例无栈形态）。
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
