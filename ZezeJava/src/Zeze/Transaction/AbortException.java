package Zeze.Transaction;

public final class AbortException extends Error {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3068088074472148295L;

	public AbortException() {
	}

	public AbortException(String msg) {
		super(msg);

	}
}