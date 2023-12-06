package Zeze.Onz;

import Zeze.Transaction.Bean;

/**
 * 开发onz服务器基础
 *
 * 包装网络和onz协议，
 * 允许多个server实例，
 * 不同的server实例功能可以交叉也可以完全不同，
 */
public class OnzServer {
	private final OnzAgent onzAgent = new OnzAgent();

	public OnzServer() {

	}

	public OnzAgent getOnzAgent() {
		return onzAgent;
	}

	public void perform(String name, OnzFuncTransaction func) {
		perform(name, func, Onz.eFlushImmediately, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode) {
		perform(name, func, flushMode, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode, Bean argument, Bean result) {
		var t = new OnzTransaction(this, name, func, flushMode, argument, result);
		try {
			t.perform();
			t.commit();
		} catch (Throwable ex) {
			t.rollback();
		}
	}
}
