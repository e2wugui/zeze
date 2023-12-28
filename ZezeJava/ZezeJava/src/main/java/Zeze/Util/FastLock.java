package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 高性能的非可重入锁,开启压缩指针时包括对象头共24字节,只有1个等待线程不分配额外线程,再多等待每个占24字节
public class FastLock {
	private static final @NotNull VarHandle vhState, vhWaitHead;

	static {
		try {
			var lookup = MethodHandles.lookup();
			vhState = lookup.findVarHandle(FastLock.class, "state", int.class);
			vhWaitHead = lookup.findVarHandle(FastLock.class, "waitHead", Object.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final class Node {
		final @NotNull Thread thread;
		final @NotNull Object next; // Node or Thread

		Node(@NotNull Thread thread, @NotNull Object next) {
			this.thread = thread;
			this.next = next;
		}
	}

	private volatile int state;
	@SuppressWarnings("unused")
	private volatile @Nullable Object waitHead; // Node -> ... -> Node -> Thread
	private Thread ownerThread;

	private void push(@NotNull Thread t) {
		for (; ; ) {
			var h = waitHead;
			if (vhWaitHead.compareAndSet(this, h, h == null ? t : new Node(t, h)))
				return;
		}
	}

	private boolean tryPopAndUnpark() {
		for (; ; ) {
			var h = waitHead;
			if (h == null)
				return false;
			Object next;
			Thread thread;
			if (h instanceof Node) {
				var n = (Node)h;
				next = n.next;
				thread = n.thread;
			} else {
				next = null;
				thread = (Thread)h;
			}
			if (vhWaitHead.compareAndSet(this, h, next)) {
				ownerThread = thread;
				LockSupport.unpark(thread);
				return true;
			}
		}
	}

	public boolean tryLock() {
		return vhState.compareAndSet(this, 0, 1);
	}

	public void lock() {
		if (!tryLock())
			lockSlow();
	}

	private void lockSlow() {
		var ct = Thread.currentThread();
		push(ct);
		do {
			LockSupport.park();
		} while (ownerThread != ct);
		ownerThread = null;
	}

	public void unlock() {
		while (!tryPopAndUnpark()) {
			state = 0;
			if (waitHead == null || !tryLock()) // retry
				break;
		}
	}
}
