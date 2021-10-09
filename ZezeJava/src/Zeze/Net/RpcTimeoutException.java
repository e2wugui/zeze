package Zeze.Net;

import Zeze.*;

public final class RpcTimeoutException extends RuntimeException {
	public RpcTimeoutException() {

	}

	public RpcTimeoutException(String str) {
		super(str);

	}
}