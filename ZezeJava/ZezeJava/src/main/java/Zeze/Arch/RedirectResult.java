package Zeze.Arch;

public class RedirectResult {
	private long sessionId;
	private int hash;
	private long resultCode;

	public long getSessionId() {
		return sessionId;
	}

	@Deprecated // 只让生成代码修改,别手动修改
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public int getHash() {
		return hash;
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated // 只让生成代码修改,别手动修改
	public void setHash(int hash) {
		this.hash = hash;
	}

	public long getResultCode() {
		return resultCode;
	}

	void setResultCode(long resultCode) {
		this.resultCode = resultCode;
	}
}
