package Zeze.Util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeadlockBreaker extends ThreadHelper {
	private static final @NotNull Logger logger = LogManager.getLogger(DeadlockBreaker.class);

	/**
	 * @return root thread group
	 */
	public static @NotNull ThreadGroup getRootThreadGroup() {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		for (ThreadGroup p = root.getParent(); p != null; p = p.getParent())
			root = p;
		return root;
	}

	/**
	 * 枚举指定线程组下的线程。
	 *
	 * @param group   线程组
	 * @param recurse a flag indicating whether also to include threads in thread
	 *                groups that are subgroups of the thread group.
	 * @return thread in the group
	 */
	public static @NotNull Map<Long, Thread> enumerate(@NotNull ThreadGroup group, boolean recurse) {
		Thread[] threads = new Thread[512];
		while (true) {
			int size = group.enumerate(threads, recurse);
			if (size < threads.length) {
				var m = new HashMap<Long, Thread>();
				for (int i = 0; i < size; ++i) {
					var thread = threads[i];
					if (thread != null) {
						@SuppressWarnings("deprecation")
						var threadId = thread.getId();
						m.put(threadId, thread);
					}
				}
				return m;
			}
			threads = new Thread[threads.length * 2];
		}
	}

	private final @NotNull ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private final @NotNull Application zeze;
	private final @NotNull ThreadGroup rootThreadGroup;
	private int sleepIdleMs = 1000;
	private int detectCount = 0;

	public DeadlockBreaker(@NotNull Application zeze) {
		super("Zeze.Util.DeadlockBreaker");
		this.zeze = zeze;
		rootThreadGroup = getRootThreadGroup();
	}

	@Override
	public void run() {
		while (isRunning()) {
			try {
				if (detect()) {
					sleepIdleMs = 2000;
					detectCount++;
					if (detectCount >= 3)
						zeze.getAchillesHeelDaemon().deadlockReport(); // 向 daemon 报告。
				} else {
					detectCount = 0;
					sleepIdleMs *= 2;
					if (sleepIdleMs > zeze.getConfig().getDeadLockBreakerPeriod())
						sleepIdleMs = zeze.getConfig().getDeadLockBreakerPeriod();
				}
				sleepIdle(sleepIdleMs);
			} catch (Throwable ex) { // logger.error
				logger.error("angel run exception:", ex);
			}
		}
	}

	/**
	 * ThreadInfo 最多只打印出8层的栈信息 see ThreadInfo.MAX_FRAMES
	 */
	public static final int MAX_DEPTH = 255;

	/**
	 * 检测死锁，并尝试打破死锁环。
	 */
	private boolean detect() {
		// 返回死锁的线程。 可能包含多个环。 以及等待环上的线程。
		long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
		if (deadlockedThreadIds == null)
			return false;

		// 构建死锁线程信息映射。
		var deadlockedThreads = new HashMap<Long, ThreadInfo>();
		for (ThreadInfo tInfo : threadMXBean.getThreadInfo(deadlockedThreadIds,
				threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported())) {
			try {
				// getLockOwnerId == -1。不在等待被其他线程拥有的锁. 肯定不是环的一部分。
				// 这种情况按道理不可能发生在findDeadlockedThreads的结果中。
				// 如果出现，一般是并发访问造成的，如线程被销毁了。这里简单的忽略掉。
				if (tInfo != null && tInfo.getLockOwnerId() != -1)
					deadlockedThreads.put(tInfo.getThreadId(), tInfo);
			} catch (Exception e) {
				// 并发访问： 在构建过程中，线程发生了变动。忽略这种错误。
				logger.debug("critical exception");
			}
		}

		/*
		 * 所有的线程。用来提供ThreadId到Thread的转换，java不提供这种转换。 xdb.Worker
		 * 可以转换，对于其他自建线程，需要枚举系统内的所有线程，用来进行中断等操作。
		 * 在检测过程中，如果需要使用才初始化这个变量。由于线程本身动态创建和销毁的并发性，
		 * 这个'所有的线程'仅在这一次检测中有效，并且不保证所有的转换查找都能成功。
		 */
		Map<Long, Thread> allThreads = null;

		// 检测死锁环，从环中随机挑选一个线程，尝试执行中断操作。
		while (!deadlockedThreads.isEmpty()) {
			var cycle = new HashMap<Long, ThreadInfo>();
			ThreadInfo tInfo = deadlockedThreads.entrySet().iterator().next().getValue();
			do {
				if (cycle.put(tInfo.getThreadId(), tInfo) != null) {
					// cycle found.

					// dump interrupt info
					StringBuilder sb = new StringBuilder("Angel.interrupt thread \"");
					sb.append(tInfo.getThreadName()).append("\" Id=")
							.append(tInfo.getThreadId());
					sb.append(" in cycle:\n");
					for (ThreadInfo info : cycle.values()) // 打印的时候把挂在环上的线程也打出来。
						dumpThreadInfoTo(info, sb);
					logger.fatal(sb);

					// interrupt thread
					allThreads = interrupt(tInfo, allThreads);

					// break and try to find another cycle
					break;
				}
			} while ((tInfo = deadlockedThreads.get(tInfo.getLockOwnerId())) != null);
			// 删除已经被处理的线程。cycle是完整的环，或者是那些等待的环已被打破剩下的孤立枝节。
			deadlockedThreads.keySet().removeAll(cycle.keySet());
		}
		return true;
	}

	public static void dumpThreadInfoTo(@NotNull ThreadInfo tInfo, @NotNull StringBuilder sb) {
		sb.append('"').append(tInfo.getThreadName()).append('"');
		sb.append(" Id=").append(tInfo.getThreadId()).append(' ').append(tInfo.getThreadState());
		if (tInfo.getLockName() != null)
			sb.append(" on ").append(tInfo.getLockName());
		if (tInfo.getLockOwnerName() != null)
			sb.append(" owned by \"").append(tInfo.getLockOwnerName()).append("\" Id=").append(tInfo.getLockOwnerId());
		if (tInfo.isSuspended())
			sb.append(" (suspended)");
		if (tInfo.isInNative())
			sb.append(" (in native)");
		sb.append('\n');
		StackTraceElement[] stackTrace = tInfo.getStackTrace();
		int i = 0;
		for (; i < stackTrace.length && i < MAX_DEPTH; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat ").append(ste.toString()).append('\n');
			if (i == 0 && tInfo.getLockInfo() != null) {
				switch (tInfo.getThreadState()) {
				case BLOCKED:
					sb.append("\t-  blocked on ").append(tInfo.getLockInfo()).append('\n');
					break;
				case WAITING:
				case TIMED_WAITING:
					sb.append("\t-  waiting on ").append(tInfo.getLockInfo()).append('\n');
					break;
				}
			}
			for (var mi : tInfo.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i)
					sb.append("\t-  locked ").append(mi).append('\n');
			}
		}
		if (i < stackTrace.length)
			sb.append("\t...\n");

		LockInfo[] locks = tInfo.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizes = ").append(locks.length).append('\n');
			for (LockInfo li : locks)
				sb.append("\t- ").append(li).append('\n');
		}
		sb.append('\n');
	}

	/**
	 * 中断线程，吃掉所有错误。
	 */
	private @Nullable Map<Long, Thread> interrupt(@NotNull ThreadInfo tInfo, @Nullable Map<Long, Thread> allThreads) {
		try {
			// 当死锁线程不是Worker时，查找系统内的所有线程，也尝试中断。
			if (allThreads == null)
				allThreads = enumerate(rootThreadGroup, true);

			var thread = allThreads.get(tInfo.getThreadId());
			if (thread != null) {
				thread.interrupt();
				return allThreads;
			}
			logger.info("thread not found: {}", tInfo);
		} catch (Throwable e) { // logger.fatal
			logger.fatal(tInfo, e);
		}
		return allThreads;
	}
}
