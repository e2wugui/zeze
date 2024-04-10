package Zeze.Util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

public class Counters extends ReentrantLock {
	private static volatile boolean enable = false;

	public static void setEnable(boolean value) {
		enable = value;
	}

	public static boolean isEnable() {
		return enable;
	}

	private final String name;
	private final ConcurrentSkipListMap<String, LongAdder> counters = new ConcurrentSkipListMap<>();
	private long period;
	private Future<?> future;
	private final HashMap<String, AtomicLong> reports = new HashMap<>(); // atomic 在这里仅为了能修改，不是为了线程安全。

	public Counters() {
		this("");
	}

	public Counters(String name) {
		this.name = name;
	}

	public void start() {
		start(2000);
	}

	public void start(long period) {
		if (period <= 0)
			throw new IllegalArgumentException("period <= 0");

		if (null != future)
			future.cancel(false);
		this.period = period;
		future = Task.scheduleUnsafe(period, period, this::report);
	}

	public void increment(String name) {
		if (enable)
			counters.computeIfAbsent(name, __ -> new LongAdder()).increment();
	}

	private void report() {
		lock();
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			var c = Calendar.getInstance();
			var sb = new StringBuilder();
			sb.append(dateFormat.format(c.getTime())).append(name).append("\n");
			var changed = false;
			for (var e : counters.entrySet()) {
				var prev = reports.computeIfAbsent(e.getKey(), __ -> new AtomicLong());
				var total = e.getValue().sum();
				var diff = total - prev.get();
				if (diff > 0)
					changed = true;
				var count = (long)(diff * 1000.0f / period);
				sb.append(e.getKey()).append(" = ").append(count).append('\n');
				prev.set(total);
			}
			if (changed)
				System.out.println(sb);
		} finally {
			unlock();
		}
	}
}
