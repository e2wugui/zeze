package Zeze.Util;

/**
 * 内联派发的回调锁，不支持重入。取得派发权的线程按就绪队列顺序执行，直到队列耗尽。
 * 锁忙时 enter 只入队；回调内 leave 只清持锁身份，下一回调在当前回调返回后执行。
 */
public final class InlineAsyncLock extends AbstractAsyncLock {
	@Override
	public void enter(Action0 onEnter) {
		readyQueue.offer(onEnter); // 无竞争时也入队，不插队。
		if (tryLock())
			drainInline(null);
	}

	@Override
	public void leave() {
		clearOwner();
	}
}
