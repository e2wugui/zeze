package Zeze.Util;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class Benchmark {
	private final long startTime;
	private final long startProcessCpuTime;
	private final OperatingSystemMXBean os;

	public Benchmark() {
		os = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
		startProcessCpuTime = os.getProcessCpuTime();
		startTime = System.nanoTime();
	}

	public double report(String name, long tasks) {
		double cpu = ((double)os.getProcessCpuTime() - startProcessCpuTime) / 1_000_000_000;
		var endTime = System.nanoTime();
		var elapsedTime = endTime - startTime;
		var seconds = (double)elapsedTime / 1_000_000_000;
		var cpuPercent = cpu / seconds;
		System.out.printf("%s tasks/s=%.2f tasks=%d time=%.2fs cpu=%.2fs concurrent=%.2f\n",
				name, (tasks / seconds), tasks, seconds, cpu, cpuPercent);
		return seconds;
	}
}
