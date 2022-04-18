package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.IntHashSet;

public interface RedirectAllFuture<R extends RedirectResult> {
	// 返回的future不能调用下面的接口方法,只用于给框架提供结果
	public static <R extends RedirectResult> RedirectAllFuture<R> result(R r) {
		return new RedirectAllFutureFinished<>(r);
	}

	// 返回的future只能调用下面的asyncResult方法
	public static <R extends RedirectResult> RedirectAllFuture<R> async() {
		return new RedirectAllFutureAsync<>();
	}

	// 只有通过async得到的future才能调用这个方法
	default void asyncResult(R r) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future, 同一future的情况下,此方法不会跟其它的onResult和onAllDone并发,每个结果回调一次
	default RedirectAllFuture<R> onResult(Action1<R> onResult) throws Throwable {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future. 同一future的情况下,此方法不会跟onResult并发,只会回调一次
	default RedirectAllFuture<R> onAllDone(Action1<ModuleRedirectAllContext<R>> onAllDone) throws Throwable {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future
	default RedirectAllFuture<R> Wait() {
		throw new IllegalStateException();
	}
}

final class RedirectAllFutureFinished<R extends RedirectResult> implements RedirectAllFuture<R> {
	private final R result;

	RedirectAllFutureFinished(R result) {
		this.result = result;
	}

	R getResult() {
		return result;
	}
}

final class RedirectAllFutureAsync<R extends RedirectResult> implements RedirectAllFuture<R> {
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
	public void asyncResult(R r) {
		Object a = RESULT.getAndSet(this, r);
		if (a != null) {
			if (a instanceof RedirectResult)
				throw new IllegalStateException();
			try {
				((Action1<R>)a).run(r);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public RedirectAllFuture<R> onResult(Action1<R> onResult) throws Throwable {
		Object r = RESULT.getAndSet(this, onResult);
		if (r != null) {
			if (r instanceof RedirectResult)
				onResult.run((R)r);
			else
				throw new IllegalStateException();
		}
		return this;
	}
}

final class RedirectAllFutureImpl<R extends RedirectResult> implements RedirectAllFuture<R> {
	private static final VarHandle ON_ALL_DONE;

	static {
		try {
			ON_ALL_DONE = MethodHandles.lookup().findVarHandle(RedirectAllFutureImpl.class, "onAllDone", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private volatile Action1<R> onResult;
	private volatile Action1<ModuleRedirectAllContext<R>> onAllDone;
	private volatile ModuleRedirectAllContext<R> ctx;
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

	void result(ModuleRedirectAllContext<R> ctx, R result) throws Throwable {
		if (this.ctx == null)
			this.ctx = ctx;
		if (onResult == null)
			return; // 等设置了onResult再处理
		var hashes = getFinishedHashes();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (hashes) {
			if (!hashes.add(result.getHash()))
				return;
		}
		ctx.getService().getZeze().NewProcedure(() -> {
			onResult.run(result);
			return Procedure.Success;
		}, "RedirectAllFutureImpl.result").Call();
	}

	@Override
	public RedirectAllFuture<R> onResult(Action1<R> onResult) throws Throwable {
		if (onResult == null)
			throw new NullPointerException();
		this.onResult = onResult;
		var c = ctx;
		if (c == null)
			return this;
		var hashes = getFinishedHashes();
		ArrayList<R> readyOnResults;
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (c) {
			var results = c.getAllResults();
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
		for (R needOnResult : readyOnResults) {
			c.getService().getZeze().NewProcedure(() -> {
				onResult.run(needOnResult);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onResult").Call();
		}
		return this;
	}

	void allDone(ModuleRedirectAllContext<R> ctx) throws Throwable {
		if (ctx == null)
			throw new NullPointerException();
		if (this.ctx == null)
			this.ctx = ctx;
		@SuppressWarnings("unchecked")
		var onA = (Action1<ModuleRedirectAllContext<R>>)ON_ALL_DONE.getAndSet(this, null);
		if (onA != null) {
			ctx.getService().getZeze().NewProcedure(() -> {
				onA.run(ctx);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.allDone").Call();
		}
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public RedirectAllFuture<R> onAllDone(Action1<ModuleRedirectAllContext<R>> onAllDone) throws Throwable {
		if (onAllDone == null)
			throw new NullPointerException();
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			this.onAllDone = onAllDone;
			c = ctx;
			if (c == null || !c.isCompleted()) // 再次确认,避免并发窗口问题
				return this;
			@SuppressWarnings("unchecked")
			var onA = (Action1<ModuleRedirectAllContext<R>>)ON_ALL_DONE.getAndSet(this, null);
			onAllDone = onA;
		}
		if (onAllDone != null) {
			var onA = onAllDone;
			var c1 = c;
			c.getService().getZeze().NewProcedure(() -> {
				onA.run(c1);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onAllDone").Call();
		}
		return this;
	}

	@Override
	public RedirectAllFuture<R> Wait() {
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			synchronized (this) {
				c = ctx;
				while (c != null && c.isCompleted()) {
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
