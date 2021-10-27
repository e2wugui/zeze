package Zeze.Util;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class Benchmark {
    private long startTime = System.nanoTime();
    private long endTime;
    private long startProcessCpuTime;
    private OperatingSystemMXBean os;
    public Benchmark() {
        os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        startProcessCpuTime = os.getProcessCpuTime();
    }

    public void Report(long calls) {
        double cpu =  (os.getProcessCpuTime() - startProcessCpuTime) / 1_000_000_000;
        endTime = System.nanoTime();
        var elapsedTime = endTime - startTime;
        var seconds = (double)elapsedTime / 1_000_000_000;
        System.out.println(String.format(
                "calls/s=%.2f time=%.2fs cpu=%.2fs",
                (calls / seconds), seconds, cpu));
    }
}
