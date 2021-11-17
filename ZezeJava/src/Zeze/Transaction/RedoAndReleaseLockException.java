package Zeze.Transaction;

public final class RedoAndReleaseLockException extends Error {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7674037466857621440L;

	public TableKey TableKey;
	public long GlobalSerialId;

	public RedoAndReleaseLockException() {
	}

	public RedoAndReleaseLockException(TableKey tkey, long serialId, String msg) {
		super(msg);
		TableKey = tkey;
		GlobalSerialId = serialId;
	}
}