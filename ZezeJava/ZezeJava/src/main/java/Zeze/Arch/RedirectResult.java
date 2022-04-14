package Zeze.Arch;

import Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest;

public class RedirectResult {
	private long sessionId;
	private int hash;
	private long resultCode;
	private Object asyncContext;
	private byte asyncState;

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

	@Deprecated // 只让生成代码修改,别手动修改
	public void setAsyncContext(Object asyncContext) {
		this.asyncContext = asyncContext;
	}

	public boolean isAsync() {
		return asyncState != 0;
	}

	public void async() {
		if (asyncState != 0)
			throw new IllegalStateException("asyncState=" + asyncState);
		asyncState = 1;
	}

	public void send() throws Throwable {
		if (asyncState != 1)
			throw new IllegalStateException("asyncState=" + asyncState);
		asyncState = 2;
		var p = (ModuleRedirectAllRequest)asyncContext;
		((ProviderDirect)p.getUserState()).SendResultForAsync(p, this);
	}
}
