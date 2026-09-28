package Zeze.Raft;

/**
 * 可重试的 Raft 异常：捕获方应按重试语义处理（重新发起或等待重发）。
 */
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
