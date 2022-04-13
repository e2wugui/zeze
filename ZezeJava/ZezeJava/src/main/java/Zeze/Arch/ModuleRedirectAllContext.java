package Zeze.Arch;

import java.util.function.ToLongFunction;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.Factory;
import Zeze.Util.IntHashMap;

public class ModuleRedirectAllContext extends Zeze.Net.Service.ManualContext {
	private final String MethodFullName;
	private final IntHashMap<Long> HashResults = new IntHashMap<>(); // <hash, resultCode>
	private int leftResultCount;
	private RedirectAllDoneHandle OnHashEnd;

	public ModuleRedirectAllContext(int concurrentLevel, String methodFullName) {
		MethodFullName = methodFullName;
		for (int hash = 0; hash < concurrentLevel; ++hash) {
			HashResults.put(hash, null);
		}
		leftResultCount = concurrentLevel;
	}

	public final String getMethodFullName() {
		return MethodFullName;
	}

	public final IntHashMap<Long> getHashResults() {
		return HashResults;
	}

	public final RedirectAllDoneHandle getOnHashEnd() {
		return OnHashEnd;
	}

	public final void setOnHashEnd(RedirectAllDoneHandle value) {
		OnHashEnd = value;
	}

	@Override
	public synchronized void OnRemoved() throws Throwable {
		var onHashEnd = OnHashEnd;
		if (onHashEnd != null) {
			OnHashEnd = null;
			HashResults.foreachUpdate((k, v) -> v != null ? v : Procedure.Timeout); // 没结果的当成超时
			onHashEnd.handle(this);
		}
	}

	/**
	 * 调用这个方法处理hash分组结果，真正的处理代码在action中实现。
	 * 1) 在锁内执行；
	 * 2) 需要时初始化UserState并传给action；
	 * 3) 处理完成时删除Context
	 */
	@SuppressWarnings("unchecked")
	public final synchronized <T> long ProcessHash(int hash, Factory<T> factory, ToLongFunction<T> action) {
		try {
			if (getUserState() == null) {
				setUserState(factory.create());
			}
			return action.applyAsLong((T)getUserState());
		} finally {
			HashResults.remove(hash); // 如果不允许一个hash分组处理措辞，把这个移到开头并判断结果。
			if (HashResults.isEmpty()) {
				getService().TryRemoveManualContext(getSessionId());
			}
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	public final void ProcessResult(Zeze.Application zeze, ModuleRedirectAllResult result) throws Throwable {
		for (var h : result.Argument.getHashs().entrySet()) {
			long resultCode = h.getValue().getReturnCode();
			final RedirectAllDoneHandle onHashEnd;
			synchronized (this) {
				if (HashResults.put(h.getKey(), resultCode) == null)
					onHashEnd = --leftResultCount == 0 ? OnHashEnd : null;
				else
					onHashEnd = null;
			}
			if (resultCode == Procedure.Success) {
				// 不判断单个分组的处理结果，错误也继续执行其他分组。XXX
				getService().getZeze().NewProcedure(() -> ProcessHashResult(
						zeze, h.getKey(), h.getValue().getParams()), MethodFullName).Call();
			}
		}
	}

	// 生成代码实现。see Zezex.ModuleRedirect.cs
	public long ProcessHashResult(Zeze.Application zeze, int _hash_, Binary _params) throws Throwable {
		return Procedure.NotImplement;
	}
}
