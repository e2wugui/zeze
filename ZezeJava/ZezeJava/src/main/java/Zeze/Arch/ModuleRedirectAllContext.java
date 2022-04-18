package Zeze.Arch;

import java.util.function.Function;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.IntHashMap;

public final class ModuleRedirectAllContext<R extends RedirectResult> extends Zeze.Net.Service.ManualContext {
	private final int concurrentLevel;
	private final IntHashMap<R> hashResults = new IntHashMap<>(); // <hash, result>
	private final Function<Binary, R> resultDecoder;
	private final RedirectAllFutureImpl<R> future;
	private boolean timeout;

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> resultDecoder) {
		this.concurrentLevel = concurrentLevel;
		this.resultDecoder = resultDecoder;
		future = resultDecoder != null ? new RedirectAllFutureImpl<>() : null;
	}

	public RedirectAllFutureImpl<R> getFuture() {
		return future;
	}

	public boolean isCompleted() {
		return hashResults.size() >= concurrentLevel || timeout;
	}

	public boolean isTimeout() {
		return timeout;
	}

	// 只用于AllDone时获取所有结果, 此时不会再修改hashResults所以没有并发问题
	public IntHashMap<R> getAllResults() {
		return hashResults;
	}

	@Override
	public synchronized void OnTimeout() throws Throwable {
		if (hashResults.size() < concurrentLevel && !timeout) {
			timeout = true;
			if (future != null) {
				getService().getZeze().NewProcedure(() -> {
					future.allDone(this);
					return Procedure.Success;
				}, "ModuleRedirectAllResponse timeout Procedure").Call();
			}
		}
	}

	@Override
	public void OnRemoved() throws Throwable {
		if (hashResults.size() >= concurrentLevel && future != null) {
			getService().getZeze().NewProcedure(() -> {
				future.allDone(this);
				return Procedure.Success;
			}, "ModuleRedirectAllResponse no-result Procedure").Call();
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	@SuppressWarnings("deprecation")
	public synchronized void ProcessResult(Zeze.Application zeze, ModuleRedirectAllResult res) throws Throwable {
		if (isTimeout())
			return; // 如果恰好刚处理了超时,那就只能忽略后续的结果了
		for (var e : res.Argument.getHashs().entrySet()) {
			int hash = e.getKey();
			var result = e.getValue();
			var resultCode = result.getReturnCode();
			if (resultDecoder != null) {
				R resultBean = resultDecoder.apply(resultCode == Procedure.Success ? result.getParams() : null);
				resultBean.setHash(hash);
				resultBean.setResultCode(resultCode);
				if (hashResults.putIfAbsent(hash, resultBean) == null) { // 不可能回复相同hash的多个结果,忽略掉后面的好了
					zeze.NewProcedure(() -> {
						future.result(this, resultBean);
						return Procedure.Success;
					}, "ModuleRedirectAllResponse Procedure").Call();
				}
			} else
				hashResults.put(hash, null);
		}
		if (hashResults.size() >= concurrentLevel)
			getService().TryRemoveManualContext(getSessionId());
	}
}
