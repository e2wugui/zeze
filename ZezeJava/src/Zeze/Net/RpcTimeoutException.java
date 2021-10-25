package Zeze.Net;

public final class RpcTimeoutException extends RuntimeException {

	private static final long serialVersionUID = -7782233468533311166L;

	public RpcTimeoutException() {

	}

	public RpcTimeoutException(String str) {
		super(str);

	}
}