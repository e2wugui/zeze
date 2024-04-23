package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import Zeze.Util.PerfCounter;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 对慢事务的性能统计和分析.
 * 一个Profile对象只能同时用于一个事务,不支持并发
 */
public class Profiler {
	private static final long PROFILE_THRESHOLD = 3_000_000_000L; // 开启事务profile的事务执行时间阈值(纳秒)
	private static final long PROFILE_TIME = 30_000_000_000L; // 每次开启事务profile的时长(纳秒)
	private static final long PROFILE_LOG_PERIOD = 1_000_000_000L; // 每种事务的日志输出间隔(纳秒)
	private static final long GET_STACK_THRESHOLD = 3_000_000_000L; // 获取栈信息的时长阈值(纳秒)
	private static final int MAX_CONTEXT = 1000; // 一个Profile记录的Context数量上限

	private static final Map<String, State> enableProcMap = new ConcurrentHashMap<>(); // <procName,State>

	private static final class State {
		private static final @NotNull AtomicLongFieldUpdater<State> nextProfileTimeUpdater =
				AtomicLongFieldUpdater.newUpdater(State.class, "nextProfileTime");

		private long timeEnd;
		private volatile long nextProfileTime;
	}

	public static final class Context implements AutoCloseable {
		private @Nullable String procedureName;
		private @Nullable Object params;
		private @Nullable Throwable e; // only for stack
		private long timeBegin;
		private long timeEnd;

		private void clearRef() {
			procedureName = null;
			params = null;
			e = null;
		}

		@Override
		public void close() {
			var t = System.nanoTime();
			timeEnd = t;
			if (t - timeBegin >= GET_STACK_THRESHOLD)
				e = new Throwable();
		}
	}

	private final ArrayList<@NotNull Context> contexts = new ArrayList<>();
	private int count; // 当前contexts的有效数量
	private long startTime;

	Profiler() {
	}

	public void reset() {
		if (count != 0) {
			for (int i = 0; i < count; i++)
				contexts.get(i).clearRef();
			count = 0;
			startTime = 0;
		}
	}

	public void onProcedureBegin(@NotNull String procName, long curTimeNs) {
		for (; ; ) {
			var state = enableProcMap.get(procName);
			if (state == null)
				break;
			if (curTimeNs < state.timeEnd) {
				startTime = curTimeNs; // enable profile
				break;
			}
			if (enableProcMap.remove(procName, state))
				break;
		}
	}

	public void onProcedureEnd(@NotNull String procName, long curTimeNs, long runTimeNs) {
		if (runTimeNs < PROFILE_THRESHOLD)
			return;
		var state = enableProcMap.compute(procName, (k, v) -> {
			if (v == null)
				v = new State();
			v.timeEnd = curTimeNs + PROFILE_TIME;
			return v;
		});
		if (startTime != 0) {
			for (; ; ) {
				var nextTime = State.nextProfileTimeUpdater.get(state);
				if (nextTime <= curTimeNs) {
					if (!State.nextProfileTimeUpdater.compareAndSet(state, nextTime, curTimeNs + PROFILE_LOG_PERIOD))
						continue;
					PerfCounter.logger.info("profile procedure '{}':\n{}", procName, this);
				}
				break;
			}
		}
	}

	private @Nullable Context beginContext(@NotNull String procedureName, @Nullable Object params) {
		int n;
		if (startTime == 0 || (n = count) >= MAX_CONTEXT)
			return null;
		ArrayList<Context> cs;
		Context c;
		if (n >= (cs = contexts).size())
			cs.add(c = new Context());
		else
			c = cs.get(n);
		c.procedureName = procedureName;
		c.params = params;
		c.timeBegin = System.nanoTime();
		count = n + 1;
		return c;
	}

	public static @Nullable Context begin() {
		return begin(null);
	}

	public static @Nullable Context begin(@Nullable Object params) {
		var t = Transaction.getCurrent();
		if (t == null)
			return null;
		var ps = t.getProcedureStack();
		var n = ps.size();
		return t.profiler.beginContext(n > 0 ? ps.get(n - 1).getActionName() : "", params);
	}

	private void genInfo(@NotNull StringBuilder sb, int indent, int idx, @NotNull Context c) {
		var timeEnd = c.timeEnd;
		sb.append(Str.indent(indent)).append((c.timeBegin - startTime) / 1_000_000).append('-')
				.append((timeEnd - startTime) / 1_000_000).append(' ').append(c.procedureName);
		if (c.params != null)
			sb.append(": ").append(c.params);
		sb.append('\n');
		if (c.e != null) {
			var traces = c.e.getStackTrace();
			for (int i = 1, n = traces.length; i < n; i++) {
				var strace = traces[i];
				sb.append("\tat ").append(strace.getClassName()).append('.').append(strace.getMethodName()).append(':')
						.append(strace.getLineNumber()).append('\n');
			}
		}
		for (; ; ) {
			if (++idx >= count || (c = contexts.get(idx)).timeBegin >= timeEnd)
				return;
			genInfo(sb, indent + 2, idx, c);
		}
	}

	@Override
	public @NotNull String toString() {
		if (count <= 0)
			return "";
		var sb = new StringBuilder();
		genInfo(sb, 0, 0, contexts.get(0));
		return sb.toString();
	}

	public static void main(String[] args) throws InterruptedException {
		var p = new Profiler();
		p.startTime = System.nanoTime();
		try (var ignored = p.beginContext("aaa", null)) {
			try (var ignored1 = p.beginContext("bbb", null)) {
				Thread.sleep(3000);
			}
			try (var ignored2 = p.beginContext("ccc", null)) {
				Thread.sleep(500);
			}
		}
		System.out.println(p);
		p.reset();
		assert p.toString().isEmpty();
	}
}
