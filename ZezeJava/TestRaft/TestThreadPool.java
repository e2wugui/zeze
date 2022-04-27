package Test;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
public class TestThreadPool {
    public static long count;
    public static final int ThreadCount = 10000;
    private static ThreadPoolExecutor threadPool;

    public static void add() {
        for (int i = 0; i < 1000; i++) {
            count++;
        }
    }

    public static class ThreadTask implements Runnable {
        public ThreadTask() {}
        @Override
        public void run() {
            try {
                Thread.sleep(15);
                Thread.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        threadPool = new ThreadPoolExecutor(ThreadCount, ThreadCount, 0L,
                TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < ThreadCount; i++) {
            threadPool.execute(new ThreadTask());
            add();
        }
        while (true) {
            if (threadPool.getActiveCount() < ThreadCount) {
                threadPool.execute(new ThreadTask());
                add();
            }
        }
    }
}
