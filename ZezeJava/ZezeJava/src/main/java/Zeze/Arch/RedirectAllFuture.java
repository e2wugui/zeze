package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.FastLock;
import Zeze.Util.IntHashSet;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RedirectAllFuture<R extends RedirectResult> {
	// 返回的future不能调用下面的接口方法,只用于给框架提供结果
	static <R extends RedirectResult> @NotNull RedirectAllFuture<R> result(R r) {
		return new RedirectAllFutureFinished<>(r);
	}

	// 返回的future只能调用下面的asyncResult方法
	static <R extends RedirectResult> @NotNull RedirectAllFuture<R> async() {
		return new RedirectAllFutureAsync<>();
	}

	// 只有通过async得到的future才能调用这个方法
	default void asyncResult(R r) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future, 同一future的情况下,此方法不会跟其它的onResult和onAllDone并发,每个结果回调一次
	default @NotNull RedirectAllFuture<R> onResult(@NotNull Action1<R> onResult) throws Exception {
		throw new IllegalStateException();
	}

	default @NotNull RedirectAllFuture<R> OnResult(@NotNull Action1<R> onResult) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future. 同一future的情况下,此方法不会跟onResult并发,只会回调一次
	default @NotNull RedirectAllFuture<R> onAllDone(@NotNull Action1<RedirectAllContext<R>> onAllDone)
			throws Exception {
		throw new IllegalStateException();
	}

	default @NotNull RedirectAllFuture<R> OnAllDone(@NotNull Action1<RedirectAllContext<R>> onAllDone) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future
	default @NotNull RedirectAllFuture<R> await() {
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
	private static final @NotNull VarHandle RESULT;

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
			} catch (Exception e) {
				Task.forceThrow(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull RedirectAllFuture<R> onResult(@NotNull Action1<R> onResult) throws Exception {
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

final class RedirectAllFutureImpl<R extends RedirectResult> extends FastLock implements RedirectAllFuture<R> {
	private static final @NotNull VarHandle ON_ALL_DONE;

	static {
		try {
			ON_ALL_DONE = MethodHandles.lookup().findVarHandle(RedirectAllFutureImpl.class, "onAllDone", Action1.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private volatile @Nullable Action1<R> onResult;
	private volatile @Nullable Action1<RedirectAllContext<R>> onAllDone;
	private volatile @Nullable RedirectAllContext<R> ctx;
	private @Nullable IntHashSet finishedHashes; // lazy-init
	private final @NotNull Condition cond = newCondition();

	private @NotNull IntHashSet getFinishedHashes() {
		var hashes = finishedHashes;
		if (hashes == null) {
			var newHashes = new IntHashSet();
			lock();
			try {
				if ((hashes = finishedHashes) == null)
					finishedHashes = hashes = newHashes;
			} finally {
				unlock();
			}
		}
		return hashes;
	}

	void result(@NotNull RedirectAllContext<R> ctx, @NotNull R result) {
		if (this.ctx == null)
			this.ctx = ctx;
		if (onResult == null)
			return; // 等设置了onResult再处理
		var hashes = getFinishedHashes();
		lock(); // synchronized (hashes)
		try {
			if (!hashes.add(result.getHash())) // 跟onResult并发时有可能失败,谁加成功谁执行回调
				return;
		} finally {
			unlock();
		}
		ctx.getService().getZeze().newProcedure(() -> {
			//noinspection DataFlowIssue
			onResult.run(result);
			return Procedure.Success;
		}, "RedirectAllFutureImpl.result").call();
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	public @NotNull RedirectAllFuture<R> onResult(@NotNull Action1<R> onResult) throws Exception {
		//noinspection ConstantValue
		if (onResult == null)
			throw new IllegalArgumentException("null onResult");
		this.onResult = onResult;
		var c = ctx;
		if (c == null)
			return this; // 等有了result再处理
		var hashes = getFinishedHashes();
		var readyResults = new ArrayList<R>();
		c.lock();
		try {
			for (var it = c.getAllResults().iterator(); it.moveToNext(); ) {
				lock(); // synchronized (hashes)
				try {
					if (hashes.add(it.key())) // 跟onResult并发时有可能失败,谁加成功谁执行回调
						readyResults.add(it.value());
				} finally {
					unlock();
				}
			}
		} finally {
			c.unlock();
		}
		for (R result : readyResults) {
			c.getService().getZeze().newProcedure(() -> {
				onResult.run(result);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onResult").call();
		}
		return this;
	}

	@Override
	public @NotNull RedirectAllFuture<R> OnResult(@NotNull Action1<R> onResult) {
		try {
			return onResult(onResult);
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	void allDone(@NotNull RedirectAllContext<R> ctx) {
		if (this.ctx == null)
			this.ctx = ctx;
		@SuppressWarnings("unchecked")
		var onA = (Action1<RedirectAllContext<R>>)ON_ALL_DONE.getAndSet(this, null);
		if (onA != null) {
			ctx.getService().getZeze().newProcedure(() -> {
				onA.run(ctx);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.allDone").call();
		}
		lock();
		try {
			cond.signalAll();
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	public @NotNull RedirectAllFuture<R> onAllDone(@NotNull Action1<RedirectAllContext<R>> onAllDone) throws Exception {
		//noinspection ConstantValue
		if (onAllDone == null)
			throw new IllegalArgumentException("null onAllDone");
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			this.onAllDone = onAllDone;
			if ((c = ctx) == null || !c.isCompleted() || !ON_ALL_DONE.compareAndSet(this, onAllDone, null)) // 再次确认,避免并发窗口问题
				return this;
		}
		var c1 = c;
		c.getService().getZeze().newProcedure(() -> {
			onAllDone.run(c1);
			return Procedure.Success;
		}, "RedirectAllFutureImpl.onAllDone").call();
		return this;
	}

	@Override
	public @NotNull RedirectAllFuture<R> OnAllDone(@NotNull Action1<RedirectAllContext<R>> onAllDone) {
		try {
			return onAllDone(onAllDone);
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	@Override
	public @NotNull RedirectAllFuture<R> await() {
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			lock();
			try {
				try {
					while ((c = ctx) == null || !c.isCompleted())
						cond.await();
				} catch (InterruptedException e) {
					Task.forceThrow(e);
				}
			} finally {
				unlock();
			}
		}
		return this;
	}
}
