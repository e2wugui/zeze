package Zeze.Services.ServiceManager;

import java.util.Objects;
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
		this.start = start;
		this.current = start;
		this.end = start.add(count);
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public AbstractAgent getAgent() {
		return Objects.requireNonNull(agent);
	}

	public Id128 get() {
		return current;
	}

	public Id128 getStart() {
		return start;
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
				// 先返回后自增（对齐TidCache）：按grant区间[start,end)全量发放；先增后返会发放
				// [start+1,end]，全局仅首个号（首段start，即(0,0)）永不进入发放域
				var r = current;
				current = current.add(1);
				//noinspection NonAtomicOperationOnVolatileField
				allocated++;
				return r;
			}
		} finally {
			unlock();
		}
		// 段耗尽，同步分配新段并立即取号。HistoryAllocCount=1（多app部署）时，段请求时刻=取号时刻
		// （调用方在rrs锁内、依赖已建立），SM按请求到达序发号 ⇒ gid序=提交序（History回放/审计的顺序保证）。
		return getAgent().allocateTid128CacheFuture(name, Objects.requireNonNull(getAgent().config).getHistoryAllocCount()).get().next();
	}
}
