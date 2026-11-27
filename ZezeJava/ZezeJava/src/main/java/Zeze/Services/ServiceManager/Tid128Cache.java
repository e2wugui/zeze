package Zeze.Services.ServiceManager;

import org.jspecify.annotations.Nullable;
import Zeze.Util.FastLock;
import Zeze.Util.Id128;

/**
 * 客户端侧Id128号段缓存：持有[start,end)号段逐个发放，耗尽时经Agent申请新段。
 */
public class Tid128Cache extends FastLock {
	public static final int ALLOCATE_COUNT_MIN = 16;
	public static final int ALLOCATE_COUNT_MAX = 1024 * 1024;

	private final String name;
	private final @Nullable AbstractAgent agent;
	private final Id128 start;
	private Id128 current;
	private final Id128 end;
	private final int count;
	private volatile int allocated;

	public Tid128Cache(String name, @Nullable AbstractAgent agent, Id128 start, int count) {
		this.name = name;
		this.agent = agent;
		this.start = start.clone();
		this.current = start.clone();
		this.end = start.add(count);
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public @Nullable AbstractAgent getAgent() {
		return agent;
	}

	public Id128 get() {
		lock();
		try {
			return current.clone();
		} finally {
			unlock();
		}
	}

	public Id128 getStart() {
		return start.clone();
	}

	public int allocateCount() {
		var half = count >> 1;
		var tmp = allocated;
		if (tmp < half)
			return Math.max(ALLOCATE_COUNT_MIN, half);
		return Math.min(ALLOCATE_COUNT_MAX, count * 2);
	}

	public Id128 next() {
		lock();
		try {
			if (current.compareTo(end) < 0) {
				// 先返回后自增，保证按 [start, end) 全量发放。
				var r = current;
				current = current.add(1);
				//noinspection NonAtomicOperationOnVolatileField
				allocated++;
				return r;
			}
		} finally {
			unlock();
		}
		// 号段耗尽时同步申请新段；单步分配（HistoryAllocCount=1）时按请求到达序保证 gid 序与提交序一致。
		//noinspection DataFlowIssue
		return getAgent().allocateTid128CacheFuture(name, getAgent().config.getHistoryAllocCount()).getNonNull().next();
	}
}
