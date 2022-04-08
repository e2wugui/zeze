package Zeze.Arch;

import java.util.function.ToLongFunction;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.IntHashSet;

public class ModuleRedirectAllContext extends Zeze.Net.Service.ManualContext {
	private final String MethodFullName;
	private final IntHashSet HashCodes = new IntHashSet();
	private RedirectAllDoneHandle OnHashEnd;

	public ModuleRedirectAllContext(int concurrentLevel, String methodFullName) {
		for (int hash = 0; hash < concurrentLevel; ++hash) {
			getHashCodes().add(hash);
		}
		MethodFullName = methodFullName;
	}

	public final String getMethodFullName() {
		return MethodFullName;
	}

	public final IntHashSet getHashCodes() {
		return HashCodes;
	}

	public final RedirectAllDoneHandle getOnHashEnd() {
		return OnHashEnd;
	}

	public final void setOnHashEnd(RedirectAllDoneHandle value) {
		OnHashEnd = value;
	}

	@Override
	public void OnRemoved() throws Throwable {
		synchronized (this) {
			if (OnHashEnd != null) {
				OnHashEnd.handle(this);
				OnHashEnd = null;
			}
		}
	}

	/**
	 * 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
	 * 1) 在锁内执行；
	 * 2) 需要时初始化UserState并传给action；
	 * 3) 处理完成时删除Context
	 */
	@SuppressWarnings("unchecked")
	public final <T> long ProcessHash(int hash, Zeze.Util.Factory<T> factory, ToLongFunction<T> action) {
		synchronized (this) {
			try {
				if (getUserState() == null) {
					setUserState(factory.create());
				}
				return action.applyAsLong((T)getUserState());
			} finally {
				HashCodes.remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
				if (HashCodes.isEmpty()) {
					getService().TryRemoveManualContext(getSessionId());
				}
			}
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	public final void ProcessResult(Zeze.Application zeze, ModuleRedirectAllResult result) throws Throwable {
		for (var h : result.Argument.getHashs().entrySet()) {
			// 嵌套存储过程，单个分组的结果处理不影响其他分组。
			// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
			getService().getZeze().NewProcedure(() -> ProcessHashResult(
							zeze, h.getKey(), h.getValue().getParams()),
					getMethodFullName()).Call();
		}
	}

	// 生成代码实现。see Zezex.ModuleRedirect.cs
	public long ProcessHashResult(Zeze.Application zeze, int _hash_, Binary _params) throws Throwable {
		return Procedure.NotImplement;
	}
}
