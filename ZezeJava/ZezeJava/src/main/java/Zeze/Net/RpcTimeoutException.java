package Zeze.Net;

public final class RpcTimeoutException extends RuntimeException {
	private static final long serialVersionUID = -7782233468533311166L;

	private static final RpcTimeoutException instance = new RpcTimeoutException();

	public static RpcTimeoutException getInstance() {
		return instance;
	}

	private RpcTimeoutException() {
		super(null, null, false, false);
	}

	public RpcTimeoutException(String message) {
		super(message);
	}
}
