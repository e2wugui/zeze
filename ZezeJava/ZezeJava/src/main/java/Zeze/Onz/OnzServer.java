package Zeze.Onz;

import Zeze.Net.AsyncSocket;
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

	public AsyncSocket getZezeInstance(String zezeName) {
		return null; // todo
	}

	public void perform(String name, OnzFuncTransaction func) {
		perform(name, func, Onz.eFlushImmediately, 10_000, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode) {
		perform(name, func, flushMode, 10_000, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode, int flushTimeout) {
		perform(name, func, flushMode, flushTimeout, null, null);
	}

	public void perform(String name, OnzFuncTransaction func, int flushMode, int flushTimeout, Bean argument, Bean result) {
		var t = new OnzTransaction(this, name, func, flushMode, flushTimeout, argument, result);
		try {
			onzAgent.addTransaction(t);
			if (0 == t.perform()) {
				t.waitPendingAsync();
				t.commit();
				t.waitFlushDone();
			} else {
				t.rollback();
			}
		} catch (Throwable ex) {
			t.rollback();
		} finally {
			onzAgent.removeTransaction(t);
		}
	}
}
