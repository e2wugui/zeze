package Zeze.Arch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import Zeze.Application;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.FastLock;
import Zeze.Util.IntHashSet;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * RedirectAll 的结果回调句柄：onResult 逐 hash 回调，onAllDone 完成回调，await 同步等待。
 */
public interface RedirectAllFuture<R extends RedirectResult> {
	// 返回的future不能调用下面的接口方法,只用于给框架提供结果
	static <R extends RedirectResult> RedirectAllFuture<R> result(R r) {
		return new RedirectAllFutureFinished<>(r);
	}

	// 返回的future只能调用下面的asyncResult方法
	static <R extends RedirectResult> RedirectAllFuture<R> async() {
		return new RedirectAllFutureAsync<>();
	}

	// 只有通过async得到的future才能调用这个方法
	default void asyncResult(R r) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future, 同一future的情况下,此方法不会跟其它的onResult和onAllDone并发,每个结果回调一次
	default RedirectAllFuture<R> onResult(Action1<R> onResult) throws Exception {
		throw new IllegalStateException();
	}

	default RedirectAllFuture<R> OnResult(Action1<R> onResult) {
		throw new IllegalStateException();
	}

	// 只用于RedirectAll方法返回的future. 同一future的情况下,此方法不会跟onResult并发,只会回调一次
	default RedirectAllFuture<R> onAllDone(Action1<RedirectAllContext<R>> onAllDone)
			throws Exception {
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

/** 已完成的 RedirectAllFuture：仅包装既有结果，供 RedirectAllFuture.result 静态工厂返回。 */
record RedirectAllFutureFinished<R extends RedirectResult>(R result) implements RedirectAllFuture<R> {

}

/** async() 的单结果实现：CAS暂存先到的结果或回调，匹配后进入终态，再执行回调。 */
final class RedirectAllFutureAsync<R extends RedirectResult> implements RedirectAllFuture<R> {
	private static final VarHandle RESULT;
	private static final Object COMPLETED = new Object();

	private record Value<R>(R result) {
	}

	static {
		try {
			RESULT = MethodHandles.lookup().findVarHandle(RedirectAllFutureAsync.class, "result", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@SuppressWarnings("unused")
	private volatile @Nullable Object result;

	private @Nullable Object publish(Object value, boolean completing) {
		for (;;) {
			var current = result;
			if (current == COMPLETED || current != null && (current instanceof Value<?>) == completing)
				throw new IllegalStateException();
			if (RESULT.compareAndSet(this, current, current == null ? value : COMPLETED))
				return current;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void asyncResult(R r) {
		Object a = publish(new Value<>(Objects.requireNonNull(r)), true);
		if (a != null) {
			try {
				((Action1<R>)a).run(r);
			} catch (Exception e) {
				throw Task.forceThrow(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public RedirectAllFuture<R> onResult(Action1<R> onResult) throws Exception {
		Object r = publish(Objects.requireNonNull(onResult), false);
		if (r != null)
			onResult.run(((Value<R>)r).result());
		return this;
	}
}

/** RedirectAll 返回的 future 实现：按 hash 去重地回调 onResult，全部完成后回调 onAllDone，并支持 await 同步等待。 */
final class RedirectAllFutureImpl<R extends RedirectResult> extends FastLock implements RedirectAllFuture<R> {
	private static final Logger logger = LogManager.getLogger(RedirectAllFutureImpl.class);
	private static final VarHandle ON_ALL_DONE;

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
	private volatile @Nullable IntHashSet finishedHashes; // lazy-init
	private volatile boolean done;
	private final Condition cond = newCondition();

	private IntHashSet getFinishedHashes() {
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

	void result(RedirectAllContext<R> ctx, R result) {
		if (this.ctx == null)
			this.ctx = ctx;
		var onRes = onResult;
		if (onRes == null)
			return; // 等设置了onResult再处理
		var hashes = getFinishedHashes();
		lock(); // synchronized (hashes)
		try {
			if (!hashes.add(result.getHash())) // 跟onResult并发时有可能失败,谁加成功谁执行回调
				return;
		} finally {
			unlock();
		}
		invokeResult(getTransactionalApp(ctx), result, onRes, "RedirectAllFutureImpl.result");
	}

	private static @Nullable Application getTransactionalApp(RedirectAllContext<?> ctx) {
		//noinspection DataFlowIssue
		var zeze = ctx.getService().getZeze();
		return zeze != null && !zeze.isNoDatabase() ? zeze : null;
	}

	private void invokeResult(@Nullable Application zeze, R result,
			Action1<R> callback, String actionName) {
		if (zeze != null) {
			zeze.newProcedure(() -> {
				callback.run(result);
				return Procedure.Success;
			}, actionName).call();
		} else {
			try {
				callback.run(result);
			} catch (Throwable e) {
				logger.error("RedirectAll onResult failed: hash={}", result.getHash(), e);
			}
		}
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	public RedirectAllFuture<R> onResult(Action1<R> onResult) throws Exception {
		//noinspection ConstantValue
		if (onResult == null)
			throw new IllegalArgumentException("null onResult");
		this.onResult = onResult;
		var c = ctx;
		if (c == null)
			return this; // 等有了result再处理
		var hashes = getFinishedHashes();
		var readyResults = new ArrayList<R>();
		// 持 ctx 锁执行回调以与生产者路径（processResult）串行，锁序保持 ctx -> future。
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
			var zeze = getTransactionalApp(c); // 同一批结果保持开始时选定的事务模式。
			for (R result : readyResults)
				invokeResult(zeze, result, onResult, "RedirectAllFutureImpl.onResult");
		} finally {
			c.unlock();
		}
		return this;
	}

	@Override
	public RedirectAllFuture<R> OnResult(Action1<R> onResult) {
		try {
			return onResult(onResult);
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	void allDone(RedirectAllContext<R> ctx) {
		if (this.ctx == null)
			this.ctx = ctx;
		@SuppressWarnings("unchecked")
		var onA = (Action1<RedirectAllContext<R>>)ON_ALL_DONE.getAndSet(this, null);
		try {
			if (onA != null) {
				//noinspection DataFlowIssue
				var zeze = ctx.getService().getZeze();
				if (zeze != null && !zeze.isNoDatabase()) {
					zeze.newProcedure(() -> {
						onA.run(ctx);
						return Procedure.Success;
					}, "RedirectAllFutureImpl.allDone").call();
				} else {
					try {
						onA.run(ctx);
					} catch (Exception e) {
						throw Task.forceThrow(e);
					}
				}
			}
		} finally {
			lock();
			try {
				done = true;
				cond.signalAll();
			} finally {
				unlock();
			}
		}
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	public RedirectAllFuture<R> onAllDone(Action1<RedirectAllContext<R>> onAllDone) throws Exception {
		//noinspection ConstantValue
		if (onAllDone == null)
			throw new IllegalArgumentException("null onAllDone");
		var c = ctx;
		if (c == null || !c.isCompleted()) {
			this.onAllDone = onAllDone;
			if ((c = ctx) == null || !c.isCompleted() || !ON_ALL_DONE.compareAndSet(this, onAllDone, null)) // 再次确认,避免并发窗口问题
				return this;
		}
		// 持 ctx 锁遍历 hashResults，避免与并发写入竞争，锁序保持 ctx -> future。
		c.lock();
		try {
			//noinspection DataFlowIssue
			var zeze = c.getService().getZeze();
			if (zeze != null && !zeze.isNoDatabase()) {
				var c1 = c;
				zeze.newProcedure(() -> {
					onAllDone.run(c1);
					return Procedure.Success;
				}, "RedirectAllFutureImpl.onAllDone").call();
			} else
				onAllDone.run(c);
		} finally {
			c.unlock();
		}
		return this;
	}

	@Override
	public RedirectAllFuture<R> OnAllDone(Action1<RedirectAllContext<R>> onAllDone) {
		try {
			return onAllDone(onAllDone);
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	@Override
	public RedirectAllFuture<R> await() {
		if (!done) {
			var c = ctx;
			// 回调在 ctx 锁内执行，挂起等待仅释放 future 锁会与生产者路径死锁，提前拦截。
			if (c != null && c.isLockHeldByCurrentThread())
				throw new IllegalStateException(
						"RedirectAllFuture.await() cannot be called while holding this context's lock " +
								"(e.g. inside its own onResult callback): waiting would deadlock.");
			lock();
			try {
				try {
					while (!done)
						cond.await();
				} catch (InterruptedException e) {
					throw Task.forceThrow(e);
				}
			} finally {
				unlock();
			}
		}
		return this;
	}
}
