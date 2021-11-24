package Zeze.Util;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class Benchmark {
    private long startTime;
    private long startProcessCpuTime;
    private OperatingSystemMXBean os;
    public Benchmark() {
        os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        startProcessCpuTime = os.getProcessCpuTime();
        startTime = System.nanoTime();
    }

    public void Report(String name, long tasks) {
        double cpu =  (os.getProcessCpuTime() - startProcessCpuTime) / 1_000_000_000;
        var endTime = System.nanoTime();
        var elapsedTime = endTime - startTime;
        var seconds = (double)elapsedTime / 1_000_000_000;
        var cpupercent = cpu / seconds;
        System.out.println(String.format(
                "%s tasks/s=%.2f time=%.2fs cpu=%.2fs concurrent=%.2f",
                name, (tasks / seconds), seconds, cpu, cpupercent));
    }
}
