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
	private final Action1<ModuleRedirectAllContext<R>> onHashResult;
	private int lastResultHash = -1; // 最近收到结果的hash

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> decodeResult) {
		this(concurrentLevel, decodeResult, null);
	}

	public ModuleRedirectAllContext(int concurrentLevel, Function<Binary, R> decodeResult,
									Action1<ModuleRedirectAllContext<R>> onHashResult) {
		this.concurrentLevel = concurrentLevel;
		this.decodeResult = decodeResult;
		this.onHashResult = onHashResult;
	}

	public final boolean isCompleted() {
		return hashResults.size() >= concurrentLevel || lastResultHash == -2;
	}

	public final boolean isTimeout() {
		return lastResultHash == -2;
	}

	public final R getLastResult() {
		return hashResults.get(lastResultHash);
	}

	// 只用于AllDone时获取所有结果, 此时不会再修改hashResults所以没有并发问题
	public final IntHashMap<R> getAllResults() {
		return hashResults;
	}

	@Override
	public synchronized void OnTimeout() throws Throwable {
		if (hashResults.size() < concurrentLevel && lastResultHash != -2) {
			lastResultHash = -2;
			if (onHashResult != null) {
				getService().getZeze().NewProcedure(() -> {
					onHashResult.run(this);
					return 0L;
				}, "ModuleRedirectAllResponse timeout Procedure").Call();
			}
		}
	}

	@Override
	public void OnRemoved() throws Throwable {
		if (concurrentLevel <= 0 && onHashResult != null) {
			getService().getZeze().NewProcedure(() -> {
				onHashResult.run(this);
				return 0L;
			}, "ModuleRedirectAllResponse no-result Procedure").Call();
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	@SuppressWarnings("deprecation")
	public final synchronized void ProcessResult(Zeze.Application zeze, ModuleRedirectAllResult allResult) throws Throwable {
		if (isTimeout())
			return; // 如果恰好刚处理了超时,那就只能忽略后续的结果了
		for (var e : allResult.Argument.getHashs().entrySet()) {
			int hash = e.getKey();
			var result = e.getValue();
			var resultCode = result.getReturnCode();
			R resultBean = decodeResult.apply(resultCode == Procedure.Success ? result.getParams() : null);
			resultBean.setSessionId(getSessionId());
			resultBean.setHash(hash);
			resultBean.setResultCode(resultCode);
			if (hashResults.putIfAbsent(hash, resultBean) != null)
				continue; // 不可能回复相同hash的多个结果,忽略掉后面的好了
			lastResultHash = hash;
			if (onHashResult != null) {
				zeze.NewProcedure(() -> {
					onHashResult.run(this);
					return 0L;
				}, "ModuleRedirectAllResponse Procedure").Call();
			}
			if (hashResults.size() >= concurrentLevel)
				getService().TryRemoveManualContext(getSessionId());
		}
	}
}
