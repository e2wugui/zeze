package Zeze.Transaction;

import Zeze.*;

public final class AbortException extends RuntimeException {
	public AbortException() {
	}

	public AbortException(String msg) {
		super(msg);

	}
}