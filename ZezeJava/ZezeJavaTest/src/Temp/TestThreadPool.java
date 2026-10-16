package Temp;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestThreadPool {
	public static long count;
	public static final int ThreadCount = 10000;
	private static @Nullable ThreadPoolExecutor threadPool;

	public static void add() {
		for (int i = 0; i < 1000; i++) {
			count++;
		}
	}

	public static void tryNotifyAdd() {
		if (Objects.requireNonNull(threadPool).getQueue().size() < ThreadCount)
			//noinspection SynchronizeOnNonFinalField
			synchronized (Objects.requireNonNull(threadPool)) {
				Objects.requireNonNull(threadPool).notify();
			}
	}

	public static void waitAdd() throws InterruptedException {
		//noinspection SynchronizeOnNonFinalField
		synchronized (Objects.requireNonNull(threadPool)) {
			Objects.requireNonNull(threadPool).wait();
		}
	}

	public static class ThreadTask implements Runnable {
		public ThreadTask() {
		}

		@Override
		public void run() {
			try {
				Thread.sleep(15);
				Thread.sleep(15);
				add();
				tryNotifyAdd();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		threadPool = new ThreadPoolExecutor(ThreadCount, ThreadCount, 0L,
				TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>(),
				Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

		var lastReportTime = System.currentTimeMillis();
		var lastCompletedTaskCount = 0L;
		for (int i = 0; i < ThreadCount; i++) {
			Objects.requireNonNull(threadPool).execute(new ThreadTask());
		}
		//noinspection InfiniteLoopStatement
		while (true) {
			var need = ThreadCount * 2 - Objects.requireNonNull(threadPool).getQueue().size();
			for (int i = 0; i < need; ++i) {
				Objects.requireNonNull(threadPool).execute(new ThreadTask());
			}
			var now = System.currentTimeMillis();
			var elapsed = now - lastReportTime;
			if (elapsed > 10 * 1000) {
				var completedTaskCount = Objects.requireNonNull(threadPool).getCompletedTaskCount();
				System.out.println((float)(completedTaskCount - lastCompletedTaskCount) / elapsed * 1000.0f);
				lastCompletedTaskCount = completedTaskCount;
				lastReportTime = now;
			}
			waitAdd();
		}
	}
}
