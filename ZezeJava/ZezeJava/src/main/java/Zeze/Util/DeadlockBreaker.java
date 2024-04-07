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

public class DeadlockBreaker extends ThreadHelper {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * @return root thread group
	 */
	public static ThreadGroup getRootThreadGroup() {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		for (ThreadGroup p = root.getParent(); null != p; p = p.getParent())
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
	public static Map<Long, Thread> enumerate(ThreadGroup group, boolean recurse) {
		Thread[] threads = new Thread[512];
		while (true) {
			int size = group.enumerate(threads, recurse);
			if (size < threads.length) {
				var m = new HashMap<Long, Thread>();
				for (int i = 0; i < size; ++i) {
					Thread thread = threads[i];
					if (null != thread) {
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

	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private final ThreadGroup rootThreadGroup;
	private int sleepIdleMs = 1000;
	private final Application zeze;
	private int detectCount = 0;

	public DeadlockBreaker(Application zeze) {
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
					if (detectCount >= 3) {
						// 向 daemon 报告。
						zeze.getAchillesHeelDaemon().deadlockReport();
					}
				} else {
					detectCount = 0;
					sleepIdleMs *= 2;
					if (sleepIdleMs > zeze.getConfig().getDeadLockBreakerPeriod())
						sleepIdleMs = zeze.getConfig().getDeadLockBreakerPeriod();
				}
				sleepIdle(sleepIdleMs);
			} catch (Throwable ex) {
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
		if (null == deadlockedThreadIds)
			return false;

		// 构建死锁线程信息映射。
		var deadlockedThreads = new HashMap<Long, ThreadInfo>();
		for (ThreadInfo tinfo : threadMXBean.getThreadInfo(deadlockedThreadIds,
				threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported())) {
			try {
				// getLockOwnerId == -1。不在等待被其他线程拥有的锁. 肯定不是环的一部分。
				// 这种情况按道理不可能发生在findDeadlockedThreads的结果中。
				// 如果出现，一般是并发访问造成的，如线程被销毁了。这里简单的忽略掉。
				if (null != tinfo && tinfo.getLockOwnerId() != -1)
					deadlockedThreads.put(tinfo.getThreadId(), tinfo);
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
			Map<Long, ThreadInfo> cycle = new HashMap<>();
			ThreadInfo tinfo = deadlockedThreads.entrySet().iterator().next().getValue();
			do {
				if (null != cycle.put(tinfo.getThreadId(), tinfo)) {
					// cycle found.

					// dump interrupt info
					StringBuilder sb = new StringBuilder("Angel.interrupt thread \"");
					sb.append(tinfo.getThreadName()).append("\" Id=")
							.append(tinfo.getThreadId());
					sb.append(" in cycle:\n");
					for (ThreadInfo info : cycle.values()) // 打印的时候把挂在环上的线程也打出来。
						dumpThreadInfoTo(info, sb);
					logger.fatal(sb);

					// interrupt thread
					allThreads = interrupt(tinfo, allThreads);

					// break and try to find another cycle
					break;
				}
			} while ((tinfo = deadlockedThreads.get(tinfo.getLockOwnerId())) != null);
			// 删除已经被处理的线程。cycle是完整的环，或者是那些等待的环已被打破剩下的孤立枝节。
			deadlockedThreads.keySet().removeAll(cycle.keySet());
		}
		return true;
	}

	public static void dumpThreadInfoTo(ThreadInfo tinfo, StringBuilder sb) {
		sb.append("\"").append(tinfo.getThreadName()).append("\"");
		sb.append(" Id=").append(tinfo.getThreadId()).append(" ");
		sb.append(tinfo.getThreadState());
		if (tinfo.getLockName() != null) {
			sb.append(" on ").append(tinfo.getLockName());
		}
		if (tinfo.getLockOwnerName() != null) {
			sb.append(" owned by \"").append(tinfo.getLockOwnerName()).append("\" Id=").append(tinfo.getLockOwnerId());
		}
		if (tinfo.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (tinfo.isInNative()) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		StackTraceElement[] stackTrace = tinfo.getStackTrace();
		int i = 0;
		for (; i < stackTrace.length && i < MAX_DEPTH; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat ").append(ste.toString());
			sb.append('\n');
			if (i == 0 && tinfo.getLockInfo() != null) {
				Thread.State ts = tinfo.getThreadState();
				switch (ts) {
				case BLOCKED:
					sb.append("\t-  blocked on ").append(tinfo.getLockInfo());
					sb.append('\n');
					break;
				case WAITING:
				case TIMED_WAITING:
					sb.append("\t-  waiting on ").append(tinfo.getLockInfo());
					sb.append('\n');
					break;
				}
			}

			for (var mi : tinfo.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i) {
					sb.append("\t-  locked ").append(mi);
					sb.append('\n');
				}
			}
		}
		if (i < stackTrace.length) {
			sb.append("\t...");
			sb.append('\n');
		}

		LockInfo[] locks = tinfo.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizers = ").append(locks.length);
			sb.append('\n');
			for (LockInfo li : locks) {
				sb.append("\t- ").append(li);
				sb.append('\n');
			}
		}
		sb.append('\n');
	}

	/**
	 * 中断线程，吃掉所有错误。
	 */
	private Map<Long, Thread> interrupt(ThreadInfo tinfo, Map<Long, Thread> allThreads) {
		try {
			// 当死锁线程不是Worker时，查找系统内的所有线程，也尝试中断。
			if (null == allThreads)
				allThreads = enumerate(rootThreadGroup, true);

			Thread thread = allThreads.get(tinfo.getThreadId());
			if (null != thread) {
				thread.interrupt();
				return allThreads;
			}
			logger.info("thread not found: {}", tinfo);

		} catch (Throwable e) {
			logger.fatal(tinfo, e);
		}
		return allThreads;
	}
}
