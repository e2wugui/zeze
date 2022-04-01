package Zeze.Arch;

import java.util.HashSet;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Beans.Provider.*;
import java.util.List;

public class ModuleRedirectAllContext extends Zeze.Net.Service.ManualContext {
	private String MethodFullName;
	public final String getMethodFullName() {
		return MethodFullName;
	}
	private HashSet<Integer> HashCodes = new HashSet<Integer> ();
	public final HashSet<Integer> getHashCodes() {
		return HashCodes;
	}
	private RedirectAllDoneHandle OnHashEnd;
	public final RedirectAllDoneHandle getOnHashEnd() {
		return OnHashEnd;
	}
	public final void setOnHashEnd(RedirectAllDoneHandle value) {
		OnHashEnd = value;
	}

	public ModuleRedirectAllContext(int concurrentLevel, String methodFullName) {
		for (int hash = 0; hash < concurrentLevel; ++hash) {
			getHashCodes().add(hash);
		}
		MethodFullName = methodFullName;
	}

	@Override
	public void OnRemoved() throws Throwable {
		synchronized (this) {
			if (OnHashEnd != null) {
				OnHashEnd.handle(this);
			}
			OnHashEnd = null;
		}
	}

	/**
	 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
	 1) 在锁内执行；
	 2) 需要时初始化UserState并传给action；
	 3) 处理完成时删除Context
	 */
	@SuppressWarnings("unchecked")
	public final <T> long ProcessHash(int hash, Zeze.Util.Factory<T> factory, Zeze.Util.Func1<T, Long> action) throws Throwable {
		synchronized (this) {
			try {
				if (null == getUserState()) {
					setUserState(factory.create());
				}
				return action.call((T)getUserState());
			}
			finally {
				HashCodes.remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
				if (HashCodes.isEmpty()) {
					getService().TryRemoveManualContext(getSessionId());
				}
			}
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	public final void ProcessResult(ModuleRedirectAllResult result) throws Throwable {
		for (var h : result.Argument.getHashs().entrySet()) {
			// 嵌套存储过程，单个分组的结果处理不影响其他分组。
			// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
			getService().getZeze().NewProcedure(() -> ProcessHashResult(
					h.getKey(), h.getValue().getReturnCode(),
					h.getValue().getParams(), h.getValue().getActions()),
					getMethodFullName()).Call();
		}
	}

	// 生成代码实现。see Zezex.ModuleRedirect.cs
	public long ProcessHashResult(int _hash_, long _returnCode_, Binary _params, List<BActionParam> _actions_) throws Throwable {
		return Procedure.NotImplement;
	}
}
