package Zeze.Arch;

import java.util.function.Function;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.IntHashMap;

public class ModuleRedirectAllContext<R extends RedirectResult> extends Zeze.Net.Service.ManualContext {
	private final int concurrentLevel;
	private final IntHashMap<R> hashResults = new IntHashMap<>(); // <hash, result>
	private final Function<Binary, R> decodeResult;
	private final Action1<R> onHashResult;
	private final RedirectAllDoneHandle<R> onHashEnd;

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> decodeResult) {
		this(concurrentLevel, decodeResult, null, null);
	}

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> decodeResult, Action1<R> onHashResult) {
		this(concurrentLevel, decodeResult, onHashResult, null);
	}

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> decodeResult, Action1<R> onHashResult,
									RedirectAllDoneHandle<R> onHashEnd) {
		this.concurrentLevel = concurrentLevel;
		this.decodeResult = decodeResult;
		this.onHashResult = onHashResult;
		this.onHashEnd = onHashEnd;
	}

	// 只用于AllDone时获取所有结果, 此时不会再修改hashResults所以没有并发问题
	public final IntHashMap<R> getHashResults() {
		return hashResults;
	}

	@Override
	public void OnRemoved() throws Throwable {
		if (onHashEnd != null)
			onHashEnd.handle(this);
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	@SuppressWarnings("deprecation")
	public final void ProcessResult(Zeze.Application zeze, ModuleRedirectAllResult result) throws Throwable {
		for (var h : result.Argument.getHashs().entrySet()) {
			var resultCode = h.getValue().getReturnCode();
			R resultBean = decodeResult.apply(resultCode == Procedure.Success ? h.getValue().getParams() : null);
			resultBean.setSessionId(getSessionId());
			resultBean.setHash(h.getKey());
			resultBean.setResultCode(resultCode);
			if (onHashResult != null) {
				zeze.NewProcedure(() -> {
					onHashResult.run(resultBean);
					return 0L;
				}, "ModuleRedirectResponse Procedure").Call();
			}
			boolean allDone;
			synchronized (this) {
				hashResults.put(h.getKey(), resultBean);
				allDone = hashResults.size() == concurrentLevel;
			}
			if (allDone)
				getService().TryRemoveManualContext(getSessionId());
		}
	}
}
