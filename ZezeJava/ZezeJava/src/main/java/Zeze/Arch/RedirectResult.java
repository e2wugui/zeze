package Zeze.Arch;

// 目前只用于RedirectAll
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
