package Zeze.Arch;

// 目前只用于RedirectAll
public class RedirectResult {
	private int hash;
	private long resultCode;

	public int getHash() {
		return hash;
	}

	// 只让生成代码修改,别手动修改
	@Deprecated
	public void setHash(int hash) {
		this.hash = hash;
	}

	public long getResultCode() {
		return resultCode;
	}

	// 只让生成代码修改,别手动修改
	@Deprecated
	void setResultCode(long resultCode) {
		this.resultCode = resultCode;
	}
}
