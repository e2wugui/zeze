package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
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
	private static final long PROFILE_TIME = 60_000_000_000L; // 每次开启事务profile的时长(纳秒)
	private static final long PROFILE_LOG_PERIOD = 1_000_000_000L; // 每种事务的日志输出间隔(纳秒)
	private static final long GET_STACK_THRESHOLD = 1_000_000_000L; // 获取栈信息的时长阈值(纳秒)
	private static final int MAX_CONTEXT = 1000; // 一个Profile记录的Context数量上限

	private static final ConcurrentHashMap<String, State> enableProcMap = new ConcurrentHashMap<>(); // <procName,State>

	private static final class State {
		private static final @NotNull VarHandle vhNextProfileTime;

		static {
			try {
				vhNextProfileTime = MethodHandles.lookup().findVarHandle(State.class, "nextProfileTime", long.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private long timeEnd;
		@SuppressWarnings("unused")
		private volatile long nextProfileTime;
	}

	public static final class Context implements AutoCloseable {
		private @Nullable Object name;
		private @Nullable Throwable e; // only for stack
		private long timeBegin;
		private long timeEnd;

		private void clearRef() {
			name = null;
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
	private long startTime; // 0表示没启动profile

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
				var nextTime = state.nextProfileTime;
				if (nextTime <= curTimeNs) {
					if (!State.vhNextProfileTime.compareAndSet(state, nextTime, curTimeNs + PROFILE_LOG_PERIOD))
						continue;
					PerfCounter.logger.info("profile procedure '{}' for {}ms:\n{}",
							procName, runTimeNs / 1_000_000, this);
				}
				break;
			}
		}
	}

	public void onRedo() {
		int n;
		if (startTime == 0 || (n = count) >= MAX_CONTEXT)
			return;
		ArrayList<Context> cs;
		Context c;
		if (n >= (cs = contexts).size())
			cs.add(c = new Context());
		else
			c = cs.get(n);
		c.name = "REDO";
		c.timeBegin = System.nanoTime();
		count = n + 1;
	}

	private @Nullable Context beginContext(@Nullable Object name) {
		int n;
		if (startTime == 0 || (n = count) >= MAX_CONTEXT)
			return null;
		ArrayList<Context> cs;
		Context c;
		if (n >= (cs = contexts).size())
			cs.add(c = new Context());
		else
			c = cs.get(n);
		c.name = name;
		c.timeBegin = System.nanoTime();
		count = n + 1;
		return c;
	}

	public static @Nullable Context begin(@Nullable Object name) {
		var t = Transaction.getCurrent();
		if (t == null)
			return null;
		if (name == null) {
			var p = t.getTopProcedure();
			name = p != null ? p.getActionName() : "";
		}
		return t.profiler.beginContext(name);
	}

	public static @Nullable Context begin(Object... names) {
		return begin((Object)names);
	}

	private int genInfo(@NotNull StringBuilder sb, int indent, int idx, @NotNull Context c) {
		var timeEnd = c.timeEnd;
		if (timeEnd == 0)
			timeEnd = c.timeBegin;
		sb.append(Str.indent(indent)).append((c.timeBegin - startTime) / 1_000_000).append('-')
				.append((timeEnd - startTime) / 1_000_000);
		var name = c.name;
		if (name != null) {
			if (name instanceof Object[]) {
				for (var s : (Object[])name)
					sb.append(' ').append(s);
			} else
				sb.append(' ').append(name);
		}
		sb.append('\n');
		if (c.e != null) {
			var traces = c.e.getStackTrace();
			for (int i = 1, n = traces.length; i < n; i++) {
				var strace = traces[i];
				sb.append("\tat ").append(strace.getClassName()).append('.').append(strace.getMethodName()).append(':')
						.append(strace.getLineNumber()).append('\n');
			}
		}
		while (++idx < count && (c = contexts.get(idx)).timeBegin < timeEnd)
			genInfo(sb, indent + 2, idx, c);
		return idx;
	}

	@Override
	public @NotNull String toString() {
		if (count <= 0)
			return "";
		var sb = new StringBuilder();
		for (int i = 0; i < count; )
			i = genInfo(sb, 0, i, contexts.get(i));
		if (count >= MAX_CONTEXT)
			sb.append("... (exceed ").append(MAX_CONTEXT).append(" records, maybe more ignored)\n");
		return sb.toString();
	}

	public static void main(String[] args) throws InterruptedException {
		var p = new Profiler();
		p.startTime = System.nanoTime();
		try (var ignored = p.beginContext("aa")) {
			try (var ignored1 = p.beginContext("bbb")) {
				Thread.sleep(3000);
			}
			p.onRedo();
			try (var ignored2 = p.beginContext("ccc")) {
				Thread.sleep(500);
			}
		}
		//noinspection EmptyTryBlock
		try (var ignored = p.beginContext("dd")) {
		}
		System.out.println(p);
		p.reset();
		assert p.toString().isEmpty();
	}
}
