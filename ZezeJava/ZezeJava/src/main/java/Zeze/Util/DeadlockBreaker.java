package Zeze.Util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import Zeze.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 死锁检测守护线程：结合 ThreadMXBean 与 FastLock 等待登记表检测死锁环（含虚拟线程），报告并尝试打断。
 */
public class DeadlockBreaker extends ThreadHelper {
	private static final Logger logger = LogManager.getLogger(DeadlockBreaker.class);

	/** 返回根线程组。 */
	public static ThreadGroup getRootThreadGroup() {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		for (ThreadGroup p = root.getParent(); p != null; p = p.getParent())
			root = p;
		return root;
	}

	/**
	 * 枚举指定线程组下的线程。
	 *
	 * @param group   线程组
	 * @param recurse 是否递归包含子线程组
	 * @return 线程 ID 到线程对象的映射
	 */
	public static Map<Long, Thread> enumerate(ThreadGroup group, boolean recurse) {
		@Nullable Thread[] threads = new Thread[512];
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

	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private final Application zeze;
	private final ThreadGroup rootThreadGroup;
	private int sleepIdleMs = 1000;
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
						// 单进程部署无 AchillesHeelDaemon，判空避免 NPE 跳过 sleepIdle
						var daemon = zeze.getAchillesHeelDaemon();
						if (daemon != null)
							daemon.deadlockReport();
						else
							logger.error("deadlock detected but no AchillesHeelDaemon configured");
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

	/** ThreadInfo 默认最多打印 8 层栈信息（见 ThreadInfo.MAX_FRAMES）。 */
	public static final int MAX_DEPTH = 255;

	/** 检测死锁并尝试打破死锁环。 */
	private boolean detect() {
		boolean found = false;

		// 平台线程的 monitor / ownable-synchronizer 死锁
		long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
		if (deadlockedThreadIds != null) {
			var deadlockedThreads = new HashMap<Long, ThreadInfo>();
			for (ThreadInfo tInfo : threadMXBean.getThreadInfo(deadlockedThreadIds,
					threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported())) {
				try {
					// 并发下线程可能已退出或释放锁，忽略非等待节点
					if (tInfo != null && tInfo.getLockOwnerId() != -1)
						deadlockedThreads.put(tInfo.getThreadId(), tInfo);
				} catch (Exception e) {
					logger.debug("concurrent thread change", e);
				}
			}

			// 懒加载全线程快照以将 threadId 映射为 Thread（仅本轮检测有效）
			Map<Long, Thread> allThreads = null;

			while (!deadlockedThreads.isEmpty()) {
				var cycle = new HashMap<Long, ThreadInfo>();
				ThreadInfo tInfo = deadlockedThreads.entrySet().iterator().next().getValue();
				do {
					if (cycle.put(tInfo.getThreadId(), tInfo) != null) {
						StringBuilder sb = new StringBuilder("Angel.interrupt thread \"");
						sb.append(tInfo.getThreadName()).append("\" Id=")
								.append(tInfo.getThreadId());
						sb.append(" in cycle:\n");
						for (ThreadInfo info : cycle.values()) // 一并打印挂在环上的等待线程
							dumpThreadInfoTo(info, sb);
						logger.fatal(sb);

						allThreads = interrupt(tInfo, allThreads);
						break;
					}
				} while ((tInfo = deadlockedThreads.get(tInfo.getLockOwnerId())) != null);
				// 删除本轮已处理的环及其孤立枝节
				deadlockedThreads.keySet().removeAll(cycle.keySet());
			}
			found = true;
		}

		if (reportAndBreakLockWaitCycles())
			found = true;

		return found;
	}

	/**
	 * 基于 {@link FastLock#waitingThreads} 检测 FastLock 等待死锁环（覆盖虚拟线程，JDK ThreadMXBean 不含 VT）。
	 * 以 {@link LockSupport#getBlocker} 确认阻塞状态后在函数图上找环；经 Condition 重获取锁构成的环不在登记范围。
	 *
	 * @return 本轮发现的死锁环（每环为构成环的线程列表）
	 */
	public static List<List<Thread>> findLockWaitDeadlockCycles() {
		var edges = new HashMap<Thread, Thread>();
		for (var entry : FastLock.waitingThreads.entrySet()) {
			var waiter = entry.getKey();
			var lock = entry.getValue();
			if (!waiter.isAlive() || LockSupport.getBlocker(waiter) != lock)
				continue;
			var owner = lock.getOwner();
			if (owner != null && owner.isAlive())
				edges.put(waiter, owner); // owner==waiter 保留为自环（非重入锁重入误用）
		}

		// 函数图找环：沿唯一出边推进，遇到当前路径节点即成环
		var cycles = new ArrayList<List<Thread>>();
		var done = new HashSet<Thread>();
		for (var start : edges.keySet()) {
			if (done.contains(start))
				continue;
			var path = new ArrayList<Thread>();
			var indexOf = new HashMap<Thread, Integer>();
			for (var t = start; t != null && !done.contains(t); t = edges.get(t)) {
				var idx = indexOf.put(t, path.size());
				if (idx != null) {
					cycles.add(new ArrayList<>(path.subList(idx, path.size())));
					break;
				}
				path.add(t);
			}
			done.addAll(path);
		}
		return cycles;
	}

	/**
	 * 报告并尝试打断包含虚拟线程的 FastLock 等待环（纯平台线程环已由 ThreadMXBean 路径处理）。
	 */
	private static boolean reportAndBreakLockWaitCycles() {
		boolean reported = false;
		for (var cycle : findLockWaitDeadlockCycles()) {
			boolean hasVirtual = false;
			for (var t : cycle)
				hasVirtual |= t.isVirtual();
			if (!hasVirtual)
				continue;

			var sb = new StringBuilder("Angel.interrupt FastLock wait cycle:\n");
			for (var t : cycle)
				dumpThreadTo(t, sb);
			logger.fatal(sb);

			for (var t : cycle) {
				try {
					t.interrupt();
				} catch (Throwable e) { // SecurityException等
					logger.warn("interrupt thread \"{}\" Id={} failed", t.getName(), t.threadId(), e);
				}
			}
			reported = true;
		}
		return reported;
	}

	private static void dumpThreadTo(Thread t, StringBuilder sb) {
		var lockWaiting = FastLock.waitingThreads.get(t);
		sb.append('"').append(t.getName()).append('"');
		sb.append(" Id=").append(t.threadId()).append(' ').append(t.getState());
		if (t.isVirtual())
			sb.append(" virtual");
		if (lockWaiting != null) {
			sb.append(" on ").append(lockWaiting);
			var owner = lockWaiting.getOwner();
			if (owner != null)
				sb.append(" owned by \"").append(owner.getName()).append("\" Id=").append(owner.threadId());
		}
		sb.append('\n');
		StackTraceElement[] stackTrace = t.getStackTrace();
		int i = 0;
		for (; i < stackTrace.length && i < MAX_DEPTH; i++)
			sb.append("\tat ").append(stackTrace[i]).append('\n');
		if (i < stackTrace.length)
			sb.append("\t...\n");
		sb.append('\n');
	}

	public static void dumpThreadInfoTo(ThreadInfo tInfo, StringBuilder sb) {
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

	/** 中断目标线程，吞掉所有异常。 */
	private @Nullable Map<Long, Thread> interrupt(ThreadInfo tInfo, @Nullable Map<Long, Thread> allThreads) {
		try {
			if (allThreads == null)
				allThreads = enumerate(rootThreadGroup, true);

			var thread = allThreads.get(tInfo.getThreadId());
			if (thread != null) {
				thread.interrupt();
				return allThreads;
			}
			logger.warn("thread not found for interrupt (may have exited): {}", tInfo);
		} catch (Throwable e) {
			logger.fatal(tInfo, e);
		}
		return allThreads;
	}
}
