package Zeze.Util;

import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class Counters {
	private String name;
	private final ConcurrentSkipListMap<String, LongAdder> counters = new ConcurrentSkipListMap<>();

	public void increment(String name) {
		counters.computeIfAbsent(name, key -> new LongAdder()).increment();
	}

	public Counters() {
		this("");
	}

	public Counters(String name) {
		this.name = name;
		Task.schedule(1000, 1000, this::report);
	}

	private final HashMap<String, AtomicLong> reports = new HashMap<>(); // atomic 在这里仅为了能修改，不是为了线程安全。

	private synchronized void report() {
		var sb = new StringBuilder();
		sb.append(name).append("\n");
		for (var e : counters.entrySet()) {
			var prev = reports.computeIfAbsent(e.getKey(), key -> new AtomicLong());
			var total = e.getValue().sum();
			sb.append(e.getKey()).append(" = ").append(total - prev.get()).append('\n');
			prev.set(total);
		}
		System.out.println(sb);
	}
}