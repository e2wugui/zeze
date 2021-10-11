package Zeze.Transaction;

public final class RedoAndReleaseLockException extends Error {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7674037466857621440L;

	public RedoAndReleaseLockException() {
	}

	public RedoAndReleaseLockException(String msg) {
		super(msg);

	}
}