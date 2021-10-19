package Zeze.Net;

import java.io.Serial;

public final class RpcTimeoutException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -7782233468533311166L;

	public RpcTimeoutException() {

	}

	public RpcTimeoutException(String str) {
		super(str);

	}
}