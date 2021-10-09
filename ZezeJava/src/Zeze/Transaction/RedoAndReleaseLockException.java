package Zeze.Transaction;

import Zeze.*;

public final class RedoAndReleaseLockException extends RuntimeException {
	public RedoAndReleaseLockException() {
	}

	public RedoAndReleaseLockException(String msg) {
		super(msg);

	}
}