package Zeze.Raft;

public class RaftRetryException extends RuntimeException {
	public RaftRetryException() {
	}

	public RaftRetryException(String msg) {
		super(msg);
	}

	public RaftRetryException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
