package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.IntHashSet;

public interface RedirectAllFuture<T extends RedirectResult> {
	// 返回的future不能调用下面的接口方法,只用于给框架提供结果
	public static <T extends RedirectResult> RedirectAllFuture<T> result(T t) {
		return new RedirectAllFutureFinished<>(t);
	}

	// 返回的future只能调用下面的asyncResult方法
	public static <T extends RedirectResult> RedirectAllFuture<T> async() {
		return new RedirectAllFutureAsync<>();
	}

	// 只有通过async得到的future才能调用这个方法
	default void asyncResult(T t) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future, 同一future的情况下,此方法不会跟其它的onResult和onAllDone并发,每个结果回调一次
	default RedirectAllFuture<T> onResult(Action1<T> onResult) throws Throwable {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future. 同一future的情况下,此方法不会跟onResult并发,只会回调一次
	default RedirectAllFuture<T> onAllDone(Action1<ModuleRedirectAllContext<T>> onAllDone) throws Throwable {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future
	default RedirectAllFuture<T> Wait() {
		throw new IllegalStateException();
	}
}

final class RedirectAllFutureFinished<T extends RedirectResult> implements RedirectAllFuture<T> {
	private final T result;

	RedirectAllFutureFinished(T result) {
		this.result = result;
	}

	T getResult() {
		return result;
	}
}

final class RedirectAllFutureAsync<T extends RedirectResult> implements RedirectAllFuture<T> {
	private static final VarHandle RESULT;

	static {
		try {
			RESULT = MethodHandles.lookup().findVarHandle(RedirectAllFutureAsync.class, "result", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unused")
	private volatile Object result;

	@SuppressWarnings("unchecked")
	@Override
	public void asyncResult(T t) {
		Object a = RESULT.getAndSet(this, t);
		if (a != null) {
			if (a instanceof RedirectResult)
				throw new IllegalStateException();
			try {
				((Action1<T>)a).run(t);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public RedirectAllFuture<T> onResult(Action1<T> onResult) throws Throwable {
		Object r = RESULT.getAndSet(this, onResult);
		if (r != null) {
			if (r instanceof RedirectResult)
				onResult.run((T)r);
			else
				throw new IllegalStateException();
		}
		return this;
	}
}

final class RedirectAllFutureImpl<T extends RedirectResult> implements RedirectAllFuture<T> {
	private static final VarHandle ON_ALL_DONE;

	static {
		try {
			ON_ALL_DONE = MethodHandles.lookup().findVarHandle(RedirectAllFutureImpl.class, "onAllDone", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private volatile Action1<T> onResult;
	private volatile Action1<ModuleRedirectAllContext<T>> onAllDone;
	private volatile ModuleRedirectAllContext<T> ctx;
	private Zeze.Application zeze;
	private IntHashSet finishedHashes; // lazy-init

	private IntHashSet getFinishedHashes() {
		var hashes = finishedHashes;
		if (hashes == null) {
			var newHashes = new IntHashSet();
			synchronized (this) {
				hashes = finishedHashes;
				if (hashes == null)
					finishedHashes = hashes = newHashes;
			}
		}
		return hashes;
	}

	void result(Zeze.Application zeze, ModuleRedirectAllContext<T> ctx, T result) throws Throwable {
		if (this.ctx == null) {
			this.zeze = zeze;
			this.ctx = ctx;
		}
		if (onResult == null)
			return; // 等设置了onResult再处理
		var hashes = getFinishedHashes();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (hashes) {
			if (!hashes.add(result.getHash()))
				return;
		}
		zeze.NewProcedure(() -> {
			onResult.run(result);
			return Procedure.Success;
		}, "RedirectAllFutureImpl.result").Call();
	}

	@Override
	public RedirectAllFuture<T> onResult(Action1<T> onResult) throws Throwable {
		if (onResult == null)
			throw new NullPointerException();
		this.onResult = onResult;
		if (ctx == null)
			return this;
		var hashes = getFinishedHashes();
		ArrayList<T> readyOnResults;
		//noinspection SynchronizeOnNonFinalField
		synchronized (ctx) {
			var results = ctx.getAllResults();
			if (results.isEmpty())
				return this;
			readyOnResults = new ArrayList<>();
			for (var it = results.iterator(); it.moveToNext(); ) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (hashes) {
					if (hashes.add(it.key()))
						readyOnResults.add(it.value());
				}
			}
		}
		for (T needOnResult : readyOnResults) {
			zeze.NewProcedure(() -> {
				onResult.run(needOnResult);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onResult").Call();
		}
		return this;
	}

	void allDone(Zeze.Application zeze, ModuleRedirectAllContext<T> ctx) throws Throwable {
		if (ctx == null)
			throw new NullPointerException();
		if (this.ctx == null) {
			this.zeze = zeze;
			this.ctx = ctx;
		}
		@SuppressWarnings("unchecked")
		var onA = (Action1<ModuleRedirectAllContext<T>>)ON_ALL_DONE.getAndSet(this, null);
		if (onA != null) {
			zeze.NewProcedure(() -> {
				onA.run(ctx);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.allDone").Call();
		}
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public RedirectAllFuture<T> onAllDone(Action1<ModuleRedirectAllContext<T>> onAllDone) throws Throwable {
		if (onAllDone == null)
			throw new NullPointerException();
		if (ctx == null || !ctx.isCompleted()) {
			this.onAllDone = onAllDone;
			if (ctx == null || !ctx.isCompleted()) // 再次确认,避免并发窗口问题
				return this;
			@SuppressWarnings("unchecked")
			var onA = (Action1<ModuleRedirectAllContext<T>>)ON_ALL_DONE.getAndSet(this, null);
			onAllDone = onA;
		}
		if (onAllDone != null) {
			var onA = onAllDone;
			zeze.NewProcedure(() -> {
				onA.run(ctx);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onAllDone").Call();
		}
		return this;
	}

	@Override
	public RedirectAllFuture<T> Wait() {
		if (ctx == null || !ctx.isCompleted()) {
			synchronized (this) {
				while (ctx != null && ctx.isCompleted()) {
					try {
						wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return this;
	}
}
