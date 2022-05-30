package Benchmark;

import demo.App;
import junit.framework.TestCase;
import org.junit.Assert;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class BBasicSimpleAddConcurrentWithConflict  extends TestCase {
    public final static int AddCount = 1_000_000;

    public void testBenchmark() throws Throwable {
        App.Instance.Start();
        try {
            App.Instance.Zeze.NewProcedure(this::Remove, "remove").Call();
            ArrayList<Future<Long>> tasks = new ArrayList<>(AddCount);
            System.out.println("benchmark start...");
            var b = new Zeze.Util.Benchmark();
            for (int i = 0; i < AddCount; ++i) {
                tasks.add(Zeze.Util.Task.run(App.Instance.Zeze.NewProcedure(this::Add, "Add")));
            }
            //b.Report(this.getClass().getName(), AddCount);
            for (var task : tasks) {
                task.get();
            }
            b.Report(this.getClass().getName(), AddCount);
            App.Instance.Zeze.NewProcedure(this::Check, "check").Call();
            App.Instance.Zeze.NewProcedure(this::Remove, "remove").Call();
        }
        finally {
            App.Instance.Stop();
        }
    }

    private long Check() {
        var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
        Assert.assertEquals(AddCount, r.getLong2());
        //System.out.println(r.getLong2());
        return 0;
    }

    private long Add() {
        var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
        r.setLong2(r.getLong2() + 1);
        return 0;
    }

    private long Remove() {
        App.Instance.demo_Module1.getTable1().remove(1L);
        return 0;
    }
}
