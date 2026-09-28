package Zeze.Arch;

/**
 * RedirectAll 的单个 hash 分组结果：hash 与返回码。（目前只用于RedirectAll）
 */
public class RedirectResult {
	private int hash;
	private long resultCode;

	public int getHash() {
		return hash;
	}

	void setHash(int hash) {
		this.hash = hash;
	}

	public long getResultCode() {
		return resultCode;
	}

	void setResultCode(long resultCode) {
		this.resultCode = resultCode;
	}
}
