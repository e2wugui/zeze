package Zeze.Arch;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.IntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RedirectAllContext<R extends RedirectResult> extends Service.ManualContext {
	private final int concurrentLevel;
	private final IntHashMap<R> hashResults = new IntHashMap<>(); // <hash, result>
	private final @Nullable Function<Binary, R> resultDecoder;
	private final @Nullable RedirectAllFutureImpl<R> future;
	private final ReentrantLock lock = new ReentrantLock();

	public RedirectAllContext(int concurrentLevel, @Nullable Function<Binary, R> resultDecoder) {
		this.concurrentLevel = concurrentLevel;
		this.resultDecoder = resultDecoder;
		future = resultDecoder != null ? new RedirectAllFutureImpl<>() : null;
	}

	public int getConcurrentLevel() {
		return concurrentLevel;
	}

	// 只用于AllDone时获取所有结果, 此时不会再修改hashResults所以没有并发问题
	public @NotNull IntHashMap<R> getAllResults() {
		return hashResults;
	}

	public RedirectAllFuture<R> getFuture() {
		return future;
	}

	void lock() {
		lock.lock();
	}

	void unlock() {
		lock.unlock();
	}

	public boolean isCompleted() {
		return hashResults.size() >= concurrentLevel || isTimeout();
	}

	@Override
	public void onRemoved() {
		lock();
		try {
			if (isCompleted() && future != null)
				future.allDone(this);
		} finally {
			unlock();
		}
	}

	// 这里处理真正redirect发生时，从远程返回的结果。
	public void processResult(@NotNull ModuleRedirectAllResult res) {
		lock();
		try {
			if (isCompleted())
				return; // 如果已经超时,那就只能忽略后续的结果了
			for (var e : res.Argument.getHashs().entrySet()) {
				int hash = e.getKey();
				var resultData = e.getValue();
				var resultCode = resultData.getReturnCode();
				if (resultDecoder != null) {
					R result = resultDecoder.apply(resultCode == Procedure.Success ? resultData.getParams() : null);
					result.setHash(hash);
					result.setResultCode(resultCode);
					if (hashResults.putIfAbsent(hash, result) == null) { // 不可能回复相同hash的多个结果,忽略掉后面的好了
						//noinspection DataFlowIssue
						future.result(this, result);
					}
				} else
					hashResults.put(hash, null);
			}
			if (hashResults.size() >= concurrentLevel)
				getService().tryRemoveManualContext(getSessionId());
		} finally {
			unlock();
		}
	}
}
