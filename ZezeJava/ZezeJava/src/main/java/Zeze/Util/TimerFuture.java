package Zeze.Util;

import java.io.Closeable;
import java.util.concurrent.Future;

public class TimerFuture implements Closeable {
	private volatile Future<?> timer;
	private TaskCompletionSource<TimerFuture> running;

	synchronized void setFuture(Future<?> timer) {
		this.timer = timer;
	}

	public Future<?> getFuture() {
		return timer;
	}

	public void cancelJoin() {
		cancelJoin(true);
	}

	public void cancelJoin(boolean mayInterruptIfRunning) {
		TaskCompletionSource<TimerFuture> r;
		synchronized (this) {
			timer.cancel(mayInterruptIfRunning);
			r = running;
		}
		if (null != r)
			r.await();
	}

	public synchronized TimerFuture beginRun() {
		if (timer.isCancelled())
			return null;
		// ScheduledExecutorService 调度的任务正在执行的时候不会再次调度。
		// 这里就不保护了。 if (null != running)
		running = new TaskCompletionSource<>();
		return this;
	}

	@Override
	public synchronized void close() {
		System.out.println("close");
		if (null != running) {
			running.setResult(this);
			running = null;
		}
	}

	public static void main(String [] args) throws InterruptedException {
		Task.tryInitThreadPool(null, null, null);

		var timer = Task.scheduleUnsafeEx(1, 1, (timerFuture) -> {
			try (var running = timerFuture.beginRun()) {
				System.out.println("run enter.");
				if (null == running)
					return;
				System.out.println("run begin.");
				Thread.sleep(30);
				System.out.println("run end.");
			}
		});
		Thread.sleep(30);
		System.out.println("join begin.");
		timer.cancelJoin(false);
		System.out.println("join end.");
	}
}
