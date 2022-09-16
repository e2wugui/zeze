package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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

	default RedirectAllFuture<R> OnResult(Action1<R> onResult) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future. 同一future的情况下,此方法不会跟onResult并发,只会回调一次
	default RedirectAllFuture<R> onAllDone(Action1<RedirectAllContext<R>> onAllDone) throws Throwable {
		throw new IllegalStateException();
	}

	default RedirectAllFuture<R> OnAllDone(Action1<RedirectAllContext<R>> onAllDone) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future
	default RedirectAllFuture<R> await() {
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
	private volatile Action1<RedirectAllContext<R>> onAllDone;
	private volatile RedirectAllContext<R> ctx;
	private IntHashSet finishedHashes; // lazy-init
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();

	private IntHashSet getFinishedHashes() {
		var hashes = finishedHashes;
		if (hashes == null) {
			var newHashes = new IntHashSet();
			lock.lock();
			try {
				if ((hashes = finishedHashes) == null)
					finishedHashes = hashes = newHashes;
			} finally {
				lock.unlock();
			}
		}
		return hashes;
	}

	void result(RedirectAllContext<R> ctx, R result) throws Throwable {
		if (this.ctx == null)
			this.ctx = ctx;
		if (onResult == null)
			return; // 等设置了onResult再处理
		var hashes = getFinishedHashes();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (hashes) {
			if (!hashes.add(result.getHash())) // 跟onResult并发时有可能失败,谁加成功谁执行回调
				return;
		}
		ctx.getService().getZeze().newProcedure(() -> {
			onResult.run(result);
			return Procedure.Success;
		}, "RedirectAllFutureImpl.result").Call();
	}

	@Override
	public RedirectAllFuture<R> onResult(Action1<R> onResult) throws Throwable {
		if (onResult == null)
			throw new IllegalArgumentException("null onResult");
		this.onResult = onResult;
		var c = ctx;
		if (c == null)
			return this; // 等有了result再处理
		var hashes = getFinishedHashes();
		var readyResults = new ArrayList<R>();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (c) {
			for (var it = c.getAllResults().iterator(); it.moveToNext(); ) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (hashes) {
					if (hashes.add(it.key())) // 跟onResult并发时有可能失败,谁加成功谁执行回调
						readyResults.add(it.value());
				}
			}
		}
		for (R result : readyResults) {
			c.getService().getZeze().newProcedure(() -> {
				onResult.run(result);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.onResult").Call();
		}
		return this;
	}

	@Override
	public RedirectAllFuture<R> OnResult(Action1<R> onResult) {
		try {
			return onResult(onResult);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	void allDone(RedirectAllContext<R> ctx) throws Throwable {
		if (this.ctx == null)
			this.ctx = ctx;
		@SuppressWarnings("unchecked")
		var onA = (Action1<RedirectAllContext<R>>)ON_ALL_DONE.getAndSet(this, null);
		if (onA != null) {
			ctx.getService().getZeze().newProcedure(() -> {
				onA.run(ctx);
				return Procedure.Success;
			}, "RedirectAllFutureImpl.allDone").Call();
		}
		lock.lock();
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public RedirectAllFuture<R> onAllDone(Action1<RedirectAllContext<R>> onAllDone) throws Throwable {
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
		}, "RedirectAllFutureImpl.onAllDone").Call();
		return this;
	}

	@Override
	public RedirectAllFuture<R> OnAllDone(Action1<RedirectAllContext<R>> onAllDone) {
		try {
			return onAllDone(onAllDone);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RedirectAllFuture<R> await() {
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			lock.lock();
			try {
				try {
					while ((c = ctx) == null || !c.isCompleted())
						cond.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			} finally {
				lock.unlock();
			}
		}
		return this;
	}
}
