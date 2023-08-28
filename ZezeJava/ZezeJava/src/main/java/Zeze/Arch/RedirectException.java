package Zeze.Arch;

public class RedirectException extends RuntimeException {
	public static final int GENERIC = 0; // 未知的通用情况,直接用setException方式传递非RedirectException的异常时
	public static final int SERVER_NOT_FOUND = 1; // 远程进程没连接上或者没提供所需的服务
	public static final int SERVER_TIMEOUT = 2; // 远程进程执行目标方法超时,不确定是否已执行
	public static final int LOCAL_EXECUTION = 3; // 本进程执行目标方法时抛出的异常,可通过getCause()获得原异常
	public static final int REMOTE_EXECUTION = 4; // 远程进程执行目标方法时抛出的异常,暂时不支持

	public static final RedirectException timeoutInstance = new RedirectException(SERVER_TIMEOUT, "timeout");

	private final int code;

	public RedirectException(int code) {
		this.code = code;
	}

	public RedirectException(int code, String message) {
		super(message);
		this.code = code;
	}

	public RedirectException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public RedirectException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
