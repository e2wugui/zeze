package Benchmark;

import Zeze.Util.Task;
import demo.App;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class CBasicSimpleAddConcurrent extends TestCase {
    public static long AddCount = 1_000_000L;
    public static long ConcurrentLevel = 1_000L;

    public void testBenchmark() throws ExecutionException, InterruptedException {
        App.Instance.Start();
        try {
            for (long i = 0; i < ConcurrentLevel; ++i) {
                final long k = i;
                App.Instance.Zeze.NewProcedure(() -> Remove(k), "remove").Call();
            }
            System.out.println("benchmark start...");
            var b = new Zeze.Util.Benchmark();
            ArrayList<Task> tasks = new ArrayList<>();
            for (long i = 0; i < AddCount; ++i) {
                final long c = i % ConcurrentLevel;
                tasks.add(Zeze.Util.Task.Run(
                        App.Instance.Zeze.NewProcedure(() -> Add(c), "Add"),
                        null));
            }
            for (var task : tasks) {
                task.get();
            }
            b.Report(AddCount);
            App.Instance.Zeze.NewProcedure(this::Check, "check").Call();
            for (long i = 0; i < ConcurrentLevel; ++i) {
                final long k = i;
                App.Instance.Zeze.NewProcedure(() -> Remove(k), "remove").Call();
            }
        }
        finally {
            App.Instance.Stop();
        }
    }

    private int Check() {
        long sum = 0;
        for (long i = 0; i < ConcurrentLevel; ++i) {
            var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
            sum += r.getLong2();
        }
        assert sum == AddCount;
        //System.out.println(r.getLong2());
        return 0;
    }

    private int Add(long key) {
        var r = App.Instance.demo_Module1.getTable1().getOrAdd(key);
        r.setLong2(r.getLong2() + 1);
        return 0;
    }

    private int Remove(long key) {
        App.Instance.demo_Module1.getTable1().remove(key);
        return 0;
    }
}
